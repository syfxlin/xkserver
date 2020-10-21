/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 生命周期抽象类
 *
 * @author Otstar Lin
 * @date 2020/10/19 下午 6:44
 */
public abstract class AbstractLifeCycle implements LifeCycle {
    protected final AutoLock lock = new AutoLock();
    private volatile State state = State.STOPPED;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public void doStart() throws Exception {}

    public void doStop() throws Exception {}

    @Override
    public void init() throws Exception {}

    @Override
    public void start() throws Exception {
        try (AutoLock l = lock.lock()) {
            try {
                switch (this.state) {
                    case STARTED:
                        return;
                    case STARTING:
                    case STOPPING:
                        throw new IllegalStateException(
                            this.getState().toString()
                        );
                    default:
                        try {
                            setStarting();
                            doStart();
                            setStarted();
                        } catch (RuntimeException e) {
                            setStopping();
                            doStop();
                            setStarted();
                        }
                }
            } catch (Throwable e) {
                setFailed(e);
                throw e;
            }
        }
    }

    @Override
    public void stop() throws Exception {
        try (AutoLock l = lock.lock()) {
            try {
                switch (this.state) {
                    case STOPPED:
                        return;
                    case STARTING:
                    case STOPPING:
                        throw new IllegalStateException(
                            this.getState().toString()
                        );
                    default:
                        setStopping();
                        doStop();
                        setStopped();
                }
            } catch (Throwable e) {
                setFailed(e);
                throw e;
            }
        }
    }

    @Override
    public void destroy() throws Exception {}

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public boolean addListener(final Listener listener) {
        if (this.listeners.contains(listener)) {
            return false;
        }
        this.listeners.add(listener);
        return true;
    }

    @Override
    public boolean removeListener(final Listener listener) {
        return this.listeners.remove(listener);
    }

    void setStarted() {
        if (this.state == State.STARTING) {
            this.state = State.STARTED;
            for (final Listener listener : this.listeners) {
                listener.lifeCycleStarted(this);
            }
        }
    }

    void setStarting() {
        this.state = State.STARTING;
        for (final Listener listener : this.listeners) {
            listener.lifeCycleStarting(this);
        }
    }

    void setStopping() {
        this.state = State.STOPPING;
        for (final Listener listener : this.listeners) {
            listener.lifeCycleStopping(this);
        }
    }

    void setStopped() {
        if (this.state == State.STOPPING) {
            this.state = State.STOPPED;
            for (final Listener listener : this.listeners) {
                listener.lifeCycleStopped(this);
            }
        }
    }

    void setFailed(final Throwable e) {
        this.state = State.FAILED;
        for (final Listener listener : this.listeners) {
            listener.lifeCycleFailure(this, e);
        }
    }
}
