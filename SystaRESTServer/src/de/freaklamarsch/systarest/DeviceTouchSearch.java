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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * A utility class for searching and interacting with DeviceTouch devices. This
 * class provides methods to create search messages, parse responses, and
 * extract device information.
 */
public class DeviceTouchSearch {
	/**
	 * Information returned from a device supporting the DeviceTouch protocol
	 */
	public static class DeviceTouchDeviceInfo {
		public String string = null;
		public String localIp = null;
		public String bcastIp = null;
		public int bcastPort = -1;
		public int app = -1;
		public String ip = null;
		public int port = -1;
		public String id = null;
		public String mac = null;
		public String name = null;
		public int platform = -1;
		public String version = null;
		public int major = -1;
		public int minor = -1;
		public String baseVersion = null;
		public String password = null;
		public boolean stouchSupported = false;

		@Override
		public String toString() {
			return "{\n" +
			// " \"SystaWebIP\":\"" + localIp + "\",\n" +
			// " \"SystaWebPort\":" + port + ",\n" +
					"    \"DeviceTouchBcastIP\":\"" + bcastIp + "\",\n" + "    \"DeviceTouchBcastPort\":" + bcastPort
					+ ",\n" + "    \"deviceTouchInfoString\":\"" + string + "\",\n" + "    \"unitIP\":\"" + ip + "\",\n"
					+ "    \"unitName\":\"" + name + "\",\n" + "    \"unitId\":\"" + id + "\",\n" + "    \"unitApp\":"
					+ app + ",\n" + "    \"unitPlatform\":" + platform + ",\n" + "    \"unitVersion\":\"" + version
					+ "\",\n" + "    \"unitMajor\":" + major + ",\n" + "    \"unitMinor\":" + minor + ",\n"
					+ "    \"unitBaseVersion\":\"" + baseVersion + "\",\n" + "    \"unitMac\":\"" + mac + "\",\n"
					+ "    \"STouchAppSupported\":" + stouchSupported + ",\n" + "    \"DeviceTouchPort\":" + port
					+ ",\n" + "    \"DeviceTouchPassword\":\"" + password + "\"\n" + "}";
		}
	}

	private static final int MAX_DATA_LENGTH = 1024;
	private static final int BCAST_PORT = 8001;

	/**
	 * Search for devices supporting the DeviceTouch protocol. The search sends UDP
	 * broadcasts and returns the {@link DeviceTouchDeviceInfo} object parsed from
	 * received reply messages
	 * 
	 * @return received {@link DeviceTouchDeviceInfo}
	 */
	public static DeviceTouchDeviceInfo search() {
		DeviceTouchDeviceInfo deviceInfo = null;
		// loop over all available interfaces
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();

			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();
				List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
				for (InterfaceAddress ia : interfaceAddresses) {
					InetAddress ip = ia.getAddress();
					if (ip instanceof Inet4Address && !ip.isLoopbackAddress()) {
						DatagramSocket searchSocket = new DatagramSocket();
						searchSocket.setBroadcast(true);
						searchSocket.setSoTimeout(1000);
						try {
							byte[] receiveData = new byte[MAX_DATA_LENGTH];
							DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
							DatagramPacket packet = createSearchMessage(ia.getBroadcast());
							String rxMessage = "";
							// search for Systa Comfort unit attached to this interface
							searchSocket.send(packet);
							searchSocket.receive(receivePacket);
							rxMessage = getSearchReplyString(receivePacket);
							deviceInfo = parseDeviceTouchInfoString(rxMessage);
							deviceInfo.localIp = ip.getHostAddress();
							deviceInfo.bcastIp = ia.getBroadcast().getHostAddress();
							deviceInfo.bcastPort = BCAST_PORT;
							// request port from Systa Comfort
							packet = createPortRequestMessage(deviceInfo.mac, ia.getBroadcast());
							searchSocket.send(packet);
							searchSocket.receive(receivePacket);
							rxMessage = getPortReplyString(receivePacket);
							deviceInfo.port = parsePortReplyString(rxMessage);
							if (deviceInfo.port != -1) {
								packet = createPasswordRequestMessage(deviceInfo.mac, ia.getBroadcast());
								searchSocket.send(packet);
								searchSocket.receive(receivePacket);
								rxMessage = getPasswordReplyString(receivePacket);
								deviceInfo.password = parsePasswordReplyString(rxMessage);
								// if this point is reached, port and password for S-Touch App are known
								deviceInfo.stouchSupported = true;
							}
						} catch (Exception e) {
							// do nothing
						}
						searchSocket.close();
						if (deviceInfo == null) {
							// no SystaComfort found on this link, check the next interface
							continue;
						}
						return deviceInfo;
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Parses a password reply message received from a DeviceTouch device.
	 *
	 * @param reply the reply string containing the password
	 * @return the extracted password
	 */
	public static String parsePasswordReplyString(String reply) {
		return reply.split(" ")[2];
	}

	/**
	 * Extracts the password reply string from a DatagramPacket received from a
	 * DeviceTouch device.
	 *
	 * @param receivePacket the DatagramPacket containing the password reply
	 * @return the extracted password reply string
	 */
	public static String getPasswordReplyString(DatagramPacket receivePacket) throws NumberFormatException {
		// replies seen so far:
		// 1234
		String reply = new String(Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength()),
				StandardCharsets.ISO_8859_1);
		reply = reply.trim();
		return reply;
	}

	/**
	 * Creates the DatagramPacket to request the password from a DeviceTouch device.
	 *
	 * @param mac          the MAC address of the target DeviceTouch device
	 * @param bcastAddress the broadcast address used for communication with the
	 *                     DeviceTouch device
	 * @return a DatagramPacket containing the password request message
	 */
	public static DatagramPacket createPasswordRequestMessage(String mac, InetAddress bcastAddress) {
		// broadcast to trigger info response from a SystaComfort on this network
		// MAC+" 6 R UDP Pass"
		byte[] passwordMessage = (mac + " 6 R UDP Pass").getBytes();
		DatagramPacket packet = new DatagramPacket(passwordMessage, passwordMessage.length, bcastAddress, BCAST_PORT);
		return packet;
	}

	/**
	 * Parses a port reply message received from a DeviceTouch device.
	 *
	 * @param reply the reply string containing the port
	 * @return the extracted port
	 * @throws NumberFormatException if the reply dows not contain a valid
	 *                               {@code Integer}
	 */
	public static int parsePortReplyString(String reply) throws NumberFormatException {
		if (reply.toLowerCase().contains("unknown value")) {
			return -1;
		} else {
			return Integer.parseInt(reply.split(" ")[2]);
		}
	}

	/**
	 * Extracts the search reply string from a DatagramPacket.
	 * 
	 * @param receivePacket the DatagramPacket containing the port reply
	 * @return the extracted port reply string
	 */
	public static String getPortReplyString(DatagramPacket receivePacket) {
		// replies seen so far:
		// 0 7 unknown value:Uremoteportalde
		// 0 7 3477\x00
		String rxMessage = new String(Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength()),
				StandardCharsets.ISO_8859_1);
		// remove all strange characters
		rxMessage = rxMessage.replaceAll("[^0-9 a-zA-Z]", "");
		rxMessage = rxMessage.trim();
		return rxMessage;
	}

	/**
	 * Creates the DatagramPacket to request the port number used from communication
	 * from a DeviceTouch device.
	 *
	 * @param mac          the MAC address of the target DeviceTouch device
	 * @param bcastAddress the broadcast address used for communication with the
	 *                     DeviceTouch device
	 * @return a DatagramPacket containing the port request message
	 */
	public static DatagramPacket createPortRequestMessage(String mac, InetAddress bcastAddress) {
		// broadcast to trigger port info response from a SystaComfort on this network
		// MAC+" 6 A R DISP Port"
		byte[] portMessage = (mac + " 6 A R DISP Port").getBytes();
		DatagramPacket packet = new DatagramPacket(portMessage, portMessage.length, bcastAddress, BCAST_PORT);
		return packet;
	}

	/**
	 * Extracts the search reply string from a DatagramPacket.
	 *
	 * @param receivePacket the DatagramPacket containing the search reply
	 * @return the extracted search reply string
	 */
	public static String getSearchReplyString(DatagramPacket receivePacket) {
		// the reply should be something like
		// SC2 1 192.168.11.23 255.255.255.0 192.168.11.1 SystaComfort-II0 0809720001 0
		// V0.34 V1.00 2CBE9700BEE9
		String reply = new String(Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength()),
				StandardCharsets.ISO_8859_1).strip();
		return reply;
	}

