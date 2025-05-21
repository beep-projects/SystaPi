/*
* Copyright (c) 2025, The beep-projects contributors
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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import de.freaklamarsch.systarest.DeviceTouchSearch.DeviceTouchDeviceInfo;
import de.freaklamarsch.systarest.STouchProtocol.Button;
import de.freaklamarsch.systarest.STouchProtocol.Coordinates;
import de.freaklamarsch.systarest.STouchProtocol.Rectangle;
import de.freaklamarsch.systarest.STouchProtocol.STouchCommand;
import de.freaklamarsch.systarest.STouchProtocol.TextXY;

/**
 * A mock implementation of the S-Touch device for testing and simulation
 * purposes. This class provides methods to simulate interactions with the
 * S-Touch device, such as connecting, disconnecting, and sending touch events.
 */
public class FakeSTouch {

	private DatagramSocket socket;
	private boolean connected = false;
	private ExecutorService listenerService = null;
	private Future<?> listenerFuture = null;

	// constants
	private static final int TIMEOUT = 1000; // 1 second
	private static final int MAX_RETRIES = 10;
	private static final int APP_MINOR = 2;
	private static final int APP_VERSION = 20;
	private static final int BASIS_VERSION = 2201;
	private static final int MAX_DATA_LENGTH = 4096;

	private boolean debug = false; // TODO change back to false
	int lastId = -1;
	int lastX = -1;
	int lastY = -1;
	/** The display associated with this S-Touch device. */
	private FakeSTouchDisplay display = new FakeSTouchDisplay();

	// int rxRetryCount = 0;
	long PktCmd;
	int port = 0;
	InetAddress inetAddress;
	int cnCmd;
	String mac;
	byte[] password = null;
	DeviceTouchDeviceInfo info = null;

	public enum ConnectionStatus {
		SUCCESS, DEVICE_ALREADY_IN_USE, WRONG_UDP_PASSWORD, TIMEOUT, ALREADY_CONNECTED, NO_COMPATIBLE_DEVICE_FOUND
	}

	enum ReplyType {
		NONE, OK, ERROR
	}

	private final Map<STouchCommand, Function<Object, Boolean>> displayMethods = new HashMap<>();

	private void initializeDisplayActions() {
		// TODO implement missing methods in FakeSTouchDiaplay.
		// i.e. the ones that are mapped to return true
		displayMethods.put(STouchCommand.DISPLAY_SWITCHON, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_SWITCHOFF, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_SETSTYLE, parameters -> this.getDisplay().setStyle((int) parameters));
		displayMethods.put(STouchCommand.DISPLAY_SETINVERS, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_SETFORECOLOR,
				parameters -> this.getDisplay().setForeColor((Color) parameters));
		displayMethods.put(STouchCommand.DISPLAY_SETBACKCOLOR,
				parameters -> this.getDisplay().setBackColor((Color) parameters));
		displayMethods.put(STouchCommand.DISPLAY_SETFONTTYPE, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_SETPIXEL, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_MOVETO, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_LINETO, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_DRAWRECT,
				parameters -> this.getDisplay().drawRect((Rectangle) parameters));
		displayMethods.put(STouchCommand.DISPLAY_DRAWARC, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_DRAWROUNDRECT, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_DRAWSYMBOL, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_DELETESYMBOL, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_SETXY, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_PUTC, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_PRINT, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_PRINTXY, parameters -> this.getDisplay().addText((TextXY) parameters));
		displayMethods.put(STouchCommand.DISPLAY_PUTCROT, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_PRINTROT, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_CALIBRATETOUCH, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_SYNCNOW, parameters -> syncnow((int) parameters));
		displayMethods.put(STouchCommand.DISPLAY_SETBACKLIGHT, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_SETBUZZER, parameters -> true);
		displayMethods.put(STouchCommand.DISPLAY_SETCLICK, parameters -> this.getDisplay().setClick((int) parameters));
		displayMethods.put(STouchCommand.DISPLAY_SETBUTTON,
				parameters -> this.getDisplay().addButton((Button) parameters));
		displayMethods.put(STouchCommand.DISPLAY_DELBUTTON,
				parameters -> this.getDisplay().delButton((int) parameters));
		displayMethods.put(STouchCommand.DISPLAY_SETTEMPOFFSETS, parameters -> true);
		displayMethods.put(STouchCommand.SYSTEM_GETSYSTEM, parameters -> true);
		displayMethods.put(STouchCommand.SYSTEM_GOSYSTEM, parameters -> true);
		displayMethods.put(STouchCommand.SYSTEM_CLEARID, parameters -> true);
		displayMethods.put(STouchCommand.SYSTEM_GETRESOURCEINFO, parameters -> true);
		displayMethods.put(STouchCommand.SYSTEM_ERASERESOURCE, parameters -> true);
		displayMethods.put(STouchCommand.SYSTEM_FLASHRESOURCE, parameters -> true);
		displayMethods.put(STouchCommand.SYSTEM_ACTIVATERESOURCE, parameters -> true);
		displayMethods.put(STouchCommand.SYSTEM_SETCONFIG, parameters -> true);
		displayMethods.put(STouchCommand.SYSTEM_CLEARAPP, parameters -> true);
		displayMethods.put(STouchCommand.SYSTEM_FLASHAPP, parameters -> true);
		displayMethods.put(STouchCommand.SYSTEM_ACTIVATEAPP, parameters -> true);
	}

