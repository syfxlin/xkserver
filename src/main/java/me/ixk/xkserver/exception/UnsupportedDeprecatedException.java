/*
 * Copyright (c) 2021, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.exception;

/**
 * @author Otstar Lin
 * @date 2020/12/6 下午 2:26
 */
public class UnsupportedDeprecatedException extends RuntimeException {

    private static final long serialVersionUID = 117581552013722101L;

    public UnsupportedDeprecatedException() {
    }

    public UnsupportedDeprecatedException(final String message) {
        super(message);
    }

    public UnsupportedDeprecatedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public UnsupportedDeprecatedException(final Throwable cause) {
        super(cause);
    }

    public UnsupportedDeprecatedException(
        final String message,
        final Throwable cause,
        final boolean enableSuppression,
        final boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
