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

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import de.freaklamarsch.systarest.DataLogger;
import de.freaklamarsch.systarest.SystaRESTAPI;
import jakarta.json.JsonObject;
//import javax.annotation.Priority;
//import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // fix incompatibility with JUnit5
class SystaRESTAPITest extends JerseyTest {
	private static final String logDir = "./SystaLogs";

	// do not name this setup()
	@BeforeAll // fix incompatibility with JUnit5
	public void before() throws Exception {
		super.setUp();
	}

	// do not name this tearDown()
	@AfterAll // fix incompatibility with JUnit5
	public void after() throws Exception {
		super.tearDown();
	}

	@Override
	public Application configure() {
		System.out.println("SystaRESTAPITest: configure()");
		enable(TestProperties.LOG_TRAFFIC);
		enable(TestProperties.DUMP_ENTITY);
		// load interfaces
		String paradigmaIfaceName = "lo";
		// get IPv4 addresses for interfaces
		String paradigmIPv4 = getIPv4Address(paradigmaIfaceName);
		assertNotNull("For working properly, SystaRESTAPI needs a configured interface", paradigmIPv4);
		System.out.println("[RESTServer] Interface for connecting to Paradigma Systa Comfort II: " + paradigmIPv4);
		changeDefaultLogDir(logDir);
		ResourceConfig config = new ResourceConfig(SystaRESTAPI.class);
		config.property(SystaRESTAPI.PROP_PARADIGMA_IP, paradigmIPv4);
		return config;
	}

	@Test
	void testStartAndStop() {
			Response response = target("/systarest/start").request().post(null);
			assertEquals(204, response.getStatus());

			response = target("/systarest/stop").request().post(null);
			assertEquals(204, response.getStatus());
	}

	@Test
	void testServiceStatus() {
			Response response = target("/systarest/servicestatus").request().get();
			assertEquals(200, response.getStatus());
			assertNotNull(response.readEntity(String.class));
	}

	@Test
	void testInvalidEndpoint() {
			Response response = target("/systarest/invalid").request().get();
			assertEquals(404, response.getStatus());
	}

