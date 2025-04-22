/*
* Copyright (c) 2021, The beep-projects contributors
* this file originated from https://github.com/beep-projects
* Do not remove the lines above.
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see https://www.gnu.org/licenses/
*
*/
package de.freaklamarsch.systarest.tests;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Field;

import org.junit.Test;

import de.freaklamarsch.systarest.CircularBuffer;

public class CircularBufferTest {
	final int testCapacity = 15;
	CircularBuffer<Integer> cb;// = new CircularBuffer<Integer>(testCapacity);

	public CircularBufferTest() {
		cb = new CircularBuffer<>(testCapacity);
	}

    @Test
    public void testAddAndRemove() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
        assertTrue(buffer.add(1));
        assertTrue(buffer.add(2));
        assertTrue(buffer.add(3));
        assertFalse(buffer.add(4)); // Buffer is full
        assertEquals(1, buffer.remove());
        assertEquals(2, buffer.remove());
        assertEquals(3, buffer.remove());
        assertNull(buffer.remove()); // Buffer is empty
    }

    @Test
    public void testOverwrite() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
        buffer.setOverwrite(true);
        assertTrue(buffer.add(1));
        assertTrue(buffer.add(2));
        assertTrue(buffer.add(3));
        assertTrue(buffer.add(4)); // Overwrites the oldest element
        assertEquals(2, buffer.remove());
        assertEquals(3, buffer.remove());
        assertEquals(4, buffer.remove());
    }

    @Test
    public void testInvalidCapacity() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(-1);
        assertEquals(8, buffer.capacity()); // Default capacity
    }

		@Test
		public void testAddToFullBufferWithoutOverwrite() {
				CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
				buffer.setOverwrite(false);
		
				assertTrue(buffer.add(1));
				assertTrue(buffer.add(2));
				assertTrue(buffer.add(3));
				assertFalse(buffer.add(4)); // Buffer is full
		}
		
		@Test
		public void testAddToFullBufferWithOverwrite() {
				CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
				buffer.setOverwrite(true);
		
				assertTrue(buffer.add(1));
				assertTrue(buffer.add(2));
				assertTrue(buffer.add(3));
				assertTrue(buffer.add(4)); // Overwrites the oldest element
				assertEquals(2, buffer.remove()); // Oldest element is overwritten
		}
				
	@Test
	public void testConstructor() {
		int defaultCapacity = -1;
		try {
			Field defaultCapacityField = CircularBuffer.class.getDeclaredField("DEFAULT_CAPACITY");
			defaultCapacityField.setAccessible(true);
			defaultCapacity = (int) defaultCapacityField.get(cb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		cb = new CircularBuffer<>(testCapacity);
		assertFalse(cb.getOverwrite());
		assertEquals(testCapacity, cb.capacity());
		assertEquals(0, cb.size());
		assertTrue(cb.isEmpty());
		assertFalse(cb.isFull());
		assertEquals(null, cb.peek());
		assertEquals(null, cb.remove());
		// test constructor with negative capacity
		// this should create a buffer with DEFAULT_CAPACITY
		cb = new CircularBuffer<>(-1);
		assertEquals(defaultCapacity, cb.capacity());
		assertEquals(0, cb.size());
		assertTrue(cb.isEmpty());
		assertFalse(cb.isFull());
		assertEquals(null, cb.peek());
		assertEquals(null, cb.remove());
		// test constructor with 0 capacity
		cb = new CircularBuffer<>(0);
		assertEquals(defaultCapacity, cb.capacity());
		assertEquals(0, cb.size());
		assertTrue(cb.isEmpty());
		assertFalse(cb.isFull());
		assertEquals(null, cb.peek());
		assertEquals(null, cb.remove());
	}

	@Test
	public void testAdd() {
		cb = new CircularBuffer<>(testCapacity);
		cb.setOverwrite(true);
		assertTrue(cb.getOverwrite());
		cb.setOverwrite(false);
		assertFalse(cb.getOverwrite());
		int i = 0;
		// fill the buffer to testCapacity-1 elements
		for (i = 0; i < testCapacity - 1; i++) {
			// add i to the buffer and check the behavior of the buffer
			assertEquals(true, cb.add(i));
			// capacity should not change
			assertEquals(testCapacity, cb.capacity());
			// size should be equal i+1, because i starts at 0
			assertEquals(i + 1, cb.size());
			// the buffer should no longer be empty
			assertEquals(false, cb.isEmpty());
			// the buffer should not be full, because i runs only to 1 less the capacity
			assertEquals(false, cb.isFull());
			// peek should return the first element, which is 0
			assertEquals(0, (int) cb.peek());
		}
		// add i to the buffer and check the behavior of the buffer
		assertEquals(true, cb.add(i));
		// capacity should not change
		assertEquals(testCapacity, cb.capacity());
		// size should be equal i+1, because i starts at 0
		assertEquals(i + 1, cb.size());
		// the buffer should no longer be empty
		assertEquals(false, cb.isEmpty());
		// the buffer should now be full
		assertEquals(true, cb.isFull());
		// peek should return the first element, which is 0
		assertEquals(0, (int) cb.peek());
		i++;
		// buffer is now full, so this add should fail
		assertEquals(false, cb.add(i));
		// remove should return the first element, which is 0
		assertEquals(0, (int) cb.remove());
		// capacity should not change
		assertEquals(testCapacity, cb.capacity());
		// size should be equal capacity - 1, because we have removed one element from a
		// full buffer
		assertEquals(testCapacity - 1, cb.size());
		// the buffer should no longer be empty
		assertEquals(false, cb.isEmpty());
		// the buffer should not be full
		assertEquals(false, cb.isFull());
		// peek should return the first element, which is now 1
		assertEquals(1, (int) cb.peek());
		// buffer is not full, so this add should now succeed
		assertEquals(true, cb.add(i));
		// test if overwrite works
		cb.setOverwrite(true);
		assertEquals(true, cb.add(++i));
		// first element should be overwritten, peek should now return 2
		assertEquals(2, (int) cb.peek());
		// test overwrite again
		assertEquals(true, cb.add(++i));
		// second element should be overwritten, peek should now return 3
		assertEquals(3, (int) cb.peek());
	}

	@Test
	public void testAddWithOverwrite() {
		cb = new CircularBuffer<>(testCapacity);
		cb.setOverwrite(true);
		int i = 0;
		// fill the buffer to testCapacity-1 elements
		for (i = 0; i < testCapacity - 1; i++) {
			// add i to the buffer and check the behavior of the buffer
			assertEquals(true, cb.add(i));
			// capacity should not change
			assertEquals(testCapacity, cb.capacity());
			// size should be equal i+1, because i starts at 0
			assertEquals(i + 1, cb.size());
			// the buffer should no longer be empty
			assertEquals(false, cb.isEmpty());
			// the buffer should not be full, because i runs only to 1 less the capacity
			assertEquals(false, cb.isFull());
			// peek should return the first element, which is 0
			assertEquals(0, (int) cb.peek());
		}
		// add i to the buffer and check the behavior of the buffer
		assertEquals(true, cb.add(i));
		// capacity should not change
		assertEquals(testCapacity, cb.capacity());
		// size should be equal i+1, because i starts at 0
		assertEquals(i + 1, cb.size());
		// size should be equal to testCapacity
		assertEquals(testCapacity, cb.size());
		// the buffer should no longer be empty
		assertEquals(false, cb.isEmpty());
		// the buffer should now be full
		assertEquals(true, cb.isFull());
		// peek should return the first element, which is 0
		assertEquals(0, (int) cb.peek());
		// add another element to se the overwrite in action
		i++;
		// add i to the buffer and check the behavior of the buffer
		assertEquals(true, cb.add(i));
		// capacity should not change
		assertEquals(testCapacity, cb.capacity());
		// size should be equal i, because we start overwriting elements, so it is no
		// longer i+1
		assertEquals(i, cb.size());
		// size should be equal to testCapacity
		assertEquals(testCapacity, cb.size());
		// the buffer should no longer be empty
		assertEquals(false, cb.isEmpty());
		// the buffer should now be full
		assertEquals(true, cb.isFull());
		// peek should return the second element, which is 1, because the first one got
		// overwritten
		assertEquals(1, (int) cb.peek());
	}

	@Test
	public void testRemove() {
		cb = new CircularBuffer<>(testCapacity);
		int i = 0;
		// fill the buffer
		while (!cb.isFull()) {
			cb.add(i);
			i++;
		}
		// check if the elements are removed with the correct order
		i = 0;
		while (!cb.isEmpty()) {
			assertEquals(Integer.valueOf(i), cb.remove());
			// size of the buffer should be reduced by one with each call to remove
			assertEquals(testCapacity - i - 1, cb.size());
			i++;
		}
		// capacity should not change
		assertEquals(testCapacity, cb.capacity());
		// the buffer should now be empty
		assertEquals(true, cb.isEmpty());
		// the buffer should not be full
		assertEquals(false, cb.isFull());
		// peek() on an empty buffer should return null
		assertEquals(null, cb.peek());
		// remove() on an empty buffer should return null
		assertEquals(null, cb.remove());
		// size of an empty buffer should be 0
		assertEquals(0, cb.size());

	}

	@Test
	public void testClear() {
		cb = new CircularBuffer<>(testCapacity);
		int i = 0;
		// fill the buffer
		while (!cb.isFull()) {
			cb.add(i);
			i++;
		}
		cb.clear();
		// capacity should not change
		assertEquals(testCapacity, cb.capacity());
		// the buffer should now be empty
		assertEquals(true, cb.isEmpty());
		// the buffer should not be full
		assertEquals(false, cb.isFull());
		// peek() on an empty buffer should return null
		assertEquals(null, cb.peek());
		// remove() on an empty buffer should return null
		assertEquals(null, cb.remove());
		// size of an empty buffer should be 0
		assertEquals(0, cb.size());
	}

}
