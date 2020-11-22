/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * @author Otstar Lin
 * @date 2020/11/21 上午 12:35
 */
public class ByteBufferStream {
    private static final int DEFAULT_CAPACITY = 1024;

    private final ByteBufferPool bufferPool;
    private volatile ByteBuffer buffer;

    private volatile InputStream inputStream;
    private volatile OutputStream outputStream;

    public ByteBufferStream() {
        this(ByteBufferPool.defaultPool());
    }

    public ByteBufferStream(ByteBufferPool bufferPool) {
        this(DEFAULT_CAPACITY, bufferPool);
    }

    public ByteBufferStream(int capacity) {
        this(capacity, ByteBufferPool.defaultPool());
    }

    public ByteBufferStream(int capacity, ByteBufferPool bufferPool) {
        this(capacity, false, bufferPool);
    }

    public ByteBufferStream(int capacity, boolean direct) {
        this(capacity, direct, ByteBufferPool.defaultPool());
    }

    public ByteBufferStream(
        int capacity,
        boolean direct,
        ByteBufferPool bufferPool
    ) {
        this(bufferPool.acquire(capacity, direct), bufferPool);
    }

    public ByteBufferStream(ByteBuffer buffer) {
        this(buffer, ByteBufferPool.defaultPool());
    }

    public ByteBufferStream(ByteBuffer buffer, ByteBufferPool bufferPool) {
        this.bufferPool = bufferPool;
        this.buffer = buffer;
    }

    public static ByteBufferStream allocate(int capacity) {
        return new ByteBufferStream(capacity, false);
    }

    public static ByteBufferStream allocateDirect(int capacity) {
        return new ByteBufferStream(capacity, true);
    }

    public static ByteBufferStream wrap(byte[] array, int offset, int length) {
        return new ByteBufferStream(ByteBuffer.wrap(array, offset, length));
    }

    public static ByteBufferStream wrap(byte[] array) {
        return new ByteBufferStream(ByteBuffer.wrap(array));
    }

    private void grow(int n) {
        if (buffer.remaining() < n) {
            int required = buffer.position() + n;
            if (required > buffer.capacity()) {
                int size = this.calculateNewCapacity(required);
                ByteBuffer buf =
                    this.bufferPool.acquire(size, this.buffer.isDirect());
                buffer.flip();
                buf.put(buffer);
                this.bufferPool.release(buffer);
                buffer = buf;
            } else {
                buffer.limit(required);
            }
        }
    }

    private int calculateNewCapacity(int minNewCapacity) {
        // 4 MiB page
        final int threshold = 1048576 * 4;
        if (minNewCapacity == threshold) {
            return threshold;
        }
        // If over threshold, do not double but just increase by threshold.
        if (minNewCapacity > threshold) {
            int newCapacity = minNewCapacity / threshold * threshold;
            newCapacity += threshold;
            return newCapacity;
        }
        // Not over threshold. Double up to 4 MiB, starting from 64.
        int newCapacity = 64;
        while (newCapacity < minNewCapacity) {
            newCapacity <<= 1;
        }
        return newCapacity;
    }

    public final int read() {
        if (buffer.hasRemaining()) {
            return buffer.get() & 0xff;
        } else {
            return -1;
        }
    }

    public final int read(byte[] bytes) {
        return read(bytes, 0, bytes.length);
    }

    public final int read(byte[] bytes, int offset, int length) {
        if (length <= 0) {
            return 0;
        }
        int remain = buffer.remaining();
        if (remain <= 0) {
            return -1;
        }
        if (length >= remain) {
            buffer.get(bytes, offset, remain);
            return remain;
        }
        buffer.get(bytes, offset, length);
        return length;
    }

    public final int read(ByteBuffer buffer) {
        int length = buffer.remaining();
        if (length <= 0) {
            return 0;
        }
        int remain = this.buffer.remaining();
        if (remain <= 0) {
            return -1;
        }
        if (length >= remain) {
            buffer.put(this.buffer);
            return remain;
        }
        int limit = this.buffer.limit();
        this.buffer.limit(this.buffer.position() + length);
        buffer.put(this.buffer);
        this.buffer.limit(limit);
        return length;
    }

