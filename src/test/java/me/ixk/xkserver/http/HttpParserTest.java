/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * @author Otstar Lin
 * @date 2020/10/23 下午 12:25
 */
class HttpParserTest {

    @Test
    void parseStartLine() {
        final HttpParser parser = new HttpParser();
        parser.parse(this.link(this.startLine(), this.fixedContentHeaders()));
        this.assertStartLine(parser);
    }

    @Test
    void parseHeaders() {
        final HttpParser parser = new HttpParser();
        parser.parse(this.link(this.startLine(), this.fixedContentHeaders()));
        this.assertHeaders(parser);
    }

    @Test
    void parseFixedContent() {
        final HttpParser parser = new HttpParser();
        parser.parse(
            this.link(
                    this.startLine(),
                    this.fixedContentHeaders(),
                    this.fixedContent()
                )
        );
        this.assertStartLine(parser);
        this.assertHeaders(parser);
        this.assertContent(parser, this.fixedContent());
    }

    @Test
    void parseEmptyContent() {
        final HttpParser p1 = new HttpParser();
        p1.parse(this.link(this.startLine(), this.emptyContentHeaders()));
        this.assertContent(p1, ByteBuffer.allocate(0));

        final HttpParser p2 = new HttpParser();
        p2.parse(this.link(this.startLine(), this.emptyContentHeaders()));
        this.assertContent(p2, ByteBuffer.allocate(0));
    }

    @Test
    void parseChunkContent() {
        final HttpParser parser = new HttpParser();
        parser.parse(
            this.link(
                    this.startLine(),
                    this.chunkContentHeaders(),
                    this.chunkContent()
                )
        );
        this.assertContent(parser, this.wrap("MozillaDeveloperNetwork"));
    }

    @Test
    void parseTrailer() {
        final HttpParser parser = new HttpParser();
        parser.parse(
            this.link(
                    this.startLine(),
                    this.trailerContentHeaders(),
                    this.trailerContent()
                )
        );
        this.assertContent(parser, this.wrap("MozillaDeveloperNetwork"));
        assertTrue(parser.getHeaders().containsKey("Expires"));
    }

    @Test
    void multipleInput() {
        final HttpParser parser = new HttpParser();
        parser.parse(this.startLine());
        parser.parse(this.fixedContentHeaders());
        this.assertStartLine(parser);
        this.assertHeaders(parser);
    }

    @Test
    void multipleFixedInput() {
        final HttpParser parser = new HttpParser();
        parser.parse(this.startLine());
        parser.parse(this.fixedContentHeaders());
        parser.parse(this.wrap("name"));
        parser.parse(this.wrap("="));
        parser.parse(this.wrap("syfxlin\r\n"));
        this.assertContent(parser, this.fixedContent());
    }

    @Test
    void multipleChunkInput() {
        final HttpParser parser = new HttpParser();
        parser.parse(this.startLine());
        parser.parse(this.chunkContentHeaders());
        parser.parse(this.wrap("7\r\nMozilla\r\n"));
        parser.parse(this.wrap("9\r\nDeveloper\r\n"));
        parser.parse(this.wrap("7\r\nNetwork\r\n"));
        parser.parse(this.wrap("0\r\n\r\n"));
        this.assertContent(parser, this.wrap("MozillaDeveloperNetwork"));
    }

    @Test
    void chunkEnd() {
        final HttpParser parser = new HttpParser();
        parser.parse(this.startLine());
        parser.parse(this.chunkContentHeaders());
        parser.parse(this.wrap("7\r\nMozilla\r\n"));
        parser.parse(this.wrap("9\r\nDeveloper\r\n"));
        parser.parse(this.wrap("7\r\nNetwork\r\n"));
        parser.setEof(true);
        parser.parse(this.wrap("0\r\n\r\n"));
        this.assertContent(parser, this.wrap("MozillaDeveloperNetwork"));
        assertThrows(
            IllegalStateException.class,
            () -> {
                parser.parse(this.wrap("123"));
            }
        );
    }

    @Test
    void fixedEnd() {
        final HttpParser parser = new HttpParser();
        parser.parse(this.startLine());
        parser.parse(this.fixedContentHeaders());
        parser.parse(this.wrap("name"));
        parser.parse(this.wrap("="));
        parser.setEof(true);
        parser.parse(this.wrap("syfxlin\r\n"));
        this.assertContent(parser, this.fixedContent());
        assertThrows(
            IllegalStateException.class,
            () -> parser.parse(this.wrap("123"))
        );
    }

