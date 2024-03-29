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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import me.ixk.xkserver.http.HttpParser.RequestHandler;
import me.ixk.xkserver.io.ByteBufferPool;
import me.ixk.xkserver.io.ByteBufferStream;
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
        parser.end();
        this.assertStartLine(handler);
    }

    @Test
    void parseHeaders() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.link(this.startLine(), this.fixedContentHeaders()));
        parser.end();
        this.assertHeaders(handler);
    }

    @Test
    void parseFixedContent() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(
            this.link(
                    this.startLine(),
                    this.fixedContentHeaders(),
                    this.fixedContent()
                )
        );
        parser.end();
        this.assertStartLine(handler);
        this.assertHeaders(handler);
        this.assertContent(handler, this.fixedContent());
    }

    @Test
    void parseEmptyContent() {
        final RequestHandlerImpl h1 = new RequestHandlerImpl();
        final HttpParser p1 = new HttpParser(h1);
        p1.parse(this.link(this.startLine(), this.emptyContentHeaders()));
        p1.end();
        this.assertContent(h1, ByteBufferStream.wrap(new byte[0]));

        final RequestHandlerImpl h2 = new RequestHandlerImpl();
        final HttpParser p2 = new HttpParser(h2);
        p2.parse(this.link(this.startLine(), this.emptyContentHeaders()));
        p2.end();
        this.assertContent(h2, ByteBufferStream.wrap(new byte[0]));
    }

    @Test
    void parseChunkContent() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(
            this.link(
                    this.startLine(),
                    this.chunkContentHeaders(),
                    this.chunkContent()
                )
        );
        parser.end();
        this.assertContent(handler, this.wrap("MozillaDeveloperNetwork"));
    }

    @Test
    void parseTrailer() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(
            this.link(
                    this.startLine(),
                    this.trailerContentHeaders(),
                    this.trailerContent()
                )
        );
        parser.end();
        this.assertContent(handler, this.wrap("MozillaDeveloperNetwork"));
        assertTrue(handler.getHttpFields().containsKey("Expires"));
    }

    @Test
    void multipleInput() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.startLine());
        parser.parse(this.fixedContentHeaders());
        parser.end();
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
        parser.end();
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
        parser.end();
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
        parser.parse(this.wrap("0\r\n\r\n"));
        parser.end();
        this.assertContent(handler, this.wrap("MozillaDeveloperNetwork"));
        assertThrows(
            BadMessageException.class,
            () -> {
                parser.parse(this.wrap("123"));
            }
        );
    }

    @Test
    void fixedEnd() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        parser.parse(this.startLine());
        parser.parse(this.fixedContentHeaders());
        parser.parse(this.wrap("name"));
        parser.parse(this.wrap("="));
        parser.parse(this.wrap("syfxlin\r\n"));
        parser.end();
        this.assertContent(handler, this.fixedContent());
        assertThrows(
            BadMessageException.class,
            () -> parser.parse(this.wrap("123"))
        );
    }

    @Test
    void splitFixedInput() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        final ByteBufferStream buffer =
            this.link(
                    this.startLine(),
                    this.fixedContentHeaders(),
                    this.fixedContent()
                );
        Arrays
            .stream(
                IoUtil
                    .read(buffer.getInputStream(), StandardCharsets.ISO_8859_1)
                    .split("")
            )
            .map(String::getBytes)
            .map(ByteBufferStream::wrap)
            .forEach(parser::parse);
        parser.end();
        this.assertStartLine(handler);
        this.assertHeaders(handler);
        this.assertContent(handler, this.fixedContent());
    }

    @Test
    void splitChunkInput() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        final ByteBufferStream buffer =
            this.link(
                    this.startLine(),
                    this.chunkContentHeaders(),
                    this.chunkContent()
                );
        Arrays
            .stream(
                IoUtil
                    .read(buffer.getInputStream(), StandardCharsets.ISO_8859_1)
                    .split("")
            )
            .map(String::getBytes)
            .map(ByteBufferStream::wrap)
            .forEach(parser::parse);
        parser.end();
        this.assertStartLine(handler);
        this.assertHeaders(handler);
        this.assertContent(handler, this.wrap("MozillaDeveloperNetwork"));
    }

    @Test
    void splitTrailerInput() {
        final RequestHandlerImpl handler = new RequestHandlerImpl();
        final HttpParser parser = new HttpParser(handler);
        final ByteBufferStream buffer =
            this.link(
                    this.startLine(),
                    this.trailerContentHeaders(),
                    this.trailerContent()
                );
        Arrays
            .stream(
                IoUtil
                    .read(buffer.getInputStream(), StandardCharsets.ISO_8859_1)
                    .split("")
            )
            .map(String::getBytes)
            .map(ByteBufferStream::wrap)
            .forEach(parser::parse);
        parser.end();
        this.assertContent(handler, this.wrap("MozillaDeveloperNetwork"));
        assertTrue(handler.getHttpFields().containsKey("Expires"));
    }

    private void assertStartLine(final RequestHandlerImpl handler) {
        assertEquals(HttpMethod.GET, handler.getHttpMethod());
        assertEquals("/url", handler.getHttpUri().asString());
        assertEquals(HttpVersion.HTTP_1_1, handler.getHttpVersion());
    }

    private void assertHeaders(final RequestHandlerImpl handler) {
        final Map<String, String> headers = Map.of(
            "Host",
            "ixk.me",
            "Accept",
            "text/html",
            "Accept-Encoding",
            "gzip, deflate, br"
        );
        final Map<String, HttpField> parserHeaders = handler.getHttpFields();
        for (final Entry<String, String> entry : headers.entrySet()) {
            final List<String> list = parserHeaders
                .get(entry.getKey())
                .getValues();
            if (list == null || list.isEmpty()) {
                fail("Header [" + entry.getKey() + "] not exist");
            }
            assertEquals(entry.getValue(), list.get(0));
        }
    }

    private void assertContent(
        final RequestHandlerImpl handler,
        final ByteBufferStream buffer
    ) {
        final String content = IoUtil
            .getUtf8Reader(handler.getHttpInput())
            .lines()
            .collect(Collectors.joining("\r\n"));
        assertEquals(this.str(buffer).trim(), content.trim());
    }

    private ByteBufferStream startLine() {
        return this.wrap("GET /url HTTP/1.1\r\n");
    }

    private ByteBufferStream fixedContentHeaders() {
        return this.wrap(
                "Host: ixk.me\r\n" +
                "Accept: text/html\r\n" +
                "Accept-Encoding: gzip, deflate, br\r\n" +
                "Content-Length: " +
                this.fixedContent().remaining() +
                "\r\n" +
                "\r\n"
            );
    }

    private ByteBufferStream fixedContent() {
        return this.wrap("name=syfxlin\r\n");
    }

    private ByteBufferStream chunkContentHeaders() {
        return this.wrap(
                "Host: ixk.me\r\n" +
                "Accept: text/html\r\n" +
                "Accept-Encoding: gzip, deflate, br\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "\r\n"
            );
    }

    private ByteBufferStream chunkContent() {
        return this.wrap(
                "7\r\n" +
                "Mozilla\r\n" +
                "9\r\n" +
                "Developer\r\n" +
                "7\r\n" +
                "Network\r\n" +
                "0\r\n" +
                "\r\n"
            );
    }

    private ByteBufferStream trailerContentHeaders() {
        return this.wrap(
                "Host: ixk.me\r\n" +
                "Accept: text/html\r\n" +
                "Accept-Encoding: gzip, deflate, br\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "Trailer: Expires\r\n" +
                "\r\n"
            );
    }

    private ByteBufferStream trailerContent() {
        return this.wrap(
                "7\r\n" +
                "Mozilla\r\n" +
                "9\r\n" +
                "Developer\r\n" +
                "7\r\n" +
                "Network\r\n" +
                "0\r\n" +
                "Expires: Wed, 21 Oct 2015 07:28:00 GMT\r\n" +
                "\r\n"
            );
    }

    private ByteBufferStream emptyContentHeaders() {
        return this.wrap("Content-Length: 0\r\n\r\n");
    }

    private ByteBufferStream wrap(final String string) {
        return new ByteBufferStream(
            ByteBuffer.wrap(string.getBytes(StandardCharsets.ISO_8859_1))
        );
    }

    private ByteBufferStream link(final ByteBufferStream... buffers) {
        return this.wrap(
                Arrays
                    .stream(buffers)
                    .map(
                        b ->
                            IoUtil.read(
                                b.getInputStream(),
                                StandardCharsets.ISO_8859_1
                            )
                    )
                    .collect(Collectors.joining())
            );
    }

    private String str(final ByteBufferStream buffer) {
        return IoUtil.read(
            buffer.getInputStream(),
            StandardCharsets.ISO_8859_1
        );
    }

    private static class RequestHandlerImpl implements RequestHandler {
        private HttpMethod httpMethod;
        private HttpUri httpUri;
        private HttpVersion httpVersion;
        private final HttpFields httpFields = new HttpFields();
        private final HttpInput httpInput = new HttpInput();

        @Override
        public ByteBufferPool bufferPool() {
            return ByteBufferPool.defaultPool();
        }

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
        public void addContent(final ByteBufferStream buffer) {
            this.httpInput.writeBuffer(buffer);
        }

        @Override
        public void requestComplete() {
            this.httpInput.flip();
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
