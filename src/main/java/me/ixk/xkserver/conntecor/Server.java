/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.conntecor;

import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import me.ixk.xkserver.life.AbstractLifeCycle;
import me.ixk.xkserver.pool.ThreadPoolExecutor;

/**
 * @author Otstar Lin
 * @date 2020/10/19 上午 11:31
 */
public class Server extends AbstractLifeCycle {
    private final ThreadPoolExecutor executor = ThreadPoolExecutor.create(
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

    private final ServerConnector connector;

    public Server() {
        this.connector = new ServerConnector(this, 4);
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    @Override
    public void doStart() throws Exception {
        this.connector.start();
    }

    public static void main(final String[] args) {
        final Server server = new Server();
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
