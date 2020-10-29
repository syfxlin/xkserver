/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import cn.hutool.core.io.IoUtil;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import me.ixk.xkserver.conntecor.Acceptor;
import me.ixk.xkserver.conntecor.Connector;
import me.ixk.xkserver.conntecor.PollerManager;
import me.ixk.xkserver.conntecor.Server;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Otstar Lin
 * @date 2020/10/28 下午 12:03
 */
class RequestTest {

    private static Request request;

    @BeforeAll
    static void beforeAll() {
        final HttpChannel channel = newChannel();
        final HttpParser parser = new HttpParser(channel);
        parser.parse(queryRequest());
        assertEquals(HttpMethod.GET, channel.getHttpMethod());
        assertEquals("/welcome", channel.getHttpUri().getPath());
        assertEquals("age=18&name=syfxlin", channel.getHttpUri().getQuery());
        assertEquals(HttpVersion.HTTP_1_1, channel.getHttpVersion());
        assertEquals("localhost:8080",
            channel.getHttpField(HttpHeader.HOST).getValue());
        request = new Request(channel);
    }

    @Test
    void getCookies() {
        final HttpChannel channel = newChannel();
        final HttpParser parser = new HttpParser(channel);
        parser.parse(queryRequest());
        final Request request = new Request(channel);
        this.assertCookies(request);
    }

    @Test
    void getAuthType() {
    }

    @Test
    void getDateHeader() {
    }

    @Test
    void getHeader() {
        assertEquals("localhost:8080",
            request.getHeader(HttpHeader.HOST.asString()));
    }

    @Test
    void getHeaders() {
        final Enumeration<String> headers = request
            .getHeaders(HttpHeader.HOST.asString());
        assertEquals("localhost:8080", headers.nextElement());
        assertFalse(headers.hasMoreElements());
    }

    @Test
    void getHeaderNames() {
        final Enumeration<String> names = request.getHeaderNames();
        int count = 0;
        while (names.hasMoreElements()) {
            final String name = names.nextElement();
            count++;
        }
        assertEquals(8, count);
    }

    @Test
    void getIntHeader() {
    }

    @Test
    void getMethod() {
        assertEquals("GET", request.getMethod());
    }

    @Test
    void getPathInfo() {
    }

    @Test
    void getPathTranslated() {
    }

    @Test
    void getContextPath() {
    }

    @Test
    void getQueryString() {
        assertEquals("age=18&name=syfxlin", request.getQueryString());
    }

    @Test
    void getRemoteUser() {
    }

    @Test
    void isUserInRole() {
    }

    @Test
    void getUserPrincipal() {
    }

    @Test
    void getRequestedSessionId() {
    }

    @Test
    void getRequestURI() {
    }

    @Test
    void getRequestURL() {
    }

    @Test
    void getServletPath() {
    }

    @Test
    void getSession() {
    }

    @Test
    void testGetSession() {
    }

    @Test
    void changeSessionId() {
    }

    @Test
    void isRequestedSessionIdValid() {
    }

    @Test
    void isRequestedSessionIdFromCookie() {
    }

    @Test
    void isRequestedSessionIdFromURL() {
    }

    @Test
    void isRequestedSessionIdFromUrl() {
    }

    @Test
    void authenticate() {
    }

    @Test
    void login() {
    }

    @Test
    void logout() {
    }

    @Test
    void getParts() {
    }

    @Test
    void getPart() {
    }

    @Test
    void upgrade() {
    }

    @Test
    void getAttribute() {
    }

    @Test
    void getAttributeNames() {
    }

    @Test
    void getCharacterEncoding() {
        assertEquals("utf-8", request.getCharacterEncoding().toLowerCase());
    }

    @Test
    void setCharacterEncoding() {
        final String resetEncoding = request.getCharacterEncoding();
        try {
            request.setCharacterEncoding("gbk");
            assertEquals("gbk", request.getCharacterEncoding());
            request.setCharacterEncoding(resetEncoding);
        } catch (final UnsupportedEncodingException e) {
            fail(e);
        }
    }

    @Test
    void getContentLength() {
        assertEquals(0, request.getContentLength());
    }

    @Test
    void getContentLengthLong() {
        assertEquals(0L, request.getContentLengthLong());
    }

    @Test
    void getContentType() {
        assertEquals("multipart/form-data; charset=utf-8; boundary=something",
            request.getContentType());
    }

    @Test
    void getInputStream() {
        final HttpChannel channel = newChannel();
        final HttpParser parser = new HttpParser(channel);
        parser.parse(bodyRequest());
        final Request request = new Request(channel);
        try {
            assertEquals("age=18&name=syfxlin", IoUtil
                .getReader(request.getInputStream(), StandardCharsets.UTF_8)
                .lines().collect(Collectors.joining("\r\n")));
        } catch (final IOException e) {
            fail(e);
        }
    }

