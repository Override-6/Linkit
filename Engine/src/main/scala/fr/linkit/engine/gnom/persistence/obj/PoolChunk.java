/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence.obj;

import fr.linkit.api.gnom.persistence.Freezable;
import fr.linkit.api.gnom.persistence.obj.PoolObject;

import java.lang.reflect.Array;
import java.util.HashMap;

public class PoolChunk<T> implements Freezable {

    private static final int BUFF_STEPS = 100;

    private final Class<?> compClass;
    private final byte tag;
    private final ObjectPool pool;
    private final int maxLength;
    private final HashMap<Integer, Integer> buffMap = new HashMap<>();

    private T[] buff;
    private int pos;

    private boolean frozen = false;

    public PoolChunk(Class<?> compClass,
                     byte tag,
                     ObjectPool pool,
                     int maxLength) {
        this.compClass = compClass;
        this.tag = tag;
        this.pool = pool;
        this.maxLength = maxLength;

        this.buff = (T[]) Array.newInstance(compClass, pool.determineBuffLength(maxLength, BUFF_STEPS));
    }

    public byte tag() {
        return this.tag;
    }

    @Override
    public boolean isFrozen() {
        return frozen || (frozen = pool.isFrozen());
    }

    @Override
    public void freeze() {
        frozen = true;
        pos = buff.length;
    }

    public void resetPos() {
        pos = 0;
    }

    public T[] array() {
        return buff;
    }

    public void add(T t) {
        if (t == null)
            throw new NullPointerException("Can't add null item");
        //if (isFrozen)
        //    throw new IllegalStateException("Could not add item in chunk: This chunk (or its pool) is frozen !")
        if (pos != 0 && pos % BUFF_STEPS == 0) {
            if (pos >= maxLength)
                throw new IllegalStateException("Chunk size exceeds maxLength ('" + maxLength + "')'");
            var extendedBuff = (T[]) Array.newInstance(compClass, Math.min(pos + BUFF_STEPS, maxLength));
            System.arraycopy(buff, 0, extendedBuff, 0, pos);
            buff = extendedBuff;
        }
        buff[pos] = t;
        if (t instanceof PoolObject<?> obj)
            buffMap.put(obj.identity(), pos + 1);
        else
            buffMap.put(System.identityHashCode(t), pos + 1);
        pos += 1;
    }

    public void addIfAbsent(T t) {
        if (indexOf(t) < 0) add(t);
    }

    public T get(int i) {
        if (i >= pos)
            throw new IndexOutOfBoundsException(i + " >= " + pos);
        T r = buff[i];
        if (r == null)
            throw new NullPointerException("Chunk '" + tag + "' returned null item. (at index $" + i + ")");
        return r;
    }

    public int indexOf(Object o) {
        if (o == null)
            return -1;
        return buffMap.get(System.identityHashCode(o)) - 1;
    }

    public int size() {
        return pos;
    }

}
