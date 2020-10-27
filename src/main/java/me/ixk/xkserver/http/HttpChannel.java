/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import me.ixk.xkserver.conntecor.Connector;
import me.ixk.xkserver.conntecor.Poller;

/**
 * HttpChannel
 *
 * @author Otstar Lin
 * @date 2020/10/27 上午 9:26
 */
public class HttpChannel {
    private final Poller poller;
    private final SelectionKey selectionKey;
    private final SocketChannel channel;
    private final Connector connector;
    private final Executor executor;

    public HttpChannel(
        Connector connector,
        Poller poller,
        SelectionKey selectionKey
    ) {
        this.connector = connector;
        this.poller = poller;
        this.selectionKey = selectionKey;
        this.channel = (SocketChannel) selectionKey.channel();
        this.executor = connector.getExecutor();
    }
}