    @Test
    void getParameter() {
    }

    @Test
    void getParameterNames() {
    }

    @Test
    void getParameterValues() {
    }

    @Test
    void getParameterMap() {
    }

    @Test
    void getProtocol() {
        assertEquals(HttpVersion.HTTP_1_1.asString(), request.getProtocol());
    }

    @Test
    void getScheme() {
        assertEquals("http", request.getScheme());
    }

    @Test
    void getServerName() {
        assertEquals("localhost", request.getServerName());
    }

    @Test
    void getServerPort() {
        assertEquals(8080, request.getServerPort());
    }

    @Test
    void getReader() {
        final HttpChannel channel = newChannel();
        final HttpParser parser = new HttpParser(channel);
        parser.parse(bodyRequest());
        final Request request = new Request(channel);
        try {
            assertEquals("age=18&name=syfxlin", request.getReader().lines()
                                                       .collect(Collectors
                                                           .joining("\r\n")));
        } catch (final IOException e) {
            fail(e);
        }
    }

    @Test
    void getRemoteAddr() {
        assertEquals("", request.getRemoteAddr());
    }

    @Test
    void getRemoteHost() {
        assertEquals("", request.getRemoteHost());
    }

    @Test
    void setAttribute() {
    }

    @Test
    void removeAttribute() {
    }

    @Test
    void getLocale() {
        assertEquals(Locale.CHINA, request.getLocale());
    }

    @Test
    void getLocales() {
        final Enumeration<Locale> locales = request.getLocales();
        assertEquals(Locale.CHINA, locales.nextElement());
        assertFalse(locales.hasMoreElements());
    }

    @Test
    void isSecure() {
        assertFalse(request.isSecure());
    }

    @Test
    void getRequestDispatcher() {
    }

    @Test
    void getRealPath() {
    }

    @Test
    void getRemotePort() {
        assertEquals(0, request.getRemotePort());
    }

    @Test
    void getLocalName() {
        assertNotNull(request.getLocalName());
    }

    @Test
    void getLocalAddr() {
        assertNotNull(request.getLocalAddr());
    }

    @Test
    void getLocalPort() {
        assertEquals(-1, request.getLocalPort());
    }

    @Test
    void getServletContext() {
    }

    @Test
    void startAsync() {
    }

    @Test
    void testStartAsync() {
    }

    @Test
    void isAsyncStarted() {
    }

    @Test
    void isAsyncSupported() {
    }

    @Test
    void getAsyncContext() {
    }

    @Test
    void getDispatcherType() {
    }

    private static ByteBuffer queryRequest() {
        return ByteBuffer.wrap(("GET /welcome?age=18&name=syfxlin HTTP/1.1\r\n"
            + "Host: localhost:8080\r\n" + "Accept-Language: zh-CN\r\n"
            + "Accept-Encoding: gzip, deflate\r\n" + "Accept: */*\r\n"
            + "Connection: keep-alive\r\n"
            + "Cookie: yummy_cookie=choco; tasty_cookie=strawberry\r\n"
            + "Content-Type: multipart/form-data; charset=utf-8; boundary=something\r\n"
            + "Content-Length: 0\r\n").getBytes(StandardCharsets.ISO_8859_1));
    }

    private static ByteBuffer bodyRequest() {
        return ByteBuffer.wrap(
            ("POST /welcome HTTP/1.1\r\n" + "Host: localhost:8080\r\n"
                + "Content-Length: 19\r\n" + "\r\n" + "age=18&name=syfxlin\r\n")
                .getBytes(StandardCharsets.ISO_8859_1));
    }

    private void assertCookies(final Request request) {
        final Map<String, Cookie> cookieMap = Arrays
            .stream(request.getCookies())
            .collect(Collectors.toMap(Cookie::getName, e -> e));
        final Cookie c1 = cookieMap.get("yummy_cookie");
        final Cookie c2 = cookieMap.get("tasty_cookie");
        if (c1 == null || c2 == null || !"choco".equals(c1.getValue())
            || !"strawberry".equals(c2.getValue())) {
            fail("Cookies is not equals");
        }
    }

    private static HttpChannel newChannel() {
        final SelectionKey key;
        try {
            final SocketChannel channel = SocketChannel.open();
            channel.configureBlocking(false);
            final Selector selector = Selector.open();
            key = channel.register(selector, 0);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return new HttpChannel(new Connector() {

            @Override
            public Server getServer() {
                return null;
            }

            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public PollerManager getPollerManager() {
                return null;
            }

            @Override
            public Acceptor getAcceptor() {
                return null;
            }
        }, null, key);
    }
}
