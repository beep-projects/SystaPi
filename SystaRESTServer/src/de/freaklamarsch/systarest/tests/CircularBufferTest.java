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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test; // Replaced org.junit.Test

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
        assertTrue(buffer.add(1), "Adding 1 should succeed");
        assertTrue(buffer.add(2), "Adding 2 should succeed");
        assertTrue(buffer.add(3), "Adding 3 should succeed");
        assertFalse(buffer.add(4), "Adding 4 to full buffer (overwrite off) should fail"); // Buffer is full
        assertEquals(1, (int) buffer.remove(), "First removed element should be 1"); 
        assertEquals(2, (int) buffer.remove(), "Second removed element should be 2");
        assertEquals(3, (int) buffer.remove(), "Third removed element should be 3");
        assertNull(buffer.remove(), "Removing from empty buffer should return null"); 
    }

    @Test
    public void testOverwrite() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
        buffer.setOverwrite(true);
        assertTrue(buffer.add(1), "Adding 1 should succeed");
        assertTrue(buffer.add(2), "Adding 2 should succeed");
        assertTrue(buffer.add(3), "Adding 3 should succeed");
        assertTrue(buffer.add(4), "Adding 4 (overwrite on) should succeed and overwrite 1"); // Overwrites the oldest element
        assertEquals(2, (int) buffer.remove(), "After overwriting 1 with 4, first removed should be 2");
        assertEquals(3, (int) buffer.remove(), "Next removed should be 3");
        assertEquals(4, (int) buffer.remove(), "Last removed should be 4");
        assertNull(buffer.remove(), "Removing from empty buffer after overwrite operations should return null");
    }

    @Test
    public void testInvalidCapacity() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(-1);
        assertEquals(8, buffer.capacity(), "Buffer with capacity -1 should default to DEFAULT_CAPACITY (8)"); 
    }

		@Test
		public void testAddToFullBufferWithoutOverwrite() {
				CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
				buffer.setOverwrite(false); // Explicitly set overwrite to false
		
				assertTrue(buffer.add(1), "Adding 1 to buffer (overwrite off) should succeed");
				assertTrue(buffer.add(2), "Adding 2 to buffer (overwrite off) should succeed");
				assertTrue(buffer.add(3), "Adding 3 to buffer (overwrite off) should succeed");
				assertFalse(buffer.add(4), "Adding 4 to full buffer (overwrite off) should fail");
		}
		
		@Test
		public void testAddToFullBufferWithOverwrite() {
				CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
				buffer.setOverwrite(true); // Explicitly set overwrite to true
		
				assertTrue(buffer.add(1), "Adding 1 to buffer (overwrite on) should succeed");
				assertTrue(buffer.add(2), "Adding 2 to buffer (overwrite on) should succeed");
				assertTrue(buffer.add(3), "Adding 3 to buffer (overwrite on) should succeed");
				assertTrue(buffer.add(4), "Adding 4 to full buffer (overwrite on) should succeed (overwriting 1)");
				assertEquals(2, (int) buffer.remove(), "After overwriting 1 with 4, first removed should be 2");
		}
				
	@Test
	public void testConstructor() {
		int defaultCapacity = -1;
		try {
			Field defaultCapacityField = CircularBuffer.class.getDeclaredField("DEFAULT_CAPACITY");
			defaultCapacityField.setAccessible(true);
			defaultCapacity = (int) defaultCapacityField.get(cb);
		} catch (Exception e) {
			// This reflection can fail, provide a message if it does.
			// However, it's not the primary focus of *this* test method.
			System.err.println("Warning: Could not reflectively access DEFAULT_CAPACITY. Test may be less robust.");
			// Fallback if reflection fails, assuming 8 is the known default.
			// This makes the test less dependent on the reflection succeeding.
			defaultCapacity = 8; 
		}
		cb = new CircularBuffer<>(testCapacity);
		assertFalse(cb.getOverwrite(), "New buffer should have overwrite off by default");
		assertEquals(testCapacity, cb.capacity(), "Capacity should be as set in constructor");
		assertEquals(0, cb.size(), "New buffer should have size 0");
		assertTrue(cb.isEmpty(), "New buffer should be empty");
		assertFalse(cb.isFull(), "New buffer should not be full");
		assertNull(cb.peek(), "Peek on new empty buffer should be null");
		assertNull(cb.remove(), "Remove on new empty buffer should be null");
		
		// test constructor with negative capacity
		cb = new CircularBuffer<>(-1);
		assertEquals(defaultCapacity, cb.capacity(), "Capacity -1 should result in default capacity");
		assertEquals(0, cb.size(), "Buffer with default capacity (from -1) should have size 0 initially");
		assertTrue(cb.isEmpty(), "Buffer with default capacity (from -1) should be empty initially");
		assertFalse(cb.isFull(), "Buffer with default capacity (from -1) should not be full initially");
		assertNull(cb.peek(), "Peek on empty buffer (from -1 capacity) should be null");
		assertNull(cb.remove(), "Remove on empty buffer (from -1 capacity) should be null");
		
		// test constructor with 0 capacity
		cb = new CircularBuffer<>(0);
		assertEquals(defaultCapacity, cb.capacity(), "Capacity 0 should result in default capacity");
		assertEquals(0, cb.size(), "Buffer with default capacity (from 0) should have size 0 initially");
		assertTrue(cb.isEmpty(), "Buffer with default capacity (from 0) should be empty initially");
		assertFalse(cb.isFull(), "Buffer with default capacity (from 0) should not be full initially");
		assertNull(cb.peek(), "Peek on empty buffer (from 0 capacity) should be null");
		assertNull(cb.remove(), "Remove on empty buffer (from 0 capacity) should be null");
	}

	@Test
	public void testAdd() {
		cb = new CircularBuffer<>(testCapacity); // testCapacity is 15
		cb.setOverwrite(true);
		assertTrue(cb.getOverwrite(), "Overwrite flag should be true after setting it");
		cb.setOverwrite(false);
		assertFalse(cb.getOverwrite(), "Overwrite flag should be false after unsetting it");
		
		int i = 0;
		// fill the buffer to testCapacity-1 elements
		for (i = 0; i < testCapacity - 1; i++) {
			assertTrue(cb.add(i), "Adding element " + i + " should succeed");
			assertEquals(testCapacity, cb.capacity(), "Capacity should remain constant");
			assertEquals(i + 1, cb.size(), "Size should be " + (i + 1) + " after adding " + (i+1) + " elements");
			assertFalse(cb.isEmpty(), "Buffer should not be empty");
			assertFalse(cb.isFull(), "Buffer should not be full yet (element " + i + " of " + (testCapacity-1) +")");
			assertEquals(0, (int) cb.peek(), "Peek should return the first element (0)");
		}
		// add the last element to make it full
		assertTrue(cb.add(i), "Adding element " + i + " (to fill buffer) should succeed");
		assertEquals(testCapacity, cb.capacity(), "Capacity should remain constant when full");
		assertEquals(i + 1, cb.size(), "Size should be " + (i + 1) + " (full capacity)");
		assertFalse(cb.isEmpty(), "Full buffer should not be empty");
		assertTrue(cb.isFull(), "Buffer should now be full");
		assertEquals(0, (int) cb.peek(), "Peek on full buffer should still return the first element (0)");
		
		i++; // This element would be testCapacity-th index if it were an array, i.e., 15th element if capacity is 15
		assertFalse(cb.add(i), "Adding to full buffer (overwrite off) should fail for element " + i);
		
		assertEquals(0, (int) cb.remove(), "Removing from full buffer should return the first element (0)");
		assertEquals(testCapacity, cb.capacity(), "Capacity should not change after remove");
		assertEquals(testCapacity - 1, cb.size(), "Size should decrease by 1 after remove");
		assertFalse(cb.isEmpty(), "Buffer should not be empty after one remove");
		assertFalse(cb.isFull(), "Buffer should not be full after one remove");
		assertEquals(1, (int) cb.peek(), "Peek should return the new first element (1)");
		
		assertTrue(cb.add(i), "Adding element " + i + " after one remove should succeed");
		
		cb.setOverwrite(true);
		assertTrue(cb.getOverwrite(), "Overwrite flag should now be true");
		int valToAdd = i + 1; // e.g. 16 if testCapacity was 15
		assertTrue(cb.add(valToAdd), "Adding element " + valToAdd + " with overwrite should succeed");
		assertEquals(2, (int) cb.peek(), "After overwriting 1, peek should return 2");
		
		valToAdd++; // e.g. 17
		assertTrue(cb.add(valToAdd), "Adding element " + valToAdd + " with overwrite should succeed again");
		assertEquals(3, (int) cb.peek(), "After overwriting 2, peek should return 3");
	}

	@Test
	public void testAddWithOverwrite() {
		cb = new CircularBuffer<>(testCapacity); // testCapacity is 15
		cb.setOverwrite(true);
		assertTrue(cb.getOverwrite(), "Overwrite should be enabled");

		int i = 0;
		// fill the buffer
		for (i = 0; i < testCapacity; i++) {
			assertTrue(cb.add(i), "Adding element " + i + " with overwrite on should succeed");
			assertEquals(testCapacity, cb.capacity(), "Capacity should remain constant");
			assertEquals(i + 1, cb.size(), "Size should be " + (i + 1));
			assertFalse(cb.isEmpty(), "Buffer should not be empty");
			if (i < testCapacity -1) {
				assertFalse(cb.isFull(), "Buffer should not be full until last element added");
			} else {
				assertTrue(cb.isFull(), "Buffer should be full after " + testCapacity + " elements");
			}
			assertEquals(0, (int) cb.peek(), "Peek should return the first element (0) while filling");
		}
		
		assertEquals(testCapacity, cb.size(), "Buffer size should be at capacity");
		assertTrue(cb.isFull(), "Buffer should be full");
		assertEquals(0, (int) cb.peek(), "Peek should be 0 before overwrite starts affecting head");

		// Add another element to trigger overwrite of the head
		i++; // i is now testCapacity (e.g. 15)
		assertTrue(cb.add(i), "Adding element " + i + " (triggering overwrite) should succeed");
		assertEquals(testCapacity, cb.capacity(), "Capacity should remain constant during overwrite");
		assertEquals(testCapacity, cb.size(), "Size should remain at capacity during overwrite");
		assertFalse(cb.isEmpty(), "Buffer should not be empty during overwrite");
		assertTrue(cb.isFull(), "Buffer should remain full during overwrite");
		assertEquals(1, (int) cb.peek(), "After overwriting 0 with " + i + ", peek should return 1");

		// Add one more element
		i++; // i is now testCapacity + 1 (e.g. 16)
		assertTrue(cb.add(i), "Adding element " + i + " (another overwrite) should succeed");
		assertEquals(2, (int) cb.peek(), "After overwriting 1 with " + i + ", peek should return 2");
	}

	@Test
	public void testRemove() {
		cb = new CircularBuffer<>(testCapacity); // testCapacity is 15
		int i = 0;
		// fill the buffer
		while (!cb.isFull()) {
			assertTrue(cb.add(i), "Adding element " + i + " for removal test");
			i++;
		}
		assertEquals(testCapacity, cb.size(), "Buffer should be full before starting removal tests");

		// check if the elements are removed in the correct order
		for (int j = 0; j < testCapacity; j++) {
			assertEquals(j, (int) cb.remove(), "Removing element " + j + " should return its value");
			assertEquals(testCapacity - j - 1, cb.size(), "Size should be " + (testCapacity - j - 1) + " after " + (j+1) + " removals");
		}
		
		assertEquals(testCapacity, cb.capacity(), "Capacity should not change after all elements removed");
		assertTrue(cb.isEmpty(), "Buffer should be empty after all elements removed");
		assertFalse(cb.isFull(), "Empty buffer should not be full");
		assertNull(cb.peek(), "Peek on empty buffer should return null");
		assertNull(cb.remove(), "Remove on empty buffer should return null");
		assertEquals(0, cb.size(), "Size of an empty buffer should be 0");
	}

	@Test
	public void testClear() {
		cb = new CircularBuffer<>(testCapacity); // testCapacity is 15
		int i = 0;
		// fill the buffer
		while (!cb.isFull()) {
			assertTrue(cb.add(i), "Adding element " + i + " before clearing");
			i++;
		}
		assertFalse(cb.isEmpty(), "Buffer should not be empty before clear");
		assertTrue(cb.isFull(), "Buffer should be full before clear");

		cb.clear();
		
		assertEquals(testCapacity, cb.capacity(), "Capacity should not change after clear");
		assertTrue(cb.isEmpty(), "Buffer should be empty after clear");
		assertFalse(cb.isFull(), "Cleared buffer should not be full");
		assertNull(cb.peek(), "Peek on cleared buffer should return null");
		assertNull(cb.remove(), "Remove on cleared buffer should return null");
		assertEquals(0, cb.size(), "Size of cleared buffer should be 0");
	}

    @Test
    public void testAddAndRemove_WithString() {
        CircularBuffer<String> buffer = new CircularBuffer<>(3);
        assertTrue(buffer.add("one"), "Adding 'one' should succeed");
        assertTrue(buffer.add("two"), "Adding 'two' should succeed");
        assertTrue(buffer.add("three"), "Adding 'three' should succeed");
        assertFalse(buffer.add("four"), "Adding 'four' to full buffer (overwrite off) should fail");
        assertEquals("one", buffer.remove(), "First removed element should be 'one'");
        assertEquals("two", buffer.remove(), "Second removed element should be 'two'");
        assertEquals("three", buffer.remove(), "Third removed element should be 'three'");
        assertNull(buffer.remove(), "Removing from empty string buffer should return null");
    }

    @Test
    public void testEnd() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(3);
        assertNull(buffer.end(), "End on empty buffer should be null");

        buffer.add(1);
        assertEquals(1, buffer.end(), "End should be 1 after adding 1");
        assertEquals(1, buffer.size(), "Size should be 1 after adding 1");

        buffer.add(2);
        assertEquals(2, buffer.end(), "End should be 2 after adding 1, 2");
        assertEquals(2, buffer.size(), "Size should be 2 after adding 1, 2");
        
        buffer.add(3);
        assertEquals(3, buffer.end(), "End should be 3 after adding 1, 2, 3");
        assertEquals(3, buffer.size(), "Size should be 3 (full)");

        Integer removed = buffer.remove(); // Removes 1
        assertEquals(1, removed, "Removed element should be 1");
        assertEquals(3, buffer.end(), "End should still be 3 (last logical element) after removing 1");
        assertEquals(2, buffer.size(), "Size should be 2 after removing 1");

        // Test with overwrite
        CircularBuffer<Integer> overwriteBuffer = new CircularBuffer<>(3);
        overwriteBuffer.setOverwrite(true);
        assertNull(overwriteBuffer.end(), "End on empty overwriteBuffer should be null");

        overwriteBuffer.add(10);
        assertEquals(10, overwriteBuffer.end(), "End on overwriteBuffer should be 10");
        overwriteBuffer.add(20);
        assertEquals(20, overwriteBuffer.end(), "End on overwriteBuffer should be 20");
        overwriteBuffer.add(30);
        assertEquals(30, overwriteBuffer.end(), "End on overwriteBuffer should be 30 (full)");
        
        overwriteBuffer.add(40); // Overwrites 10
        assertEquals(40, overwriteBuffer.end(), "End should be 40 after overwriting 10");
        assertEquals(3, overwriteBuffer.size(), "Size should remain 3 after overwrite");
        assertEquals(20, (int)overwriteBuffer.peek(), "Peek should be 20 after 10 was overwritten by 40");


        overwriteBuffer.add(50); // Overwrites 20
        assertEquals(50, overwriteBuffer.end(), "End should be 50 after overwriting 20");
        assertEquals(3, overwriteBuffer.size(), "Size should remain 3 after another overwrite");
        assertEquals(30, (int)overwriteBuffer.peek(), "Peek should be 30 after 20 was overwritten by 50");
    }

    @Test
    public void testPeekAndEndOnEmptyBuffer() {
        CircularBuffer<String> buffer = new CircularBuffer<>(5); // Arbitrary capacity
        assertNull(buffer.peek(), "Peek on a freshly created empty buffer should be null");
        assertNull(buffer.end(), "End on a freshly created empty buffer should be null");
        assertTrue(buffer.isEmpty(), "Freshly created buffer should be empty");
        assertEquals(0, buffer.size(), "Freshly created buffer should have size 0");
    }
}
