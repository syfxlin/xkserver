/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import me.ixk.xkserver.http.HttpHeader.Value;

/**
 * Request
 *
 * @author Otstar Lin
 * @date 2020/10/27 上午 8:15
 */
public class Request implements HttpServletRequest {
    private final HttpChannel httpChannel;
    private final HttpFields httpFields;
    private final HttpMethod httpMethod;
    private final HttpUri httpUri;
    private final HttpVersion httpVersion;
    private final HttpInput httpInput;
    private final InetSocketAddress remote;
    private final InetSocketAddress local;
    private List<Cookie> cookies;
    private String characterEncoding;

    public Request(
        final HttpChannel httpChannel,
        final HttpFields httpFields,
        final HttpMethod httpMethod,
        final HttpUri httpUri,
        final HttpVersion httpVersion,
        final HttpInput httpInput
    ) {
        this.httpChannel = httpChannel;
        this.httpFields = httpFields;
        this.httpMethod = httpMethod;
        this.httpUri = httpUri;
        this.httpVersion = httpVersion;
        this.httpInput = httpInput;
        this.remote =
            (InetSocketAddress) this.httpChannel.getSocket()
                .getRemoteSocketAddress();
        this.local =
            (InetSocketAddress) this.httpChannel.getSocket()
                .getLocalSocketAddress();
    }

    @Override
    public String getAuthType() {
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
                    for (final Entry<String, String> entry : field
                        .getParams(i)
                        .entrySet()) {
                        this.cookies.add(
                                new Cookie(entry.getKey(), entry.getValue())
                            );
                    }
                }
            }
        }
        return this.cookies.toArray(Cookie[]::new);
    }

    @Override
    public long getDateHeader(final String name) {
        return 0;
    }

    @Override
    public String getHeader(final String name) {
        return httpFields.getValue(name);
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
        return Collections.enumeration(httpFields.getValues(name));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(httpFields.keySet());
    }

    @Override
    public int getIntHeader(final String name) {
        return 0;
    }

    @Override
    public String getMethod() {
        return httpMethod.asString();
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(final String name) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public String getServletPath() {
        return null;
    }

    @Override
    public HttpSession getSession(final boolean b) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(final HttpServletResponse httpServletResponse)
        throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(final String name, final String name1)
        throws ServletException {}

    @Override
    public void logout() throws ServletException {}

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(final String name)
        throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(final Class<T> aClass)
        throws IOException, ServletException {
        return null;
    }

    @Override
    public Object getAttribute(final String name) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
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
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(final String name) {
        return new String[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return null;
    }

    @Override
    public String getProtocol() {
        return this.httpVersion.asString();
    }

    @Override
    public String getScheme() {
        return this.httpUri.getScheme();
    }

    @Override
    public String getServerName() {
        return this.httpUri.getHost();
    }

    @Override
    public int getServerPort() {
        return this.httpUri.getPort();
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

    private String normalizeHost(String host) {
        if (host.isEmpty() || host.charAt(0) == '[' || !host.contains(":")) {
            return host;
        }
        return "[" + host + "]";
    }

    @Override
    public void setAttribute(final String name, final Object o) {}

    @Override
    public void removeAttribute(final String name) {}

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
        return null;
    }

    @Override
    public String getRealPath(final String name) {
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
            String name = InetAddress.getLocalHost().getHostName();
            if ("0.0.0.0".equals(name)) {
                return null;
            }
            return this.normalizeHost(name);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Override
    public String getLocalAddr() {
        if (this.local != null) {
            InetAddress address = this.local.getAddress();
            String result = address == null
                ? this.local.getHostString()
                : address.getHostAddress();
            return this.normalizeHost(result);
        }
        try {
            String name = InetAddress.getLocalHost().getHostAddress();
            if ("0.0.0.0".equals(name)) {
                return null;
            }
            return this.normalizeHost(name);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Override
    public int getLocalPort() {
        return this.local == null ? 0 : this.local.getPort();
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(
        final ServletRequest servletRequest,
        final ServletResponse servletResponse
    )
        throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }
}
