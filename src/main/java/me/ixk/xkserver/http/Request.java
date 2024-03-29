/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import me.ixk.xkserver.http.HttpHeader.Value;
import me.ixk.xkserver.http.MultiParts.MultiPartConfig;
import me.ixk.xkserver.utils.Json;
import me.ixk.xkserver.utils.MultiMap;

/**
 * Request
 *
 * @author Otstar Lin
 * @date 2020/10/27 上午 8:15
 */
public class Request implements HttpServletRequest {

    private static final String MULTIPART_CONFIG_ANNOTATION =
        "me.ixk.xkserver.multipartConfig";
    private static final MultiMap<String> NO_PARAMS = new MultiMap<>();
    public static final String ALL_HOST_ADDRESS = "0.0.0.0";
    public static final String IPV6_LEFT = "[";
    public static final String IPV6_RIGHT = "]";
    public static final String IPV6_SPLIT = ":";
    private final HttpChannel httpChannel;
    private HttpFields httpFields;
    private HttpMethod httpMethod;
    private HttpUri httpUri;
    private HttpVersion httpVersion;
    private HttpInput httpInput;
    private InetSocketAddress remote;
    private InetSocketAddress local;
    private List<Cookie> cookies;
    private String characterEncoding;

    private Attributes attributes;
    private boolean sessionIdReady = false;
    private String sessionId;
    private boolean sessionFromCookie = true;
    private SessionCookieConfig sessionCookieConfig;

    private MultiMap<String> parameters;
    private MultiMap<String> queryParameters;
    private MultiMap<String> contentParameters;
    private MultiParts multiParts;
    private JsonNode parseBody;

    public Request(final HttpChannel httpChannel) {
        this.httpChannel = httpChannel;
        this.setMetaData(httpChannel);
    }

    private void setMetaData(final HttpChannel channel) {
        this.httpFields = channel.getHttpFields();
        this.httpMethod = channel.getHttpMethod();
        this.httpVersion = channel.getHttpVersion();
        this.httpInput = channel.getHttpInput();
        this.remote =
            (InetSocketAddress) channel.getSocket().getRemoteSocketAddress();
        this.local =
            (InetSocketAddress) channel.getSocket().getLocalSocketAddress();
        final HttpUri uri = channel.getHttpUri();
        if (uri.isAbsolute() && uri.hasAuthority() && uri.getPath() != null) {
            this.httpUri = uri;
        } else {
            final HttpUri build = new HttpUri(uri);
            if (!build.isAbsolute()) {
                build.setScheme("http");
            }
            if (build.getPath() == null) {
                build.setPath("/");
            }
            if (!uri.hasAuthority()) {
                final String hostAndPort =
                    this.httpFields.getValue(HttpHeader.HOST.asString());
                if (hostAndPort != null) {
                    final String[] split = hostAndPort.split(":");
                    build.setHost(split[0]);
                    if (split.length > 1) {
                        build.setPort(Integer.parseInt(split[1]));
                    } else {
                        build.setPort(
                            this.schemeDefaultPort(build.getScheme())
                        );
                    }
                } else {
                    build.setHost(this.findServerHost());
                    build.setPort(this.findServerPort());
                }
            }
            this.httpUri = build;
        }
    }

    @Override
    public String getAuthType() {
        // TODO: 未完成
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        if (this.cookies == null) {
            this.cookies = new ArrayList<>();
            final HttpField field =
                this.httpFields.get(HttpHeader.COOKIE.asString());
            if (field != null) {
                for (int i = 0; i < field.size(); i++) {
                    for (final String value : field.getParamValues(i)) {
                        final String[] cookie = value.split("=");
                        this.cookies.add(
                                new Cookie(cookie[0].trim(), cookie[1].trim())
                            );
                    }
                }
            }
        }
        return this.cookies.toArray(Cookie[]::new);
    }

    @Override
    public long getDateHeader(final String name) {
        final HttpField field = this.httpFields.get(name);
        if (field == null) {
            return -1;
        }
        final String value = field.getParamValue();
        if (value == null) {
            return -1;
        }
        return DateParser.parseDate(value);
    }

    @Override
    public String getHeader(final String name) {
        return this.httpFields.getValue(name);
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
        return Collections.enumeration(this.httpFields.getValues(name));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(this.httpFields.keySet());
    }

