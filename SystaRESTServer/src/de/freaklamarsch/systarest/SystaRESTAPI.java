package de.freaklamarsch.systarest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import de.freaklamarsch.systarest.FakeSystaWeb.FakeSystaWebStatus;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * class for creating a Jersey resource that offers a REST API for a Paradigma
 * SystaComfort II For running this class a {@link SystaRESTServer} is required
 *
 */
@Path("{systarest : (?i)systarest}")
public class SystaRESTAPI {
	public final static String PROP_PARADIGMA_IP = "PARADIGMA_IP";
	private static FakeSystaWeb fsw = null;
	private static Thread t = null;
	private final Map<String, Object> config = new HashMap<String, Object>();
	private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(config);
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E-dd.MM.yy-HH:mm:ss");

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
		System.out.println("[ParadigmaRESTAPI] start: called");
		if (t == null || !t.isAlive()) {
			// in Java you can start a thread only once, so we need a new one
			t = new Thread(fsw);
			System.out.println("[ParadigmaRESTAPI] start: starting FakeSystaWeb");
			String confInetAddress = (String) config.getProperty(PROP_PARADIGMA_IP);
			System.out.println("[ParadigmaRESTAPI] start: Configuring FakeSystaWeb to listen on IP " + confInetAddress);
			if (confInetAddress != null) {
				fsw.setInetAddress(confInetAddress);
			}
			try {
				t.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("[ParadigmaRESTAPI] start: running");
		} else {
			System.out.println("[ParadigmaRESTAPI] start: FakeSystaWeb is already running, ignoring request");
		}
	}