	public FakeSTouch() {
		initializeDisplayActions();
	}

	/**
	 * Enable/disable the writing of debug messages
	 *
	 * @param debug {@code true} enables writing of debugging info, {@code false}
	 *              disables it.
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * Retrieves the simulated display associated with this S-Touch device.
	 *
	 * @return the {@link FakeSTouchDisplay} instance
	 */
	public synchronized FakeSTouchDisplay getDisplay() {
		return display;
	}

	/**
	 * Get the {@link DeviceTouchDeviceInfo} currently set as communication target
	 *
	 * @return the {@link DeviceTouchDeviceInfo}
	 */
	public DeviceTouchDeviceInfo getInfo() {
		return info;
	}

	/**
	 * Set the {@link DeviceTouchDeviceInfo} that should be used as communication
	 * target
	 *
	 * @param info the info to set
	 */
	public boolean setInfo(DeviceTouchDeviceInfo info) {
		if (this.connected) {
			return false;
		}
		this.info = info;
		this.mac = info.mac;
		try {
			this.inetAddress = InetAddress.getByName(info.ip);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
		this.port = info.port;
		this.password = info.password.getBytes();
		return true;
	}

	/**
	 * Connects the simulated S-Touch device to a Paradigma SystaComfort unit. If
	 * the {@code info} is set, this will be used as communication target, otherwise
	 * a compatible device is searched on the configured interface.
	 *
	 * @return {@code true} if the connection was successful; {@code false}
	 *         otherwise
	 * @throws IOException          if an error occurs during sending of connection
	 *                              messages
	 * @throws InterruptedException if the thread gets interrupted during waiting
	 *                              for a reply for connection messages
	 */
	public synchronized ConnectionStatus connect() throws IOException, InterruptedException {
		if (this.connected) {
			return ConnectionStatus.ALREADY_CONNECTED;
		}
		if (this.info == null) {
			setInfo(searchSTouchDevice());
		}
		if (this.info == null) {
			return ConnectionStatus.NO_COMPATIBLE_DEVICE_FOUND;
		}

		DatagramPacket connectionRequest = createConnectionRequestMessage();

		this.socket = new DatagramSocket();
		this.socket.setSoTimeout(TIMEOUT);

		for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
			this.socket.send(connectionRequest);
			DatagramPacket responsePacket = receive();
			if (responsePacket == null) {
				continue; // retry
			}
			int responseLength = responsePacket.getLength();
			byte[] responseBytes = responsePacket.getData();
			if (responseLength == 7 && responseBytes[5] == 1) {
				if (responseBytes[6] == 1) {
					printDebugInfo("Connect() succeeded, starting communication");
					this.connected = true;
					startListening();
					return ConnectionStatus.SUCCESS;
				} else if (responseBytes[6] == 0) {
					printDebugInfo("Connect() failed, wrong UDP password sent. Password: " + new String(this.password));
					return ConnectionStatus.WRONG_UDP_PASSWORD;
				} else if (responseBytes[6] == -2) {
					printDebugInfo("Connect() failed, device is already in use");
					return ConnectionStatus.DEVICE_ALREADY_IN_USE;
				}
			}
		}
		this.socket.close();
		return ConnectionStatus.TIMEOUT;
	}

