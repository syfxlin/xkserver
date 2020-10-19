/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver;

import java.util.EventListener;

/**
 * 生命周期接口
 *
 * @author Otstar Lin
 * @date 2020/10/19 下午 6:31
 */
public interface LifeCycle {
    enum State {
        /**
         * 已停止
         */
        STOPPED,
        /**
         * 正在启动
         */
        STARTING,
        /**
         * 已启动
         */
        STARTED,
        /**
         * 正在停止
         */
        STOPPING,
        /**
         * 已失败
         */
        FAILED,
    }

    /**
     * 初始化
     *
     * @throws Exception 异常
     */
    void init() throws Exception;

    /**
     * 启动
     *
     * @throws Exception 异常
     */
    void start() throws Exception;

    /**
     * 停止
     *
     * @throws Exception 异常
     */
    void stop() throws Exception;

    /**
     * 销毁
     *
     * @throws Exception 异常
     */
    void destroy() throws Exception;

    /**
     * 获取当前状态
     *
     * @return 当前状态
     */
    State getState();

    /**
     * 是否正在运行
     *
     * @return 是否正在运行
     */
    default boolean isRunning() {
        switch (this.getState()) {
            case STARTING:
            case STARTED:
                return true;
            default:
                return false;
        }
    }

    /**
     * 是否已启动
     *
     * @return 是否已启动
     */
    default boolean isStarted() {
        return this.getState() == State.STARTED;
    }

    /**
     * 是否正在启动
     *
     * @return 是否正在启动
     */
    default boolean isStarting() {
        return this.getState() == State.STARTING;
    }

    /**
     * 是否正在停止
     *
     * @return 是否正在停止
     */
    default boolean isStopping() {
        return this.getState() == State.STOPPING;
    }

    /**
     * 是否已停止
     *
     * @return 是否已停止
     */
    default boolean isStopped() {
        return this.getState() == State.STOPPED;
    }

    /**
     * 是否已失败
     *
     * @return 是否已失败
     */
    default boolean isFailed() {
        return this.getState() == State.FAILED;
    }

    /**
     * 添加监听器
     *
     * @param listener 监听器
     *
     * @return 是否添加成功
     */
    boolean addListener(Listener listener);

    /**
     * 删除监听器
     *
     * @param listener 监听器
     *
     * @return 是否删除成功
     */
    boolean removeListener(Listener listener);

    interface Listener extends EventListener {
        /**
         * 正在启动监听器
         *
         * @param event 事件
         */
        default void lifeCycleStarting(LifeCycle event) {}

        /**
         * 已启动监听器
         *
         * @param event 事件
         */
        default void lifeCycleStarted(LifeCycle event) {}

        /**
         * 失败监听器
         *
         * @param event 事件
         * @param cause 异常
         */
        default void lifeCycleFailure(LifeCycle event, Throwable cause) {}

        /**
         * 正在停止监听器
         *
         * @param event 事件
         */
        default void lifeCycleStopping(LifeCycle event) {}

        /**
         * 已停止监听器
         *
         * @param event 事件
         */
        default void lifeCycleStopped(LifeCycle event) {}
    }
}
