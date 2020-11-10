/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpField
 *
 * @author Otstar Lin
 * @date 2020/10/27 下午 2:57
 */
public class HttpField {
    private volatile int hashCache = 0;
    private final HttpHeader header;
    private final String name;
    private final List<String> values;

    public HttpField(
        final HttpHeader header,
        final String name,
        final List<String> values
    ) {
        if (header != null && name == null) {
            this.name = header.asString();
        } else if (name != null) {
            this.name = name;
        } else {
            throw new NullPointerException("HttpField must have a name");
        }
        this.header = header;
        this.values = values;
    }

    public HttpField(final HttpHeader header, final List<String> values) {
        this(header, null, values);
    }

    public HttpField(final String name, final List<String> values) {
        this(HttpHeader.from(name), name, values);
    }

    public HttpField(final HttpHeader header) {
        this(header, new ArrayList<>());
    }

    public HttpField(final String name) {
        this(name, new ArrayList<>());
    }

    public void addValue(final String value) {
        values.add(value);
    }

    public String getValue() {
        return this.getValue(0);
    }

    public String getValue(final int index) {
        return values.get(index);
    }

    public HttpHeader getHeader() {
        return header;
    }

    public String getName() {
        return name;
    }

    public String getLowerCaseName() {
        return this.name.toLowerCase();
    }

    public List<String> getValues() {
        return values;
    }

    public boolean is(final String name) {
        return this.name.equalsIgnoreCase(name);
    }

    public boolean is(final HttpField field) {
        return this.equals(field);
    }

    public String getParamValue() {
        return this.getParams().getValue();
    }

    public String getParamValue(int index) {
        return this.getParams(index).getValue();
    }

    public Params getParams() {
        return this.getParams(0);
    }

    public Params getParams(final int index) {
        final String value = this.getValue(index);
        if (value == null) {
            return null;
        }
        final int i = value.indexOf(";");
        if (i < 0) {
            return new Params(value);
        }
        final String[] values = value.substring(i + 1).split(";");
        final Map<String, String> params = new ConcurrentHashMap<>(
            values.length
        );
        for (final String kv : values) {
            final String[] split = kv.split("=");
            params.put(split[0].trim(), split[1].trim());
        }
        return new Params(value.substring(0, i), params);
    }

    public String getParam(final String name) {
        return this.getParam(name, 0);
    }

    public String getParam(final String name, final int index) {
        final Params params = this.getParams(index);
        if (params != null) {
            return params.get(name);
        }
        return null;
    }

    public String stripParam(final int index) {
        final String value = this.getValue(index);
        if (value == null) {
            return null;
        }
        final int i = value.indexOf(";");
        if (i == -1) {
            return value.trim();
        }
        return value.substring(0, i).trim();
    }

    public String[] getParamValues() {
        return this.getParamValues(";");
    }

    public String[] getParamValues(final String split) {
        return this.getParamValues(0, split);
    }

    public String[] getParamValues(final int index) {
        return this.getParamValues(index, ";");
    }

    public String[] getParamValues(final int index, final String split) {
        final String value = this.getValue(index);
        if (value == null) {
            return null;
        }
        return value.split(split);
    }

    public int size() {
        return this.getValues().size();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final HttpField httpField = (HttpField) o;
        return (
            header == httpField.header && name.equalsIgnoreCase(httpField.name)
        );
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(header);
        result = 31 * result + this.nameHashCode();
        return result;
    }

    private int nameHashCode() {
        int h = this.hashCache;
        final int len = this.name.length();
        if (h == 0 && len > 0) {
            for (int i = 0; i < len; i++) {
                char c = this.name.charAt(i);
                if ((c >= 'a' && c <= 'z')) {
                    c -= 0x20;
                }
                h = 31 * h + c;
            }
            this.hashCache = h;
        }
        return h;
    }

    public static class Params {
        public static final String VALUE_NAME = "value";

        private final String value;
        private final Map<String, String> params;

        public Params(final String value) {
            this.value = value;
            this.params = Collections.emptyMap();
        }

        public Params(final Map<String, String> params) {
            this.value = null;
            this.params = params;
        }

        public Params(final String value, final Map<String, String> params) {
            this.value = value;
            this.params = params;
        }

        public String getValue() {
            return value;
        }

        public Map<String, String> getParams() {
            return params;
        }

        public String get(final String name) {
            if (VALUE_NAME.equals(name)) {
                return this.getValue();
            }
            if (this.params == null) {
                return null;
            }
            return this.params.get(name);
        }
    }
}
