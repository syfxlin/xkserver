/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import me.ixk.xkserver.pool.ExecutionStrategy;

/**
 * @author Otstar Lin
 * @date 2020/10/19 上午 11:12
 */
@Slf4j
public class Poller extends AbstractLifeCycle implements Runnable {
    private final int id;
    private volatile String name;
    private final PollerManager pollerManager;
    private volatile Selector selector;
    private final SelectorProducer producer;

    public Poller(final int id, final PollerManager pollerManager) {
        this.id = id;
        this.pollerManager = pollerManager;
        this.producer = new SelectorProducer();
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();
        this.selector = this.pollerManager.newSelector();
        this.pollerManager.execute(this);
    }

    public void register(final SocketChannel channel)
        throws ClosedChannelException {
        channel.register(
            this.selector,
            SelectionKey.OP_READ,
            new Accept(channel)
        );
        log.info("Register");
        this.selector.wakeup();
    }

    public int select(Selector selector) {
        try {
            int selected = selector.select();
            if (selected == 0) {
                log.debug("Selector {} woken with none selected", selector);
            }
            return selected;
        } catch (IOException e) {
            // TODO: 异常处理
            log.error("Select error", e);
        }
        return 0;
    }

    @Override
    public void run() {
        final Thread thread = Thread.currentThread();
        String name = thread.getName();
        this.name = String.format("poller-%d-%s", this.id, name);
        thread.setName(this.name);

        try {
            while (this.isRunning()) {
                final Runnable produce = this.producer.produce();
                if (produce != null) {
                    produce.run();
                    // this.pollerManager.execute(produce);
                }
            }
        } catch (final Throwable e) {
            log.error("Poller error", e);
        } finally {
            thread.setName(name);
        }
    }

    private class SelectorProducer implements ExecutionStrategy.Producer {
        private volatile Set<SelectionKey> keys = Collections.emptySet();
        private volatile Iterator<SelectionKey> iterator = Collections.emptyIterator();

        @Override
        public Runnable produce() {
            while (true) {
                Runnable task = this.processSelected();
                // 当前有任务，执行
                if (task != null) {
                    return task;
                }

                if (!this.select()) {
                    return null;
                }
            }
        }

        private boolean select() {
            try {
                Selector selector = Poller.this.selector;
                if (selector != null) {
                    int selected = Poller.this.select(selector);
                    if (selected != 0) {
                        selector = Poller.this.selector;
                        if (selector != null) {
                            this.keys = selector.selectedKeys();
                            this.iterator =
                                this.keys.isEmpty()
                                    ? Collections.emptyIterator()
                                    : keys.iterator();
                            return true;
                        }
                    }
                }
            } catch (Throwable e) {
                // TODO: 异常处理
                log.error("Select error", e);
            }
            return false;
        }

        private Runnable processSelected() {
            while (this.iterator.hasNext()) {
                SelectionKey key = this.iterator.next();
                this.iterator.remove();
                Object attachment = key.attachment();
                SelectableChannel channel = key.channel();
                if (key.isValid()) {
                    if (attachment instanceof Selectable) {
                        Runnable task =
                            ((Selectable) attachment).selected(key, channel);
                        if (task != null) {
                            return task;
                        }
                    } else {
                        throw new IllegalStateException(
                            "key=" + key + "att=" + attachment
                        );
                    }
                }
            }
            return null;
        }
    }

    public interface Selectable {
        /**
         * Select 任务
         *
         * @param key     SelectionKey
         * @param channel SelectableChannel
         *
         * @return 任务
         */
        Runnable selected(SelectionKey key, SelectableChannel channel);
    }

    public class Accept implements Selectable {
        private final SocketChannel channel;

        public Accept(SocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public Runnable selected(SelectionKey key, SelectableChannel channel) {
            return () -> {
                try {
                    // final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    // sc.read(byteBuffer);
                    // log.info("Msg: {}", new String(byteBuffer.array()).trim());
                    log.info("Poller: {}", Poller.this.id);
                    this.channel.write(
                            ByteBuffer.wrap(
                                (
                                    "HTTP/1.1 200 OK\n" +
                                    "Content-Length: 5\n" +
                                    "Date: Mon, 19 Oct 2020 05:10:08 GMT\n" +
                                    "Expires: Thu, 01 Jan 1970 00:00:00 GMT\n" +
                                    "Server: Jetty(9.4.30.v20200611)\n" +
                                    "Set-Cookie: JSESSIONID=node01ddhx5zo238k11hwjj6pwus3ax0.node0; Path=/\n" +
                                    "\n" +
                                    "post" +
                                    Poller.this.id
                                ).getBytes(StandardCharsets.UTF_8)
                            )
                        );
                    this.channel.close();
                } catch (IOException e) {
                    log.error("Read error", e);
                }
            };
        }
    }
}
