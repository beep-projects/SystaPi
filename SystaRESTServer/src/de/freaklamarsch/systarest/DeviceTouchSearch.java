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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * Utility class for discovering and interacting with DeviceTouch-compatible devices on the local network.
 * It facilitates sending broadcast messages to discover devices, parsing their responses,
 * and retrieving specific information like IP address, port, MAC address, and version.
 * <p>
 * The discovery process involves:
 * <ol>
 *     <li>Iterating through all network interfaces of the host machine.</li>
 *     <li>For each suitable interface (IPv4, not loopback), sending a broadcast search message.</li>
 *     <li>Listening for responses from DeviceTouch devices.</li>
 *     <li>If a device responds, further messages are sent to retrieve its communication port and password.</li>
 * </ol>
 * All communication happens via UDP datagrams on a predefined broadcast port.
 */
public class DeviceTouchSearch {

    /**
     * Holds information about a discovered DeviceTouch device.
     * This includes network details, device identifiers, version information, and communication parameters.
     */
    public static class DeviceTouchDeviceInfo {
        /** The raw response string received from the device during the initial search. */
        public String string = null;
        /** The local IP address of the network interface on which the device was discovered. */
        public String localIp = null;
        /** The broadcast IP address used on the network interface where the device responded. */
        public String bcastIp = null;
        /** The broadcast port ({@value #BCAST_PORT}) used for DeviceTouch communication. */
        public int bcastPort = -1;
        /** Application identifier extracted from the device ID. */
        public int app = -1;
        /** IP address of the discovered DeviceTouch device. */
        public String ip = null;
        /** Communication port reported by the DeviceTouch device for S-Touch App interaction. -1 if not supported or not found. */
        public int port = -1;
        /** Unique identifier string of the DeviceTouch device. */
        public String id = null;
        /** MAC address of the DeviceTouch device. */
        public String mac = null;
        /** Name of the DeviceTouch device (e.g., "SystaComfort-II0"). */
        public String name = null;
        /** Platform identifier extracted from the device ID. */
        public int platform = -1;
        /** Full version string (e.g., "major.minor") constructed from major and minor version numbers. */
        public String version = null;
        /** Major version number. */
        public int major = -1;
        /** Minor version number. */
        public int minor = -1;
        /** Base version string reported by the device (e.g., "V0.34"). */
        public String baseVersion = null;
        /** Password for S-Touch App communication, if supported and retrieved. */
        public String password = null;
        /** Flag indicating if S-Touch App specific communication (port and password) is supported and was successfully retrieved. */
        public boolean stouchSupported = false;

        @Override
        public String toString() {
            return "{\n" +
                    "    \"DeviceTouchBcastIP\":\"" + bcastIp + "\",\n" +
                    "    \"DeviceTouchBcastPort\":" + bcastPort + ",\n" +
                    "    \"deviceTouchInfoString\":\"" + string + "\",\n" +
                    "    \"unitIP\":\"" + ip + "\",\n" +
                    "    \"unitName\":\"" + name + "\",\n" +
                    "    \"unitId\":\"" + id + "\",\n" +
                    "    \"unitApp\":" + app + ",\n" +
                    "    \"unitPlatform\":" + platform + ",\n" +
                    "    \"unitVersion\":\"" + version + "\",\n" +
                    "    \"unitMajor\":" + major + ",\n" +
                    "    \"unitMinor\":" + minor + ",\n" +
                    "    \"unitBaseVersion\":\"" + baseVersion + "\",\n" +
                    "    \"unitMac\":\"" + mac + "\",\n" +
                    "    \"STouchAppSupported\":" + stouchSupported + ",\n" +
                    "    \"DeviceTouchPort\":" + port + ",\n" +
                    "    \"DeviceTouchPassword\":\"" + password + "\"\n" +
                    "}";
        }
    }

    private static final int MAX_DATA_LENGTH = 1024; // Maximum buffer size for received UDP packets
    private static final int BCAST_PORT = 8001;      // UDP port for DeviceTouch broadcast communication
    private static final int SOCKET_TIMEOUT_MS = 1000; // Socket timeout in milliseconds