	/**
	 * Create a DatagramPacket to send a connection request to a Paradigma
	 * SystaComfort device.
	 *
	 * @return the DatatgramPacket holding the created connection request
	 */
	public DatagramPacket createConnectionRequestMessage() {
		int txLen = 10;
		byte[] headerBytes = { 8, 0, 0, 0, 0, 1 };
		byte[] txBytes = new byte[headerBytes.length + this.password.length];
		System.arraycopy(headerBytes, 0, txBytes, 0, headerBytes.length);
		System.arraycopy(this.password, 0, txBytes, headerBytes.length, this.password.length);
		DatagramPacket datagramPacket = new DatagramPacket(txBytes, txLen, this.inetAddress, this.port);
		return datagramPacket;
	}

	/**
	 * Create a DatagramPacket to inform a Paradigma SystaComfort device about the
	 * disconnection from this emulates S-Touch device.
	 *
	 * @return the DatatgramPacket holding the created disconnect message
	 */
	public DatagramPacket createDisconnectRequestMessage() {
		int txLen = 10;
		byte[] txBytes = { 8, 0, 0, 0, 0, 1, 0, 0, 0, 0 };
		DatagramPacket datagramPacket = new DatagramPacket(txBytes, txLen, this.inetAddress, this.port);
		return datagramPacket;
	}

	private void startListening() {
		this.listenerService = Executors.newSingleThreadExecutor();
		this.listenerFuture = this.listenerService.submit(() -> {
			while (this.connected) {
				DatagramPacket packet = receive();
				if (packet != null) {
					DatagramPacket reply = processCommands(packet);
					if (reply != null) {
						try {
							if (this.connected) {
								this.socket.send(reply);
							} else {
								printDebugInfo(
										"FakeSTouch got disconnected while processing commands, not sending reply message.");
							}
							getDisplay().setTouch(-1, -1);
						} catch (IOException ioe) {
							if (this.debug) {
								ioe.printStackTrace();
							}
						}
					}
				}
			}
		});
	}

