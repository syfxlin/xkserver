/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Otstar Lin
 * @date 2020/10/19 上午 10:57
 */
@Slf4j
public class Acceptor implements Runnable {
    private final int id;
    private String name;
    private final ServerSocketChannel acceptChannel;
    private final PollerManager pollerManager;

    public Acceptor(final int id, final PollerManager manager) {
        this.id = id;
        this.pollerManager = manager;
        try {
            this.acceptChannel = ServerSocketChannel.open();
            this.acceptChannel.socket().bind(new InetSocketAddress(8080));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void accept() throws IOException {
        final ServerSocketChannel serverChannel = acceptChannel;
        if (serverChannel != null && serverChannel.isOpen()) {
            final SocketChannel channel = serverChannel.accept();
            accepted(channel);
        }
    }

    public void accepted(SocketChannel channel) throws IOException {
        channel.configureBlocking(false);
        this.pollerManager.register(channel);
    }

    @Override
    public void run() {
        final Thread thread = Thread.currentThread();
        String name = thread.getName();
        this.name = String.format("acceptor-%d-%s", this.id, name);
        thread.setName(this.name);

        try {
            while (true) {
                try {
                    this.accept();
                } catch (Throwable e) {
                    log.error("Accept error", e);
                }
            }
        } finally {
            thread.setName(name);
        }
    }

    @Override
    public String toString() {
        String name = this.name;
        if (name == null) {
            return String.format("acceptor-%d@%x", this.id, hashCode());
        }
        return name;
    }
}
