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
import java.util.Objects;
import java.util.zip.ZipOutputStream;

import de.freaklamarsch.systarest.DataLogger.DataLoggerStatus;
import de.freaklamarsch.systarest.DeviceTouchSearch.DeviceTouchDeviceInfo;
import de.freaklamarsch.systarest.SystaWaterHeaterStatus.tempUnit;

/**
 * Emulates the Paradigma SystaWeb service, which communicates with a SystaComfort
 * heating controller (version I or II) via UDP. This class listens for data packets
 * from the SystaComfort unit, processes them, stores the data, and can log this
 * data to files. It also provides methods to retrieve the current status and data.
 * <p>
 * The communication involves receiving data bursts from the SystaComfort unit,
 * typically every minute. These bursts consist of several UDP packets (types 0x01 to 0x04)
 * that together form a complete data set representing the state of the heating system.
 * This class parses these packets, stores the integer data values in a ring buffer,
 * and sends acknowledgment replies to keep the communication alive.
 * </p>
 * <p>
 * Key functionalities:
 * <ul>
 *   <li>Listening for UDP packets on a specific port (default 22460).</li>
 *   <li>Parsing different types of data packets from the SystaComfort unit.</li>
 *   <li>Storing received data (raw bytes and processed integers) in internal ring buffers.</li>
 *   <li>Sending acknowledgment replies to the SystaComfort unit.</li>
 *   <li>Optional logging of raw and processed data to CSV files using {@link DataLogger}.</li>
 *   <li>Providing status information about the FakeSystaWeb service and the connected device.</li>
 *   <li>Retrieving the latest processed data set and specific heating system parameters.</li>
 *   <li>Managing log files (zipping, deleting).</li>
 * </ul>
 * This class implements {@link Runnable} and is intended to be run in a separate thread.
 * </p>
 */
public class FakeSystaWeb implements Runnable {

    /**
     * Defines the types of messages received from the SystaComfort unit or
     * representing internal states.
     */
    enum MessageType {
        /** No message or an undefined state. */
        NONE,
        /** Data packet type 0x00 (typically an initial empty packet). */
        DATA0,
        /** Data packet type 0x01. */
        DATA1,
        /** Data packet type 0x02. */
        DATA2,
        /** Data packet type 0x03. */
        DATA3,
        /** Data packet type 0x04. */
        DATA4,
        /** Confirmation packet (e.g., parameter change OK, type 0xFF). */
        OK,
        /** Error or unknown message type. */
        ERR
    }

