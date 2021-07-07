package de.freaklamarsch.systarest;

/*
 * 
 * MIT License
 * 
 * Copyright (c) 2017 Eugen Paraschiv
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
 * Implementation of a circular buffer. A circular buffer is a data structure that uses
 * a single, fixed-size buffer as if it were connected end-to-end. By default, this implementation 
 * denies adding new elements if the buffer is full. This behaviour can be changed by setting {@link #overwrite}
 * @param <E> object type to store within this buffer
 */
public class CircularBuffer<E> {

	private static final int DEFAULT_CAPACITY = 8;

	private final int capacity;
	private final E[] data;
	private volatile int writeSequence, readSequence;
	private boolean overwrite;

	/**
	 * Creates a {@code CircularBuffer} with a capacity of {@code capacity} elements.
	 * @param capacity number of elements that can be stored in the {@code CircularBuffer}
	 */
	@SuppressWarnings("unchecked")
	public CircularBuffer(int capacity) {
		this.capacity = (capacity < 1) ? DEFAULT_CAPACITY : capacity;
		this.data = (E[]) new Object[this.capacity];
		this.readSequence = 0;
		this.writeSequence = -1;
		this.overwrite = false;
	}

	/**
	 * moves the markers to represent an empty buffer. Note, this does not immediately destroy the stored elements in the buffer.
	 */
	public void clear() {
		// mark the buffer as empty
		this.readSequence = 0;
		this.writeSequence = -1;
	}

	/**
	 * Add an element to the {@code CircularBuffer}
	 * @param element the element added to the {@code CircularBuffer}
	 * @return true if the element could be added to the buffer, false otherwise
	 */
	public boolean add(E element) {

		if (isNotFull() || overwrite) {

			int nextWriteSeq = writeSequence + 1;
			data[nextWriteSeq % capacity] = element;

			//if the buffer is full and overwrite is active
			//the read index has to be moved, for removing the first element
			//this has to be done before the write index is moved, otherwise the isFull() test will fail
			if(isFull() && overwrite) {
				readSequence++;
			}
			writeSequence++;
			return true;
		}

		return false;
	}

	/**
	 * Removes the oldest element from the {@code CircularBuffer} and returns it
	 * @return the oldest element
	 */
	public E remove() {
		if (isNotEmpty()) {
			E nextValue = data[readSequence % capacity];
			readSequence++;
			return nextValue;
		}
		return null;
	}

	/**
	 *  @return the oldest element from the buffer without removing it
	 */
	public E peek() {
		if (isNotEmpty()) {
			E nextValue = data[readSequence % capacity];
			return nextValue;
		}
		return null;
	}

	/**
	 * @return the capacity of the {@code CircularBuffer}
	 */
	public int capacity() {
		return capacity;
	}

	/**
	 * @return  the size of the {@code CircularBuffer}, i.e. elements currently stored.
	 */
	public int size() {
		return (writeSequence - readSequence) + 1;
	}

	/**
	 * @param overwrite Set the behavior if an element is added to a full {@code CircularBuffer}.
	 * If set to {@code true}, the oldest element is overwritten.
	 * If set to {@code false}, add will ignore the add request and return {@code false}.
	 */
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
	
	/**
	 * @return the current value of {@link #overwrite}
	 */
	public boolean getOverwrite() {
		return overwrite;
	}
	
	/**
	 * @return if this {@code CircularBuffer} is empty or not.
	 */
	public boolean isEmpty() {
		return writeSequence < readSequence;
	}

	/**
	 * @return if this {@code CircularBuffer} is full or not.
	 */
	public boolean isFull() {
		return size() >= capacity;
	}

	private boolean isNotEmpty() {
		return !isEmpty();
	}

	private boolean isNotFull() {
		return !isFull();
	}
}