    @Test
    void splitFixedInput() {
        final HttpParser parser = new HttpParser();
        final ByteBuffer buffer =
            this.link(
                    this.startLine(),
                    this.fixedContentHeaders(),
                    this.fixedContent()
                );
        Arrays
            .stream(StrUtil.str(buffer, StandardCharsets.ISO_8859_1).split(""))
            .map(String::getBytes)
            .map(ByteBuffer::wrap)
            .forEach(parser::parse);
        this.assertStartLine(parser);
        this.assertHeaders(parser);
        this.assertContent(parser, this.fixedContent());
    }

    @Test
    void splitChunkInput() {
        final HttpParser parser = new HttpParser();
        final ByteBuffer buffer =
            this.link(
                    this.startLine(),
                    this.chunkContentHeaders(),
                    this.chunkContent()
                );
        Arrays
            .stream(StrUtil.str(buffer, StandardCharsets.ISO_8859_1).split(""))
            .map(String::getBytes)
            .map(ByteBuffer::wrap)
            .forEach(parser::parse);
        this.assertStartLine(parser);
        this.assertHeaders(parser);
        this.assertContent(parser, this.wrap("MozillaDeveloperNetwork"));
    }

    @Test
    void splitTrailerInput() {
        final HttpParser parser = new HttpParser();
        final ByteBuffer buffer =
            this.link(
                    this.startLine(),
                    this.trailerContentHeaders(),
                    this.trailerContent()
                );
        Arrays
            .stream(StrUtil.str(buffer, StandardCharsets.ISO_8859_1).split(""))
            .map(String::getBytes)
            .map(ByteBuffer::wrap)
            .forEach(parser::parse);
        this.assertContent(parser, this.wrap("MozillaDeveloperNetwork"));
        assertTrue(parser.getHeaders().containsKey("Expires"));
    }

    private void assertStartLine(final HttpParser parser) {
        assertEquals(HttpMethod.GET, parser.getMethod());
        assertEquals("/url", parser.getUri());
        assertEquals(HttpVersion.HTTP_1_1, parser.getVersion());
    }

    private void assertHeaders(final HttpParser parser) {
        final Map<String, String> headers = Map.of(
            "Host",
            "ixk.me",
            "Accept",
            "text/html",
            "Accept-Encoding",
            "gzip, deflate, br"
        );
        final Map<String, List<String>> parserHeaders = parser.getHeaders();
        for (final Entry<String, String> entry : headers.entrySet()) {
            final List<String> list = parserHeaders.get(entry.getKey());
            if (list == null || list.isEmpty()) {
                throw new AssertionFailedError(
                    "Header [" + entry.getKey() + "] not exist"
                );
            }
            assertEquals(entry.getValue(), list.get(0));
        }
    }

    private void assertContent(
        final HttpParser parser,
        final ByteBuffer buffer
    ) {
        final String content = IoUtil
            .getUtf8Reader(parser.getHttpInput())
            .lines()
            .collect(Collectors.joining("\r\n"));
        assertEquals(this.str(buffer).trim(), content.trim());
    }

    private ByteBuffer startLine() {
        return this.wrap("GET /url HTTP/1.1\r\n");
    }

    private ByteBuffer fixedContentHeaders() {
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

    private ByteBuffer fixedContent() {
        return this.wrap("name=syfxlin\r\n");
    }

    private ByteBuffer chunkContentHeaders() {
        return this.wrap(
                "Host: ixk.me\r\n" +
                "Accept: text/html\r\n" +
                "Accept-Encoding: gzip, deflate, br\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "\r\n"
            );
    }

    private ByteBuffer chunkContent() {
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

    private ByteBuffer trailerContentHeaders() {
        return this.wrap(
                "Host: ixk.me\r\n" +
                "Accept: text/html\r\n" +
                "Accept-Encoding: gzip, deflate, br\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "Trailer: Expires\r\n" +
                "\r\n"
            );
    }

    private ByteBuffer trailerContent() {
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

    private ByteBuffer emptyContentHeaders() {
        return this.wrap("Content-Length: 0\r\n\r\n");
    }

    private ByteBuffer wrap(final String string) {
        return ByteBuffer.wrap(string.getBytes(StandardCharsets.ISO_8859_1));
    }

    private ByteBuffer link(final ByteBuffer... buffers) {
        return this.wrap(
                Arrays
                    .stream(buffers)
                    .map(ByteBuffer::array)
                    .map(String::new)
                    .collect(Collectors.joining())
            );
    }

    private String str(final ByteBuffer buffer) {
        return StrUtil.str(buffer, StandardCharsets.ISO_8859_1);
    }
}
