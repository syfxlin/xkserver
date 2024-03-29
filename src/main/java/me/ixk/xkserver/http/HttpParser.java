/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import me.ixk.xkserver.http.HttpHeader.Value;
import me.ixk.xkserver.http.HttpTokens.Type;
import me.ixk.xkserver.io.ByteBufferPool;
import me.ixk.xkserver.io.ByteBufferStream;

/**
 * Http 报文解析
 *
 * @author Otstar Lin
 * @date 2020/10/22 下午 9:10
 */
public class HttpParser {
    private static final String CONTENT_LENGTH = "content-length";
    private static final String TRANSFER_ENCODING = "transfer-encoding";
    private static final String TRAILER = "trailer";

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
        HEADER,
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
        CONTENT,
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
        /**
         * 结束
         */
        END,
    }

    private final RequestHandler handler;
    private State state = State.START_LINE;
    private int contentLength = -1;
    private String transferEncoding = null;
    private int chunkLength = 0;
    private final Set<String> trailers;
    private boolean hasCr = false;
    private int maxHeaderByteLength = -1;

    /**
     * 在使用后重置
     */
    private final StringBuilder string = new StringBuilder();
    private final StringBuilder value = new StringBuilder();
    private int length = 0;
    private State headerState;

    private boolean eof = false;

    public HttpParser(final RequestHandler handler) {
        this(handler, -1);
    }

    public HttpParser(
        final RequestHandler handler,
        final int maxHeaderByteLength
    ) {
        this.handler = handler;
        this.trailers = new HashSet<>();
        this.maxHeaderByteLength = maxHeaderByteLength;
    }

    public void parse(final ByteBufferStream buffer) {
        try {
            if (this.state == State.END) {
                throw new IllegalStateException("HttpParser status is END");
            }
            if (this.state == State.START_LINE) {
                this.state = State.METHOD;
            }
            // 请求行 "Method Url Version" "GET /url HTTP/1.1"
            this.parseLine(buffer);
            // 头字段 "Name: Value" "Host: ixk.me"
            this.parseHeaders(buffer);
            // 解析内容
            this.parseContent(buffer);
            // Trailer
            this.parseTrailer(buffer);

            // 清除末尾多余的换行
            this.parseCrLf(buffer);

            if (this.isEof() && !buffer.hasRemaining()) {
                this.end();
            }
        } catch (final BadMessageException e) {
            this.handler.failure(e);
        } catch (final Throwable e) {
            this.handler.failure(
                    new BadMessageException(HttpStatus.BAD_REQUEST, e)
                );
        }
    }

    public void end() {
        this.setEof(true);
        this.handler.requestComplete();
        this.state = State.END;
    }

    private void parseLine(final ByteBufferStream buffer) {
        if (
            this.state.ordinal() >= State.HEADER.ordinal() ||
            !buffer.hasRemaining()
        ) {
            return;
        }
        while (buffer.hasRemaining()) {
            final HttpTokens.Token token = this.next(buffer);
            if (token == null) {
                break;
            }

            this.addLength(
                    l -> {
                        throw new BadMessageException(
                            this.state == State.URI
                                ? HttpStatus.URI_TOO_LONG
                                : HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE
                        );
                    }
                );

            final HttpTokens.Type type = token.getType();
            final char ch = token.getChar();

            switch (this.state) {
                case METHOD:
                    switch (type) {
                        // 方法字符
                        case ALPHA:
                            this.string.append(ch);
                            break;
                        // 空格切换到 SPACE1 状态
                        case SPACE:
                            final HttpMethod method = HttpMethod.from(
                                this.string.toString()
                            );
                            if (method == null) {
                                throw new BadMessageException();
                            }
                            this.handler.setHttpMethod(method);
                            this.string.setLength(0);
                            this.state = State.SPACE1;
                            break;
                        default:
                            throw new IllegalCharacterException(token);
                    }
                    break;
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
                            this.string.append(ch);
                            this.state = State.URI;
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
                            this.string.append(ch);
                            break;
                        // 遇到空格则说明 URL 部分结束，转换到第二个空格状态
                        case SPACE:
                            this.handler.setHttpUri(
                                    new HttpUri(this.string.toString())
                                );
                            this.string.setLength(0);
                            this.state = State.SPACE2;
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
                            this.state = State.VERSION;
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
                            final HttpVersion version = HttpVersion.from(
                                this.string.toString()
                            );
                            if (version == null) {
                                throw new BadMessageException(
                                    HttpStatus.HTTP_VERSION_NOT_SUPPORTED,
                                    "Unknown Version"
                                );
                            }
                            this.handler.setHttpVersion(version);
                            this.string.setLength(0);
                            this.length = 0;
                            this.state = State.HEADER;
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

    private void parseHeaders(final ByteBufferStream buffer) {
        if (!buffer.hasRemaining()) {
            return;
        }
        if (
            this.state.ordinal() >= State.CONTENT.ordinal() &&
            this.state != State.TRAILER
        ) {
            return;
        }
        while (buffer.hasRemaining()) {
            final HttpTokens.Token token = this.next(buffer);
            if (token == null) {
                break;
            }

            this.addLength(
                    l -> {
                        throw new BadMessageException(
                            state == State.HEADER
                                ? HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE
                                : HttpStatus.PAYLOAD_TOO_LARGE
                        );
                    }
                );

            final HttpTokens.Type type = token.getType();
            final char ch = token.getChar();

            switch (this.state) {
                case HEADER:
                case TRAILER:
                    // HEADER 状态的下一个状态一定是 HEADER_NAME，头字段的名称首字符一定为字母
                    switch (type) {
                        case ALPHA:
                            this.string.append(ch);
                            this.headerState = this.state;
                            this.state = State.HEADER_NAME;
                            break;
                        // 头字段部分结束，进入 CONTENT 部分
                        case LF:
                            if (this.headerState != State.TRAILER) {
                                this.handler.headerComplete();
                            } else {
                                this.handler.trailerComplete();
                            }
                            this.state = State.CONTENT;
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
                            this.state = State.HEADER_COLON;
                            break;
                        // 遇到换行，说明这是一个无值的头字段
                        case LF:
                            this.addHeader();
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
                            this.value.append(ch);
                            this.state = State.HEADER_VALUE;
                            break;
                        // 遇到换行，说明这是一个无值的头字段
                        case LF:
                            this.addHeader();
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
                            this.addHeader();
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

    private void parseContent(final ByteBufferStream buffer) {
        if (
            this.state.ordinal() >= State.TRAILER.ordinal() ||
            !buffer.hasRemaining()
        ) {
            return;
        }

        if (this.state == State.CONTENT) {
            // 首次解析内容的时候按规则分配
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
        }

        while (buffer.hasRemaining()) {
            switch (this.state) {
                // 空内容
                case EMPTY_CONTENT:
                    this.handler.addContent(
                            new ByteBufferStream(0, handler.bufferPool())
                        );
                    this.state = State.END_CONTENT;
                    return;
                // 定长内容
                case FIXED_CONTENT:
                    if (this.contentLength == 0) {
                        this.state = State.END_CONTENT;
                        return;
                    }
                    final ByteBufferStream content = buffer.duplicate();
                    if (buffer.remaining() > this.contentLength) {
                        content.limit(content.position() + this.contentLength);
                    }
                    buffer.position(buffer.position() + content.remaining());
                    this.contentLength -= content.remaining();
                    this.handler.addContent(content);
                    return;
                // 分块内容
                case CHUNKED_CONTENT:
                case CHUNK_SIZE:
                    final HttpTokens.Token token = this.next(buffer);
                    if (token == null) {
                        return;
                    }
                    final HttpTokens.Type type = token.getType();
                    final char ch = token.getChar();
                    switch (type) {
                        case ALPHA:
                        case DIGIT:
                            // 拼接分块大小 16 进制数
                            this.state = State.CHUNK_SIZE;
                            this.string.append(ch);
                            break;
                        case LF:
                            if (this.state == State.CHUNK_SIZE) {
                                try {
                                    this.chunkLength =
                                        Integer.parseInt(
                                            this.string.toString(),
                                            16
                                        );
                                } catch (final NumberFormatException e) {
                                    throw new BadMessageException(
                                        "Invalid chunk-length value [" +
                                        this.string.toString() +
                                        "]"
                                    );
                                }
                                if (this.chunkLength == 0) {
                                    // 分块为 0 则代表内容已经结束
                                    this.state = State.END_CONTENT;
                                } else {
                                    // 不为 0 则切换为读取分块内容状态
                                    this.state = State.CHUNK_CONTENT;
                                }
                                this.string.setLength(0);
                            }
                            break;
                        default:
                            throw new IllegalCharacterException(token);
                    }
                    break;
                case CHUNK_CONTENT:
                    if (this.chunkLength <= 0) {
                        // 当分块内容读取完毕时切换到分块的初始状态，进行下一轮分块读取
                        this.state = State.CHUNKED_CONTENT;
                    } else {
                        final ByteBufferStream chunk = buffer.duplicate();
                        if (buffer.remaining() > this.chunkLength) {
                            chunk.limit(chunk.position() + this.chunkLength);
                        }
                        this.chunkLength -= chunk.remaining();
                        buffer.position(buffer.position() + chunk.remaining());
                        this.handler.addContent(chunk);
                    }
                    break;
                case END_CONTENT:
                    this.handler.contentComplete();
                    this.state = State.TRAILER;
                    return;
                default:
                    throw new IllegalStateException(this.state.toString());
            }
        }
    }

    private void addHeader() {
        final String headerName = this.string.toString();

        if (
            this.headerState == State.TRAILER &&
            !this.trailers.contains(headerName)
        ) {
            throw new BadMessageException("Header is not defined in Trailer");
        }

        final String headerValue = this.value.toString();
        HttpField field = this.handler.getHttpField(headerName);
        if (field == null) {
            field = new HttpField(headerName);
        }
        field.addValue(headerValue);
        if (this.headerState != State.TRAILER) {
            this.handler.addHttpHeader(field);
        } else {
            this.handler.addHttpTrailer(field);
        }

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
            case TRAILER:
                this.trailers.addAll(field.getValues());
                break;
            default:
            //
        }

        // 重置
        this.string.setLength(0);
        this.value.setLength(0);
        this.length = 0;
        this.state = this.headerState;
    }

    private void parseTrailer(final ByteBufferStream buffer) {
        if (
            this.state.ordinal() > State.TRAILER.ordinal() ||
            !buffer.hasRemaining() ||
            this.trailers.isEmpty()
        ) {
            return;
        }
        this.parseHeaders(buffer);
    }

    private void parseCrLf(final ByteBufferStream buffer) {
        if (
            (this.trailers.isEmpty() && this.state == State.END_CONTENT) ||
            this.state == State.TRAILER
        ) {
            final ByteBufferStream copy = buffer.duplicate();
            while (buffer.hasRemaining()) {
                final HttpTokens.Token next = this.next(copy);
                if (next != null && next.getType() == Type.LF) {
                    this.next(buffer);
                } else {
                    break;
                }
            }
        }
    }

    private void addLength(final Consumer<Integer> consumer) {
        this.length++;
        if (
            this.maxHeaderByteLength > 0 &&
            this.length > this.maxHeaderByteLength
        ) {
            consumer.accept(this.length);
        }
    }

    private HttpTokens.Token next(final ByteBufferStream buffer) {
        if (!buffer.hasRemaining()) {
            return null;
        }
        final int ch = buffer.read();
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

    public boolean isEof() {
        return eof;
    }

    public void setEof(final boolean eof) {
        this.eof = eof;
    }

    public interface RequestHandler {
        /**
         * 读取 BufferPool
         *
         * @return ByteBufferPool
         */
        ByteBufferPool bufferPool();

        /**
         * 设置 Http 方法
         *
         * @param method 方法
         */
        void setHttpMethod(HttpMethod method);

        /**
         * 设置 HttpUri
         *
         * @param uri URI
         */
        void setHttpUri(HttpUri uri);

        /**
         * 设置 Http 版本
         *
         * @param version 版本
         */
        void setHttpVersion(HttpVersion version);

        /**
         * 获取 Http 字段
         *
         * @param name 字段名称
         *
         * @return Http 字段
         */
        HttpField getHttpField(String name);

        /**
         * 添加 Http 头
         *
         * @param field 头字段
         */
        void addHttpHeader(HttpField field);

        /**
         * 添加 Http 尾
         *
         * @param field 尾字段
         */
        void addHttpTrailer(HttpField field);

        /**
         * 添加内容
         *
         * @param buffer ByteBuffer
         */
        void addContent(ByteBufferStream buffer);

        /**
         * 失败时调用
         *
         * @param e 错误
         */
        default void failure(final BadMessageException e) {
            throw e;
        }

        /**
         * 头字段解析完成
         */
        default void headerComplete() {}

        /**
         * 内容解析完成
         */
        default void contentComplete() {}

        /**
         * 尾字段解析完成
         */
        default void trailerComplete() {}

        /**
         * 请求解析完成
         */
        default void requestComplete() {}
    }
}
