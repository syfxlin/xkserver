/*
 * Copyright (c) 2021, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.conntecor;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import me.ixk.xkserver.life.AbstractLifeCycle;

/**
 * @author Otstar Lin
 * @date 2020/10/16 下午 10:48
 */
@Slf4j
public class ServerConnector extends AbstractLifeCycle implements Connector {

    private final PollerManager pollerManager;
    private final Acceptor acceptor;
    private final Executor executor;
    private final Server server;

    public ServerConnector(final Server server, final int pollerCount) {
        this.server = server;
        this.executor = server.getExecutor();
        this.pollerManager = new PollerManager(this, pollerCount);
        this.acceptor = new Acceptor(this);
    }

    @Override
    public void doStart() throws Exception {
        this.acceptor.start();
        this.pollerManager.start();
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public Executor getExecutor() {
        return this.executor;
    }

    @Override
    public PollerManager getPollerManager() {
        return pollerManager;
    }

    @Override
    public Acceptor getAcceptor() {
        return acceptor;
    }
}
