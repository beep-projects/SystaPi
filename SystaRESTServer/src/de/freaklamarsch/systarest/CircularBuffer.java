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
 * A circular buffer implementation that uses a fixed-size buffer to store elements in a FIFO (First In, First Out) manner.
 * By default, this implementation denies adding new elements if the buffer is full. This behavior can be changed by
 * enabling the {@link #overwrite} setting.
 *
 * <p>This class is not thread-safe. External synchronization is required for concurrent access. Synchronization can be avoided, if you
 * have only one reader and one writer accessing the {@code CircularBuffer}</p>
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
	 * Creates a {@code CircularBuffer} with a capacity of {@code capacity}
	 * elements.
	 *
	 * @param capacity number of elements that can be stored in the
	 *                 {@code CircularBuffer}
	 */
	@SuppressWarnings("unchecked")
	public CircularBuffer(int capacity) {
		this.capacity = (capacity < 1) ? DEFAULT_CAPACITY : capacity;
		this.data = (E[]) new Object[this.capacity];
		this.readIndex = 0;
		this.writeIndex = -1;
		this.overwrite = false;
	}

    /**
     * Clears the buffer by resetting the read and write indices.
     * Note, this does not destroy the stored elements in the buffer.
	 */
	public void clear() {
		// mark the buffer as empty
		this.readIndex = 0;
		this.writeIndex = -1;
	}

    /**
     * Adds an element to the buffer. If the buffer is full and {@link #overwrite} is {@code true}, the oldest element
     * is overwritten. If {@link #overwrite} is {@code false}, the element is not added.
     *
     * @param element the element to add to the buffer
     * @return {@code true} if the element was added successfully; {@code false} if the buffer is full and overwriting
     *         is disabled
     */
	public boolean add(E element) {

		if (isNotFull() || overwrite) {

			int nextWriteIndex = writeIndex + 1;
			data[nextWriteIndex % capacity] = element;

			// if the buffer is full and overwrite is active
			// the read index has to be moved, for removing the first element
			// this has to be done before the write index is moved, otherwise the isFull()
			// test will fail
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
     * @return the oldest element, or {@code null} if the buffer is empty
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
     * @return the oldest element, or {@code null} if the buffer is empty
     */
	public E peek() {
		if (isNotEmpty()) {
			E nextValue = data[readIndex % capacity];
			return nextValue;
		}
		return null;
	}

    /**
     * Returns the newest element in the buffer without removing it.
     *
     * @return the newest element, or {@code null} if the buffer is empty
     */
	public E end() {
		if (isNotEmpty()) {
			E nextValue = data[writeIndex % capacity];
			return nextValue;
		}
		return null;
	}

    /**
     * Returns the maximum number of elements the buffer can hold.
     *
     * @return the capacity of the buffer
     */
	public int capacity() {
		return capacity;
	}

    /**
     * Returns the number of elements currently stored in the buffer.
     *
     * @return the number of elements in the buffer
     */
	public int size() {
		return (writeIndex - readIndex) + 1;
	}

	/**
	 * @param overwrite Set the overwrite behavior if an element is added to a full
	 *                  {@code CircularBuffer}. If set to {@code true}, the oldest
	 *                  element is overwritten. If set to {@code false}, add will
	 *                  ignore the add request and return {@code false}.
	 */
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

    /**
     * Returns whether the buffer is configured to overwrite the oldest element when full.
     *
     * @return {@code true} if overwriting is enabled; {@code false} otherwise
     */
	public boolean getOverwrite() {
		return overwrite;
	}

    /**
     * Returns whether the buffer is empty.
     *
     * @return {@code true} if the buffer is empty; {@code false} otherwise
     */
	public boolean isEmpty() {
		return writeIndex < readIndex;
	}

    /**
     * Returns whether the buffer is full.
     *
     * @return {@code true} if the buffer is full; {@code false} otherwise
     */
	public boolean isFull() {
		return size() >= capacity;
	}

    /**
     * Returns whether the buffer is not empty.
     *
     * @return {@code true} if the buffer is not empty; {@code false} otherwise
     */
	private boolean isNotEmpty() {
		return !isEmpty();
	}

    /**
     * Returns whether the buffer is not full.
     *
     * @return {@code true} if the buffer is not full; {@code false} otherwise
     */
	private boolean isNotFull() {
		return !isFull();
	}
}