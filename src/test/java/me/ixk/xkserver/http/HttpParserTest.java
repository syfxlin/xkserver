/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import me.ixk.xkserver.http.HttpParser.RequestHandler;
import org.junit.jupiter.api.Test;

/**
 * @author Otstar Lin
 * @date 2020/10/23 下午 12:25
 */
class HttpParserTest {

    @Test
    void parseStartLine() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.link(this.startLine(), this.fixedContentHeaders()));
        this.assertStartLine(handler);
    }

    @Test
    void parseHeaders() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.link(this.startLine(), this.fixedContentHeaders()));
        this.assertHeaders(handler);
    }

    @Test
    void parseFixedContent() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.link(this.startLine(), this.fixedContentHeaders(),
            this.fixedContent()));
        this.assertStartLine(handler);
        this.assertHeaders(handler);
        this.assertContent(handler, this.fixedContent());
    }

    @Test
    void parseEmptyContent() {
        final RequestHandlerImpl h1 = new RequestHandlerImpl();
        final HttpParser p1 = new HttpParser(h1);
        p1.parse(this.link(this.startLine(), this.emptyContentHeaders()));
        this.assertContent(h1, ByteBuffer.allocate(0));

        final RequestHandlerImpl h2 = new RequestHandlerImpl();
        final HttpParser p2 = new HttpParser(h2);
        p2.parse(this.link(this.startLine(), this.emptyContentHeaders()));
        this.assertContent(h2, ByteBuffer.allocate(0));
    }

    @Test
    void parseChunkContent() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.link(this.startLine(), this.chunkContentHeaders(),
            this.chunkContent()));
        this.assertContent(handler, this.wrap("MozillaDeveloperNetwork"));
    }

    @Test
    void parseTrailer() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.link(this.startLine(), this.trailerContentHeaders(),
            this.trailerContent()));
        this.assertContent(handler, this.wrap("MozillaDeveloperNetwork"));
        assertTrue(handler.getHttpFields().containsKey("Expires"));
    }

    @Test
    void multipleInput() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.startLine());
        parser.parse(this.fixedContentHeaders());
        this.assertStartLine(handler);
        this.assertHeaders(handler);
    }

    @Test
    void multipleFixedInput() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.startLine());
        parser.parse(this.fixedContentHeaders());
        parser.parse(this.wrap("name"));
        parser.parse(this.wrap("="));
        parser.parse(this.wrap("syfxlin\r\n"));
        this.assertContent(handler, this.fixedContent());
    }

    @Test
    void multipleChunkInput() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.startLine());
        parser.parse(this.chunkContentHeaders());
        parser.parse(this.wrap("7\r\nMozilla\r\n"));
        parser.parse(this.wrap("9\r\nDeveloper\r\n"));
        parser.parse(this.wrap("7\r\nNetwork\r\n"));
        parser.parse(this.wrap("0\r\n\r\n"));
        this.assertContent(handler, this.wrap("MozillaDeveloperNetwork"));
    }

    @Test
    void chunkEnd() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.startLine());
        parser.parse(this.chunkContentHeaders());
        parser.parse(this.wrap("7\r\nMozilla\r\n"));
        parser.parse(this.wrap("9\r\nDeveloper\r\n"));
        parser.parse(this.wrap("7\r\nNetwork\r\n"));
        parser.setEof(true);
        parser.parse(this.wrap("0\r\n\r\n"));
        this.assertContent(handler, this.wrap("MozillaDeveloperNetwork"));
        assertThrows(BadMessageException.class, () -> {
            parser.parse(this.wrap("123"));
        });
    }

    @Test
    void fixedEnd() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.startLine());
        parser.parse(this.fixedContentHeaders());
        parser.parse(this.wrap("name"));
        parser.parse(this.wrap("="));
        parser.setEof(true);
        parser.parse(this.wrap("syfxlin\r\n"));
        this.assertContent(handler, this.fixedContent());
        assertThrows(BadMessageException.class,
            () -> parser.parse(this.wrap("123")));
    }

    @Test
    void splitFixedInput() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        final ByteBuffer buffer = this
            .link(this.startLine(), this.fixedContentHeaders(),
                this.fixedContent());
        Arrays
            .stream(StrUtil.str(buffer, StandardCharsets.ISO_8859_1).split(""))
            .map(String::getBytes).map(ByteBuffer::wrap).forEach(parser::parse);
        this.assertStartLine(handler);
        this.assertHeaders(handler);
        this.assertContent(handler, this.fixedContent());
    }

    @Test
    void splitChunkInput() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        final ByteBuffer buffer = this
            .link(this.startLine(), this.chunkContentHeaders(),
                this.chunkContent());
        Arrays
            .stream(StrUtil.str(buffer, StandardCharsets.ISO_8859_1).split(""))
            .map(String::getBytes).map(ByteBuffer::wrap).forEach(parser::parse);
        this.assertStartLine(handler);
        this.assertHeaders(handler);
        this.assertContent(handler, this.wrap("MozillaDeveloperNetwork"));
    }

    @Test
    void splitTrailerInput() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        final ByteBuffer buffer = this
            .link(this.startLine(), this.trailerContentHeaders(),
                this.trailerContent());
        Arrays
            .stream(StrUtil.str(buffer, StandardCharsets.ISO_8859_1).split(""))
            .map(String::getBytes).map(ByteBuffer::wrap).forEach(parser::parse);
        this.assertContent(handler, this.wrap("MozillaDeveloperNetwork"));
        assertTrue(handler.getHttpFields().containsKey("Expires"));
    }

    private void assertStartLine(final RequestHandlerImpl handler) {
        assertEquals(HttpMethod.GET, handler.getHttpMethod());
        assertEquals("/url", handler.getHttpUri().asString());
        assertEquals(HttpVersion.HTTP_1_1, handler.getHttpVersion());
    }

    private void assertHeaders(final RequestHandlerImpl handler) {
        final Map<String, String> headers = Map
            .of("Host", "ixk.me", "Accept", "text/html", "Accept-Encoding",
                "gzip, deflate, br");
        final Map<String, HttpField> parserHeaders = handler.getHttpFields();
        for (final Entry<String, String> entry : headers.entrySet()) {
            final List<String> list = parserHeaders.get(entry.getKey())
                                                   .getValues();
            if (list == null || list.isEmpty()) {
                fail("Header [" + entry.getKey() + "] not exist");
            }
            assertEquals(entry.getValue(), list.get(0));
        }
    }

    private void assertContent(final RequestHandlerImpl handler,
        final ByteBuffer buffer) {
        final String content = IoUtil.getUtf8Reader(handler.getHttpInput())
                                     .lines()
                                     .collect(Collectors.joining("\r\n"));
        assertEquals(this.str(buffer).trim(), content.trim());
    }

    private ByteBuffer startLine() {
        return this.wrap("GET /url HTTP/1.1\r\n");
    }

    private ByteBuffer fixedContentHeaders() {
        return this.wrap("Host: ixk.me\r\n" + "Accept: text/html\r\n"
            + "Accept-Encoding: gzip, deflate, br\r\n" + "Content-Length: "
            + this.fixedContent().remaining() + "\r\n" + "\r\n");
    }

    private ByteBuffer fixedContent() {
        return this.wrap("name=syfxlin\r\n");
    }

    private ByteBuffer chunkContentHeaders() {
        return this.wrap("Host: ixk.me\r\n" + "Accept: text/html\r\n"
            + "Accept-Encoding: gzip, deflate, br\r\n"
            + "Transfer-Encoding: chunked\r\n" + "\r\n");
    }

    private ByteBuffer chunkContent() {
        return this.wrap(
            "7\r\n" + "Mozilla\r\n" + "9\r\n" + "Developer\r\n" + "7\r\n"
                + "Network\r\n" + "0\r\n" + "\r\n");
    }

    private ByteBuffer trailerContentHeaders() {
        return this.wrap("Host: ixk.me\r\n" + "Accept: text/html\r\n"
            + "Accept-Encoding: gzip, deflate, br\r\n"
            + "Transfer-Encoding: chunked\r\n" + "Trailer: Expires\r\n"
            + "\r\n");
    }

    private ByteBuffer trailerContent() {
        return this.wrap(
            "7\r\n" + "Mozilla\r\n" + "9\r\n" + "Developer\r\n" + "7\r\n"
                + "Network\r\n" + "0\r\n"
                + "Expires: Wed, 21 Oct 2015 07:28:00 GMT\r\n" + "\r\n");
    }

    private ByteBuffer emptyContentHeaders() {
        return this.wrap("Content-Length: 0\r\n\r\n");
    }

    private ByteBuffer wrap(final String string) {
        return ByteBuffer.wrap(string.getBytes(StandardCharsets.ISO_8859_1));
    }

    private ByteBuffer link(final ByteBuffer... buffers) {
        return this.wrap(
            Arrays.stream(buffers).map(ByteBuffer::array).map(String::new)
                  .collect(Collectors.joining()));
    }

    private String str(final ByteBuffer buffer) {
        return StrUtil.str(buffer, StandardCharsets.ISO_8859_1);
    }

    private static class RequestHandlerImpl implements RequestHandler {

        private HttpMethod httpMethod;
        private HttpUri httpUri;
        private HttpVersion httpVersion;
        private final HttpFields httpFields = new HttpFields();
        private final HttpInput httpInput = new HttpInput();

        @Override
        public void setHttpMethod(final HttpMethod method) {
            this.httpMethod = method;
        }

        @Override
        public void setHttpUri(final HttpUri uri) {
            this.httpUri = uri;
        }

        @Override
        public void setHttpVersion(final HttpVersion version) {
            this.httpVersion = version;
        }

        @Override
        public HttpField getHttpField(final String name) {
            return this.httpFields.get(name);
        }

        @Override
        public void addHttpHeader(final HttpField field) {
            this.httpFields.put(field);
        }

        @Override
        public void addHttpTrailer(final HttpField field) {
            this.httpFields.put(field);
        }

        @Override
        public void addContent(final ByteBuffer buffer) {
            this.httpInput.addBuffer(buffer);
        }

        public HttpMethod getHttpMethod() {
            return httpMethod;
        }

        public HttpUri getHttpUri() {
            return httpUri;
        }

        public HttpVersion getHttpVersion() {
            return httpVersion;
        }

        public HttpFields getHttpFields() {
            return httpFields;
        }

        public HttpInput getHttpInput() {
            return httpInput;
        }
    }
}
