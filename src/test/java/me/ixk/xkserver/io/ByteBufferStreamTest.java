/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * @author Otstar Lin
 * @date 2020/11/21 上午 1:09
 */
class ByteBufferStreamTest {

    @Test
    void read() {
        final ByteBufferStream stream = ByteBufferStream.wrap(
            ("Hello").getBytes()
        );
        assertEquals('H', stream.read());
    }

    @Test
    void testRead() {
        final ByteBufferStream stream = ByteBufferStream.wrap(
            ("Hello").getBytes()
        );
        final byte[] bytes = new byte[2];
        stream.read(bytes);
        assertArrayEquals(new byte[] { 'H', 'e' }, bytes);
    }

    @Test
    void testRead1() {
        final ByteBufferStream stream = ByteBufferStream.wrap(
            ("Hello").getBytes()
        );
        final byte[] bytes = new byte[6];
        stream.read(bytes, 2, 2);
        assertArrayEquals(new byte[] { 0, 0, 'H', 'e', 0, 0 }, bytes);
    }

    @Test
    void testRead2() {
        final ByteBufferStream stream = ByteBufferStream.wrap(
            ("Hello").getBytes()
        );
        final ByteBuffer buffer = ByteBuffer.allocate(2);
        stream.read(buffer);
        buffer.flip();
        assertEquals("He", StrUtil.str(buffer, StandardCharsets.UTF_8));
    }

    @Test
    void skip() {
        final ByteBufferStream stream = ByteBufferStream.wrap(
            ("Hello").getBytes()
        );
        stream.skip(1);
        assertEquals('e', stream.read());
    }

    @Test
    void available() {
        final ByteBufferStream stream = ByteBufferStream.wrap(
            ("Hello").getBytes()
        );
        assertEquals(5, stream.available());
        stream.skip(5);
        assertEquals(0, stream.available());
    }

    @Test
    void write() {
        final ByteBufferStream stream = new ByteBufferStream();
        stream.write('H');
        stream.flip();
        assertEquals('H', stream.read());
    }

    @Test
    void testWrite() {
        final ByteBufferStream stream = new ByteBufferStream();
        stream.write(("Hello").getBytes());
        stream.flip();
        assertEquals('H', stream.read());
    }

    @Test
    void testWrite1() {
        final ByteBufferStream stream = new ByteBufferStream();
        stream.write(("Hello").getBytes(), 1, 1);
        stream.flip();
        assertEquals('e', stream.read());
    }

    @Test
    void testWrite2() {
        final ByteBufferStream stream = new ByteBufferStream();
        stream.write(ByteBuffer.wrap(("Hello").getBytes()));
        stream.flip();
        assertEquals('H', stream.read());
    }

    @Test
    void toArray() {
        final byte[] bytes = ("Hello").getBytes();
        final ByteBufferStream stream = ByteBufferStream.wrap(bytes);
        assertArrayEquals(bytes, stream.toArray());
    }

    @Test
    void readFrom() {
        final ByteBufferStream stream = new ByteBufferStream();
        try {
            stream.readFrom(new ByteArrayInputStream(("Hello").getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stream.flip();
        assertEquals('H', stream.read());
    }

    @Test
    void writeTo() {
        final ByteBufferStream stream = ByteBufferStream.wrap(
            ("Hello").getBytes()
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            stream.writeTo(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals("Hello", out.toString());
    }

    @Test
    void close() {
        final ByteBufferStream stream = ByteBufferStream.allocate(1024);
        stream.write(("Hello").getBytes());
        assertEquals(0L, stream.getBufferPool().getMemory(false).get());
        stream.close();
        assertEquals(1024L, stream.getBufferPool().getMemory(false).get());
    }

    @Test
    void getInputStream() {
        ByteBufferStream stream = ByteBufferStream.wrap(("Hello").getBytes());
        assertEquals(
            "Hello",
            IoUtil.read(stream.getInputStream(), StandardCharsets.UTF_8)
        );
    }

    @Test
    void getOutputStream() throws IOException {
        ByteBufferStream stream = ByteBufferStream.allocate(1024);
        final byte[] bytes = ("Hello").getBytes();
        stream.getOutputStream().write(bytes);
        assertArrayEquals(bytes, stream.toArray());
    }

    @Test
    void grow() {
        ByteBufferStream stream = ByteBufferStream.allocate(1024);
        stream.write(StrUtil.repeat('A', 1048576 * 4 + 1).getBytes());
        assertEquals(1048576 * 8, stream.getBuffer().capacity());
    }
}
