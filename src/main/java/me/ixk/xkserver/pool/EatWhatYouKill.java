/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.pool;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import me.ixk.xkserver.life.AbstractLifeCycle;
import me.ixk.xkserver.utils.AutoLock;

/**
 * 已知 Bug，因分配策略存在问题导致所有 Selector 被占用的时候无法接受连接
 *
 * @author Otstar Lin
 * @date 2020/10/20 下午 6:07
 */
@Slf4j
public class EatWhatYouKill
    extends AbstractLifeCycle
    implements ExecutionStrategy, Runnable {

    private enum State {
        /**
         * 空闲
         */
        IDLE,
        /**
         * 生产中
         */
        PRODUCING,
        /**
         * 再次生产中
         */
        REPRODUCING,
    }

    private enum Mode {
        /**
         * 生产-消费分离
         */
        PRODUCE_EXECUTE_CONSUME,
        /**
         * 生产-消费不分离
         */
        EXECUTE_PRODUCE_CONSUME,
    }

    private final AutoLock lock = new AutoLock();
    private final Producer producer;
    private final TryExecutor executor;
    private volatile State state = State.IDLE;

    public EatWhatYouKill(final Producer producer, final Executor executor) {
        this.producer = producer;
        this.executor = TryExecutor.asTryExecutor(executor);
        try {
            this.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        this.execute();
    }

    @Override
    public void execute() {
        try (final AutoLock l = lock.lock()) {
            switch (this.state) {
                case IDLE:
                    this.state = State.PRODUCING;
                    break;
                case PRODUCING:
                    this.state = State.REPRODUCING;
                    return;
                default:
                    return;
            }
        }

        while (this.isRunning()) {
            try {
                if (this.doProduce()) {
                    continue;
                }
                return;
            } catch (final Throwable th) {
                log.warn("Unable to produce", th);
            }
        }
    }

    private boolean doProduce() {
        final Runnable task = this.produceTask();
        if (task == null) {
            try (final AutoLock l = this.lock.lock()) {
                switch (this.state) {
                    case PRODUCING:
                        this.state = State.IDLE;
                        return false;
                    case REPRODUCING:
                        this.state = State.PRODUCING;
                        return true;
                    default:
                        throw new IllegalStateException("State: " + this.state);
                }
            }
        }
        final Mode mode;

        try (final AutoLock l = this.lock.lock()) {
            if (this.executor.tryExecute(this)) {
                this.state = State.IDLE;
                mode = Mode.EXECUTE_PRODUCE_CONSUME;
            } else {
                mode = Mode.PRODUCE_EXECUTE_CONSUME;
            }
        }

        switch (mode) {
            case PRODUCE_EXECUTE_CONSUME:
                this.executeTask(task);
                return true;
            case EXECUTE_PRODUCE_CONSUME:
                this.runTask(task);
                try (AutoLock l = this.lock.lock()) {
                    if (this.state == State.IDLE) {
                        this.state = State.PRODUCING;
                        return true;
                    }
                }
                return false;
            default:
                throw new IllegalStateException("State: " + this.state);
        }
    }

    private Runnable produceTask() {
        return this.producer.produce();
    }

    private void executeTask(final Runnable task) {
        this.executor.execute(task);
    }

    private void runTask(final Runnable task) {
        task.run();
    }
}
