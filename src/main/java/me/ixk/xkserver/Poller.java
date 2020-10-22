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
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import lombok.extern.slf4j.Slf4j;
import me.ixk.xkserver.pool.EatWhatYouKill;
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
    private final ExecutionStrategy strategy;
    private volatile Deque<SelectUpdate> updates = new ConcurrentLinkedDeque<>();
    private volatile Deque<SelectUpdate> updateable = new ConcurrentLinkedDeque<>();
    private final AutoLock lock = new AutoLock();

    public Poller(final int id, final PollerManager pollerManager) {
        this.id = id;
        this.pollerManager = pollerManager;
        this.producer = new SelectorProducer();
        this.strategy =
            new EatWhatYouKill(this.producer, this.pollerManager.getExecutor());
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();
        this.selector = this.pollerManager.newSelector();
        this.pollerManager.execute(this);
    }

    public void submit(SelectUpdate update) {
        this.updates.add(update);
        if (this.selector != null) {
            this.selector.wakeup();
        }
    }

    public Set<SelectionKey> select() {
        Selector selector = this.selector;
        if (selector == null) {
            return Collections.emptySet();
        }
        try {
            final int selected = selector.select();
            if (selected == 0) {
                log.debug("Selector {} woken with none selected", selector);
            }
            return selected == 0
                ? Collections.emptySet()
                : selector.selectedKeys();
        } catch (final IOException e) {
            // TODO: 异常处理
            log.error("Select error", e);
        }
        return Collections.emptySet();
    }

    @Override
    public void run() {
        final Thread thread = Thread.currentThread();
        final String name = thread.getName();
        this.name = String.format("poller-%d-%s", this.id, name);
        thread.setName(this.name);

        try {
            // while (this.isRunning()) {
            //     final Runnable produce = this.producer.produce();
            //     if (produce != null) {
            //         // 临时模拟 EatWhatYouKill
            //         // produce.run();
            //         this.pollerManager.execute(produce);
            //     }
            // }
            this.pollerManager.execute(this.strategy::execute);
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
                final Runnable task = this.processSelected();
                // 当前有任务，执行
                if (task != null) {
                    return task;
                }

                this.processUpdates();

                if (!this.select()) {
                    return null;
                }
            }
        }

        private boolean select() {
            try {
                final Set<SelectionKey> selectionKeys = Poller.this.select();
                if (selector != null) {
                    this.keys = selectionKeys;
                    this.iterator =
                        selectionKeys.isEmpty()
                            ? Collections.emptyIterator()
                            : selectionKeys.iterator();
                    return true;
                }
            } catch (final Throwable e) {
                // TODO: 异常处理
                log.error("Select error", e);
            }
            return false;
        }

        private Runnable processSelected() {
            while (this.iterator.hasNext()) {
                final SelectionKey key = this.iterator.next();
                this.iterator.remove();
                final Object attachment = key.attachment();
                final SelectableChannel channel = key.channel();
                if (key.isValid()) {
                    // 取消 Selector 的监听状态，用以解决无限循环 select 的问题
                    key.interestOps(0);
                    if (attachment instanceof Selectable) {
                        final Runnable task =
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

        private void processUpdates() {
            try (final AutoLock l = Poller.this.lock.lock()) {
                final Deque<SelectUpdate> updates = Poller.this.updates;
                Poller.this.updates = Poller.this.updateable;
                Poller.this.updateable = updates;
            }
            for (final SelectUpdate update : Poller.this.updateable) {
                if (Poller.this.selector != null) {
                    update.update(Poller.this.selector);
                }
            }
            Poller.this.updateable.clear();
            if (
                Poller.this.selector != null && !Poller.this.updates.isEmpty()
            ) {
                Poller.this.selector.wakeup();
            }
        }
    }

    public interface SelectUpdate {
        /**
         * 更新任务
         *
         * @param selector 选择器
         */
        void update(Selector selector);
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

    public class Accept implements Selectable, SelectUpdate {
        private final SocketChannel channel;

        public Accept(SocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public Runnable selected(
            final SelectionKey key,
            final SelectableChannel channel
        ) {
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
                    try {
                        // 模拟业务阻塞
                        Thread.sleep(50);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.channel.close();
                } catch (final IOException e) {
                    log.error("Read error", e);
                }
            };
        }

        @Override
        public void update(Selector selector) {
            try {
                this.channel.register(
                        selector,
                        SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                        this
                    );
            } catch (ClosedChannelException e) {
                log.error("Update error", e);
            }
        }
    }
}
