/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Otstar Lin
 * @date 2020/11/21 上午 1:01
 */
public class ByteBufferOutputStream extends OutputStream {
    private final ByteBufferStream stream;

    public ByteBufferOutputStream(final ByteBufferStream stream) {
        this.stream = stream;
    }

    @Override
    public final void write(int b) throws IOException {
        stream.write(b);
    }

    @Override
    public final void write(byte[] bytes) throws IOException {
        stream.write(bytes);
    }

    @Override
    public final void write(byte[] bytes, int offset, int length)
        throws IOException {
        stream.write(bytes, offset, length);
    }

    @Override
    public final void close() throws IOException {
        stream.close();
    }
}
