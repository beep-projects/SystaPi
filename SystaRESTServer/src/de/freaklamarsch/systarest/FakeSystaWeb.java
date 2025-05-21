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
package de.freaklamarsch.systarest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.freaklamarsch.systarest.DataLogger.DataLoggerStatus;
import de.freaklamarsch.systarest.DeviceTouchSearch.DeviceTouchDeviceInfo;
import de.freaklamarsch.systarest.SystaWaterHeaterStatus.tempUnit;

/**
 * @see Runnable implementation to mock the Paradigma SystaWeb service. This
 *      class opens a @see DatagramSocket for the communication with a Paradigma
 *      SystaComfort I or II. This class provides access to the received data.
 */
public class FakeSystaWeb implements Runnable {

	enum MessageType {
		NONE, DATA0, DATA1, DATA2, DATA3, DATA4, OK, ERR
	}

	/**
	 * Inner class for representing the status of this @see FakeSystaWeb
	 */
	public static class FakeSystaWebStatus {
		public final boolean running;
		public final boolean connected;
		public final String lastTimestamp;
		public final long dataPacketsReceived;
		public final String localAddress;
		public final int localPort;
		public final InetAddress remoteAddress;
		public final int remotePort;
		public final boolean logging;
		public final int packetsPerFile;
		public final String loggerFilePrefix;
		public final String loggerEntryDelimiter;
		public final String loggerFileRootPath;
		public final int loggerFileCount;
		public final int loggerBufferedEntries;
		public final String commitDate;

		public FakeSystaWebStatus(boolean running, boolean connected, long dataPacketsReceived, String timestamp,
				String localAddress, int localPort, InetAddress remoteAddress, int remotePort, boolean saveLoggedData,
				int capacity, String logFilePrefix, String logEntryDelimiter, String logFileRootPath,
				int writerFileCount, int bufferedEntries, String commitDate) {

			this.running = running;
			this.connected = connected;
			this.lastTimestamp = timestamp;
			this.dataPacketsReceived = dataPacketsReceived;
			this.localAddress = localAddress;
			this.localPort = localPort;
			this.remoteAddress = remoteAddress;
			this.remotePort = remotePort;
			this.logging = saveLoggedData;
			this.packetsPerFile = capacity;
			this.loggerFilePrefix = logFilePrefix;
			this.loggerEntryDelimiter = logEntryDelimiter;
			this.loggerFileRootPath = logFileRootPath;
			this.loggerFileCount = writerFileCount;
			this.loggerBufferedEntries = bufferedEntries;
			this.commitDate = "2025-05-21T20:46:45+00:00";
		}
	}

	/**
	 * Inner class for representing the info about a SystaComfort unit
	 */
	public class SystaComfortInfo {
		public final String systawebIp;
		public final int systawebPort;
		public final String systaBcastIp;
		public final int systaBcastPort;
		public final String scInfoString;
		public final String unitIp;
		public final int unitStouchPort;
		public final String unitName;
		public final String unitId;
		public final int unitApp;
		public final int unitPlatform;
		public final String unitScFullVersion;
		public final int unitScVersion;
		public final int unitScMinor;
		public final String unitPassword;
		public final String unitBaseVersion;
		public final String unitMac;

		public SystaComfortInfo(String systawebIp, int systawebPort, String systaBcastIp, int systaBcastPort,
				String scInfoString, String unitIp, int unitStouchPort, String unitName, String unitId, int unitApp,
				int unitPlatform, String unitScFullVersion, int unitScVersion, int unitScMinor, String unitPassword,
				String unitBaseVersion, String unitMac) {
			this.systawebIp = systawebIp;
			this.systawebPort = systawebPort;
			this.systaBcastIp = systaBcastIp;
			this.systaBcastPort = systaBcastPort;
			this.scInfoString = scInfoString;
			this.unitIp = unitIp;
			this.unitStouchPort = unitStouchPort;
			this.unitName = unitName;
			this.unitId = unitId;
			this.unitApp = unitApp;
			this.unitPlatform = unitPlatform;
			this.unitScFullVersion = unitScFullVersion;
			this.unitScVersion = unitScVersion;
			this.unitScMinor = unitScMinor;
			this.unitPassword = unitPassword;
			this.unitBaseVersion = unitBaseVersion;
			this.unitMac = unitMac;

		}
	}

