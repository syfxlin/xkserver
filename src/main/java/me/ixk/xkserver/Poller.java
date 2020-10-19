/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Otstar Lin
 * @date 2020/10/19 上午 11:12
 */
@Slf4j
public class Poller implements Runnable {
    private final int id;
    private final Selector selector;

    public Poller(final int id) {
        this.id = id;
        try {
            this.selector = Selector.open();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void register(final SocketChannel channel)
        throws ClosedChannelException {
        channel.register(this.selector, SelectionKey.OP_READ);
        log.info("Register");
        this.selector.wakeup();
    }

    @Override
    public void run() {
        try {
            while (true) {
                final int ready = this.selector.select();
                log.info("Select");
                if (ready == 0) {
                    continue;
                }
                final Set<SelectionKey> keys = this.selector.selectedKeys();
                final Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    final SelectionKey item = iterator.next();
                    iterator.remove();
                    if (item.isReadable()) {
                        final SocketChannel sc = (SocketChannel) item.channel();
                        final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        sc.read(byteBuffer);
                        log.info(
                            "Msg: {}",
                            new String(byteBuffer.array()).trim()
                        );
                        sc.write(
                            ByteBuffer.wrap(
                                (
                                    "HTTP/1.1 200 OK\n" +
                                    "Content-Length: 4\n" +
                                    "Date: Mon, 19 Oct 2020 05:10:08 GMT\n" +
                                    "Expires: Thu, 01 Jan 1970 00:00:00 GMT\n" +
                                    "Server: Jetty(9.4.30.v20200611)\n" +
                                    "Set-Cookie: JSESSIONID=node01ddhx5zo238k11hwjj6pwus3ax0.node0; Path=/\n" +
                                    "\n" +
                                    "post"
                                ).getBytes(StandardCharsets.UTF_8)
                            )
                        );
                        sc.close();
                    }
                }
            }
        } catch (final Throwable e) {
            log.error("Poller error", e);
        }
    }
}
