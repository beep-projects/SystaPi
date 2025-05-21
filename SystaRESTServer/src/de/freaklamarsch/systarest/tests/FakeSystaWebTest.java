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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;

import de.freaklamarsch.systarest.CircularBuffer;
import de.freaklamarsch.systarest.DataLogger;
import de.freaklamarsch.systarest.DataLogger.DataLoggerStatus;
import de.freaklamarsch.systarest.FakeSystaWeb;
import de.freaklamarsch.systarest.FakeSystaWeb.FakeSystaWebStatus;
import de.freaklamarsch.systarest.SystaStatus;

class FakeSystaWebTest {
	ByteBuffer[] data = null;
	private static final String logDir = "./SystaLogs";
	private FilenameFilter logfileFilter;
	private Field logFileFilterStringField;
	private String logFileFilterString;
	private FakeSystaWeb fsw;
	private Field RING_BUFFER_SIZE;
	private Field readIndex;
	private Field writeIndex;
	private Field timestamp;
	private Field socketField;
	private DatagramSocket socket;
	private Field intData;
	private DataLogger<Integer> logInt;
	private Field logIntDataBufferField;
	private CircularBuffer<Integer> logIntDataBuffer;
	private DataLogger<Byte> logRaw;

	FakeSystaWebTest() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException,
			NoSuchMethodException {
		// change the default log dir, so we do not mess with the users home directory
		changeDefaultLogDir(logDir);
		initialize();
	}

