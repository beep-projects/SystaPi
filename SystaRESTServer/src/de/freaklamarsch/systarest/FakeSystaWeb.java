package de.freaklamarsch.systarest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import de.freaklamarsch.systarest.DataLogger.DataLoggerStatus;
import de.freaklamarsch.systarest.SystaWaterHeaterStatus.tempUnit;

/**
 * @see Runnable implementation to mock the Paradigma SystaWeb service. This class opens a @see DatagramSocket for the communication with a Paradigma SystaComfort II.
 * This class provides access to the received data.
 */
public class FakeSystaWeb implements Runnable {
	
	/**
	 * Inner class for representing the status of this @see FakeSystaWeb 
	 */
	public class FakeSystaWebStatus {
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

		public FakeSystaWebStatus(boolean running, boolean connected, long dataPacketsReceived, String timestamp,
				String localAddress, int localPort, InetAddress remoteAddress, int remotePort, boolean saveLoggedData,
				int capacity, String logFilePrefix, String logEntryDelimiter, String logFileRootPath,
				int writerFileCount, int bufferedEntries) {

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
		}
	}

	private InetAddress remoteAddress;
	private int remotePort;
	private final int PORT = 22460;
	private final int MAX_DATA_LENGTH = 1024;
	private final int MAX_NUMBER_ENTRIES = 250;
	private final int COUNTER_OFFSET = 0x3FBF;
	private final int MAC_OFFSET = 0x8E83;
	private final int REPLY_MSG_LENGTH = 20;

	private final int RING_BUFFER_SIZE = 2;
	private int readIndex = -1;
	private int writeIndex = -1;
	private long dataPacketsReceived = 0;
	private byte[][] mac = new byte[RING_BUFFER_SIZE][6];
	private int[][] rawData = new int[RING_BUFFER_SIZE][MAX_NUMBER_ENTRIES + 1];
	private byte[] reply = new byte[REPLY_MSG_LENGTH];
	private long[] timestamp = new long[RING_BUFFER_SIZE];

	private String inetAddress = "192.186.1.1";
	private DatagramSocket socket = null;
	private boolean running = false;
	private boolean stopRequested = false;
	private byte[] receiveData = new byte[MAX_DATA_LENGTH];
	private DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

	private final String[] WATER_HEATER_OPERATION_MODES = { "off", "normal", "comfort", "locked" };

	private int WRITER_MAX_DATA = 60;
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E-dd.MM.yy-HH:mm:ss");
	private DataLogger log = new DataLogger(WRITER_MAX_DATA);

	// constructor
	public FakeSystaWeb() {
		for (int i = 0; i < timestamp.length; i++) {
			timestamp[i] = -1;
		}
	}