    @Override
    public int getIntHeader(final String name) {
        final String value = this.getHeader(name);
        return value == null ? -1 : Integer.parseInt(value);
    }

    @Override
    public String getMethod() {
        return httpMethod.asString();
    }

    @Override
    public String getPathInfo() {
        // TODO: 未完成
        return null;
    }

    @Override
    public String getPathTranslated() {
        // TODO: 未完成
        return null;
    }

    @Override
    public String getContextPath() {
        // TODO: 未完成
        return null;
    }

    @Override
    public String getQueryString() {
        final HttpUri uri = this.httpUri;
        return uri == null ? null : uri.getQuery();
    }

    @Override
    public String getRemoteUser() {
        final Principal principal = this.getUserPrincipal();
        if (principal == null) {
            return null;
        }
        return principal.getName();
    }

    @Override
    public boolean isUserInRole(final String name) {
        // TODO: 未完成
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        // TODO: 未完成
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        if (!this.sessionIdReady && this.sessionId == null) {
            // Cookie
            final Cookie[] cookies = this.getCookies();
            final String name = this.sessionCookieConfig.getName();
            if (cookies != null && cookies.length > 0) {
                for (final Cookie cookie : cookies) {
                    if (name.equalsIgnoreCase(cookie.getName())) {
                        final String id = cookie.getValue();
                        this.sessionFromCookie = true;
                        this.sessionId = id;
                        break;
                    }
                }
            }
            // Url
            if (this.sessionId == null) {
                final String uri = this.getRequestURI();
                final String prefix = ";" + name + "=";
                int s = uri.indexOf(prefix);
                if (s >= 0) {
                    s += prefix.length();
                    int i = s;
                    while (i < uri.length()) {
                        final char c = uri.charAt(i);
                        if (c == ';' || c == '#' || c == '?' || c == '/') {
                            break;
                        }
                        i++;
                    }

                    this.sessionId = uri.substring(s, i);
                    this.sessionFromCookie = false;
                }
            }
            this.sessionIdReady = true;
        }
        return this.sessionId;
    }

    @Override
    public String getRequestURI() {
        return this.httpUri == null ? null : this.httpUri.getPath();
    }

    @Override
    public StringBuffer getRequestURL() {
        if (this.httpUri == null) {
            return null;
        }
        final StringBuffer url = new StringBuffer();
        final String scheme = this.getScheme();
        url
            .append(scheme)
            .append("://")
            .append(this.normalizeHost(this.getServerName()));
        final int port = this.getServerPort();
        if (port > 0) {
            switch (scheme) {
                case "http":
                    if (port != 80) {
                        url.append(":").append(port);
                    }
                    break;
                case "https":
                    if (port != 443) {
                        url.append(":").append(port);
                    }
                    break;
                default:
                    url.append(":").append(port);
            }
        }
        url.append(this.getRequestURI());
        return url;
    }

    @Override
    public String getServletPath() {
        // TODO: 未完成
        return null;
    }

    @Override
    public HttpSession getSession(final boolean b) {
        // TODO: 未完成
        return null;
    }

    @Override
    public HttpSession getSession() {
        // TODO: 未完成
        return null;
    }

