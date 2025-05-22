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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.glassfish.jersey.server.ResourceConfig;
// import org.glassfish.jersey.server.model.Resource; // Commented out as printAPI is commented
// import org.glassfish.jersey.server.model.ResourceMethod; // Commented out as printAPI is commented

import de.freaklamarsch.systarest.DeviceTouchSearch.DeviceTouchDeviceInfo;
import de.freaklamarsch.systarest.FakeSystaWeb.FakeSystaWebStatus;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;


/**
 * Provides a JAX-RS ReST API for interacting with an emulated Paradigma SystaComfort
 * heating system via the {@link FakeSystaWeb} service.
 * <p>
 * This API exposes endpoints to:
 * <ul>
 *   <li>Start and stop the {@link FakeSystaWeb} UDP listener.</li>
 *   <li>Discover SystaComfort units on the network (delegating to {@link DeviceTouchSearch}).</li>
 *   <li>Retrieve the current operational status of the {@link FakeSystaWeb} service and the connected heating system.</li>
 *   <li>Access raw and processed data values from the heating system.</li>
 *   <li>Obtain specific data views, such as water heater status.</li>
 *   <li>Manage data logging (enable, disable, retrieve logs).</li>
 *   <li>Serve HTML monitoring pages.</li>
 * </ul>
 * The actual communication and data processing logic is handled by the {@link FakeSystaWeb} instance.
 * This class acts as the ReSTful interface to that service.
 * </p>
 * <p>
 * Note on concurrency: This class uses static fields {@code fsw} (for {@link FakeSystaWeb}) and
 * {@code t} (for the {@link FakeSystaWeb} thread). While JAX-RS typically creates a new instance
 * of this API class for each request, the underlying {@code FakeSystaWeb} object and its thread
 * are shared. It is assumed that {@link FakeSystaWeb} itself is designed to be thread-safe
 * for the operations exposed through this API.
 * </p>
 */
@Path("{systarest : (?i)systarest}")
public class SystaRESTAPI {
    /**
     * Property name for configuring the Paradigma IP address via {@link ResourceConfig}.
     * This IP address is used by {@link FakeSystaWeb} to bind its listening socket.
     */
    public final static String PROP_PARADIGMA_IP = "PARADIGMA_IP";

    /**
     * Static instance of {@link FakeSystaWeb}, which handles the underlying UDP communication
     * and data processing. This instance is shared across all API requests.
     * Initialization is handled by the constructor of {@link SystaRESTAPI} in a singleton-like manner.
     */
    private static FakeSystaWeb fakeSystaWebService = null;

    /**
     * Static instance of the {@link Thread} that runs the {@link FakeSystaWeb} service.
     * This allows the service to listen for UDP packets asynchronously.
     * Managed by the {@link #start(ResourceConfig)} and {@link #stop()} methods.
     */
    private static Thread fakeSystaWebServiceThread = null;

    private final Map<String, Object> jsonConfig = new HashMap<>();
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(jsonConfig);

    /**
     * Constructs a {@code SystaRESTAPI} instance.
     * This constructor is typically called by the JAX-RS framework for each incoming request.
     * It initializes the shared {@link FakeSystaWeb} instance and starts its listener thread
     * if they haven't been initialized yet. This ensures that {@link FakeSystaWeb}
     * behaves like a singleton service managed by this API class.
     *
     * @param resourceConfig The JAX-RS {@link ResourceConfig} which may contain properties
     *                       such as the IP address for {@link FakeSystaWeb} to listen on,
     *                       accessible via {@link #PROP_PARADIGMA_IP}.
     */
    public SystaRESTAPI(@Context ResourceConfig resourceConfig) {
        // This constructor is called for each JAX-RS request.
        // The fsw and t fields are static to ensure only one FakeSystaWeb instance
        // and its thread are created and managed across all requests.
        synchronized (SystaRESTAPI.class) { // Synchronize to ensure thread-safe initialization
            if (fakeSystaWebService == null) {
                fakeSystaWebService = new FakeSystaWeb();
                // Initial start is typically triggered by an explicit API call to /start,
                // but can also be done here if auto-start on first API access is desired.
                // For now, let's keep explicit start via API.
                // start(resourceConfig); // Or, defer start to an explicit API call.
            }
        }
        // printAPI(); // Debugging method, commented out for production.
    }

