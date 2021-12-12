package de.freaklamarsch.systarest;

import java.io.IOException;
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

public class DeviceTouchSearch {
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
	}

	private static final int MAX_DATA_LENGTH = 1024;
	private static final int BCAST_PORT = 8001;
	

	public static DeviceTouchDeviceInfo search() {
		DeviceTouchDeviceInfo deviceInfo = null;
		//loop over all available interfaces
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();

		    while (interfaces.hasMoreElements()) {
		        NetworkInterface networkInterface = interfaces.nextElement();
                List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                for (InterfaceAddress ia : interfaceAddresses) {
            	    InetAddress ip = ia.getAddress();
            	    if(ip instanceof Inet4Address && !ip.isLoopbackAddress()) {
            	    	DatagramSocket searchSocket = new DatagramSocket();
            	    	searchSocket.setBroadcast(true);
            	    	searchSocket.setSoTimeout(1000);
            			try {
                	    	byte[] receiveData = new byte[MAX_DATA_LENGTH];
                	    	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
							String rxMessage = getDeviceTouchInfo(ia, searchSocket, receivePacket);
                			deviceInfo = parseDeviceTouchInfoString(rxMessage);
                			deviceInfo.localIp = ip.getHostAddress();
                			deviceInfo.bcastIp = ia.getBroadcast().getHostAddress();
                			deviceInfo.bcastPort = BCAST_PORT;
                			deviceInfo.port = getDeviceTouchPort(deviceInfo, ia, searchSocket, receivePacket);
                            if(deviceInfo.port != -1) {
                            	deviceInfo.password = getDeviceTouchPassword(deviceInfo, ia, searchSocket, receivePacket);
                			}
            			} catch (Exception e) {
            				// do nothing
            			}
            			searchSocket.close();
            	        if(deviceInfo == null) {
            	        	//no SystaComfort found on this link, check the next interface
            	        	continue;
            	        }
            	        return deviceInfo;
             	    }
                }
            }
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param deviceInfo
	 * @param ia
	 * @param searchSocket
	 * @param receivePacket
	 * @return
	 * @throws IOException
	 */
	private static String getDeviceTouchPassword(DeviceTouchDeviceInfo deviceInfo, InterfaceAddress ia,
			DatagramSocket searchSocket, DatagramPacket receivePacket) throws IOException {
		String rxMessage;
		//broadcast to trigger info response from a SystaComfort on this network MAC+" 6 R UDP Pass"
		byte[] passwordMessage = (deviceInfo.mac+" 6 R UDP Pass").getBytes();
		DatagramPacket packet = new DatagramPacket(passwordMessage, passwordMessage.length, ia.getBroadcast(), BCAST_PORT);
		searchSocket.send(packet);
		searchSocket.receive(receivePacket);
		//the reply should be something like
		//0 7 1234
		rxMessage = new String(Arrays.copyOfRange(receivePacket.getData(),0,receivePacket.getLength()), StandardCharsets.ISO_8859_1);
		return rxMessage.split(" ")[2];
	}

	/**
	 * @param deviceInfo
	 * @param ia
	 * @param searchSocket
	 * @param receivePacket
	 * @return
	 * @throws IOException
	 */
	private static int getDeviceTouchPort(DeviceTouchDeviceInfo deviceInfo, InterfaceAddress ia,
			DatagramSocket searchSocket, DatagramPacket receivePacket) throws IOException {
		String rxMessage;
		//broadcast to trigger port info response from a SystaComfort on this network MAC+" 6 A R DISP Port"
		byte[] portMessage = (deviceInfo.mac+" 6 A R DISP Port").getBytes();
		DatagramPacket packet = new DatagramPacket(portMessage, portMessage.length, ia.getBroadcast(), BCAST_PORT);
		searchSocket.send(packet);
		searchSocket.receive(receivePacket);
		// replies seen so far:
		// 0 7 unknown value:Uremoteportalde
		// 0 7 3477\x00
		rxMessage = new String(Arrays.copyOfRange(receivePacket.getData(),0,receivePacket.getLength()), StandardCharsets.ISO_8859_1);
		if(rxMessage.toLowerCase().contains("unknown value")) {
			return -1;
		} else {
			return Integer.parseInt(rxMessage.split(" ")[2]);
		}
	}

	/**
	 * @param ia
	 * @param searchSocket
	 * @param receivePacket
	 * @return
	 * @throws IOException
	 */
	private static String getDeviceTouchInfo(InterfaceAddress ia, DatagramSocket searchSocket,
			DatagramPacket receivePacket) throws IOException {
		//broadcast to trigger info response from a SystaComfort on this network "0 1 A"
		byte[] searchMessage = "0 1 A".getBytes();
		DatagramPacket packet = new DatagramPacket(searchMessage, searchMessage.length, ia.getBroadcast(), BCAST_PORT);
		searchSocket.send(packet);
		searchSocket.receive(receivePacket);
		//the reply should be something like
		//SC2 1 192.168.11.23 255.255.255.0 192.168.11.1 SystaComfort-II0 0809720001 0 V0.34 V1.00 2CBE9700BEE9
		String rxMessage = new String(Arrays.copyOfRange(receivePacket.getData(),0,receivePacket.getLength()), StandardCharsets.ISO_8859_1).strip();
		return rxMessage;
	}

	/**
	 * Function to parse the info string returned from a Paradigma SystaComfort
	 * The string looks like SC2 1 192.168.11.23 255.255.255.0 192.168.11.1 SystaComfort-II0 0809720001 0 V0.34 V1.00 2CBE9700BEE9
	 * @param deviceTouchInfoString the string received from the Paradigma SystaComfort
	 * @return the SCInfoString object parsed from the string
	 */
	private static DeviceTouchDeviceInfo parseDeviceTouchInfoString(String deviceTouchInfoString) {
		if(deviceTouchInfoString == null) {
			return null;
		}
		DeviceTouchDeviceInfo deviceInfo = new DeviceTouchSearch.DeviceTouchDeviceInfo();
		deviceInfo.string = deviceTouchInfoString;
		String[] info = deviceTouchInfoString.split(" ");
		if(info.length != 11) {
			return null;
		}
	    deviceInfo.ip = info[2];
		deviceInfo.name = info[5].replace("\u00000", ""); //the name is zero terminated which gets parsed to unicode \00000
        deviceInfo.id = info[6];
        if(deviceInfo.id.length() == 10) {
            deviceInfo.app = Integer.valueOf(deviceInfo.id.substring(0,2), 16);
            deviceInfo.platform = Integer.valueOf(deviceInfo.id.substring(2,4), 16);
            deviceInfo.major = Integer.valueOf(deviceInfo.id.substring(6,8) + deviceInfo.id.substring(4,6), 16);
            deviceInfo.minor = Integer.valueOf(deviceInfo.id.substring(8,10), 16);
    		deviceInfo.version = deviceInfo.major/100.0 + "." + deviceInfo.minor;
            deviceInfo.baseVersion = info[8];
            deviceInfo.mac = info[10];
        }
        return deviceInfo;
	}
	
}
