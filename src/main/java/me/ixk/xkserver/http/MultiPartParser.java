/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import me.ixk.xkserver.http.HttpTokens.Token;
import me.ixk.xkserver.http.HttpTokens.Type;
import me.ixk.xkserver.io.ByteBufferPool;
import me.ixk.xkserver.io.ByteBufferStream;

/**
 * Multipart 解析器
 *
 * @author Otstar Lin
 * @date 2020/11/11 上午 8:20
 */
public class MultiPartParser {
    private static final String CONTENT_HEADER_PREFIX = "content";

    public enum State {
        /**
         * 初始
         */
        START,
        /**
         * 分割符
         */
        DELIMITER,
        /**
         * 分割符 Padding
         */
        DELIMITER_PADDING,
        /**
         * 分割符，结束
         */
        DELIMITER_CLOSE,
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
         * 首字节
         */
        FIRST_OCTETS,
        /**
         * 字节流
         */
        OCTETS,
        /**
         * 结束
         */
        END,
    }

    private State state = State.START;
    private boolean hasCr = false;
    private final PartHandler handler;
    private final SearchPattern delimiterSearch;
    private final int maxHeaderByteLength;
    private final ByteBufferStream patternBuffer;

    private final StringBuilder string = new StringBuilder();
    private final StringBuilder value = new StringBuilder();
    private int length = 0;
    private boolean eof = false;
    private int partialBoundary = 2;

    public MultiPartParser(final PartHandler handler, final String boundary) {
        this(handler, boundary, -1);
    }

    public MultiPartParser(
        final PartHandler handler,
        final String boundary,
        final int maxHeaderByteLength
    ) {
        this.handler = handler;
        final String delimiter = "\r\n--" + boundary;
        this.patternBuffer =
            new ByteBufferStream(
                ByteBuffer.wrap(delimiter.getBytes(StandardCharsets.US_ASCII)),
                handler.bufferPool()
            );
        this.delimiterSearch = SearchPattern.compile(delimiter);
        this.maxHeaderByteLength = maxHeaderByteLength;
    }

    public void parse(final ByteBufferStream buffer) {
        try {
            if (this.state == State.END) {
                throw new IllegalStateException("PartParser status is END");
            }
            if (this.state == State.START) {
                this.state = State.DELIMITER;
            }
            while (buffer.hasRemaining()) {
                switch (this.state) {
                    case DELIMITER:
                    case DELIMITER_PADDING:
                    case DELIMITER_CLOSE:
                        // 分割符 "--Web" "--Web--"
                        this.parseDelimiter(buffer);
                        continue;
                    case HEADER:
                    case HEADER_NAME:
                    case HEADER_COLON:
                    case HEADER_VALUE:
                        // 头字段 "Content-Type: text/plain"
                        this.parsePartHeaders(buffer);
                        break;
                    case FIRST_OCTETS:
                    case OCTETS:
                        // 字节流 "test"
                        this.parseOctetContent(buffer);
                        break;
                    case END:
                        break;
                    default:
                        throw new IllegalStateException();
                }
                if (this.isEof() && !buffer.hasRemaining()) {
                    this.end();
                }
            }
        } catch (final BadMessageException e) {
            this.handler.failure(e);
        } catch (final Exception e) {
            this.handler.failure(
                    new BadMessageException(HttpStatus.BAD_REQUEST, e)
                );
        }
    }

    public void end() {
        this.setEof(true);
        this.handler.partComplete();
        this.state = State.END;
    }

