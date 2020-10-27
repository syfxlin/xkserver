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
        if (index.get() >= this.buffers.size()) {
            return -1;
        }
        final ByteBuffer buffer = this.buffers.get(index.get());
        if (buffer.hasRemaining()) {
            return buffer.get();
        }
        index.getAndIncrement();
        return this.read();
    }
}