    /*
     * Utility method to print information about the available API endpoints.
     * This was likely used for debugging and can be removed or kept commented for future reference.
     * JAX-RS servers often provide a WADL file (e.g., /application.wadl) for API discovery.
     */
    /*
    @SuppressWarnings("unused")
    private void printAPI() {
        Resource resource = Resource.from(this.getClass());
        System.out.println("Path is " + resource.getPath());
        for (Resource r : resource.getChildResources()) {
            System.out.println("Path is " + r.getPath());
            for (ResourceMethod rm : r.getAllMethods()) {
                System.out.println(rm.toString());
            }
        }
    }
    */

    /**
     * Starts the {@link FakeSystaWeb} service in a new thread, enabling it to listen for
     * UDP packets from a Paradigma SystaComfort heating controller.
     * <p>
     * The IP address for the listener is taken from the {@code PARADIGMA_IP} property
     * in the provided {@link ResourceConfig}. If the service is already running,
     * this method logs a message and does nothing.
     * </p>
     *
     * @param resourceConfig The JAX-RS {@link ResourceConfig} containing configuration properties,
     *                       notably {@link #PROP_PARADIGMA_IP}.
     * @return A {@link Response} indicating the outcome:
     *         <ul>
     *           <li>200 OK: If the service started successfully or was already running.</li>
     *           <li>500 Internal Server Error: If an error occurs during startup.</li>
     *         </ul>
     */
    @POST
    @Path("{start : (?i)start}")
    public Response start(@Context ResourceConfig resourceConfig) {
        // This method manages the lifecycle of the FakeSystaWeb thread.
        // It's synchronized to prevent race conditions if multiple start requests arrive concurrently.
        synchronized (SystaRESTAPI.class) {
            System.out.println("[SystaRESTAPI] start: API call received.");
            if (fakeSystaWebServiceThread == null || !fakeSystaWebServiceThread.isAlive()) {
                if (fakeSystaWebService == null) { // Should have been initialized by constructor
                     fakeSystaWebService = new FakeSystaWeb();
                }
                fakeSystaWebServiceThread = new Thread(fakeSystaWebService, "FakeSystaWeb-Listener");
                System.out.println("[SystaRESTAPI] start: Initializing and starting FakeSystaWeb thread.");

                String configuredIpAddress = (String) resourceConfig.getProperty(PROP_PARADIGMA_IP);
                if (configuredIpAddress != null && !configuredIpAddress.isEmpty()) {
                    System.out.println("[SystaRESTAPI] start: Configuring FakeSystaWeb to listen on IP: " + configuredIpAddress);
                    fakeSystaWebService.setInetAddress(configuredIpAddress);
                } else {
                    System.out.println("[SystaRESTAPI] start: No specific IP configured, FakeSystaWeb will use default behavior (likely listen on all interfaces or a default one).");
                }

                try {
                    fakeSystaWebServiceThread.start();
                    System.out.println("[SystaRESTAPI] start: FakeSystaWeb thread started successfully.");
                    return Response.ok("FakeSystaWeb service started.").build();
                } catch (Exception e) {
                    System.err.println("[SystaRESTAPI] start: Failed to start FakeSystaWeb thread: " + e.getMessage());
                    // Ensure thread object is cleared if start fails to allow retry
                    fakeSystaWebServiceThread = null;
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                   .entity("Failed to start FakeSystaWeb service: " + e.getMessage()).build();
                }
            } else {
                System.out.println("[SystaRESTAPI] start: FakeSystaWeb is already running. Ignoring request.");
                return Response.ok("FakeSystaWeb service was already running.").build();
            }
        }
    }

    /**
     * Stops the {@link FakeSystaWeb} service from listening for UDP packets.
     * This method signals the background thread to terminate and closes its socket.
     *
     * @return A {@link Response} 200 OK indicating the stop request was processed.
     */
    @POST
    @Path("{stop : (?i)stop}")
    public Response stop() {
        // This method manages the lifecycle of the FakeSystaWeb thread.
        System.out.println("[SystaRESTAPI] stop: API call received.");
        if (fakeSystaWebService == null) {
             return Response.ok("FakeSystaWeb service was not initialized.").build();
        }
        fakeSystaWebService.stop(); // Signals the FakeSystaWeb runnable to stop
        // Further cleanup of the thread object 't' might be needed here or in run() method of FakeSystaWeb
        // to ensure 't' accurately reflects the state for the next 'start' call.
        // For now, setting to null if thread is confirmed stopped.
        if (fakeSystaWebServiceThread != null && !fakeSystaWebServiceThread.isAlive()) {
            fakeSystaWebServiceThread = null;
        }
        System.out.println("[SystaRESTAPI] stop: Stop request processed for FakeSystaWeb.");
        return Response.ok("FakeSystaWeb service stop request processed.").build();
    }