    public final long skip(long length) {
        if (length <= 0) {
            return 0;
        }
        int remain = buffer.remaining();
        if (remain <= 0) {
            return 0;
        }
        if (length > remain) {
            buffer.position(buffer.limit());
            return remain;
        }
        buffer.position(buffer.position() + (int) length);
        return length;
    }

    public final int available() {
        return buffer.remaining();
    }

    public final boolean markSupported() {
        return true;
    }

    public final void mark() {
        buffer.mark();
    }

    public final void reset() {
        buffer.reset();
    }

    public final void write(int b) {
        grow(1);
        buffer.put((byte) b);
    }

    public final void write(byte[] bytes) {
        write(bytes, 0, bytes.length);
    }

    public final void write(byte[] bytes, int offset, int length) {
        grow(length);
        buffer.put(bytes, offset, length);
    }

    public final void write(ByteBuffer buffer) {
        grow(buffer.remaining());
        this.buffer.put(buffer);
    }

    public final void flip() {
        if (buffer.position() != 0) {
            buffer.flip();
        }
    }

    public final void rewind() {
        buffer.rewind();
    }

    public final byte[] toArray() {
        flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);
        return data;
    }

    public final ByteBufferStream duplicate() {
        return new ByteBufferStream(this.buffer.duplicate(), this.bufferPool);
    }

    public final boolean hasRemaining() {
        return buffer.hasRemaining();
    }

    public final int remaining() {
        return buffer.remaining();
    }

    public final int limit() {
        return buffer.limit();
    }

    public final void limit(int limit) {
        buffer.limit(limit);
    }

    public final int position() {
        return buffer.position();
    }

    public final void position(int position) {
        buffer.position(position);
    }

    public final boolean isDirect() {
        return buffer.isDirect();
    }

    public final byte[] array() {
        return buffer.array();
    }

    public final int arrayOffset() {
        return buffer.arrayOffset();
    }

    public final ByteBufferStream slice() {
        return new ByteBufferStream(buffer.slice(), this.bufferPool);
    }

    public final void readFrom(ByteChannel channel, int length)
        throws IOException {
        int readed = 0;
        grow(length);
        buffer.limit(buffer.position() + length);
        while (readed < length) {
            int curr = channel.read(buffer);
            if (curr == -1) {
                break;
            }
            readed += curr;
        }
        if (readed < length) {
            throw new IllegalStateException("Unexpected EOF");
        }
    }

    public final void readFrom(InputStream stream) throws IOException {
        byte[] bytes = new byte[8192];
        for (;;) {
            int length = stream.read(bytes);
            if (length == -1) {
                break;
            }
            write(bytes, 0, length);
        }
    }

    public final void writeTo(ByteChannel channel) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    public final void writeTo(OutputStream stream) throws IOException {
        if (buffer.hasArray()) {
            stream.write(
                buffer.array(),
                buffer.arrayOffset() + buffer.position(),
                buffer.remaining()
            );
        } else {
            byte[] bytes = new byte[8192];
            for (;;) {
                int length = read(bytes);
                if (length == -1) {
                    break;
                }
                stream.write(bytes, 0, length);
            }
        }
    }

    public final void close() {
        if (buffer != null) {
            this.bufferPool.release(buffer);
            buffer = null;
        }
    }

    public final InputStream getInputStream() {
        if (this.inputStream == null) {
            this.inputStream = new ByteBufferInputStream(this);
        }
        return this.inputStream;
    }

    public final OutputStream getOutputStream() {
        if (this.outputStream == null) {
            this.outputStream = new ByteBufferOutputStream(this);
        }
        return this.outputStream;
    }

    public ByteBufferPool getBufferPool() {
        return bufferPool;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}
