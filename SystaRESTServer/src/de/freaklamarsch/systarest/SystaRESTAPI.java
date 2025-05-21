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
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import de.freaklamarsch.systarest.DeviceTouchSearch.DeviceTouchDeviceInfo;
import de.freaklamarsch.systarest.FakeSystaWeb.FakeSystaWebStatus;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
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
 /**
 * A REST API for interacting with the Paradigma SystaComfort system.
 * This API provides endpoints for retrieving system status, monitoring raw data, and managing logging.
 * This class is intended to be run by a {@link SystaRESTServer}
 */
@Path("{systarest : (?i)systarest}")
public class SystaRESTAPI {
	public final static String PROP_PARADIGMA_IP = "PARADIGMA_IP";
	private static FakeSystaWeb fsw = null;
	private static Thread t = null;
	private final Map<String, Object> config = new HashMap<>();
	private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(config);

	/**
	 * Create SystaRESTAPI object which provides the Jersey REST API resource for
	 * the SystaRESTServer. The constructor is called by every call to the server
	 *
	 * @param config {@code ResourceConfig} that holds the property
	 *               {@code PROP_PARADIGMA_IP} for configuring the used IP address
	 */
	public SystaRESTAPI(@Context ResourceConfig config) {
		// constructor is called for each request, so make sure only one FakeSystaWeb is
		// created, or the socket will be blocked
		if (fsw == null) {
			fsw = new FakeSystaWeb();
			start(config);
		}
		// printAPI();
	}

	/**
	 * print information about the provided functions by this REST API. This
	 * function is only used for debugging server problems. In general, the API can
	 * also be accesses by calling
	 * {@code http://<ip>:<port>/application.wadl?detail=true}
	 */
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

