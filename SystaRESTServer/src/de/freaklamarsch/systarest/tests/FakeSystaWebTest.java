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
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;

import de.freaklamarsch.systarest.DataLogger;
import de.freaklamarsch.systarest.FakeSystaWeb;
import de.freaklamarsch.systarest.SystaStatus;

class FakeSystaWebTest {
	ByteBuffer data = ByteBuffer.allocate(1048).order(ByteOrder.LITTLE_ENDIAN);
	private static final String logDir = "./SystaLogs";
	private FilenameFilter logfileFilter;
	private Field logFileFilterStringField;
	private String logFileFilterString;
	private FakeSystaWeb fsw;
	private Field RING_BUFFER_SIZE;
	private Field readIndex;
	private Field writeIndex;
	private Field timestamp;
	private Field intData;
	private Method logRawAddData;
	private Method logIntAddData;
	private DataLogger<Integer> logInt;
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
			intData = FakeSystaWeb.class.getDeclaredField("intData");
			intData.setAccessible(true);
			Field logRawField = FakeSystaWeb.class.getDeclaredField("logRaw");
			logRawField.setAccessible(true);
			logRaw = (DataLogger<Byte>) logRawField.get(fsw);
			logRawAddData = logRaw.getClass().getMethod("addData", Object[].class, long.class);
			Field logIntField = FakeSystaWeb.class.getDeclaredField("logInt");
			logIntField.setAccessible(true);
			logInt = (DataLogger<Integer>) logIntField.get(fsw);
			logIntAddData = logInt.getClass().getDeclaredMethod("addData", Object[].class, long.class);
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
	void testProcessType1() {
		initialize();
		// processType1 is only called from run(), so we have to set some
		// variables first which usually get set by run()
		assertTrue(updateWriteIndexAndTimestamp(fsw));
		// now we can invoke processType1
		Method processType1 = null;
		try {
			processType1 = prepareInvokeProcessType1(fsw);
			processType1.invoke(fsw, data);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown when trying to obtain method processType1");
		}
		SystaStatus status = fsw.getParadigmaStatus();
		data.position(0);
		assertEquals(1048, data.remaining());
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
	void testGetAllLogs() {
		// make sure initialization is successfull
		assertTrue(initialize());
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
			Method processType1 = prepareInvokeProcessType1(fsw);
			// enable logging
			fsw.logRawData("test", "<>", 10);
			// prepare data
			byte[] bytes = data.array();
			Byte[] Data = new Byte[bytes.length];
			int j = 0;
			// Associating Byte array values with bytes. (byte[] to Byte[])
			for (byte b : bytes) {
				Data[j++] = b; // Autoboxing.
			}
			// "send" 151 packets
			for (int i = 0; i < 151; i++) {
				assertTrue(updateWriteIndexAndTimestamp(fsw));
				logRawAddData.invoke(logRaw, Data, ((long[]) timestamp.get(fsw))[writeIndex.getInt(fsw)]);
				if ((i % 3) == 0) {
					// mimic the behavior that each 3rd packet is a data packet
					processType1.invoke(fsw, data);
					logIntAddData.invoke(logInt, ((Integer[][]) intData.get(fsw))[readIndex.getInt(fsw)],
							((long[]) timestamp.get(fsw))[readIndex.getInt(fsw)]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown while adding packets to DataLoggers" + e);
		}
		// get the files created by the DataLoggers
		File[] files = logs.listFiles(logfileFilter);
		// for 151 logged pakets, we should see 10 files, 5 raw and 5 data
		assertEquals(9, files.length);
		// validate the content of the zip
		File zf = fsw.getAllLogs();
		
		ZipFile zipFile = null;
		try {
			// open a zip file for reading
			zipFile = new ZipFile(zf);
			// check if we have 10 entries as expected
			assertEquals(9, zipFile.size());
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
		File newFile = new File(logDir + File.separator + "dont-delete-data.txt");
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
	 * @param fsw
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 */
	private Method prepareInvokeProcessType1(FakeSystaWeb fsw) throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException, NoSuchMethodException {
		Method processDataType1 = FakeSystaWeb.class.getDeclaredMethod("processDataType1", ByteBuffer.class);
		processDataType1.setAccessible(true);
		return processDataType1;
	}

	/**
	 * update the writeIndex and timestamp as it is done inside the FakeSystaWeb run method
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initializeData() {
		// load a captured packet
		data.position(0);
		data.put((byte) 0);
		data.put((byte) -105);
		data.put((byte) -66);
		data.put((byte) 44);
		data.put((byte) 62);
		data.put((byte) 108);
		data.put((byte) -4);
		data.put((byte) 6);
		data.put((byte) 9);
		data.put((byte) 9);
		data.put((byte) 12);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) -36);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 53);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 56);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 25);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -4);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 64);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -44);
		data.put((byte) -2);
		data.put((byte) -1);
		data.put((byte) -1);
		data.put((byte) -44);
		data.put((byte) -2);
		data.put((byte) -1);
		data.put((byte) -1);
		data.put((byte) -44);
		data.put((byte) -2);
		data.put((byte) -1);
		data.put((byte) -1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 31);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 31);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 7);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 112);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -46);
		data.put((byte) -2);
		data.put((byte) -1);
		data.put((byte) -1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 113);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -46);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -16);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -76);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 112);
		data.put((byte) 30);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 113);
		data.put((byte) 30);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 84);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 4);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 88);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -106);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -106);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 20);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 30);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -56);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 100);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 5);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 30);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -12);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 4);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 35);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -56);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -36);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -106);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 35);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 36);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 94);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 13);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -68);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -56);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 100);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 20);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 120);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -56);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 100);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 5);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 30);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -12);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 4);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 35);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -56);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -36);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -106);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 35);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 36);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 94);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 13);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -68);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -56);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 100);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 20);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 120);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -56);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 100);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 5);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -12);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 4);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 35);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 38);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 88);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 100);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 100);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -118);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 10);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 82);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 10);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 5);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -6);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 25);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 3);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 15);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -67);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 27);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 45);
		data.put((byte) 15);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 48);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 110);
		data.put((byte) 9);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 22);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 44);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -118);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 100);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -6);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 24);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -68);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -56);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 100);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 2);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -56);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 30);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 20);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 30);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 30);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 50);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 30);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 8);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) -1);
		data.put((byte) -1);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 7);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 6);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
		data.put((byte) 0);
	}

}
