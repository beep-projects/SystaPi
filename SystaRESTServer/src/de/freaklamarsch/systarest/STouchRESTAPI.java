/*
* Copyright (c) 2024 - 2025, The beep-projects contributors
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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import de.freaklamarsch.systarest.DeviceTouchSearch.DeviceTouchDeviceInfo;
import de.freaklamarsch.systarest.FakeSTouch.ConnectionStatus;
import de.freaklamarsch.systarest.FakeSTouchDisplay.DisplayButton;
import de.freaklamarsch.systarest.FakeSTouchDisplay.DisplayText;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.UriInfo;

/**
 * Provides a JAX-RS ReST API for emulating an S-Touch display application
 * and interacting with a Paradigma SystaComfort heating controller.
 * <p>
 * This API allows clients to:
 * <ul>
 *     <li>Discover SystaComfort units on the network.</li>
 *     <li>Connect to and disconnect from a discovered or specified unit.</li>
 *     <li>Simulate touch events on the emulated S-Touch screen.</li>
 *     <li>Retrieve the current state of the emulated screen as an image or a structured object tree.</li>
 *     <li>Execute automated sequences of actions (e.g., navigating menus, pressing buttons).</li>
 * </ul>
 * It relies on an underlying {@link FakeSTouch} instance to manage the S-Touch protocol communication
 * and screen state emulation.
 * </p>
 * <p>
 * The API is designed to be hosted within a JAX-RS compliant server environment, such as one
 * provided by {@link SystaRESTServer}.
 * </p>
 * <p>
 * Note on concurrency: This class uses a static field {@code fst} for the {@link FakeSTouch} instance.
 * While JAX-RS typically creates a new instance of this API class for each request, the underlying
 * {@code FakeSTouch} object is shared. It is assumed that {@link FakeSTouch} itself is designed
 * to be thread-safe for the operations exposed through this API. If {@code FakeSTouch} methods
 * are not thread-safe, external synchronization or a different instantiation strategy for
 * {@code FakeSTouch} would be required.
 * </p>
 */
@Path("{stouchrest : (?i)stouchrest}")
public class STouchRESTAPI {

    /**
     * The single, shared instance of the {@link FakeSTouch} device emulator.
     * Initialized on the first instantiation of {@link STouchRESTAPI}.
     * This static nature implies that all API requests operate on the same emulated device.
     * Concurrency considerations for {@link FakeSTouch} methods are important.
     */
    private static FakeSTouch fst = null;

    // Path constants for JAX-RS annotations
    private static final String PATH_CONNECT = "{connect : (?i)connect}";
    private static final String PATH_FIND_SYSTACOMFORT = "{findsystacomfort : (?i)findsystacomfort}";
    private static final String PATH_DISCONNECT = "{disconnect : (?i)disconnect}";
    private static final String PATH_TOUCH = "/touch"; // Query params for x, y
    private static final String PATH_SCREEN = "{screen : (?i)screen}";
    private static final String PATH_DEBUG_SCREEN = "{debugscreen : (?i)debugscreen}";
    private static final String PATH_OBJECT_TREE = "{objecttree : (?i)objecttree}";
    private static final String PATH_TOUCH_BUTTON = "{touchbutton : (?i)touchbutton}"; // Query param for id
    private static final String PATH_TOUCH_TEXT = "{touchtext : (?i)touchtext}"; // Query param for text
    private static final String PATH_AUTOMATION = "automation";


    /**
     * Default constructor. Initializes the static {@link FakeSTouch} instance
     * if it hasn't been created yet.
     */
    public STouchRESTAPI() {
        // Initialize fst only if it's null. This makes it a singleton for the application.
        if (fst == null) {
            fst = new FakeSTouch();
        }
    }

    /**
     * Establishes a connection to the previously specified or discovered SystaComfort unit.
     * The device information should be set beforehand using {@code /findsystacomfort} or
     * an equivalent mechanism if not relying on discovery.
     *
     * @return A {@link Response} indicating the outcome:
     *         <ul>
     *             <li>200 OK: Connection successful.</li>
     *             <li>401 Unauthorized: Incorrect UDP password.</li>
     *             <li>408 Request Timeout: Connection attempt timed out.</li>
     *             <li>409 Conflict: Device already in use, or already connected, or no compatible device found.</li>
     *             <li>500 Internal Server Error: An unexpected error occurred during connection.</li>
     *         </ul>
     */
    @POST
    @Path(PATH_CONNECT)
    public Response connect() {
        if (fst == null) { // Should not happen due to constructor, but defensive check
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("FakeSTouch service not initialized.").build();
        }
        ConnectionStatus state;
        try {
            state = fst.connect();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error during connect: " + e.getMessage());
            Thread.currentThread().interrupt(); // Preserve interrupt status if InterruptedException
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to initiate connection: " + e.getMessage()).build();
        }

        if (state == null) {
             return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unknown error during connection attempt.").build();
        }

        switch (state) {
            case SUCCESS:
                return Response.ok("Connection successful.").build();
            case DEVICE_ALREADY_IN_USE:
                return Response.status(Response.Status.CONFLICT).entity("Device is already in use by another client.").build();
            case WRONG_UDP_PASSWORD:
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Connection failed: Incorrect UDP password.").build();
            case TIMEOUT:
                return Response.status(Response.Status.REQUEST_TIMEOUT).entity("Connection attempt timed out.").build();
            case ALREADY_CONNECTED:
                return Response.status(Response.Status.CONFLICT).entity("Already connected to the device.").build();
            case NO_COMPATIBLE_DEVICE_FOUND:
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("No compatible SystaComfort device found.").build();
            default: // Should ideally not be reached if all ConnectionStatus cases are handled
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unknown connection error state: " + state).build();
        }
    }

