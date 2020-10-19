/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

/**
 * @author Otstar Lin
 * @date 2020/10/19 上午 11:31
 */
public class Server {
    private final ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
        4,
        8,
        0L,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(30),
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
        this.manager = new PollerManager(this.poolExecutor, 2);
        this.acceptor = new Acceptor(1, this.manager);
    }

    public void start() {
        poolExecutor.execute(this.acceptor);
        try {
            this.manager.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (Poller poller : this.manager.getPollers()) {
            try {
                poller.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