    // Constants for parsing the DeviceTouch info string
    private static final int INFO_STRING_EXPECTED_PARTS = 11;
    private static final int INFO_STRING_IP_INDEX = 2;
    private static final int INFO_STRING_NAME_INDEX = 5;
    private static final int INFO_STRING_ID_INDEX = 6;
    private static final int INFO_STRING_BASE_VERSION_INDEX = 8;
    private static final int INFO_STRING_MAC_INDEX = 10;

    private static final int DEVICE_ID_EXPECTED_LENGTH = 10;
    private static final int DEVICE_ID_APP_START_INDEX = 0;
    private static final int DEVICE_ID_APP_END_INDEX = 2;
    private static final int DEVICE_ID_PLATFORM_START_INDEX = 2;
    private static final int DEVICE_ID_PLATFORM_END_INDEX = 4;
    private static final int DEVICE_ID_MAJOR_PART1_START_INDEX = 6;
    private static final int DEVICE_ID_MAJOR_PART1_END_INDEX = 8;
    private static final int DEVICE_ID_MAJOR_PART2_START_INDEX = 4;
    private static final int DEVICE_ID_MAJOR_PART2_END_INDEX = 6;
    private static final int DEVICE_ID_MINOR_START_INDEX = 8;
    private static final int DEVICE_ID_MINOR_END_INDEX = 10;


