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
 * Main class for starting the SystaREST server.
 * <p>
 * This server provides ReSTful APIs for interacting with a Paradigma SystaComfort
 * heating system, primarily through the {@link SystaRESTAPI} (for data polling and logging)
 * and {@link STouchRESTAPI} (for emulating S-Touch app interactions).
 * </p>
 * <p>
 * Configuration is loaded from a {@code SystaREST.properties} file located in the classpath.
 * This file must specify:
 * <ul>
 *   <li>{@code RESTAPI_INTERFACE}: The network interface name for hosting the ReST API.</li>
 *   <li>{@code PARADIGMA_INTERFACE}: The network interface name for communicating with the SystaComfort unit.</li>
 *   <li>{@code RESTAPI_PORT}: The port number for the ReST API server.</li>
 * </ul>
 * The server resolves the IPv4 addresses for the specified interfaces. If configuration is missing
 * or interfaces are not found/up, the server will print error messages and exit.
 * </p>
 * <p>
 * Upon successful startup, it initializes a {@link FakeSystaWeb} instance (via a POST request
 * to its own {@code /SystaREST/start} endpoint) to begin listening for UDP packets from the
 * SystaComfort unit. The main thread then waits indefinitely (joins itself) until interrupted,
 * at which point it stops the HTTP server.
 * </p>
 */
public class SystaRESTServer {

    private static final String PROPERTIES_FILE_NAME = "SystaREST.properties";
    private static final String PROP_RESTAPI_INTERFACE = "RESTAPI_INTERFACE";
    private static final String PROP_PARADIGMA_INTERFACE = "PARADIGMA_INTERFACE";
    private static final String PROP_RESTAPI_PORT = "RESTAPI_PORT";
    private static final String SYSTAREST_START_PATH = "SystaREST/start";

