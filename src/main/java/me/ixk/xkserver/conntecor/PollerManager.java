/*
 * Copyright (c) 2021, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.conntecor;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import me.ixk.xkserver.life.AbstractLifeCycle;

/**
 * @author Otstar Lin
 * @date 2020/10/19 上午 11:26
 */
public class PollerManager extends AbstractLifeCycle {

    private final AtomicInteger pollerRouter = new AtomicInteger(0);
    private final Poller[] pollers;
    private final Executor executor;

    public PollerManager(final Connector connector, final int count) {
        this.executor = connector.getExecutor();
        this.pollers = new Poller[count];
    }

    public Executor getExecutor() {
        return executor;
    }

    public Selector newSelector() throws IOException {
        return Selector.open();
    }

    public Poller newPoller(final int id) {
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
        final Poller poller = this.getPoller0();
        poller.submit(poller.new Accept(channel));
    }

    public void execute(final Runnable run) {
        this.executor.execute(run);
    }

    @Override
    public void doStart() throws Exception {
        for (int i = 0; i < this.pollers.length; i++) {
            this.pollers[i] = this.newPoller(i);
            this.pollers[i].start();
        }
        super.doStart();
    }
}
