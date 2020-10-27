/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public void addValue(String value) {
        values.add(value);
    }

    public String getValue() {
        return this.getValue(0);
    }

    public String getValue(int index) {
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

    public boolean is(String name) {
        return this.name.equalsIgnoreCase(name);
    }

    public boolean is(HttpField field) {
        return this.equals(field);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HttpField httpField = (HttpField) o;
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
        int len = this.name.length();
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
}