    // Constants 
	private static final String commitDate = "2025-05-21T20:46:45+00:00";
    private static final int PORT = 22460;
    private static final int MAX_DATA_LENGTH = 1048;
    private static final int MAX_NUMBER_ENTRIES = 256;
    private static final int MAX_NUMBER_DATA_PACKETS = 4;
    private static final int COUNTER_OFFSET_REPLY = 0x3FBF;
    private static final int COUNTER_OFFSET_REPLY_2 = 0x3FC0;
    private static final int MAC_OFFSET_REPLY = 0x8E82;
	// the SystaComfort sends burst of 3 to 4 messages every minute
	// at the moment packets of type 0x01, 0x02, 0x03, 0x04 are processed, so 4 buffers should be
	// enough
	// TODO adjust this value if more packet types are processed
	private final int RING_BUFFER_SIZE = 6; //make it 6, just to have some additional space
	private static final String[] WATER_HEATER_OPERATION_MODES = { "off", "normal", "comfort", "locked" };
	private static final int WRITER_MAX_DATA = 60;
	private static final String DELIMITER = ";";
	private static final String PREFIX = "SystaREST";
	private static final String LOG_PATH = System.getProperty("user.home") + File.separator + "logs";
	private static final String logFileFilterString = ".*-(raw|data)-[0-9]+\\.txt";
	private static final FilenameFilter logFileFilter = (dir, name) -> name.matches(logFileFilterString);
	/*private static final FilenameFilter logFileFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.matches(logFileFilterString);
		}
	};*/
	
	private MessageType typeOfLastReceivedMessage = MessageType.NONE;
	private InetAddress remoteAddress;
	private int remotePort;

	private int readIndex = -1;
	private int writeIndex = -1;
	private long dataPacketsReceived = 0;
	private byte[][] replyHeader = new byte[RING_BUFFER_SIZE][8];
	private Integer[][] intData = new Integer[RING_BUFFER_SIZE][MAX_NUMBER_ENTRIES*MAX_NUMBER_DATA_PACKETS];
	private long[] timestamp = new long[RING_BUFFER_SIZE];

	private String inetAddress = "not configured";
	private DatagramSocket socket = null;
	private boolean running = false;
	private boolean stopRequested = false;
	private byte[] receiveData = new byte[MAX_DATA_LENGTH];
	private DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

	private DateTimeFormatter timestampFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
			.withZone(ZoneId.systemDefault());
	private DataLogger<Integer> logInt = new DataLogger<>(PREFIX, "data", DELIMITER, WRITER_MAX_DATA, LOG_PATH, timestampFormatter);
	private DataLogger<Byte> logRaw = new DataLogger<>(PREFIX, "raw", DELIMITER, WRITER_MAX_DATA, LOG_PATH, timestampFormatter);

	// constructor
	public FakeSystaWeb() {
		/*for (Integer[] data : intData) {
            Arrays.fill(data, 0);
		}*/
        Arrays.stream(intData).forEach(data -> Arrays.fill(data, 0));
		Arrays.fill(timestamp, -1);
	}

    /**
     * Simulates retrieving the status of the SystaComfort unit.
     *
     * @return a {@link SystaStatus} object containing the current status
     */
	public FakeSystaWebStatus getStatus() {
		DataLoggerStatus dls = logRaw.getStatus();
		// if we have received data within the last 120 seconds, we are considered being
		// connected
		boolean connected = (readIndex < 0) ? false
				: (timestamp[readIndex] > 0 && (Instant.now().toEpochMilli() - timestamp[readIndex] < 120));
		return new FakeSystaWebStatus(this.running, connected, this.dataPacketsReceived, this.getTimestampString(),
				this.inetAddress, FakeSystaWeb.PORT, this.remoteAddress, this.remotePort, dls.saveLoggedData, dls.capacity,
				dls.logFilePrefix, dls.logEntryDelimiter, dls.logFileRootPath, dls.writerFileCount, dls.bufferedEntries,
				FakeSystaWeb.commitDate);
	}

	public DeviceTouchDeviceInfo findSystaComfort() {
		return DeviceTouchSearch.search();
	}

	/**
	 * @return the inetAddress
	 */
	public String getInetAddress() {
		return inetAddress;
	}

	/**
	 * @param inetAddress the inetAddress to set
	 */
	public void setInetAddress(String inetAddress) {
		this.inetAddress = inetAddress;
	}

	/**
	 * get the timestamp for the current measurement
	 *
	 * @return the timestamp in seconds from the epoch of 1970-01-01T00:00:00Z.
	 *         Which is UTC.
	 */
	public long getTimestamp() {
		return (readIndex < 0) ? -1 : timestamp[readIndex];
	}

	/**
	 * get the timestamp for the current measurement as human readable string
	 *
	 * @return the timestamp as a LocalDateTime string, which is a date-time with a
	 *         time-zone offset in the ISO-8601 calendar system e.g.
	 *         2021-12-24T14:49:27.123+01:00 or "never"
	 */
	public String getTimestampString() {
		return (readIndex < 0) ? "never" : getFormattedTimeString(timestamp[readIndex]);
	}

