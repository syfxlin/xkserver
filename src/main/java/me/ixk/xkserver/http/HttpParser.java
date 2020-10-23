/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.ixk.xkserver.http.HttpHeader.Value;
import me.ixk.xkserver.http.HttpTokens.Type;

/**
 * Http 报文解析
 *
 * @author Otstar Lin
 * @date 2020/10/22 下午 9:10
 */
public class HttpParser {
    private static final String CONTENT_LENGTH = "content-length";
    private static final String TRANSFER_ENCODING = "transfer-encoding";

    public enum State {
        /**
         * 开始
         */
        START_LINE,
        /**
         * 方法
         */
        METHOD,
        /**
         * 请求行第一个空格
         */
        SPACE1,
        /**
         * 路径
         */
        URI,
        /**
         * 请求行第二个空格
         */
        SPACE2,
        /**
         * 请求版本
         */
        VERSION,
        /**
         * 头字段
         */
        START_HEADER,
        /**
         * 头字段名称
         */
        HEADER_NAME,
        /**
         * 头字段分隔符
         */
        HEADER_COLON,
        /**
         * 头字段值
         */
        HEADER_VALUE,
        /**
         * 内容
         */
        START_CONTENT,
        /**
         * 空内容
         */
        EMPTY_CONTENT,
        /**
         * 定长内容
         */
        FIXED_CONTENT,
        /**
         * 分块内容
         */
        CHUNKED_CONTENT,
        /**
         * Chunk 大小
         */
        CHUNK_SIZE,
        /**
         * Chunk 内容
         */
        CHUNK_CONTENT,
        /**
         * 结束内容
         */
        END_CONTENT,
        /**
         * 尾字段
         */
        TRAILER,
    }

    private State state = State.START_LINE;
    private HttpMethod method;
    private final StringBuilder uri;
    private HttpVersion version;
    private final Map<String, List<String>> headers;
    private int contentLength = -1;
    private String transferEncoding = null;
    private int chunkLength = 0;
    private final List<ByteBuffer> contents;
    private boolean hasCr = false;
    private final int maxHeaderByteLength = -1;

    private final StringBuilder string = new StringBuilder();
    private final StringBuilder value = new StringBuilder();
    private int length = 0;

    public HttpParser() {
        this.method = HttpMethod.GET;
        this.uri = new StringBuilder();
        this.version = HttpVersion.HTTP_1_1;
        this.headers = new ConcurrentHashMap<>();
        this.contents = new ArrayList<>();
    }

    public void parse(final ByteBuffer buffer) {
        if (this.state == State.START_LINE) {
            this.state = State.METHOD;
        }
        // 请求行 "Method Url Version" "GET /url HTTP/1.1"
        this.parseLine(buffer);
        // 头字段 "Name: Value" "Host: ixk.me"
        this.parseHeaders(buffer);
        // 解析内容
        this.parseContent(buffer);
    }

