/*
 * Copyright (c) 2021, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.session;

import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import me.ixk.xkserver.exception.UnsupportedDeprecatedException;
import me.ixk.xkserver.utils.AutoLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Otstar Lin
 * @date 2020/12/5 下午 3:45
 */
public class Session implements HttpSession {

    private static final Logger log = LoggerFactory.getLogger(Session.class);

    public enum State {
        /**
         * 可用
         */
        VALID,
        /**
         * 已过期
         */
        INVALID,
        /**
         * 正在过期
         */
        INVALIDATING,
    }

    protected final SessionData data;
    protected final ServletContext context;

    protected volatile State state = State.VALID;
    protected final AutoLock lock = new AutoLock();

    public Session(final SessionData data, final ServletContext context) {
        this.data = data;
        this.context = context;
    }

    @Override
    public long getCreationTime() {
        try (final AutoLock lock = this.lock.lock()) {
            check();
            return data.getCreated();
        }
    }

    @Override
    public String getId() {
        try (final AutoLock lock = this.lock.lock()) {
            return data.getId();
        }
    }

    @Override
    public long getLastAccessedTime() {
        try (final AutoLock lock = this.lock.lock()) {
            check();
            return data.getLastAccessed();
        }
    }

    @Override
    public ServletContext getServletContext() {
        return context;
    }

    @Override
    public void setMaxInactiveInterval(final int interval) {
        try (final AutoLock lock = this.lock.lock()) {
            data.setMaxInactiveInterval(interval);
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        try (final AutoLock lock = this.lock.lock()) {
            final int interval = data.getMaxInactiveInterval();
            return interval < 0 ? -1 : interval;
        }
    }

    @Override
    public HttpSessionContext getSessionContext() {
        throw new UnsupportedDeprecatedException();
    }

    @Override
    public Object getAttribute(final String name) {
        try (final AutoLock lock = this.lock.lock()) {
            check();
            return data.getAttribute(name);
        }
    }

    @Override
    public Object getValue(final String name) {
        return this.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        try (final AutoLock lock = this.lock.lock()) {
            check();
            return data.getAttributeNames();
        }
    }

    @Override
    public String[] getValueNames() {
        try (final AutoLock lock = this.lock.lock()) {
            check();
            return data.getAttributeNameSet().toArray(String[]::new);
        }
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        try (final AutoLock lock = this.lock.lock()) {
            check();
            data.setAttribute(name, value);
            // TODO: 运行 SessionAttribute 监听器
        }
    }

    @Override
    public void putValue(final String name, final Object value) {
        this.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(final String name) {
        try (final AutoLock lock = this.lock.lock()) {
            check();
            data.removeAttribute(name);
        }
    }

    @Override
    public void removeValue(final String name) {
        this.removeAttribute(name);
    }

    @Override
    public void invalidate() {
        final boolean result = this.startInvalidate();
        try {
            if (result) {
                try {
                    // TODO: 运行 Session 结束监听器
                } finally {
                    this.endInvalidate();
                }
            }
        } catch (final Exception e) {
            log.info("Unable to invalidate Session {}", this, e);
        }
    }

    @Override
    public boolean isNew() {
        return false;
    }

    private boolean startInvalidate() {
        try (final AutoLock lock = this.lock.lock()) {
            switch (this.state) {
                case INVALID:
                    throw new IllegalStateException("Session already invalid");
                case INVALIDATING:
                    return false;
                case VALID:
                    this.state = State.INVALIDATING;
                    return true;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private void endInvalidate() {
        try (final AutoLock lock = this.lock.lock()) {
            try {
                if (
                    this.state == State.VALID ||
                        this.state == State.INVALIDATING
                ) {
                    for (final String key : data.getAttributeNameSet()) {
                        // TODO: 运行 SessionAttribute 监听器
                    }
                }
            } finally {
                this.state = State.INVALID;
            }
        }
    }

    private void check() {
        if (this.state == State.INVALID) {
            throw new IllegalStateException(
                "Invalid for read: " + data.getId()
            );
        }
    }
}