    /**
     * Searches for a SystaComfort unit on the network and configures it as the target for subsequent operations.
     *
     * @return A {@link Response} containing:
     *         <ul>
     *             <li>200 OK: With JSON details of the discovered device if successful.</li>
     *             <li>503 Service Unavailable: If no compatible device is found.</li>
     *             <li>500 Internal Server Error: If an unexpected error occurs during search.</li>
     *         </ul>
     */
    @POST
    @Path(PATH_FIND_SYSTACOMFORT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchsystacomfort() {
        if (fst == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("FakeSTouch service not initialized.").build();
        }
        try {
            DeviceTouchDeviceInfo info = fst.searchSTouchDevice();
            if (info != null) {
                fst.setInfo(info); // Configure fst to use this discovered device
                JsonObject deviceInfoJson = Json.createObjectBuilder()
                        .add("SystaWebIP", info.localIp != null ? info.localIp : JsonValue.NULL)
                        .add("SystaWebPort", 22460) // Default port, consider making it dynamic if needed
                        .add("DeviceTouchBcastIP", info.bcastIp != null ? info.bcastIp : JsonValue.NULL)
                        .add("DeviceTouchBcastPort", info.bcastPort)
                        .add("deviceTouchInfoString", info.string != null ? info.string : JsonValue.NULL)
                        .add("unitIP", info.ip != null ? info.ip : JsonValue.NULL)
                        .add("unitName", info.name != null ? info.name : JsonValue.NULL)
                        .add("unitId", info.id != null ? info.id : JsonValue.NULL)
                        .add("unitApp", info.app)
                        .add("unitPlatform", info.platform)
                        .add("unitVersion", info.version != null ? info.version : JsonValue.NULL)
                        .add("unitMajor", info.major)
                        .add("unitMinor", info.minor)
                        .add("unitBaseVersion", info.baseVersion != null ? info.baseVersion : JsonValue.NULL)
                        .add("unitMac", info.mac != null ? info.mac : JsonValue.NULL)
                        .add("STouchAppSupported", info.stouchSupported)
                        .add("DeviceTouchPort", info.port)
                        .add("DeviceTouchPassword", info.password != null ? info.password : JsonValue.NULL)
                        .build();
                return Response.ok(deviceInfoJson).build();
            } else {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("No compatible SystaComfort device found on any interface.").build();
            }
        } catch (IOException e) {
            System.err.println("IOException during device search: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error during device search: " + e.getMessage()).build();
        }
    }

    /**
     * Disconnects from the currently connected SystaComfort unit.
     *
     * @return A {@link Response} indicating the outcome:
     *         <ul>
     *             <li>200 OK: Disconnection successful or was not connected.</li>
     *             <li>500 Internal Server Error: An error occurred during disconnection.</li>
     *         </ul>
     */
    @POST
    @Path(PATH_DISCONNECT)
    public Response disconnect() {
        if (fst == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("FakeSTouch service not initialized.").build();
        }
        try {
            fst.disconnect();
            return Response.ok("Disconnected successfully or was not connected.").build();
        } catch (IOException e) {
            System.err.println("IOException during disconnect: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Exception occurred during disconnect: " + e.getMessage()).build();
        }
    }

    /**
     * Simulates a touch event at the specified (x, y) coordinates on the emulated S-Touch screen.
     *
     * @param x The x-coordinate of the touch.
     * @param y The y-coordinate of the touch.
     * @return A {@link Response} 200 OK if the touch event was processed.
     *         Returns 500 if fst is not initialized.
     */
    @POST
    @Path(PATH_TOUCH)
    public Response touch(@QueryParam("x") int x, @QueryParam("y") int y) {
        if (fst == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("FakeSTouch service not initialized.").build();
        }
        fst.touch(x, y);
        return Response.ok("Touch event at (" + x + "," + y + ") processed.").build();
    }

    /**
     * Retrieves the current state of the emulated S-Touch screen as a PNG image.
     *
     * @return A {@link Response} containing:
     *         <ul>
     *             <li>200 OK: With the PNG image data if successful.</li>
     *             <li>500 Internal Server Error: If an error occurs during image generation or processing,
     *                                          or if the FakeSTouch service is not initialized.</li>
     *         </ul>
     */
    @GET
    @Path(PATH_SCREEN)
    @Produces("image/png")
    public Response getScreen() {
        if (fst == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("FakeSTouch service not initialized.").build();
        }
        try {
            BufferedImage image = fst.getScreenAsImage();
            if (image == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to retrieve screen image from emulator.").build();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageData = baos.toByteArray();
            return Response.ok(imageData).build();
        } catch (IOException e) {
            System.err.println("IOException in getScreen: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error processing screen image: " + e.getMessage()).build();
        }
    }

    /**
     * Provides an interactive HTML page for debugging. The page displays the current S-Touch screen image
     * and allows users to click on the image to simulate touch events. Clicked coordinates are
     * recorded and can be used to build automation sequences.
     *
     * @param listOfTouches A list of "x,y" coordinate strings from previous interactions, used to populate the click protocol.
     * @return A {@link Response} containing:
     *         <ul>
     *             <li>200 OK: With the HTML debug page.</li>
     *             <li>400 Bad Request: If coordinate parameters are malformed.</li>
     *             <li>500 Internal Server Error: If an error occurs generating the page or image,
     *                                          or if the FakeSTouch service is not initialized.</li>
     *         </ul>
     */
    @GET
    @Path(PATH_DEBUG_SCREEN)
    @Produces("text/html")
    public Response getDebugScreenWithHTML(@QueryParam("touch") List<String> listOfTouches) {
        if (fst == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("FakeSTouch service not initialized.").build();
        }
        List<int[]> touchProtocol = new ArrayList<>();
        if (listOfTouches != null) {
            try {
                touchProtocol = listOfTouches.stream().map(coordinates -> {
                    String[] parts = coordinates.split(",");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Invalid coordinate format: " + coordinates + ". Expected 'x,y'.");
                    }
                    try {
                        return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException("Invalid number in coordinate: " + coordinates, nfe);
                    }
                }).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing touch coordinates for debug screen: " + e.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Error parsing 'touch' query parameters: " + e.getMessage()).build();
            }
        }

        try {
            StringBuilder protocolHtml = new StringBuilder();
            for (int[] touchCoordinates : touchProtocol) {
                protocolHtml.append("        <li>touch?x=").append(touchCoordinates[0]).append("&amp;y=")
                            .append(touchCoordinates[1]).append("</li>\n");
            }

            BufferedImage image = fst.getScreenAsImage();
            if (image == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to retrieve screen image for debug page.").build();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64Image = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());

            // Formatted HTML string
            String html = String.join("\n",
                "<!DOCTYPE html>",
                "<html lang=\"en\">",
                "<head>",
                "    <meta charset=\"UTF-8\">",
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
                "    <title>SystaPi FakeSTouch Debug Screen</title>",
                "    <style>",
                "        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }",
                "        h3, h4 { color: #333; }",
                "        img { border: 2px solid #333; cursor: crosshair; background-color: #fff; }",
                "        #coordinates { margin-top: 10px; padding: 8px; background-color: #eee; border: 1px solid #ddd; display: inline-block; }",
                "        #protocol { margin-top: 20px; padding: 10px; background-color: #fff; border: 1px solid #ddd; max-height: 200px; overflow-y: auto; }",
                "        #protocol ul { list-style-type: none; padding-left: 0; }",
                "        #protocol li { padding: 3px 0; border-bottom: 1px dotted #ccc; }",
                "        #protocol li:last-child { border-bottom: none; }",
                "    </style>",
                "</head>",
                "<body>",
                "    <h3>FakeSTouch Interactive Screen</h3>",
                "    <img id=\"image\" src=\"data:image/png;base64," + base64Image + "\" alt=\"S-Touch Screen Image\">",
                "    <p id=\"coordinates\">Mouse Coordinates: (x: -, y: -)</p>",
                "    <div id=\"protocol\">",
                "        <h4>Click Protocol (appended to URL):</h4>",
                "        <ul id=\"protocolList\">",
                protocolHtml.toString(),
                "        </ul>",
                "    </div>",
                "    <script>",
                "        const image = document.getElementById('image');",
                "        const coordinatesDisplay = document.getElementById('coordinates');",
                "        image.addEventListener('mousemove', function(event) {",
                "            const rect = image.getBoundingClientRect();",
                "            const x = Math.round(event.clientX - rect.left);",
                "            const y = Math.round(event.clientY - rect.top);",
                "            coordinatesDisplay.textContent = 'Mouse Coordinates: (x: ' + x + ', y: ' + y + ')';",
                "        });",
                "        image.addEventListener('click', function(event) {",
                "            const rect = image.getBoundingClientRect();",
                "            const x = Math.round(event.clientX - rect.left);",
                "            const y = Math.round(event.clientY - rect.top);",
                "            fetch('/stouchrest/touch?x=' + x + '&y=' + y, { method: 'POST' })", // Assuming relative path to API base
                "            .then(response => {",
                "                if (!response.ok) { console.error('Touch command failed:', response.statusText); }",
                "                setTimeout(function() {", // Delay to allow backend processing before reload
                "                    const currentUrl = new URL(window.location.href);",
                "                    currentUrl.searchParams.append('touch', x + ',' + y);",
                "                    window.location.href = currentUrl.toString();",
                "                }, 500);", // Adjusted delay
                "            })",
                "            .catch(error => console.error('Error sending touch event:', error));",
                "        });",
                "        image.addEventListener('mouseleave', function() {",
                "            coordinatesDisplay.textContent = 'Mouse Coordinates: (x: -, y: -)';" + "        });",
                "    </script>",
                "</body>",
                "</html>"
            );
            return Response.ok(html).build();
        } catch (IOException e) {
            System.err.println("IOException generating debug screen: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error processing image for debug screen: " + e.getMessage()).build();
        }
    }

    /**
     * Retrieves the emulated S-Touch screen's current object tree (buttons, text elements) as a JSON string.
     *
     * @return A {@link Response} 200 OK with the JSON string representing the screen's object tree.
     *         Returns 500 if fst is not initialized.
     */
    @GET
    @Path(PATH_OBJECT_TREE)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectTree() {
        if (fst == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("FakeSTouch instance not initialized.").build();
        }
        return Response.ok(fst.getObjectTree()).build();
    }

    /**
     * Simulates pressing a button on the S-Touch screen identified by its ID.
     *
     * @param buttonId The ID of the button to press.
     * @return A {@link Response} indicating the outcome:
     *         <ul>
     *             <li>200 OK: If the button was found and pressed.</li>
     *             <li>404 Not Found: If no button with the given ID exists on the screen.</li>
     *             <li>500 Internal Server Error: If an unexpected error occurs or fst is not initialized.</li>
     *         </ul>
     */
    @POST
    @Path(PATH_TOUCH_BUTTON)
    public Response touchButton(@QueryParam("id") byte buttonId) {
        if (fst == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("FakeSTouch instance not initialized.").build();
        }
        try {
            boolean success = fst.getDisplay().pushButton(buttonId);
            if (success) {
                return Response.ok("Button " + buttonId + " pressed successfully.").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("Button with ID " + buttonId + " not found on the current screen.").build();
            }
        } catch (Exception e) { // Catch a broader range of exceptions from pushButton if any
            System.err.println("Error in touchButton (ID: " + buttonId + "): " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error while attempting to press button " + buttonId + ": " + e.getMessage()).build();
        }
    }

    /**
     * Simulates a touch event on the first occurrence of the specified text on the S-Touch screen.
     *
     * @param text The text content to find and "touch".
     * @return A {@link Response} indicating the outcome:
     *         <ul>
     *             <li>200 OK: If the text was found and touched.</li>
     *             <li>400 Bad Request: If the text parameter is null or empty.</li>
     *             <li>404 Not Found: If the text is not found on the screen.</li>
     *             <li>500 Internal Server Error: If an unexpected error occurs or fst is not initialized.</li>
     *         </ul>
     */
    @POST
    @Path(PATH_TOUCH_TEXT)
    public Response touchText(@QueryParam("text") String text) {
        if (fst == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("FakeSTouch instance not initialized.").build();
        }
        if (text == null || text.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Text parameter cannot be null or empty.").build();
        }
        try {
            boolean success = fst.getDisplay().touchText(text);
            if (success) {
                return Response.ok("Text \"" + text + "\" touched successfully.").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Text \"" + text + "\" not found on the current screen.").build();
            }
        } catch (Exception e) { // Catch a broader range of exceptions
            System.err.println("Error in touchText (text: \"" + text + "\"): " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error while attempting to touch text \"" + text + "\": " + e.getMessage()).build();
        }
    }

    /**
	 * Executes a sequence of commands provided as query parameters.
	 *
	 * @param queryParams the commands to execute. Supported commands are: connect
	 *                    connect to the SystaComfort unit touch=x,y emulate a touch
	 *                    event on screen coordinates x and y touchText=text emulate
	 *                    a touch event on the given text, if it is in the object
	 *                    tree of the screen touchButton=id emulate a touch event on
	 *                    the button with the given id, if it is in the object tree
	 *                    of the screen whileText=text&amp;doAction while the given
	 *                    text is in the object tree of the screen, do the given
	 *                    action whileButton=id&amp;doAction while the button with
	 *                    the given id is in the object tree of the screen, do the
	 *                    given action
	 *                    checkText=text&amp;theDoThisAction&amp;elseDoThisAction if
	 *                    the given text is in the object tree of the screen, then
	 *                    do the first action, else do the second
	 *                    checkButton=id&amp;theDoThisAction&amp;elseDoThisAction if
	 *                    the button with the given id is in the object tree of the
	 *                    screen, then do the first action, else do the second
	 *                    disconnect disconnect from the SystaComfort unit
	 * @return a {@link Response} indicating the result of the automation
     * Executes a sequence of S-Touch commands provided as query parameters.
     * This endpoint allows for automation of interactions with the emulated S-Touch device.
     * <p>
     * Commands are specified as key-value pairs in the query string, separated by '&amp;'.
     * The order of commands in the query string determines their execution order.
     * Example: {@code /automation?connect&touch=100,150&touchtext=Settings&disconnect}
     * </p>
     * <p>Supported commands:</p>
     * <ul>
     *     <li>{@code connect}: Connects to the SystaComfort unit.</li>
     *     <li>{@code touch=x,y}: Simulates a touch at screen coordinates (x,y).</li>
     *     <li>{@code touchtext=sample%20text}: Finds "sample text" on screen and simulates a touch on its center.</li>
     *     <li>{@code touchbutton=id}: Simulates a press on the button with the given numeric ID.</li>
     *     <li>{@code whiletext==text&doaction=...}: While "text" IS visible, executes the subsequent 'doaction'.</li>
     *     <li>{@code whiletext!=text&doaction=...}: While "text" IS NOT visible, executes the subsequent 'doaction'.</li>
     *     <li>{@code whilebutton==id&doaction=...}: While button 'id' IS visible, executes 'doaction'.</li>
     *     <li>{@code whilebutton!=id&doaction=...}: While button 'id' IS NOT visible, executes 'doaction'.</li>
     *     <li>{@code checktext==text&thenaction=...&elseaction=...}: If "text" IS visible, executes 'thenaction', otherwise 'elseaction'. (elseaction is optional)</li>
     *     <li>{@code checktext!=text&thenaction=...&elseaction=...}: If "text" IS NOT visible, executes 'thenaction', otherwise 'elseaction'. (elseaction is optional)</li>
     *     <li>{@code checkbutton==id&thenaction=...&elseaction=...}: If button 'id' IS visible, executes 'thenaction', otherwise 'elseaction'.</li>
     *     <li>{@code checkbutton!=id&thenaction=...&elseaction=...}: If button 'id' IS NOT visible, executes 'thenaction', otherwise 'elseaction'.</li>
     *     <li>{@code disconnect}: Disconnects from the SystaComfort unit.</li>
     *     <li>{@code none}: No operation, can be used as a placeholder in conditional actions.</li>
     * </ul>
     * A 2-second delay is automatically inserted after each successfully executed command to allow the device state to update.
     *
     * @param uriInfo Contextual URI information, used to extract query parameters.
     * @return A {@link Response} indicating the overall result:
     *         <ul>
     *             <li>200 OK: If all commands executed successfully.</li>
     *             <li>400 Bad Request: If a command or its parameters are malformed.</li>
     *             <li>500 Internal Server Error: If a command execution fails or an unexpected error occurs.</li>
     *         </ul>
     */
    @GET
    @Path(PATH_AUTOMATION)
    public Response automation(@Context UriInfo uriInfo) {
        if (fst == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("FakeSTouch instance not initialized.").build();
        }
        String query = uriInfo.getRequestUri().getRawQuery(); // Use raw query to preserve encoding of values
        if (query == null || query.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("No automation commands provided.").build();
        }

        // Query parameters are split by '&'. Each part is a command or a key-value pair.
        String[] commandArray = query.split("&");
        boolean skipNextAction = false; // Used by checkText/checkButton to skip the 'else' or 'then' part

        for (int i = 0; i < commandArray.length; i++) {
            String fullCommandString = commandArray[i];
            if (skipNextAction) {
                printDebugInfo("Skipping action due to previous conditional: " + fullCommandString);
                skipNextAction = false; // Reset for the next independent command
                continue;
            }

            String commandKey;
            String commandValue = null;
            int equalsIndex = fullCommandString.indexOf('=');
            if (equalsIndex != -1) {
                commandKey = fullCommandString.substring(0, equalsIndex).toLowerCase(); // Command keys are case-insensitive
                commandValue = fullCommandString.substring(equalsIndex + 1); // Value remains case-sensitive and URL-encoded
            } else {
                commandKey = fullCommandString.toLowerCase();
            }
            
            // Decode commandValue here if it's generally expected to be URL decoded
            // For now, specific handlers will decode if necessary (e.g. for text search)

            printDebugInfo("Executing automation step: " + commandKey + (commandValue != null ? "=" + commandValue : ""));

            Response commandResponse = null;
            try {
                // Conditional commands: whileText, checkText, whileButton, checkButton
                if (commandKey.startsWith(CMD_WHILE_TEXT)) {
                    // Expects: whiletext==<text>&doaction=<action_command_string> OR whiletext!=<text>&doaction=<action_command_string>
                    // The 'doaction' is the next command in commandArray
                    if (i + 1 >= commandArray.length || !commandArray[i+1].toLowerCase().startsWith("doaction=")) {
                        return Response.status(Response.Status.BAD_REQUEST).entity("Missing 'doaction' for " + commandKey).build();
                    }
                    String actionCommandString = commandArray[i+1].substring("doaction=".length());
                    commandResponse = automationWhileTextCommand(commandKey, commandValue, actionCommandString);
                    i++; // Consume the doaction part
                } else if (commandKey.startsWith(CMD_CHECK_TEXT)) {
                    // Expects: checktext==<text>&thenaction=<action>&elseaction=<action>
                    // elseaction is optional. thenaction is required.
                    String thenActionString = null;
                    String elseActionString = CMD_NONE; // Default to no-op if elseaction is missing
                    if (i + 1 < commandArray.length && commandArray[i+1].toLowerCase().startsWith("thenaction=")) {
                        thenActionString = commandArray[i+1].substring("thenaction=".length());
                        i++; // Consume thenaction
                        if (i + 1 < commandArray.length && commandArray[i+1].toLowerCase().startsWith("elseaction=")) {
                            elseActionString = commandArray[i+1].substring("elseaction=".length());
                            i++; // Consume elseaction
                        }
                    } else {
                        return Response.status(Response.Status.BAD_REQUEST).entity("Missing 'thenaction' for " + commandKey).build();
                    }
                    skipNextAction = !automationCheckTextLogic(commandKey, commandValue, thenActionString, elseActionString);
                    commandResponse = Response.ok("Conditional text check processed.").build(); // Intermediate success
                } else if (commandKey.startsWith(CMD_WHILE_BUTTON)) {
                     if (i + 1 >= commandArray.length || !commandArray[i+1].toLowerCase().startsWith("doaction=")) {
                        return Response.status(Response.Status.BAD_REQUEST).entity("Missing 'doaction' for " + commandKey).build();
                    }
                    String actionCommandString = commandArray[i+1].substring("doaction=".length());
                    commandResponse = automationWhileButtonCommand(commandKey, commandValue, actionCommandString);
                    i++;
                } else if (commandKey.startsWith(CMD_CHECK_BUTTON)) {
                    String thenActionString = null;
                    String elseActionString = CMD_NONE;
                     if (i + 1 < commandArray.length && commandArray[i+1].toLowerCase().startsWith("thenaction=")) {
                        thenActionString = commandArray[i+1].substring("thenaction=".length());
                        i++;
                        if (i + 1 < commandArray.length && commandArray[i+1].toLowerCase().startsWith("elseaction=")) {
                            elseActionString = commandArray[i+1].substring("elseaction=".length());
                            i++;
                        }
                    } else {
                         return Response.status(Response.Status.BAD_REQUEST).entity("Missing 'thenaction' for " + commandKey).build();
                    }
                    skipNextAction = !automationCheckButtonLogic(commandKey, commandValue, thenActionString, elseActionString);
                    commandResponse = Response.ok("Conditional button check processed.").build();
                } else {
                    // Direct commands
                    commandResponse = automationExecuteDirectCommand(commandKey, commandValue);
                }

                if (commandResponse != null && commandResponse.getStatus() != Response.Status.OK.getStatusCode()) {
                    return Response.status(commandResponse.getStatus()) // Propagate specific error status
                            .entity("Command failed: " + fullCommandString + ". Error: " + commandResponse.getEntity()).build();
                }
                // Only sleep if the command was not a conditional check that itself handles flow
                if (!commandKey.startsWith(CMD_CHECK_TEXT) && !commandKey.startsWith(CMD_CHECK_BUTTON)) {
                    sleepForTwoSeconds(); // Allow time for UI to update or actions to complete
                }

            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid parameter for command " + fullCommandString + ": " + e.getMessage()).build();
            } catch (Exception e) { // Catch any other unexpected errors during command execution
                 System.err.println("Automation error for command '" + fullCommandString + "': " + e.getMessage());
                 return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error executing command " + fullCommandString + ": " + e.getMessage()).build();
            }
        }
        return Response.ok("Automation sequence executed successfully.").build();
    }

    /**
     * Handles 'whiletext' automation command.
     * Loops execution of an action while a text condition (exists or not exists) is met.
     *
     * @param whileTextCommandKey The full "whiletext==text" or "whiletext!=text" command string.
     * @param textToSearch The text value to search for (URL decoded).
     * @param actionToExecute The command string for the action to execute in each loop iteration.
     * @return {@code Response.ok()} if loop completes, or an error Response if sub-command fails.
     */
    private Response automationWhileTextCommand(String whileTextCommandKey, String textToSearch, String actionToExecute) {
        // textToSearch is already URL decoded by JAX-RS if extracted from @QueryParam,
        // but if from raw query, it might need decoding. Assuming value from raw query needs decoding.
        String decodedSearchText = decodeURL(textToSearch);
        boolean conditionIsEquality = whileTextCommandKey.equals(CMD_WHILE_TEXT + "=="); // Check if it's whiletext== or whiletext!=
        
        int maxLoops = 100; // Safety break for while loop
        int loopCount = 0;

        printDebugInfo("Entering whileText loop: conditionIsEquality=" + conditionIsEquality + ", searchText='" + decodedSearchText + "', action='" + actionToExecute + "'");

        DisplayText foundText = STouchRESTAPI.fst.getDisplay().findTextInObjectTree(decodedSearchText);
        // Loop condition:
        // - If conditionIsEquality (whiletext==), loop while foundText IS NOT NULL.
        // - If !conditionIsEquality (whiletext!=), loop while foundText IS NULL.
        while (loopCount < maxLoops && (conditionIsEquality == (foundText != null))) {
            printDebugInfo("whileText loop iteration " + (loopCount+1) + ". Condition met. Executing action: " + actionToExecute);
            Response actionResponse = automationExecuteDirectCommand(actionToExecute, null); // Assuming actionToExecute is a simple command key for now
            if (actionResponse.getStatus() != Response.Status.OK.getStatusCode()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Action '" + actionToExecute + "' in whileText loop failed: " + actionResponse.getEntity()).build();
            }
            sleepForTwoSeconds(); // Allow UI to update
            foundText = STouchRESTAPI.fst.getDisplay().findTextInObjectTree(decodedSearchText); // Re-check condition
            loopCount++;
        }
        if(loopCount == maxLoops) {
            printDebugInfo("whileText loop exited due to max iterations.");
            return Response.status(Response.Status.REQUEST_TIMEOUT).entity("whileText loop for '" + decodedSearchText + "' timed out after " + maxLoops + " iterations.").build();
        }
        printDebugInfo("Exited whileText loop for '" + decodedSearchText + "'.");
        return Response.ok("whileText command processed.").build();
    }

    /**
     * Handles 'checktext' automation command logic.
     * Executes one of two actions based on whether a text condition (exists or not exists) is met.
     *
     * @param checkTextCommandKey The full "checktext==text" or "checktext!=text" command string.
     * @param textValue The text value to search for (URL decoded).
     * @param thenAction The command string for the action if condition is true.
     * @param elseAction The command string for the action if condition is false.
     * @return {@code true} if the 'then' action was taken (or should be taken next), {@code false} if 'else' action.
     */
    private boolean automationCheckTextLogic(String checkTextCommandKey, String textValue, String thenAction, String elseAction) {
        String decodedTextValue = decodeURL(textValue);
        boolean conditionIsEquality = checkTextCommandKey.equals(CMD_CHECK_TEXT + "==");
        DisplayText foundText = STouchRESTAPI.fst.getDisplay().findTextInObjectTree(decodedTextValue);
        boolean conditionMet = (conditionIsEquality == (foundText != null));

        printDebugInfo("checkText: conditionIsEquality=" + conditionIsEquality + ", searchText='" + decodedTextValue + "', conditionMet=" + conditionMet);

        Response actionResponse;
        if (conditionMet) {
            printDebugInfo("checkText: Condition met, executing 'then' action: " + thenAction);
            actionResponse = automationExecuteDirectCommand(thenAction, null); // Assuming simple command key
        } else {
            printDebugInfo("checkText: Condition NOT met, executing 'else' action: " + elseAction);
            actionResponse = automationExecuteDirectCommand(elseAction, null); // Assuming simple command key
        }
        if (actionResponse.getStatus() != Response.Status.OK.getStatusCode()) {
            // This is tricky, how to propagate this error? For now, log it.
            // The main loop will see skipNextAction and might not execute the intended subsequent command.
            System.err.println("Error executing action in checkText: " + actionResponse.getEntity());
            // Potentially throw an exception or return a specific error state to main automation loop.
        }
        return conditionMet; // True if 'then' path was taken.
    }


    /**
     * Handles 'whilebutton' automation command.
     * Loops execution of an action while a button condition (exists or not exists) is met.
     * @param whileButtonCommandKey The command key (e.g., "whilebutton==" or "whilebutton!=").
     * @param buttonIdString The ID of the button to check, as a string.
     * @param actionToExecute The command string for the action to execute.
     * @return Response indicating success or failure of the loop or sub-commands.
     */
    private Response automationWhileButtonCommand(String whileButtonCommandKey, String buttonIdString, String actionToExecute) {
        Byte searchId = Byte.parseByte(decodeURL(buttonIdString)); // Ensure button ID is decoded
        boolean conditionIsEquality = whileButtonCommandKey.equals(CMD_WHILE_BUTTON + "==");
        
        int maxLoops = 100; // Safety break
        int loopCount = 0;
        printDebugInfo("Entering whileButton loop: conditionIsEquality=" + conditionIsEquality + ", buttonId=" + searchId + ", action='" + actionToExecute + "'");

        DisplayButton foundButton = STouchRESTAPI.fst.getDisplay().findButtonInObjectTree(searchId);
        while (loopCount < maxLoops && (conditionIsEquality == (foundButton != null))) {
            printDebugInfo("whileButton loop iteration " + (loopCount+1) + ". Condition met. Executing action: " + actionToExecute);
            Response actionResponse = automationExecuteDirectCommand(actionToExecute, null);
            if (actionResponse.getStatus() != Response.Status.OK.getStatusCode()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Action '" + actionToExecute + "' in whileButton loop failed: " + actionResponse.getEntity()).build();
            }
            sleepForTwoSeconds();
            foundButton = STouchRESTAPI.fst.getDisplay().findButtonInObjectTree(searchId);
            loopCount++;
        }
         if(loopCount == maxLoops) {
            printDebugInfo("whileButton loop exited due to max iterations.");
            return Response.status(Response.Status.REQUEST_TIMEOUT).entity("whileButton loop for ID " + searchId + " timed out after " + maxLoops + " iterations.").build();
        }
        printDebugInfo("Exited whileButton loop for ID " + searchId);
        return Response.ok("whileButton command processed.").build();
    }

    /**
     * Handles 'checkbutton' automation command logic.
     * @param checkButtonCommandKey The command key.
     * @param buttonIdString The button ID string.
     * @param thenAction The action if condition true.
     * @param elseAction The action if condition false.
     * @return True if 'then' path taken, false otherwise.
     */
    private boolean automationCheckButtonLogic(String checkButtonCommandKey, String buttonIdString, String thenAction, String elseAction) {
        Byte searchId = Byte.parseByte(decodeURL(buttonIdString));
        boolean conditionIsEquality = checkButtonCommandKey.equals(CMD_CHECK_BUTTON + "==");
        DisplayButton foundButton = STouchRESTAPI.fst.getDisplay().findButtonInObjectTree(searchId);
        boolean conditionMet = (conditionIsEquality == (foundButton != null));

        printDebugInfo("checkButton: conditionIsEquality=" + conditionIsEquality + ", buttonId=" + searchId + ", conditionMet=" + conditionMet);
        
        Response actionResponse;
        if (conditionMet) {
            printDebugInfo("checkButton: Condition met, executing 'then' action: " + thenAction);
            actionResponse = automationExecuteDirectCommand(thenAction, null);
        } else {
            printDebugInfo("checkButton: Condition NOT met, executing 'else' action: " + elseAction);
            actionResponse = automationExecuteDirectCommand(elseAction, null);
        }
        if (actionResponse.getStatus() != Response.Status.OK.getStatusCode()) {
            System.err.println("Error executing action in checkButton: " + actionResponse.getEntity());
        }
        return conditionMet;
    }

    /**
     * Extracts the comparison value from a command string (e.g., "whiletext==My Text" -> "My Text").
     * This method assumes the command format is "prefix==value" or "prefix!=value".
     *
     * @param command The full command string.
     * @param prefix  The command prefix (e.g., "whiletext").
     * @return The extracted value part of the command.
     */
    private String extractComparisonValue(String command, String prefix) {
        // Handles "prefix==value" and "prefix!=value"
        // Length of "==" or "!=" is 2.
        return command.substring(prefix.length() + 2);
    }
    
    /**
     * Extracts a byte value from a command string (e.g., "touchbutton=123" -> 123).
     * Assumes format "prefix=value".
     * @param command Full command string.
     * @param prefix Command prefix.
     * @return Parsed byte value.
     * @throws NumberFormatException if value is not a valid byte.
     */
    private Byte extractByteValue(String command, String prefix) {
        return Byte.parseByte(command.substring(prefix.length() + 1));
    }


    /**
     * Utility method to introduce a fixed delay.
     * Used in automation to allow the emulated device or UI to update.
     * Throws a {@link RuntimeException} if interrupted.
     */
    private void sleepForTwoSeconds() {
        try {
            Thread.sleep(2000); // 2-second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve interrupt status
            // Wrap in a RuntimeException as this is not typically expected to be handled by callers here
            throw new RuntimeException("Automation sleep interrupted.", e);
        }
    }

    /**
     * Executes a single, direct automation command (not a conditional or loop).
     *
     * @param commandKey The key part of the command (e.g., "connect", "touch").
     * @param commandValue The value part of the command (e.g., "100,150" for touch), can be null.
     * @return A JAX-RS {@link Response} indicating the outcome of the command.
     */
    private Response automationExecuteDirectCommand(String commandKey, String commandValue) {
        // Ensure commandKey is lowercased for consistent matching
        String lowerCommandKey = commandKey.toLowerCase();
        String decodedValue = commandValue != null ? decodeURL(commandValue) : null;

        if (lowerCommandKey.equals(CMD_NONE)) {
            return Response.ok("No operation executed.").build();
        } else if (lowerCommandKey.equals(CMD_CONNECT)) {
            return connect();
        } else if (lowerCommandKey.equals(CMD_TOUCH_BUTTON)) {
            if (decodedValue == null) return Response.status(Response.Status.BAD_REQUEST).entity("Missing button ID for " + commandKey).build();
            return touchButton(Byte.parseByte(decodedValue));
        } else if (lowerCommandKey.equals(CMD_TOUCH_TEXT)) {
            if (decodedValue == null) return Response.status(Response.Status.BAD_REQUEST).entity("Missing text for " + commandKey).build();
            return touchText(decodedValue);
        } else if (lowerCommandKey.equals(CMD_TOUCH)) {
            if (decodedValue == null) return Response.status(Response.Status.BAD_REQUEST).entity("Missing coordinates for " + commandKey).build();
            String[] coordinates = decodedValue.split(",");
            if (coordinates.length != 2) return Response.status(Response.Status.BAD_REQUEST).entity("Invalid coordinate format for " + commandKey + ": " + decodedValue).build();
            try {
                int x = Integer.parseInt(coordinates[0].trim());
                int y = Integer.parseInt(coordinates[1].trim());
                return touch(x, y);
            } catch (NumberFormatException e) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid number in coordinates for " + commandKey + ": " + decodedValue).build();
            }
        } else if (lowerCommandKey.equals(CMD_DISCONNECT)) {
            return disconnect();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Unknown direct command: " + commandKey).build();
        }
    }
    
    /**
     * Helper to URL decode a string value.
     * @param value The value to decode.
     * @return The decoded string, or original if decoding fails.
     */
    private String decodeURL(String value) {
        if (value == null) return null;
        try {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            // Should not happen with UTF-8
            System.err.println("Error URL decoding value '" + value + "': " + e.getMessage());
            return value; // Fallback to original value
        }
    }
}
