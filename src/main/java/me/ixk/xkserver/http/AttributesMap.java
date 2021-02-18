/*
 * Copyright (c) 2021, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Otstar Lin
 * @date 2020/11/28 下午 2:57
 */
public class AttributesMap implements Attributes {

    private final AtomicReference<ConcurrentMap<String, Object>> map = new AtomicReference<>();

    protected ConcurrentMap<String, Object> map() {
        return this.map.get();
    }

    protected ConcurrentMap<String, Object> ensureMap() {
        while (true) {
            ConcurrentMap<String, Object> concurrentMap = this.map();
            if (concurrentMap != null) {
                return concurrentMap;
            }
            concurrentMap = new ConcurrentHashMap<>(16);
            if (this.map.compareAndSet(null, concurrentMap)) {
                return concurrentMap;
            }
        }
    }

    @Override
    public void removeAttribute(final String name) {
        final ConcurrentMap<String, Object> map = this.map();
        if (map != null) {
            map.remove(name);
        }
    }

    @Override
    public void setAttribute(final String name, final Object attribute) {
        if (attribute == null) {
            this.removeAttribute(name);
        } else {
            this.ensureMap().put(name, attribute);
        }
    }

    @Override
    public Object getAttribute(final String name) {
        final ConcurrentMap<String, Object> map = this.map();
        return map == null ? null : map.get(name);
    }

    @Override
    public Set<String> getAttributeNameSet() {
        final ConcurrentMap<String, Object> map = this.map();
        return map == null ? Collections.emptySet() : map.keySet();
    }

    @Override
    public void clearAttributes() {
        final ConcurrentMap<String, Object> map = this.map();
        if (map != null) {
            map.clear();
        }
    }

    public int size() {
        final ConcurrentMap<String, Object> map = map();
        return map == null ? 0 : map.size();
    }

    public void addAll(final Attributes attributes) {
        final Enumeration<String> e = attributes.getAttributeNames();
        while (e.hasMoreElements()) {
            final String name = e.nextElement();
            setAttribute(name, attributes.getAttribute(name));
        }
    }
}
