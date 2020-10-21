/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import me.ixk.xkserver.pool.ThreadPoolExecutor;

/**
 * @author Otstar Lin
 * @date 2020/10/19 上午 11:31
 */
public class Server {
    private final ThreadPoolExecutor poolExecutor = ThreadPoolExecutor.create(
        8,
        18,
        0L,
        TimeUnit.MILLISECONDS,
        100,
        r -> {
            final Thread thread = new Thread(r);
            thread.setName(String.format("tpe-%d", thread.hashCode()));
            return thread;
        },
        new AbortPolicy()
    );

    private final PollerManager manager;
    private final Acceptor acceptor;

    public Server() {
        this.manager = new PollerManager(this.poolExecutor, 4);
        this.acceptor = new Acceptor(1, this.manager);
    }

    public void start() {
        try {
            this.acceptor.start();
            this.manager.start();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(final String[] args) {
        final Server server = new Server();
        server.start();
    }
}
