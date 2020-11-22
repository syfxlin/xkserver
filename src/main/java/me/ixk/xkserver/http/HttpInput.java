/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.io.IOException;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import me.ixk.xkserver.io.ByteBufferStream;

/**
 * HttpInput
 *
 * @author Otstar Lin
 * @date 2020/10/27 上午 8:25
 */
public class HttpInput extends ServletInputStream {
    private final ByteBufferStream stream = new ByteBufferStream();

    public void writeBuffer(final ByteBufferStream buffer) {
        try {
            stream.readFrom(buffer.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ByteBufferStream readBuffer() {
        return stream;
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public synchronized void reset() throws IOException {
        stream.reset();
    }

    @Override
    public boolean isFinished() {
        return stream.available() <= 0;
    }

    @Override
    public boolean isReady() {
        return stream.available() > 0;
    }

    public void flip() {
        stream.flip();
    }

    @Override
    public void setReadListener(ReadListener readListener) {}
}