	public FakeSystaWebStatus getStatus() {
		DataLoggerStatus dls = log.getStatus();
		// if we have received data within the last 120 seconds, we are considered being
		// connected
		boolean connected = (readIndex < 0) ? false
				: (timestamp[readIndex] > 0
						&& (LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - timestamp[readIndex] < 120));
		return new FakeSystaWebStatus(this.running, connected, this.dataPacketsReceived, this.getTimestampString(),
				this.inetAddress, this.PORT, this.remoteAddress, this.remotePort, dls.saveLoggedData, dls.capacity,
				dls.logFilePrefix, dls.logEntryDelimiter, dls.logFileRootPath, dls.writerFileCount,
				dls.bufferedEntries);
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
	 * @return the timestamp as a LocalDateTime string, which is a date-time without
	 *         a time-zone in the ISO-8601 calendar system
	 */
	public String getTimestampString() {
		return (readIndex < 0) ? "never"
				: formatter.format(LocalDateTime.ofEpochSecond(timestamp[readIndex], 0, ZoneOffset.UTC));
	}

	/**
	 * @return the rawData of the current measurement, or null if no measurement has
	 *         been done so far
	 */
	public int[] getData() {
		// safe readIndex, so we do not read inconsistent data, if it gets updated
		// between calls
		// System.out.println("[FakeSystaWeb] FSW.getData()");
		int i = readIndex;
		// System.out.println("FSW.getData(): i=="+i);
		if (i >= 0 && timestamp[i] > 0) {
			// System.out.println("[FakeSystaWeb] FSW.getData(): return rawData["+i+"]");
			return rawData[i];
		} else {
			// System.out.println("[FakeSystaWeb] FSW.getData(): return null");
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
		status.currentTemperature = rawData[i][SystaIndex.HOT_WATER_TEMP] / 10.0;
		status.targetTemperature = rawData[i][SystaIndex.HOT_WATER_TEMP_SET] / 10.0;
		status.targetTemperatureHigh = rawData[i][SystaIndex.HOT_WATER_TEMP_MAX] / 10.0;
		status.targetTemperatureLow = Math.max(0.0,
				status.targetTemperature - rawData[i][SystaIndex.HOT_WATER_HYSTERESIS] / 10.0);
		status.temperatureUnit = tempUnit.TEMP_CELSIUS;
		status.currentOperation = WATER_HEATER_OPERATION_MODES[rawData[i][SystaIndex.HOT_WATER_OPERATION_MODE]];
		status.operationList = WATER_HEATER_OPERATION_MODES;
		status.supportedFeatures = new String[] {}; // TODO check what supported features are
		status.is_away_mode_on = false; // TODO match with ferien mode if possible
		status.timestamp = timestamp[i];
		status.timestampString = formatter.format(LocalDateTime.ofEpochSecond(timestamp[i], 0, ZoneOffset.UTC));
		return status;
	}

	public SystaStatus getParadigmaStatus() {
		SystaStatus status = new SystaStatus();
		int i = readIndex; // save readIndex, so we do not read inconsistent data if it gets updated
		if (i < 0 || timestamp[i] <= 0) {
			return null;
		}
		status.outsideTemp = rawData[i][SystaIndex.OUTSIDE_TEMP] / 10.0;
		status.circuit1FlowTemp = rawData[i][SystaIndex.CIRCUIT_1_FLOW_TEMP] / 10.0;
		status.circuit1ReturnTemp = rawData[i][SystaIndex.CIRCUIT_1_RETURN_TEMP] / 10.0;
		status.hotWaterTemp = rawData[i][SystaIndex.HOT_WATER_TEMP] / 10.0;
		status.bufferTempTop = rawData[i][SystaIndex.BUFFER_TEMP_TOP] / 10.0;
		status.bufferTempBottom = rawData[i][SystaIndex.BUFFER_TEMP_BOTTOM] / 10.0;
		status.circulationTemp = rawData[i][SystaIndex.CIRCULATION_TEMP] / 10.0;
		status.circuit2FlowTemp = rawData[i][SystaIndex.CIRCUIT_2_FLOW_TEMP] / 10.0;
		status.circuit2ReturnTemp = rawData[i][SystaIndex.CIRCUIT_2_RETURN_TEMP] / 10.0;
		status.roomTempActual1 = rawData[i][SystaIndex.ROOM_TEMP_ACTUAL_1] / 10.0;
		status.roomTempActual2 = rawData[i][SystaIndex.ROOM_TEMP_ACTUAL_2] / 10.0;
		status.collectorTempActual = rawData[i][SystaIndex.COLLECTOR_TEMP_ACTUAL] / 10.0;
		status.boilerFlowTemp = rawData[i][SystaIndex.BOILER_FLOW_TEMP] / 10.0;
		status.boilerReturnTemp = rawData[i][SystaIndex.BOILER_RETURN_TEMP] / 10.0;
		status.stoveFlowTemp = rawData[i][SystaIndex.STOVE_FLOW_TEMP] / 10.0;
		status.stoveReturnTemp = rawData[i][SystaIndex.STOVE_RETURN_TEMP] / 10.0;
		status.woodBoilerBufferTempTop = rawData[i][SystaIndex.WOOD_BOILER_BUFFER_TEMP_TOP] / 10.0;
		status.swimmingpoolTemp = rawData[i][SystaIndex.SWIMMINGPOOL_TEMP] / 10.0;
		status.swimmingpoolFlowTeamp = rawData[i][SystaIndex.SWIMMINGPOOL_FLOW_TEMP] / 10.0;
		status.swimmingpoolReturnTemp = rawData[i][SystaIndex.SWIMMINGPOOL_RETURN_TEMP] / 10.0;
		status.hotWaterTempSet = rawData[i][SystaIndex.HOT_WATER_TEMP_SET] / 10.0;
		status.roomTempSet1 = rawData[i][SystaIndex.ROOM_TEMP_SET_1] / 10.0;
		status.circuit1FlowTempSet = rawData[i][SystaIndex.CIRCUIT_1_FLOW_TEMP_SET] / 10.0;
		status.circuit2FlowTempSet = rawData[i][SystaIndex.CIRCUIT_2_FLOW_TEMP_SET] / 10.0;
		status.roomTempSet2 = rawData[i][SystaIndex.ROOM_TEMP_SET_2] / 10.0;
		status.bufferTempSet = rawData[i][SystaIndex.BUFFER_TEMP_SET] / 10.0;
		status.boilerTempSet = rawData[i][SystaIndex.BOILER_TEMP_SET] / 10.0;
		status.operationMode = rawData[i][SystaIndex.OPERATION_MODE];
		status.roomTempSetNormal = rawData[i][SystaIndex.ROOM_TEMP_SET_NORMAL] / 10.0;
		status.roomTempSetComfort = rawData[i][SystaIndex.ROOM_TEMP_SET_COMFORT] / 10.0;
		status.roomTempSetLowering = rawData[i][SystaIndex.ROOM_TEMP_SET_LOWERING] / 10.0;
		status.heatingOperationMode = rawData[i][SystaIndex.HEATING_OPERATION_MODE];
		status.controlledBy = rawData[i][SystaIndex.CONTROLLED_BY];
		status.heatingCurveBasePoint = rawData[i][SystaIndex.HEATING_CURVE_BASE_POINT] / 10.0;
		status.heatingCurveGradient = rawData[i][SystaIndex.HEATING_CURVE_GRADIENT] / 10.0;
		status.maxFlowTemp = rawData[i][SystaIndex.MAX_FLOW_TEMP] / 10.0;
		status.heatingLimitTemp = rawData[i][SystaIndex.HEATING_LIMIT_TEMP] / 10.0;
		status.heatingLimitTeampLowering = rawData[i][SystaIndex.HEATING_LIMIT_TEMP_LOWERING] / 10.0;
		status.antiFreezeOutsideTemp = rawData[i][SystaIndex.ANTI_FREEZE_OUTSIDE_TEMP] / 10.0;
		status.heatUpTime = rawData[i][SystaIndex.HEAT_UP_TIME]; // in minutes
		status.roomImpact = rawData[i][SystaIndex.ROOM_IMPACT] / 10.0;
		status.boilerSuperelevation = rawData[i][SystaIndex.BOILER_SUPERELEVATION];
		status.spreadingHeatingCircuit = rawData[i][SystaIndex.SPREADING_HEATING_CIRCUIT] / 10.0;
		status.heatingMinSpeedPump = rawData[i][SystaIndex.HEATING_MIN_SPEED_PUMP]; // in %
		status.mixerRuntime = rawData[i][SystaIndex.MIXER_RUNTIME]; // in minutes
		status.roomTempCorrection = rawData[i][SystaIndex.ROOM_TEMP_CORRECTION] / 10.0;
		status.underfloorHeatingBasePoint = rawData[i][SystaIndex.UNDERFLOOR_HEATING_BASE_POINT] / 10.0;
		status.underfloorHeatingGradient = rawData[i][SystaIndex.UNDERFLOOR_HEATING_GRADIENT] / 10.0;
		status.hotWaterTempNormal = rawData[i][SystaIndex.HOT_WATER_TEMP_NORMAL] / 10.0;
		status.hotWaterTempComfort = rawData[i][SystaIndex.HOT_WATER_TEMP_COMFORT] / 10.0;
		status.hotWaterOperationMode = rawData[i][SystaIndex.HOT_WATER_OPERATION_MODE];
		status.hotWaterHysteresis = rawData[i][SystaIndex.HOT_WATER_HYSTERESIS] / 10.0;
		status.hotWaterTempMax = rawData[i][SystaIndex.HOT_WATER_TEMP_MAX] / 10.0;
		status.pumpOverrun = rawData[i][SystaIndex.PUMP_OVERRUN];
		status.bufferTempMax = rawData[i][SystaIndex.BUFFER_TEMP_MAX] / 10.0;
		status.bufferTempMin = rawData[i][SystaIndex.BUFFER_TEMP_MIN] / 10.0;
		status.boilerHysteresis = rawData[i][SystaIndex.BOILER_HYSTERESIS] / 10.0;
		status.boilerOperationTime = rawData[i][SystaIndex.BOILER_RUNTIME_MIN];
		status.boilerShutdownTemp = rawData[i][SystaIndex.BOILER_SHUTDOWN_TEMP] / 10.0;
		status.boilerMinSpeedPump = rawData[i][SystaIndex.BOILER_MIN_SPEED_PUMP];
		status.circulationPumpOverrun = rawData[i][SystaIndex.CIRCULATION_PUMP_OVERRUN];
		status.circulationHysteresis = rawData[i][SystaIndex.CIRCULATION_HYSTERESIS] / 10.0;
		status.adjustRoomTempBy = rawData[i][SystaIndex.ADJUST_ROOM_TEMP_BY] / 10.0;
		status.boilerOperationTimeHours = rawData[i][SystaIndex.BOILER_OPERATION_TIME_HOURS];
		status.boilerOperationTimeMinutes = rawData[i][SystaIndex.BOILER_OPERATION_TIME_MINUTES];
		status.numberBurnerStarts = rawData[i][SystaIndex.BURNER_NUMBER_STARTS];
		status.solarPowerActual = rawData[i][SystaIndex.SOLAR_POWER_ACTUAL] / 10.0;
		status.solarGainDay = rawData[i][SystaIndex.SOLAR_GAIN_DAY] / 10.0;
		status.solarGainTotal = rawData[i][SystaIndex.SOLAR_GAIN_TOTAL] / 10.0;
		status.countdown = rawData[i][SystaIndex.COUNTDOWN];
		status.relay = rawData[i][SystaIndex.RELAY];
		status.heatingPumpIsOn = (status.relay & SystaStatus.HEATING_PUMP_MASK) != 0;
		status.chargePumpIsOn = (status.relay & SystaStatus.CHARGE_PUMP_MASK) != 0;
		status.circulationPumpIsOn = (status.relay & SystaStatus.CIRCULATION_PUMP_MASK) != 0;
		status.boilerIsOn = (status.relay & SystaStatus.BOILER_MASK) != 0;
		status.burnerIsOn = status.boilerIsOn && ((status.boilerFlowTemp - status.boilerReturnTemp) > 0.2);
		status.unknowRelayState1IsOn = (status.relay & SystaStatus.UNKNOWN_1_MASK) != 0;
		status.unknowRelayState2IsOn = (status.relay & SystaStatus.UNKNOWN_2_MASK) != 0;
		status.unknowRelayState3IsOn = (status.relay & SystaStatus.UNKNOWN_3_MASK) != 0;
		status.unknowRelayState4IsOn = (status.relay & SystaStatus.UNKNOWN_4_MASK) != 0;
		status.unknowRelayState5IsOn = (status.relay & SystaStatus.UNKNOWN_5_MASK) != 0;
		status.error = rawData[i][SystaIndex.ERROR];
		status.operationModeX = rawData[i][SystaIndex.OPERATION_MODE_X];
		status.heatingOperationModeX = rawData[i][SystaIndex.HEATING_OPERATION_MODE_X];
		status.stovePumpSpeedActual = rawData[i][SystaIndex.STOVE_PUMP_SPEED_ACTUAL];
		status.timestamp = timestamp[i];
		status.timestampString = formatter.format(LocalDateTime.ofEpochSecond(timestamp[i], 0, ZoneOffset.UTC));
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
	 * start the communication with a Paradigma SystaComfort II
	 */
	@Override // from the Runnable interface
	public void run() {
		if (running) {
			// we can't be started twice
			return;
		}
		running = true;
		System.out.println("[FakeSystaWeb] trying to open DatagramSocket for UDP communication");
		// try to open the listening socket
		try {
			InetAddress ip = InetAddress.getByName(inetAddress);
			socket = new DatagramSocket(PORT, ip);
		} catch (Exception e) {
			System.out.println("[FakeSystaWeb] exception thrown when trying to open DatagramSocket");
			e.printStackTrace();
			running = false;
			return;
		}
		stopRequested = false;
		dataPacketsReceived = 0;
		System.out.println("[FakeSystaWeb] UDP communication with Paradigma SystaComfort II started");
		while (!stopRequested) {
			try {
				socket.receive(receivePacket);
				dataPacketsReceived++;
			} catch (IOException e) {
				if (stopRequested) {
					// this exception should be thrown if the socket is closed on request
					System.out.println("[FakeSystaWeb] call to receive UDP packets got interrupted");
					break;
				} else {
					e.printStackTrace();
					break;
				}
			}
			// calculate the buffer id for writing
			writeIndex = (readIndex + 1) % RING_BUFFER_SIZE;
			remoteAddress = receivePacket.getAddress();
			remotePort = receivePacket.getPort();
			// get the entire content
			ByteBuffer data = ByteBuffer.wrap(receivePacket.getData()).order(ByteOrder.LITTLE_ENDIAN);
			// parse it
			// 0..5: MAC address of paradigma control board:
			mac[writeIndex][3] = data.get();
			mac[writeIndex][2] = data.get();
			mac[writeIndex][1] = data.get();
			mac[writeIndex][0] = data.get();
			mac[writeIndex][5] = data.get();
			mac[writeIndex][4] = data.get();
			// 6..7: counter, incremented by 1 for each packet
			// 8..15: always "09 09 0C 00 32 DA 00 00"
			// 16: packet type (00 = empty intial packet, 01 = actual data packet, 02 =
			// short final packet)
			data.position(16);
			byte type = data.get();
			if (type == 0x00) {
				// processType0(data, currentWriteBuffer);
			} else if (type == 0x01) {
				processType1(data);
				// writing done, release the buffer for reading
				//readIndex = writeIndex;
			} else if (type == 0x02) {
				// processType2(data, currentWriteBuffer);
			}
			// send the reply for the received data
			sendReply(socket, data, remoteAddress, remotePort);
			if (type == 0x01) {
				log.addData(rawData[readIndex], timestamp[readIndex]);
			}
		}
		System.out.println("[FakeSystaWeb] UDP communication with Paradigma SystaComfort II stopped");
		socket.close();
		stopRequested = false;
		running = false;
	}

	/**
	 * function to reply the messages received from a Paradigma SystaComfort II, for
	 * keeping the communication alive
	 * 
	 * @param socket        local socket used for communication with the Paradigma
	 *                      SystaComfort II
	 * @param data          ByteBuffer holding the message for which the reply is
	 *                      sent
	 * @param remoteAddress IP address of the Paradigma SystaComfort II
	 * @param remotePort    Port of the Paradigma SystaComfort II
	 */
	private void sendReply(DatagramSocket socket, ByteBuffer data, InetAddress remoteAddress, int remotePort) {
		createReply(data);
		// send out the reply
		DatagramPacket replyPacket = new DatagramPacket(reply, reply.length, remoteAddress, remotePort);
		try {
			socket.send(replyPacket);
		} catch (IOException ioe) {
			// do nothing
			System.out.println("[FakeSystaWeb] could not send reply");
		}
	}

	private void createReply(ByteBuffer data) {
		data.position(0);
		// copy the first reply.length bytes from the data buffer into the reply byte
		// array
		data.get(reply, 0, reply.length - 1);
		// keep the first 8 bytes and fill the rest with 0
		for (int i = 8; i < reply.length; i++) {
			reply[i] = 0;
		}
		// Always constant:
		reply[12] = 0x01;
		// Generate reply ID from MAC address:

		int m = (((data.getInt(5) & 0xFF) << 8) + (data.getInt(4) & 0xFF) + MAC_OFFSET) & 0xFFFF;
		reply[16] = (byte) (m & 0xFF);
		reply[17] = (byte) (m >> 8);
		// Generate reply counter with offset:
		int n = (((reply[7] & 0xFF) << 8) + (reply[6] & 0xFF) + COUNTER_OFFSET) & 0xFFFF;
		reply[18] = (byte) (n & 0xFF);
		reply[19] = (byte) (n >> 8);
	}

	/**
	 * process UPD packets from Paradigma SystaComfort II with type field set to
	 * 0x00
	 * 
	 * @param data ByteBuffer that holds the received data
	 */
	private void processType0(ByteBuffer data) {
		while (data.remaining() >= 4) {
			System.out.println("[FakeSystaWeb] Pos: " + data.position() + " Val: " + data.getInt());
		}
	}

	/**
	 * process UPD packets from Paradigma SystaComfort II with type field set to
	 * 0x01
	 * 
	 * @param data ByteBuffer that holds the received data
	 */
	private void processType1(ByteBuffer data) {
		/*data.position(0);
		while (data.remaining() >= 4) {
			System.out.println("[FakeSystaWeb] Pos: " + data.position() + " Val: " + data.get());
		}*/
		if(writeIndex < 0) { //this should usually not happen, except when testing
			// calculate the buffer id for writing
			writeIndex = (readIndex + 1) % RING_BUFFER_SIZE;
		}

		timestamp[writeIndex] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
		data.position(24);
		while (data.remaining() >= 4) {
			rawData[writeIndex][(data.position() - 24) / 4] = data.getInt();
		}
		readIndex = writeIndex;
	}

	/**
	 * process UDP packets from Paradigma SystaComfort II with type field set to
	 * 0x02
	 * 
	 * @param data ByteBuffer that holds the received data
	 */
	private void processType2(ByteBuffer data) {
		while (data.remaining() >= 4) {
			System.out.println("[FakeSystaWeb] Pos: " + data.position() + " Val: " + data.getInt());
		}
	}

	public void logRawData() {
		log.saveLoggedData();
	}

	public void logRawData(int entriesPerFile) {
		log.saveLoggedData(entriesPerFile);
	}

	public void logRawData(String filePrefix) {
		log.saveLoggedData(filePrefix);
	}

	public void logRawData(String filePrefix, String delimiter, int entriesPerFile) {
		log.saveLoggedData(filePrefix, delimiter, entriesPerFile);
	}

	public void stopLoggingRawData() {
		log.stopSavingLoggedData();
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