	/**
	 * initialize components for access via reflection
	 */
	@SuppressWarnings("unchecked")
	private boolean initialize() {
		fsw = new FakeSystaWeb();
		logInt = null;
		logRaw = null;
		try {
			RING_BUFFER_SIZE = FakeSystaWeb.class.getDeclaredField("RING_BUFFER_SIZE");
			RING_BUFFER_SIZE.setAccessible(true);
			readIndex = FakeSystaWeb.class.getDeclaredField("readIndex");
			readIndex.setAccessible(true);
			writeIndex = FakeSystaWeb.class.getDeclaredField("writeIndex");
			writeIndex.setAccessible(true);
			timestamp = FakeSystaWeb.class.getDeclaredField("timestamp");
			timestamp.setAccessible(true);
			socket = new DatagramSocket();
			socketField = FakeSystaWeb.class.getDeclaredField("socket");
			socketField.setAccessible(true);
			socketField.set(fsw, socket);
			intData = FakeSystaWeb.class.getDeclaredField("intData");
			intData.setAccessible(true);
			Field logRawField = FakeSystaWeb.class.getDeclaredField("logRaw");
			logRawField.setAccessible(true);
			logRaw = (DataLogger<Byte>) logRawField.get(fsw);
			logRaw.getClass().getMethod("addData", Object[].class, long.class);
			Field logIntField = fsw.getClass().getDeclaredField("logInt");
			logIntField.setAccessible(true);
			logInt = (DataLogger<Integer>) logIntField.get(fsw);
			logIntDataBufferField = logInt.getClass().getDeclaredField("dataBuffer");
			logIntDataBufferField.setAccessible(true);
			logIntDataBuffer = (CircularBuffer<Integer>) logIntDataBufferField.get(logInt);
			logInt.getClass().getDeclaredMethod("addData", Object[].class, long.class);
			logFileFilterStringField = FakeSystaWeb.class.getDeclaredField("logFileFilterString");
			logFileFilterStringField.setAccessible(true);
			logFileFilterString = (String) logFileFilterStringField.get(fsw);
			logfileFilter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.matches(logFileFilterString);
				}
			};
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception during test initialization: " + e);
			return false;
		}
		initializeData();
		return true;
	}

	@Test
	void findSC() {
		FakeSystaWeb fsw = new FakeSystaWeb();
		fsw.findSystaComfort();
	}

	@Test
	void testProcessDataType1() {
		initialize();
		// processType1 is only called from run(), so we have to set some
		// variables first which usually get set by run()
		assertTrue(updateWriteIndexAndTimestamp(fsw));
		// now we can invoke processType1
		Method processDataType1 = null;
		try {
			processDataType1 = prepareInvokeMethod("processDataType1", ByteBuffer.class);
			processDataType1.invoke(fsw, data[0]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown when trying to obtain method processDataType1");
		}
		SystaStatus status = fsw.getParadigmaStatus();
		data[0].position(0);
		assertEquals(1048, data[0].remaining());
		assertEquals(25.3, status.outsideTemp);
		assertEquals(30.9, status.circuit1FlowTemp);
		assertEquals(31.2, status.circuit1ReturnTemp);
		assertEquals(0, status.circuit1OperationMode);
		assertEquals(79.3, status.hotWaterTemp);
		assertEquals(76.4, status.bufferTempTop);
		assertEquals(57.6, status.bufferTempBottom);
		assertEquals(-30, status.circulationTemp);
		assertEquals(-30, status.circuit2FlowTemp);
		assertEquals(-30, status.circuit2ReturnTemp);
		assertEquals(0, status.roomTempActual1);
		assertEquals(0, status.roomTempActual2);
		assertEquals(0, status.collectorTempActual);
		assertEquals(28.7, status.boilerFlowTemp);
		assertEquals(28.7, status.boilerReturnTemp);
		assertEquals(26.3, status.logBoilerFlowTemp);
		assertEquals(36.8, status.logBoilerReturnTemp);
		assertEquals(-30.2, status.logBoilerBufferTempTop);
		assertEquals(0, status.swimmingpoolTemp);
		assertEquals(0, status.swimmingpoolFlowTemp);
		assertEquals(0, status.swimmingpoolReturnTemp);
		assertEquals(0, status.hotWaterTempSet);
		assertEquals(0, status.roomTempSet1);
		assertEquals(0, status.circuit1FlowTempSet);
		assertEquals(0, status.circuit2FlowTempSet);
		assertEquals(0, status.roomTempSet2);
		assertEquals(0, status.bufferTempSet);
		assertEquals(0, status.boilerTempSet);
		assertEquals(7, status.operationMode);
		assertEquals(21, status.roomTempSetNormal);
		assertEquals(24, status.roomTempSetComfort);
		assertEquals(18, status.roomTempSetLowering);
		assertEquals(0, status.heatingOperationMode);
		assertEquals(0, status.controlledBy);
		assertEquals(34, status.heatingCurveBasePoint);
		assertEquals(0.4, status.heatingCurveGradient);
		assertEquals(60, status.maxFlowTemp);
		assertEquals(15, status.heatingLimitTemp);
		assertEquals(15, status.heatingLimitTeampLowering);
		assertEquals(2, status.antiFreezeOutsideTemp);
		assertEquals(30, status.heatUpTime);
		assertEquals(0, status.roomImpact);
		assertEquals(0, status.boilerSuperelevation);
		assertEquals(20, status.heatingCircuitSpreading);
		assertEquals(100, status.heatingPumpSpeedMin);
		assertEquals(2, status.mixerRuntime);
		assertEquals(0, status.roomTempCorrection);
		assertEquals(35, status.underfloorHeatingBasePoint);
		assertEquals(1.3, status.underfloorHeatingGradient);
		assertEquals(55, status.hotWaterTempNormal);
		assertEquals(60, status.hotWaterTempComfort);
		assertEquals(3, status.hotWaterOperationMode);
		assertEquals(5, status.hotWaterHysteresis);
		assertEquals(65, status.hotWaterTempMax);
		assertEquals(10, status.heatingPumpOverrun);
		assertEquals(0, status.heatingPumpSpeedActual);
		assertEquals(85, status.bufferTempMax);
		assertEquals(1, status.bufferTempMin);
		assertEquals(5, status.boilerHysteresis);
		assertEquals(5, status.boilerOperationTime);
		assertEquals(25, status.boilerShutdownTemp);
		assertEquals(25, status.boilerPumpSpeedMin);
		assertEquals(0, status.boilerPumpSpeedActual);
		assertEquals(6, status.boilerOperationMode);
		assertEquals(0, status.circulationOperationMode);
		assertEquals(3, status.circulationPumpOverrun);
		assertEquals(15, status.circulationLockoutTimePushButton);
		assertEquals(5, status.circulationHysteresis);
		assertEquals(0, status.adjustRoomTempBy);
		assertEquals(3885, status.boilerOperationTimeHours);
		assertEquals(48, status.boilerOperationTimeMinutes);
		assertEquals(2414, status.burnerNumberOfStarts);
		assertEquals(0, status.solarPowerActual);
		assertEquals(0, status.solarGainDay);
		assertEquals(0, status.solarGainTotal);
		assertEquals(22, status.systemNumberOfStarts);
		assertEquals(0, status.circuit1LeadTime);
		assertEquals(0, status.circuit2LeadTime);
		assertEquals(0, status.circuit3LeadTime);
		assertEquals(2048, status.relay);
		assertFalse(status.heatingPumpIsOn);
		assertFalse(status.chargePumpIsOn);
		assertFalse(status.circulationPumpIsOn);
		assertFalse(status.boilerIsOn);
		assertFalse(status.burnerIsOn);
		assertFalse(status.boilerLedIsOn);
		assertFalse(status.unknowRelayState1IsOn);
		assertFalse(status.unknowRelayState2IsOn);
		assertFalse(status.mixer1IsOnWarm);
		assertFalse(status.mixer1IsOnCool);
		assertEquals(0, status.mixer1State);
		assertTrue(status.unknowRelayState5IsOn);
		assertEquals(65535, status.error);
		assertEquals(7, status.operationModeX);
		assertEquals(0, status.heatingOperationModeX);
		assertEquals(30.0, status.logBoilerBufferTempMin);
		assertEquals(65.0, status.logBoilerTempMin);
		assertEquals(5.0, status.logBoilerSpreadingMin);
		assertEquals(100, status.logBoilerPumpSpeedMin);
		assertEquals(0, status.logBoilerPumpSpeedActual);
		assertEquals(27, status.logBoilerSettings);
		assertTrue(status.logBoilerParallelOperation);
		assertEquals(0, status.logBoilerOperationMode);
		assertTrue(status.boilerHeatsBuffer);
		assertEquals(3, status.bufferType);
		/*
		 * timestamp is generated with each data paket it cannot be tested statically
		 * status.timestamp; status.timestampString;
		 */
	}

	@Test
	void testProcessDatagram() {
		initialize();
		// testProcessDatagram is only called from run(), so we have to set some
		// variables first which usually get set by run()
		assertTrue(updateWriteIndexAndTimestamp(fsw));
		// now we can invoke processType1
		Method processDatagram = null;
		try {
			processDatagram = prepareInvokeMethod("processDatagram", ByteBuffer.class);
			processDatagram.invoke(fsw, data[1]); // type 0x00, not logged
			Thread.sleep(5);
			processDatagram.invoke(fsw, data[2]);
			Thread.sleep(5);
			processDatagram.invoke(fsw, data[3]);
			Thread.sleep(5);
			processDatagram.invoke(fsw, data[4]);
			Thread.sleep(5);
			processDatagram.invoke(fsw, data[5]); // type 0x00, not logged
			Thread.sleep(5);
			processDatagram.invoke(fsw, data[6]);
			Thread.sleep(5);
			processDatagram.invoke(fsw, data[7]);
			Thread.sleep(5);
			processDatagram.invoke(fsw, data[8]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown when trying to obtain method testProcessDatagram");
		}
		FakeSystaWebStatus fswStatus = fsw.getStatus();
		DataLoggerStatus logIntStatus = logInt.getStatus();
		// data[0].position(0);
		assertEquals(6, logIntDataBuffer.size());
		assertFalse(fswStatus.running);// fsw was not started for this test
		assertTrue(fswStatus.connected);// connected is calculated from processed packets
		assertEquals(logIntStatus.lastTimestamp, fswStatus.lastTimestamp);
		assertEquals(0, fswStatus.dataPacketsReceived);// we did not use the receive method
		assertEquals(6, logIntStatus.bufferedEntries);
	}

	@Test
	void testGetAllLogs() {
		// make sure initialization is successfull
		assertTrue(initialize());
		int sendXPackets = 151;
		int maxDataIdx = 8;
		int numOfEntries = 10;
		// for 151 logged packets, with 10 entries per file,
		// we should see 26 files, 15 raw and 11 data, because not all packets are
		// logged to data
		int expectedNumOfFiles = 26;
		File logs = new File(logDir);
		if (!logs.exists()) {
			logs.mkdirs();
		}
		// clean folder
		for (File file : logs.listFiles()) {
			file.delete();
		}
		// add data packets to the logs
		try {
			Method processDatagram = prepareInvokeMethod("processDatagram", ByteBuffer.class);
			// enable logging
			fsw.logRawData("test", "<>", numOfEntries);
			// "send" 151 packets
			for (int i = 1; i <= sendXPackets; i++) {
				data[(i) % maxDataIdx].position(0);
				processDatagram.invoke(fsw, data[(i) % maxDataIdx]);
				Thread.sleep(2);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown while adding packets to DataLoggers" + e);
		}
		// get the files created by the DataLoggers
		File[] files = logs.listFiles(logfileFilter);
		assertEquals(expectedNumOfFiles, files.length);
		// validate the content of the zip
		File zf = fsw.getAllLogs();

		ZipFile zipFile = null;
		try {
			// open a zip file for reading
			zipFile = new ZipFile(zf);
			// check if we have expectedNumOfFiles entries
			assertEquals(expectedNumOfFiles, zipFile.size());
			// get an enumeration of the ZIP file entries
			Enumeration<? extends ZipEntry> e = zipFile.entries();
			while (e.hasMoreElements()) {
				ZipEntry entry = e.nextElement();
				// get the name of the entry
				String entryName = entry.getName();
				// check if the name of the entry is contained in the filtered log files present
				// in logDir
				assertTrue(Arrays.stream(files).anyMatch(x -> entryName.equals(x.getName())));
			}
		} catch (IOException ioe) {
			fail("IOException when opening zip file: " + ioe);
		} finally {
			try {
				if (zipFile != null) {
					zipFile.close();
				}
			} catch (IOException ioe) {
				// closing the file is not subject of this test, but the tester should be
				// informed
				System.out.println("Error while closing zip file" + ioe);
			}
		}
		// now we can use this test to also test deleteAllLogs
		// first add a file that should not be deleted
		File newFile = new File(logDir + File.separator + "dont-delete-data[0].txt");
		try {
			newFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// delete the logs
		fsw.deleteAllLogs();
		// check what is left in the directory
		files = logs.listFiles(logfileFilter);
		// now all lof files should be gone
		assertEquals(0, files.length);
		// now there should be at least two files left
		// the one we have just created and the zip file
		assertNotEquals(0, logs.listFiles());
	}

	/**
	 * @param methodName the name of the method, that should be retrieved from
	 *                   FakeSystaWeb
	 * @return Method processDatagram from fsw
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * 
	 *                                  private Method prepareInvokeMethod(String
	 *                                  methodName) throws NoSuchFieldException,
	 *                                  SecurityException, IllegalArgumentException,
	 *                                  IllegalAccessException,
	 *                                  NoSuchMethodException { //Method
	 *                                  processDataType1 =
	 *                                  FakeSystaWeb.class.getDeclaredMethod("processDataType1",
	 *                                  ByteBuffer.class); Method method =
	 *                                  FakeSystaWeb.class.getDeclaredMethod(methodName,
	 *                                  ByteBuffer.class);
	 *                                  method.setAccessible(true); return method; }
	 */

	/**
	 * @param methodName the name of the method, that should be retrieved from
	 *                   FakeSystaWeb
	 * @return Method processDatagram from fsw
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 */
	private Method prepareInvokeMethod(String methodName, Class<?>... parameters) throws NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException {
		// Method processDataType1 =
		// FakeSystaWeb.class.getDeclaredMethod("processDataType1", ByteBuffer.class);
		Method method = FakeSystaWeb.class.getDeclaredMethod(methodName, parameters);
		method.setAccessible(true);
		return method;
	}

	/**
	 * update the writeIndex and timestamp as it is done inside the FakeSystaWeb run
	 * method
	 * 
	 * @param fsw
	 * @return true if everything worked fine, false if an error occurred
	 */
	private boolean updateWriteIndexAndTimestamp(FakeSystaWeb fsw) {
		try {
			// set the write index
			writeIndex.setInt(fsw, (readIndex.getInt(fsw) + 1) % RING_BUFFER_SIZE.getInt(fsw));
			// set the timestamp
			long[] tmpTimestamp = (long[]) timestamp.get(fsw);
			// make sure timestamp advances with every call
			tmpTimestamp[writeIndex.getInt(fsw)] = (tmpTimestamp[writeIndex.getInt(fsw)] == -1)
					? LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
					: tmpTimestamp[writeIndex.getInt(fsw)] + 1;
			timestamp.set(fsw, tmpTimestamp);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/*
	 * TODO add new tests for messages sent to SystaComfort, once the rework of this
	 * part is finished
	 * 
	 * @Test void testCreateReply() { initializeData(); FakeSystaWeb fsw = new
	 * FakeSystaWeb(); Method method = null;
	 * 
	 * try { method = FakeSystaWeb.class.getDeclaredMethod("createReply",
	 * ByteBuffer.class); method.setAccessible(true); method.invoke(fsw, data); }
	 * catch (Exception e) { e.printStackTrace(); fail("Exception thrown"); } try {
	 * Field replyField = FakeSystaWeb.class.getDeclaredField("reply");
	 * replyField.setAccessible(true); byte[] reply = (byte[]) replyField.get(fsw);
	 * assertEquals(0, reply[0]); assertEquals(-105, reply[1]); assertEquals(-66,
	 * reply[2]); assertEquals(44, reply[3]); assertEquals(62, reply[4]);
	 * assertEquals(108, reply[5]); assertEquals(-4, reply[6]); assertEquals(6,
	 * reply[7]); assertEquals(0, reply[8]); assertEquals(0, reply[9]);
	 * assertEquals(0, reply[10]); assertEquals(0, reply[11]); assertEquals(1,
	 * reply[12]); assertEquals(0, reply[13]); assertEquals(0, reply[14]);
	 * assertEquals(0, reply[15]); assertEquals(-63, reply[16]); assertEquals(-6,
	 * reply[17]); assertEquals(-69, reply[18]); assertEquals(70, reply[19]); }
	 * catch (Exception e) { e.printStackTrace(); fail("Exception thrown"); } }
	 */

	/**
	 * Use reflection to change the hardcoded directory used for the location of log
	 * files. As DEFAULT_ROOT_PATH is static final in DataLogger, it is officially a
	 * security risk to change this value. This function might not work with future
	 * Java versions.
	 */
	private void changeDefaultLogDir(String newDir) {
		try {
			// HACK to change the default folder to avoid that this test does any damage to
			// the users home folder
			// its not a nice way
			// Field modifiersField = Field.class.getDeclaredField("modifiers");
			// modifiersField.setAccessible(true);
			// modifiersField.set(DataLogger.class, DataLogger.class.getModifiers() &
			// ~Modifier.FINAL);
			Field DEFAULT_ROOT_PATH;
			Field LOG_PATH;
			VarHandle MODIFIERS;
			var lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
			MODIFIERS = lookup.findVarHandle(Field.class, "modifiers", int.class);
			DEFAULT_ROOT_PATH = DataLogger.class.getDeclaredField("DEFAULT_ROOT_PATH");
			int mods = DEFAULT_ROOT_PATH.getModifiers();
			if (Modifier.isFinal(mods)) {
				MODIFIERS.set(DEFAULT_ROOT_PATH, mods & ~Modifier.FINAL);
			}
			DEFAULT_ROOT_PATH.setAccessible(true);
			DEFAULT_ROOT_PATH.set(DataLogger.class, newDir);

			LOG_PATH = FakeSystaWeb.class.getDeclaredField("LOG_PATH");
			mods = LOG_PATH.getModifiers();
			if (Modifier.isFinal(mods)) {
				MODIFIERS.set(LOG_PATH, mods & ~Modifier.FINAL);
			}
			LOG_PATH.setAccessible(true);
			LOG_PATH.set(DataLogger.class, newDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeData() {
		data = new ByteBuffer[9];
		for (int i = 0; i < data.length; i++) {
			data[i] = ByteBuffer.allocate(1048).order(ByteOrder.LITTLE_ENDIAN);
		}
		// load a captured packets for tests
		String testDir = this.getClass().getResource(".").getPath();
		readHexTextIntoByteBuffer(data[0], testDir + "data00_09_00.txt");
		readHexTextIntoByteBuffer(data[1], testDir + "data01_09_00.txt");
		readHexTextIntoByteBuffer(data[2], testDir + "data02_09_01.txt");
		readHexTextIntoByteBuffer(data[3], testDir + "data03_09_02.txt");
		readHexTextIntoByteBuffer(data[4], testDir + "data04_09_03.txt");
		readHexTextIntoByteBuffer(data[5], testDir + "data05_09_00.txt");
		readHexTextIntoByteBuffer(data[6], testDir + "data06_09_01.txt");
		readHexTextIntoByteBuffer(data[7], testDir + "data07_09_02.txt");
		readHexTextIntoByteBuffer(data[8], testDir + "data08_09_03.txt");
	}

	/**
	 * loads a hexText file into a ByteBuffer. A hexText file is a file, that has
	 * the Hex Stream of a captured packet as a single line. The size of the
	 * ByteBuffer has to match the Hex Stream in the file. No checks are done.
	 * 
	 * @param byteBuffer  the buffer in which the bytes from the hexText file should
	 *                    be stored
	 * @param hexTextFile path to the hexText file
	 */
	private void readHexTextIntoByteBuffer(ByteBuffer byteBuffer, String hexTextFile) {
		try (Scanner scanner = new Scanner(new File(hexTextFile))) {
			scanner.findAll("[0-9A-Fa-f]{2}").mapToInt(m -> Integer.parseInt(m.group(), 16)).forEachOrdered(i -> {
				if (byteBuffer.hasRemaining())
					byteBuffer.put((byte) i);
			});
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		System.out.print("Test Data loaded: ");
		for (int i = 0; i < byteBuffer.limit(); i++) {
			System.out.format("%02x", byteBuffer.get(i));
		}
		System.out.println();
		// reset the buffer position to 0
		byteBuffer.position(0);
	}

}