	/**
	 * get the timestamp (epoch seconds) as formatted string for the local timezone
	 *
	 * @return the timestamp as a LocalDateTime string, which is a date-time with a
	 *         time-zone offset in the ISO-8601 calendar system e.g.
	 *         2021-12-24T14:49:27.123+01:00
	 */
	public String getFormattedTimeString(long timestamp) {
		return timestampFormatter.format(Instant.ofEpochMilli(timestamp));
	}

	/**
	 * @return the intData of the current measurement, or null if no measurement has
	 *         been done so far
	 */
	public Integer[] getData() {
		// safe readIndex at the beginning, so we do not read inconsistent data, if it gets updated
		// between calls
		int i = readIndex;
		if (i >= 0 && timestamp[i] > 0) {
			return intData[i];
		} else {
			return null;
		}
	}

	public SystaWaterHeaterStatus getWaterHeaterStatus() {
		SystaWaterHeaterStatus status = new SystaWaterHeaterStatus();
		int i = readIndex; // save readIndex, so we do not read inconsistent data if it gets updated
		if (i < 0 || timestamp[i] <= 0) {
			return null;
		}
		status.minTemp = 40.0; // TODO check this value
		status.maxTemp = 65.0; // TODO check this value
		status.currentTemperature = intData[i][SystaIndex.HOT_WATER_TEMP] / 10.0;
		status.targetTemperature = intData[i][SystaIndex.HOT_WATER_TEMP_SET] / 10.0;
		status.targetTemperatureHigh = intData[i][SystaIndex.HOT_WATER_TEMP_MAX] / 10.0;
		status.targetTemperatureLow = Math.max(0.0,
				status.targetTemperature - intData[i][SystaIndex.HOT_WATER_HYSTERESIS] / 10.0);
		status.temperatureUnit = tempUnit.TEMP_CELSIUS;
		status.currentOperation = WATER_HEATER_OPERATION_MODES[intData[i][SystaIndex.HOT_WATER_OPERATION_MODE]];
		status.operationList = WATER_HEATER_OPERATION_MODES;
		status.supportedFeatures = new String[] {}; // TODO check what supported features are
		status.is_away_mode_on = false; // TODO match with ferien mode if possible
		status.timestamp = timestamp[i];
		status.timestampString = getFormattedTimeString(timestamp[readIndex]);
		return status;
	}

