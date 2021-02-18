/*
 * Copyright (c) 2021, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.session;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import me.ixk.xkserver.http.AttributesMap;

/**
 * @author Otstar Lin
 * @date 2020/12/6 下午 2:01
 */
public class SessionData extends AttributesMap implements Serializable {

    private static final long serialVersionUID = 1L;

    private volatile String id;
    private volatile long created;
    private volatile long lastAccessed;
    private volatile int maxInactiveInterval;

    public SessionData(
        final String id,
        final long created,
        final long lastAccessed,
        final int maxInactiveInterval
    ) {
        this.id = id;
        this.created = created;
        this.lastAccessed = lastAccessed;
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public long getCreated() {
        return created;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(final long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(final int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    private void writeObject(final java.io.ObjectOutputStream out)
        throws IOException {
        out.writeUTF(id);
        out.writeLong(created);
        out.writeLong(lastAccessed);
        out.writeInt(maxInactiveInterval);
        final ConcurrentMap<String, Object> map = this.map();
        final int size = map.size();
        out.writeInt(size);
        for (final Entry<String, Object> entry : map.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeObject(entry.getValue());
        }
    }

    private void readObject(final java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        this.id = in.readUTF();
        this.created = in.readLong();
        this.lastAccessed = in.readLong();
        this.maxInactiveInterval = in.readInt();
        final int size = in.readInt();
        for (int i = 0; i < size; i++) {
            this.setAttribute(in.readUTF(), in.readObject());
        }
    }
}