	/**
	 * Creates a search message to broadcast on the network for discovering
	 * DeviceTouch devices.
	 *
	 * @param bcastAddress the broadcast address to send the search message
	 * @return a DatagramPacket containing the search message
	 */
	public static DatagramPacket createSearchMessage(InetAddress bcastAddress) {
		// broadcast to trigger info response from a SystaComfort on this network
		// "0 1 A"
		byte[] searchMessage = "0 1 A".getBytes();
		DatagramPacket packet = new DatagramPacket(searchMessage, searchMessage.length, bcastAddress, BCAST_PORT);
		return packet;
	}

	/**
	 * Function to parse the info string returned from a Paradigma SystaComfort The
	 * string looks like SC2 1 192.168.11.23 255.255.255.0 192.168.11.1
	 * SystaComfort-II0 0809720001 0 V0.34 V1.00 2CBE9700BEE9
	 * 
	 * @param deviceTouchInfoString the string received from the Paradigma
	 *                              SystaComfort
	 * @return the SCInfoString object parsed from the string
	 */
	public static DeviceTouchDeviceInfo parseDeviceTouchInfoString(String deviceTouchInfoString) {
		if (deviceTouchInfoString == null) {
			return null;
		}
		DeviceTouchDeviceInfo deviceInfo = new DeviceTouchSearch.DeviceTouchDeviceInfo();
		deviceInfo.string = deviceTouchInfoString;
		String[] info = deviceTouchInfoString.split(" ");
		if (info.length != 11) {
			return null;
		}
		deviceInfo.ip = info[2];
		deviceInfo.name = info[5].replace("\u00000", ""); // the name is zero terminated which gets parsed to unicode
															// \00000
		deviceInfo.id = info[6];
		if (deviceInfo.id.length() == 10) {
			deviceInfo.app = Integer.valueOf(deviceInfo.id.substring(0, 2), 16);
			deviceInfo.platform = Integer.valueOf(deviceInfo.id.substring(2, 4), 16);
			deviceInfo.major = Integer.valueOf(deviceInfo.id.substring(6, 8) + deviceInfo.id.substring(4, 6), 16);
			deviceInfo.minor = Integer.valueOf(deviceInfo.id.substring(8, 10), 16);
			deviceInfo.version = deviceInfo.major / 100.0 + "." + deviceInfo.minor;
			deviceInfo.baseVersion = info[8];
			deviceInfo.mac = info[10];
		}
		return deviceInfo;
	}

}