    private void parseDelimiter(final ByteBufferStream buffer) {
        if (
            this.state.ordinal() >= State.HEADER.ordinal() ||
            !buffer.hasRemaining()
        ) {
            return;
        }
        switch (this.state) {
            case DELIMITER:
                // 如果存在未解析完的分割符，则检查 Buffer 是否以指定位置开始匹配
                if (this.partialBoundary > 0) {
                    final int partial =
                        this.delimiterSearch.startsWith(
                                buffer.array(),
                                buffer.arrayOffset() + buffer.position(),
                                buffer.remaining(),
                                this.partialBoundary
                            );
                    if (partial > 0) {
                        // 如果长度刚好为分割符则说明此时的位置为分割符
                        if (partial == this.delimiterSearch.length()) {
                            buffer.position(
                                buffer.position() +
                                partial -
                                this.partialBoundary
                            );
                            this.partialBoundary = 0;
                            this.state = State.DELIMITER_PADDING;
                            return;
                        }

                        // 长度不刚好，但是匹配，则增加 partialBoundary
                        this.partialBoundary = partial;
                        // 清空 Buffer
                        this.clearBuffer(buffer);
                        return;
                    }
                    // 否则该位置不为分割符，重新开始匹配
                    this.partialBoundary = 0;
                }

                // 查找 Buffer 中匹配的分割符的索引
                final int match =
                    this.delimiterSearch.match(
                            buffer.array(),
                            buffer.arrayOffset() + buffer.position(),
                            buffer.remaining()
                        );
                // 大于 0 则说明查找到，此时将 Buffer 定位到索引
                if (match >= 0) {
                    buffer.position(
                        match -
                        buffer.arrayOffset() +
                        this.delimiterSearch.length()
                    );
                    this.state = State.DELIMITER_PADDING;
                    return;
                }

                // 查看是否有匹配的结尾，若有将其存入 partialBoundary，下次 Buffer 提交进来的时候就可以解析
                this.partialBoundary =
                    this.delimiterSearch.endsWith(
                            buffer.array(),
                            buffer.arrayOffset() + buffer.position(),
                            buffer.remaining()
                        );
                this.clearBuffer(buffer);
                break;
            case DELIMITER_PADDING:
            case DELIMITER_CLOSE:
                while (buffer.hasRemaining()) {
                    final HttpTokens.Token token = next(buffer);
                    // 可能为 CR
                    if (token == null) {
                        continue;
                    }
                    // 若为 LF，则进入 Header 解析
                    if (token.getType() == Type.LF) {
                        this.state = State.HEADER;
                        this.handler.startPart();
                        return;
                    }
                    if (this.state == State.DELIMITER_PADDING) {
                        if (token.getChar() == '-') {
                            this.state = State.DELIMITER_CLOSE;
                        }
                    } else if (this.state == State.DELIMITER_CLOSE) {
                        // 若检测到两个 -- 则说明 Body 解析完成
                        if (token.getChar() == '-') {
                            this.state = State.END;
                            return;
                        }
                    }
                }
                break;
            default:
                throw new IllegalStateException(this.state.toString());
        }
    }