    @Override
    public String changeSessionId() {
        // TODO: 未完成
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        final String id = this.getRequestedSessionId();
        if (id == null) {
            return false;
        }
        return this.getSession(false) != null;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        this.getRequestedSessionId();
        return this.sessionFromCookie;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        this.getRequestedSessionId();
        return !this.sessionFromCookie;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return this.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean authenticate(final HttpServletResponse httpServletResponse)
        throws IOException, ServletException {
        // TODO: 未完成
        return false;
    }

    @Override
    public void login(final String name, final String name1)
        throws ServletException {
        // TODO: 未完成
    }

    @Override
    public void logout() throws ServletException {
        // TODO: 未完成
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        if (this.multiParts == null) {
            this.extractContentParameters();
        }
        return this.multiParts.getCollection();
    }

    @Override
    public Part getPart(final String name)
        throws IOException, ServletException {
        if (this.multiParts == null) {
            this.extractContentParameters();
        }
        return this.multiParts.getPart(name);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(final Class<T> aClass)
        throws IOException, ServletException {
        // TODO: 未完成
        return null;
    }

    @Override
    public Object getAttribute(final String name) {
        // TODO: 测试使用
        if (MULTIPART_CONFIG_ANNOTATION.equals(name)) {
            return new MultiPartConfig(null, -1, -1, 0);
        }
        return this.getAttributes().getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return this.getAttributes().getAttributeNames();
    }

    public Attributes getAttributes() {
        if (this.attributes == null) {
            this.attributes = new AttributesMap();
        }
        return this.attributes;
    }

    @Override
    public String getCharacterEncoding() {
        if (this.characterEncoding == null) {
            final HttpField field =
                this.httpFields.get(HttpHeader.CONTENT_TYPE.asString());
            if (field != null) {
                this.characterEncoding =
                    field.getParam(Value.CHARSET.asString());
            }
            if (this.characterEncoding == null) {
                this.characterEncoding = StandardCharsets.UTF_8.name();
            }
        }
        return this.characterEncoding;
    }

    @Override
    public void setCharacterEncoding(final String encoding)
        throws UnsupportedEncodingException {
        Charset.forName(encoding);
        this.characterEncoding = encoding;
    }

    @Override
    public int getContentLength() {
        final HttpField field =
            this.httpFields.get(HttpHeader.CONTENT_LENGTH.asString());
        return field == null ? -1 : Integer.parseInt(field.getValue());
    }

    @Override
    public long getContentLengthLong() {
        final HttpField field =
            this.httpFields.get(HttpHeader.CONTENT_LENGTH.asString());
        return field == null ? -1 : Long.parseLong(field.getValue());
    }

    @Override
    public String getContentType() {
        final HttpField field =
            this.httpFields.get(HttpHeader.CONTENT_TYPE.asString());
        return field == null ? null : field.getValue();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return this.httpInput;
    }

    @Override
    public String getParameter(final String name) {
        return this.getParameters().getValue(name, 0);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(this.getParameters().keySet());
    }

    @Override
    public String[] getParameterValues(final String name) {
        final List<String> values = this.getParameters().getValues(name);
        return values == null ? null : values.toArray(String[]::new);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return this.getParameters().toStringArrayMap();
    }

    @Override
    public String getProtocol() {
        final HttpVersion version = this.httpVersion;
        return version == null ? null : version.asString();
    }

    @Override
    public String getScheme() {
        final HttpUri uri = this.httpUri;
        return uri == null ? "http" : uri.getScheme();
    }

    @Override
    public String getServerName() {
        final HttpUri uri = this.httpUri;
        return uri == null ? this.findServerHost() : uri.getHost();
    }

    @Override
    public int getServerPort() {
        final HttpUri uri = this.httpUri;
        final int port = uri == null ? 0 : uri.getPort();
        if (port <= 0) {
            return this.schemeDefaultPort(this.getScheme());
        }
        return port;
    }

    private int schemeDefaultPort(final String scheme) {
        switch (scheme) {
            case "https":
            case "wss":
                return 443;
            case "http":
            case "ws":
            default:
                return 80;
        }
    }

    private String findServerHost() {
        final String name = this.getLocalName();
        if (name != null) {
            return name;
        }
        return this.getLocalAddr();
    }

    private int findServerPort() {
        return this.getLocalPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.httpInput));
    }

    @Override
    public String getRemoteAddr() {
        if (this.remote == null) {
            return "";
        }
        final InetAddress address = this.remote.getAddress();
        final String result = address == null
            ? this.remote.getHostString()
            : address.getHostAddress();
        return this.normalizeHost(result);
    }

    @Override
    public String getRemoteHost() {
        if (this.remote == null) {
            return "";
        }
        return this.normalizeHost(this.remote.getHostString());
    }

    private String normalizeHost(final String host) {
        if (
            host.isEmpty() ||
            host.startsWith(IPV6_LEFT) ||
            !host.contains(IPV6_SPLIT)
        ) {
            return host;
        }
        return IPV6_LEFT + host + IPV6_RIGHT;
    }

    @Override
    public void setAttribute(final String name, final Object o) {
        this.getAttributes().setAttribute(name, o);
    }

    @Override
    public void removeAttribute(final String name) {
        this.getAttributes().removeAttribute(name);
    }

    @Override
    public Locale getLocale() {
        final HttpField field =
            this.httpFields.get(HttpHeader.ACCEPT_LANGUAGE.asString());
        if (field == null) {
            return Locale.getDefault();
        }
        final String language = field.getValue();
        return this.parseLocal(language);
    }