	public SystaStatus getParadigmaStatus() {
		SystaStatus status = new SystaStatus();
		int i = readIndex; // save readIndex, so we do not read inconsistent data if it gets updated
		if (i < 0 || timestamp[i] <= 0) {
			return null;
		}
		status.outsideTemp = intData[i][SystaIndex.OUTSIDE_TEMP] / 10.0;
		status.circuit1FlowTemp = intData[i][SystaIndex.CIRCUIT_1_FLOW_TEMP] / 10.0;
		status.circuit1ReturnTemp = intData[i][SystaIndex.CIRCUIT_1_RETURN_TEMP] / 10.0;
		status.circuit1OperationMode = intData[i][SystaIndex.CIRCUIT_1_OPERATION_MODE];
		status.hotWaterTemp = intData[i][SystaIndex.HOT_WATER_TEMP] / 10.0;
		status.bufferTempTop = intData[i][SystaIndex.BUFFER_TEMP_TOP] / 10.0;
		status.bufferTempBottom = intData[i][SystaIndex.BUFFER_TEMP_BOTTOM] / 10.0;
		status.circulationTemp = intData[i][SystaIndex.CIRCULATION_TEMP] / 10.0;
		status.circuit2FlowTemp = intData[i][SystaIndex.CIRCUIT_2_FLOW_TEMP] / 10.0;
		status.circuit2ReturnTemp = intData[i][SystaIndex.CIRCUIT_2_RETURN_TEMP] / 10.0;
		status.roomTempActual1 = intData[i][SystaIndex.ROOM_TEMP_ACTUAL_1] / 10.0;
		status.roomTempActual2 = intData[i][SystaIndex.ROOM_TEMP_ACTUAL_2] / 10.0;
		status.collectorTempActual = intData[i][SystaIndex.COLLECTOR_TEMP_ACTUAL] / 10.0;
		status.boilerFlowTemp = intData[i][SystaIndex.BOILER_FLOW_TEMP] / 10.0;
		status.boilerReturnTemp = intData[i][SystaIndex.BOILER_RETURN_TEMP] / 10.0;
		status.logBoilerFlowTemp = intData[i][SystaIndex.LOG_BOILER_FLOW_TEMP] / 10.0;
		status.logBoilerReturnTemp = intData[i][SystaIndex.LOG_BOILER_RETURN_TEMP] / 10.0;
		status.logBoilerBufferTempTop = intData[i][SystaIndex.LOG_BOILER_BUFFER_TEMP_TOP] / 10.0;
		status.swimmingpoolTemp = intData[i][SystaIndex.SWIMMINGPOOL_TEMP] / 10.0;
		status.swimmingpoolFlowTemp = intData[i][SystaIndex.SWIMMINGPOOL_FLOW_TEMP] / 10.0;
		status.swimmingpoolReturnTemp = intData[i][SystaIndex.SWIMMINGPOOL_RETURN_TEMP] / 10.0;
		status.hotWaterTempSet = intData[i][SystaIndex.HOT_WATER_TEMP_SET] / 10.0;
		status.roomTempSet1 = intData[i][SystaIndex.ROOM_TEMP_SET_1] / 10.0;
		status.circuit1FlowTempSet = intData[i][SystaIndex.CIRCUIT_1_FLOW_TEMP_SET] / 10.0;
		status.circuit2FlowTempSet = intData[i][SystaIndex.CIRCUIT_2_FLOW_TEMP_SET] / 10.0;
		status.roomTempSet2 = intData[i][SystaIndex.ROOM_TEMP_SET_2] / 10.0;
		status.bufferTempSet = intData[i][SystaIndex.BUFFER_TEMP_SET] / 10.0;
		status.boilerTempSet = intData[i][SystaIndex.BOILER_TEMP_SET] / 10.0;
		status.operationMode = intData[i][SystaIndex.OPERATION_MODE];
		status.roomTempSetNormal = intData[i][SystaIndex.ROOM_TEMP_SET_NORMAL] / 10.0;
		status.roomTempSetComfort = intData[i][SystaIndex.ROOM_TEMP_SET_COMFORT] / 10.0;
		status.roomTempSetLowering = intData[i][SystaIndex.ROOM_TEMP_SET_LOWERING] / 10.0;
		status.heatingOperationMode = intData[i][SystaIndex.HEATING_OPERATION_MODE];
		status.controlledBy = intData[i][SystaIndex.CONTROLLED_BY];
		status.heatingCurveBasePoint = intData[i][SystaIndex.HEATING_CURVE_BASE_POINT] / 10.0;
		status.heatingCurveGradient = intData[i][SystaIndex.HEATING_CURVE_GRADIENT] / 10.0;
		status.maxFlowTemp = intData[i][SystaIndex.MAX_FLOW_TEMP] / 10.0;
		status.heatingLimitTemp = intData[i][SystaIndex.HEATING_LIMIT_TEMP] / 10.0;
		status.heatingLimitTeampLowering = intData[i][SystaIndex.HEATING_LIMIT_TEMP_LOWERING] / 10.0;
		status.antiFreezeOutsideTemp = intData[i][SystaIndex.ANTI_FREEZE_OUTSIDE_TEMP] / 10.0;
		status.heatUpTime = intData[i][SystaIndex.HEAT_UP_TIME]; // in minutes
		status.roomImpact = intData[i][SystaIndex.ROOM_IMPACT] / 10.0;
		status.boilerSuperelevation = intData[i][SystaIndex.BOILER_SUPERELEVATION];
		status.heatingCircuitSpreading = intData[i][SystaIndex.HEATING_CIRCUIT_SPREADING] / 10.0;
		status.heatingPumpSpeedMin = intData[i][SystaIndex.HEATING_PUMP_SPEED_MIN]; // in %
		status.mixerRuntime = intData[i][SystaIndex.MIXER_RUNTIME]; // in minutes
		status.roomTempCorrection = intData[i][SystaIndex.ROOM_TEMP_CORRECTION] / 10.0;
		status.underfloorHeatingBasePoint = intData[i][SystaIndex.UNDERFLOOR_HEATING_BASE_POINT] / 10.0;
		status.underfloorHeatingGradient = intData[i][SystaIndex.UNDERFLOOR_HEATING_GRADIENT] / 10.0;
		status.hotWaterTempNormal = intData[i][SystaIndex.HOT_WATER_TEMP_NORMAL] / 10.0;
		status.hotWaterTempComfort = intData[i][SystaIndex.HOT_WATER_TEMP_COMFORT] / 10.0;
		status.hotWaterOperationMode = intData[i][SystaIndex.HOT_WATER_OPERATION_MODE];
		status.hotWaterHysteresis = intData[i][SystaIndex.HOT_WATER_HYSTERESIS] / 10.0;
		status.hotWaterTempMax = intData[i][SystaIndex.HOT_WATER_TEMP_MAX] / 10.0;
		status.heatingPumpOverrun = intData[i][SystaIndex.PUMP_OVERRUN];
		status.heatingPumpSpeedActual = intData[i][SystaIndex.HEATING_PUMP_SPEED_ACTUAL] * 5; // in %
		status.bufferTempMax = intData[i][SystaIndex.BUFFER_TEMP_MAX] / 10.0;
		status.bufferTempMin = intData[i][SystaIndex.BUFFER_TEMP_MIN] / 10.0;
		status.boilerHysteresis = intData[i][SystaIndex.BOILER_HYSTERESIS] / 10.0;
		status.boilerOperationTime = intData[i][SystaIndex.BOILER_RUNTIME_MIN]; // in min
		status.boilerShutdownTemp = intData[i][SystaIndex.BOILER_SHUTDOWN_TEMP] / 10.0;
		status.boilerPumpSpeedMin = intData[i][SystaIndex.BOILER_PUMP_SPEED_MIN]; // in %
		status.boilerPumpSpeedActual = intData[i][SystaIndex.BOILER_PUMP_SPEED_ACTUAL] * 5;// 0=0%, 20=100%
		status.boilerOperationMode = intData[i][SystaIndex.BOILER_OPERATION_MODE];
		status.circulationOperationMode = intData[i][SystaIndex.CIRCULATION_OPERATION_MODE];
		status.circulationPumpOverrun = intData[i][SystaIndex.CIRCULATION_PUMP_OVERRUN]; // in min
		status.circulationLockoutTimePushButton = intData[i][SystaIndex.CIRCULATION_LOCKOUT_TIME_PUSH_BUTTON]; // in min
		status.circulationHysteresis = intData[i][SystaIndex.CIRCULATION_HYSTERESIS] / 10.0;
		status.adjustRoomTempBy = intData[i][SystaIndex.ADJUST_ROOM_TEMP_BY] / 10.0;
		status.boilerOperationTimeHours = intData[i][SystaIndex.BOILER_OPERATION_TIME_HOURS];
		status.boilerOperationTimeMinutes = intData[i][SystaIndex.BOILER_OPERATION_TIME_MINUTES];
		status.burnerNumberOfStarts = intData[i][SystaIndex.BURNER_NUMBER_OF_STARTS];
		status.solarPowerActual = intData[i][SystaIndex.SOLAR_POWER_ACTUAL] / 10.0;
		status.solarGainDay = intData[i][SystaIndex.SOLAR_GAIN_DAY]; // in kWh
		status.solarGainTotal = intData[i][SystaIndex.SOLAR_GAIN_TOTAL]; // in kWh
		status.systemNumberOfStarts = intData[i][SystaIndex.SYSTEM_NUMBER_OF_STARTS];
		status.circuit1LeadTime = intData[i][SystaIndex.CIRCUIT_1_LEAD_TIME]; // in min
		status.circuit2LeadTime = intData[i][SystaIndex.CIRCUIT_2_LEAD_TIME]; // in min
		status.circuit3LeadTime = intData[i][SystaIndex.CIRCUIT_3_LEAD_TIME]; // in min
		status.relay = intData[i][SystaIndex.RELAY];
		status.heatingPumpIsOn = (status.relay & SystaStatus.HEATING_PUMP_MASK) != 0;
		status.chargePumpIsOn = (status.relay & SystaStatus.CHARGE_PUMP_MASK) != 0;
		status.logBoilderChargePumpIsOn = (status.relay & SystaStatus.CHARGE_PUMP_LOG_BOILER_MASK) != 0;
		status.circulationPumpIsOn = (status.relay & SystaStatus.CIRCULATION_PUMP_MASK) != 0;
		status.boilerIsOn = ((status.relay & SystaStatus.BOILER_MASK) != 0)
				|| (List.of(1, 2, 3, 8, 9, 11, 12).contains(status.boilerOperationMode));
		status.burnerIsOn = ((status.relay & SystaStatus.BURNER_MASK) != 0);
		status.boilerLedIsOn = (status.relay & SystaStatus.LED_BOILER_MASK) != 0;
		status.unknowRelayState1IsOn = (status.relay & SystaStatus.UNKNOWN_1_MASK) != 0;
		status.unknowRelayState2IsOn = (status.relay & SystaStatus.UNKNOWN_2_MASK) != 0;
		status.mixer1IsOnWarm = (status.relay & SystaStatus.MIXER_WARM_MASK) != 0;
		status.mixer1IsOnCool = (status.relay & SystaStatus.MIXER_COLD_MASK) != 0;
		status.mixer1State = (!status.mixer1IsOnWarm && !status.mixer1IsOnCool) ? 0
				: ((status.mixer1IsOnWarm && !status.mixer1IsOnCool) ? 1
						: ((!status.mixer1IsOnWarm && status.mixer1IsOnCool) ? 2 : 3));
		status.unknowRelayState5IsOn = (status.relay & SystaStatus.UNKNOWN_5_MASK) != 0;
		status.error = intData[i][SystaIndex.ERROR];
		status.operationModeX = intData[i][SystaIndex.OPERATION_MODE_X];
		status.heatingOperationModeX = intData[i][SystaIndex.HEATING_OPERATION_MODE_X];
		status.logBoilerBufferTempMin = intData[i][SystaIndex.LOG_BOILER_BUFFER_TEMP_MIN] / 10.0;
		status.logBoilerTempMin = intData[i][SystaIndex.LOG_BOILER_TEMP_MIN] / 10.0;
		status.logBoilerSpreadingMin = intData[i][SystaIndex.LOG_BOILER_SPREADING_MIN] / 10.0;
		status.logBoilerPumpSpeedMin = intData[i][SystaIndex.LOG_BOILER_PUMP_SPEED_MIN];// it is already in %
		status.logBoilerPumpSpeedActual = intData[i][SystaIndex.LOG_BOILER_PUMP_SPEED_ACTUAL] * 5;// 0=0%, 20=100%
		status.logBoilerSettings = intData[i][SystaIndex.LOG_BOILER_SETTINGS];
		status.logBoilerParallelOperation = (status.logBoilerSettings
				& SystaStatus.LOG_BOILER_PARALLEL_OPERATION_MASK) != 0;
		status.logBoilerOperationMode = intData[i][SystaIndex.LOG_BOILER_OPERATION_MODE];
		status.boilerHeatsBuffer = (status.logBoilerSettings & SystaStatus.BOILER_HEATS_BUFFER_MASK) != 0;
		status.bufferType = intData[i][SystaIndex.BUFFER_TYPE];
		status.timestamp = timestamp[i];
		status.timestampString = getFormattedTimeString(timestamp[readIndex]);
		return status;
	}

