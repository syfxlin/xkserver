/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import cn.hutool.core.io.IoUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.Part;
import me.ixk.xkserver.conntecor.Acceptor;
import me.ixk.xkserver.conntecor.Connector;
import me.ixk.xkserver.conntecor.PollerManager;
import me.ixk.xkserver.conntecor.Server;
import me.ixk.xkserver.io.ByteBufferStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Otstar Lin
 * @date 2020/10/28 下午 12:03
 */
class RequestTest {
    private static Request queryRequest;
    private static Request bodyRequest;
    private static Request multiRequest;
    private static Request jsonRequest;

    @BeforeAll
    static void beforeAll() {
        final HttpChannel c1 = newChannel();
        final HttpParser p1 = new HttpParser(c1);
        p1.parse(queryRequest());
        p1.end();
        assertEquals(HttpMethod.GET, c1.getHttpMethod());
        assertEquals("/welcome", c1.getHttpUri().getPath());
        assertEquals("age=18&name=syfxlin", c1.getHttpUri().getQuery());
        assertEquals(HttpVersion.HTTP_1_1, c1.getHttpVersion());
        assertEquals(
            "localhost:8080",
            c1.getHttpField(HttpHeader.HOST).getValue()
        );
        queryRequest = new Request(c1);

        final HttpChannel c2 = newChannel();
        final HttpParser p2 = new HttpParser(c2);
        p2.parse(bodyRequest());
        p2.end();
        bodyRequest = new Request(c2);

        final HttpChannel c3 = newChannel();
        final HttpParser p3 = new HttpParser(c3);
        p3.parse(multiPartRequest());
        p3.end();
        multiRequest = new Request(c3);

        final HttpChannel c4 = newChannel();
        final HttpParser p4 = new HttpParser(c4);
        p4.parse(jsonRequest());
        p4.end();
        jsonRequest = new Request(c4);
    }

    @Test
    void getCookies() {
        final HttpChannel channel = newChannel();
        final HttpParser parser = new HttpParser(channel);
        parser.parse(queryRequest());
        parser.end();
        final Request request = new Request(channel);
        this.assertCookies(request);
    }

    @Test
    void getAuthType() {}

    @Test
    void getDateHeader() {}

    @Test
    void getHeader() {
        assertEquals(
            "localhost:8080",
            queryRequest.getHeader(HttpHeader.HOST.asString())
        );
    }

    @Test
    void getHeaders() {
        final Enumeration<String> headers = queryRequest.getHeaders(
            HttpHeader.HOST.asString()
        );
        assertEquals("localhost:8080", headers.nextElement());
        assertFalse(headers.hasMoreElements());
    }

    @Test
    void getHeaderNames() {
        final Enumeration<String> names = queryRequest.getHeaderNames();
        int count = 0;
        while (names.hasMoreElements()) {
            final String name = names.nextElement();
            count++;
        }
        assertEquals(8, count);
    }

    @Test
    void getIntHeader() {}

    @Test
    void getMethod() {
        assertEquals("GET", queryRequest.getMethod());
    }

    @Test
    void getPathInfo() {}

    @Test
    void getPathTranslated() {}

    @Test
    void getContextPath() {}

    @Test
    void getQueryString() {
        assertEquals("age=18&name=syfxlin", queryRequest.getQueryString());
    }

    @Test
    void getRemoteUser() {}

    @Test
    void isUserInRole() {}

    @Test
    void getUserPrincipal() {}

    @Test
    void getRequestedSessionId() {}

    @Test
    void getRequestURI() {}

    @Test
    void getRequestURL() {}

    @Test
    void getServletPath() {}

    @Test
    void getSession() {}

    @Test
    void testGetSession() {}

    @Test
    void changeSessionId() {}

    @Test
    void isRequestedSessionIdValid() {}

    @Test
    void isRequestedSessionIdFromCookie() {}

    @Test
    void isRequestedSessionIdFromURL() {}

    @Test
    void isRequestedSessionIdFromUrl() {}

    @Test
    void authenticate() {}

    @Test
    void login() {}

    @Test
    void logout() {}

    @Test
    void getParts() throws IOException, ServletException {
        assertEquals(2, multiRequest.getParts().size());
    }

