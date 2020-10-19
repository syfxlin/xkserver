/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Otstar Lin
 * @date 2020/10/19 上午 11:26
 */
public class PollerManager extends AbstractLifeCycle {
    private final AtomicInteger pollerRouter = new AtomicInteger(0);
    private final Poller[] pollers;
    private final Executor executor;

    public PollerManager(final Executor executor, final int count) {
        this.executor = executor;
        this.pollers = new Poller[count];
    }

    public Selector newSelector() throws IOException {
        return Selector.open();
    }

    public Poller newPoller(int id) {
        return new Poller(id, this);
    }

    public Poller[] getPollers() {
        return this.pollers;
    }

    public Poller getPoller0() {
        final int index =
            Math.abs(this.pollerRouter.getAndIncrement()) % this.pollers.length;
        return this.pollers[index];
    }

    public void register(final SocketChannel channel)
        throws ClosedChannelException {
        this.getPoller0().register(channel);
    }

    public void execute(Runnable run) {
        this.executor.execute(run);
    }

    @Override
    public void doStart() throws Exception {
        for (int i = 0; i < this.pollers.length; i++) {
            this.pollers[i] = this.newPoller(i);
        }
        super.doStart();
    }
}
