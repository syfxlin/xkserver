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
     * 获取线程池
     *
     * @return 线程池
     */
    Executor getExecutor();
}