    /**
     * Main entry point for the SystaREST server application.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Loads server configuration from {@value #PROPERTIES_FILE_NAME} located in the classpath.
     *       Exits if the file cannot be loaded.</li>
     *   <li>Retrieves network interface names for the ReST API and Paradigma communication
     *       from the loaded properties.</li>
     *   <li>Resolves the IPv4 addresses for these interfaces using {@link #getIPv4Address(String)}.
     *       Exits if either IP address cannot be resolved or if the interfaces are not up.</li>
     *   <li>Constructs the base URI for the HTTP server using the resolved ReST API IP address
     *       and the port number from properties.</li>
     *   <li>Configures Jersey {@link ResourceConfig} to register JAX-RS resource classes
     *       ({@link SystaRESTAPI}, {@link STouchRESTAPI}) and the {@link CorsFilter}.
     *       It also passes the resolved Paradigma interface IP to {@link SystaRESTAPI} as a property.</li>
     *   <li>Creates and starts a {@link JdkHttpServerFactory} based HTTP server.</li>
     *   <li>Sends an initial HTTP POST request to its own {@code /SystaREST/start} endpoint.
     *       This self-request is a mechanism to trigger the initialization and start of the
     *       {@link FakeSystaWeb} UDP listening service within the JAX-RS application context,
     *       ensuring it uses the correct configuration passed via {@code ResourceConfig}.</li>
     *   <li>The main thread then blocks by joining itself, keeping the server alive until
     *       an {@link InterruptedException} (e.g., from a SIGINT/Ctrl+C) occurs.</li>
     *   <li>Upon interruption, it stops the HTTP server gracefully.</li>
     * </ol>
     * Error messages are printed to {@code System.err} for critical failures, leading to application exit.
     * Informational messages about server lifecycle are printed to {@code System.out}.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        // Load configuration from SystaREST.properties
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        String defaultConfigPath = rootPath + PROPERTIES_FILE_NAME;
        System.out.println("[SystaRESTServer] Reading properties from file: " + defaultConfigPath);
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(defaultConfigPath)) {
            props.load(fis);
        } catch (IOException e1) {
            System.err.println("[SystaRESTServer] FATAL: Could not load properties file: " + defaultConfigPath + ". Error: " + e1.getMessage());
            System.exit(1);
            return; // Ensure exit for clarity, though System.exit(1) will terminate.
        }

        // Load interface names from properties
        String restApiInterfaceName = props.getProperty(PROP_RESTAPI_INTERFACE);
        String paradigmaInterfaceName = props.getProperty(PROP_PARADIGMA_INTERFACE);

        // Get IPv4 addresses for the configured interfaces
        String restApiIpV4 = getIPv4Address(restApiInterfaceName);
        String paradigmaIpV4 = getIPv4Address(paradigmaInterfaceName);

        // Validate that IP addresses were successfully resolved
        if (restApiIpV4 == null || restApiIpV4.isEmpty()) {
            System.err.println("[SystaRESTServer] FATAL: REST API interface '" + restApiInterfaceName + "' is not configured properly or IP address not found. Please check your configuration. Exiting.");
            System.exit(1);
            return;
        }
        if (paradigmaIpV4 == null || paradigmaIpV4.isEmpty()) {
            System.err.println("[SystaRESTServer] FATAL: Paradigma interface '" + paradigmaInterfaceName + "' is not configured properly or IP address not found. Please check your configuration. Exiting.");
            System.exit(1);
            return;
        }

        System.out.println("[SystaRESTServer] Using IP for ReST API: " + restApiIpV4);
        System.out.println("[SystaRESTServer] Using IP for Paradigma SystaComfort II communication: " + paradigmaIpV4);

        // Create and configure the JAX-RS ReST API server
        System.out.println("[SystaRESTServer] Starting HTTP server for ReST API...");
        URI baseUri = UriBuilder.fromUri("http://" + restApiIpV4 + "/")
                .port(Integer.parseInt(props.getProperty(PROP_RESTAPI_PORT)))
                .build();

        ResourceConfig resourceConfig = new ResourceConfig(SystaRESTAPI.class);
        resourceConfig.register(STouchRESTAPI.class); // Register the S-Touch API
        resourceConfig.register(new CorsFilter());   // Register CORS filter for cross-origin requests
        // Pass the resolved Paradigma IP to the SystaRESTAPI so it can configure FakeSystaWeb
        resourceConfig.property(SystaRESTAPI.PROP_PARADIGMA_IP, paradigmaIpV4);

        // Start the HTTP server using JDK's built-in HTTP server
        HttpServer server = JdkHttpServerFactory.createHttpServer(baseUri, resourceConfig, false);
        try {
            server.start();
            System.out.println("[SystaRESTServer] HTTP server started successfully at " + baseUri);
        } catch (Exception e) { // Catching generic Exception as server.start() doesn't declare specific checked exceptions
            System.err.println("[SystaRESTServer] FATAL: Failed to start HTTP server: " + e.getMessage());
            System.exit(1);
            return;
        }
        
        // After the server has started, send an initial POST request to its own /SystaREST/start endpoint.
        // This is a common pattern to trigger initialization logic within the JAX-RS application context,
        // specifically to start the FakeSystaWeb UDP listener which requires the ResourceConfig properties.
        System.out.println("[SystaRESTServer] Sending initial POST request to trigger FakeSystaWeb start: " + baseUri + SYSTAREST_START_PATH);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUri + SYSTAREST_START_PATH))
                    .POST(HttpRequest.BodyPublishers.noBody()) // Empty POST body
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 204 ) { // OK or No Content
                 System.out.println("[SystaRESTServer] Successfully triggered FakeSystaWeb start. Response status: " + response.statusCode());
            } else {
                 System.err.println("[SystaRESTServer] WARN: POST request to " + SYSTAREST_START_PATH + " returned status " + response.statusCode() + ". Body: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[SystaRESTServer] ERROR: Failed to send initial POST request to start FakeSystaWeb: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // Preserve interrupt status
            }
            // Depending on severity, might consider server.stop(0) and System.exit(1) here.
            // For now, server remains running but FakeSystaWeb might not be started.
        }

        // Keep the main server thread alive until interrupted (e.g., by Ctrl+C)
        System.out.println("[SystaRESTServer] Server running. Press Ctrl+C to stop.");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.err.println("[SystaRESTServer] Main thread interrupted. Shutting down server...");
            Thread.currentThread().interrupt(); // Preserve interrupt status
        } finally {
            // Stop the HTTP server gracefully
            server.stop(0); // 0 seconds delay
            System.out.println("[SystaRESTServer] HTTP server stopped.");
        }
    }

    /**
     * Retrieves the IPv4 address of a specified network interface.
     *
     * @param interfaceName The name of the network interface (e.g., "eth0", "en0").
     * @return The IPv4 address as a String if found and the interface is up,
     *         otherwise {@code null}.
     */
    private static String getIPv4Address(String interfaceName) {
        if (interfaceName == null || interfaceName.trim().isEmpty()) {
            System.err.println("[SystaRESTServer] getIPv4Address: Interface name is null or empty.");
            return null;
        }
        try {
            NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
            if (networkInterface == null) {
                System.err.println("[SystaRESTServer] getIPv4Address: Network interface '" + interfaceName + "' not found.");
                return null;
            }
            if (!networkInterface.isUp()) {
                System.err.println("[SystaRESTServer] getIPv4Address: Network interface '" + interfaceName + "' is down.");
                return null;
            }

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                    return addr.getHostAddress();
                }
            }
            System.err.println("[SystaRESTServer] getIPv4Address: No non-loopback IPv4 address found for interface '" + interfaceName + "'.");
        } catch (SocketException e) {
            System.err.println("[SystaRESTServer] getIPv4Address: SocketException while querying interface '" + interfaceName + "': " + e.getMessage());
        }
        return null;
    }
}