    private Locale parseLocal(String language) {
        String country = "";
        final int dash = language.indexOf('-');
        if (dash > -1) {
            country = language.substring(dash + 1).trim();
            language = language.substring(0, dash).trim();
        }
        return new Locale(language, country);
    }

    @Override
    public Enumeration<Locale> getLocales() {
        final HttpField field =
            this.httpFields.get(HttpHeader.ACCEPT_LANGUAGE.asString());
        if (field == null) {
            return Collections.enumeration(
                Collections.singletonList(Locale.getDefault())
            );
        }
        return Collections.enumeration(
            field
                .getValues()
                .stream()
                .map(this::parseLocal)
                .collect(Collectors.toList())
        );
    }

    @Override
    public boolean isSecure() {
        return "https".equalsIgnoreCase(this.getScheme());
    }

    @Override
    public RequestDispatcher getRequestDispatcher(final String name) {
        // TODO: 未完成
        return null;
    }

    @Override
    public String getRealPath(final String name) {
        // TODO: 未完成
        return null;
    }

    @Override
    public int getRemotePort() {
        return this.remote == null ? 0 : this.remote.getPort();
    }

    @Override
    public String getLocalName() {
        if (this.local != null) {
            return this.normalizeHost(this.local.getHostString());
        }
        try {
            final String name = InetAddress.getLocalHost().getHostName();
            if (ALL_HOST_ADDRESS.equals(name)) {
                return null;
            }
            return this.normalizeHost(name);
        } catch (final UnknownHostException e) {
            return null;
        }
    }

    @Override
    public String getLocalAddr() {
        if (this.local != null) {
            final InetAddress address = this.local.getAddress();
            final String result = address == null
                ? this.local.getHostString()
                : address.getHostAddress();
            return this.normalizeHost(result);
        }
        try {
            final String name = InetAddress.getLocalHost().getHostAddress();
            if (ALL_HOST_ADDRESS.equals(name)) {
                return null;
            }
            return this.normalizeHost(name);
        } catch (final UnknownHostException e) {
            return null;
        }
    }

    @Override
    public int getLocalPort() {
        return this.local == null ? -1 : this.local.getPort();
    }

    @Override
    public ServletContext getServletContext() {
        // TODO: 未完成
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        // TODO: 未完成
        return null;
    }

    @Override
    public AsyncContext startAsync(
        final ServletRequest servletRequest,
        final ServletResponse servletResponse
    ) throws IllegalStateException {
        // TODO: 未完成
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        // TODO: 未完成
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        // TODO: 未完成
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        // TODO: 未完成
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        // TODO: 未完成
        return null;
    }

    public HttpChannel getHttpChannel() {
        return httpChannel;
    }

    public JsonNode getParseBody() {
        if (parseBody == null) {
            this.extractContentParameters();
        }
        return parseBody;
    }

    private MultiMap<String> decodeParameters(final String paramsString) {
        if (paramsString == null) {
            return null;
        }
        final MultiMap<String> params = new MultiMap<>();
        for (final String param : paramsString.split("&")) {
            final String[] kv = param.split("=");
            params.add(
                URLUtil.decode(kv[0].trim(), this.getCharacterEncoding()),
                kv.length == 1
                    ? ""
                    : URLUtil.decode(kv[1].trim(), this.getCharacterEncoding())
            );
        }
        return params;
    }

    private void extractQueryParameters() {
        if (this.httpUri == null || StrUtil.isEmpty(this.httpUri.getQuery())) {
            this.queryParameters = NO_PARAMS;
        } else {
            try {
                this.queryParameters =
                    this.decodeParameters(this.getQueryString());
            } catch (final UtilException e) {
                throw new BadMessageException("Unable to parse URI query", e);
            }
        }
    }

