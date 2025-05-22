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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

/**
 * Logs arrays of generic data type {@code T} along with timestamps to delimited text files (e.g., CSV).
 *
 * <p><b>Data Storage and Buffering:</b>
 * The {@code DataLogger} maintains an in-memory {@link CircularBuffer} for data entries ({@code T[]})
 * and a corresponding buffer for their timestamps. The capacity of these buffers is configurable.
 * When new data is added via {@link #addData(Object[], long)}:
 * <ul>
 *     <li>If file logging ({@link #saveLoggedData}) is disabled (default), new entries overwrite the oldest
 *         entries in the circular buffer once capacity is reached.</li>
 *     <li>If file logging is enabled, and the buffer becomes full, its contents are written to a new log file,
 *         and the buffer is cleared.</li>
 * </ul>
 * This design allows for continuous data capture in memory even if file writing is not active,
 * and efficient batch writing to files when it is.
 * </p>
 *
 * <p><b>File Writing:</b>
 * When {@link #saveLoggedData} is true, data is written to files in the specified {@link #logFileRootPath}.
 * File names are generated using a prefix ({@link #logFilePrefix}), a base filename ({@link #logFilename}),
 * and an incrementing counter ({@link #writerFileCount}).
 * The delimiter for entries within the log file can be customized (default is ";").
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *     <li>Configurable buffer capacity ({@link #setCapacity(int)}).</li>
 *     <li>Dynamic control over file logging activation ({@link #saveLoggedData()}, {@link #stopSavingLoggedData()}).</li>
 *     <li>Customizable log file path, prefix, name, and entry delimiter.</li>
 *     <li>Timestamping of data entries with a configurable {@link DateTimeFormatter}.</li>
 *     <li>Status reporting via {@link #getStatus()}.</li>
 * </ul>
 * </p>
 *
 * <p><b>Thread Safety:</b>
 * Methods that modify the internal buffers or logging state (e.g., {@code addData}, {@code saveLoggedData},
 * {@code setCapacity}) are synchronized to ensure thread safety when accessed concurrently.
 * File I/O operations are also performed within synchronized blocks or methods.
 * </p>
 *
 * @param <T> The type of data elements within the arrays being logged.
 */
public class DataLogger<T> {

    /**
     * Inner class representing the current status of the {@link DataLogger}.
     * Provides a snapshot of key configuration parameters and operational state.
     */
    public static class DataLoggerStatus {
        public final int capacity;
        public final boolean saveLoggedData;
        public final String logFilePrefix;
        public final String logFileRootPath;
        public final int writerFileCount;
        public final String logEntryDelimiter;
        public final int bufferedEntries;
        public final String lastTimestamp;

        /**
         * Constructs a {@code DataLoggerStatus} object.
         * @param capacity Current buffer capacity.
         * @param saveLoggedData Whether file logging is active.
         * @param logFilePrefix Prefix for log file names.
         * @param logFileRootPath Root directory for log files.
         * @param logEntryDelimiter Delimiter used in log files.
         * @param writerFileCount Number of files written so far.
         * @param bufferedEntries Number of entries currently in the buffer.
         * @param lastTimestamp Timestamp of the most recent entry, or "never" if buffer is empty.
         */
        public DataLoggerStatus(int capacity, boolean saveLoggedData, String logFilePrefix, String logFileRootPath,
                                String logEntryDelimiter, int writerFileCount, int bufferedEntries, String lastTimestamp) {
            this.capacity = capacity;
            this.saveLoggedData = saveLoggedData;
            this.logFilePrefix = logFilePrefix;
            this.logFileRootPath = logFileRootPath;
            this.logEntryDelimiter = logEntryDelimiter;
            this.writerFileCount = writerFileCount;
            this.bufferedEntries = bufferedEntries;
            this.lastTimestamp = lastTimestamp;
        }
    }