	/**
	 * stop the communication with a Paradigma SystaComfort II if running
	 */
	public void stop() {
		stopRequested = true;
		if (socket != null && !socket.isClosed()) {
			socket.close();
		}
	}

	/**
	 * start the communication with a Paradigma SystaComfort II requires the globals
	 * inetAddress and PORT to be properly configured
	 */
	@Override // from the Runnable interface
	public void run() {
		if (running) {
			// FakeSystaWeb can't be started twice
			return;
		}
		running = true;
		System.out.println("[FakeSystaWeb] trying to open DatagramSocket for UDP communication on "+inetAddress+":"+PORT);
		// try to open the listening socket
		try {
			InetAddress ip = InetAddress.getByName(inetAddress);
			socket = new DatagramSocket(PORT, ip);
		} catch (Exception e) {
			System.out.println("[FakeSystaWeb] exception thrown when trying to open DatagramSocket");
			e.printStackTrace();
			running = false;
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
			return;
		}
		stopRequested = false;
		dataPacketsReceived = 0;
		System.out.println("[FakeSystaWeb] UDP communication with Paradigma SystaComfort II started");
		while (!stopRequested) {
			receiveNextDatagram();
			if(receivePacket.getLength() == 0) {
				// the receive call was interrupted, by a close request to the interface
				// this indicates a stopRequested == true
				break;
			}
			processDatagram(ByteBuffer.wrap(receivePacket.getData()).order(ByteOrder.LITTLE_ENDIAN));
			synchronized (typeOfLastReceivedMessage) {
				typeOfLastReceivedMessage.notifyAll();
			}
		}
		System.out.println("[FakeSystaWeb] UDP communication with Paradigma SystaComfort II stopped");
		socket.close();
		stopRequested = false;
		running = false;
	}

