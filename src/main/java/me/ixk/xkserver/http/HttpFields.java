/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpFields
 *
 * @author Otstar Lin
 * @date 2020/10/27 下午 3:25
 */
public class HttpFields implements Map<String, HttpField> {
    private final Map<String, HttpField> fields;

    public HttpFields() {
        this(new ConcurrentHashMap<>());
    }

    public HttpFields(Map<String, HttpField> fields) {
        this.fields = fields;
    }

    public List<String> getValues(String name) {
        final HttpField field = this.get(name);
        return field == null ? null : field.getValues();
    }

    public String getValue(String name) {
        return this.getValue(name, 0);
    }

    public String getValue(String name, int index) {
        final HttpField field = this.get(name);
        return field == null ? null : field.getValue(index);
    }

    public HttpField put(HttpField field) {
        return this.put(field.getLowerCaseName(), field);
    }

    public void putAll(List<HttpField> fields) {
        for (HttpField field : fields) {
            this.put(field);
        }
    }

    @Override
    public int size() {
        return this.fields.size();
    }

    @Override
    public boolean isEmpty() {
        return this.fields.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            return this.fields.containsKey(((String) key).toLowerCase());
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return this.fields.containsValue(value);
    }

    @Override
    public HttpField get(Object key) {
        if (key instanceof String) {
            return this.fields.get(((String) key).toLowerCase());
        }
        return null;
    }

    @Override
    public HttpField put(String key, HttpField value) {
        return this.fields.put(key.toLowerCase(), value);
    }

    @Override
    public HttpField remove(Object key) {
        if (key instanceof String) {
            return this.fields.remove(((String) key).toLowerCase());
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends HttpField> m) {
        for (Entry<? extends String, ? extends HttpField> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        this.fields.clear();
    }

    @Override
    public Set<String> keySet() {
        return this.fields.keySet();
    }

    @Override
    public Collection<HttpField> values() {
        return this.fields.values();
    }

    @Override
    public Set<Entry<String, HttpField>> entrySet() {
        return this.fields.entrySet();
    }
}
