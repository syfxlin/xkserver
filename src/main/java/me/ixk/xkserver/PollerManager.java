/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Otstar Lin
 * @date 2020/10/19 上午 11:26
 */
public class PollerManager {
    private final AtomicInteger pollerRouter = new AtomicInteger(0);
    private final Poller[] pollers;

    public PollerManager(int count) {
        this.pollers = new Poller[count];
        for (int i = 0; i < count; i++) {
            this.pollers[i] = new Poller(i);
        }
    }

    public Poller[] getPollers() {
        return this.pollers;
    }

    public Poller getPoller0() {
        int index =
            Math.abs(this.pollerRouter.getAndIncrement()) % this.pollers.length;
        return this.pollers[index];
    }

    public void register(SocketChannel channel) throws ClosedChannelException {
        this.getPoller0().register(channel);
    }
}
