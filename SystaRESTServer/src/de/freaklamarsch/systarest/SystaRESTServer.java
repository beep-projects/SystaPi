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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Enumeration;
import java.util.Properties;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.sun.net.httpserver.HttpServer;

import jakarta.ws.rs.core.UriBuilder;

/**
 * The main function of this class starts an
 * {@link com.sun.net.httpserver.HttpServer} for providing {@link SystaRESTAPI} and {@link STouchRESTAPI}.
 * The main method requires a configured SystaREST.properties for setting the
 * interfaces that should be used. One interface is used for the connection to the Paradigma
 * SystaComfort II and one for accessing the REST APIs.
 */
public class SystaRESTServer {

	public static void main(String[] args) {
		// load configuration from SystaREST.properties
		String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		String defaultConfigPath = rootPath + "SystaREST.properties";
		System.out.println("[SystaRESTServer] Reading properties from file: " + defaultConfigPath);
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(defaultConfigPath));
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("[SystaRESTServer] Could not load properties file: " + defaultConfigPath + ". Exiting.");
			System.exit(1);
		}
		// load interfaces
		String restAPIIfaceName = props.getProperty("RESTAPI_INTERFACE");
		String paradigmaIfaceName = props.getProperty("PARADIGMA_INTERFACE");
		// get IPv4 addresses for interfaces
		String restAPIIPv4 = getIPv4Address(restAPIIfaceName);
		String paradigmIPv4 = getIPv4Address(paradigmaIfaceName);

		if (restAPIIPv4 == null || paradigmIPv4 == null) {
			System.out.println(restAPIIfaceName + " and/or " + paradigmaIfaceName
					+ " are not configured properly. Please check your configuration. I am exiting now!");
			System.out.println("[SystaRESTServer] Interface for Paradigma RESTAPI: " + restAPIIPv4);
			System.out.println("[SystaRESTServer] Interface for connecting to Paradigma SystaComfort II: " + paradigmIPv4);
			System.exit(1);
		}

		System.out.println("[SystaRESTServer] Interface for Paradigma RESTAPI: " + restAPIIPv4);
		System.out.println("[SystaRESTServer] Interface for connecting to Paradigma SystaComfort II: " + paradigmIPv4);

		// create REST API server
		System.out.println("[SystaRESTServer] Starting SystaRESTServer");
		URI baseUri = UriBuilder.fromUri("http://" + restAPIIPv4 + "/")
				.port(Integer.parseInt(props.getProperty("RESTAPI_PORT"))).build();
		ResourceConfig config = new ResourceConfig(SystaRESTAPI.class);
		//config.register(new STouchRESTAPI());
		config.register(STouchRESTAPI.class);
		config.register(new CorsFilter());
		config.property(SystaRESTAPI.PROP_PARADIGMA_IP, paradigmIPv4);
		// config.property("jersey.config.server.wadl.disableWadl", true);
		HttpServer server = JdkHttpServerFactory.createHttpServer(baseUri, config, false);
		server.start();
		System.out.println("[SystaRESTServer] SystaRESTServer started at " + baseUri);
		try {
			// start the SystaRESTAPI
			System.out.println("[SystaRESTServer] Calling: " + baseUri + "SystaREST/start");
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUri + "SystaREST/start"))
					.POST(HttpRequest.BodyPublishers.ofString("")).build();
			// send the POST SystaREST/start request
			client.send(request, HttpResponse.BodyHandlers.ofString());
			System.out.println("[SystaRESTServer] Returning from: " + baseUri + "SystaREST/start");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			System.out.println("[SystaRESTServer] Joining currenThread");
			Thread.currentThread().join();
			System.out.println("[SystaRESTServer] Server exited");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		server.stop(0);
        System.out.println("[SystaRESTServer] Server stopped");
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
				System.out.println("[SystaRESTServer] Interface " + interfaceName + " is not configured");
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
