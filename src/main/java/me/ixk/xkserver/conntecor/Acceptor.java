/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.conntecor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import me.ixk.xkserver.life.AbstractLifeCycle;

/**
 * @author Otstar Lin
 * @date 2020/10/19 上午 10:57
 */
@Slf4j
public class Acceptor extends AbstractLifeCycle implements Runnable {
    private volatile String name;
    private final ServerSocketChannel acceptChannel;
    private final PollerManager pollerManager;

    public Acceptor(final PollerManager manager) {
        this.pollerManager = manager;
        try {
            this.acceptChannel = ServerSocketChannel.open();
            this.acceptChannel.socket().bind(new InetSocketAddress(8080));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void doStart() throws Exception {
        this.pollerManager.execute(this);
    }

    public void accept() throws IOException {
        if (this.acceptChannel != null && this.acceptChannel.isOpen()) {
            final SocketChannel channel = this.acceptChannel.accept();
            this.accepted(channel);
        }
    }

    public void accepted(SocketChannel channel) throws IOException {
        if (channel != null && channel.isOpen()) {
            channel.configureBlocking(false);
            this.pollerManager.register(channel);
        }
    }

    @Override
    public void run() {
        final Thread thread = Thread.currentThread();
        String name = thread.getName();
        this.name = String.format("acceptor-%s-%d", name, hashCode());
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
            return String.format("acceptor@%x", hashCode());
        }
        return name;
    }
}
