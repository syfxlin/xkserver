/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * MultiMap
 *
 * @author Otstar Lin
 * @date 2020/11/10 上午 10:33
 */
@SuppressWarnings("serial")
public class MultiMap<V> extends HashMap<String, List<V>> {

    public MultiMap() {
        super();
    }

    public MultiMap(final Map<String, List<V>> map) {
        super(map);
    }

    public MultiMap(final MultiMap<V> map) {
        super(map);
    }

    public List<V> getValues(final String name) {
        final List<V> vals = super.get(name);
        if ((vals == null) || vals.isEmpty()) {
            return null;
        }
        return vals;
    }

    public V getValue(final String name, final int i) {
        final List<V> vals = getValues(name);
        if (vals == null) {
            return null;
        }
        if (i == 0 && vals.isEmpty()) {
            return null;
        }
        return vals.get(i);
    }

    public String getString(final String name) {
        final List<V> vals = get(name);
        if ((vals == null) || (vals.isEmpty())) {
            return null;
        }

        if (vals.size() == 1) {
            // simple form.
            return vals.get(0).toString();
        }

        // delimited form
        final StringBuilder values = new StringBuilder(128);
        for (final V e : vals) {
            if (e != null) {
                if (values.length() > 0) {
                    values.append(',');
                }
                values.append(e.toString());
            }
        }
        return values.toString();
    }

    public List<V> put(final String name, final V value) {
        if (value == null) {
            return super.put(name, null);
        }
        final List<V> vals = new ArrayList<>();
        vals.add(value);
        return put(name, vals);
    }

    public void putAllValues(final Map<String, V> input) {
        for (final Entry<String, V> entry : input.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public List<V> putValues(final String name, final List<V> values) {
        return super.put(name, values);
    }

    @SafeVarargs
    public final List<V> putValues(final String name, final V... values) {
        final List<V> list = new ArrayList<>();
        list.addAll(Arrays.asList(values));
        return super.put(name, list);
    }

    public void add(final String name, final V value) {
        List<V> lo = get(name);
        if (lo == null) {
            lo = new ArrayList<>();
        }
        lo.add(value);
        super.put(name, lo);
    }

    public void addValues(final String name, final List<V> values) {
        List<V> lo = get(name);
        if (lo == null) {
            lo = new ArrayList<>();
        }
        lo.addAll(values);
        put(name, lo);
    }

    public void addValues(final String name, final V[] values) {
        List<V> lo = get(name);
        if (lo == null) {
            lo = new ArrayList<>();
        }
        lo.addAll(Arrays.asList(values));
        put(name, lo);
    }

    public boolean addAllValues(final MultiMap<V> map) {
        boolean merged = false;

        if ((map == null) || (map.isEmpty())) {
            // done
            return merged;
        }

        for (final Entry<String, List<V>> entry : map.entrySet()) {
            final String name = entry.getKey();
            final List<V> values = entry.getValue();

            if (this.containsKey(name)) {
                merged = true;
            }

            this.addValues(name, values);
        }

        return merged;
    }

    public boolean removeValue(final String name, final V value) {
        final List<V> lo = get(name);
        if ((lo == null) || (lo.isEmpty())) {
            return false;
        }
        final boolean ret = lo.remove(value);
        if (lo.isEmpty()) {
            remove(name);
        } else {
            put(name, lo);
        }
        return ret;
    }

    public boolean containsSimpleValue(final V value) {
        for (final List<V> vals : values()) {
            if ((vals.size() == 1) && vals.contains(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        final Iterator<Entry<String, List<V>>> iter = entrySet().iterator();
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean delim = false;
        while (iter.hasNext()) {
            final Entry<String, List<V>> e = iter.next();
            if (delim) {
                sb.append(", ");
            }
            final String key = e.getKey();
            final List<V> vals = e.getValue();
            sb.append(key);
            sb.append('=');
            if (vals.size() == 1) {
                sb.append(vals.get(0));
            } else {
                sb.append(vals);
            }
            delim = true;
        }
        sb.append('}');
        return sb.toString();
    }

    public Map<String, String[]> toStringArrayMap() {
        final HashMap<String, String[]> map = new HashMap<String, String[]>(
            size() * 3 / 2
        ) {

            @Override
            public String toString() {
                final StringBuilder b = new StringBuilder();
                b.append('{');
                for (final String k : super.keySet()) {
                    if (b.length() > 1) {
                        b.append(',');
                    }
                    b.append(k);
                    b.append('=');
                    b.append(Arrays.asList(super.get(k)));
                }

                b.append('}');
                return b.toString();
            }
        };

        for (final Entry<String, List<V>> entry : entrySet()) {
            String[] a = null;
            if (entry.getValue() != null) {
                a = new String[entry.getValue().size()];
                a = entry.getValue().toArray(a);
            }
            map.put(entry.getKey(), a);
        }
        return map;
    }
}
