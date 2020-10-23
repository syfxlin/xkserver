/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

/**
 * @author Otstar Lin
 * @date 2020/10/22 下午 9:50
 */
public class BadMessageException extends RuntimeException {
    final int status;

    public BadMessageException() {
        this(400, null);
    }

    public BadMessageException(int code) {
        this(code, null);
    }

    public BadMessageException(HttpStatus status) {
        this(status.getValue(), status.getReasonPhrase());
    }

    public BadMessageException(String reason) {
        this(400, reason);
    }

    public BadMessageException(String reason, Throwable cause) {
        this(400, reason, cause);
    }

    public BadMessageException(int status, String reason) {
        this(status, reason, null);
    }

    public BadMessageException(int status, String reason, Throwable cause) {
        super(reason, cause);
        this.status = status;
    }

    public BadMessageException(HttpStatus status, String reason) {
        this(status.getValue(), reason);
    }

    public BadMessageException(HttpStatus status, Throwable cause) {
        this(status.getValue(), status.getReasonPhrase(), cause);
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return this.getMessage();
    }
}
