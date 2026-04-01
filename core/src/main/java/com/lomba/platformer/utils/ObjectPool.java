package com.lomba.platformer.utils;

import java.util.ArrayDeque;
import java.util.function.Supplier;

/**
 * ObjectPool<T> — generic free-list pool.
 *
 * WHY: Android's GC causes frame hitches when many short-lived objects (projectiles,
 * particle effects, collision events) are allocated and freed each second.
 * Pre-allocating and recycling avoids GC pressure entirely.
 *
 * USAGE:
 *   ObjectPool<Bullet> pool = new ObjectPool<>(Bullet::new, 64);
 *   Bullet b = pool.obtain();
 *   // ... use b ...
 *   pool.free(b);
 *
 * Implements {@link Poolable} so objects can reset themselves on release.
 */
public class ObjectPool<T> {

    /** Objects that implement this can clean up their state before recycling. */
    public interface Poolable {
        void reset();
    }

    private final ArrayDeque<T>  freeObjects;
    private final Supplier<T>    factory;
    private final int            maxSize;

    private int peakObtained = 0;   // diagnostic: highest concurrent usage

    /**
     * @param factory  creates a new instance when the pool is empty
     * @param maxSize  cap on how many objects to keep in the free list
     */
    public ObjectPool(Supplier<T> factory, int maxSize) {
        this.factory    = factory;
        this.maxSize    = maxSize;
        this.freeObjects = new ArrayDeque<>(maxSize);
    }

    /**
     * Get an object from the pool. Creates a new one if the pool is empty.
     * Never returns null.
     */
    public T obtain() {
        T obj = freeObjects.isEmpty() ? factory.get() : freeObjects.pop();
        peakObtained++;
        return obj;
    }

    /**
     * Return an object to the pool.
     * If the pool is full, the object is dropped (eligible for GC).
     */
    public void free(T obj) {
        if (obj instanceof Poolable) {
            ((Poolable) obj).reset();
        }
        if (freeObjects.size() < maxSize) {
            freeObjects.push(obj);
        }
        peakObtained = Math.max(0, peakObtained - 1);
    }

    /** Pre-warm the pool to avoid allocation spikes at runtime. */
    public void prewarm(int count) {
        for (int i = 0; i < Math.min(count, maxSize); i++) {
            freeObjects.push(factory.get());
        }
    }

    public int getFreeCount()    { return freeObjects.size(); }
    public int getPeakObtained() { return peakObtained; }
}