    /**
     * Represents the operational status of the {@link FakeSystaWeb} service.
     * This includes connection state, network parameters, logging status, and basic metrics.
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
        public final String commitDate; // This seems to be a static value from the original code

        /**
         * Constructs a {@code FakeSystaWebStatus} object.
         * @param running               True if the FakeSystaWeb service thread is active.
         * @param connected             True if data has been received from the SystaComfort unit recently.
         * @param dataPacketsReceived   Total number of UDP packets received since start.
         * @param timestamp             Timestamp of the last received data packet, as a formatted string.
         * @param localAddress          The local IP address the service is listening on.
         * @param localPort             The local UDP port the service is listening on.
         * @param remoteAddress         The IP address of the connected SystaComfort unit.
         * @param remotePort            The UDP port of the connected SystaComfort unit.
         * @param saveLoggedData        True if data logging to files is active.
         * @param capacity              Number of data entries stored per log file.
         * @param logFilePrefix         Prefix for log file names.
         * @param logEntryDelimiter     Delimiter used for entries in log files.
         * @param logFileRootPath       Root directory for log files.
         * @param writerFileCount       Number of log files written by the raw data logger.
         * @param bufferedEntries       Number of entries currently buffered by the raw data logger.
         * @param commitDate            A static string, possibly representing a build or version date.
         */
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
            this.commitDate = "2025-05-21T20:46:45+00:00"; // Matches original static value
        }
    }

    /**
     * Represents information discovered about a Paradigma SystaComfort unit,
     * typically obtained via {@link DeviceTouchSearch}. This class is currently
     * defined but not actively used within FakeSystaWeb to store discovered info.
     * It might be intended for future use or for other parts of the application.
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

        /**
         * Constructs a {@code SystaComfortInfo} object.
         * @param systawebIp IP address of the SystaWeb interface (if applicable).
         * @param systawebPort Port of the SystaWeb interface.
         * @param systaBcastIp Broadcast IP for Systa communication.
         * @param systaBcastPort Broadcast port for Systa communication.
         * @param scInfoString Raw information string from DeviceTouch search.
         * @param unitIp IP address of the SystaComfort unit.
         * @param unitStouchPort S-Touch communication port of the unit.
         * @param unitName Name of the unit.
         * @param unitId ID of the unit.
         * @param unitApp Application ID of the unit.
         * @param unitPlatform Platform ID of the unit.
         * @param unitScFullVersion Full version string of the unit's software.
         * @param unitScVersion Major version number.
         * @param unitScMinor Minor version number.
         * @param unitPassword Password for S-Touch communication.
         * @param unitBaseVersion Base software version of the unit.
         * @param unitMac MAC address of the unit.
         */
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
    private static final String COMMIT_DATE_STATIC = "2025-05-21T20:46:45+00:00"; // Static commit date from original code
    private static final int DEFAULT_PORT = 22460; // Default UDP port for SystaWeb communication
    private static final int MAX_DATAGRAM_LENGTH = 1048; // Maximum expected length of a UDP datagram
    private static final int MAX_ENTRIES_PER_DATAPACKET = 256; // Max number of integer values in one part of a multi-packet data set
    private static final int MAX_DATA_PACKETS_PER_SET = 4; // Max number of data packets (types 1-4) forming a complete data set

    /**
     * Offset used in generating a reply counter. Its specific meaning is tied to the
     * SystaComfort communication protocol for acknowledging received packets.
     */
    private static final int COUNTER_OFFSET_REPLY = 0x3FBF;
    /**
     * An alternative offset for the reply counter, possibly used for specific units or packet conditions.
     * The original code had a TODO to make this generic.
     */
    private static final int COUNTER_OFFSET_REPLY_2 = 0x3FC0;
    /**
     * Offset used in generating a reply ID from the MAC address of the SystaComfort unit.
     * Part of the acknowledgment mechanism.
     */
    private static final int MAC_OFFSET_REPLY = 0x8E82;

    // Ring buffer size for storing received data sets.
    // SystaComfort sends data in bursts; this buffer holds multiple recent data sets.
    // Original TODO: Adjust if more packet types are processed.
    private final int RING_BUFFER_SIZE = 6; // Increased slightly from the number of data packets for some leeway

    private static final String[] WATER_HEATER_OPERATION_MODES = {"off", "normal", "comfort", "locked"};
    private static final int DEFAULT_LOGGER_ENTRIES_PER_FILE = 60; // Default number of data sets per log file
    private static final String DEFAULT_LOGGER_DELIMITER = ";";
    private static final String DEFAULT_LOGGER_PREFIX = "SystaREST";
    private static final String DEFAULT_LOG_PATH = System.getProperty("user.home") + File.separator + "logs";
    private static final String LOG_FILE_REGEX = ".*-(raw|data)-[0-9]+\\.txt"; // Regex to identify log files
    private static final FilenameFilter logFileFilter = (dir, name) -> name.matches(LOG_FILE_REGEX);

    private MessageType typeOfLastReceivedMessage = MessageType.NONE;
    private InetAddress remoteAddress; // IP address of the connected SystaComfort unit
    private int remotePort; // Port of the connected SystaComfort unit

    // Ring buffer implementation details
    private volatile int readIndex = -1; // Index of the last fully processed and readable data set
    private int writeIndex = -1; // Index where the next incoming data set will be written

    private long dataPacketsReceived = 0; // Counter for total UDP packets received
    private final byte[][] replyHeader = new byte[RING_BUFFER_SIZE][8]; // Stores headers of received packets for replies

    /**
     * Ring buffer for storing processed integer data.
     * Each inner array represents a full data set, potentially assembled from multiple UDP packets.
     * The indices correspond to values defined in {@link SystaIndex}.
     * Access to this should be synchronized or carefully managed if read while writeIndex is being updated.
     * The current implementation copies data in processDataPacket, which helps.
     */
    private final Integer[][] intData = new Integer[RING_BUFFER_SIZE][MAX_ENTRIES_PER_DATAPACKET * MAX_DATA_PACKETS_PER_SET];
    private final long[] timestamp = new long[RING_BUFFER_SIZE]; // Timestamps for each data set in the ring buffer

    private String configuredInetAddress = "not configured"; // Local IP address to bind the listening socket to
    private DatagramSocket socket = null;
    private volatile boolean running = false; // Flag indicating if the main listening loop is active
    private volatile boolean stopRequested = false; // Flag to signal the listening loop to terminate
    private final byte[] receiveDataBuffer = new byte[MAX_DATAGRAM_LENGTH]; // Reusable buffer for incoming UDP packets
    private final DatagramPacket receivePacket = new DatagramPacket(receiveDataBuffer, receiveDataBuffer.length);

    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault());
    private final DataLogger<Integer> processedDataLogger; // Logger for processed integer data
    private final DataLogger<Byte> rawDataLogger; // Logger for raw byte data of received packets

    /**
     * Constructs a {@code FakeSystaWeb} instance.
     * Initializes data loggers and internal data structures.
     */
    public FakeSystaWeb() {
        Arrays.stream(intData).forEach(data -> Arrays.fill(data, 0)); // Initialize intData arrays
        Arrays.fill(timestamp, -1L); // Initialize timestamps to an invalid value

        // Initialize data loggers with default settings
        this.processedDataLogger = new DataLogger<>(DEFAULT_LOGGER_PREFIX, "data", DEFAULT_LOGGER_DELIMITER,
                DEFAULT_LOGGER_ENTRIES_PER_FILE, DEFAULT_LOG_PATH, timestampFormatter);
        this.rawDataLogger = new DataLogger<>(DEFAULT_LOGGER_PREFIX, "raw", DEFAULT_LOGGER_DELIMITER,
                DEFAULT_LOGGER_ENTRIES_PER_FILE, DEFAULT_LOG_PATH, timestampFormatter);
    }

    /**
     * Retrieves the current operational status of the FakeSystaWeb service.
     *
     * @return A {@link FakeSystaWebStatus} object containing status details.
     */
    public FakeSystaWebStatus getStatus() {
        DataLoggerStatus rawLoggerStatus = rawDataLogger.getStatus();
        boolean isConnected;
        String lastValidTimestampString;

        // Ensure thread-safe access to readIndex and timestamp
        // While readIndex is volatile, timestamp array elements are not.
        // A brief lock or using a snapshot of readIndex is safer.
        synchronized (this) { // Synchronize on 'this' to protect readIndex and timestamp read
            int currentReadIndex = this.readIndex;
            isConnected = (currentReadIndex >= 0) && (timestamp[currentReadIndex] > 0) &&
                          ((Instant.now().toEpochMilli() - timestamp[currentReadIndex]) < 120000); // 120 seconds
            lastValidTimestampString = (currentReadIndex >= 0 && timestamp[currentReadIndex] > 0) ?
                                       getFormattedTimeString(timestamp[currentReadIndex]) : "never";
        }

        return new FakeSystaWebStatus(this.running, isConnected, this.dataPacketsReceived, lastValidTimestampString,
                this.configuredInetAddress, DEFAULT_PORT, this.remoteAddress, this.remotePort,
                rawLoggerStatus.saveLoggedData, rawLoggerStatus.capacity,
                rawLoggerStatus.logFilePrefix, rawLoggerStatus.logEntryDelimiter, rawLoggerStatus.logFileRootPath,
                rawLoggerStatus.writerFileCount, rawLoggerStatus.bufferedEntries,
                COMMIT_DATE_STATIC);
    }

    /**
     * Searches for a SystaComfort device on the network using {@link DeviceTouchSearch}.
     *
     * @return {@link DeviceTouchDeviceInfo} for the found device, or {@code null} if not found.
     * @throws IOException if an error occurs during network communication.
     */
    public DeviceTouchDeviceInfo findSystaComfort() throws IOException {
        return DeviceTouchSearch.search();
    }

    /**
     * Gets the configured local IP address string that the service will attempt to bind to.
     *
     * @return The configured local IP address string.
     */
    public String getInetAddress() {
        return configuredInetAddress;
    }

    /**
     * Sets the local IP address string for the service to bind to.
     * This should be configured before starting the service via {@link #run()}.
     *
     * @param inetAddress The local IP address string.
     */
    public void setInetAddress(String inetAddress) {
        this.configuredInetAddress = inetAddress;
    }

    /**
     * Gets the timestamp of the last fully processed data set.
     *
     * @return The timestamp in milliseconds since the epoch, or -1 if no data has been processed.
     */
    public long getTimestamp() {
        // Ensure thread-safe access to readIndex and timestamp
        synchronized (this) {
            return (readIndex < 0) ? -1L : timestamp[readIndex];
        }
    }

    /**
     * Gets the timestamp of the last fully processed data set as a formatted string.
     *
     * @return The formatted timestamp string (ISO 8601 format with offset), or "never" if no data.
     */
    public String getTimestampString() {
        long tsValue;
        synchronized (this) { // Protect read of readIndex and timestamp element
            tsValue = (readIndex < 0) ? -1L : timestamp[readIndex];
        }
        return (tsValue <= 0) ? "never" : getFormattedTimeString(tsValue);
    }

    /**
     * Formats a given timestamp (milliseconds since epoch) into a human-readable string.
     * Uses the ISO 8601 format with local time zone offset (e.g., "2021-12-24T14:49:27.123+01:00").
     *
     * @param timestampMillis The timestamp in milliseconds since the epoch.
     * @return The formatted timestamp string.
     */
    public String getFormattedTimeString(long timestampMillis) {
        return timestampFormatter.format(Instant.ofEpochMilli(timestampMillis));
    }

    /**
     * Retrieves the last fully processed data set as an array of Integers.
     * The indices in the array correspond to values defined in {@link SystaIndex}.
     *
     * @return An array of {@link Integer} representing the data set, or {@code null} if no valid data is available.
     *         The returned array is a copy to prevent external modification.
     */
    public Integer[] getData() {
        Integer[] dataSnapshot = null;
        // Ensure thread-safe access to readIndex and intData/timestamp
        synchronized (this) {
            int currentReadIndex = this.readIndex;
            if (currentReadIndex >= 0 && timestamp[currentReadIndex] > 0) {
                // Return a copy to prevent external modification if intData[currentReadIndex] is mutable
                // and to ensure consistency with the timestamp.
                dataSnapshot = Arrays.copyOf(intData[currentReadIndex], intData[currentReadIndex].length);
            }
        }
        return dataSnapshot;
    }

    /**
     * Retrieves the status of the hot water system based on the latest processed data.
     *
     * @return A {@link SystaWaterHeaterStatus} object, or {@code null} if no valid data is available.
     */
    public SystaWaterHeaterStatus getWaterHeaterStatus() {
        SystaWaterHeaterStatus status = new SystaWaterHeaterStatus();
        Integer[] currentData;
        long currentTimestamp;

        synchronized (this) { // Synchronize access to shared data
            int currentReadIndex = this.readIndex;
            if (currentReadIndex < 0 || timestamp[currentReadIndex] <= 0) {
                return null;
            }
            // Make a copy of the data to work on outside the synchronized block if possible,
            // or ensure all reads are done within. Here, direct use is fine as it's brief.
            currentData = intData[currentReadIndex]; // No copy needed if just reading
            currentTimestamp = timestamp[currentReadIndex];
        }

        // TODO: Consider making these default/min/max values configurable or constants
        status.minTemp = 40.0;
        status.maxTemp = 65.0;
        status.currentTemperature = currentData[SystaIndex.HOT_WATER_TEMP] / 10.0;
        status.targetTemperature = currentData[SystaIndex.HOT_WATER_TEMP_SET] / 10.0;
        status.targetTemperatureHigh = currentData[SystaIndex.HOT_WATER_TEMP_MAX] / 10.0;
        status.targetTemperatureLow = Math.max(0.0,
                status.targetTemperature - currentData[SystaIndex.HOT_WATER_HYSTERESIS] / 10.0);
        status.temperatureUnit = tempUnit.TEMP_CELSIUS;
        status.currentOperation = WATER_HEATER_OPERATION_MODES[currentData[SystaIndex.HOT_WATER_OPERATION_MODE]];
        status.operationList = WATER_HEATER_OPERATION_MODES; // This is a static list
        status.supportedFeatures = new String[]{}; // TODO: Populate if features are known
        status.is_away_mode_on = false; // TODO: Correlate with heating system's holiday/away mode if available
        status.timestamp = currentTimestamp;
        status.timestampString = getFormattedTimeString(currentTimestamp);
        return status;
    }

    /**
     * Retrieves a comprehensive status of the Paradigma heating system based on the latest processed data.
     *
     * @return A {@link SystaStatus} object populated with various system parameters,
     *         or {@code null} if no valid data is available.
     */
    public SystaStatus getParadigmaStatus() {
        SystaStatus status = new SystaStatus();
        Integer[] currentData;
        long currentTimestamp;

        synchronized (this) { // Synchronize access to shared data
            int currentReadIndex = this.readIndex;
            if (currentReadIndex < 0 || timestamp[currentReadIndex] <= 0) {
                return null;
            }
            // Direct use of intData[currentReadIndex] is okay for reads if brief
            // or if data structure is effectively immutable post-write.
            currentData = intData[currentReadIndex];
            currentTimestamp = timestamp[currentReadIndex];
        }

        status.outsideTemp = currentData[SystaIndex.OUTSIDE_TEMP] / 10.0;
        status.circuit1FlowTemp = currentData[SystaIndex.CIRCUIT_1_FLOW_TEMP] / 10.0;
        status.circuit1ReturnTemp = currentData[SystaIndex.CIRCUIT_1_RETURN_TEMP] / 10.0;
        status.circuit1OperationMode = currentData[SystaIndex.CIRCUIT_1_OPERATION_MODE];
        status.hotWaterTemp = currentData[SystaIndex.HOT_WATER_TEMP] / 10.0;
        status.bufferTempTop = currentData[SystaIndex.BUFFER_TEMP_TOP] / 10.0;
        status.bufferTempBottom = currentData[SystaIndex.BUFFER_TEMP_BOTTOM] / 10.0;
        status.circulationTemp = currentData[SystaIndex.CIRCULATION_TEMP] / 10.0;
        status.circuit2FlowTemp = currentData[SystaIndex.CIRCUIT_2_FLOW_TEMP] / 10.0;
        status.circuit2ReturnTemp = currentData[SystaIndex.CIRCUIT_2_RETURN_TEMP] / 10.0;
        status.roomTempActual1 = currentData[SystaIndex.ROOM_TEMP_ACTUAL_1] / 10.0;
        status.roomTempActual2 = currentData[SystaIndex.ROOM_TEMP_ACTUAL_2] / 10.0;
        status.collectorTempActual = currentData[SystaIndex.COLLECTOR_TEMP_ACTUAL] / 10.0;
        status.boilerFlowTemp = currentData[SystaIndex.BOILER_FLOW_TEMP] / 10.0;
        status.boilerReturnTemp = currentData[SystaIndex.BOILER_RETURN_TEMP] / 10.0;
        status.logBoilerFlowTemp = currentData[SystaIndex.LOG_BOILER_FLOW_TEMP] / 10.0;
        status.logBoilerReturnTemp = currentData[SystaIndex.LOG_BOILER_RETURN_TEMP] / 10.0;
        status.logBoilerBufferTempTop = currentData[SystaIndex.LOG_BOILER_BUFFER_TEMP_TOP] / 10.0;
        status.swimmingpoolTemp = currentData[SystaIndex.SWIMMINGPOOL_TEMP] / 10.0;
        status.swimmingpoolFlowTemp = currentData[SystaIndex.SWIMMINGPOOL_FLOW_TEMP] / 10.0;
        status.swimmingpoolReturnTemp = currentData[SystaIndex.SWIMMINGPOOL_RETURN_TEMP] / 10.0;
        status.hotWaterTempSet = currentData[SystaIndex.HOT_WATER_TEMP_SET] / 10.0;
        status.roomTempSet1 = currentData[SystaIndex.ROOM_TEMP_SET_1] / 10.0;
        status.circuit1FlowTempSet = currentData[SystaIndex.CIRCUIT_1_FLOW_TEMP_SET] / 10.0;
        status.circuit2FlowTempSet = currentData[SystaIndex.CIRCUIT_2_FLOW_TEMP_SET] / 10.0;
        status.roomTempSet2 = currentData[SystaIndex.ROOM_TEMP_SET_2] / 10.0;
        status.bufferTempSet = currentData[SystaIndex.BUFFER_TEMP_SET] / 10.0;
        status.boilerTempSet = currentData[SystaIndex.BOILER_TEMP_SET] / 10.0;
        status.operationMode = currentData[SystaIndex.OPERATION_MODE];
        status.roomTempSetNormal = currentData[SystaIndex.ROOM_TEMP_SET_NORMAL] / 10.0;
        status.roomTempSetComfort = currentData[SystaIndex.ROOM_TEMP_SET_COMFORT] / 10.0;
        status.roomTempSetLowering = currentData[SystaIndex.ROOM_TEMP_SET_LOWERING] / 10.0;
        status.heatingOperationMode = currentData[SystaIndex.HEATING_OPERATION_MODE];
        status.controlledBy = currentData[SystaIndex.CONTROLLED_BY];
        status.heatingCurveBasePoint = currentData[SystaIndex.HEATING_CURVE_BASE_POINT] / 10.0;
        status.heatingCurveGradient = currentData[SystaIndex.HEATING_CURVE_GRADIENT] / 10.0;
        status.maxFlowTemp = currentData[SystaIndex.MAX_FLOW_TEMP] / 10.0;
        status.heatingLimitTemp = currentData[SystaIndex.HEATING_LIMIT_TEMP] / 10.0;
        status.heatingLimitTeampLowering = currentData[SystaIndex.HEATING_LIMIT_TEMP_LOWERING] / 10.0;
        status.antiFreezeOutsideTemp = currentData[SystaIndex.ANTI_FREEZE_OUTSIDE_TEMP] / 10.0;
        status.heatUpTime = currentData[SystaIndex.HEAT_UP_TIME]; // in minutes
        status.roomImpact = currentData[SystaIndex.ROOM_IMPACT] / 10.0;
        status.boilerSuperelevation = currentData[SystaIndex.BOILER_SUPERELEVATION];
        status.heatingCircuitSpreading = currentData[SystaIndex.HEATING_CIRCUIT_SPREADING] / 10.0;
        status.heatingPumpSpeedMin = currentData[SystaIndex.HEATING_PUMP_SPEED_MIN]; // in %
        status.mixerRuntime = currentData[SystaIndex.MIXER_RUNTIME]; // in minutes
        status.roomTempCorrection = currentData[SystaIndex.ROOM_TEMP_CORRECTION] / 10.0;
        status.underfloorHeatingBasePoint = currentData[SystaIndex.UNDERFLOOR_HEATING_BASE_POINT] / 10.0;
        status.underfloorHeatingGradient = currentData[SystaIndex.UNDERFLOOR_HEATING_GRADIENT] / 10.0;
        status.hotWaterTempNormal = currentData[SystaIndex.HOT_WATER_TEMP_NORMAL] / 10.0;
        status.hotWaterTempComfort = currentData[SystaIndex.HOT_WATER_TEMP_COMFORT] / 10.0;
        status.hotWaterOperationMode = currentData[SystaIndex.HOT_WATER_OPERATION_MODE];
        status.hotWaterHysteresis = currentData[SystaIndex.HOT_WATER_HYSTERESIS] / 10.0;
        status.hotWaterTempMax = currentData[SystaIndex.HOT_WATER_TEMP_MAX] / 10.0;
        status.heatingPumpOverrun = currentData[SystaIndex.PUMP_OVERRUN];
        status.heatingPumpSpeedActual = currentData[SystaIndex.HEATING_PUMP_SPEED_ACTUAL] * 5; // in %
        status.bufferTempMax = currentData[SystaIndex.BUFFER_TEMP_MAX] / 10.0;
        status.bufferTempMin = currentData[SystaIndex.BUFFER_TEMP_MIN] / 10.0;
        status.boilerHysteresis = currentData[SystaIndex.BOILER_HYSTERESIS] / 10.0;
        status.boilerOperationTime = currentData[SystaIndex.BOILER_RUNTIME_MIN]; // in min
        status.boilerShutdownTemp = currentData[SystaIndex.BOILER_SHUTDOWN_TEMP] / 10.0;
        status.boilerPumpSpeedMin = currentData[SystaIndex.BOILER_PUMP_SPEED_MIN]; // in %
        status.boilerPumpSpeedActual = currentData[SystaIndex.BOILER_PUMP_SPEED_ACTUAL] * 5;// 0=0%, 20=100%
        status.boilerOperationMode = currentData[SystaIndex.BOILER_OPERATION_MODE];
        status.circulationOperationMode = currentData[SystaIndex.CIRCULATION_OPERATION_MODE];
        status.circulationPumpOverrun = currentData[SystaIndex.CIRCULATION_PUMP_OVERRUN]; // in min
        status.circulationLockoutTimePushButton = currentData[SystaIndex.CIRCULATION_LOCKOUT_TIME_PUSH_BUTTON]; // in min
        status.circulationHysteresis = currentData[SystaIndex.CIRCULATION_HYSTERESIS] / 10.0;
        status.adjustRoomTempBy = currentData[SystaIndex.ADJUST_ROOM_TEMP_BY] / 10.0;
        status.boilerOperationTimeHours = currentData[SystaIndex.BOILER_OPERATION_TIME_HOURS];
        status.boilerOperationTimeMinutes = currentData[SystaIndex.BOILER_OPERATION_TIME_MINUTES];
        status.burnerNumberOfStarts = currentData[SystaIndex.BURNER_NUMBER_OF_STARTS];
        status.solarPowerActual = currentData[SystaIndex.SOLAR_POWER_ACTUAL] / 10.0;
        status.solarGainDay = currentData[SystaIndex.SOLAR_GAIN_DAY]; // in kWh
        status.solarGainTotal = currentData[SystaIndex.SOLAR_GAIN_TOTAL]; // in kWh
        status.systemNumberOfStarts = currentData[SystaIndex.SYSTEM_NUMBER_OF_STARTS];
        status.circuit1LeadTime = currentData[SystaIndex.CIRCUIT_1_LEAD_TIME]; // in min
        status.circuit2LeadTime = currentData[SystaIndex.CIRCUIT_2_LEAD_TIME]; // in min
        status.circuit3LeadTime = currentData[SystaIndex.CIRCUIT_3_LEAD_TIME]; // in min
        status.relay = currentData[SystaIndex.RELAY];
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
        status.error = currentData[SystaIndex.ERROR];
        status.operationModeX = currentData[SystaIndex.OPERATION_MODE_X];
        status.heatingOperationModeX = currentData[SystaIndex.HEATING_OPERATION_MODE_X];
        status.logBoilerBufferTempMin = currentData[SystaIndex.LOG_BOILER_BUFFER_TEMP_MIN] / 10.0;
        status.logBoilerTempMin = currentData[SystaIndex.LOG_BOILER_TEMP_MIN] / 10.0;
        status.logBoilerSpreadingMin = currentData[SystaIndex.LOG_BOILER_SPREADING_MIN] / 10.0;
        status.logBoilerPumpSpeedMin = currentData[SystaIndex.LOG_BOILER_PUMP_SPEED_MIN];// it is already in %
        status.logBoilerPumpSpeedActual = currentData[SystaIndex.LOG_BOILER_PUMP_SPEED_ACTUAL] * 5;// 0=0%, 20=100%
        status.logBoilerSettings = currentData[SystaIndex.LOG_BOILER_SETTINGS];
        status.logBoilerParallelOperation = (status.logBoilerSettings
                & SystaStatus.LOG_BOILER_PARALLEL_OPERATION_MASK) != 0;
        status.logBoilerOperationMode = currentData[SystaIndex.LOG_BOILER_OPERATION_MODE];
        status.boilerHeatsBuffer = (status.logBoilerSettings & SystaStatus.BOILER_HEATS_BUFFER_MASK) != 0;
        status.bufferType = currentData[SystaIndex.BUFFER_TYPE];
        status.timestamp = currentTimestamp;
        status.timestampString = getFormattedTimeString(currentTimestamp);
        return status;
    }

    /**
     * Stops the UDP communication listener thread and closes the socket.
     * This method signals the {@link #run()} method to terminate its loop.
     */
    public void stop() {
        printDebugInfo("Stop requested for FakeSystaWeb service.");
        stopRequested = true;
        if (socket != null && !socket.isClosed()) {
            socket.close(); // This will interrupt the blocking socket.receive() call in run()
        }
    }

    /**
     * Starts the UDP communication listener. This method is the entry point for the
     * {@link Runnable} interface. It opens a {@link DatagramSocket} on the configured
     * IP address and port, then enters a loop to receive and process data packets
     * from the SystaComfort unit until {@link #stop()} is called.
     * <p>
     * If the socket cannot be opened, the {@code running} flag is set to false, and the method returns.
     * </p>
     */
    @Override
    public void run() {
        if (running) {
            System.err.println("[FakeSystaWeb] Service is already running. Ignoring run() call.");
            return;
        }
        running = true;
        stopRequested = false; // Reset stop request flag
        dataPacketsReceived = 0; // Reset packet counter

        printDebugInfo("Starting FakeSystaWeb service on " + configuredInetAddress + ":" + DEFAULT_PORT);

        try {
            InetAddress ip = InetAddress.getByName(configuredInetAddress);
            socket = new DatagramSocket(DEFAULT_PORT, ip);
            printDebugInfo("DatagramSocket opened successfully.");
        } catch (IOException e) {
            // Use System.err for critical startup failures
            System.err.println("[FakeSystaWeb] Failed to open DatagramSocket on " + configuredInetAddress + ":" + DEFAULT_PORT + ". " + e.getMessage());
            running = false;
            // No need to close socket here as it likely wasn't opened.
            return;
        }

        printDebugInfo("UDP communication listener started. Waiting for packets...");
        while (!stopRequested) {
            try {
                receiveNextDatagram(); // Blocks until a packet is received or socket is closed
            } catch (IOException e) { // Catch specific IO exception from receiveNextDatagram
                if (stopRequested) {
                    printDebugInfo("Socket operation interrupted by stop request during receiveNextDatagram.");
                } else {
                    System.err.println("[FakeSystaWeb] IOException during receiveNextDatagram: " + e.getMessage());
                    // Consider if this error is recoverable or if the loop should break.
                    // For now, continue, but this might lead to rapid looping on certain errors.
                }
            }

            if (receivePacket.getLength() == 0 && stopRequested) {
                // Socket closed by stop() method, interrupting receive()
                printDebugInfo("Receive interrupted by socket closure, likely due to stop request.");
                break;
            }

            if (receivePacket.getLength() > 0) {
                // Process the received datagram
                processDatagram(ByteBuffer.wrap(receiveDataBuffer, 0, receivePacket.getLength()).order(ByteOrder.LITTLE_ENDIAN));
                // Notify any threads waiting on typeOfLastReceivedMessage
                // This is used by external components to synchronize with packet arrival.
                synchronized (typeOfLastReceivedMessage) {
                    typeOfLastReceivedMessage.notifyAll();
                }
            }
        }

        // Cleanup after loop termination
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        running = false;
        printDebugInfo("UDP communication listener stopped.");
    }

    /**
     * Processes the content of a received UDP datagram.
     * This involves updating the internal ring buffers with new data,
     * logging the data if enabled, and sending an acknowledgment reply.
     *
     * @param data A {@link ByteBuffer} containing the raw data from the datagram,
     *             starting after any initial fields already processed by the caller.
     *             The buffer should be in Little Endian order.
     */
    private void processDatagram(ByteBuffer data) {
        // Critical section: update shared data structures (writeIndex, timestamp, replyHeader, intData, readIndex)
        synchronized (this) {
            writeIndex = (readIndex + 1) % RING_BUFFER_SIZE;
            timestamp[writeIndex] = Instant.now().toEpochMilli();
            remoteAddress = receivePacket.getAddress(); // Assuming receivePacket is from the correct source
            remotePort = receivePacket.getPort();

            // Log raw data before further processing
            // Create a temporary copy of the relevant part of receiveDataBuffer for logging
            byte[] rawPacketData = Arrays.copyOf(receiveDataBuffer, receivePacket.getLength());
            rawDataLogger.addData(toByteArray(rawPacketData), timestamp[writeIndex]);

            // Extract the 8-byte header from the packet for the reply
            // The data buffer here is already positioned after the initial fields (e.g. type)
            // if processDatagram is called with data already advanced.
            // Assuming 'data' starts at the beginning of the UDP payload for header extraction.
            data.rewind(); // Ensure we read from the start of the payload for the header
            if (data.remaining() < 8) {
                printDebugInfo("Datagram too short for header extraction. Length: " + data.remaining());
                typeOfLastReceivedMessage = MessageType.ERR;
                return;
            }
            for (int i = 0; i < 8; i++) {
                replyHeader[writeIndex][i] = data.get();
            }

            // Further processing based on packet type (byte 16 of the original payload)
            if (data.limit() > 16) { // Check if byte 16 exists
                data.position(16); // Position to read the type byte
                byte type = data.get();
                printDebugInfo("Processing packet type: " + String.format("0x%02X", type));

                switch (type) {
                    case 0x00: // Empty initial packet
                        typeOfLastReceivedMessage = MessageType.DATA0;
                        sendDataReply(writeIndex); // Acknowledge
                        break;
                    case 0x01: // Data packet part 1
                        processDataType1(data); // data buffer is already positioned after type byte
                        typeOfLastReceivedMessage = MessageType.DATA1;
                        sendDataReply(writeIndex);
                        processedDataLogger.addData(intData[readIndex], timestamp[readIndex]);
                        break;
                    case 0x02: // Data packet part 2
                        processDataType2(data);
                        typeOfLastReceivedMessage = MessageType.DATA2;
                        sendDataReply(writeIndex);
                        processedDataLogger.addData(intData[readIndex], timestamp[readIndex]);
                        break;
                    case 0x03: // Data packet part 3
                        processDataType3(data);
                        typeOfLastReceivedMessage = MessageType.DATA3;
                        sendDataReply(writeIndex);
                        processedDataLogger.addData(intData[readIndex], timestamp[readIndex]);
                        break;
                    case 0x04: // Data packet part 4
                        processDataType4(data);
                        typeOfLastReceivedMessage = MessageType.DATA4;
                        sendDataReply(writeIndex);
                        processedDataLogger.addData(intData[readIndex], timestamp[readIndex]);
                        break;
                    case (byte) 0xFF: // Parameter change confirmation
                        typeOfLastReceivedMessage = MessageType.OK;
                        // Typically, no data reply needed for 0xFF, but protocol might vary.
                        // Original code does not sendDataReply here.
                        break;
                    default:
                        System.err.println("[FakeSystaWeb] Unknown message type received: " + String.format("0x%02X", type));
                        typeOfLastReceivedMessage = MessageType.ERR;
                        // Do not send a reply for truly unknown types, or send an error reply if protocol dictates.
                }
            } else {
                printDebugInfo("Datagram too short for type byte at position 16. Length: " + data.limit());
                typeOfLastReceivedMessage = MessageType.ERR;
            }
        } // End synchronized block
    }

    /**
     * Receives the next UDP datagram packet.
     * This method blocks until a packet is received or the socket times out/is closed.
     *
     * @throws IOException if a socket error occurs (other than timeout if not stopping).
     */
    private void receiveNextDatagram() throws IOException {
        try {
            // receivePacket uses receiveDataBuffer
            socket.receive(receivePacket);
            dataPacketsReceived++;
        } catch (IOException e) {
            if (stopRequested || socket.isClosed()) {
                // This is expected if stop() was called or socket closed externally
                printDebugInfo("Socket receive interrupted, likely due to stop request or closed socket.");
                receivePacket.setLength(0); // Ensure no partial data is processed
            } else {
                // Unexpected IOException
                System.err.println("[FakeSystaWeb] IOException while waiting for data on " +
                                   (socket.getLocalAddress() != null ? socket.getLocalAddress().getHostAddress() : "any") +
                                   ":" + socket.getLocalPort() + " - " + e.getMessage());
                receivePacket.setLength(0); // Ensure no partial data is processed
                throw e; // Re-throw to allow run() loop to handle or terminate
            }
        }
    }

    /**
     * Converts a {@code byte[]} to {@code Byte[]}.
     * Used for compatibility with {@link DataLogger} if it requires {@code Byte[]} objects.
     *
     * @param primitiveBytes The primitive byte array.
     * @return The corresponding {@code Byte} object array, or {@code null} if input is null.
     */
    private Byte[] toByteArray(byte[] primitiveBytes) {
        if (primitiveBytes == null) {
            return null;
        }
        Byte[] objectBytes = new Byte[primitiveBytes.length];
        for (int i = 0; i < primitiveBytes.length; i++) {
            objectBytes[i] = primitiveBytes[i]; // Autoboxing
        }
        return objectBytes;
    }

    /**
     * Sends an acknowledgment reply to the SystaComfort unit.
     * The reply is constructed based on the header of the received packet.
     *
     * @param currentWriteIndex The index in the ring buffer corresponding to the received packet
     *                          for which this reply is being sent.
     */
    private void sendDataReply(int currentWriteIndex) {
        // replyHeader is populated in processDatagram, from the start of the UDP payload
        byte[] replyPayload = Arrays.copyOf(replyHeader[currentWriteIndex], 16); // Reply is 16 bytes long

        // Modify bytes 12-15 of the replyPayload based on protocol rules
        // Generate reply ID from MAC address (bytes 4 and 5 of original header/payload):
        int macPartForId = (((replyPayload[5] & 0xFF) << 8) + (replyPayload[4] & 0xFF) + MAC_OFFSET_REPLY) & 0xFFFF;
        replyPayload[12] = (byte) (macPartForId & 0xFF);
        replyPayload[13] = (byte) (macPartForId >> 8);

        // Generate reply counter with offset (bytes 6 and 7 of original header/payload):
        int counterForReply = (((replyPayload[7] & 0xFF) << 8) + (replyPayload[6] & 0xFF) + COUNTER_OFFSET_REPLY) & 0xFFFF;

        // TODO: This specific unit check needs to be generalized or understood better.
        // It seems to be a workaround for particular SystaComfort units.
        if ((replyPayload[5] + replyPayload[4]) == 57 || (replyPayload[5] + replyPayload[4]) == 313) {
            counterForReply = (((replyPayload[7] & 0xFF) << 8) + (replyPayload[6] & 0xFF) + COUNTER_OFFSET_REPLY_2) & 0xFFFF;
        }
        replyPayload[14] = (byte) (counterForReply & 0xFF);
        replyPayload[15] = (byte) (counterForReply >> 8);

        send(replyPayload);
    }

    /**
     * Sends a byte array as a UDP datagram to the connected {@code remoteAddress} and {@code remotePort}.
     *
     * @param payload The byte array to send.
     */
    private void send(byte[] payload) {
        if (socket == null || socket.isClosed() || remoteAddress == null) {
            printDebugInfo("Cannot send reply: Socket closed or remote address unknown.");
            return;
        }
        try {
            DatagramPacket replyPacket = new DatagramPacket(payload, payload.length, remoteAddress, remotePort);
            socket.send(replyPacket);
            printDebugInfo("Sent reply of " + payload.length + " bytes to " + remoteAddress + ":" + remotePort);
        } catch (IOException ioe) {
            System.err.println("[FakeSystaWeb] Could not send reply: IOException, " + ioe.getMessage());
        } catch (IllegalArgumentException iae) {
            System.err.println("[FakeSystaWeb] Could not send reply: IllegalArgumentException, " + iae.getMessage());
        }
    }

    /**
     * Processes a data packet by updating a segment of the {@code intData} ring buffer.
     * Data packets (types 1-4) typically update different portions of the full data set.
     *
     * @param data         The {@link ByteBuffer} containing the packet data, positioned after initial header fields.
     * @param entryOffset  The starting index in the {@code intData} array for this packet type.
     */
    private void processDataPacket(ByteBuffer data, int entryOffset) {
        // This method is called within a synchronized block in processDatagram
        // data.position(24) was original; assuming data is now positioned right after type byte (pos 17)
        // The actual data values start after an additional 7 bytes of unknown/fixed fields (17+7=24)
        if (data.position() != 17) { // Should be at pos 17 if type byte was just read
             printDebugInfo("processDataPacket: Unexpected buffer position: " + data.position() + ". Resetting to 17.");
             if(data.limit() > 16 ) data.position(17); else {printDebugInfo("Buffer too short."); return;}
        }
        if (data.remaining() < 7 ) { // Need at least the 7 unknown bytes
             printDebugInfo("processDataPacket: Buffer too short for fixed fields. Remaining: " + data.remaining()); return;
        }
        data.position(data.position() + 7); // Skip 7 bytes (position 17 to 23 inclusive)

        if (readIndex >= 0) {
            // Data packets are updates; copy the previous full data set to the current writeIndex
            // to ensure un-updated fields retain their previous values.
            System.arraycopy(intData[readIndex], 0, intData[writeIndex], 0, intData[readIndex].length);
        } else {
            // First time receiving data, ensure the target array is clean (though constructor does this)
             Arrays.fill(intData[writeIndex], 0);
        }

        int dataValueIndex = 0;
        while (data.remaining() >= 4) { // Each data value is an int (4 bytes)
            intData[writeIndex][entryOffset + dataValueIndex] = data.getInt();
            dataValueIndex++;
        }
        readIndex = writeIndex; // The current writeIndex now holds the latest complete (or updated) data set
        printDebugInfo("Processed data packet, updated " + dataValueIndex + " integers at offset " + entryOffset + ". New readIndex: " + readIndex);
    }

    /**
     * Processes data from a type 0x01 UDP packet.
     * These packets typically contain the first block of data values.
     *
     * @param data ByteBuffer containing the packet data, positioned after the type byte.
     */
    private void processDataType1(ByteBuffer data) {
        processDataPacket(data, 0); // Offset 0 for type 1 data
    }

    /**
     * Processes data from a type 0x02 UDP packet.
     *
     * @param data ByteBuffer containing the packet data, positioned after the type byte.
     */
    private void processDataType2(ByteBuffer data) {
        processDataPacket(data, MAX_ENTRIES_PER_DATAPACKET); // Offset by 1 * MAX_ENTRIES_PER_DATAPACKET
    }

    /**
     * Processes data from a type 0x03 UDP packet.
     *
     * @param data ByteBuffer containing the packet data, positioned after the type byte.
     */
    private void processDataType3(ByteBuffer data) {
        processDataPacket(data, 2 * MAX_ENTRIES_PER_DATAPACKET); // Offset by 2 * MAX_ENTRIES_PER_DATAPACKET
    }

    /**
     * Processes data from a type 0x04 UDP packet.
     *
     * @param data ByteBuffer containing the packet data, positioned after the type byte.
     */
    private void processDataType4(ByteBuffer data) {
        processDataPacket(data, 3 * MAX_ENTRIES_PER_DATAPACKET); // Offset by 3 * MAX_ENTRIES_PER_DATAPACKET
    }

    /**
     * Activates logging for both raw and processed data.
     * Uses default settings for entries per file.
     */
    public void logRawData() {
        rawDataLogger.saveLoggedData();
        processedDataLogger.saveLoggedData();
        printDebugInfo("Started logging raw and processed data with default entries per file.");
    }

    /**
     * Activates logging for both raw and processed data with a specified number of entries per file.
     *
     * @param entriesPerFile Number of data entries (packets/sets) to store in each log file.
     */
    public void logRawData(int entriesPerFile) {
        rawDataLogger.saveLoggedData(entriesPerFile);
        processedDataLogger.saveLoggedData(entriesPerFile);
        printDebugInfo("Started logging raw and processed data with " + entriesPerFile + " entries per file.");
    }

    /**
     * Activates logging for both raw and processed data with a specified file prefix.
     *
     * @param filePrefix The prefix to use for log file names.
     */
    public void logRawData(String filePrefix) {
        rawDataLogger.saveLoggedData(filePrefix);
        processedDataLogger.saveLoggedData(filePrefix);
        printDebugInfo("Started logging raw and processed data with file prefix: " + filePrefix);
    }

    /**
     * Activates logging for both raw and processed data with specified prefix, delimiter, and entries per file.
     *
     * @param filePrefix     The prefix for log file names.
     * @param delimiter      The delimiter to use between values in log entries.
     * @param entriesPerFile Number of data entries per log file.
     */
    public void logRawData(String filePrefix, String delimiter, int entriesPerFile) {
        rawDataLogger.saveLoggedData(filePrefix, delimiter, entriesPerFile);
        processedDataLogger.saveLoggedData(filePrefix, delimiter, entriesPerFile);
        printDebugInfo("Started logging with custom settings: prefix=" + filePrefix + ", delimiter='" + delimiter + "', entriesPerFile=" + entriesPerFile);
    }

    /**
     * Stops logging for both raw and processed data.
     * Any buffered data will be flushed to files.
     */
    public void stopLoggingRawData() {
        rawDataLogger.stopSavingLoggedData();
        processedDataLogger.stopSavingLoggedData();
        printDebugInfo("Stopped logging raw and processed data.");
    }

    /**
     * Collects all existing log files (matching {@link #LOG_FILE_REGEX}) from the
     * configured log path, zips them into a single archive, and returns the archive file.
     * The archive is named with a timestamp, e.g., "SystaPiLogs_yyyyMMddHHmmss.zip".
     * If an existing archive with the same name is present, it is deleted first.
     *
     * @return The {@link File} object representing the created zip archive,
     *         or {@code null} if an error occurs during zipping or if no log files are found.
     */
    public File getAllLogs() {
        String zipFileName = DEFAULT_LOG_PATH + File.separator + "SystaPiLogs_"
                + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".zip";
        File zippedLogs = new File(zipFileName);

        if (zippedLogs.exists()) {
            if (!zippedLogs.delete()) {
                System.err.println("[FakeSystaWeb] Could not delete existing zip file: " + zipFileName);
                // Optionally, could append a unique ID to filename instead of failing
            }
        }

        File logDirectory = new File(DEFAULT_LOG_PATH);
        if (!logDirectory.exists() || !logDirectory.isDirectory()) {
            printDebugInfo("Log directory does not exist: " + DEFAULT_LOG_PATH);
            return null;
        }

        File[] filesToZip = logDirectory.listFiles(logFileFilter);

        if (filesToZip == null || filesToZip.length == 0) {
            printDebugInfo("No log files found in " + DEFAULT_LOG_PATH + " matching filter " + LOG_FILE_REGEX);
            return null;
        }

        printDebugInfo("Found " + filesToZip.length + " log files to be zipped into " + zipFileName);

        try (FileOutputStream fos = new FileOutputStream(zippedLogs);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File file : filesToZip) {
                if (file.isDirectory()) {
                    continue; // Skip directories
                }
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) >= 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                } catch (IOException e) {
                    // Log error for specific file and continue if possible, or rethrow
                    System.err.println("[FakeSystaWeb] Error zipping file " + file.getName() + ": " + e.getMessage());
                    // Depending on desired behavior, could throw e here to stop zipping.
                }
            }
        } catch (IOException e) {
            System.err.println("[FakeSystaWeb] Error creating zip archive " + zipFileName + ": " + e.getMessage());
            // Attempt to delete partially created zip file on error
            if (zippedLogs.exists()) {
                zippedLogs.delete();
            }
            return null; // Or throw a new RuntimeException wrapping e
        }
        printDebugInfo("Successfully created log archive: " + zipFileName);
        return zippedLogs;
    }

    /**
     * Deletes all log files matching the {@link #LOG_FILE_REGEX} from the configured log path.
     * Also resets the internal file write counters of the data loggers.
     *
     * @return The number of files successfully deleted.
     */
    public int deleteAllLogs() {
        AtomicInteger deletedCount = new AtomicInteger(0);
        File logDir = new File(DEFAULT_LOG_PATH);

        if (!logDir.exists() || !logDir.isDirectory()) {
            printDebugInfo("Log directory does not exist for deletion: " + DEFAULT_LOG_PATH);
            return 0;
        }

        File[] filesToDelete = logDir.listFiles(logFileFilter);

        if (filesToDelete != null) {
            Arrays.stream(filesToDelete).forEach(file -> {
                if (file.delete()) {
                    printDebugInfo("Deleted log file: " + file.getName());
                    deletedCount.incrementAndGet();
                } else {
                    System.err.println("[FakeSystaWeb] Failed to delete log file: " + file.getName());
                }
            });
        }

        int loggerFileCountSum = rawDataLogger.getWriterFileCount() + processedDataLogger.getWriterFileCount();
        if (deletedCount.get() != loggerFileCountSum && filesToDelete != null && filesToDelete.length > 0) {
            // This condition might be noisy if files were deleted manually or by another process.
            // Only log if there was an expectation of files to delete.
            printDebugInfo("Mismatch in deleted file count. Deleted: " + deletedCount.get() +
                           ", Loggers expected: " + loggerFileCountSum);
        }

        rawDataLogger.setWriterFileCount(0);
        processedDataLogger.setWriterFileCount(0);
        printDebugInfo("Log file counters reset. Total files deleted: " + deletedCount.get());
        return deletedCount.get();
    }
}
