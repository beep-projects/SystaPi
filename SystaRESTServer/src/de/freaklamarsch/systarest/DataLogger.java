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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * A {@code DataLogger} can be used for logging data entries represented as
 * {@code T[]} to delimited text files. The default delimiter used for this is
 * {@code ;}, making it a logger for CSV files. The {@code DataLogger} has a
 * defined capacity, which is the number of elements that can be stored.
 * Depending on the {@link #saveLoggedData} setting adding new elements will
 * trigger {@link #writeLoggedDataToFile} and empty the {@link #dataBuffer} or
 * just overwrite the oldest element stored.
 */
public class DataLogger<T> {

	/**
	 * Inner class for representing the status of this @see DataLogger.
	 */
	public class DataLoggerStatus {
		public final int capacity;
		public final boolean saveLoggedData;
		public final String logFilePrefix;
		public final String logFileRootPath;
		public final int writerFileCount;
		public final String logEntryDelimiter;
		public final int bufferedEntries;

		public DataLoggerStatus(int capacity, boolean saveLoggedData, String logFilePrefix, String logFileRootPath,
				String logEntryDelimiter, int writerFileCount, int bufferedEntries) {
			this.capacity = capacity;
			this.saveLoggedData = saveLoggedData;
			this.logFilePrefix = logFilePrefix;
			this.logFileRootPath = logFileRootPath;
			this.logEntryDelimiter = logEntryDelimiter;
			this.writerFileCount = writerFileCount;
			this.bufferedEntries = bufferedEntries;
		}
	}

	private static final int DEFAULT_CAPACITY = 60;
	private int capacity = DEFAULT_CAPACITY;
	private CircularBuffer<T[]> dataBuffer = null;
	private CircularBuffer<String> timestampBuffer = null;
	private boolean saveLoggedData = false;
	private String logFilePrefix = "DataLogger";
	private String logFilename = "";
	// private String logFileRootPath =
	// DataLogger.class.getClassLoader().getResource("").getPath();
	private String logFileRootPath = System.getProperty("user.home") + File.separator + "logs";
	private String logEntryDelimiter = ";";
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E-dd.MM.yy-HH:mm:ss");
	private int writerFileCount = 0;

	/**
	 * Constructor for a DataLogger that writes one file per
	 * {@value #DEFAULT_CAPACITY} entries.
	 */
	public DataLogger() {
		this("", DEFAULT_CAPACITY);
	}

	/**
	 * Constructor for a DataLogger that writes one file per {@code entriesPerFile}
	 * entries.
	 *
	 * @param entriesPerFile this is the number of entries contained in each written
	 *                       log file.
	 */
	public DataLogger(String filename, int entriesPerFile) {
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
	}

	/**
	 * @param delimiter the delimiter for the logged entries to set. The delimiter
	 *                  is set in {@link DataLogger#logEntryDelimiter}.
	 */
	public void setLogEntryDelimiter(String delimiter) {
		this.logEntryDelimiter = delimiter;
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
				writerFileCount, timestampBuffer.size());
	}

	/**
	 * Activates the saving of log files. For each {@link DataLogger#capacity}
	 * entries a new log file will be written.
	 */
	public void saveLoggedData() {
		if (saveLoggedData) {
			stopSavingLoggedData();
		} else {
			writeLoggedDataToFile();
		}
		saveLoggedData = true;
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
		// access to dataBuffer and timestampBuffer has to be synchronized
		if (saveLoggedData) {
			stopSavingLoggedData();
		} else {
			writeLoggedDataToFile();
		}
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
		String newTimestamp = formatter.format(LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC));
		String lastTimestamp = timestampBuffer.peek();
		if (lastTimestamp != null && newTimestamp.equals(lastTimestamp)) {
			// check if there is already data in the buffer (lastTimestamp!=null)
			// and make sure the timestamp is updated
			// if no new data is available, so just return
			return;
		}
		// save new values
		timestampBuffer.add(newTimestamp);
		// save a copy of data, otherwise the stored array will change when the outside
		// array changes
		T[] d = Arrays.copyOf(data, data.length);
		dataBuffer.add(d);

		if (timestampBuffer.isFull() && saveLoggedData) {
			writeLoggedDataToFile();
		}
	}

	private synchronized boolean writeLoggedDataToFile() {
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
			// System.out.println("add next entry");
			for (T e : entry) {
				// System.out.println("R: "+r+", C: "+c+", E: "+e);
				// fileContent[r][c] = Integer.toString(e);
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
		// make sure the log dir exists
		File path = new File(logFileRootPath);
		if (!path.exists()) {
			path.mkdirs();
		}
		String fileName = logFileRootPath + File.separator + logFilePrefix + "-" + logFilename + "-" + writerFileCount
				+ ".txt";
		try {
			FileWriter myWriter = new FileWriter(fileName);
			BufferedWriter bufferedWriter = new BufferedWriter(myWriter);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
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
			System.out.println("[DataLogger] Successfully wrote to " + fileName);
		} catch (IOException e) {
			System.out.println("[DataLogger] An error occurred while trying to write " + fileName);
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