	/**
	 * stop listening for packets
	 */
	@POST
	@Path("{stop : (?i)stop}")
	public void stop() {
		System.out.println("[ParadigmaRESTAPI] stop: called");
		fsw.stop();
		System.out.println("[ParadigmaRESTAPI] stop: stopped");
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
					.add("timeStampString", formatter.format(LocalDateTime.now())).add("connected", fsws.connected)
					.add("running", fsws.running).add("lastDataReceivedAt", fsws.lastTimestamp)
					.add("packetsReceived", fsws.dataPacketsReceived).add("paradigmaListenerIP", fsws.localAddress)
					.add("paradigmaListenerPort", fsws.localPort)
					.add("paradigmaIP", (fsws.remoteAddress == null) ? "" : fsws.remoteAddress.getHostAddress())
					.add("paradigmaPort", fsws.remotePort).add("loggingData", fsws.logging)
					.add("logFileSize", fsws.packetsPerFile).add("logFilePrefix", fsws.loggerFilePrefix)
					.add("logFileDelimiter", fsws.loggerEntryDelimiter).add("logFileRootPath", fsws.loggerFileRootPath)
					.add("logFilesWritten", fsws.loggerFileCount).add("logBufferedEntries", fsws.loggerBufferedEntries)
					.build();
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
		// System.out.println("[ParadigmaRESTAPI] getRawData: called");
		// System.out.println("[ParadigmaRESTAPI] getRawData: fsw.getData()");
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
		// System.out.println("[ParadigmaRESTAPI] getRawData: got
		// rawData["+rawData.length+"] from fsw");
		// System.out.println("[ParadigmaRESTAPI] getRawData: create rawData JSON
		// array");
		JsonArrayBuilder jab = jsonFactory.createArrayBuilder();
		// System.out.println("[ParadigmaRESTAPI] getRawData: populate rawData JSON
		// array");
		for (int i : rawData) {
			jab.add(i);
		}
		// System.out.println("[ParadigmaRESTAPI] getRawData: building JSON object");
		JsonObject jo = jsonFactory.createObjectBuilder().add("timestamp", timestamp)
				.add("timestampString", timestampString).add("rawData", jab.build()).build();
		// System.out.println("[ParadigmaRESTAPI] getRawData: finished building JSON
		// object");
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
				.add("circuit1FlowTemp", ps.circuit1FlowTemp).add("circuit1ReturnTemp", ps.circuit1ReturnTemp)
				.add("hotWaterTemp", ps.hotWaterTemp).add("bufferTempTop", ps.bufferTempTop)
				.add("bufferTempBottom", ps.bufferTempBottom).add("circulationTemp", ps.circulationTemp)
				.add("circuit2FlowTemp", ps.circuit2FlowTemp).add("circuit2ReturnTemp", ps.circuit2ReturnTemp)
				.add("roomTempActual1", ps.roomTempActual1).add("roomTempActual2", ps.roomTempActual2)
				.add("collectorTempActual", ps.collectorTempActual).add("boilerFlowTemp", ps.boilerFlowTemp)
				.add("boilerReturnTemp", ps.boilerReturnTemp).add("stoveFlowTemp", ps.stoveFlowTemp)
				.add("stoveReturnTemp", ps.stoveReturnTemp).add("woodBoilerBufferTempTop", ps.woodBoilerBufferTempTop)
				.add("swimmingpoolTemp", ps.swimmingpoolTemp).add("swimmingpoolFlowTeamp", ps.swimmingpoolFlowTeamp)
				.add("swimmingpoolReturnTemp", ps.swimmingpoolReturnTemp).add("hotWaterTempSet", ps.hotWaterTempSet)
				.add("roomTempSet1", ps.roomTempSet1).add("circuit1FlowTempSet", ps.circuit1FlowTempSet)
				.add("circuit2FlowTempSet", ps.circuit2FlowTempSet).add("roomTempSet2", ps.roomTempSet2)
				.add("bufferTempSet", ps.bufferTempSet).add("boilerTempSet", ps.boilerTempSet)
				.add("operationMode", ps.operationMode).add("operationModeName", ps.operationModes[ps.operationMode])
				.add("roomTempSetNormal", ps.roomTempSetNormal).add("roomTempSetComfort", ps.roomTempSetComfort)
				.add("roomTempSetLowering", ps.roomTempSetLowering).add("heatingOperationMode", ps.heatingOperationMode)
				.add("heatingOperationModeName", ps.heatingOperationModes[ps.heatingOperationMode])
				.add("controlledBy", ps.controlledBy).add("controlMethodName", ps.controlMethods[ps.controlledBy])
				.add("heatingCurveBasePoint", ps.heatingCurveBasePoint)
				.add("heatingCurveGradient", ps.heatingCurveGradient).add("maxFlowTemp", ps.maxFlowTemp)
				.add("heatingLimitTemp", ps.heatingLimitTemp)
				.add("heatingLimitTeampLowering", ps.heatingLimitTeampLowering)
				.add("antiFreezeOutsideTemp", ps.antiFreezeOutsideTemp).add("heatUpTime", ps.heatUpTime)
				.add("roomImpact", ps.roomImpact).add("boilerSuperelevation", ps.boilerSuperelevation)
				.add("spreadingHeatingCircuit", ps.spreadingHeatingCircuit)
				.add("heatingMinSpeedPump", ps.heatingMinSpeedPump).add("mixerRuntime", ps.mixerRuntime)
				.add("roomTempCorrection", ps.roomTempCorrection)
				.add("underfloorHeatingBasePoint", ps.underfloorHeatingBasePoint)
				.add("underfloorHeatingGradient", ps.underfloorHeatingGradient)
				.add("hotWaterTempNormal", ps.hotWaterTempNormal).add("hotWaterTempComfort", ps.hotWaterTempComfort)
				.add("hotWaterOperationMode", ps.hotWaterOperationMode)
				.add("hotWaterOperationModeName", ps.hotWaterOperationModes[ps.hotWaterOperationMode])
				.add("hotWaterHysteresis", ps.hotWaterHysteresis).add("hotWaterTempMax", ps.hotWaterTempMax)
				.add("pumpOverrun", ps.pumpOverrun).add("bufferTempMax", ps.bufferTempMax)
				.add("bufferTempMin", ps.bufferTempMin).add("boilerHysteresis", ps.boilerHysteresis)
				.add("boilerOperationTime", ps.boilerOperationTime).add("boilerShutdownTemp", ps.boilerShutdownTemp)
				.add("boilerMinSpeedPump", ps.boilerMinSpeedPump)
				.add("circulationPumpOverrun", ps.circulationPumpOverrun)
				.add("circulationHysteresis", ps.circulationHysteresis).add("adjustRoomTempBy", ps.adjustRoomTempBy)
				.add("boilerOperationTimeHours", ps.boilerOperationTimeHours)
				.add("boilerOperationTimeMinutes", ps.boilerOperationTimeMinutes)
				.add("numberBurnerStarts", ps.numberBurnerStarts).add("solarPowerActual", ps.solarPowerActual)
				.add("solarGainDay", ps.solarGainDay).add("solarGainTotal", ps.solarGainTotal)
				.add("countdown", ps.countdown).add("relay", ps.relay).add("heatingPumpIsOn", ps.heatingPumpIsOn)
				.add("chargePumpIsOn", ps.chargePumpIsOn).add("circulationPumpIsOn", ps.circulationPumpIsOn)
				.add("boilerIsOn", ps.boilerIsOn).add("burnerIsOn", ps.burnerIsOn)
				.add("unknowRelayState1IsOn", ps.unknowRelayState1IsOn)
				.add("unknowRelayState2IsOn", ps.unknowRelayState2IsOn)
				.add("unknowRelayState3IsOn", ps.unknowRelayState3IsOn)
				.add("unknowRelayState4IsOn", ps.unknowRelayState4IsOn)
				.add("unknowRelayState5IsOn", ps.unknowRelayState5IsOn).add("error", ps.error)
				.add("operationModeX", ps.operationModeX).add("heatingOperationModeX", ps.heatingOperationModeX)
				.add("stovePumpSpeedActual", ps.stovePumpSpeedActual).add("timestamp", ps.timestamp)
				.add("timestampString", ps.timestampString).build();
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

}