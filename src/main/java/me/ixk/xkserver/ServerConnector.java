/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Otstar Lin
 * @date 2020/10/16 下午 10:48
 */
@Slf4j
public class ServerConnector implements Connector {

    @Override
    public Executor getExecutor() {
        return null;
    }
}