    private static final int DEFAULT_CAPACITY = 60;
    private static final String DEFAULT_DELIMITER = ";";
    private static final String DEFAULT_PREFIX = "DataLogger";
    private static final String DEFAULT_FILENAME = ""; // Base filename, often combined with prefix and count
    private static final String DEFAULT_ROOT_PATH = System.getProperty("user.home") + File.separator + "logs";
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("E-dd.MM.yy-HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    private int capacity = DEFAULT_CAPACITY;
    private CircularBuffer<T[]> dataBuffer; // Buffer for data arrays
    private CircularBuffer<String> timestampBuffer; // Buffer for corresponding timestamps as formatted strings
    private boolean saveLoggedData = false; // Flag indicating if data should be written to files
    private String logFilePrefix = DEFAULT_PREFIX;
    private String logFilename = DEFAULT_FILENAME;
    private String logFileRootPath = DEFAULT_ROOT_PATH;
    private String logEntryDelimiter = DEFAULT_DELIMITER;
    private DateTimeFormatter timestampFormatter = DEFAULT_FORMATTER;
    private int writerFileCount = 0; // Counter for generated log files

    /**
     * Constructs a {@code DataLogger} with default settings.
     * It uses a default capacity of {@value #DEFAULT_CAPACITY} entries per file,
     * a default delimiter (";"), prefix ("DataLogger"), and root path for logs
     * (user's home directory + "/logs").
     * File logging is initially disabled.
     */
    public DataLogger() {
        this(DEFAULT_PREFIX, DEFAULT_FILENAME, DEFAULT_DELIMITER, DEFAULT_CAPACITY, DEFAULT_ROOT_PATH, DEFAULT_FORMATTER);
    }

    /**
     * Constructs a {@code DataLogger} with specified parameters.
     *
     * @param prefix         The prefix for log file names (e.g., "SensorA").
     * @param filename       The base name for log files (can be empty, often used with prefix and counter).
     * @param delimiter      The delimiter string to separate values in log entries (e.g., ";", ",").
     * @param entriesPerFile The capacity of the internal buffer and the number of entries per log file.
     *                       If non-positive, {@value #DEFAULT_CAPACITY} is used.
     * @param rootPath       The root directory where log files will be stored.
     * @param formatter      The {@link DateTimeFormatter} to format timestamps for log entries.
     */
    public DataLogger(String prefix, String filename, String delimiter, int entriesPerFile, String rootPath, DateTimeFormatter formatter) {
        this.capacity = (entriesPerFile > 0) ? entriesPerFile : DEFAULT_CAPACITY;
        this.logFilePrefix = prefix;
        this.logFilename = filename;
        this.logEntryDelimiter = delimiter;
        this.logFileRootPath = rootPath;
        this.timestampFormatter = (formatter != null) ? formatter : DEFAULT_FORMATTER;

        this.dataBuffer = new CircularBuffer<>(this.capacity);
        this.dataBuffer.setOverwrite(true); // Overwrite oldest if buffer full and not saving to file
        this.timestampBuffer = new CircularBuffer<>(this.capacity);
        this.timestampBuffer.setOverwrite(true); // Consistent overwrite behavior
    }

    /**
     * Sets the delimiter string used for separating values within each log entry in the output file.
     * If logging is active, it is stopped, current buffer contents are written to a file (if any),
     * and then logging is re-enabled with the new delimiter for subsequent files.
     *
     * @param delimiter The new delimiter string (e.g., ";", ",").
     */
    public synchronized void setLogEntryDelimiter(String delimiter) {
        boolean wasLoggingActive = flushBuffersAndMaintainLoggingState();
        this.logEntryDelimiter = delimiter;
        if (wasLoggingActive) {
            this.saveLoggedData = true; // Restore logging state if it was active
        }
    }

    /**
     * Retrieves the current status of the {@code DataLogger}.
     * This includes configuration settings like capacity, file paths, and operational state
     * like whether logging is active and the number of buffered entries.
     *
     * @return A {@link DataLoggerStatus} object containing the current status.
     */
    public DataLoggerStatus getStatus() {
        // Synchronization is not strictly necessary here if reads are atomic and
        // eventual consistency of status is acceptable. However, to get a perfectly
        // consistent snapshot with buffer size, it's safer.
        synchronized (this) {
            return new DataLoggerStatus(capacity, saveLoggedData, logFilePrefix, logFileRootPath, logEntryDelimiter,
                    writerFileCount, timestampBuffer.size(), Objects.requireNonNullElse(timestampBuffer.end(), "never"));
        }
    }

    /**
     * Gets the current capacity of the internal buffer (and entries per log file).
     * @return The capacity.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Sets the capacity of the internal buffer and the number of entries per log file.
     * If logging is active, it is stopped, current buffer contents are written to a file (if any),
     * buffers are reinitialized with the new capacity, and then logging is re-enabled.
     *
     * @param capacity The new capacity. If non-positive, {@value #DEFAULT_CAPACITY} is used.
     */
    public synchronized void setCapacity(int capacity) {
        boolean wasLoggingActive = flushBuffersAndMaintainLoggingState();
        this.capacity = (capacity > 0) ? capacity : DEFAULT_CAPACITY;
        this.dataBuffer = new CircularBuffer<>(this.capacity);
        this.dataBuffer.setOverwrite(true);
        this.timestampBuffer = new CircularBuffer<>(this.capacity);
        this.timestampBuffer.setOverwrite(true);
        if (wasLoggingActive) {
            this.saveLoggedData = true; // Restore logging state
        }
    }

    /**
     * Gets the current prefix used for log file names.
     * @return The log file prefix.
     */
    public String getLogFilePrefix() {
        return logFilePrefix;
    }

    /**
     * Sets the prefix for log file names.
     * If logging is active, it is stopped, current buffer contents are written to a file (if any),
     * and then logging is re-enabled for subsequent files with the new prefix.
     *
     * @param logFilePrefix The new log file prefix.
     */
    public synchronized void setLogFilePrefix(String logFilePrefix) {
        boolean wasLoggingActive = flushBuffersAndMaintainLoggingState();
        this.logFilePrefix = logFilePrefix;
        if (wasLoggingActive) {
            this.saveLoggedData = true;
        }
    }

    /**
     * Gets the current base name for log files.
     * @return The log file base name.
     */
    public String getLogFilename() {
        return logFilename;
    }

    /**
     * Sets the base name for log files.
     * If logging is active, it is stopped, current buffer contents are written to a file (if any),
     * and then logging is re-enabled for subsequent files with the new base name.
     *
     * @param logFilename The new log file base name.
     */
    public synchronized void setLogFilename(String logFilename) {
        boolean wasLoggingActive = flushBuffersAndMaintainLoggingState();
        this.logFilename = logFilename;
        if (wasLoggingActive) {
            this.saveLoggedData = true;
        }
    }

    /**
     * Gets the current root directory path for storing log files.
     * @return The log file root path.
     */
    public String getLogFileRootPath() {
        return logFileRootPath;
    }

    /**
     * Sets the root directory path for storing log files.
     * If logging is active, it is stopped, current buffer contents are written to a file (if any),
     * and then logging is re-enabled to use the new root path for subsequent files.
     *
     * @param logFileRootPath The new root directory path.
     */
    public synchronized void setLogFileRootPath(String logFileRootPath) {
        boolean wasLoggingActive = flushBuffersAndMaintainLoggingState();
        this.logFileRootPath = logFileRootPath;
        if (wasLoggingActive) {
            this.saveLoggedData = true;
        }
    }

    /**
     * Gets the count of log files written since the DataLogger was instantiated or {@link #setWriterFileCount(int)} was last called.
     * @return The number of files written.
     */
    public int getWriterFileCount() {
        return writerFileCount;
    }

    /**
     * Sets the counter for log files. This can be used to reset or change the numbering of subsequent log files.
     * @param writerFileCount The new starting count for written files.
     */
    public void setWriterFileCount(int writerFileCount) {
        // This method typically doesn't need to interrupt logging flow,
        // but if consistency with file naming is paramount during a change,
        // synchronization might be added.
        this.writerFileCount = writerFileCount;
    }

    /**
     * Gets the delimiter string used for separating values within each log entry.
     * @return The log entry delimiter.
     */
    public String getLogEntryDelimiter() {
        return logEntryDelimiter;
    }

    /**
     * Activates the saving of log data to files.
     * When the internal buffer reaches its capacity ({@link #getCapacity()}), its contents
     * will be written to a new log file, and the buffer will be cleared.
     * If logging was already active, this call effectively ensures it remains active.
     * If logging was inactive, any existing data in the buffer is written to a file first.
     */
    public synchronized void saveLoggedData() {
        flushBuffersAndMaintainLoggingState(); // Write out any existing buffer content if changing state
        this.saveLoggedData = true;
    }

    /**
     * Helper method to manage logging state changes.
     * If file logging ({@code saveLoggedData}) is currently active, it stops logging
     * and writes any buffered data to a file. The method then returns {@code true}
     * indicating logging was initially active.
     * If file logging is not active, it still writes any buffered data to a file
     * (effectively flushing the buffer) and returns {@code false}.
     * This is used by setters to ensure data isn't lost when parameters change
     * and to correctly restore the logging state.
     *
     * @return {@code true} if {@code saveLoggedData} was {@code true} before this call, {@code false} otherwise.
     */
    private synchronized boolean flushBuffersAndMaintainLoggingState() {
        boolean wasSaving = saveLoggedData;
        if (saveLoggedData) {
            // If currently saving, temporarily disable, write, then caller will re-enable if needed.
            saveLoggedData = false; // Prevent recursive calls or writing while changing params
            writeLoggedDataToFileInternal(); // Internal call to avoid re-checking saveLoggedData flag
        } else {
            // If not currently saving, still write out any buffered data before changing params.
            writeLoggedDataToFileInternal();
        }
        return wasSaving;
    }

    /**
     * Activates file logging and sets the number of entries per log file (buffer capacity).
     * If logging is already active, current buffer contents are written to a file,
     * buffers are reinitialized with the new capacity, and logging is restarted.
     *
     * @param entriesPerFile The number of entries per log file. This also sets the buffer capacity.
     *                       If non-positive, {@value #DEFAULT_CAPACITY} is used.
     */
    public synchronized void saveLoggedData(int entriesPerFile) {
        flushBuffersAndMaintainLoggingState(); // Flushes buffer and captures old logging state
        int newCapacity = (entriesPerFile > 0) ? entriesPerFile : DEFAULT_CAPACITY;
        if (this.capacity != newCapacity) {
            this.capacity = newCapacity;
            this.dataBuffer = new CircularBuffer<>(this.capacity);
            this.dataBuffer.setOverwrite(true);
            this.timestampBuffer = new CircularBuffer<>(this.capacity);
            this.timestampBuffer.setOverwrite(true);
        }
        this.saveLoggedData = true; // Explicitly enable logging
    }

    /**
     * Activates file logging and sets the prefix for log file names.
     * If logging is already active, this method ensures it continues with the new prefix.
     * Any existing data in the buffer is written out first if the logging state changes.
     *
     * @param filePrefix The new prefix for log file names.
     */
    public synchronized void saveLoggedData(String filePrefix) {
        flushBuffersAndMaintainLoggingState();
        this.logFilePrefix = filePrefix;
        this.saveLoggedData = true;
    }

    /**
     * Activates file logging and sets the file prefix, entry delimiter, and entries per file (capacity).
     * This is a convenience method to configure multiple logging parameters at once.
     * If logging is already active, current buffer contents are written, settings are updated,
     * and logging is restarted.
     *
     * @param filePrefix     The new prefix for log file names.
     * @param delimiter      The new delimiter for log entries.
     * @param entriesPerFile The new number of entries per log file (buffer capacity).
     *                       If non-positive, {@value #DEFAULT_CAPACITY} is used.
     */
    public synchronized void saveLoggedData(String filePrefix, String delimiter, int entriesPerFile) {
        flushBuffersAndMaintainLoggingState(); // Flush current buffer, store old logging state
        this.logFilePrefix = filePrefix;
        this.logEntryDelimiter = delimiter;
        int newCapacity = (entriesPerFile > 0) ? entriesPerFile : DEFAULT_CAPACITY;
        if (this.capacity != newCapacity) {
            this.capacity = newCapacity;
            this.dataBuffer = new CircularBuffer<>(this.capacity);
            this.dataBuffer.setOverwrite(true);
            this.timestampBuffer = new CircularBuffer<>(this.capacity);
            this.timestampBuffer.setOverwrite(true);
        }
        this.saveLoggedData = true; // Explicitly enable logging
    }

    /**
     * Stops the saving of log data to files.
     * If file logging was active, any remaining data in the buffer is written to a final log file.
     * The {@code DataLogger} will then revert to only storing data in its in-memory circular buffer,
     * overwriting old entries when full.
     */
    public synchronized void stopSavingLoggedData() {
        if (saveLoggedData) {
            saveLoggedData = false; // Disable further automatic writes
            // Write out the last file with any remaining buffered data
            writeLoggedDataToFileInternal();
        }
        // If not saveLoggedData, buffer might still contain data.
        // Consider if an explicit flush option without changing saveLoggedData state is needed,
        // or if users should call writeLoggedDataToFile() manually if they want to clear
        // the buffer when saveLoggedData is false. Current behavior: stop implies flush.
    }

    /**
     * Adds a data array and its corresponding timestamp to the internal buffers.
     * <ul>
     *     <li>The timestamp is formatted using the configured {@link #timestampFormatter}.</li>
     *     <li>To prevent duplicate entries with the exact same millisecond timestamp,
     *         this method checks if the new formatted timestamp is identical to the last one added;
     *         if so, the new data is ignored.</li>
     *     <li>A shallow copy of the input data array is stored to prevent external modifications
     *         from affecting buffered data.</li>
     *     <li>If file logging ({@link #saveLoggedData}) is active and the buffers become full after
     *         adding the new entry, {@link #writeLoggedDataToFileInternal()} is called to write the
     *         buffer contents to a file.</li>
     * </ul>
     * This method is synchronized to ensure thread-safe access to the data and timestamp buffers.
     *
     * @param data      The array of data items ({@code T[]}) to be logged.
     * @param timestamp The timestamp (in milliseconds since epoch) for this data entry.
     */
    public synchronized void addData(T[] data, long timestamp) {
        // Format the timestamp
        String newTimestamp = timestampFormatter.format(Instant.ofEpochMilli(timestamp));
        String lastTimestamp = timestampBuffer.end(); // Peek at the last timestamp without removing

        // Prevent duplicate entries if timestamps are identical (e.g. multiple calls within the same millisecond)
        // This check relies on the configured timestampFormatter's resolution.
        if (lastTimestamp != null && newTimestamp.equals(lastTimestamp)) {
            // If data is arriving faster than the timestamp resolution, subsequent entries
            // for the same formatted timestamp will be skipped.
            return;
        }

        // Add the new formatted timestamp to its buffer
        timestampBuffer.add(newTimestamp);

        // Add a shallow copy of the data array to its buffer.
        // This is crucial: if the caller reuses/modifies the 'data' array after this call,
        // the buffered version remains unchanged. For deep immutability of T elements,
        // users must ensure T itself is immutable or perform deep copies before calling addData.
        T[] dataCopy = Arrays.copyOf(data, data.length);
        dataBuffer.add(dataCopy);

        // If buffers are now full and file logging is active, write data to a file
        if (timestampBuffer.isFull() && saveLoggedData) {
            writeLoggedDataToFileInternal();
        }
    }

    /**
     * Internal method to write the current contents of the data and timestamp buffers to a log file.
     * This method is called when buffers are full (if {@code saveLoggedData} is true) or when
     * logging is being stopped/reconfigured.
     * <p>
     * Operations:
     * 1. Checks buffer synchronization via {@link #checkAndFixBufferSync()}. If unsynced or empty, returns false.
     * 2. Converts buffer contents to a 2D String array using {@link #convertBuffersToStringArray()}.
     * 3. Ensures the log directory ({@link #logFileRootPath}) exists, creating it if necessary.
     * 4. Constructs the log file name using prefix, base name, root path, and write counter.
     * 5. Writes the data to the file using {@link #writeLogFile(String[][], String)}.
     * </p>
     * This method assumes it's called from a synchronized context if protection is needed.
     *
     * @return {@code true} if data was successfully written to a file, {@code false} otherwise (e.g., buffers empty, sync issues, I/O error).
     */
    private boolean writeLoggedDataToFileInternal() {
        // Critical section for buffer access and file writing, ensure calling context is synchronized.
        if (!checkAndFixBufferSync()) { // Also handles empty buffer case
            return false; // Buffers were empty or became unsynced and were cleared.
        }

        // Convert data from circular buffers to a 2D array suitable for writing.
        // This process also clears the buffers.
        String[][] fileContent = convertBuffersToStringArray();

        // Ensure the target directory for log files exists.
        File logDirectory = new File(logFileRootPath);
        if (!logDirectory.exists()) {
            if (!logDirectory.mkdirs()) {
                // Consider logging this failure to a more robust logging framework if available
                System.err.println("[DataLogger] Failed to create log directory: " + logFileRootPath);
                return false; // Cannot write file if directory creation fails.
            }
        }

        // Construct the full path and filename for the new log file.
        String fileName = logFileRootPath + File.separator + logFilePrefix + "-" + logFilename + "-" + writerFileCount + ".txt";

        // Write the formatted data to the specified file.
        try {
            writeLogFile(fileContent, fileName);
            return true;
        } catch (IOException e) {
            // Error handling for writeLogFile is now centralized there,
            // but this catch is for any unexpected issue from writeLogFile itself if it were to throw something else
            // or if we add more logic here.
            // For now, writeLogFile handles its own logging of IOExceptions.
            return false;
        }
    }


    /**
     * Converts the data from {@link #timestampBuffer} and {@link #dataBuffer} into a 2D String array.
     * The first row of the array will contain timestamps, and subsequent rows will contain the corresponding data items.
     * Each column represents a single logged entry (timestamp + data array).
     * This method consumes the elements from the buffers (they become empty after this call).
     * <p>
     * Precondition: {@link #checkAndFixBufferSync()} should have been called to ensure buffers are synchronized and not empty.
     * </p>
     *
     * @return A 2D String array where {@code array[0][j]} is the j-th timestamp,
     *         and {@code array[i+1][j]} is the i-th element of the j-th data array.
     *         Returns an empty or minimally-sized array if buffers were unexpectedly empty,
     *         though {@code checkAndFixBufferSync} aims to prevent this.
     */
    private String[][] convertBuffersToStringArray() {
        // This method assumes buffers are synchronized and not empty, as per checkAndFixBufferSync().
        if (timestampBuffer.isEmpty() || dataBuffer.isEmpty() || dataBuffer.peek() == null) {
            // This case should ideally not be reached if checkAndFixBufferSync worked.
            // Return an empty array or handle error appropriately.
            return new String[0][0];
        }

        int numberOfEntries = timestampBuffer.size(); // Number of columns in the output
        // Determine number of rows: 1 for timestamps + number of elements in a typical data array.
        // Assumes all data arrays in the buffer have the same length as the first one peeked.
        int elementsPerDataArray = dataBuffer.peek().length;
        int numberOfRows = elementsPerDataArray + 1; // +1 for the timestamp row

        String[][] fileContent = new String[numberOfRows][numberOfEntries];

        // Populate the first row with timestamps
        for (int col = 0; col < numberOfEntries; col++) {
            fileContent[0][col] = timestampBuffer.remove(); // Consumes from timestampBuffer
        }

        // Populate subsequent rows with data from dataBuffer
        for (int col = 0; col < numberOfEntries; col++) {
            T[] dataEntry = dataBuffer.remove(); // Consumes from dataBuffer
            for (int i = 0; i < elementsPerDataArray; i++) {
                if (i < dataEntry.length && dataEntry[i] != null) {
                    fileContent[i + 1][col] = String.valueOf(dataEntry[i]);
                } else {
                    fileContent[i + 1][col] = ""; // Handle nulls or shorter-than-expected arrays gracefully
                }
            }
        }
        return fileContent;
    }

    /**
     * Checks if the timestamp and data buffers are synchronized (i.e., have the same size) and are not empty.
     * If they are out of sync, both buffers are cleared to prevent inconsistent data logging,
     * and an error message is printed to standard error.
     * <p>
     * This method is crucial before attempting to write buffer contents to a file.
     * It assumes it's called from a synchronized context.
     * </p>
     *
     * @return {@code true} if buffers are synchronized and not empty.
     *         {@code false} if buffers are empty or were found to be out of sync (and subsequently cleared).
     */
    private boolean checkAndFixBufferSync() {
        // This method assumes it's called from a synchronized context.
        if (timestampBuffer.isEmpty() || dataBuffer.isEmpty()) {
            // If either buffer is empty, there's no data to write or they are inherently desynced if one isn't.
            // If both are empty, it's a valid state meaning no data to write.
            if (!timestampBuffer.isEmpty() || !dataBuffer.isEmpty()) {
                // This means one is empty and the other is not - a desync state.
                timestampBuffer.clear();
                dataBuffer.clear();
                throw new IllegalStateException("[DataLogger] Buffer inconsistency: One buffer empty, the other not. Cleared both.");
            }
            return false; // Both buffers are empty, no data to write.
        }

        if (timestampBuffer.size() != dataBuffer.size()) {
            // Buffers are out of sync in terms of element count. This indicates a logical error.
            // Clear both to prevent writing potentially corrupted/misaligned data.
            int tsSize = timestampBuffer.size();
            int dbSize = dataBuffer.size();
            timestampBuffer.clear();
            dataBuffer.clear();
            throw new IllegalStateException(
                    "[DataLogger] Critical error: Buffer sizes do not match (timestamps: " + tsSize +
                    ", data: " + dbSize + "). Cleared both buffers to prevent data corruption.");
        }
        // Buffers are synchronized and not empty.
        return true;
    }

    /**
     * Writes the provided 2D array of {@code String} data to the specified file.
     * Each inner array represents a row, and elements are joined by {@link #logEntryDelimiter}.
     * Each row is terminated by a system-dependent line separator.
     * Increments {@link #writerFileCount} upon successful write.
     *
     * @param fileContent The 2D array of strings to write. Assumes first dimension is rows, second is columns.
     * @param fileName    The full path and name of the file to write to.
     * @return {@code true} if writing was successful.
     * @throws IOException if an I/O error occurs during file writing.
     */
    private boolean writeLogFile(String[][] fileContent, String fileName) throws IOException {
        // Check if there's any content to write. fileContent[0] might be null if fileContent is new String[x][0]
        if (fileContent.length == 0 || fileContent[0].length == 0) {
            // No actual data to write (e.g. buffer was full of empty arrays, or became empty before conversion)
            // This might be normal if buffer was cleared due to sync issues or contained no real entries.
            System.out.println("[DataLogger] No content to write to file: " + fileName);
            return false; // Or true, depending on whether "writing nothing" is a success. Let's say false.
        }

        int rows = fileContent.length;
        int cols = fileContent[0].length; // Assuming all rows in fileContent[0] (timestamps) have content if any row does.

        // Using try-with-resources for automatic resource management of FileWriter and BufferedWriter
        try (FileWriter fileWriter = new FileWriter(fileName);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    // Ensure that fileContent[r] is not null and c is within its bounds,
                    // especially if rows can have variable numbers of columns (though current design implies fixed cols per row).
                    if (fileContent[r] != null && c < fileContent[r].length && fileContent[r][c] != null) {
                        bufferedWriter.write(fileContent[r][c]);
                    } else {
                        // Write empty string if data is null or column doesn't exist for this specific row
                        // This helps prevent NullPointerExceptions if the 2D array is jagged or sparse.
                        // Current convertBuffersToStringArray() should produce a non-jagged array.
                    }
                    if (c < cols - 1) { // If this is not the last entry in the column for this row
                        bufferedWriter.write(logEntryDelimiter);
                    }
                }
                bufferedWriter.newLine(); // Use BufferedWriter's newLine for system-independent line separator
            }
            // No need to explicitly call bufferedWriter.close() or fileWriter.close() due to try-with-resources.
        } catch (IOException e) {
            // Log the error or notify the caller. Re-throwing allows caller to handle.
            System.err.println("[DataLogger] An error occurred while trying to write " + fileName);
            // e.printStackTrace(); // Avoid printStackTrace in library code, prefer logging or re-throwing.
            throw e; // Re-throw the exception to be handled by the caller
        }
        writerFileCount++;
        System.out.println("[DataLogger] Successfully wrote " + fileName + " with " + cols + " entries.");
        return true;
    }
}
