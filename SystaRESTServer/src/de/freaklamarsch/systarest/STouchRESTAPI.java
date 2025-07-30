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
import jakarta.ws.rs.core.UriInfo;

/**
 * A REST API for emulating the S-Touch app to interact with the Paradigma
 * SystaComfort system. This API provides endpoints for connecting to the
 * device, simulating touch events, retrieving the screen state, and automating
 * sequences of actions. This class is intended to be run by a
 * {@link SystaRESTServer}
 */
@Path("{stouchrest : (?i)stouchrest}")
public class STouchRESTAPI {

	/** The instance of the FakeSTouch device used for emulation. */
	private static FakeSTouch fst = null;

	public STouchRESTAPI() {
		if (fst == null) {
			fst = new FakeSTouch();
		}
	}

	/**
	 * Establishes a connection to the SystaComfort unit.
	 *
	 * @return a {@link Response} indicating the result of the connection attempt
	 */
	@POST
	@Path("{connect : (?i)connect}")
	public Response connect() {
		ConnectionStatus state = null;
		try {
			state = fst.connect();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		switch (state) {
		case SUCCESS:
			return Response.ok().build();
		case DEVICE_ALREADY_IN_USE:
			return Response.status(Response.Status.CONFLICT).entity("Device is already in use").build();
		case WRONG_UDP_PASSWORD:
			return Response.status(Response.Status.UNAUTHORIZED)
					.entity("Error when establishing connection. Wrong UDP password").build();
		case TIMEOUT:
			return Response.status(Response.Status.REQUEST_TIMEOUT).entity("Connection request timed out").build();
		case ALREADY_CONNECTED:
			return Response.status(Response.Status.CONFLICT).entity("Device is already connected").build();
		case NO_COMPATIBLE_DEVICE_FOUND:
			return Response.status(Response.Status.CONFLICT).entity("No compatible device found").build();
		default:
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unknown error").build();
		}
	}

	@POST
	@Path("{findsystacomfort : (?i)findsystacomfort}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchsystacomfort() {
		DeviceTouchDeviceInfo info = fst.searchSTouchDevice();
		if (info != null) {
			fst.setInfo(info);
			JsonObject jo = Json.createObjectBuilder().add("SystaWebIP", info.localIp).add("SystaWebPort", 22460)
					.add("DeviceTouchBcastIP", info.bcastIp).add("DeviceTouchBcastPort", info.bcastPort)
					.add("deviceTouchInfoString", info.string).add("unitIP", info.ip).add("unitName", info.name)
					.add("unitId", info.id).add("unitApp", info.app).add("unitPlatform", info.platform)
					.add("unitVersion", info.version).add("unitMajor", info.major).add("unitMinor", info.minor)
					.add("unitBaseVersion", info.baseVersion).add("unitMac", info.mac)
					.add("STouchAppSupported", info.stouchSupported).add("DeviceTouchPort", info.port)
					.add("DeviceTouchPassword", (info.password == null) ? "null" : info.password).build();
			return Response.ok(jo).build();
		} else {
			return Response.status(Response.Status.SERVICE_UNAVAILABLE)
					.entity("STouchRESTAPI service not started: No compatible device found on any interface").build();
		}

	}

	/**
	 * Disconnects from the SystaComfort unit.
	 *
	 * @return a {@link Response} indicating the result of the disconnection attempt
	 */
	@POST
	@Path("{disconnect : (?i)disconnect}")
	public Response disconnect() {
		try {
			fst.disconnect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Exception occurred when sending disconnect message").build();
		}
		return Response.ok().build();
	}

	/**
	 * Simulates a touch event at the specified coordinates on the screen.
	 *
	 * @param x the x-coordinate of the touch
	 * @param y the y-coordinate of the touch
	 * @return a {@link Response} indicating the result of the touch event
	 */
	@POST
	@Path("{touch : (?i)touch}")
	public Response touch(@QueryParam("x") int x, @QueryParam("y") int y) {
		fst.touch(x, y);
		return Response.ok().build();
	}

	/**
	 * Retrieves the current screen as a PNG image.
	 *
	 * @return a {@link Response} containing the screen image
	 */
	@GET
	@Path("{screen : (?i)screen}")
	@Produces("image/png")
	public Response getScreen() {
		try {
			BufferedImage image = fst.getScreenAsImage();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageData = baos.toByteArray();

			return Response.ok(imageData).build();
		} catch (IOException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error processing image").build();
		}
	}

	/**
	 * Returns an interactive HTML page for debugging touch events on the screen.
	 *
	 * @return a {@link Response} containing the HTML page
	 */
	@GET
	@Path("{debugscreen : (?i)debugscreen}")
	@Produces("text/html")
	public Response getDebugScreenWithHTML(@QueryParam("touch") List<String> listOfTouches) {
		List<int[]> touchProtocol = new ArrayList<>();
		try {
			touchProtocol = listOfTouches.stream().map(coordinates -> {
				String[] parts = coordinates.split(",");
				if (parts.length != 2) {
					throw new IllegalArgumentException("Invalid coordinate format: " + coordinates);
				}
				return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
			}).collect(Collectors.toList());
		} catch (IllegalArgumentException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Error parsing coordinates: " + e.getMessage())
					.build();
		}

		try {
			// Build the protocol list in HTML
			StringBuilder protocolHtml = new StringBuilder();
			for (int[] touchCoordinates : touchProtocol) {
				protocolHtml.append("<li>").append("touch?x=").append(touchCoordinates[0]).append("&y=")
						.append(touchCoordinates[1]).append("</li>");
			}

			// Convert the image to Base64 format to embed in the HTML
			BufferedImage image = fst.getScreenAsImage();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageData = baos.toByteArray();
			String base64Image = java.util.Base64.getEncoder().encodeToString(imageData);

			// Build the HTML content as a plain String
			String html = "<!DOCTYPE html>" + "<html lang=\"en\">" + "<head>" + "<meta charset=\"UTF-8\">"
					+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
					+ "<title>SystaPi FakeSTouch</title>" + "<style>" + "img {" + "border: 1px solid black;" + "}"
					+ "#coordinates {" + "margin-top: 10px;" + "font-family: Arial, sans-serif;" + "}" + "#protocol {"
					+ "margin-top: 20px;" + "font-family: Arial, sans-serif;" + "}" + "</style>" + "</head>" + "<body>"
					+ "<h3>FakeSTouch Screen</h3>" + "<img id=\"image\" src=\"data:image/png;base64," + base64Image
					+ "\" alt=\"Screen Image\">" + "<p id=\"coordinates\">Coordinates: (x: -, y: -)</p>"
					+ "<div id=\"protocol\">" + "<h4>Click Protocol:</h4>" + "<ul id=\"protocolList\">"
					+ protocolHtml.toString() + "</ul>" + "</div>" + "<script>"
					+ "const image = document.getElementById('image');"
					+ "const coordinates = document.getElementById('coordinates');"
					+ "image.addEventListener('mousemove', function(event) {"
					+ "const rect = image.getBoundingClientRect();" + "const x = event.clientX - rect.left;"
					+ "const y = event.clientY - rect.top;"
					+ "coordinates.textContent = 'Coordinates: (x: ' + Math.round(x) + ', y: ' + Math.round(y) + ')';"
					+ "});" + "image.addEventListener('click', function(event) {"
					+ "const rect = image.getBoundingClientRect();" + "const x = Math.round(event.clientX - rect.left);"
					+ "const y = Math.round(event.clientY - rect.top);"
					+ "fetch('/stouchrest/touch?x=' + x + '&y=' + y, {" + "method: 'POST'" + "}).then(function() {"
					+ "setTimeout(function() {" + "const currentUrl = new URL(window.location.href);"
					+ "currentUrl.searchParams.append('touch', x + ',' + y);"
					+ "window.location.href = currentUrl.toString();" + "}, 1000);" + "}).catch(function(error) {"
					+ "console.error('Error sending coordinates:', error);" + "});" + "});"
					+ "image.addEventListener('mouseleave', function() {"
					+ "coordinates.textContent = 'Coordinates: (x: -, y: -)';" + "});" + "</script>" + "</body>"
					+ "</html>";

			// Return the HTML response
			return Response.ok(html).build();

		} catch (IOException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error processing image").build();
		}
	}

	@GET
	@Path("{objecttree : (?i)objecttree}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getObjectTree() {
		return Response.ok(fst.getObjectTree()).build();
	}

	@GET
	@Path("{hasbutton : (?i)hasbutton}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response hasButton(@QueryParam("id") byte buttonId) {
		DisplayButton foundButton = STouchRESTAPI.fst.getDisplay().findButtonInObjectTree(buttonId);
		return Response.ok(Json.createObjectBuilder().add("result", foundButton != null).build()).build();
	}

	@POST
	@Path("{touchbutton : (?i)touchbutton}")
	public Response touchButton(@QueryParam("id") byte buttonId) {
		try {
			boolean success = fst.getDisplay().pushButton(buttonId);
			if (success) {
				return Response.ok("Button " + buttonId + " pressed successfully").build();
			} else {
				return Response.status(Response.Status.NOT_FOUND).entity("Button with ID " + buttonId + " not found")
						.build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error while pressing button: " + e.getMessage()).build();
		}
	}

	@GET
	@Path("{hastext : (?i)hastext}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response hasText(@QueryParam("text") String text) {
		DisplayText foundText = STouchRESTAPI.fst.getDisplay().findTextInObjectTree(text);
		return Response.ok(Json.createObjectBuilder().add("result", foundText != null).build()).build();
	}

	@POST
	@Path("{touchtext : (?i)touchtext}")
	public Response touchText(@QueryParam("text") String text) {
		try {
			boolean success = fst.getDisplay().touchText(text);
			if (success) {
				return Response.ok("Text \"" + text + "\" touched successfully").build();
			} else {
				return Response.status(Response.Status.NOT_FOUND)
						.entity("Text \"" + text + "\" not found on the display").build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error while touching text: " + e.getMessage()).build();
		}
	}

	/**
	 * Executes a sequence of commands provided as query parameters.
	 *
	 * @param uriInfo uriInfo is parsed to retrieve the query parameters, which are
	 *                the commands that assemble the automation. Supported commands
	 *                are: connect connect to the SystaComfort unit touch=x,y
	 *                emulate a touch event on screen coordinates x and y
	 *                touchText=text emulate a touch event on the given text, if it
	 *                is in the object tree of the screen touchButton=id emulate a
	 *                touch event on the button with the given id, if it is in the
	 *                object tree of the screen whileText=text&amp;doAction while
	 *                the given text is in the object tree of the screen, do the
	 *                given action whileButton=id&amp;doAction while the button with
	 *                the given id is in the object tree of the screen, do the given
	 *                action checkText=text&amp;theDoThisAction&amp;elseDoThisAction
	 *                if the given text is in the object tree of the screen, then do
	 *                the first action, else do the second
	 *                checkButton=id&amp;theDoThisAction&amp;elseDoThisAction if the
	 *                button with the given id is in the object tree of the screen,
	 *                then do the first action, else do the second disconnect
	 *                disconnect from the SystaComfort unit
	 * @return a {@link Response} indicating the result of the automation
	 */
	@GET
	@Path("{automation : (?i)automation}")
	public Response automation(@Context UriInfo uriInfo) {
		String query = uriInfo.getRequestUri().getQuery();
		String[] commandArray = query.split("&");
		boolean skipNext = false;
		//System.out.println("start automation");
		for (int i = 0; i < commandArray.length; i++) {
			String command = commandArray[i];
			//System.out.println("command["+i+"]: "+command);
			if (skipNext) {
				skipNext = false;
				//System.out.println("skip next");
				continue;
			}
			if (command.toLowerCase().startsWith("whiletext")) {
				//System.out.println("whiletext");
				Response response = automationWhileTextCommand(commandArray, i);
				if (response != null)
				{
					//System.out.println("return "+response);
					return response;
				}
				skipNext = true;
				continue;
			}
			if (command.toLowerCase().startsWith("checktext")) {
				//System.out.println("checktext: "+command);
				skipNext = automationCheckTextCommand(command);
				//System.out.println("text found == "+skipNext);
				continue;
			}
			if (command.toLowerCase().startsWith("whilebutton")) {
				//System.out.println("whilebutton");
				Response response = automationWhileButtonCommand(commandArray, i);
				if (response != null) {
					//System.out.println("return "+response);
					return response;
				}
				skipNext = true;
				continue;
			}
			if (command.toLowerCase().startsWith("checkbutton")) {
				//System.out.println("checkbutton");
				skipNext = automationCheckButtonCommand(command);
				//System.out.println("button "+command+" found == "+skipNext);
				continue;
			}
			//System.out.println("execute command "+command);
			Response response = automationExecuteCommand(command);
			//System.out.println("result of command: "+response);
			if (response.getStatus() != Response.Status.OK.getStatusCode()) {
				response = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.entity("Command failed: " + command + ", Error: " + response.getEntity()).build();
				//System.out.println("result of command: "+response);
				return response;
			}
			shortSleep();
		}
		//System.out.println("Automation executed successfully");
		return Response.ok("Automation executed successfully").build();
	}

	private Response automationWhileTextCommand(String[] commandArray, int i) {
		//System.out.println("whileTextCommand");
		String command = commandArray[i];
		//System.out.println("command == "+command);
		String searchText = extractComparisonValue(command, "whiletext");
		//System.out.println("searchText == "+searchText);
		boolean isEqualComparison = command.toLowerCase().startsWith("whiletext==");
		DisplayText foundText = STouchRESTAPI.fst.getDisplay().findTextInObjectTree(searchText);
		//System.out.println("foundText == "+foundText);
		while ((isEqualComparison && foundText != null) || (!isEqualComparison && foundText == null)) {
			//System.out.println("whileConditionNotMet");
			//System.out.println("execute command "+commandArray[i + 1]);
			Response response = automationExecuteCommand(commandArray[i + 1]);
			//System.out.println("response == "+response);
			if (response.getStatus() != Response.Status.OK.getStatusCode()) {
				//System.out.println("error");
				//System.out.println(fst.getObjectTree());
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity("Command failed: " + command + ", Error: " + response.getEntity()).build();
			}
			shortSleep();
			//System.out.println("search text again: "+searchText);
			foundText = STouchRESTAPI.fst.getDisplay().findTextInObjectTree(searchText);
			//System.out.println("now foundText == "+foundText);
		}
		return null;
	}

	private boolean automationCheckTextCommand(String command) {
		String searchText = extractComparisonValue(command, "checktext");
		DisplayText foundText = STouchRESTAPI.fst.getDisplay().findTextInObjectTree(searchText);
		boolean isEqualComparison = command.toLowerCase().startsWith("checktext==");
		return isEqualComparison ? foundText == null : foundText != null;
	}

	private Response automationWhileButtonCommand(String[] commandArray, int i) {
		String command = commandArray[i];
		Byte searchId = extractByteValue(command, "whilebutton");
		boolean isEqualComparison = command.toLowerCase().startsWith("whilebutton==");
		DisplayButton foundButton = STouchRESTAPI.fst.getDisplay().findButtonInObjectTree(searchId);
		while ((isEqualComparison && foundButton != null) || (!isEqualComparison && foundButton == null)) {
			Response response = automationExecuteCommand(commandArray[i + 1]);
			if (response.getStatus() != Response.Status.OK.getStatusCode()) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity("Command failed: " + command + ", Error: " + response.getEntity()).build();
			}
			shortSleep();
			foundButton = STouchRESTAPI.fst.getDisplay().findButtonInObjectTree(searchId);
		}
		return null;
	}

	private boolean automationCheckButtonCommand(String command) {
		Byte searchId = extractByteValue(command, "checkbutton");
		DisplayButton foundButton = STouchRESTAPI.fst.getDisplay().findButtonInObjectTree(searchId);
		boolean isEqualComparison = command.toLowerCase().startsWith("checkbutton==");
		return isEqualComparison ? foundButton == null : foundButton != null;
	}

	private String extractComparisonValue(String command, String prefix) {
		return command.substring((prefix + "??").length());
	}

	private Byte extractByteValue(String command, String prefix) {
		return Byte.parseByte(command.substring((prefix + "??").length()));
	}

	private void shortSleep() {
		try {
			Thread.sleep(750);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Execution interrupted.");
		}
	}

	private Response automationExecuteCommand(String command) {
	    if (command == null || command.trim().isEmpty()) {
	        return Response.status(Response.Status.BAD_REQUEST)
	                .entity("Error: Command is null or empty. Received: '" + command + "'. Please provide a valid command.")
	                .build();
	    }

	    try {
	        if (command.equalsIgnoreCase("none")) {
	            return Response.ok().build();
	        } else if (command.equalsIgnoreCase("connect")) {
	            Response response = connect();
	            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
	                // just try if a hanging connection is the cause
	                disconnect();
	                return connect();
	            }
	            return response;
	        } else if (command.toLowerCase().startsWith("touchbutton")) {
	            String[] parts = command.split("=");
	            if (parts.length != 2) {
	                return Response.status(Response.Status.BAD_REQUEST)
	                        .entity("Error: 'touchbutton' command format is incorrect. Received: '" + command +
	                                "'. Expected format: 'touchbutton=<number>'.")
	                        .build();
	            }
	            return touchButton(Byte.parseByte(parts[1]));
	        } else if (command.toLowerCase().startsWith("touchtext")) {
	            String[] parts = command.split("=");
	            if (parts.length != 2) {
	                return Response.status(Response.Status.BAD_REQUEST)
	                        .entity("Error: 'touchtext' command format is incorrect. Received: '" + command +
	                                "'. Expected format: 'touchtext=<text>'.")
	                        .build();
	            }
				ArrayList<DisplayText> texts = fst.getDisplay().getTexts();
				Response response = touchText(parts[1]);
				if(texts.equals(fst.getDisplay().getTexts())) {
					shortSleep();
					// the touch should always change the screen content
					response = touchText(parts[1]);
				}
	            return response;
	        } else if (command.toLowerCase().startsWith("touch")) {
	            String[] parts = command.split("=");
	            if (parts.length != 2) {
	                return Response.status(Response.Status.BAD_REQUEST)
	                        .entity("Error: 'touch' command format is incorrect. Received: '" + command +
	                                "'. Expected format: 'touch=x,y'.")
	                        .build();
	            }

	            String[] coordinates = parts[1].split(",");
	            if (coordinates.length != 2) {
	                return Response.status(Response.Status.BAD_REQUEST)
	                        .entity("Error: Coordinates are missing or incomplete in command: '" + command +
	                                "'. Expected format: 'touch=x,y'.")
	                        .build();
	            }

	            int x = Integer.parseInt(coordinates[0]);
	            int y = Integer.parseInt(coordinates[1]);
	            return touch(x, y);
	        } else if (command.equalsIgnoreCase("disconnect")) {
	            return disconnect();
	        } else {
	            return Response.status(Response.Status.BAD_REQUEST)
	                    .entity("Error: Unknown command received: '" + command +
	                            "'. Please check spelling or usage.")
	                    .build();
	        }
	    } catch (NumberFormatException e) {
	        return Response.status(Response.Status.BAD_REQUEST)
	                .entity("Error: Invalid number format in command: '" + command +
	                        "'. Details: " + e.getMessage())
	                .build();
	    } catch (Exception e) {
	        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
	                .entity("Error: Unexpected error while processing command: '" + command +
	                        "'. Details: " + e.getMessage())
	                .build();
	    }
	}

}
