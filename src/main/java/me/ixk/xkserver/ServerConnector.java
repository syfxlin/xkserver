/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Otstar Lin
 * @date 2020/10/16 下午 10:48
 */
@Slf4j
public class ServerConnector {
    private final ServerSocketChannel acceptChannel;
    private final Selector selector;

    public ServerConnector() {
        try {
            this.acceptChannel = ServerSocketChannel.open();
            this.acceptChannel.socket().bind(new InetSocketAddress(8080));
            this.selector = Selector.open();
        } catch (IOException e) {
            log.error("Error:", e);
            throw new RuntimeException(e);
        }
    }

    public static void main(final String[] args) {
        ServerConnector connector = new ServerConnector();
        while (true) {
            try {
                connector.accept();
            } catch (IOException e) {
                log.error("Err", e);
            }
        }
    }

    public void accept() throws IOException {
        ServerSocketChannel serverChannel = acceptChannel;
        if (serverChannel != null && serverChannel.isOpen()) {
            SocketChannel channel = serverChannel.accept();
            accepted(channel);
        }
    }

    private void accepted(SocketChannel channel) throws IOException {
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        final SelectionKey key = channel.register(
            this.selector,
            SelectionKey.OP_READ,
            this
        );
        while (true) {
            final int ready = this.selector.select();
            if (ready == 0) {
                continue;
            }
            final Set<SelectionKey> keys = this.selector.selectedKeys();
            final Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey item = iterator.next();
                iterator.remove();
                if (item.isReadable()) {
                    SocketChannel sc = (SocketChannel) item.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    sc.read(byteBuffer);
                    log.info("Msg: {}", new String(byteBuffer.array()));
                }
            }
        }
    }
}
