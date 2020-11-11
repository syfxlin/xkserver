/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * SearchPattern
 * <p>
 * Boyer–Moore–Horspool 算法
 *
 * @author Otstar Lin
 * @date 2020/11/11 下午 9:05
 */
public class SearchPattern {
    private static final int ALPHABET_SIZE = 256;
    private final int[] table = new int[ALPHABET_SIZE];
    private final byte[] pattern;

    public static SearchPattern compile(final byte[] pattern) {
        return new SearchPattern(Arrays.copyOf(pattern, pattern.length));
    }

    public static SearchPattern compile(final String pattern) {
        return new SearchPattern(pattern.getBytes(StandardCharsets.UTF_8));
    }

    private SearchPattern(final byte[] pattern) {
        this.pattern = pattern;
        if (pattern.length == 0) {
            throw new IllegalArgumentException("Empty Pattern");
        }
        Arrays.fill(table, pattern.length);
        for (int i = 0; i < pattern.length - 1; ++i) {
            table[0xff & pattern[i]] = pattern.length - 1 - i;
        }
    }

    public int match(final byte[] data, final int offset, final int length) {
        validateArgs(data, offset, length);

        int skip = offset;
        while (skip <= offset + length - pattern.length) {
            for (
                int i = pattern.length - 1;
                data[skip + i] == pattern[i];
                i--
            ) {
                if (i == 0) {
                    return skip;
                }
            }

            skip += table[0xff & data[skip + pattern.length - 1]];
        }

        return -1;
    }

    public int endsWith(final byte[] data, final int offset, final int length) {
        validateArgs(data, offset, length);

        int skip = (pattern.length <= length)
            ? (offset + length - pattern.length)
            : offset;
        while (skip < offset + length) {
            for (
                int i = (offset + length - 1) - skip;
                data[skip + i] == pattern[i];
                --i
            ) {
                if (i == 0) {
                    return (offset + length - skip);
                }
            }
            skip++;
        }

        return 0;
    }

    public int startsWith(
        final byte[] data,
        final int offset,
        final int length,
        final int matched
    ) {
        validateArgs(data, offset, length);

        int matchedCount = 0;
        for (int i = 0; i < pattern.length - matched && i < length; i++) {
            if (data[offset + i] == pattern[i + matched]) {
                matchedCount++;
            } else {
                return 0;
            }
        }

        return matched + matchedCount;
    }

    private void validateArgs(
        final byte[] data,
        final int offset,
        final int length
    ) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset was negative");
        } else if (length < 0) {
            throw new IllegalArgumentException("length was negative");
        } else if (offset + length > data.length) {
            throw new IllegalArgumentException(
                "(offset+length) out of bounds of data[]"
            );
        }
    }

    public int length() {
        return pattern.length;
    }
}