    @Test
    void getPart() throws IOException, ServletException {
        final Part elementName = multiRequest.getPart("element-name");
        assertNotNull(elementName);
        assertEquals(
            "Name",
            IoUtil.read(
                elementName.getInputStream(),
                StandardCharsets.ISO_8859_1
            )
        );
        final Part data = multiRequest.getPart("data");
        assertNotNull(data);
        assertEquals(
            "{\n" + "  \"id\": 999,\n" + "  \"value\": \"content\"\n" + "}",
            IoUtil.read(data.getInputStream(), StandardCharsets.ISO_8859_1)
        );
        assertEquals("Name", multiRequest.getParameter("element-name"));
    }

    @Test
    void upgrade() {}

    @Test
    void getAttribute() {}

    @Test
    void getAttributeNames() {}

    @Test
    void getCharacterEncoding() {
        assertEquals(
            "utf-8",
            queryRequest.getCharacterEncoding().toLowerCase()
        );
    }

    @Test
    void setCharacterEncoding() {
        final String resetEncoding = queryRequest.getCharacterEncoding();
        try {
            queryRequest.setCharacterEncoding("gbk");
            assertEquals("gbk", queryRequest.getCharacterEncoding());
            queryRequest.setCharacterEncoding(resetEncoding);
        } catch (final UnsupportedEncodingException e) {
            fail(e);
        }
    }

    @Test
    void getContentLength() {
        assertEquals(0, queryRequest.getContentLength());
    }

    @Test
    void getContentLengthLong() {
        assertEquals(0L, queryRequest.getContentLengthLong());
    }

    @Test
    void getContentType() {
        assertEquals(
            "multipart/form-data; charset=utf-8; boundary=something",
            queryRequest.getContentType()
        );
    }

    @Test
    void getInputStream() {
        final HttpChannel channel = newChannel();
        final HttpParser parser = new HttpParser(channel);
        parser.parse(bodyRequest());
        parser.end();
        final Request request = new Request(channel);
        try {
            assertEquals(
                "age=18&name=syfxlin",
                IoUtil
                    .getReader(request.getInputStream(), StandardCharsets.UTF_8)
                    .lines()
                    .collect(Collectors.joining("\r\n"))
            );
        } catch (final IOException e) {
            fail(e);
        }
    }

    @Test
    void getParameter() {
        assertEquals("syfxlin", queryRequest.getParameter("name"));
        assertEquals("syfxlin", bodyRequest.getParameter("name"));
    }

    @Test
    void getParameterNames() {
        assertEquals("name", queryRequest.getParameterNames().nextElement());
    }

    @Test
    void getParameterValues() {
        assertArrayEquals(
            new String[] { "syfxlin" },
            queryRequest.getParameterValues("name")
        );
    }

    @Test
    void getParameterMap() {
        assertEquals(
            "{name=[syfxlin],age=[18]}",
            queryRequest.getParameterMap().toString()
        );
    }

    @Test
    void getProtocol() {
        assertEquals(
            HttpVersion.HTTP_1_1.asString(),
            queryRequest.getProtocol()
        );
    }

    @Test
    void getScheme() {
        assertEquals("http", queryRequest.getScheme());
    }

    @Test
    void getServerName() {
        assertEquals("localhost", queryRequest.getServerName());
    }

    @Test
    void getServerPort() {
        assertEquals(8080, queryRequest.getServerPort());
    }

    @Test
    void getReader() {
        final HttpChannel channel = newChannel();
        final HttpParser parser = new HttpParser(channel);
        parser.parse(bodyRequest());
        parser.end();
        final Request request = new Request(channel);
        try {
            assertEquals(
                "age=18&name=syfxlin",
                request.getReader().lines().collect(Collectors.joining("\r\n"))
            );
        } catch (final IOException e) {
            fail(e);
        }
    }

    @Test
    void getRemoteAddr() {
        assertEquals("", queryRequest.getRemoteAddr());
    }

    @Test
    void getRemoteHost() {
        assertEquals("", queryRequest.getRemoteHost());
    }

    @Test
    void setAttribute() {}

    @Test
    void removeAttribute() {}

    @Test
    void getLocale() {
        assertEquals(Locale.CHINA, queryRequest.getLocale());
    }

    @Test
    void getLocales() {
        final Enumeration<Locale> locales = queryRequest.getLocales();
        assertEquals(Locale.CHINA, locales.nextElement());
        assertFalse(locales.hasMoreElements());
    }

    @Test
    void isSecure() {
        assertFalse(queryRequest.isSecure());
    }

    @Test
    void getRequestDispatcher() {}

