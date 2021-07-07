package de.freaklamarsch.systarest.tests;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Field;

import org.junit.Test;

import de.freaklamarsch.systarest.CircularBuffer;

public class CircularBufferTest {
    final int testCapacity = 15;
	CircularBuffer<Integer> cb;// = new CircularBuffer<Integer>(testCapacity);

	public CircularBufferTest() {
		cb = new CircularBuffer<Integer>(testCapacity);
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
		cb = new CircularBuffer<Integer>(testCapacity);
		assertEquals(testCapacity, cb.capacity());
		assertEquals(0, cb.size());
		assertEquals(true, cb.isEmpty());
		assertEquals(false, cb.isFull());
		assertEquals(null, cb.peek());
		assertEquals(null, cb.remove());
		//test constructor with negative capacity
		//this should create a buffer with DEFAULT_CAPACITY
		cb = new CircularBuffer<Integer>(-1);
		assertEquals(defaultCapacity, cb.capacity());
		assertEquals(0, cb.size());
		assertEquals(true, cb.isEmpty());
		assertEquals(false, cb.isFull());
		assertEquals(null, cb.peek());
		assertEquals(null, cb.remove());
		//test constructor with 0 capacity
		cb = new CircularBuffer<Integer>(0);
		assertEquals(defaultCapacity, cb.capacity());
		assertEquals(0, cb.size());
		assertEquals(true, cb.isEmpty());
		assertEquals(false, cb.isFull());
		assertEquals(null, cb.peek());
		assertEquals(null, cb.remove());
	}

	@Test
	public void testAdd() {
		cb = new CircularBuffer<Integer>(testCapacity);
		cb.setOverwrite(false);
		int i = 0;
		//fill the buffer to testCapacity-1 elements
		for(i=0;i<testCapacity-1;i++) {
			//add i to the buffer and check the behavior of the buffer
			assertEquals(true, cb.add(i));
			//capacity should not change
			assertEquals(testCapacity, cb.capacity());
			//size should be equal i+1, because i starts at 0
			assertEquals(i+1, cb.size());
			//the buffer should no longer be empty
			assertEquals(false, cb.isEmpty());
			//the buffer should not be full, because i runs only to 1 less the capacity
			assertEquals(false, cb.isFull());
			//peek should return the first element, which is 0
			assertEquals(0, (int)cb.peek());
		}
		//add i to the buffer and check the behavior of the buffer
		assertEquals(true, cb.add(i));
		//capacity should not change
		assertEquals(testCapacity, cb.capacity());
		//size should be equal i+1, because i starts at 0
		assertEquals(i+1, cb.size());
		//the buffer should no longer be empty
		assertEquals(false, cb.isEmpty());
		//the buffer should now be full
		assertEquals(true, cb.isFull());
		//peek should return the first element, which is 0
		assertEquals(0, (int)cb.peek());
		i++;
		//buffer is now full, so this add should fail
		assertEquals(false, cb.add(i));
		//remove should return the first element, which is 0
		assertEquals(0, (int)cb.remove());
		//capacity should not change
		assertEquals(testCapacity, cb.capacity());
		//size should be equal capacity - 1, because we have removed one element from a full buffer
		assertEquals(testCapacity-1, cb.size());
		//the buffer should no longer be empty
		assertEquals(false, cb.isEmpty());
		//the buffer should not be full
		assertEquals(false, cb.isFull());
		//peek should return the first element, which is now 1
		assertEquals(1, (int)cb.peek());
		//buffer is not full, so this add should now succeed
		assertEquals(true, cb.add(i));
		//test if overwrite works
		cb.setOverwrite(true);
		assertEquals(true, cb.add(++i));
		//first element should be overwritten, peek should now return 2
		assertEquals(2, (int)cb.peek());
		//test overwrite again
		assertEquals(true, cb.add(++i));
		//second element should be overwritten, peek should now return 3
		assertEquals(3, (int)cb.peek());
	}

	@Test
	public void testAddWithOverwrite() {
		cb = new CircularBuffer<Integer>(testCapacity);
		cb.setOverwrite(true);
		int i = 0;
		//fill the buffer to testCapacity-1 elements
		for(i=0;i<testCapacity-1;i++) {
			//add i to the buffer and check the behavior of the buffer
			assertEquals(true, cb.add(i));
			//capacity should not change
			assertEquals(testCapacity, cb.capacity());
			//size should be equal i+1, because i starts at 0
			assertEquals(i+1, cb.size());
			//the buffer should no longer be empty
			assertEquals(false, cb.isEmpty());
			//the buffer should not be full, because i runs only to 1 less the capacity
			assertEquals(false, cb.isFull());
			//peek should return the first element, which is 0
			assertEquals(0, (int)cb.peek());
		}
		//add i to the buffer and check the behavior of the buffer
		assertEquals(true, cb.add(i));
		//capacity should not change
		assertEquals(testCapacity, cb.capacity());
		//size should be equal i+1, because i starts at 0
		assertEquals(i+1, cb.size());
		//size should be equal to testCapacity
		assertEquals(testCapacity, cb.size());
		//the buffer should no longer be empty
		assertEquals(false, cb.isEmpty());
		//the buffer should now be full
		assertEquals(true, cb.isFull());
		//peek should return the first element, which is 0
		assertEquals(0, (int)cb.peek());
        //add another element to se the overwrite in action
		i++;
		//add i to the buffer and check the behavior of the buffer
		assertEquals(true, cb.add(i));
		//capacity should not change
		assertEquals(testCapacity, cb.capacity());
		//size should be equal i, because we start overwriting elements, so it is no longer i+1
		assertEquals(i, cb.size());
		//size should be equal to testCapacity
		assertEquals(testCapacity, cb.size());
		//the buffer should no longer be empty
		assertEquals(false, cb.isEmpty());
		//the buffer should now be full
		assertEquals(true, cb.isFull());
		//peek should return the second element, which is 1, because the first one got overwritten
		assertEquals(1, (int)cb.peek());
	}
	
	@Test
	public void testRemove() {
		cb = new CircularBuffer<Integer>(testCapacity);
		int i = 0;
		//fill the buffer
		while(!cb.isFull()) {
			cb.add(i);
			i++;
		}
		//check if the elements are removed with the correct order
		i = 0;
		while(!cb.isEmpty()) {
			assertEquals(Integer.valueOf(i), cb.remove());
			//size of the buffer should be reduced by one with each call to remove
			assertEquals(testCapacity-i-1, cb.size());
			i++;
		}
		//capacity should not change
		assertEquals(testCapacity, cb.capacity());
		//the buffer should now be empty
		assertEquals(true, cb.isEmpty());
		//the buffer should not be full
		assertEquals(false, cb.isFull());
		//peek() on an empty buffer should return null
		assertEquals(null, cb.peek());
		//remove() on an empty buffer should return null
		assertEquals(null, cb.remove());
		//size of an empty buffer should be 0
		assertEquals(0, cb.size());

	}

	@Test
	public void testClear() {
		cb = new CircularBuffer<Integer>(testCapacity);
		int i = 0;
		//fill the buffer
		while(!cb.isFull()) {
			cb.add(i);
			i++;
		}
		cb.clear();
		//capacity should not change
		assertEquals(testCapacity, cb.capacity());
		//the buffer should now be empty
		assertEquals(true, cb.isEmpty());
		//the buffer should not be full
		assertEquals(false, cb.isFull());
		//peek() on an empty buffer should return null
		assertEquals(null, cb.peek());
		//remove() on an empty buffer should return null
		assertEquals(null, cb.remove());
		//size of an empty buffer should be 0
		assertEquals(0, cb.size());
	}

}