	/**
	 * @param data ByteBuffer that holds the raw data of the Datagram
	 */
	private void processDatagram(ByteBuffer data) {
		writeIndex = (readIndex + 1) % RING_BUFFER_SIZE;
		timestamp[writeIndex] = Instant.now().toEpochMilli();
		remoteAddress = receivePacket.getAddress();
		remotePort = receivePacket.getPort();
		logRaw.addData(toByteArray(receivePacket.getData()), timestamp[writeIndex]);
		for(int i=0;i<8;i++) {
		  // 0..5: MAC address of SystaComfort Ethernet port:
		  // 6..7: counter, incremented by 1 for each packet
		  replyHeader[writeIndex][i] = data.get();
		}
		// 8..15: always "09 09 0C 00 32 DA 00 00"
		// byte 12, 13 seem to be the protocol version 32 DA, or 32 DC or 33 DF
		// 16: packet type (00 = empty intial packet, 01 = actual data packet, 02 =
		// short final packet, FF = parameter change ok)
		data.position(16);
		byte type = data.get();
		switch(type) {
		  case 0x00:
			typeOfLastReceivedMessage = MessageType.DATA0;
			sendDataReply(writeIndex);
			break;
		  case 0x01:
			processDataType1(data);
			typeOfLastReceivedMessage = MessageType.DATA1;
			sendDataReply(writeIndex);
			logInt.addData(intData[readIndex], timestamp[readIndex]);
			break;
		  case 0x02:
			processDataType2(data);
			typeOfLastReceivedMessage = MessageType.DATA2;
			sendDataReply(writeIndex);
			logInt.addData(intData[readIndex], timestamp[readIndex]);
			break;
		  case 0x03:
			processDataType3(data);
			typeOfLastReceivedMessage = MessageType.DATA3;
			sendDataReply(writeIndex);
			logInt.addData(intData[readIndex], timestamp[readIndex]);
			break;
		  case 0x04:
			processDataType4(data);
			typeOfLastReceivedMessage = MessageType.DATA4;
			sendDataReply(writeIndex);
			logInt.addData(intData[readIndex], timestamp[readIndex]);
			break;
		  case (byte)0xFF:
			typeOfLastReceivedMessage = MessageType.OK;
			break;
		  default:
			System.out.println("[FakeSystaWeb] unknown message type received " + String.format("0x%02X", type));
			typeOfLastReceivedMessage = MessageType.ERR;
		}
	}