    private void parseLine(final ByteBuffer buffer) {
        if (
            this.state.ordinal() >= State.START_HEADER.ordinal() ||
            !buffer.hasRemaining()
        ) {
            return;
        }
        this.length = 0;
        if (this.state == State.METHOD) {
            this.method = HttpMethod.bytesToMethod(buffer);
            if (this.method == null) {
                throw new BadMessageException();
            }
            buffer.position(
                buffer.position() + this.method.asString().length() + 1
            );
            this.length += this.method.asString().length();
            this.setState(State.SPACE1);
        }

        while (buffer.hasRemaining()) {
            final HttpTokens.Token token = this.next(buffer);
            if (token == null) {
                break;
            }
            this.length++;
            if (
                this.maxHeaderByteLength > 0 &&
                this.length > this.maxHeaderByteLength
            ) {
                if (this.state == State.URI) {
                    throw new BadMessageException(HttpStatus.URI_TOO_LONG);
                } else {
                    throw new BadMessageException(
                        HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE
                    );
                }
            }

            final HttpTokens.Type type = token.getType();
            final char ch = token.getChar();

            switch (this.state) {
                case SPACE1:
                    switch (type) {
                        // 空格跳过
                        case SPACE:
                            break;
                        // 不是空格则切换到 URL 状态，URL 包含字母，符号，数字等
                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                            this.uri.setLength(0);
                            this.setState(State.URI);
                            this.uri.append(ch);
                            break;
                        default:
                            throw new BadMessageException("Not URI");
                    }
                    break;
                case URI:
                    switch (type) {
                        // URL 包含除换行，空格等一些控制字符外字符
                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                        case OTEXT:
                            this.uri.append(ch);
                            break;
                        // 遇到空格则说明 URL 部分结束，转换到第二个空格状态
                        case SPACE:
                            this.setState(State.SPACE2);
                            break;
                        default:
                            throw new IllegalCharacterException(token);
                    }
                    break;
                case SPACE2:
                    switch (type) {
                        // 遇到空格跳过
                        case SPACE:
                            break;
                        // 遇到字母则切换到 HTTP 版本状态
                        case ALPHA:
                            this.string.setLength(0);
                            this.string.append(ch);
                            this.setState(State.VERSION);
                            break;
                        default:
                            throw new IllegalCharacterException(token);
                    }
                    break;
                case VERSION:
                    switch (type) {
                        // HTTP 版本包含字符
                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                            this.string.append(ch);
                            break;
                        // 遇到换行说明请求行结束，此时验证 HTTP 版本字段是否符合规范，如果是，则结束请求行的解析，同时切换到解析头字段的状态
                        case LF:
                            this.version =
                                HttpVersion.from(this.string.toString());
                            if (this.version == null) {
                                throw new BadMessageException(
                                    HttpStatus.HTTP_VERSION_NOT_SUPPORTED,
                                    "Unknown Version"
                                );
                            }
                            this.length = 0;
                            this.setState(State.START_HEADER);
                            return;
                        default:
                            throw new IllegalCharacterException(token);
                    }
                    break;
                default:
                    throw new IllegalStateException(this.state.toString());
            }
        }
    }

    private void parseHeaders(final ByteBuffer buffer) {
        // TODO: 处理 TRAILER
        if (
            this.state.ordinal() >= State.START_CONTENT.ordinal() ||
            !buffer.hasRemaining()
        ) {
            return;
        }
        this.length = 0;
        final State state = this.state;

        while (buffer.hasRemaining()) {
            final HttpTokens.Token token = this.next(buffer);
            if (token == null) {
                break;
            }
            this.length++;
            if (
                this.maxHeaderByteLength > 0 &&
                this.length > this.maxHeaderByteLength
            ) {
                throw new BadMessageException(
                    state == State.START_HEADER
                        ? HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE
                        : HttpStatus.PAYLOAD_TOO_LARGE
                );
            }

            final HttpTokens.Type type = token.getType();
            final char ch = token.getChar();

            switch (this.state) {
                case START_HEADER:
                case TRAILER:
                    // HEADER 状态的下一个状态一定是 HEADER_NAME，头字段的名称首字符一定为字母
                    switch (type) {
                        case ALPHA:
                            this.setState(State.HEADER_NAME);
                            this.string.setLength(0);
                            this.string.append(ch);
                            this.value.setLength(0);
                            this.length = 1;
                            break;
                        // 头字段部分结束，进入 CONTENT 部分
                        case LF:
                            this.setState(State.START_CONTENT);
                            return;
                        default:
                            throw new IllegalCharacterException(token);
                    }
                    break;
                case HEADER_NAME:
                    switch (type) {
                        // 继续拼接 header name
                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                            this.string.append(ch);
                            break;
                        // 分隔符，切换成头字段分隔符状态
                        case COLON:
                            this.setState(State.HEADER_COLON);
                            break;
                        // 遇到换行，说明这是一个无值的头字段
                        case LF:
                            this.addHeader(this.string, this.value, state);
                            this.length = 0;
                            break;
                        default:
                            throw new IllegalCharacterException(token);
                    }
                    break;
                case HEADER_COLON:
                    switch (type) {
                        // 所有空白字符都跳过
                        case SPACE:
                        case HTAB:
                            break;
                        // 转换到 header value
                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                        case OTEXT:
                            this.setState(State.HEADER_VALUE);
                            this.value.append(ch);
                            break;
                        // 遇到换行，说明这是一个无值的头字段
                        case LF:
                            this.addHeader(this.string, this.value, state);
                            this.length = 0;
                            break;
                        default:
                            throw new IllegalCharacterException(token);
                    }
                    break;
                case HEADER_VALUE:
                    switch (type) {
                        // 继续拼接 header value
                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                        case OTEXT:
                        case SPACE:
                        case HTAB:
                            this.value.append(ch);
                            break;
                        // 遇到换行，说明当前的 header 已经结束
                        case LF:
                            this.addHeader(this.string, this.value, state);
                            this.length = 0;
                            break;
                        default:
                            throw new IllegalCharacterException(token);
                    }
                    break;
                default:
                    throw new IllegalStateException(this.state.toString());
            }
        }
    }

