/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * HttpVersion
 *
 * @author Otstar Lin
 * @date 2020/10/23 上午 1:00
 */
public enum HttpVersion {
    /**
     * Http 0.9
     */
    HTTP_0_9("HTTP/0.9", 9),
    /**
     * Http 1.0
     */
    HTTP_1_0("HTTP/1.0", 10),
    /**
     * Http 1.1
     */
    HTTP_1_1("HTTP/1.1", 11),
    /**
     * Http 2.0
     */
    HTTP_2("HTTP/2.0", 20),;

    public static final Map<String, HttpVersion> CACHE = new HashMap<>();

    static {
        for (final HttpVersion version : HttpVersion.values()) {
            CACHE.put(version.toString(), version);
        }
    }

    public static HttpVersion bytesToVersion(
        final byte[] bytes,
        final int position,
        final int limit
    ) {
        final int length = limit - position;
        if (length < 9) {
            return null;
        }

        if (
            bytes[position + 4] == '/' &&
            bytes[position + 6] == '.' &&
            Character.isWhitespace((char) bytes[position + 8]) &&
            (
                (
                    bytes[position] == 'H' &&
                    bytes[position + 1] == 'T' &&
                    bytes[position + 2] == 'T' &&
                    bytes[position + 3] == 'P'
                ) ||
                (
                    bytes[position] == 'h' &&
                    bytes[position + 1] == 't' &&
                    bytes[position + 2] == 't' &&
                    bytes[position + 3] == 'p'
                )
            )
        ) {
            switch (bytes[position + 5]) {
                case '1':
                    switch (bytes[position + 7]) {
                        case '0':
                            return HTTP_1_0;
                        case '1':
                            return HTTP_1_1;
                        default:
                            return null;
                    }
                case '2':
                    if (bytes[position + 7] == '0') {
                        return HTTP_2;
                    }
                    return null;
                default:
                    return null;
            }
        }

        return null;
    }

    public static HttpVersion bytesToVersion(final ByteBuffer buffer) {
        if (buffer.hasArray()) {
            return bytesToVersion(
                buffer.array(),
                buffer.arrayOffset() + buffer.position(),
                buffer.arrayOffset() + buffer.limit()
            );
        }
        return null;
    }

    private final String string;
    private final int version;

    HttpVersion(final String s, final int version) {
        string = s;
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public boolean is(final String s) {
        return string.equalsIgnoreCase(s);
    }

    public String asString() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

    public static HttpVersion from(final String version) {
        return CACHE.get(version);
    }

    public static HttpVersion from(final int version) {
        switch (version) {
            case 9:
                return HttpVersion.HTTP_0_9;
            case 10:
                return HttpVersion.HTTP_1_0;
            case 11:
                return HttpVersion.HTTP_1_1;
            case 20:
                return HttpVersion.HTTP_2;
            default:
                throw new IllegalArgumentException();
        }
    }
}