	/**
	 * start the associated {@link FakeSystaWeb}, for receiving packets from a
	 * Paradigma SystaComfort II
	 *
	 * @param config {@code ResourceConfig} that holds the property
	 *               {@code PROP_PARADIGMA_IP} for configuring the used IP address
	 */
	@POST
	@Path("{start : (?i)start}")
	public void start(@Context ResourceConfig config) {
		System.out.println("SystaRESTAPI] start: called");
		if (t == null || !t.isAlive()) {
			// in Java you can start a thread only once, so we need a new one
			t = new Thread(fsw);
			System.out.println("[SystaRESTAPI] start: starting FakeSystaWeb");
			String confInetAddress = (String) config.getProperty(PROP_PARADIGMA_IP);
			System.out.println("[SystaRESTAPI] start: Configuring FakeSystaWeb to listen on IP " + confInetAddress);
			if (confInetAddress != null) {
				fsw.setInetAddress(confInetAddress);
			}
			try {
				t.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("[SystaRESTAPI] start: running");
		} else {
			System.out.println("[SystaRESTAPI] start: FakeSystaWeb is already running, ignoring request");
		}
	}

	/**
	 * stop listening for packets
	 */
	@POST
	@Path("{stop : (?i)stop}")
	public void stop() {
		System.out.println("[SystaRESTAPI] stop: called");
		fsw.stop();
		System.out.println("[SystaRESTAPI] stop: stopped");
	}

	/**
	 * find SystaComfort units using the search capability of the device touch
	 * protocol. This function looks over all available network interfaces and tries
	 * to discover device touch capable units using a search broadcast message.
	 *
	 * @return a JSON object representing the found unit, or null
	 */
	@GET
	@Path("{findsystacomfort : (?i)findsystacomfort}")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject findSystaComfort() {
		// System.out.println("Service Status called");
		try {
			DeviceTouchDeviceInfo sci = fsw.findSystaComfort();
			if (sci == null) {
				return null;
			} else {
				JsonObject jo = jsonFactory.createObjectBuilder().add("SystaWebIP", sci.localIp)
						.add("SystaWebPort", 22460).add("DeviceTouchBcastIP", sci.bcastIp)
						.add("DeviceTouchBcastPort", sci.bcastPort).add("deviceTouchInfoString", sci.string)
						.add("unitIP", sci.ip).add("unitName", sci.name).add("unitId", sci.id).add("unitApp", sci.app)
						.add("unitPlatform", sci.platform).add("unitVersion", sci.version).add("unitMajor", sci.major)
						.add("unitMinor", sci.minor).add("unitBaseVersion", sci.baseVersion).add("unitMac", sci.mac)
						.add("STouchAppSupported", sci.stouchSupported).add("DeviceTouchPort", sci.port)
						.add("DeviceTouchPassword", (sci.password == null) ? "null" : sci.password).build();
				return jo;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * return the status of the SystaRESTAPI service
	 *
	 * @return JSONObject holding the status
	 */
	@GET
	@Path("{servicestatus : (?i)servicestatus}")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject status() {
		// System.out.println("Service Status called");
		try {
			FakeSystaWebStatus fsws = fsw.getStatus();

			JsonObject jo = jsonFactory.createObjectBuilder()
					.add("timeStampString",
							DateTimeFormatter.ISO_OFFSET_DATE_TIME
									.format(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault())))
					.add("connected", fsws.connected).add("running", fsws.running)
					.add("lastDataReceivedAt", fsws.lastTimestamp).add("packetsReceived", fsws.dataPacketsReceived)
					.add("paradigmaListenerIP", fsws.localAddress).add("paradigmaListenerPort", fsws.localPort)
					.add("paradigmaIP", (fsws.remoteAddress == null) ? "" : fsws.remoteAddress.getHostAddress())
					.add("paradigmaPort", fsws.remotePort).add("loggingData", fsws.logging)
					.add("logFileSize", fsws.packetsPerFile).add("logFilePrefix", fsws.loggerFilePrefix)
					.add("logFileDelimiter", fsws.loggerEntryDelimiter).add("logFileRootPath", fsws.loggerFileRootPath)
					.add("logFilesWritten", fsws.loggerFileCount).add("logBufferedEntries", fsws.loggerBufferedEntries)
					.add("commitDate", fsws.commitDate).build();
			return jo;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the last values received by the FakeSystaWeb, without any conversion or
	 * interpretation
	 *
	 * @return JsonObject holding the values of the last received data
	 */
	@GET
	@Path("{rawdata : (?i)rawdata}")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject getRawData() {
		Integer[] rawData = fsw.getData();
		if (rawData == null) {
			return jsonFactory.createObjectBuilder().build();
		}
		// NOTE: it is not guaranteed that the time stamps match each other and the
		// timestamp of the rawData
		// there is a possibility that there is an update on the side of the
		// FakeSystaWeb between the calls
		long timestamp = fsw.getTimestamp();
		String timestampString = fsw.getTimestampString();
		JsonArrayBuilder jab = jsonFactory.createArrayBuilder();
		for (Integer i : rawData) {
			// rawData is initialized to all 0, so we do not have to check for null here
			jab.add(i.intValue());
		}
		JsonObject jo = jsonFactory.createObjectBuilder().add("timestamp", timestamp)
				.add("timestampString", timestampString).add("rawData", jab.build()).build();
		return jo;
	}

	/**
	 * returns a JsonObject holding the fields of an <a href=
	 * "https://developers.home-assistant.io/docs/core/entity/water-heater/">Home
	 * Assistant Water Heater Entity</a>
	 *
	 * @return the Water Heater Entity
	 */
	@GET
	@Path("{waterheater : (?i)waterheater}")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject getWaterHeater() {
		SystaWaterHeaterStatus whs = fsw.getWaterHeaterStatus();
		if (whs == null) {
			return jsonFactory.createObjectBuilder().build();
		}
		JsonArrayBuilder jabOperationList = jsonFactory.createArrayBuilder();
		for (String s : whs.operationList) {
			jabOperationList.add(s);
		}
		JsonArrayBuilder jabSupportedFeatures = jsonFactory.createArrayBuilder();
		for (String s : whs.supportedFeatures) {
			jabSupportedFeatures.add(s);
		}
		JsonObject jo = jsonFactory.createObjectBuilder().add("min_temp", whs.minTemp).add("max_temp", whs.maxTemp)
				.add("current_temperature", whs.currentTemperature).add("target_temperature", whs.targetTemperature)
				.add("target_temperature_high", whs.targetTemperatureHigh)
				.add("target_temperature_low", whs.targetTemperatureLow)
				.add("temperature_unit", whs.temperatureUnit.toString()).add("current_operation", whs.currentOperation)
				.add("operation_list", jabOperationList.build()).add("supported_features", jabSupportedFeatures.build())
				.add("is_away_mode_on", whs.is_away_mode_on).add("timestamp", whs.timestamp)
				.add("timestampString", whs.timestampString).build();
		return jo;
	}

	/**
	 * returns a JsonObject holding the status of the connected Paradigma
	 * SystaComfort II. The status is all known fields.
	 *
	 * @return the status
	 */
	@GET
	@Path("{status : (?i)status}")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject getStatus() {
		SystaStatus ps = fsw.getParadigmaStatus();
		if (ps == null) {
			return jsonFactory.createObjectBuilder().build();
		}
		JsonObject jo = jsonFactory.createObjectBuilder().add("outsideTemp", ps.outsideTemp)
				.add("operationMode", ps.operationMode).add("operationModeName", ps.operationModes[ps.operationMode])
				.add("circuit1FlowTemp", ps.circuit1FlowTemp).add("circuit1ReturnTemp", ps.circuit1ReturnTemp)
				.add("circuit1FlowTempSet", ps.circuit1FlowTempSet).add("circuit1LeadTime", ps.circuit1LeadTime)
				.add("circuit1OperationMode", ps.circuit1OperationMode)
				.add("circuit1OperationModeName", ps.circuit1OperationModeNames[ps.circuit1OperationMode])
				.add("hotWaterTemp", ps.hotWaterTemp).add("hotWaterTempSet", ps.hotWaterTempSet)
				.add("hotWaterTempNormal", ps.hotWaterTempNormal).add("hotWaterTempComfort", ps.hotWaterTempComfort)
				.add("hotWaterTempMax", ps.hotWaterTempMax).add("hotWaterOperationMode", ps.hotWaterOperationMode)
				.add("hotWaterOperationModeName", ps.hotWaterOperationModes[ps.hotWaterOperationMode])
				.add("hotWaterHysteresis", ps.hotWaterHysteresis).add("bufferTempTop", ps.bufferTempTop)
				.add("bufferTempBottom", ps.bufferTempBottom).add("bufferTempSet", ps.bufferTempSet)
				.add("bufferType", ps.bufferType).add("bufferTypeName", ps.bufferTypeNames[ps.bufferType])
				.add("logBoilerFlowTemp", ps.logBoilerFlowTemp).add("logBoilerReturnTemp", ps.logBoilerReturnTemp)
				.add("logBoilerBufferTempTop", ps.logBoilerBufferTempTop)
				.add("logBoilerBufferTempMin", ps.logBoilerBufferTempMin).add("logBoilerTempMin", ps.logBoilerTempMin)
				.add("logBoilerSpreadingMin", ps.logBoilerSpreadingMin)
				.add("logBoilerPumpSpeedMin", ps.logBoilerPumpSpeedMin)
				.add("logBoilerPumpSpeedActual", ps.logBoilerPumpSpeedActual)
				.add("logBoilderChargePumpIsOn", ps.logBoilderChargePumpIsOn)
				.add("logBoilerSettings", ps.logBoilerSettings)
				.add("logBoilerParallelOperation", ps.logBoilerParallelOperation)
				.add("logBoilerOperationMode", ps.logBoilerOperationMode)
				.add("logBoilerOperationModeName", ps.logBoilerOperationModeNames[ps.logBoilerOperationMode])
				.add("boilerHeatsBuffer", ps.boilerHeatsBuffer).add("boilerOperationMode", ps.boilerOperationMode)
				.add("boilerOperationModeName", ps.boilerOperationModeNames[ps.boilerOperationMode])
				.add("boilerFlowTemp", ps.boilerFlowTemp).add("boilerReturnTemp", ps.boilerReturnTemp)
				.add("boilerTempSet", ps.boilerTempSet).add("boilerSuperelevation", ps.boilerSuperelevation)
				.add("boilerHysteresis", ps.boilerHysteresis).add("boilerOperationTime", ps.boilerOperationTime)
				.add("boilerShutdownTemp", ps.boilerShutdownTemp).add("boilerPumpSpeedMin", ps.boilerPumpSpeedMin)
				.add("boilerPumpSpeedActual", ps.boilerPumpSpeedActual).add("boilerLedIsOn", ps.boilerLedIsOn)
				.add("circulationOperationMode", ps.circulationOperationMode)
				.add("circulationOperationModeName", ps.circulationOperationModeNames[ps.circulationOperationMode])
				.add("circulationTemp", ps.circulationTemp).add("circulationPumpIsOn", ps.circulationPumpIsOn)
				.add("circulationPumpOverrun", ps.circulationPumpOverrun)
				.add("circulationLockoutTimePushButton", ps.circulationLockoutTimePushButton)
				.add("circulationHysteresis", ps.circulationHysteresis).add("circuit2FlowTemp", ps.circuit2FlowTemp)
				.add("circuit2FlowTemp", ps.circuit2FlowTemp).add("circuit2ReturnTemp", ps.circuit2ReturnTemp)
				.add("circuit2FlowTempSet", ps.circuit2FlowTempSet).add("roomTempActual1", ps.roomTempActual1)
				.add("roomTempSet1", ps.roomTempSet1).add("roomTempActual2", ps.roomTempActual2)
				.add("roomTempSet2", ps.roomTempSet2).add("roomTempSetNormal", ps.roomTempSetNormal)
				.add("roomTempSetComfort", ps.roomTempSetComfort).add("roomTempSetLowering", ps.roomTempSetLowering)
				.add("roomImpact", ps.roomImpact).add("roomTempCorrection", ps.roomTempCorrection)
				.add("collectorTempActual", ps.collectorTempActual).add("swimmingpoolTemp", ps.swimmingpoolTemp)
				.add("swimmingpoolFlowTemp", ps.swimmingpoolFlowTemp)
				.add("swimmingpoolReturnTemp", ps.swimmingpoolReturnTemp)
				.add("heatingOperationMode", ps.heatingOperationMode)
				.add("heatingOperationModeName", ps.heatingOperationModes[ps.heatingOperationMode])
				.add("heatingCurveBasePoint", ps.heatingCurveBasePoint)
				.add("heatingCurveGradient", ps.heatingCurveGradient).add("heatingLimitTemp", ps.heatingLimitTemp)
				.add("heatingLimitTeampLowering", ps.heatingLimitTeampLowering)
				.add("heatingPumpSpeedActual", ps.heatingPumpSpeedActual)
				.add("heatingPumpOverrun", ps.heatingPumpOverrun).add("heatingPumpIsOn", ps.heatingPumpIsOn)
				.add("heatingCircuitSpreading", ps.heatingCircuitSpreading)
				.add("heatingPumpSpeedMin", ps.heatingPumpSpeedMin).add("controlledBy", ps.controlledBy)
				.add("controlMethodName", ps.controlMethods[ps.controlledBy]).add("maxFlowTemp", ps.maxFlowTemp)
				.add("antiFreezeOutsideTemp", ps.antiFreezeOutsideTemp).add("heatUpTime", ps.heatUpTime)
				.add("mixerRuntime", ps.mixerRuntime).add("mixer1IsOnWarm", ps.mixer1IsOnWarm)
				.add("mixer1IsOnCool", ps.mixer1IsOnCool).add("mixer1State", ps.mixer1State)
				.add("mixer1StateName", ps.mixerStateNames[ps.mixer1State])
				.add("underfloorHeatingBasePoint", ps.underfloorHeatingBasePoint)
				.add("underfloorHeatingGradient", ps.underfloorHeatingGradient).add("bufferTempMax", ps.bufferTempMax)
				.add("bufferTempMin", ps.bufferTempMin).add("adjustRoomTempBy", ps.adjustRoomTempBy)
				.add("solarPowerActual", ps.solarPowerActual).add("solarGainDay", ps.solarGainDay)
				.add("solarGainTotal", ps.solarGainTotal).add("relay", ps.relay)
				.add("chargePumpIsOn", ps.chargePumpIsOn).add("boilerIsOn", ps.boilerIsOn)
				.add("burnerIsOn", ps.burnerIsOn).add("systemNumberOfStarts", ps.systemNumberOfStarts)
				.add("burnerNumberOfStarts", ps.burnerNumberOfStarts)
				.add("boilerOperationTimeHours", ps.boilerOperationTimeHours)
				.add("boilerOperationTimeMinutes", ps.boilerOperationTimeMinutes)
				.add("unknowRelayState1IsOn", ps.unknowRelayState1IsOn)
				.add("unknowRelayState2IsOn", ps.unknowRelayState2IsOn)
				.add("unknowRelayState5IsOn", ps.unknowRelayState5IsOn).add("error", ps.error)
				.add("operationModeX", ps.operationModeX).add("heatingOperationModeX", ps.heatingOperationModeX)
				.add("timestamp", ps.timestamp).add("timestampString", ps.timestampString).build();
		return jo;

	}

	/**
	 * enables the logging of each received data element to a log file
	 *
	 * @param filePrefix     the prefix for the created log files. Following the
	 *                       prefix a running number is added to the file names
	 *                       defaults to {@code paradigma}
	 * @param delimiter      the delimiter used in the log files for seperating the
	 *                       entries. Defaults to {@code ;}
	 * @param entriesPerFile the logger collects up to this number of elemnt before
	 *                       writing the file to disk. Defaults to {@code 60}
	 */
	@PUT
	@Path("{enablelogging : (?i)enablelogging}")
	public void enablelogging(@DefaultValue("SystaREST") @QueryParam("filePrefix") String filePrefix,
			@DefaultValue(";") @QueryParam("logEntryDelimiter") String delimiter,
			@DefaultValue("60") @QueryParam("entriesPerFile") int entriesPerFile) {
		fsw.logRawData(filePrefix, delimiter, entriesPerFile);
	}

	/**
	 * disables the logging of the received data packets. Writes the collected
	 * packets from memory to disk, even if the last file is not full
	 */
	@PUT
	@Path("{disablelogging : (?i)disablelogging}")
	public void disablelogging() {
		fsw.stopLoggingRawData();
	}

	@GET
	@Path("{getalllogs : (?i)getalllogs}")
	@Produces("application/zip")
	public Response getAllLogs() {
		File file = fsw.getAllLogs();
		System.out.println("[SystaRESTServer] return zip file: " + file);
		return Response.ok(file).header("Content-Disposition", "attachment; filename=" + file.getName())
				.entity(new StreamingOutput() {
					@Override
					public void write(final OutputStream output) throws IOException, WebApplicationException {
						try {
							Files.copy(file.toPath(), output);
						} finally {
							file.delete();
						}
					}
				}).build();
	}

	@DELETE
	@Path("{deletealllogs : (?i)deletealllogs}")
	public void deleteAllLogs() {
		fsw.deleteAllLogs();
	}

	/**
	 * Returns the a .html file for monitoring raw data in the browser.
	 * 
	 * @param theme parameter to define which page .html file to load. Possible
	 *              values are systarest or systaweb
	 * @return the InputStream of the file, or null, if something went wrong in the
	 *         file handling
	 */
	@GET
	@Produces({ MediaType.TEXT_HTML })
	@Path("{monitorrawdata : (?i)monitorrawdata}")
	public InputStream getMonitorRawDataHTML(@DefaultValue("systarest") @QueryParam("theme") String theme) {
		String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		String monitorHTML = rootPath;
		if (theme.equalsIgnoreCase("systaweb")) {
			monitorHTML += "fakeremoteportal.html";
		} else {
			// default page to load
			monitorHTML += "rawdatamonitor.html";
		}

		File f = new File(monitorHTML);
		try {
			return new FileInputStream(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the a .html file for showing a dashboard for the values of the last
	 * 24h in the browser.
	 * 
	 * @return the InputStream of the file, or null, if something went wrong in the
	 *         file handling
	 */
	@GET
	@Produces({ MediaType.TEXT_HTML })
	@Path("{dashboard : (?i)dashboard}")
	public InputStream getDashboardHTML() {
		String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		String dashboardHTML = rootPath;
		dashboardHTML += "systapidashboard.html";

		File f = new File(dashboardHTML);
		try {
			return new FileInputStream(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

}
