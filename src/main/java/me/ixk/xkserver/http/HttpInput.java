/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletInputStream;

/**
 * HttpInput
 *
 * @author Otstar Lin
 * @date 2020/10/27 上午 8:25
 */
public class HttpInput extends ServletInputStream {
    private final List<ByteBuffer> buffers = new ArrayList<>();
    private final AtomicInteger index = new AtomicInteger(0);

    public void addBuffer(ByteBuffer buffer) {
        this.buffers.add(buffer);
    }

    @Override
    public int read() throws IOException {
        if (this.index.get() >= this.buffers.size()) {
            return -1;
        }
        final ByteBuffer buffer = this.buffers.get(this.index.get());
        if (buffer.hasRemaining()) {
            return buffer.get();
        }
        this.index.getAndIncrement();
        return this.read();
    }

    public boolean hasNext() {
        if (this.index.get() >= this.buffers.size()) {
            return false;
        }
        return this.buffers.get(this.index.get()).hasRemaining();
    }

    @Override
    public synchronized void reset() throws IOException {
        for (ByteBuffer buffer : this.buffers) {
            buffer.rewind();
        }
        this.index.set(0);
    }
}
