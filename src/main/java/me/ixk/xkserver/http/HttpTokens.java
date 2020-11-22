/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

/**
 * Ascii 码映射
 *
 * @author Otstar Lin
 * @date 2020/10/22 下午 9:58
 */
public class HttpTokens {
    static final byte COLON = (byte) ':';
    static final byte TAB = 0x09;
    static final byte LINE_FEED = 0x0A;
    static final byte CARRIAGE_RETURN = 0x0D;
    static final byte SPACE = 0x20;
    static final byte[] CRLF = { CARRIAGE_RETURN, LINE_FEED };

    public enum Type {
        /**
         * 控制字符
         */
        CNTL,
        /**
         * 水平制表符
         */
        HTAB,
        /**
         * LF
         */
        LF,
        /**
         * CR
         */
        CR,
        /**
         * 空格
         */
        SPACE,
        /**
         * 分隔符
         */
        COLON,
        /**
         * 数字
         */
        DIGIT,
        /**
         * 字母
         */
        ALPHA,
        /**
         * 部分符号
         */
        TCHAR,
        /**
         * 其他可见字符
         */
        VCHAR,
        /**
         * 超过 Ascii 码表定义的字符，即 0x80 以上的字符
         */
        OTEXT,
    }

    public static class Token {
        private final Type type;
        private final byte b;
        private final char c;

        private Token(final byte b, final Type type) {
            this.type = type;
            this.b = b;
            c = (char) (0xff & b);
        }

        public Type getType() {
            return type;
        }

        public byte getByte() {
            return b;
        }

        public char getChar() {
            return c;
        }

        @Override
        public String toString() {
            switch (type) {
                case SPACE:
                case COLON:
                case ALPHA:
                case DIGIT:
                case TCHAR:
                case VCHAR:
                    return type + "='" + c + "'";
                case CR:
                    return "CR=\\r";
                case LF:
                    return "LF=\\n";
                default:
                    return String.format("%s=0x%x", type, b);
            }
        }
    }

    public static final Token[] TOKENS = new Token[256];

    static {
        for (int b = 0; b < 256; b++) {
            // token          = 1*tchar
            // tchar          = "!" / "#" / "$" / "%" / "&" / "'" / "*"
            //                / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~"
            //                / DIGIT / ALPHA
            //                ; any VCHAR, except delimiters
            // quoted-string  = DQUOTE *( qdtext / quoted-pair ) DQUOTE
            // qdtext         = HTAB / SP /%x21 / %x23-5B / %x5D-7E / obs-text
            // obs-text       = %x80-FF
            // comment        = "(" *( ctext / quoted-pair / comment ) ")"
            // ctext          = HTAB / SP / %x21-27 / %x2A-5B / %x5D-7E / obs-text
            // quoted-pair    = "\" ( HTAB / SP / VCHAR / obs-text )

            switch (b) {
                case LINE_FEED:
                    TOKENS[b] = new Token((byte) b, Type.LF);
                    break;
                case CARRIAGE_RETURN:
                    TOKENS[b] = new Token((byte) b, Type.CR);
                    break;
                case SPACE:
                    TOKENS[b] = new Token((byte) b, Type.SPACE);
                    break;
                case TAB:
                    TOKENS[b] = new Token((byte) b, Type.HTAB);
                    break;
                case COLON:
                    TOKENS[b] = new Token((byte) b, Type.COLON);
                    break;
                case '!':
                case '#':
                case '$':
                case '%':
                case '&':
                case '\'':
                case '*':
                case '+':
                case '-':
                case '.':
                case '^':
                case '_':
                case '`':
                case '|':
                case '~':
                    TOKENS[b] = new Token((byte) b, Type.TCHAR);
                    break;
                default:
                    if (b >= 0x30 && b <= 0x39) {
                        TOKENS[b] = new Token((byte) b, Type.DIGIT);
                    } else if (b >= 0x41 && b <= 0x5A) {
                        TOKENS[b] = new Token((byte) b, Type.ALPHA);
                    } else if (b >= 0x61 && b <= 0x7A) {
                        TOKENS[b] = new Token((byte) b, Type.ALPHA);
                    } else if (b >= 0x21 && b <= 0x7E) {
                        TOKENS[b] = new Token((byte) b, Type.VCHAR);
                    } else if (b >= 0x80) {
                        TOKENS[b] = new Token((byte) b, Type.OTEXT);
                    } else {
                        TOKENS[b] = new Token((byte) b, Type.CNTL);
                    }
            }
        }
    }

    public static Token parse(byte ch) {
        return TOKENS[0xff & ch];
    }

    public static Token parse(int ch) {
        return TOKENS[0xff & ch];
    }

    public static Token parse(char ch) {
        return parse((byte) ch);
    }
}
