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
 * A utility class for logging data entries represented as
 * {@code T[]} to delimited text files. The default delimiter used for this is
 * {@code ;}, making it a logger for CSV files. The {@code DataLogger} has a
 * defined capacity, which is the number of elements that can be stored.
 * Depending on the {@link #saveLoggedData} setting, adding new elements will
 * trigger {@link #writeLoggedDataToFile} and empty the {@link #dataBuffer} or
 * just overwrite the oldest element stored.
 */
public class DataLogger<T> {

	/**
	 * Inner class for representing the status of this @see DataLogger.
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
	private static final String DEFAULT_FILENAME = "";
	private static final String DEFAULT_ROOT_PATH = System.getProperty("user.home") + File.separator + "logs";
	private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("E-dd.MM.yy-HH:mm:ss.SSS")
			.withZone(ZoneId.systemDefault());
	private int capacity = DEFAULT_CAPACITY;
	private CircularBuffer<T[]> dataBuffer = null;
	private CircularBuffer<String> timestampBuffer = null;
	private boolean saveLoggedData = false;
	private String logFilePrefix = DEFAULT_PREFIX;
	private String logFilename = DEFAULT_FILENAME;
	private String logFileRootPath = DEFAULT_ROOT_PATH;
	private String logEntryDelimiter = DEFAULT_DELIMITER;
	private DateTimeFormatter timestampFormatter = DEFAULT_FORMATTER;
	private int writerFileCount = 0;

	/**
	 * Constructor for a DataLogger that writes one file per
	 * {@value #DEFAULT_CAPACITY} entries.
	 */
	public DataLogger() {
		this(DEFAULT_PREFIX, DEFAULT_FILENAME, DEFAULT_DELIMITER, DEFAULT_CAPACITY, DEFAULT_ROOT_PATH,
				DEFAULT_FORMATTER);
	}

	/**
	 * Constructor for a DataLogger that writes one file per {@code entriesPerFile}
	 * entries.
	 *
	 * @param prefix         the value to use for {@link #logFilePrefix}
	 * @param filename       the value to use for {@link #logFilename}
	 * @param delimiter      the value to use for {@link #logEntryDelimiter}
	 * @param entriesPerFile the value to use for {@link #capacity}
	 * @param rootPath       the value to use for {@link #logFileRootPath}
	 * @param rootPath       the formatter to use for {@link #timestampFormatter}
	 */
	public DataLogger(String prefix, String filename, String delimiter, int entriesPerFile, String rootPath,
			DateTimeFormatter formatter) {
		if (entriesPerFile > 0) {
			this.capacity = entriesPerFile;
		} else {
			this.capacity = DEFAULT_CAPACITY;
		}
		this.logFilename = filename;
		this.dataBuffer = new CircularBuffer<>(capacity);
		this.dataBuffer.setOverwrite(true);
		this.timestampBuffer = new CircularBuffer<>(capacity);
		this.timestampBuffer.setOverwrite(true);
		this.timestampFormatter = formatter;
	}

	/**
	 * @param delimiter the delimiter for the logged entries to set. The delimiter
	 *                  is set in {@link DataLogger#logEntryDelimiter}.
	 */
	public void setLogEntryDelimiter(String delimiter) {
		boolean wasLoggingRunning = stopLoggingAndWriteFileIfRunning();
		this.logEntryDelimiter = delimiter;
		saveLoggedData = wasLoggingRunning;
	}

	/**
	 * @return returns the status of the DataLogger. The status is composed of the
	 *         fields {@link DataLogger#capacity},
	 *         {@link DataLogger#saveLoggedData}, {@link DataLogger#logFilePrefix},
	 *         {@link DataLogger#logFileRootPath},
	 *         {@link DataLogger#writerFileCount}
	 */
	public DataLoggerStatus getStatus() {
		return new DataLoggerStatus(capacity, saveLoggedData, logFilePrefix, logFileRootPath, logEntryDelimiter,
				writerFileCount, timestampBuffer.size(), Objects.requireNonNullElse(timestampBuffer.end(), "never"));
	}

	/**
	 * @return the capacity
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * @param capacity the capacity to set
	 */
	public void setCapacity(int capacity) {
		boolean wasLoggingRunning = stopLoggingAndWriteFileIfRunning();
		this.capacity = capacity;
		this.dataBuffer = new CircularBuffer<>(capacity);
		this.dataBuffer.setOverwrite(true);
		this.timestampBuffer = new CircularBuffer<>(capacity);
		this.timestampBuffer.setOverwrite(true);
		saveLoggedData = wasLoggingRunning;
	}

	/**
	 * @return the logFilePrefix
	 */
	public String getLogFilePrefix() {
		return logFilePrefix;
	}

	/**
	 * @param logFilePrefix the logFilePrefix to set
	 */
	public void setLogFilePrefix(String logFilePrefix) {
		boolean wasLoggingRunning = stopLoggingAndWriteFileIfRunning();
		this.logFilePrefix = logFilePrefix;
		saveLoggedData = wasLoggingRunning;
	}

	/**
	 * @return the logFilename
	 */
	public String getLogFilename() {
		return logFilename;
	}

	/**
	 * @param logFilename the logFilename to set
	 */
	public void setLogFilename(String logFilename) {
		boolean wasLoggingRunning = stopLoggingAndWriteFileIfRunning();
		this.logFilename = logFilename;
		saveLoggedData = wasLoggingRunning;
	}

	/**
	 * @return the logFileRootPath
	 */
	public String getLogFileRootPath() {
		return logFileRootPath;
	}

	/**
	 * @param logFileRootPath the logFileRootPath to set
	 */
	public void setLogFileRootPath(String logFileRootPath) {
		boolean wasLoggingRunning = stopLoggingAndWriteFileIfRunning();
		this.logFileRootPath = logFileRootPath;
		saveLoggedData = wasLoggingRunning;
	}

	/**
	 * @return the writerFileCount
	 */
	public int getWriterFileCount() {
		return writerFileCount;
	}

	/**
	 * @param writerFileCount the writerFileCount to set
	 */
	public void setWriterFileCount(int writerFileCount) {
		this.writerFileCount = writerFileCount;
	}

	/**
	 * @return the logEntryDelimiter
	 */
	public String getLogEntryDelimiter() {
		return logEntryDelimiter;
	}

	/**
	 * Activates the saving of log files. For each {@link DataLogger#capacity}
	 * entries a new log file will be written.
	 */
	public void saveLoggedData() {
		stopLoggingAndWriteFileIfRunning();
		saveLoggedData = true;
	}

	/**
	 * checks if {@link DataLogger#saveLoggedData} is already {@code true}. If it is
	 * {@code true}, {@link DataLogger#stopSavingLoggedData} is called. If it is
	 * {@code false}, {@link DataLogger#writeLoggedDataToFile} is called.
	 *
	 * @return returns the initial value of {@link DataLogger#saveLoggedData}
	 */
	private boolean stopLoggingAndWriteFileIfRunning() {
		if (saveLoggedData) {
			stopSavingLoggedData();
			return true;
		} else {
			writeLoggedDataToFile();
			return false;
		}
	}

	/**
	 * Activates the saving of log files. For each {@code entriesPerFile} entries a
	 * new log file will be written. If {@link DataLogger#saveLoggedData} is already
	 * {@code true}, the current logging will be stopped, causing all recorded
	 * entries to be written to a log file, before the new logging is started.
	 *
	 * @param entriesPerFile this is the number of entries contained in each written
	 *                       log file. {@link DataLogger#capacity} will be set to
	 *                       this value
	 */
	public synchronized void saveLoggedData(int entriesPerFile) {
		stopLoggingAndWriteFileIfRunning();
		if (entriesPerFile > 0) {
			this.capacity = entriesPerFile;
		} else {
			this.capacity = DEFAULT_CAPACITY;
		}
		this.dataBuffer = new CircularBuffer<>(capacity);
		this.dataBuffer.setOverwrite(true);
		this.timestampBuffer = new CircularBuffer<>(capacity);
		this.timestampBuffer.setOverwrite(true);
		saveLoggedData();
	}

	/**
	 * activate the saving of logged data and set the {@link #logFilePrefix}
	 *
	 * @param filePrefix the value to use for {@link #logFilePrefix}
	 */
	public void saveLoggedData(String filePrefix) {
		logFilePrefix = filePrefix;
		saveLoggedData();
	}

	/**
	 * activate the saving of logged data and set the {@link #logFilePrefix},
	 * {@link #logEntryDelimiter}, {@link #capacity}.
	 *
	 * @param filePrefix     the value to use for {@link #logFilePrefix}
	 * @param delimiter      the value to use for {@link #logEntryDelimiter}
	 * @param entriesPerFile the value to use for {@link #capacity}
	 */
	public void saveLoggedData(String filePrefix, String delimiter, int entriesPerFile) {
		logFilePrefix = filePrefix;
		logEntryDelimiter = delimiter;
		saveLoggedData(entriesPerFile);
	}

	/**
	 * if data is currently saved to a file, this command will write the last file
	 * and stop the saving of files. This will clear the {@code DataLogger}.
	 */
	public void stopSavingLoggedData() {
		if (saveLoggedData) {
			saveLoggedData = false;
			// write out the last file
			writeLoggedDataToFile();
		}
	}

	/**
	 * add T[] data and its timestamp to dataBuffer, respectively timestampBuffer
	 *
	 * @param data      the T[] that should be added to the dataBuffer
	 * @param timestamp the timestamp for the added data. It will be added to
	 *                  timestampBuffer
	 */
	public synchronized void addData(T[] data, long timestamp) {
		// access to dataBuffer and timestampBuffer has to be synchronized
		// make sure that there is new data to write
		String newTimestamp = timestampFormatter.format(Instant.ofEpochMilli(timestamp));
		String lastTimestamp = timestampBuffer.end();
		if (lastTimestamp != null && newTimestamp.equals(lastTimestamp)) {
			// check if there is already data in the buffer (lastTimestamp!=null)
			// and make sure the timestamp is updated
			// if no new data is available, just return
			return;
		}
		// save new values
		timestampBuffer.add(newTimestamp);
		// save a shallow copy of data, otherwise the stored array will change when the
		// outside
		// array changes
		T[] d = Arrays.copyOf(data, data.length);
		dataBuffer.add(d);
		if (timestampBuffer.isFull() && saveLoggedData) {
			writeLoggedDataToFile();
		}
	}

	private synchronized boolean writeLoggedDataToFile() {
		if (checkAndFixBufferSync() == false) {
			return false;
		}
		String[][] fileContent = convertBuffersToStringArray();
		// make sure the log dir exists
		File path = new File(logFileRootPath);
		if (!path.exists()) {
			path.mkdirs();
		}
		String fileName = logFileRootPath + File.separator + logFilePrefix + "-" + logFilename + "-" + writerFileCount
				+ ".txt";
		return writeLogFile(fileContent, fileName);
	}

	/**
	 * @return
	 */
	private String[][] convertBuffersToStringArray() {
		int cols = timestampBuffer.size();
		int rows = dataBuffer.peek().length + 1; // +1 because first row will be timestamps
		String[][] fileContent = new String[rows][cols];
		int r = 0; // begin in first row/line
		int c = 0; // begin in first column
		// insert the timestamps into the first row/line
		while (!timestampBuffer.isEmpty()) {
			fileContent[r][c] = timestampBuffer.remove();
			c++;
		}
		r = 1;// first row/line filled
		c = 0;// begin in first column
		// add the data records column by column to the fileContent
		while (!dataBuffer.isEmpty()) {
			T[] entry = dataBuffer.remove();
			for (T e : entry) {
				if (e == null) {
					fileContent[r][c] = "";
				} else {
					fileContent[r][c] = "" + e;
				}
				r++;
			}
			c++; // move to next column for next record
			r = 1; // keep in mind, that the first column is already filled with the timestamps
		}
		return fileContent;
	}

	/**
	 * @return returns true, if buffers are ok, to work with. Returns false, if
	 *         buffers are empty, or out of sync.
	 */
	private boolean checkAndFixBufferSync() {
		// access to dataBuffer and timestampBuffer has to be synchronized
		if (timestampBuffer.isEmpty() || dataBuffer.isEmpty()) {
			return false;
		} else if (timestampBuffer.size() != dataBuffer.size()) {
			System.out.println(
					"[DataLogger] writeLoggedDataToFile: buffer sizes don't match, clearing buffers for recovery.");
			timestampBuffer.clear();
			dataBuffer.clear();
			return false;
		}
		return true;
	}

	/**
	 * @param fileContent
	 * @param fileName
	 * @return true if writing the file was successful, false otherwise
	 */
	private boolean writeLogFile(String[][] fileContent, String fileName) {
		int rows = fileContent.length;
		int cols = fileContent[0].length;
		try {
			FileWriter myWriter = new FileWriter(fileName);
			BufferedWriter bufferedWriter = new BufferedWriter(myWriter);
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					bufferedWriter.write(fileContent[r][c]);
					if (c < cols - 1) { // if this is not the last entry in the row, add the logEntryDelimiter
						bufferedWriter.write(logEntryDelimiter);
					}
				}
				// add a new line
				bufferedWriter.write(System.lineSeparator());
			}
			bufferedWriter.close();
			myWriter.close();
			writerFileCount++;
			System.out.println("[DataLogger] wrote " + fileName);
		} catch (IOException e) {
			System.out.println("[DataLogger] An error occurred while trying to write " + fileName);
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