	/**
	 * receive the next UDP {@link java.net.DatagramPacket} from the configured {@link socket} into {@link receivePacket}
	 */
	private void receiveNextDatagram() {
		try {
			socket.receive(receivePacket);
			dataPacketsReceived++;
		} catch (IOException e) {
            // make sure this receivePacket is not used by anyone else
			receivePacket.setLength(0);
			if (stopRequested) {
				// this exception should be thrown if the socket is closed on request
				System.out.println("[FakeSystaWeb] call to receive UDP packets got interrupted");
			} else {
				System.out.println("[FakeSystaWeb] IOException thrown when waiting for data on "+inetAddress+":"+PORT);
				e.printStackTrace();
			}
		}
	}

	/**
	 * private helper function to convert from {@link byte[]} to {@link Byte[]}
	 * @param bytes
	 * @return
	 */
	private Byte[] toByteArray(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		Byte[] b = new Byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			b[i] = Byte.valueOf(bytes[i]);
		}
		return b;
	}

	/**
	 * function to reply the messages received from a Paradigma SystaComfort II, for
	 * keeping the communication alive
	 *
	 * @param index index in replyHeader where the current message is stored
	 */
	private void sendDataReply(int index) {
		byte[] reply = Arrays.copyOf(replyHeader[index], 16);
		// Generate reply ID from MAC address:
		int m = (((reply[5] & 0xFF) << 8) + (reply[4] & 0xFF) + MAC_OFFSET_REPLY) & 0xFFFF;
		reply[12] = (byte) (m & 0xFF);
		reply[13] = (byte) (m >> 8);
		// Generate reply counter with offset:
		int n = (((reply[7] & 0xFF) << 8) + (reply[6] & 0xFF) + COUNTER_OFFSET_REPLY) & 0xFFFF;
		if ((reply[5] + reply[4]) == 57 || (reply[5] + reply[4]) == 313) {
			// TODO this is just a hack to support a specific unit.
			// Find out why this is needed and make it generic
			n = (((reply[7] & 0xFF) << 8) + (reply[6] & 0xFF) + COUNTER_OFFSET_REPLY_2) & 0xFFFF;
		}
		reply[14] = (byte) (n & 0xFF);
		reply[15] = (byte) (n >> 8);
		send(reply);
	}

	/**
	 * send a message to the SystaComfort II unit
	 *
	 * @param reply byte[] holding the message to be sent
	 */
	private void send(byte[] reply) {
		// send out the reply
		try {
			DatagramPacket replyPacket = new DatagramPacket(reply, reply.length, remoteAddress, remotePort);
			socket.send(replyPacket);
		} catch (IOException ioe) {
			// do nothing
			System.out.println("[FakeSystaWeb] could not send reply: IOException, " + ioe.getMessage());
		}
		 catch (IllegalArgumentException iae) {
				// do nothing
				System.out.println("[FakeSystaWeb] could not send reply: IllegalArgumentException, " + iae.getMessage());
		}
	}

	private void processDataPacket(ByteBuffer data, int offset) {
		data.position(24);
		if(readIndex >= 0) {
			// data packets are only updates for a part of the data set.
			// Copy the current data set and update the part that was received in the new packet
			intData[writeIndex] = Arrays.copyOf(intData[readIndex],intData[readIndex].length);
		}
		data.position(24);
		while (data.remaining() >= 4) {
			intData[writeIndex][offset + (data.position() - 24) / 4] = data.getInt();
		}
		readIndex = writeIndex;
	}
	
	/**
	 * process UPD packets from Paradigma SystaComfort II with type field set to
	 * 0x01
	 *
	 * @param data ByteBuffer that holds the received data
	 */
	private void processDataType1(ByteBuffer data) {
		processDataPacket(data, 0);
	}

	/**
	 * process UDP packets from Paradigma SystaComfort II with type field set to
	 * 0x02
	 *
	 * @param data ByteBuffer that holds the received data
	 */
	private void processDataType2(ByteBuffer data) {
		processDataPacket(data, MAX_NUMBER_ENTRIES);
	}

	/**
	 * process UDP packets from Paradigma SystaComfort II with type field set to
	 * 0x03
	 *
	 * @param data ByteBuffer that holds the received data
	 */
	private void processDataType3(ByteBuffer data) {
		processDataPacket(data, 2*MAX_NUMBER_ENTRIES);
	}

	/**
	 * process UDP packets from Paradigma SystaComfort II with type field set to
	 * 0x04
	 *
	 * @param data ByteBuffer that holds the received data
	 */
	private void processDataType4(ByteBuffer data) {
		processDataPacket(data, 3*MAX_NUMBER_ENTRIES);
	}

	public void logRawData() {
		logRaw.saveLoggedData();
		logInt.saveLoggedData();
	}

	public void logRawData(int entriesPerFile) {
		logRaw.saveLoggedData(entriesPerFile);
		logInt.saveLoggedData(entriesPerFile);
	}

	public void logRawData(String filePrefix) {
		logRaw.saveLoggedData(filePrefix);
		logInt.saveLoggedData(filePrefix);
	}

	public void logRawData(String filePrefix, String delimiter, int entriesPerFile) {
		logRaw.saveLoggedData(filePrefix, delimiter, entriesPerFile);
		logInt.saveLoggedData(filePrefix, delimiter, entriesPerFile);
	}

	public void stopLoggingRawData() {
		logRaw.stopSavingLoggedData();
		logInt.stopSavingLoggedData();
	}

	public File getAllLogs() {
		String zipFileName = LOG_PATH + File.separator + "SystaPiLogs_"
				+ new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".zip";
		File zippedLogs = new File(zipFileName);
		if (zippedLogs.exists()) {
			zippedLogs.delete();
		}
		try {
			FileOutputStream fos = new FileOutputStream(zippedLogs);
			ZipOutputStream zos = new ZipOutputStream(fos);
			// no checked needed, if the folder does not exist, it is empty
			File folderToBeZipped = new File(LOG_PATH);

			File[] files = folderToBeZipped.listFiles(logFileFilter);
			System.out.println("[FakeSystaWeb] found " + files.length + " files to be zipped");
			for (File file : files) {
				if (file.isDirectory()) {
					// ignore directories
					continue;
				}
				FileInputStream fis = new FileInputStream(file);
				ZipEntry zipEntry = new ZipEntry(file.getName());
				zos.putNextEntry(zipEntry);
				byte[] bytes = new byte[1024];
				while (fis.read(bytes) >= 0) {
					zos.write(bytes, 0, bytes.length);
				}
				zos.closeEntry();
				fis.close();
			}
			zos.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		// zippedLogs.delete();
		return zippedLogs;
	}

	public int deleteAllLogs() {
		AtomicInteger i = new AtomicInteger(0);
		Arrays.stream(new File(LOG_PATH).listFiles(logFileFilter)).forEach(file -> {
			if (file.delete()) {
				System.out.println("[FakeSystaWeb] deleted "+file.getName());
				i.incrementAndGet();
			}
		});
		if(i.get() != (logRaw.getWriterFileCount()+logInt.getWriterFileCount())) {
			System.out.println("[FakeSystaWeb] missmatch in numbers. Deleted "+i.get()+" files, but loggers had counted "+(logRaw.getWriterFileCount()+logInt.getWriterFileCount())+" files.");
		}
		logRaw.setWriterFileCount(0);
		logInt.setWriterFileCount(0);
		return i.get();
	}

	/*
	 * private String byteToHex(byte num) { char[] hexDigits = new char[2];
	 * hexDigits[0] = Character.forDigit((num >>> 4) & 0xF, 16); hexDigits[1] =
	 * Character.forDigit((num & 0xF), 16); return new String(hexDigits); }
	 */

	/*
	 * private void printBytesAsHex(byte[] bytes) { for (int j = 0; j <
	 * bytes.length; j++) { System.out.print(byteToHex(bytes[j])); }
	 * System.out.println(); }
	 */

	/*
	 * private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	 * public void printBytesAsHex(byte[] bytes) { char[] hexChars = new
	 * char[bytes.length * 2]; for (int j = 0; j < bytes.length; j++) { int v =
	 * bytes[j] & 0xFF; hexChars[j * 2] = HEX_ARRAY[v >>> 4]; hexChars[j * 2 + 1] =
	 * HEX_ARRAY[v & 0x0F]; } System.out.println(hexChars); //return new
	 * String(hexChars); }
	 */

}
