/*
* Copyright (c) 2021, The beep-projects contributors
* this file originated from https://github.com/beep-projects
* Do not remove the lines above.
* The rest of this source code is subject to the terms of the Mozilla Public License.
* You can obtain a copy of the MPL at <https://www.mozilla.org/MPL/2.0/>.
*/
package de.freaklamarsch.systarest.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;

import org.junit.Test;

import de.freaklamarsch.systarest.DataLogger;
import de.freaklamarsch.systarest.DataLogger.DataLoggerStatus;

public class DataLoggerTest {

	DataLogger logger = null;
	Integer[] data = { -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };;
	long timestamp = -1;

	public DataLoggerTest() {
		logger = new DataLogger();
	}

	@Test
	public void testConstructor() {
		DataLoggerStatus dls = logger.getStatus();
		// this should be the default units
		try {
			Field defaultCapacityField = DataLogger.class.getDeclaredField("DEFAULT_CAPACITY");
			defaultCapacityField.setAccessible(true);
			int defaultCapacity = (int) defaultCapacityField.get(dls);
			assertEquals(defaultCapacity, dls.capacity);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(false, dls.saveLoggedData);
		assertEquals("DataLogger", dls.logFilePrefix);
		assertFalse(dls.logFileRootPath.equals(""));
		assertEquals(";", dls.logEntryDelimiter);
		assertEquals(0, dls.writerFileCount);
		assertEquals(0, dls.bufferedEntries);
	}

	@Test
	public void testBufferingOfData() {
		DataLoggerStatus dls = logger.getStatus();
		for (int i = 0; i < dls.capacity; i++) {
			logger.addData(data, timestamp);
			timestamp++;
		}
		dls = logger.getStatus();
		assertEquals(dls.capacity, dls.bufferedEntries);
		assertEquals(0, dls.writerFileCount);
		logger.addData(data, timestamp);
		timestamp++;
		dls = logger.getStatus();
		assertEquals(dls.capacity, dls.bufferedEntries);
		assertEquals(0, dls.writerFileCount);
		logger.addData(data, timestamp);
		timestamp++;
		dls = logger.getStatus();
		assertEquals(dls.capacity, dls.bufferedEntries);
		assertEquals(0, dls.writerFileCount);
		logger.addData(data, timestamp);
		timestamp++;
		dls = logger.getStatus();
		assertEquals(dls.capacity, dls.bufferedEntries);
		assertEquals(0, dls.writerFileCount);
		logger.addData(data, timestamp);
		timestamp++;
		dls = logger.getStatus();
		assertEquals(dls.capacity, dls.bufferedEntries);
		assertEquals(0, dls.writerFileCount);

		// set logger to save data
		// capacity set to 1 means that each new data will trigger a ne file write
		logger.saveLoggedData("unittest", ";", 1);
		dls = logger.getStatus();
		// this should trigger the logger to save the current buffered entries
		assertEquals(0, dls.bufferedEntries);
		assertEquals(1, dls.writerFileCount);
		assertEquals(1, dls.capacity);
		// this should trigger the logger to save the current buffered entries
		logger.addData(data, timestamp);
		timestamp++;
		dls = logger.getStatus();
		assertEquals(0, dls.bufferedEntries);
		assertEquals(2, dls.writerFileCount);
		assertEquals(1, dls.capacity);
		logger.addData(data, timestamp);
		timestamp++;
		dls = logger.getStatus();
		assertEquals(0, dls.bufferedEntries);
		assertEquals(3, dls.writerFileCount);
		assertEquals(1, dls.capacity);

		// change capacity to 3 data elements,
		// i.e. only after 3 added elements a new file is written
		logger.saveLoggedData(3);
		logger.addData(data, timestamp);
		timestamp++;
		logger.addData(data, timestamp);
		timestamp++;
		// the 2 entries should be buffered
		// no new file should be written
		dls = logger.getStatus();
		assertEquals(2, dls.bufferedEntries);
		assertEquals(3, dls.writerFileCount);
		assertEquals(3, dls.capacity);
		logger.addData(data, timestamp);
		timestamp++;
		// a new file should be written
		dls = logger.getStatus();
		assertEquals(0, dls.bufferedEntries);
		assertEquals(4, dls.writerFileCount);
		assertEquals(3, dls.capacity);

		logger.stopSavingLoggedData();
		// if not writing log files
		// the logger should keep capacity elements in the buffer
		// and write them when saving is enabled
		logger.addData(data, timestamp);
		timestamp++;
		logger.addData(data, timestamp);
		timestamp++;
		logger.addData(data, timestamp);
		timestamp++;
		dls = logger.getStatus();
		assertEquals(3, dls.bufferedEntries);
		assertEquals(4, dls.writerFileCount);
		assertEquals(3, dls.capacity);
		// adding elements should not trigger saving
		logger.addData(data, timestamp);
		timestamp++;
		dls = logger.getStatus();
		assertEquals(3, dls.bufferedEntries);
		assertEquals(4, dls.writerFileCount);
		assertEquals(3, dls.capacity);
		// adding elements should not trigger saving
		logger.addData(data, timestamp);
		timestamp++;
		dls = logger.getStatus();
		assertEquals(3, dls.bufferedEntries);
		assertEquals(4, dls.writerFileCount);
		assertEquals(3, dls.capacity);
		// adding elements should not trigger saving
		logger.addData(data, timestamp);
		timestamp++;
		dls = logger.getStatus();
		assertEquals(3, dls.bufferedEntries);
		assertEquals(4, dls.writerFileCount);
		assertEquals(3, dls.capacity);
		// adding elements should not trigger saving
		logger.addData(data, timestamp);
		timestamp++;
		dls = logger.getStatus();
		assertEquals(3, dls.bufferedEntries);
		assertEquals(4, dls.writerFileCount);
		assertEquals(3, dls.capacity);
		// when saving is enabled, the current buffer should be written
		logger.saveLoggedData();
		dls = logger.getStatus();
		assertEquals(0, dls.bufferedEntries);
		assertEquals(5, dls.writerFileCount);
		assertEquals(3, dls.capacity);
	}
}