    @Test
    void getRealPath() {}

    @Test
    void getRemotePort() {
        assertEquals(0, queryRequest.getRemotePort());
    }

    @Test
    void getLocalName() {
        assertNotNull(queryRequest.getLocalName());
    }

    @Test
    void getLocalAddr() {
        assertNotNull(queryRequest.getLocalAddr());
    }

    @Test
    void getLocalPort() {
        assertEquals(-1, queryRequest.getLocalPort());
    }

    @Test
    void getServletContext() {}

    @Test
    void startAsync() {}

    @Test
    void testStartAsync() {}

    @Test
    void isAsyncStarted() {}

    @Test
    void isAsyncSupported() {}

    @Test
    void getAsyncContext() {}

    @Test
    void getDispatcherType() {}

    @Test
    void getParseBody() {
        final JsonNode parseBody = jsonRequest.getParseBody();
        assertNotNull(parseBody);
        assertTrue(parseBody.isObject());
        assertEquals("value", parseBody.get("key").asText());
    }

    private static ByteBufferStream queryRequest() {
        return ByteBufferStream.wrap(
            (
                "GET /welcome?age=18&name=syfxlin HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Accept-Language: zh-CN\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "Accept: */*\r\n" +
                "Connection: keep-alive\r\n" +
                "Cookie: yummy_cookie=choco; tasty_cookie=strawberry\r\n" +
                "Content-Type: multipart/form-data; charset=utf-8; boundary=something\r\n" +
                "Content-Length: 0\r\n"
            ).getBytes(StandardCharsets.ISO_8859_1)
        );
    }

    private static ByteBufferStream bodyRequest() {
        return ByteBufferStream.wrap(
            (
                "POST /welcome HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Length: 19\r\n" +
                "Content-Type: application/x-www-form-urlencoded\r\n" +
                "\r\n" +
                "age=18&name=syfxlin\r\n"
            ).getBytes(StandardCharsets.ISO_8859_1)
        );
    }

    private static ByteBufferStream multiPartRequest() {
        return ByteBufferStream.wrap(
            (
                "POST / HTTP/1.1\r\n" +
                "Content-Type: multipart/form-data; boundary=WebAppBoundary\r\n" +
                "Content-Length: 351\r\n" +
                "Host: localhost:8080\r\n" +
                "Connection: Keep-Alive\r\n" +
                "User-Agent: Apache-HttpClient/4.5.12 (Java/11.0.8)\r\n" +
                "Accept-Encoding: gzip,deflate\r\n" +
                "\r\n" +
                "--WebAppBoundary\r\n" +
                "Content-Disposition: form-data; name=\"element-name\"\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Transfer-Encoding: 8bit\r\n" +
                "\r\n" +
                "Name\r\n" +
                "--WebAppBoundary\r\n" +
                "Content-Disposition: form-data; name=\"data\"; filename=\"data.json\"\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Transfer-Encoding: binary\r\n" +
                "\r\n" +
                "{\n" +
                "  \"id\": 999,\n" +
                "  \"value\": \"content\"\n" +
                "}\r\n" +
                "--WebAppBoundary--"
            ).getBytes(StandardCharsets.ISO_8859_1)
        );
    }

    private static ByteBufferStream jsonRequest() {
        return ByteBufferStream.wrap(
            (
                "POST /method HTTP/1.1\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 20\r\n" +
                "Host: localhost:8080\r\n" +
                "Connection: Keep-Alive\r\n" +
                "User-Agent: Apache-HttpClient/4.5.12 (Java/11.0.8)\r\n" +
                "Accept-Encoding: gzip,deflate\r\n" +
                "\r\n" +
                "{\n" +
                "  \"key\": \"value\"\n" +
                "}"
            ).getBytes(StandardCharsets.ISO_8859_1)
        );
    }

    private void assertCookies(final Request request) {
        final Map<String, Cookie> cookieMap = Arrays
            .stream(request.getCookies())
            .collect(Collectors.toMap(Cookie::getName, e -> e));
        final Cookie c1 = cookieMap.get("yummy_cookie");
        final Cookie c2 = cookieMap.get("tasty_cookie");
        if (
            c1 == null ||
            c2 == null ||
            !"choco".equals(c1.getValue()) ||
            !"strawberry".equals(c2.getValue())
        ) {
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
        return new HttpChannel(
            new Connector() {

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
            },
            null,
            key
        );
    }
}
