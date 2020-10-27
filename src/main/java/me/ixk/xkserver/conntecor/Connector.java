/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.conntecor;

import java.util.concurrent.Executor;

/**
 * @author Otstar Lin
 * @date 2020/10/19 下午 7:37
 */
public interface Connector {
    /**
     * 获取服务器
     *
     * @return 服务器
     */
    Server getServer();

    /**
     * 获取线程池
     *
     * @return 线程池
     */
    Executor getExecutor();

    /**
     * 获取 Poller 管理器
     *
     * @return Poller 管理器
     */
    PollerManager getPollerManager();

    /**
     * 获取连接器
     *
     * @return 连接器
     */
    Acceptor getAcceptor();
}
