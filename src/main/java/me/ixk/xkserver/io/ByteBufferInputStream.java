/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Otstar Lin
 * @date 2020/11/21 上午 1:00
 */
public class ByteBufferInputStream extends InputStream {
    private final ByteBufferStream stream;

    public ByteBufferInputStream(final ByteBufferStream stream) {
        this.stream = stream;
    }

    @Override
    public final int read() throws IOException {
        return stream.read();
    }

    @Override
    public final int read(byte[] bytes) throws IOException {
        return stream.read(bytes);
    }

    @Override
    public final int read(byte[] bytes, int offset, int length)
        throws IOException {
        return stream.read(bytes, offset, length);
    }

    @Override
    public final long skip(long length) throws IOException {
        return stream.skip(length);
    }

    @Override
    public final int available() throws IOException {
        return stream.available();
    }

    @Override
    public final boolean markSupported() {
        return stream.markSupported();
    }

    @Override
    public final synchronized void mark(int limit) {
        stream.mark();
    }

    @Override
    public final synchronized void reset() throws IOException {
        stream.reset();
    }

    @Override
    public final void close() throws IOException {
        stream.close();
    }
}