    private void extractContentParameters() {
        final String contentType = this.getContentType();
        final int contentLength = this.getContentLength();
        if (StrUtil.isEmpty(contentType) || contentLength == 0) {
            this.contentParameters = NO_PARAMS;
        } else {
            final HttpField field =
                this.httpFields.get(HttpHeader.CONTENT_TYPE.asString());
            final String baseType = field.getParamValue();
            if (
                MimeType.FORM_ENCODED.is(baseType) && this.isFormEncodedMethod()
            ) {
                if (!this.isContentEncodingSupported()) {
                    throw new BadMessageException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Unsupported Content-Encoding"
                    );
                }
                this.extractFormParameters();
            } else if (
                MimeType.MULTIPART_FORM_DATA.is(baseType) &&
                this.isFormEncodedMethod() &&
                this.getAttribute(MULTIPART_CONFIG_ANNOTATION) != null
            ) {
                if (!this.isContentEncodingSupported()) {
                    throw new BadMessageException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Unsupported Content-Encoding"
                    );
                }
                this.extractParts();
            } else if (
                (
                    MimeType.TEXT_JSON.is(baseType) ||
                    MimeType.APPLICATION_JSON.is(baseType)
                ) &&
                this.isFormEncodedMethod()
            ) {
                if (!this.isContentEncodingSupported()) {
                    throw new BadMessageException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Unsupported Content-Encoding"
                    );
                }
                this.extractJson();
            }
        }
    }

    private void extractFormParameters() {
        try {
            this.contentParameters =
                this.decodeParameters(
                        IoUtil.read(
                            this.getInputStream(),
                            this.getCharacterEncoding()
                        )
                    );
        } catch (final UtilException | IOException e) {
            throw new BadMessageException("Unable to parse Form parameters", e);
        }
    }

    private boolean isFormEncodedMethod() {
        switch (this.httpMethod) {
            case POST:
            case PUT:
                return true;
            default:
                return false;
        }
    }

    private boolean isContentEncodingSupported() {
        final HttpField contentEncoding =
            this.httpFields.get(HttpHeader.CONTENT_ENCODING.asString());
        if (contentEncoding == null) {
            return true;
        }
        return HttpHeader.Value.IDENTITY.is(contentEncoding.getValue());
    }

    private void extractParts() {
        this.multiParts =
            new MultiParts(
                (MultiPartConfig) this.getAttribute(
                        MULTIPART_CONFIG_ANNOTATION
                    ),
                this.getContentType()
            );
        final HttpField field =
            this.httpFields.get(HttpHeader.CONTENT_TYPE.asString());
        final String boundary = field.getParam("boundary");
        if (boundary == null) {
            throw new BadMessageException(
                "Content-Type did not find boundary attribute"
            );
        }
        final MultiPartParser parser = new MultiPartParser(
            this.multiParts,
            boundary
        );
        if (this.httpInput.isReady()) {
            parser.parse(this.httpInput.readBuffer());
        }
        ByteArrayOutputStream os = null;
        for (final Part part : this.multiParts.getCollection()) {
            if (part.getSubmittedFileName() == null) {
                String charset = null;
                if (part.getContentType() != null) {
                    final HttpField contentType = new HttpField(
                        HttpHeader.CONTENT_TYPE,
                        Collections.singletonList(this.getContentType())
                    );
                    charset = contentType.getParam("charset");
                }

                try (final InputStream is = part.getInputStream()) {
                    if (os == null) {
                        os = new ByteArrayOutputStream();
                    }
                    IoUtil.copy(is, os);

                    final String content = new String(
                        os.toByteArray(),
                        Charset.forName(
                            charset == null
                                ? this.getCharacterEncoding()
                                : charset
                        )
                    );
                    if (this.contentParameters == null) {
                        this.contentParameters = new MultiMap<>();
                    }
                    this.contentParameters.add(part.getName(), content);
                } catch (final IOException e) {
                    throw new BadMessageException(
                        "Unable to parse multi parts",
                        e
                    );
                }
                os.reset();
            }
        }
    }

    private void extractJson() {
        try {
            this.parseBody =
                Json.parse(
                    IoUtil.read(
                        this.getInputStream(),
                        this.getCharacterEncoding()
                    )
                );
        } catch (final IOException e) {
            throw new BadMessageException("Unable to parse Json body", e);
        }
    }

    private MultiMap<String> getParameters() {
        if (this.parameters != null) {
            return this.parameters;
        }
        if (this.queryParameters == null) {
            this.extractQueryParameters();
        }
        if (this.contentParameters == null) {
            this.extractContentParameters();
        }
        if (this.isEmptyParameters(this.queryParameters)) {
            this.parameters = this.contentParameters;
        } else if (this.isEmptyParameters(this.contentParameters)) {
            this.parameters = this.queryParameters;
        } else {
            this.parameters = new MultiMap<>();
            this.parameters.addAllValues(this.queryParameters);
            this.parameters.addAllValues(this.contentParameters);
        }
        return this.parameters;
    }

    private boolean isEmptyParameters(final MultiMap<String> parameters) {
        return parameters == null || parameters.isEmpty();
    }
}