	/**
	 * Use reflection to change the hardcoded directory used for the location of log
	 * files. As DEFAULT_ROOT_PATH is static final in DataLogger, it is officially a
	 * security risk to change this value. This function might not work with future
	 * Java versions.
	 */
	private void changeDefaultLogDir(String newDir) {
		Field DEFAULT_ROOT_PATH;
		try {
			// HACK to change the default folder to avoid that this test does any damage to
			// the users home folder
			// its not a nice way
			// Field modifiersField = Field.class.getDeclaredField("modifiers");
			// modifiersField.setAccessible(true);
			// modifiersField.set(DataLogger.class, DataLogger.class.getModifiers() &
			// ~Modifier.FINAL);
			VarHandle MODIFIERS;
			var lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
			MODIFIERS = lookup.findVarHandle(Field.class, "modifiers", int.class);
			DEFAULT_ROOT_PATH = DataLogger.class.getDeclaredField("DEFAULT_ROOT_PATH");
			int mods = DEFAULT_ROOT_PATH.getModifiers();
			if (Modifier.isFinal(mods)) {
				MODIFIERS.set(DEFAULT_ROOT_PATH, mods & ~Modifier.FINAL);
			}
			DEFAULT_ROOT_PATH.setAccessible(true);
			DEFAULT_ROOT_PATH.set(DataLogger.class, newDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

/*	@Test
	public void testServicestatus() {
		System.out.println("SystaRESTAPITest: testServicestatus()");
		Response response = target("/systarest/servicestatus").request().get();
		assertEquals("should return status 200", 200, response.getStatus());
		// test the returned JsonObject for default values
		JsonObject json = response.readEntity(JsonObject.class);
		System.out.println("testServicestatus: " + json);
		assertFalse(json.getBoolean("connected"));
		assertTrue(json.getBoolean("running"));
		assertEquals("never", json.getString("lastDataReceivedAt"));
		assertEquals(0, json.getInt("packetsReceived"));
		assertEquals("127.0.0.1", json.getString("paradigmaListenerIP"));
		assertEquals(22460, json.getInt("paradigmaListenerPort"));
		assertEquals("", json.getString("paradigmaIP"));
		assertEquals(0, json.getInt("paradigmaPort"));
		assertFalse(json.getBoolean("loggingData"));
		assertEquals(60, json.getInt("logFileSize"));
		assertEquals("DataLogger", json.getString("logFilePrefix"));
		assertEquals(";", json.getString("logFileDelimiter"));
		assertTrue(json.getString("logFileRootPath").endsWith(logDir));
		assertEquals(";", json.getString("logFileDelimiter"));
		assertEquals(0, json.getInt("logFilesWritten"));
		assertEquals(0, json.getInt("logBufferedEntries"));
	}*/

	@Test
	public void testStart() {
		System.out.println("SystaRESTAPITest: testStart()");
		Response response = target("/systarest/stop").request().post(Entity.json(""));
		response = target("/systarest/start").request().post(Entity.json(""));
		assertEquals("should return status 204", 204, response.getStatus());
		// test the status of the REST API service if it reflects the running state
		response = target("/systarest/servicestatus").request().get();
		assertEquals("should return status 200", 200, response.getStatus());
		JsonObject json = response.readEntity(JsonObject.class);
		System.out.println("testStart: " + json);
		assertFalse(json.getBoolean("connected"));
		assertTrue(json.getBoolean("running"));
		assertEquals("never", json.getString("lastDataReceivedAt"));
		assertEquals(0, json.getInt("packetsReceived"));
		assertEquals("127.0.0.1", json.getString("paradigmaListenerIP"));
		assertEquals(22460, json.getInt("paradigmaListenerPort"));
		assertEquals("", json.getString("paradigmaIP"));
		assertEquals(0, json.getInt("paradigmaPort"));
		assertFalse(json.getBoolean("loggingData"));
		assertEquals(60, json.getInt("logFileSize"));
		assertEquals("DataLogger", json.getString("logFilePrefix"));
		assertEquals(";", json.getString("logFileDelimiter"));
		assertTrue(json.getString("logFileRootPath").endsWith(logDir));
		assertEquals(";", json.getString("logFileDelimiter"));
		assertEquals(0, json.getInt("logFilesWritten"));
		assertEquals(0, json.getInt("logBufferedEntries"));
	}

	@Test
	public void testStop() {
		System.out.println("SystaRESTAPITest: testStop()");
		Response response = target("/systarest/start").request().post(Entity.json(""));
		response = target("/systarest/stop").request().post(Entity.json(""));
		assertEquals("should return status 204", 204, response.getStatus());
		// test the status of the REST API service if it reflects the running state
		response = target("/systarest/servicestatus").request().get();
		assertEquals("should return status 200", 200, response.getStatus());
		JsonObject json = response.readEntity(JsonObject.class);
		System.out.println("testStop: " + json);
		assertFalse(json.getBoolean("connected"));
		assertFalse(json.getBoolean("running"));
		assertEquals("never", json.getString("lastDataReceivedAt"));
		assertEquals(0, json.getInt("packetsReceived"));
		assertEquals("127.0.0.1", json.getString("paradigmaListenerIP"));
		assertEquals(22460, json.getInt("paradigmaListenerPort"));
		assertEquals("", json.getString("paradigmaIP"));
		assertEquals(0, json.getInt("paradigmaPort"));
		assertFalse(json.getBoolean("loggingData"));
		assertEquals(60, json.getInt("logFileSize"));
		assertEquals("DataLogger", json.getString("logFilePrefix"));
		assertEquals(";", json.getString("logFileDelimiter"));
		assertTrue(json.getString("logFileRootPath").endsWith(logDir));
		assertEquals(";", json.getString("logFileDelimiter"));
		assertEquals(0, json.getInt("logFilesWritten"));
		assertEquals(0, json.getInt("logBufferedEntries"));
	}

	@Test
	public void testEnablelogging() {
		System.out.println("SystaRESTAPITest: testEnablelogging()");
		Response response = target("/systarest/start").request().post(Entity.json(""));
		target("/systarest/enablelogging").queryParam("filePrefix", "test").queryParam("logEntryDelimiter", "<>")
				.queryParam("entriesPerFile", 10).request().put(Entity.json(""));
		response = target("/systarest/servicestatus").request().get();
		assertEquals("should return status 200", 200, response.getStatus());
		// test the returned JsonObject for default values
		JsonObject json = response.readEntity(JsonObject.class);
		System.out.println("testEnablelogging: " + json);
		assertFalse(json.getBoolean("connected"));
		assertTrue(json.getBoolean("running"));
		assertEquals("never", json.getString("lastDataReceivedAt"));
		assertEquals(0, json.getInt("packetsReceived"));
		assertEquals("127.0.0.1", json.getString("paradigmaListenerIP"));
		assertEquals(22460, json.getInt("paradigmaListenerPort"));
		assertEquals("", json.getString("paradigmaIP"));
		assertEquals(0, json.getInt("paradigmaPort"));
		assertTrue(json.getBoolean("loggingData"));
		assertEquals(10, json.getInt("logFileSize"));
		assertEquals("test", json.getString("logFilePrefix"));
		assertEquals("<>", json.getString("logFileDelimiter"));
		assertTrue(json.getString("logFileRootPath").endsWith(logDir));
		assertEquals(0, json.getInt("logFilesWritten"));
		assertEquals(0, json.getInt("logBufferedEntries"));
	}

	@Test
	public void testDisablelogging() {
		System.out.println("SystaRESTAPITest: testDisablelogging()");
		Response response = target("/systarest/start").request().post(Entity.json(""));
		target("/systarest/enablelogging").request().put(Entity.json(""));
		target("/systarest/disablelogging").request().put(Entity.json(""));
		response = target("/systarest/servicestatus").request().get();
		assertEquals("should return status 200", 200, response.getStatus());
		// test the returned JsonObject for default values
		JsonObject json = response.readEntity(JsonObject.class);
		System.out.println("testDisablelogging: " + json);
		assertFalse(json.getBoolean("connected"));
		assertTrue(json.getBoolean("running"));
		assertEquals("never", json.getString("lastDataReceivedAt"));
		assertEquals(0, json.getInt("packetsReceived"));
		assertEquals("127.0.0.1", json.getString("paradigmaListenerIP"));
		assertEquals(22460, json.getInt("paradigmaListenerPort"));
		assertEquals("", json.getString("paradigmaIP"));
		assertEquals(0, json.getInt("paradigmaPort"));
		assertFalse(json.getBoolean("loggingData"));
		assertEquals(60, json.getInt("logFileSize"));
		assertEquals("SystaREST", json.getString("logFilePrefix"));
		assertEquals(";", json.getString("logFileDelimiter"));
		assertTrue(json.getString("logFileRootPath").endsWith(logDir));
		assertEquals(0, json.getInt("logFilesWritten"));
		assertEquals(0, json.getInt("logBufferedEntries"));
	}

	/**
	 * Helper function to get the IPv4 address for a given interface name
	 *
	 * @param interfaceName
	 * @return the configured IPv4 address as {@code String}, e.g. "127.0.0.1", or
	 *         null if no configured IPv4 address was found
	 */
	private static String getIPv4Address(String interfaceName) {
		try {
			NetworkInterface restAPIIface = NetworkInterface.getByName(interfaceName);
			if (restAPIIface == null || !restAPIIface.isUp()) {
				System.out.println("[RESTServer] Interface " + interfaceName + " is not configured");
				return null;
			}
			Enumeration<InetAddress> addresses = restAPIIface.getInetAddresses();
			while (addresses.hasMoreElements()) {
				InetAddress addr = addresses.nextElement();
				if (addr instanceof Inet4Address) {
					return addr.getHostAddress();
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

}