	/**
	 * Receive a message from the connected Paradigma SystaComfort unit
	 *
	 * @return the received DatagramPaket from the connected device, or null if no
	 *         valid packet was received
	 */
	private DatagramPacket receive() {
		DatagramPacket packet = null;
		try {
			byte[] rcv = new byte[MAX_DATA_LENGTH];
			packet = new DatagramPacket(rcv, rcv.length);
			this.socket.receive(packet);
			if (packet.getAddress().getHostAddress().equalsIgnoreCase(this.inetAddress.getHostAddress())) {
				return packet;
			} else {
				printDebugInfo("irgnoring packet from " + packet.getAddress().getHostAddress());
				return null;
			}
		} catch (IOException ioe) {
			if (this.debug) {
				ioe.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * Disconnects the simulated S-Touch device from the SystaComfort unit.
	 *
	 * @throws IOException if an error occurs during disconnection
	 */
	public synchronized void disconnect() throws IOException {
		if (!this.connected) {
			return;
		}
		// set connected to false, so the listenerService can gracefully exit
		this.connected = false;
		// the thread might be stuck in the receive() function, so we force it out of
		// that
		if (this.listenerFuture != null) {
			this.listenerFuture.cancel(true);
			this.listenerFuture = null;
		}
		DatagramPacket disconnectMessage = createDisconnectRequestMessage();

		for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
			this.socket.send(disconnectMessage);
			DatagramPacket disconnectConfirmation = receive();
			if (disconnectConfirmation == null) {
				continue; // retry
			}
			int responseLength = disconnectConfirmation.getLength();
			byte[] responseBytes = disconnectConfirmation.getData();
			byte[] disconnectConfirmationBytes = { 8, 0, 0, 0, 0, 1, 0, 0, 1, -1 };
			if (responseLength == 7 && Arrays.equals(responseBytes, 0, 6, disconnectConfirmationBytes, 0, 6)) {
				printDebugInfo("Disconnect confirmed by " + this.inetAddress.getHostAddress());
				break;
			} else {
				printDebugInfo(
						"Unknown disconnectConfirmation message received from " + this.inetAddress.getHostAddress());
				if (this.debug) {
					System.out.println("responseLength == " + responseLength);
					printByteArrayAsHex(responseBytes, responseLength);
				}
			}
		}

		printDebugInfo("Closing connection to " + this.inetAddress.getHostAddress());

		this.socket.close();

		if (this.listenerService != null) {
			this.listenerService.shutdownNow();
			this.listenerService = null;
		}
	}

	public DeviceTouchDeviceInfo searchSTouchDevice() {
		return DeviceTouchSearch.search();
	}

	private void printDebugInfo(String string) {
		if (this.debug) {
			System.out.println(string);
		}
	}

	private boolean syncnow(int id) {
		id = id + 1;
		this.cnCmd = id;
		this.PktCmd = id;
		return true;
	}

	/**
	 * Simulates a touch event on the S-Touch display at the specified coordinates.
	 *
	 * @param x the x-coordinate of the touch
	 * @param y the y-coordinate of the touch
	 */
	public void touch(int x, int y) {
		getDisplay().setTouch(x, y);
	}

	public void printScreen() {
		getDisplay().printContent();
	}

	public String getObjectTree() {
		return getDisplay().getObjectTree().toString();
	}

	public BufferedImage getScreenAsImage() {
		return getDisplay().getContentAsImage();
	}

	/**
	 * @param addr the InetAddress this instance should use for communication
	 */
	public void setAddr(InetAddress addr) {
		this.inetAddress = addr;
	}

	boolean isCommandEnabled(STouchCommand cmd) {
		boolean enabled;
		switch (cmd) {
		case DISPLAY_SYNCNOW:
		case SYSTEM_GETSYSTEM:
		case SYSTEM_GOSYSTEM:
		case SYSTEM_CLEARID:
		case SYSTEM_GETRESOURCEINFO:
		case SYSTEM_ERASERESOURCE:
		case SYSTEM_FLASHRESOURCE:
		case SYSTEM_ACTIVATERESOURCE:
		case SYSTEM_SETCONFIG:
		case SYSTEM_CLEARAPP:
		case SYSTEM_FLASHAPP:
		case SYSTEM_ACTIVATEAPP:
			return true;
		/*
		 * case DISPLAY_SETBACKLIGHT: case DISPLAY_SETBUZZER: case DISPLAY_SETCLICK:
		 * case DISPLAY_SETBUTTON: case DISPLAY_DELBUTTON: case DISPLAY_SETTEMPOFFSETS:
		 */
		default:
			if ((this.getDisplay().config & 1) == 1) {
				if (this.cnCmd == this.PktCmd) {
					this.cnCmd++;
					enabled = true;
				} else {
					enabled = false;
				}
				this.PktCmd++;
			} else {
				enabled = true;
			}
		}
		return enabled;
	}

	public DatagramPacket processCommands(DatagramPacket packet) {
		ReplyType replyType = ReplyType.ERROR;
		DatagramPacket replyPacket = null;
		byte[] rcvBuf = packet.getData();
		int rcvLen = packet.getLength();
		byte[] replyBuf = new byte[MAX_DATA_LENGTH];
		ByteBuffer rcvBuffer = ByteBuffer.wrap(rcvBuf).asReadOnlyBuffer();
		rcvBuffer.order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer replyBuffer = ByteBuffer.wrap(replyBuf);
		replyBuffer.order(ByteOrder.LITTLE_ENDIAN);
		printDebugInfo("THE RECEIVED PACKET, len = " + rcvLen);
		if (this.debug) {
			printByteArrayAsHex(rcvBuf, rcvLen);
		}
		int packetType = rcvBuffer.get();
		printDebugInfo("PACKET TYPE = " + packetType);
		if (packetType == 1 || packetType == 9) {
			this.PktCmd = (Integer) STouchProtocol.read(STouchCommand.TYPE_SHORT_INTEGER, rcvBuffer);
			int pktId = (int) STouchProtocol.read(STouchCommand.TYPE_SHORT_INTEGER, rcvBuffer);
			replyPacket = new DatagramPacket(replyBuf, 0, this.inetAddress, this.port);
			STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, packetType);
			STouchProtocol.write(STouchCommand.TYPE_SHORT_INTEGER, replyBuffer, (int) this.PktCmd);
			STouchProtocol.write(STouchCommand.TYPE_SHORT_INTEGER, replyBuffer, pktId);
			if (rcvLen >= 5) {
				replyType = ReplyType.OK;
			} else {
				replyType = ReplyType.ERROR;
			}
			int ignoredCommandsCount = 0;
			int processedCommandsCount = 0;
			// loop over all received commands
			while (true) {
				printDebugInfo("pos @ " + rcvBuffer.position());
				if (rcvBuffer.position() < rcvLen) {
					printDebugInfo("process cmdIndex #" + rcvBuffer.position());
					processedCommandsCount++;
					if (packetType == 1 && processedCommandsCount >= 255) {
						replyType = ReplyType.ERROR;
					} else if (rcvLen > 5) {
						STouchCommand cmd = (STouchCommand) STouchProtocol.read(STouchCommand.TYPE_COMMAND, rcvBuffer);
						if (cmd != null) {
							int cmdLen = cmd.length(rcvBuffer);
							if (cmdLen >= 0) {
								if (isCommandEnabled(cmd)) {
									printDebugInfo("process cmd: " + cmd.name());
									Object parameters = null;
									switch (cmd) {
									case DISPLAY_SWITCHON:
									case DISPLAY_SWITCHOFF:
									case DISPLAY_SETSTYLE:
									case DISPLAY_SETINVERS:
									case DISPLAY_SETFORECOLOR:
									case DISPLAY_SETBACKCOLOR:
									case DISPLAY_SETFONTTYPE:
									case DISPLAY_SETPIXEL:
									case DISPLAY_MOVETO:
									case DISPLAY_LINETO:
									case DISPLAY_DRAWRECT:
									case DISPLAY_DRAWARC:
									case DISPLAY_DRAWROUNDRECT:
									case DISPLAY_DRAWSYMBOL:
									case DISPLAY_DELETESYMBOL:
									case DISPLAY_SETXY:
									case DISPLAY_PUTC:
									case DISPLAY_PRINT:
									case DISPLAY_PRINTXY:
									case DISPLAY_PUTCROT:
									case DISPLAY_PRINTROT:
									case DISPLAY_CALIBRATETOUCH:
									case DISPLAY_SYNCNOW:
									case DISPLAY_SETBACKLIGHT:
									case DISPLAY_SETBUZZER:
									case DISPLAY_SETCLICK:
									case DISPLAY_SETBUTTON:
									case DISPLAY_DELBUTTON:
									case DISPLAY_SETTEMPOFFSETS:
									case SYSTEM_GOSYSTEM:
									case SYSTEM_CLEARID:
									case SYSTEM_CLEARAPP:
									case SYSTEM_FLASHAPP:
									case SYSTEM_ACTIVATEAPP:
										if (cmdLen > 0) {
											parameters = STouchProtocol.read(cmd, rcvBuffer);
											printDebugInfo("Parameters: " + parameters);
										}
										printDebugInfo(cmd.name());
										Function<Object, Boolean> processor = displayMethods.get(cmd);
										if (processor != null) {
											// printDebugInfo(cmd.name());
											try {
												if (!processor.apply(parameters)) {
													printDebugInfo("Error, stop processing");
													replyType = ReplyType.ERROR;
												}
											} catch (Exception e) {
												printDebugInfo("Error, stop processing");
												replyType = ReplyType.ERROR;
											}
										} else {
											printDebugInfo("Unknown display method type: " + cmd.name());
											replyType = ReplyType.ERROR;
										}
										break;
									case SYSTEM_GETSYSTEM:
										printDebugInfo(cmd.name());
										replyType = ReplyType.NONE;
										STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer,
												STouchCommand.SYSTEM_GETSYSTEM);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 1);
										STouchProtocol.write(STouchCommand.TYPE_SHORT_INTEGER, replyBuffer,
												FakeSTouch.BASIS_VERSION / 100);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer,
												(FakeSTouch.BASIS_VERSION % 100));
										STouchProtocol.write(STouchCommand.TYPE_SHORT_INTEGER, replyBuffer,
												FakeSTouch.APP_VERSION);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer,
												FakeSTouch.APP_MINOR);
										break;
									case SYSTEM_GETRESOURCEINFO:
										printDebugInfo(cmd.name());
										replyType = ReplyType.NONE;
										STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer,
												STouchCommand.SYSTEM_GETRESOURCEINFO);
										STouchProtocol.write(STouchCommand.TYPE_SHORT_INTEGER, replyBuffer,
												this.getDisplay().RESSOURCE_ID);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer,
												this.getDisplay().FONTS_USED);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer,
												this.getDisplay().SYMBOLS_USED);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer,
												this.getDisplay().getFonts() - this.getDisplay().FONTS_USED);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer,
												this.getDisplay().getSymbs() - this.getDisplay().SYMBOLS_USED);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, -1);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, -1);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, -1);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, -1);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, -8);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 0);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 0);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 0);
										STouchProtocol.write(STouchCommand.TYPE_INTEGER, replyBuffer,
												this.getDisplay().getChecksum());
										break;
									case SYSTEM_ERASERESOURCE:
										printDebugInfo(cmd.name());
										replyType = ReplyType.NONE;
										STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer,
												STouchCommand.SYSTEM_ERASERESOURCE);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 0);
										break;
									case SYSTEM_FLASHRESOURCE:
										printDebugInfo(cmd.name());
										replyType = ReplyType.NONE;
										STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer,
												STouchCommand.SYSTEM_FLASHRESOURCE);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 0);
										break;
									case SYSTEM_ACTIVATERESOURCE:
										printDebugInfo(cmd.name());
										replyType = ReplyType.NONE;
										if (!this.getDisplay().setChecksum(
												(int) STouchProtocol.read(STouchCommand.TYPE_INTEGER, rcvBuffer))) {
											STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer,
													STouchCommand.SYSTEM_ACTIVATERESOURCE);
											STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 2);
										} else {
											STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer,
													STouchCommand.SYSTEM_ACTIVATERESOURCE);
											STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 0);
										}
										break;
									case SYSTEM_SETCONFIG:
										printDebugInfo(cmd.name());
										replyType = ReplyType.NONE;
										this.getDisplay().setConfig(
												(int) STouchProtocol.read(STouchCommand.TYPE_INTEGER, rcvBuffer));
										if ((this.getDisplay().config & 1) == 1) {
											this.cnCmd = 0;
										}
										STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer,
												STouchCommand.SYSTEM_SETCONFIG);
										STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 0);
										STouchProtocol.write(STouchCommand.TYPE_INTEGER, replyBuffer,
												this.getDisplay().config);
										break;
									default:
										printDebugInfo("default");
										replyType = ReplyType.NONE;
										rcvBuffer.position(rcvLen);
										replyBuffer.position(0);
										replyPacket.setLength(0);
										break;
									}
									if (debug) {
										System.out.println("reply packet after processing of: " + cmd.name());
										System.out.println("buffer position is: " + replyBuffer.position());
										System.out.print("The current replyBuffer: ");
										printByteArrayAsHex(replyBuf, replyBuffer.position());
									}
								} else {
									// if not emeaCmd(cmd)
									printDebugInfo("Command is not enable, continue with next");
									// "read" rest of the command
									rcvBuffer.position(rcvBuffer.position() + cmdLen);
									ignoredCommandsCount++;
								}
							} else {
								replyType = ReplyType.ERROR;
							}
						}
					}
				} else {
					pktId = pktId != 0 ? 1 : 0;
					break;
				}
			}
			if (replyType == ReplyType.ERROR) {
				STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, -1);
				if (packetType == 9) {
					STouchProtocol.write(STouchCommand.TYPE_SHORT_INTEGER, replyBuffer, processedCommandsCount);
				} else {
					STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, processedCommandsCount);
				}
				STouchProtocol.write(STouchCommand.TYPE_SHORT_INTEGER, replyBuffer,
						this.getDisplay().getFreeCommandSpace());
				STouchProtocol.write(STouchCommand.TYPE_SHORT_INTEGER, replyBuffer, ignoredCommandsCount);
			} else if (replyType == ReplyType.OK) {
				if ((this.getDisplay().config & 1) == 1 && this.getDisplay().getButton() == -1) {
					if (pktId == this.lastId) {
						this.getDisplay().setTouch(this.lastX, this.lastY);
					}
				}
				replyBuffer.position(5);
				STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 0);
				if (packetType == 9) {
					STouchProtocol.write(STouchCommand.TYPE_SHORT_INTEGER, replyBuffer, processedCommandsCount);
				} else {
					STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, processedCommandsCount);
				}
				STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, this.display.getButton());
				STouchProtocol.write(STouchCommand.DISPLAY_SETXY, replyBuffer,
						new Coordinates(this.display.getX(), this.display.getY()));
				STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 0);
				STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 0);
				STouchProtocol.write(STouchCommand.TYPE_SHORT_INTEGER, replyBuffer,
						this.getDisplay().getFreeCommandSpace());
				STouchProtocol.write(STouchCommand.TYPE_SHORT_INTEGER, replyBuffer, ignoredCommandsCount);
			}
			replyPacket.setLength(replyBuffer.position());
		} else {
			printDebugInfo("Unknow packet type received. Clearing reply packet.");
			replyPacket = null;
		}
		printDebugInfo("THE REPLY PACKET");
		if (this.debug) {
			if (replyPacket == null) {
				System.out.println("null");
			} else {
				printByteArrayAsHex(replyPacket.getData(), replyPacket.getLength());
			}
		}
		return replyPacket;
	}

	private void printByteArrayAsHex(byte[] bytes, int len) {
		printByteArrayAsHex(Arrays.copyOfRange(bytes, 0, len));
	}

	private void printByteArrayAsHex(byte[] bytes) {
		BigInteger bigInteger = new BigInteger(1, bytes);
		System.out.println(String.format("%0" + (bytes.length << 1) + "X", bigInteger));
	}
}
