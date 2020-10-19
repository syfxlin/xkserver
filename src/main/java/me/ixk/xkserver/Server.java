/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
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
        new LinkedBlockingQueue<Runnable>(30),
        new ThreadFactory() {

            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(r);
            }
        },
        new AbortPolicy()
    );

    private final PollerManager manager;
    private final Acceptor acceptor;

    public Server() {
        this.manager = new PollerManager(2);
        this.acceptor = new Acceptor(1, this.manager);
    }

    public void start() {
        poolExecutor.execute(this.acceptor);
        for (Poller poller : this.manager.getPollers()) {
            poolExecutor.execute(poller);
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