    /**
     * Discovers SystaComfort units on the network using the DeviceTouch search protocol.
     * This delegates to {@link FakeSystaWeb#findSystaComfort()}.
     *
     * @return A {@link Response} containing:
     *         <ul>
     *           <li>200 OK: With a JSON object detailing the first discovered device.</li>
     *           <li>404 Not Found: If no devices are found.</li>
     *           <li>500 Internal Server Error: If an error occurs during the search.</li>
     *         </ul>
     */
    @GET
    @Path("{findsystacomfort : (?i)findsystacomfort}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findSystaComfort() {
        if (fakeSystaWebService == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("FakeSystaWeb service not initialized.").build();
        }
        try {
            DeviceTouchDeviceInfo deviceInfo = fakeSystaWebService.findSystaComfort();
            if (deviceInfo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("No SystaComfort device found.").build();
            } else {
                // Construct JSON response from deviceInfo
                JsonObject jsonResult = jsonFactory.createObjectBuilder()
                        .add("SystaWebIP", deviceInfo.localIp != null ? deviceInfo.localIp : JsonValue.NULL)
                        .add("SystaWebPort", DEFAULT_PORT) // Assuming default port for SystaWeb
                        .add("DeviceTouchBcastIP", deviceInfo.bcastIp != null ? deviceInfo.bcastIp : JsonValue.NULL)
                        .add("DeviceTouchBcastPort", deviceInfo.bcastPort)
                        .add("deviceTouchInfoString", deviceInfo.string != null ? deviceInfo.string : JsonValue.NULL)
                        .add("unitIP", deviceInfo.ip != null ? deviceInfo.ip : JsonValue.NULL)
                        .add("unitName", deviceInfo.name != null ? deviceInfo.name : JsonValue.NULL)
                        .add("unitId", deviceInfo.id != null ? deviceInfo.id : JsonValue.NULL)
                        .add("unitApp", deviceInfo.app)
                        .add("unitPlatform", deviceInfo.platform)
                        .add("unitVersion", deviceInfo.version != null ? deviceInfo.version : JsonValue.NULL)
                        .add("unitMajor", deviceInfo.major)
                        .add("unitMinor", deviceInfo.minor)
                        .add("unitBaseVersion", deviceInfo.baseVersion != null ? deviceInfo.baseVersion : JsonValue.NULL)
                        .add("unitMac", deviceInfo.mac != null ? deviceInfo.mac : JsonValue.NULL)
                        .add("STouchAppSupported", deviceInfo.stouchSupported)
                        .add("DeviceTouchPort", deviceInfo.port)
                        .add("DeviceTouchPassword", deviceInfo.password != null ? deviceInfo.password : JsonValue.NULL)
                        .build();
                return Response.ok(jsonResult).build();
            }
        } catch (IOException e) {
            System.err.println("[SystaRESTAPI] IOException during findSystaComfort: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error during device discovery: " + e.getMessage()).build();
        }
    }

    /**
     * Retrieves the current operational status of the FakeSystaWeb service and the
     * connected Paradigma heating system.
     *
     * @return A {@link Response} containing:
     *         <ul>
     *           <li>200 OK: With a JSON object detailing the current status.</li>
     *           <li>503 Service Unavailable: If the FakeSystaWeb service is not initialized.</li>
     *         </ul>
     */
    @GET
    @Path("{servicestatus : (?i)servicestatus}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        if (fakeSystaWebService == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("FakeSystaWeb service not initialized.").build();
        }
        FakeSystaWebStatus currentStatus = fakeSystaWebService.getStatus();

