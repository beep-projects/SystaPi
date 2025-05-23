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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern; // Added import for Pattern.quote
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.freaklamarsch.systarest.DataLogger;
import de.freaklamarsch.systarest.DataLogger.DataLoggerStatus;

public class DataLoggerTest {

    @TempDir
    Path tempDir;

    DataLogger<Integer> logger;
    Integer[] dataArray = { -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    Path logPath;

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("E-dd.MM.yy-HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault());


    @BeforeEach
    void setUp() throws IOException {
        logger = new DataLogger<Integer>();
        logPath = tempDir.resolve("test_logs");
        Files.createDirectories(logPath);
        logger.setLogFileRootPath(logPath.toString());
    }

    @Test
    public void testConstructor() {
        // Logger is already initialized in setUp
        DataLoggerStatus dls = logger.getStatus();
        int expectedDefaultCapacity = 60; // Default capacity as per DataLogger.java
        try {
            Field defaultCapacityField = DataLogger.class.getDeclaredField("DEFAULT_CAPACITY");
            defaultCapacityField.setAccessible(true);
            // Access static field on the class, not an instance
            expectedDefaultCapacity = (int) defaultCapacityField.get(null);
        } catch (Exception e) {
            System.err.println("Could not reflectively access DEFAULT_CAPACITY, using hardcoded value 60 for test. Error: " + e.getMessage());
        }

        assertEquals(expectedDefaultCapacity, dls.capacity, "Default capacity should be " + expectedDefaultCapacity);
        assertFalse(dls.saveLoggedData, "saveLoggedData should be false by default");
        assertEquals("DataLogger", dls.logFilePrefix, "Default logFilePrefix should be 'DataLogger'");
        // logFileRootPath is now set by @BeforeEach, so we check that
        assertEquals(logPath.toString(), dls.logFileRootPath, "logFileRootPath should be set by setup");
        assertEquals(";", dls.logEntryDelimiter, "Default logEntryDelimiter should be ';'");
        assertEquals(0, dls.writerFileCount, "writerFileCount should be 0 initially");
        assertEquals(0, dls.bufferedEntries, "bufferedEntries should be 0 initially");
        assertEquals("never", dls.lastTimestamp, "lastTimestamp should be 'never' initially");
    }

    @Test
    void testSingleFileWriteOnCapacityOne() throws IOException {
        logger.saveLoggedData("singleEntry", ";", 1);
        long timestamp = Instant.now().toEpochMilli();
        logger.addData(dataArray.clone(), timestamp); // Clone to avoid modification issues if any

        List<Path> files = Files.list(logPath).collect(Collectors.toList());
        assertEquals(1, files.size(), "Should be one log file created");
        Path logFile = files.get(0);
        assertTrue(logFile.getFileName().toString().startsWith("singleEntry-"), "File prefix mismatch");
        assertTrue(logFile.getFileName().toString().endsWith("-0.txt"), "File suffix/count mismatch");
        
        List<Map.Entry<Long, Integer[]>> expectedEntries = List.of(Map.entry(timestamp, dataArray));
        verifyFileContent(logFile, ";", expectedEntries, dataArray.length);
    }

    @Test
    void testMultipleFileWritesAndBuffering() throws IOException {
        String prefix = "multiEntry";
        String delimiter = "#";
        int capacity = 3;
        logger.saveLoggedData(prefix, delimiter, capacity);

        List<Map.Entry<Long, Integer[]>> entriesForFile0 = new ArrayList<>();
        long ts = Instant.now().toEpochMilli();

        // Add 2 entries, should be buffered
        logger.addData(dataArray.clone(), ts);
        entriesForFile0.add(Map.entry(ts, dataArray.clone()));
        ts++;
        logger.addData(dataArray.clone(), ts);
        entriesForFile0.add(Map.entry(ts, dataArray.clone()));
        ts++;

        assertEquals(2, logger.getStatus().bufferedEntries, "Two entries should be buffered");
        assertEquals(0, logger.getStatus().writerFileCount, "No file should be written yet");

        // Add 1 more entry, should trigger file write
        logger.addData(dataArray.clone(), ts);
        entriesForFile0.add(Map.entry(ts, dataArray.clone()));
        ts++;

        assertEquals(0, logger.getStatus().bufferedEntries, "Buffer should be empty after write");
        assertEquals(1, logger.getStatus().writerFileCount, "One file should be written");
        Path file0 = logPath.resolve(prefix + "--0.txt");
        assertTrue(Files.exists(file0), "Log file " + file0.getFileName() + " should exist");
        verifyFileContent(file0, delimiter, entriesForFile0, dataArray.length);

        // Add 3 more entries, should trigger another file write
        List<Map.Entry<Long, Integer[]>> entriesForFile1 = new ArrayList<>();
        logger.addData(dataArray.clone(), ts);
        entriesForFile1.add(Map.entry(ts, dataArray.clone()));
        ts++;
        logger.addData(dataArray.clone(), ts);
        entriesForFile1.add(Map.entry(ts, dataArray.clone()));
        ts++;
        logger.addData(dataArray.clone(), ts);
        entriesForFile1.add(Map.entry(ts, dataArray.clone()));

        assertEquals(0, logger.getStatus().bufferedEntries, "Buffer should be empty after second write");
        assertEquals(2, logger.getStatus().writerFileCount, "Two files should be written in total");
        Path file1 = logPath.resolve(prefix + "--1.txt");
        assertTrue(Files.exists(file1), "Log file " + file1.getFileName() + " should exist");
        verifyFileContent(file1, delimiter, entriesForFile1, dataArray.length);
    }

    @Test
    void testStopSavingFlushesBuffer() throws IOException {
        String prefix = "flushTest";
        String delimiter = ",";
        int capacity = 2;
        logger.saveLoggedData(prefix, delimiter, capacity);
        
        long ts1 = Instant.now().toEpochMilli();
        Integer[] data1 = {1,2,3};
        logger.addData(data1, ts1);
        assertEquals(1, logger.getStatus().bufferedEntries, "One entry should be buffered");

        logger.stopSavingLoggedData(); // This should flush the buffer

        assertEquals(0, logger.getStatus().bufferedEntries, "Buffer should be empty after stopSavingLoggedData");
        assertEquals(1, logger.getStatus().writerFileCount, "One file should be written after stopSavingLoggedData");
        assertFalse(logger.getStatus().saveLoggedData, "saveLoggedData should be false after stop");

        Path logFile = logPath.resolve(prefix + "--0.txt");
        assertTrue(Files.exists(logFile), "Log file should exist after stopSavingLoggedData");
        verifyFileContent(logFile, delimiter, List.of(Map.entry(ts1, data1)), data1.length);

        // Add more data, should not write files
        logger.addData(new Integer[]{4,5,6}, Instant.now().toEpochMilli());
        logger.addData(new Integer[]{7,8,9}, Instant.now().toEpochMilli());
        logger.addData(new Integer[]{10,11,12}, Instant.now().toEpochMilli());
        
        assertEquals(capacity, logger.getStatus().bufferedEntries, "Buffer should fill up to capacity when logging is off");
        assertEquals(1, logger.getStatus().writerFileCount, "No new file should be written when logging is off");
    }
    
    @Test
    void testNoFileWrittenWhenLoggingIsOff() throws IOException {
        logger.setCapacity(2); // Default is saveLoggedData = false
        assertFalse(logger.getStatus().saveLoggedData, "Logging should be off by default after construction or setCapacity");

        logger.addData(new Integer[]{1,2}, Instant.now().toEpochMilli());
        logger.addData(new Integer[]{3,4}, Instant.now().toEpochMilli());
        logger.addData(new Integer[]{5,6}, Instant.now().toEpochMilli()); // This should overwrite first entry

        List<Path> files = Files.list(logPath).collect(Collectors.toList());
        assertEquals(0, files.size(), "No log files should be created when saveLoggedData is false");
        assertEquals(2, logger.getStatus().bufferedEntries, "Buffer should contain up to its capacity");
        assertEquals(0, logger.getStatus().writerFileCount, "Writer file count should be 0");
    }

    @Test
    void testReactivatingLoggingFlushesExistingBuffer() throws IOException {
        logger.setCapacity(3); // Logging off by default
        long ts1 = Instant.now().toEpochMilli();
        Integer[] entry1 = {10, 20};
        logger.addData(entry1, ts1);
        long ts2 = Instant.now().toEpochMilli() +1; // ensure different timestamp
        Integer[] entry2 = {30, 40};
        logger.addData(entry2, ts2);

        assertEquals(2, logger.getStatus().bufferedEntries, "Two entries should be buffered before reactivating logging");

        String prefix = "reactivateTest";
        logger.saveLoggedData(prefix, ";", 3); // This should flush the 2 existing entries

        assertEquals(0, logger.getStatus().bufferedEntries, "Buffer should be empty after reactivating and flushing");
        assertEquals(1, logger.getStatus().writerFileCount, "One file should be written upon reactivation");
        Path file0 = logPath.resolve(prefix + "--0.txt");
        assertTrue(Files.exists(file0), "Log file " + file0.getFileName() + " should exist after reactivation");
        verifyFileContent(file0, ";", List.of(Map.entry(ts1, entry1), Map.entry(ts2, entry2)), entry1.length);
        
        // Add more entries to trigger a new file
        long ts3 = Instant.now().toEpochMilli() + 2;
        Integer[] entry3 = {50, 60};
        logger.addData(entry3, ts3);
        assertEquals(1, logger.getStatus().bufferedEntries, "One entry buffered after adding one more");

        long ts4 = Instant.now().toEpochMilli() + 3;
        Integer[] entry4 = {70, 80};
        logger.addData(entry4, ts4);
        assertEquals(2, logger.getStatus().bufferedEntries, "Two entries buffered");
        
        long ts5 = Instant.now().toEpochMilli() + 4;
        Integer[] entry5 = {90, 100};
        logger.addData(entry5, ts5); // This fills capacity and writes file
        
        assertEquals(0, logger.getStatus().bufferedEntries, "Buffer should be empty after second write");
        assertEquals(2, logger.getStatus().writerFileCount, "Second file should be written");
        Path file1 = logPath.resolve(prefix + "--1.txt");
        assertTrue(Files.exists(file1), "Log file " + file1.getFileName() + " should exist");
        verifyFileContent(file1, ";", List.of(Map.entry(ts3, entry3), Map.entry(ts4, entry4), Map.entry(ts5, entry5)), entry3.length);
    }


    // Helper method to verify file content
    // Note: DataLogger writes timestamps in the first row, then data for entry1 in col1, data for entry2 in col2 etc.
    // Each subsequent row in the file corresponds to an element in the T[] array.
    private void verifyFileContent(Path filePath, String expectedDelimiter,
                                   List<Map.Entry<Long, Integer[]>> expectedEntriesInChronologicalOrder,
                                   int expectedArrayLength) throws IOException {
        assertTrue(Files.exists(filePath), "Log file " + filePath + " does not exist for verification.");
        List<String> lines = Files.readAllLines(filePath);

        assertFalse(lines.isEmpty(), "Log file should not be empty. File: " + filePath);
        assertEquals(expectedArrayLength + 1, lines.size(),
                     "Number of lines should be data array length + 1 (for timestamp header). File: " + filePath);

        // Header line contains timestamps
        String[] timestampsInFile;
        if (expectedDelimiter.isEmpty()) {
            // If delimiter is empty, split will produce array of chars. Treat as single concatenated string.
            timestampsInFile = new String[]{lines.get(0)};
            String expectedConcatenatedTimestamps = expectedEntriesInChronologicalOrder.stream()
                .map(entry -> DEFAULT_FORMATTER.format(Instant.ofEpochMilli(entry.getKey())))
                .collect(Collectors.joining());
            assertEquals(expectedConcatenatedTimestamps, timestampsInFile[0], "Concatenated timestamp mismatch for empty delimiter. File: " + filePath);
        } else {
            timestampsInFile = lines.get(0).split(Pattern.quote(expectedDelimiter));
            assertEquals(expectedEntriesInChronologicalOrder.size(), timestampsInFile.length,
                         "Number of timestamps in header should match expected entries. File: " + filePath);
            // Verify each timestamp
            for (int i = 0; i < expectedEntriesInChronologicalOrder.size(); i++) {
                String expectedFormattedTimestamp = DEFAULT_FORMATTER.format(Instant.ofEpochMilli(expectedEntriesInChronologicalOrder.get(i).getKey()));
                assertEquals(expectedFormattedTimestamp, timestampsInFile[i], "Timestamp mismatch at index " + i + ". File: " + filePath);
            }
        }


        // Verify data rows
        for (int dataArrayIndex = 0; dataArrayIndex < expectedArrayLength; dataArrayIndex++) {
            String[] dataValuesInLine;
            if (expectedDelimiter.isEmpty()) {
                dataValuesInLine = new String[]{lines.get(dataArrayIndex + 1)};
                 String expectedConcatenatedData = expectedEntriesInChronologicalOrder.stream()
                    .map(entry -> String.valueOf(entry.getValue()[dataArrayIndex]))
                    .collect(Collectors.joining());
                assertEquals(expectedConcatenatedData, dataValuesInLine[0],
                             "Concatenated data mismatch for empty delimiter at data row " + dataArrayIndex + ". File: " + filePath);

            } else {
                dataValuesInLine = lines.get(dataArrayIndex + 1).split(Pattern.quote(expectedDelimiter));
                assertEquals(expectedEntriesInChronologicalOrder.size(), dataValuesInLine.length,
                             "Number of data values in line " + (dataArrayIndex + 1) + " should match expected entries. File: " + filePath);
                for (int entryIndex = 0; entryIndex < expectedEntriesInChronologicalOrder.size(); entryIndex++) {
                    Integer expectedValue = expectedEntriesInChronologicalOrder.get(entryIndex).getValue()[dataArrayIndex];
                    assertEquals(String.valueOf(expectedValue), dataValuesInLine[entryIndex],
                                 "Data value mismatch at line " + (dataArrayIndex + 1) + ", entry " + entryIndex +
                                 " (0-indexed value in file). Expected " + expectedValue + ". File: " + filePath);
                }
            }
        }
    }

    @Test
    void testLogEntryDelimiterEdgeCases() throws IOException {
        // Test with empty delimiter
        String emptyDelimiterPrefix = "emptyDelimiterTest";
        logger.saveLoggedData(emptyDelimiterPrefix, "", 1); // Capacity 1 to force write on each add
        long ts1 = Instant.now().toEpochMilli();
        Integer[] data1 = {1, 2, 3};
        logger.addData(data1.clone(), ts1);

        Path logFileEmpty = logPath.resolve(emptyDelimiterPrefix + "--0.txt");
        assertTrue(Files.exists(logFileEmpty), "Log file for empty delimiter should exist.");
        verifyFileContent(logFileEmpty, "", List.of(Map.entry(ts1, data1)), data1.length);

        // Test with multi-character delimiter
        // Need a new logger instance or reset state for writerFileCount if using same logger
        setUp(); // Reset logger and logPath for a clean test environment
        String multiCharDelimiterPrefix = "multiCharDelimiterTest";
        String multiCharDelimiter = "--|--";
        logger.saveLoggedData(multiCharDelimiterPrefix, multiCharDelimiter, 1);
        long ts2 = Instant.now().toEpochMilli();
        Integer[] data2 = {4, 5, 6};
        logger.addData(data2.clone(), ts2);
        
        Path logFileMulti = logPath.resolve(multiCharDelimiterPrefix + "--0.txt");
        assertTrue(Files.exists(logFileMulti), "Log file for multi-char delimiter should exist.");
        verifyFileContent(logFileMulti, multiCharDelimiter, List.of(Map.entry(ts2, data2)), data2.length);
    }

    @Test
    void testTimestampLogic() throws IOException {
        // Verify duplicate timestamp prevention
        logger.saveLoggedData("tsTest", ";", 2); // Capacity 2
        long timestamp = Instant.now().toEpochMilli();
        Integer[] entryData1 = {1,2,3};
        Integer[] entryData2 = {4,5,6}; // Different data for clarity if needed

        logger.addData(entryData1.clone(), timestamp);
        assertEquals(1, logger.getStatus().bufferedEntries, "One entry should be buffered after first add.");

        logger.addData(entryData2.clone(), timestamp); // Add again with the *same* timestamp
        assertEquals(1, logger.getStatus().bufferedEntries, "Adding data with a duplicate timestamp should not increase buffered entries.");
        // Also check that the data in buffer is still the first one
        // This requires peeking into the buffer, which is not directly possible.
        // So, we rely on the next add with new timestamp to reveal what was kept.

        long newTimestamp = timestamp + 1000; // Ensure it's different
        logger.addData(entryData2.clone(), newTimestamp); // Add with a new timestamp
        
        assertEquals(0, logger.getStatus().bufferedEntries, "Buffer should flush after new timestamp as capacity (2) is met.");
        assertEquals(1, logger.getStatus().writerFileCount, "File should be written.");

        Path logFile = logPath.resolve("tsTest--0.txt");
        assertTrue(Files.exists(logFile), "Timestamp test log file should exist.");
        
        // Expected: only the first entry with 'timestamp' and the entry with 'newTimestamp'
        List<Map.Entry<Long, Integer[]>> expectedEntries = List.of(
            Map.entry(timestamp, entryData1), // The first data added with 'timestamp'
            Map.entry(newTimestamp, entryData2)  // The data added with 'newTimestamp'
        );
        verifyFileContent(logFile, ";", expectedEntries, entryData1.length);
    }

    @Test
    void testWriteOnEmptyBufferNoFileGenerated() throws IOException {
        String prefix = "emptyBufferWriteTest";
        logger.saveLoggedData(prefix, ";", 1); // Configure for saving

        // Explicitly do not add any data
        assertTrue(logger.getStatus().bufferedEntries == 0, "Buffer should be empty initially after saveLoggedData call.");

        logger.stopSavingLoggedData(); // This calls writeLoggedDataToFile internally

        assertEquals(0, logger.getStatus().writerFileCount, "No file should be written when stopping logging on an empty buffer.");
        
        List<Path> files;
        try (Stream<Path> stream = Files.list(logPath)) {
            files = stream.filter(p -> p.getFileName().toString().startsWith(prefix))
                          .collect(Collectors.toList());
        }
        assertEquals(0, files.size(), "No log file with prefix '" + prefix + "' should exist.");
    }
}
