/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

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
public class Poller extends AbstractLifeCycle implements Runnable {
    private final int id;
    private volatile String name;
    private final PollerManager pollerManager;
    private volatile Selector selector;

    public Poller(final int id, final PollerManager pollerManager) {
        this.id = id;
        this.pollerManager = pollerManager;
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();
        this.selector = this.pollerManager.newSelector();
        this.pollerManager.execute(this);
    }

    public void register(final SocketChannel channel)
        throws ClosedChannelException {
        channel.register(this.selector, SelectionKey.OP_READ);
        log.info("Register");
        this.selector.wakeup();
    }

    @Override
    public void run() {
        final Thread thread = Thread.currentThread();
        String name = thread.getName();
        this.name = String.format("poller-%d-%s", this.id, name);
        thread.setName(this.name);

        try {
            while (this.isRunning()) {
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
                        log.info("Poller: {}", this.id);
                        sc.write(
                            ByteBuffer.wrap(
                                (
                                    "HTTP/1.1 200 OK\n" +
                                    "Content-Length: 5\n" +
                                    "Date: Mon, 19 Oct 2020 05:10:08 GMT\n" +
                                    "Expires: Thu, 01 Jan 1970 00:00:00 GMT\n" +
                                    "Server: Jetty(9.4.30.v20200611)\n" +
                                    "Set-Cookie: JSESSIONID=node01ddhx5zo238k11hwjj6pwus3ax0.node0; Path=/\n" +
                                    "\n" +
                                    "post" +
                                    this.id
                                ).getBytes(StandardCharsets.UTF_8)
                            )
                        );
                        sc.close();
                    }
                }
            }
        } catch (final Throwable e) {
            log.error("Poller error", e);
        } finally {
            thread.setName(name);
        }
    }
}
