package edu.jhuapl.trinity.utils;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @param <E> Item type for CircularQueue
 * @author Sean Phillips
 */
public class CircularConcurrentQueue<E> {
    private final AtomicInteger next = new AtomicInteger(0);
    private final E[] elements;
    ConcurrentLinkedQueue queue;

    public CircularConcurrentQueue(Collection<E> items) {
        queue = new ConcurrentLinkedQueue<>(items);
        this.elements = (E[]) queue.toArray();
    }

    public Object[] getElements() {
        return queue.toArray();
    }

    public E get() {
        return elements[next.getAndIncrement() % elements.length];
    }

    public int getCurrentIndex() {
        return next.get() % elements.length;
    }

    public int getElementsLength() {
        return elements.length;
    }
}
