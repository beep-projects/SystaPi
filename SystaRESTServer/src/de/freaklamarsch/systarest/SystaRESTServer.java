/*
* Copyright (c) 2021, The beep-projects contributors
* this file originated from https://github.com/beep-projects
* Do not remove the lines above.
* The rest of this source code is subject to the terms of the Mozilla Public License.
* You can obtain a copy of the MPL at <https://www.mozilla.org/MPL/2.0/>.
*/
package de.freaklamarsch.systarest;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.sun.net.httpserver.HttpServer;

import jakarta.ws.rs.core.UriBuilder;

/**
 * The main function of this class starts an
 * {@link com.sun.net.httpserver.HttpServer} for providing {@link SystaRESTAPI}.
 * The main method requires a configured SystaREST.properties for setting the
 * interfaces to use. On interface is used for the connection to the Paradigma
 * SystaComfort II and one for acessing the REST API.
 */
public class SystaRESTServer {

	public static void main(String[] args) {
		// load configuration from SystaREST.properties
		String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		String defaultConfigPath = rootPath + "SystaREST.properties";
		System.out.println("[RESTServer] Reading properties from file: " + defaultConfigPath);
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(defaultConfigPath));
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("[RESTServer] Could not load properties file: " + defaultConfigPath + ". Exiting.");
			System.exit(0);
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
			System.out.println("[RESTServer] Interface for Paradigma RESTAPI: " + restAPIIPv4);
			System.out.println("[RESTServer] Interface for connecting to Paradigma SystaComfort II: " + paradigmIPv4);
			System.exit(0);
		}

		System.out.println("[RESTServer] Interface for Paradigma RESTAPI: " + restAPIIPv4);
		System.out.println("[RESTServer] Interface for connecting to Paradigma SystaComfort II: " + paradigmIPv4);

		// create REST API server
		System.out.println("[RESTServer] Starting RESTServer");
		URI baseUri = UriBuilder.fromUri("http://" + restAPIIPv4 + "/")
				.port(Integer.parseInt(props.getProperty("RESTAPI_PORT"))).build();
		ResourceConfig config = new ResourceConfig(SystaRESTAPI.class);
		config.property(SystaRESTAPI.PROP_PARADIGMA_IP, paradigmIPv4);
		// config.property("jersey.config.server.wadl.disableWadl", true);
		HttpServer server = JdkHttpServerFactory.createHttpServer(baseUri, config, false);
		server.start();
		System.out.println("[RESTServer] RESTServer started at " + baseUri);
		try {
			// start the SystaRESTAPI
			System.out.println("Calling: " + baseUri + "SystaREST/start");
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUri + "SystaREST/start"))
					.POST(HttpRequest.BodyPublishers.ofString("")).build();
			// send the POST SystaREST/start request
			client.send(request, HttpResponse.BodyHandlers.ofString());

		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Thread.currentThread().join();
			System.out.println("[RESTServer] Server exited");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		server.stop(0);
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

	/**
	 * Helper function to print information about a given {@link NetworkInterface}
	 * 
	 * @param netint the {@link NetworkInterface} for which the information should
	 *               be print
	 * 
	 */
	private static void displayInterfaceInformation(NetworkInterface netint) throws SocketException {
		System.out.printf("Display name: %s\n", netint.getDisplayName());
		System.out.printf("Name: %s\n", netint.getName());
		Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
		for (InetAddress inetAddress : Collections.list(inetAddresses)) {
			System.out.printf("InetAddress: %s\n", inetAddress);
		}
		System.out.printf("\n");
	}
}