    private void parseContent(final ByteBuffer buffer) {
        if (
            this.state.ordinal() >= State.TRAILER.ordinal() ||
            !buffer.hasRemaining()
        ) {
            return;
        }
        if (this.state == State.START_CONTENT) {
            if (this.contentLength > 0) {
                this.state = State.FIXED_CONTENT;
            } else if (this.contentLength == 0) {
                this.state = State.EMPTY_CONTENT;
            } else if (Value.CHUNKED.is(this.transferEncoding)) {
                this.state = State.CHUNKED_CONTENT;
            } else {
                throw new BadMessageException(
                    "Transfer-Encoding and Content-Length must exist one"
                );
            }
            this.contents.clear();
        }
        while (buffer.hasRemaining()) {
            out:switch (this.state) {
                case EMPTY_CONTENT:
                    this.contents.add(
                            ByteBuffer.allocate(0).asReadOnlyBuffer()
                        );
                    this.setState(State.END_CONTENT);
                    return;
                case FIXED_CONTENT:
                    if (this.contentLength == 0) {
                        this.setState(State.END_CONTENT);
                        return;
                    }
                    final ByteBuffer content = buffer.asReadOnlyBuffer();
                    if (buffer.remaining() > this.contentLength) {
                        content.limit(content.position() + this.contentLength);
                    }
                    buffer.position(buffer.position() + content.remaining());
                    this.contentLength -= content.remaining();
                    this.contents.add(content);
                    return;
                case CHUNKED_CONTENT:
                    this.setState(State.CHUNK_SIZE);
                    break;
                case CHUNK_SIZE:
                    StringBuilder size = new StringBuilder();
                    while (buffer.hasRemaining()) {
                        final HttpTokens.Token token = this.next(buffer);
                        if (token == null) {
                            return;
                        }
                        final HttpTokens.Type type = token.getType();
                        final char ch = token.getChar();
                        switch (type) {
                            case ALPHA:
                            case DIGIT:
                                size.append(ch);
                                break;
                            case LF:
                                try {
                                    this.chunkLength =
                                        Integer.parseInt(size.toString(), 16);
                                } catch (NumberFormatException e) {
                                    throw new BadMessageException(
                                        "Invalid Chunk-length Value"
                                    );
                                }
                                // Chunk 结束
                                if (this.chunkLength == 0) {
                                    this.setState(State.END_CONTENT);
                                    break out;
                                }
                                this.setState(State.CHUNK_CONTENT);
                                break out;
                            default:
                                throw new IllegalCharacterException(token);
                        }
                    }
                    break;
                case CHUNK_CONTENT:
                    if (this.chunkLength <= 0) {
                        this.setState(State.CHUNKED_CONTENT);
                    } else {
                        ByteBuffer chunk = buffer.asReadOnlyBuffer();
                        if (buffer.remaining() > this.chunkLength) {
                            chunk.limit(chunk.position() + this.chunkLength);
                        }
                        this.chunkLength -= chunk.remaining();
                        buffer.position(buffer.position() + chunk.remaining());
                        this.contents.add(chunk);
                        HttpTokens.Token token = this.next(buffer);
                        if (token != null && token.getType() != Type.LF) {
                            throw new IllegalCharacterException(token);
                        }
                    }
                    break;
                case END_CONTENT:
                    final HttpTokens.Token token = this.next(buffer);
                    if (token != null && token.getType() != Type.LF) {
                        throw new IllegalCharacterException(token);
                    }
                    break;
                default:
                    throw new IllegalStateException(this.state.toString());
            }
        }
    }