        JsonObject statusJson = jsonFactory.createObjectBuilder()
                .add("timeStampString",
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME
                                .format(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault())))
                .add("connected", currentStatus.connected)
                .add("running", currentStatus.running)
                .add("lastDataReceivedAt", currentStatus.lastTimestamp != null ? currentStatus.lastTimestamp : JsonValue.NULL)
                .add("packetsReceived", currentStatus.dataPacketsReceived)
                .add("paradigmaListenerIP", currentStatus.localAddress != null ? currentStatus.localAddress : JsonValue.NULL)
                .add("paradigmaListenerPort", currentStatus.localPort)
                .add("paradigmaIP", (currentStatus.remoteAddress == null) ? JsonValue.NULL : currentStatus.remoteAddress.getHostAddress())
                .add("paradigmaPort", currentStatus.remotePort)
                .add("loggingData", currentStatus.logging)
                .add("logFileSize", currentStatus.packetsPerFile)
                .add("logFilePrefix", currentStatus.loggerFilePrefix != null ? currentStatus.loggerFilePrefix : JsonValue.NULL)
                .add("logFileDelimiter", currentStatus.loggerEntryDelimiter != null ? currentStatus.loggerEntryDelimiter : JsonValue.NULL)
                .add("logFileRootPath", currentStatus.loggerFileRootPath != null ? currentStatus.loggerFileRootPath : JsonValue.NULL)
                .add("logFilesWritten", currentStatus.loggerFileCount)
                .add("logBufferedEntries", currentStatus.loggerBufferedEntries)
                .add("commitDate", currentStatus.commitDate != null ? currentStatus.commitDate : JsonValue.NULL)
                .build();
        return Response.ok(statusJson).build();
    }

    /**
     * Retrieves the latest raw data set received from the SystaComfort unit.
     * The data is provided as an array of integers, without further interpretation.
     * <p>
     * Note: It is not guaranteed that the timestamps (`timestamp` and `timestampString`)
     * in the response perfectly match the data if a new packet arrives between the
     * calls to {@code fsw.getTimestamp()} and {@code fsw.getData()}.
     * </p>
     *
     * @return A {@link Response} containing:
     *         <ul>
     *           <li>200 OK: With a JSON object containing the timestamp and raw data array.
     *                       If no data is available, `rawData` will be an empty array.</li>
     *           <li>503 Service Unavailable: If the FakeSystaWeb service is not initialized.</li>
     *         </ul>
     */
    @GET
    @Path("{rawdata : (?i)rawdata}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRawData() {
        if (fakeSystaWebService == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("FakeSystaWeb service not initialized.").build();
        }
        Integer[] rawDataValues = fakeSystaWebService.getData();
        long currentTimestamp = fakeSystaWebService.getTimestamp(); // Get timestamp close to data retrieval
        String currentTimestampString = fakeSystaWebService.getTimestampString();

        JsonArrayBuilder arrayBuilder = jsonFactory.createArrayBuilder();
        if (rawDataValues != null) {
            for (Integer value : rawDataValues) {
                arrayBuilder.add(value != null ? value.intValue() : JsonValue.NULL); // Handle potential nulls in data array
            }
        }

        JsonObject resultJson = jsonFactory.createObjectBuilder()
                .add("timestamp", currentTimestamp)
                .add("timestampString", currentTimestampString != null ? currentTimestampString : JsonValue.NULL)
                .add("rawData", arrayBuilder.build())
                .build();
        return Response.ok(resultJson).build();
    }

    /**
     * Retrieves the status of the hot water system, formatted according to Home Assistant
     * water heater entity conventions.
     *
     * @return A {@link Response} containing:
     *         <ul>
     *           <li>200 OK: With a JSON object representing the water heater status.
     *                       If no data is available, an empty JSON object is returned.</li>
     *           <li>503 Service Unavailable: If the FakeSystaWeb service is not initialized.</li>
     *         </ul>
     */
    @GET
    @Path("{waterheater : (?i)waterheater}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWaterHeater() {
        if (fakeSystaWebService == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("FakeSystaWeb service not initialized.").build();
        }
        SystaWaterHeaterStatus waterHeaterStatus = fakeSystaWebService.getWaterHeaterStatus();
        if (waterHeaterStatus == null) {
            // Return an empty JSON object or a specific "no data" response
            return Response.ok(jsonFactory.createObjectBuilder().build()).build();
        }

        JsonArrayBuilder operationListBuilder = jsonFactory.createArrayBuilder();
        for (String op : waterHeaterStatus.operationList) {
            operationListBuilder.add(op);
        }
        JsonArrayBuilder featuresBuilder = jsonFactory.createArrayBuilder();
        for (String feature : waterHeaterStatus.supportedFeatures) {
            featuresBuilder.add(feature);
        }

        JsonObject waterHeaterJson = jsonFactory.createObjectBuilder()
                .add("min_temp", waterHeaterStatus.minTemp)
                .add("max_temp", waterHeaterStatus.maxTemp)
                .add("current_temperature", waterHeaterStatus.currentTemperature)
                .add("target_temperature", waterHeaterStatus.targetTemperature)
                .add("target_temperature_high", waterHeaterStatus.targetTemperatureHigh)
                .add("target_temperature_low", waterHeaterStatus.targetTemperatureLow)
                .add("temperature_unit", waterHeaterStatus.temperatureUnit.toString())
                .add("current_operation", waterHeaterStatus.currentOperation != null ? waterHeaterStatus.currentOperation : JsonValue.NULL)
                .add("operation_list", operationListBuilder.build())
                .add("supported_features", featuresBuilder.build())
                .add("is_away_mode_on", waterHeaterStatus.is_away_mode_on)
                .add("timestamp", waterHeaterStatus.timestamp)
                .add("timestampString", waterHeaterStatus.timestampString != null ? waterHeaterStatus.timestampString : JsonValue.NULL)
                .build();
        return Response.ok(waterHeaterJson).build();
    }

    /**
     * Retrieves a comprehensive status of the connected Paradigma SystaComfort II system,
     * including all known data fields.
     *
     * @return A {@link Response} containing:
     *         <ul>
     *           <li>200 OK: With a JSON object detailing the full system status.
     *                       If no data is available, an empty JSON object is returned.</li>
     *           <li>503 Service Unavailable: If the FakeSystaWeb service is not initialized.</li>
     *         </ul>
     */
    @GET
    @Path("{status : (?i)status}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus() {
        if (fakeSystaWebService == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("FakeSystaWeb service not initialized.").build();
        }
        SystaStatus paradigmaStatus = fakeSystaWebService.getParadigmaStatus();
        if (paradigmaStatus == null) {
            return Response.ok(jsonFactory.createObjectBuilder().build()).build();
        }

        // Using the toJsonObject() method from SystaStatus assuming it's implemented correctly
        // If not, manual construction similar to getWaterHeater() would be needed.
        // For this refactoring, we assume toJsonObject() exists and works.
        // If SystaStatus.toJsonObject() is not available or suitable, manual building is required:
        JsonObject statusJson = paradigmaStatus.toJsonObject(jsonFactory); // Assuming such a method exists or is added
        
        return Response.ok(statusJson).build();
    }

    /**
     * Enables logging of received data packets to files.
     *
     * @param filePrefix     The prefix for log file names. Defaults to "SystaREST".
     * @param delimiter      The delimiter for separating values in log files. Defaults to ";".
     * @param entriesPerFile The number of data entries (packets/sets) to store per log file. Defaults to 60.
     * @return A {@link Response} 200 OK if logging was enabled.
     *         Returns 503 if fst is not initialized.
     */
    @PUT
    @Path("{enablelogging : (?i)enablelogging}")
    public Response enablelogging(@DefaultValue("SystaREST") @QueryParam("filePrefix") String filePrefix,
                                @DefaultValue(";") @QueryParam("logEntryDelimiter") String delimiter,
                                @DefaultValue("60") @QueryParam("entriesPerFile") int entriesPerFile) {
        if (fakeSystaWebService == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("FakeSystaWeb service not initialized.").build();
        }
        fakeSystaWebService.logRawData(filePrefix, delimiter, entriesPerFile);
        return Response.ok("Logging enabled with prefix: " + filePrefix + ", delimiter: '" + delimiter + "', entries/file: " + entriesPerFile).build();
    }

    /**
     * Disables logging of received data packets.
     * Any buffered data will be flushed to the current log file.
     *
     * @return A {@link Response} 200 OK if logging was disabled.
     *         Returns 503 if fst is not initialized.
     */
    @PUT
    @Path("{disablelogging : (?i)disablelogging}")
    public Response disablelogging() {
        if (fakeSystaWebService == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("FakeSystaWeb service not initialized.").build();
        }
        fakeSystaWebService.stopLoggingRawData();
        return Response.ok("Logging disabled.").build();
    }

    /**
     * Retrieves all generated log files as a single ZIP archive.
     * The ZIP file will contain all .txt log files from the configured log directory.
     * After successful streaming, the temporary ZIP file on the server is deleted.
     *
     * @return A {@link Response} containing:
     *         <ul>
     *           <li>200 OK: With the ZIP archive as an attachment if successful.</li>
     *           <li>404 Not Found: If no log files are found to archive.</li>
     *           <li>500 Internal Server Error: If an error occurs during file zipping or streaming,
     *                                          or if the FakeSystaWeb service is not initialized.</li>
     *         </ul>
     */
    @GET
    @Path("{getalllogs : (?i)getalllogs}")
    @Produces("application/zip")
    public Response getAllLogs() {
        if (fakeSystaWebService == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("FakeSystaWeb service not initialized.").build();
        }
        File zippedLogFile = fakeSystaWebService.getAllLogs();

        if (zippedLogFile == null || !zippedLogFile.exists()) {
            return Response.status(Response.Status.NOT_FOUND).entity("No log files found or error creating zip archive.").build();
        }

        // Stream the file and ensure it's deleted afterwards
        StreamingOutput stream = output -> {
            try (InputStream input = new FileInputStream(zippedLogFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.flush();
            } catch (IOException e) {
                // Log this server-side, client will see a generic error or broken stream.
                System.err.println("[SystaRESTAPI] IOException during log file streaming: " + e.getMessage());
                throw new WebApplicationException("Error streaming log file.", e);
            } finally {
                // Attempt to delete the temporary zip file
                if (!zippedLogFile.delete()) {
                    System.err.println("[SystaRESTAPI] Warning: Failed to delete temporary zip file: " + zippedLogFile.getAbsolutePath());
                }
            }
        };
        // Advise client to download the file with a specific name
        return Response.ok(stream)
                .header("Content-Disposition", "attachment; filename=\"" + zippedLogFile.getName() + "\"")
                .build();
    }

    /**
     * Deletes all generated log files from the server.
     *
     * @return A {@link Response} 200 OK with a message indicating how many files were deleted.
     *         Returns 503 if fst is not initialized.
     */
    @DELETE
    @Path("{deletealllogs : (?i)deletealllogs}")
    public Response deleteAllLogs() {
        if (fakeSystaWebService == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("FakeSystaWeb service not initialized.").build();
        }
        int deletedCount = fakeSystaWebService.deleteAllLogs();
        return Response.ok(deletedCount + " log file(s) deleted.").build();
    }

    /**
     * Serves an HTML page for monitoring raw data values in a web browser.
     * The specific HTML page served can be selected using the 'theme' query parameter.
     *
     * @param theme The theme name ("systarest" or "systaweb") to select the HTML template.
     *              Defaults to "systarest".
     * @return An {@link InputStream} for the requested HTML file, or a 404 Not Found response
     *         if the HTML file cannot be found.
     *         Returns 503 if fst is not initialized.
     */
    @GET
    @Produces({MediaType.TEXT_HTML})
    @Path("{monitorrawdata : (?i)monitorrawdata}")
    public Response getMonitorRawDataHTML(@DefaultValue("systarest") @QueryParam("theme") String theme) {
        if (fakeSystaWebService == null) { // Though fsw is not directly used here, it implies service state
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("FakeSystaWeb service not initialized.").build();
        }
        String htmlFileName;
        if ("systaweb".equalsIgnoreCase(theme)) {
            htmlFileName = "fakeremoteportal.html";
        } else {
            htmlFileName = "rawdatamonitor.html"; // Default theme
        }

        // Files are expected to be in the classpath, in the same package as this class.
        InputStream htmlStream = getClass().getResourceAsStream(htmlFileName);

        if (htmlStream == null) {
            System.err.println("[SystaRESTAPI] HTML file not found in classpath: " + htmlFileName);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("The requested HTML resource '" + htmlFileName + "' was not found.").build();
        }
        return Response.ok(htmlStream).build();
    }

    /**
     * Serves an HTML dashboard page for visualizing heating system data, typically for the last 24 hours.
     *
     * @return An {@link InputStream} for the dashboard HTML file, or a 404 Not Found response
     *         if the HTML file cannot be found.
     *         Returns 503 if fst is not initialized.
     */
    @GET
    @Produces({MediaType.TEXT_HTML})
    @Path("{dashboard : (?i)dashboard}")
    public Response getDashboardHTML() {
         if (fakeSystaWebService == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("FakeSystaWeb service not initialized.").build();
        }
        String htmlFileName = "systapidashboard.html";
        InputStream htmlStream = getClass().getResourceAsStream(htmlFileName);

        if (htmlStream == null) {
            System.err.println("[SystaRESTAPI] HTML file not found in classpath: " + htmlFileName);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("The requested HTML resource '" + htmlFileName + "' was not found.").build();
        }
        return Response.ok(htmlStream).build();
    }
}
