package edu.jhuapl.trinity.utils;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
