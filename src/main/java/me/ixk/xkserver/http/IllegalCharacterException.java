/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import me.ixk.xkserver.http.HttpTokens.Token;

/**
 * @author Otstar Lin
 * @date 2020/10/23 上午 12:15
 */
public class IllegalCharacterException extends BadMessageException {
    private static final long serialVersionUID = -6532808020138326821L;

    public IllegalCharacterException(final Token token) {
        super(400, "Illegal character " + token);
    }
}
