/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
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
        assertEquals(6, count);
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
    }

    @Test
    void setCharacterEncoding() {
    }

    @Test
    void getContentLength() {
    }

    @Test
    void getContentLengthLong() {
    }

    @Test
    void getContentType() {
    }

    @Test
    void getInputStream() {
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
    }

    @Test
    void getScheme() {
    }

    @Test
    void getServerName() {
    }

    @Test
    void getServerPort() {
    }

    @Test
    void getReader() {
    }

    @Test
    void getRemoteAddr() {
    }

    @Test
    void getRemoteHost() {
    }

    @Test
    void setAttribute() {
    }

    @Test
    void removeAttribute() {
    }

    @Test
    void getLocale() {
    }

    @Test
    void getLocales() {
    }

    @Test
    void isSecure() {
    }

    @Test
    void getRequestDispatcher() {
    }

    @Test
    void getRealPath() {
    }

    @Test
    void getRemotePort() {
    }

    @Test
    void getLocalName() {
    }

    @Test
    void getLocalAddr() {
    }

    @Test
    void getLocalPort() {
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
            + "Host: localhost:8080\r\n" + "User-Agent: HTTPie/2.2.0\r\n"
            + "Accept-Encoding: gzip, deflate\r\n" + "Accept: */*\r\n"
            + "Connection: keep-alive\r\n"
            + "Cookie: yummy_cookie=choco; tasty_cookie=strawberry\r\n")
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