    /**
     * Searches for DeviceTouch-compatible devices on all available network interfaces.
     * It sends UDP broadcast messages and attempts to parse responses to populate
     * a {@link DeviceTouchDeviceInfo} object. The method returns information for the
     * first device found.
     *
     * @return A {@link DeviceTouchDeviceInfo} object containing details of the first
     *         discovered device, or {@code null} if no device is found or an error occurs.
     * @throws SocketException If an error occurs while accessing network interfaces or sockets.
     * @throws IOException If an I/O error occurs during socket communication (sending/receiving packets).
     */
    public static DeviceTouchDeviceInfo search() throws IOException {
        DeviceTouchDeviceInfo deviceInfo = null;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        // Iterate over all network interfaces (e.g., Ethernet, Wi-Fi)
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();

            // Iterate over IP addresses associated with the current interface
            for (InterfaceAddress ia : interfaceAddresses) {
                InetAddress localAddress = ia.getAddress();
                InetAddress broadcastAddress = ia.getBroadcast();

                // Skip non-IPv4 addresses, loopback addresses, or interfaces without a broadcast address
                if (!(localAddress instanceof Inet4Address) || localAddress.isLoopbackAddress() || broadcastAddress == null) {
                    continue;
                }
                // Try-with-resources ensures the socket is closed automatically
                try (DatagramSocket searchSocket = new DatagramSocket()) {
                    searchSocket.setBroadcast(true);
                    searchSocket.setSoTimeout(SOCKET_TIMEOUT_MS); // Set timeout for receive operations

                    byte[] receiveData = new byte[MAX_DATA_LENGTH];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    DatagramPacket packet;
                    String rxMessage;

                    // 1. Send initial search broadcast to discover devices
                    // Message: "0 1 A"
                    packet = createSearchMessage(broadcastAddress);
                    searchSocket.send(packet);
                    searchSocket.receive(receivePacket); // Wait for a response
                    rxMessage = getSearchReplyString(receivePacket);
                    deviceInfo = parseDeviceTouchInfoString(rxMessage);

                    if (deviceInfo == null || deviceInfo.mac == null) {
                        // Parsing failed or MAC address (crucial for next steps) is missing
                        deviceInfo = null; // Ensure it's null if parsing failed partially
                        continue; // Try next interface or address
                    }

                    deviceInfo.localIp = localAddress.getHostAddress();
                    deviceInfo.bcastIp = broadcastAddress.getHostAddress();
                    deviceInfo.bcastPort = BCAST_PORT;

                    // 2. Request communication port from the discovered device
                    // Message: "<MAC> 6 A R DISP Port"
                    packet = createPortRequestMessage(deviceInfo.mac, broadcastAddress);
                    searchSocket.send(packet);
                    searchSocket.receive(receivePacket);
                    rxMessage = getPortReplyString(receivePacket);
                    deviceInfo.port = parsePortReplyString(rxMessage);

                    // 3. If port retrieval was successful, request password
                    if (deviceInfo.port != -1) {
                        // Message: "<MAC> 6 R UDP Pass"
                        packet = createPasswordRequestMessage(deviceInfo.mac, broadcastAddress);
                        searchSocket.send(packet);
                        searchSocket.receive(receivePacket);
                        rxMessage = getPasswordReplyString(receivePacket);
                        deviceInfo.password = parsePasswordReplyString(rxMessage);
                        // If this point is reached, port and password for S-Touch App are known
                        deviceInfo.stouchSupported = true;
                    }
                    // Successfully found and processed a device
                    return deviceInfo;

                } catch (SocketTimeoutException e) {
                    // Expected if no device responds on this interface within the timeout
                    // Continue to the next interface or address
                    deviceInfo = null; // Reset deviceInfo as this attempt failed
                } catch (IOException e) {
                    // Other IO errors during send/receive, allow to propagate
                    // Or log and continue if desired, but current requirement is to propagate
                    // For now, we'll let it propagate from the method signature
                    throw e;
                }
                // If deviceInfo was populated but some step failed (e.g. timeout after initial discovery),
                // and we are here, it means we should continue searching.
                // If a full deviceInfo was found, the method would have returned already.
            }
        }
        return null; // No device found on any interface
    }

    /**
     * Parses a password reply string from a DeviceTouch device.
     * The expected format is typically "<code>0 7 &lt;password&gt;</code>" or just "&lt;password&gt;".
     * This method assumes the password is the third part if split by space, or the whole string if no spaces.
     *
     * @param reply The raw reply string containing the password.
     * @return The extracted password, or the original reply if parsing fails to find parts.
     */
    public static String parsePasswordReplyString(String reply) {
        // Example reply: "0 7 1234" or just "1234"
        String[] parts = reply.split(" ");
        if (parts.length >= 3) { // "0 7 <password>"
            return parts[2];
        } else if (parts.length == 1 && !parts[0].isEmpty()) { // Just "<password>"
            return parts[0];
        }
        return reply; // Fallback or if format is unexpected
    }

    /**
     * Extracts the password reply string from a {@link DatagramPacket}.
     * The raw data is converted to a String using ISO-8859-1 encoding and then trimmed.
     *
     * @param receivePacket The {@link DatagramPacket} containing the password reply.
     * @return The trimmed password reply string.
     */
    public static String getPasswordReplyString(DatagramPacket receivePacket) {
        // Example raw reply: "1234" or "0 7 1234" (potentially with padding/nulls)
        String reply = new String(
                Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength()),
                StandardCharsets.ISO_8859_1);
        return reply.trim(); // Trim whitespace and non-printable characters
    }

    /**
     * Creates a {@link DatagramPacket} to request the S-Touch App password from a DeviceTouch device.
     * The message format is typically: "{@code <MAC_ADDRESS> 6 R UDP Pass}".
     *
     * @param mac          The MAC address of the target DeviceTouch device.
     * @param bcastAddress The broadcast address of the network interface.
     * @return A {@link DatagramPacket} configured for the password request.
     */
    public static DatagramPacket createPasswordRequestMessage(String mac, InetAddress bcastAddress) {
        // Message format: MAC + " 6 R UDP Pass"
        byte[] passwordMessage = (mac + " 6 R UDP Pass").getBytes(StandardCharsets.ISO_8859_1);
        return new DatagramPacket(passwordMessage, passwordMessage.length, bcastAddress, BCAST_PORT);
    }

    /**
     * Parses a port reply string from a DeviceTouch device.
     * Expected reply format for a valid port is typically "<code>0 7 &lt;port_number&gt;</code>".
     * If the reply contains "unknown value", it indicates the port is not available or supported.
     *
     * @param reply The raw reply string from the device.
     * @return The extracted port number as an integer. Returns -1 if the port is unknown,
     *         not specified, or if the reply format is unexpected.
     * @throws NumberFormatException if the port number part of the reply is not a valid integer.
     */
    public static int parsePortReplyString(String reply) throws NumberFormatException {
        // Example replies:
        // "0 7 3477" (valid port)
        // "0 7 unknown value:Uremoteportalde" (port not available)
        if (reply.toLowerCase().contains("unknown value")) {
            return -1; // Port is explicitly unknown or not supported
        } else {
            String[] parts = reply.split(" ");
            if (parts.length >= 3) {
                // Assuming the port is the third part, e.g., "0 7 <port>"
                return Integer.parseInt(parts[2].trim());
            }
            return -1; // Unexpected format or port not found
        }
    }

    /**
     * Extracts the port reply string from a {@link DatagramPacket}.
     * The raw data from the packet is converted to a String using ISO-8859-1 encoding.
     * It then removes characters that are not alphanumeric or spaces, which might be
     * necessary due to specific device behaviors sending extraneous characters. Finally, trims whitespace.
     *
     * @param receivePacket The {@link DatagramPacket} received from the device.
     * @return The cleaned and trimmed port reply string.
     */
    public static String getPortReplyString(DatagramPacket receivePacket) {
        // Example raw replies seen:
        // "0 7 unknown value:Uremoteportalde"
        // "0 7 3477\x00" (contains null terminator)
        String rxMessage = new String(
                Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength()),
                StandardCharsets.ISO_8859_1);

        // The replaceAll call is used to strip out unexpected non-alphanumeric characters
        // (excluding spaces) that some devices might send. This helps to normalize the string
        // for reliable parsing, e.g., removing null terminators or other control characters.
        rxMessage = rxMessage.replaceAll("[^0-9 a-zA-Z]", "");
        return rxMessage.trim();
    }

    /**
     * Creates a {@link DatagramPacket} to request the S-Touch App communication port from a DeviceTouch device.
     * The message format is typically: "{@code <MAC_ADDRESS> 6 A R DISP Port}".
     *
     * @param mac          The MAC address of the target DeviceTouch device.
     * @param bcastAddress The broadcast address of the network interface.
     * @return A {@link DatagramPacket} configured for the port request.
     */
    public static DatagramPacket createPortRequestMessage(String mac, InetAddress bcastAddress) {
        // Message format: MAC + " 6 A R DISP Port"
        byte[] portMessage = (mac + " 6 A R DISP Port").getBytes(StandardCharsets.ISO_8859_1);
        return new DatagramPacket(portMessage, portMessage.length, bcastAddress, BCAST_PORT);
    }

    /**
     * Extracts the initial search reply string from a {@link DatagramPacket}.
     * The raw data is converted to a String using ISO-8859-1 encoding and then whitespace is stripped
     * from the beginning and end.
     *
     * @param receivePacket The {@link DatagramPacket} containing the search reply.
     * @return The stripped search reply string.
     */
    public static String getSearchReplyString(DatagramPacket receivePacket) {
        // Example reply: "SC2 1 192.168.11.23 255.255.255.0 192.168.11.1 SystaComfort-II0 0809720001 0 V0.34 V1.00 2CBE9700BEE9"
        // The reply might have leading/trailing whitespace or null characters from the buffer.
        return new String(
                Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength()),
                StandardCharsets.ISO_8859_1)
                .strip(); // .strip() is preferred over .trim() for Unicode whitespace.
    }

    /**
     * Creates a {@link DatagramPacket} for the initial DeviceTouch discovery broadcast.
     * The message is a simple string: "{@code 0 1 A}".
     *
     * @param bcastAddress The broadcast address to send the search message to.
     * @return A {@link DatagramPacket} configured for device discovery.
     */
    public static DatagramPacket createSearchMessage(InetAddress bcastAddress) {
        // Standard DeviceTouch search message
        byte[] searchMessage = "0 1 A".getBytes(StandardCharsets.ISO_8859_1);
        return new DatagramPacket(searchMessage, searchMessage.length, bcastAddress, BCAST_PORT);
    }

    /**
     * Parses the detailed information string received from a DeviceTouch device during discovery.
     * The string is expected to be space-delimited and contain specific information at fixed indices.
     * <p>
     * Example string:
     * {@code SC2 1 192.168.11.23 255.255.255.0 192.168.11.1 SystaComfort-II0 0809720001 0 V0.34 V1.00 2CBE9700BEE9}
     * </p>
     * This method populates a {@link DeviceTouchDeviceInfo} object with the parsed data.
     *
     * @param deviceTouchInfoString The raw information string from the device.
     * @return A {@link DeviceTouchDeviceInfo} object populated with parsed data,
     *         or {@code null} if the input string is null or doesn't match the expected format.
     */
    public static DeviceTouchDeviceInfo parseDeviceTouchInfoString(String deviceTouchInfoString) {
        if (deviceTouchInfoString == null) {
            return null;
        }

        DeviceTouchDeviceInfo deviceInfo = new DeviceTouchDeviceInfo();
        deviceInfo.string = deviceTouchInfoString; // Store the original string

        String[] infoParts = deviceTouchInfoString.split(" ");
        if (infoParts.length != INFO_STRING_EXPECTED_PARTS) {
            // Does not match the expected number of space-separated parts
            return null;
        }

        deviceInfo.ip = infoParts[INFO_STRING_IP_INDEX];
        // Device name might contain null characters if not properly terminated by the device firmware before sending.
        // Replace null character (U+0000) if it's part of the parsed name.
        deviceInfo.name = infoParts[INFO_STRING_NAME_INDEX].replace("\u0000", "");
        deviceInfo.id = infoParts[INFO_STRING_ID_INDEX];

        // Parse detailed version and platform info from the device ID string
        if (deviceInfo.id.length() == DEVICE_ID_EXPECTED_LENGTH) {
            try {
                deviceInfo.app = Integer.parseInt(deviceInfo.id.substring(DEVICE_ID_APP_START_INDEX, DEVICE_ID_APP_END_INDEX), 16);
                deviceInfo.platform = Integer.parseInt(deviceInfo.id.substring(DEVICE_ID_PLATFORM_START_INDEX, DEVICE_ID_PLATFORM_END_INDEX), 16);
                // Major version is split in the ID string, e.g., "xxxxMMMMxxxx" -> "MM" from pos 6-8 and "MM" from pos 4-6
                String majorHex = deviceInfo.id.substring(DEVICE_ID_MAJOR_PART1_START_INDEX, DEVICE_ID_MAJOR_PART1_END_INDEX) +
                                  deviceInfo.id.substring(DEVICE_ID_MAJOR_PART2_START_INDEX, DEVICE_ID_MAJOR_PART2_END_INDEX);
                deviceInfo.major = Integer.parseInt(majorHex, 16);
                deviceInfo.minor = Integer.parseInt(deviceInfo.id.substring(DEVICE_ID_MINOR_START_INDEX, DEVICE_ID_MINOR_END_INDEX), 16);
                // Construct a version string like "major.minor"
                deviceInfo.version = (deviceInfo.major / 100.0) + "." + deviceInfo.minor; // Assuming major is scaled by 100
            } catch (NumberFormatException e) {
                // Failed to parse hex values from ID string, indicates malformed ID.
                // Log this or handle as appropriate. For now, leave partially parsed info.
                // Consider returning null or setting a flag in deviceInfo if ID parsing is critical.
            }
        }

        deviceInfo.baseVersion = infoParts[INFO_STRING_BASE_VERSION_INDEX];
        deviceInfo.mac = infoParts[INFO_STRING_MAC_INDEX];

        return deviceInfo;
    }
}
