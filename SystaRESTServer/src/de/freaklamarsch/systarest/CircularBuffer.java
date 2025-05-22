package de.freaklamarsch.systarest;

/*
 *
 * MIT License
 *
 * Copyright (c) 2017 Eugen Paraschiv & 2021 The beep-projects contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * originally obtained from:
 * https://github.com/eugenp/tutorials/blob/master/data-structures/src/main/java/com/baeldung/circularbuffer/CircularBuffer.java
 */

/**
 * A circular buffer implementation that uses a fixed-size buffer to store elements in a FIFO (First-In, First-Out) manner.
 * When the buffer is full, new elements can either be rejected or can overwrite the oldest element in the buffer,
 * depending on the {@link #overwrite} setting.
 *
 * <p>This class is not thread-safe. External synchronization is required for concurrent access.
 * Synchronization can be avoided if you have only one reader and one writer accessing the {@code CircularBuffer}.</p>
 *
 * @param <E> the type of elements stored in the buffer
 */
public class CircularBuffer<E> {
    /** The default capacity of the buffer if no capacity is specified. */
    private static final int DEFAULT_CAPACITY = 8;
    /** The maximum number of elements the buffer can hold. */
    private final int capacity;
    /** The underlying array used to store elements in the buffer. */
    private final E[] data;
    /** The index where the next element will be written. */
    private volatile int writeIndex;
    /** The index of the oldest element in the buffer. */
    private volatile int readIndex;
    /** Determines whether the buffer overwrites the oldest element when full. */
    private boolean overwrite;

    /**
     * Creates a {@code CircularBuffer} with the specified capacity.
     * If the provided capacity is less than 1, {@link #DEFAULT_CAPACITY} is used.
     *
     * @param capacity The maximum number of elements that can be stored in the {@code CircularBuffer}.
     */
    @SuppressWarnings("unchecked")
    public CircularBuffer(int capacity) {
        this.capacity = (capacity < 1) ? DEFAULT_CAPACITY : capacity;
        this.data = (E[]) new Object[this.capacity];
        this.readIndex = 0;
        this.writeIndex = -1; // Indicates that the buffer is initially empty
        this.overwrite = false;
    }

    /**
     * Clears the buffer by resetting the read and write indices.
     * Note: This does not erase the elements stored in the buffer array but makes them inaccessible.
     */
    public void clear() {
        // Mark the buffer as empty
        this.readIndex = 0;
        this.writeIndex = -1;
    }

    /**
     * Adds an element to the buffer.
     * If the buffer is full and {@link #overwrite} is {@code true}, the oldest element is overwritten.
     * If the buffer is full and {@link #overwrite} is {@code false}, the element is not added.
     *
     * @param element The element to add to the buffer.
     * @return {@code true} if the element was added successfully;
     *         {@code false} if the buffer is full and overwriting is disabled.
     */
    public boolean add(E element) {
        if (isNotFull() || overwrite) {
            int nextWriteIndex = writeIndex + 1;
            data[nextWriteIndex % capacity] = element;

            // If the buffer is full and overwrite is active,
            // the read index must be moved to effectively remove the oldest element.
            // This must be done before the write index is moved, otherwise isFull()
            // might return an incorrect result in that specific step.
            if (isFull() && overwrite) {
                readIndex++;
            }
            writeIndex++;
            return true;
        }
        return false;
    }

    /**
     * Removes and returns the oldest element from the {@code CircularBuffer}.
     *
     * @return The oldest element, or {@code null} if the buffer is empty.
     */
    public E remove() {
        if (isNotEmpty()) {
            E nextValue = data[readIndex % capacity];
            readIndex++;
            return nextValue;
        }
        return null;
    }

    /**
     * Returns the oldest element in the buffer without removing it.
     *
     * @return The oldest element, or {@code null} if the buffer is empty.
     */
    public E peek() {
        if (isNotEmpty()) {
            // No need to check for readIndex % capacity here if isNotEmpty is true,
            // as readIndex will always be valid or buffer is empty.
            E nextValue = data[readIndex % capacity];
            return nextValue;
        }
        return null;
    }

    /**
     * Returns the newest element in the buffer without removing it (the last element added).
     *
     * @return The newest element, or {@code null} if the buffer is empty.
     */
    public E end() {
        if (isNotEmpty()) {
            // No need to check for writeIndex % capacity here if isNotEmpty is true,
            // as writeIndex will point to the last written element or buffer is empty.
            E nextValue = data[writeIndex % capacity];
            return nextValue;
        }
        return null;
    }

    /**
     * Returns the maximum number of elements the buffer can hold.
     *
     * @return The capacity of the buffer.
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns the number of elements currently stored in the buffer.
     *
     * @return The number of elements in the buffer.
     */
    public int size() {
        // If writeIndex is -1 (buffer empty), size should be 0.
        // (writeIndex - readIndex) + 1 correctly calculates the number of items.
        // Example: writeIndex = 0, readIndex = 0 => size = 1
        // Example: writeIndex = -1, readIndex = 0 => size = 0
        return (writeIndex - readIndex) + 1;
    }

    /**
     * Sets the overwrite behavior for when an element is added to a full {@code CircularBuffer}.
     *
     * @param overwrite If {@code true}, the oldest element is overwritten when the buffer is full.
     *                  If {@code false}, adding an element to a full buffer will be ignored (the {@code add} method will return {@code false}).
     */
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Returns whether the buffer is configured to overwrite the oldest element when full.
     *
     * @return {@code true} if overwriting is enabled; {@code false} otherwise.
     */
    public boolean getOverwrite() {
        return overwrite;
    }

    /**
     * Returns whether the buffer is empty.
     * The buffer is empty if the {@code writeIndex} is less than the {@code readIndex}.
     * This occurs when the buffer is initialized (writeIndex = -1, readIndex = 0)
     * or after all elements have been removed.
     *
     * @return {@code true} if the buffer is empty; {@code false} otherwise.
     */
    public boolean isEmpty() {
        return writeIndex < readIndex;
    }

    /**
     * Returns whether the buffer is full.
     * The buffer is full if the number of elements ({@link #size()}) is equal to or greater than its {@link #capacity()}.
     * It can be greater if overwrite is enabled and readIndex is incremented past writeIndex temporarily during an add operation.
     *
     * @return {@code true} if the buffer is full; {@code false} otherwise.
     */
    public boolean isFull() {
        return size() >= capacity;
    }

    /**
     * Returns whether the buffer is not empty.
     * This is a convenience method, equivalent to {@code !isEmpty()}.
     *
     * @return {@code true} if the buffer is not empty; {@code false} otherwise.
     */
    private boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * Returns whether the buffer is not full.
     * This is a convenience method, equivalent to {@code !isFull()}.
     *
     * @return {@code true} if the buffer is not full; {@code false} otherwise.
     */
    private boolean isNotFull() {
        return !isFull();
    }
}