    private void addHeader(
        final StringBuilder name,
        final StringBuilder value,
        final State nextState
    ) {
        final String headerName = name.toString();
        final String headerValue = value.toString();
        final List<String> list =
            this.headers.getOrDefault(headerName, new ArrayList<>());
        list.add(headerValue);
        this.headers.put(headerName, list);
        this.setState(nextState);

        switch (headerName.toLowerCase()) {
            case CONTENT_LENGTH:
                if (this.transferEncoding != null) {
                    throw new BadMessageException(
                        "Transfer-Encoding and Content-Length cannot exist at the same time"
                    );
                }
                if (this.contentLength >= 0) {
                    throw new BadMessageException("Multiple Content-Length");
                }
                try {
                    this.contentLength = Integer.parseInt(headerValue);
                } catch (final NumberFormatException e) {
                    throw new BadMessageException(
                        "Invalid Content-Length Value"
                    );
                }
                break;
            case TRANSFER_ENCODING:
                if (this.contentLength >= 0) {
                    throw new BadMessageException(
                        "Transfer-Encoding and Content-Length cannot exist at the same time"
                    );
                }
                if (this.transferEncoding != null) {
                    throw new BadMessageException("Multiple Transfer-Encoding");
                }
                this.transferEncoding = headerValue;
                break;
            default:
            //
        }
    }

    private void setState(final State state) {
        this.state = state;
    }

    private HttpTokens.Token next(final ByteBuffer buffer) {
        final byte ch = buffer.get();
        final HttpTokens.Token token = HttpTokens.parse(ch);
        switch (token.getType()) {
            case CNTL:
                throw new IllegalCharacterException(token);
            case LF:
                this.hasCr = false;
                break;
            case CR:
                if (this.hasCr) {
                    throw new BadMessageException(
                        "Bad EOL, LF does not exist after CR"
                    );
                }
                this.hasCr = true;
                // 分块的时候跳过 CR
                if (buffer.hasRemaining()) {
                    return next(buffer);
                }
                return null;
            case ALPHA:
            case DIGIT:
            case TCHAR:
            case VCHAR:
            case HTAB:
            case SPACE:
            case OTEXT:
            case COLON:
                if (this.hasCr) {
                    throw new BadMessageException("Bad EOL");
                }
                break;
            default:
                break;
        }
        return token;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public StringBuilder getUri() {
        return uri;
    }

    public HttpVersion getVersion() {
        return version;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public List<ByteBuffer> getContent() {
        return contents;
    }

    public ByteBuffer getAllContent() {
        final ByteBuffer buffer = ByteBuffer.allocate(
            this.contents.stream().mapToInt(Buffer::remaining).sum()
        );
        for (final ByteBuffer content : this.contents) {
            buffer.put(content.asReadOnlyBuffer());
        }
        buffer.flip();
        return buffer.asReadOnlyBuffer();
    }
}