    private void parsePartHeaders(final ByteBufferStream buffer) {
        if (
            this.state.ordinal() >= State.OCTETS.ordinal() ||
            !buffer.hasRemaining()
        ) {
            return;
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
                throw new BadMessageException(
                    HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE
                );
            }

            final HttpTokens.Type type = token.getType();
            final char ch = token.getChar();

            switch (this.state) {
                case HEADER:
                    // HEADER 状态的下一个状态一定是 HEADER_NAME，头字段的名称首字符一定为字母
                    switch (type) {
                        case ALPHA:
                            this.string.append(ch);
                            this.state = State.HEADER_NAME;
                            break;
                        // 头字段部分结束，进入 CONTENT 部分
                        case LF:
                            this.handler.headerComplete();
                            this.state = State.OCTETS;
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

    private void parseOctetContent(final ByteBufferStream buffer) {
        if (
            this.state.ordinal() >= State.END.ordinal() ||
            !buffer.hasRemaining()
        ) {
            return;
        }
        if (this.partialBoundary > 0) {
            final int partial =
                this.delimiterSearch.startsWith(
                        buffer.array(),
                        buffer.arrayOffset() + buffer.position(),
                        buffer.remaining(),
                        this.partialBoundary
                    );
            if (partial > 0) {
                if (partial == this.delimiterSearch.length()) {
                    buffer.position(
                        buffer.position() +
                        this.delimiterSearch.length() -
                        this.partialBoundary
                    );
                    this.partialBoundary = 0;
                    this.state = State.DELIMITER_PADDING;
                    this.handler.addContent(
                            new ByteBufferStream(0, handler.bufferPool())
                        );
                    this.handler.endPart();
                    return;
                }

                this.partialBoundary = partial;
                this.clearBuffer(buffer);
                return;
            } else {
                // output up to _partialBoundary of the search pattern
                final ByteBufferStream content = this.patternBuffer.slice();
                if (this.state == State.FIRST_OCTETS) {
                    this.state = State.OCTETS;
                    content.position(2);
                }
                content.limit(this.partialBoundary);
                this.partialBoundary = 0;
                this.handler.addContent(content);
                return;
            }
        }

        final int match =
            this.delimiterSearch.match(
                    buffer.array(),
                    buffer.arrayOffset() + buffer.position(),
                    buffer.remaining()
                );
        if (match > 0) {
            final ByteBufferStream content = buffer.slice();
            content.limit(match - buffer.arrayOffset() - buffer.position());
            buffer.position(
                match - buffer.arrayOffset() + this.delimiterSearch.length()
            );
            this.partialBoundary = 0;
            this.state = State.DELIMITER_PADDING;
            this.handler.addContent(content);
            this.handler.endPart();
            return;
        }

        this.partialBoundary =
            this.delimiterSearch.endsWith(
                    buffer.array(),
                    buffer.arrayOffset() + buffer.position(),
                    buffer.remaining()
                );
        if (this.partialBoundary > 0) {
            final ByteBufferStream content = buffer.slice();
            content.limit(content.limit() - this.partialBoundary);
            this.clearBuffer(buffer);
            this.handler.addContent(content);
            return;
        }

        final ByteBufferStream content = buffer.slice();
        this.clearBuffer(buffer);
        this.handler.addContent(content);
    }

    public boolean isEof() {
        return eof;
    }

    public void setEof(final boolean eof) {
        this.eof = eof;
    }

    private void addHeader() {
        final String headerName = this.string.toString();

        if (!headerName.toLowerCase().startsWith(CONTENT_HEADER_PREFIX)) {
            throw new BadMessageException(
                "The body cannot contain headers other than the \"Content-XXX\" header"
            );
        }

        final String headerValue = this.value.toString();
        HttpField field = this.handler.getHttpField(headerName);
        if (field == null) {
            field = new HttpField(headerName);
        }
        field.addValue(headerValue);
        this.handler.addHttpHeader(field);

        // 重置
        this.string.setLength(0);
        this.value.setLength(0);
        this.length = 0;
        this.partialBoundary = 0;
        this.state = State.HEADER;
    }

    private Token next(final ByteBufferStream buffer) {
        final int ch = buffer.read();
        final Token t = HttpTokens.TOKENS[0xff & ch];

        switch (t.getType()) {
            case CNTL:
                throw new IllegalCharacterException(t);
            case LF:
                hasCr = false;
                break;
            case CR:
                if (hasCr) {
                    throw new BadMessageException("Bad EOL");
                }

                hasCr = true;
                return null;
            case ALPHA:
            case DIGIT:
            case TCHAR:
            case VCHAR:
            case HTAB:
            case SPACE:
            case OTEXT:
            case COLON:
                if (hasCr) {
                    throw new BadMessageException("Bad EOL");
                }
                break;
            default:
                break;
        }

        return t;
    }

    private void clearBuffer(final ByteBufferStream buffer) {
        buffer.position(0);
        buffer.limit(0);
    }

    public interface PartHandler {
        /**
         * 读取 BufferPool
         *
         * @return ByteBufferPool
         */
        ByteBufferPool bufferPool();

        /**
         * 开始
         */
        void startPart();

        /**
         * 添加 Http 头
         *
         * @param field 头字段
         */
        void addHttpHeader(HttpField field);

        /**
         * 添加内容
         *
         * @param buffer ByteBuffer
         */
        void addContent(ByteBufferStream buffer);

        /**
         * 结束
         */
        void endPart();

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
         * Part 解析完成
         */
        default void partComplete() {}

        /**
         * 获取 Http 字段
         *
         * @param headerName 字段名称
         *
         * @return Http 字段
         */
        HttpField getHttpField(String headerName);
    }
}
