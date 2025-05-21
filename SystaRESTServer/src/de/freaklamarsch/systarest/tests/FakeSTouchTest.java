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
package de.freaklamarsch.systarest.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import de.freaklamarsch.systarest.DeviceTouchSearch;
import de.freaklamarsch.systarest.DeviceTouchSearch.DeviceTouchDeviceInfo;
import de.freaklamarsch.systarest.FakeSTouch;

class FakeSTouchTest {
	public class PcapPacket {
		public boolean tx;
		public DatagramPacket packet;

		PcapPacket(boolean tx, DatagramPacket packet) {
			this.tx = tx;
			this.packet = packet;
		}
	}

	DeviceTouchDeviceInfo deviceInfo;
	ArrayList<PcapPacket> packetData;
	// byte[] localIpAddrBytes = { 0x0a, 0x00, 0x00, 0x6a };
	byte[] localIpAddrBytes = { (byte) 0xc0, (byte) 0xa8, 0x0b, 0x01 };

	/**
	 * reads {@code packetData} from a pcap dumped into a c array header file. It is
	 * assumed that the file starts with the default startup sequence of the S-Touch
	 * app searching for a compatible device
	 * 
	 * @param testFileName
	 */
	private void initializeData(String testFileName) {
		initializeData(testFileName, 1, 3, 5);
	}

	/**
	 * reads {@code packetData} from a pcap dumped into a c array header file. The
	 * capture file should hold packets from the communication startup sequence and
	 * you have to provide the indices of these packets
	 * 
	 * @param testFileName
	 * @param idxDeviceTouchInfoReply index of the packet holding the reply with the
	 *                                info string about the communication partner
	 * @param idxPortReply            index of the packet holding the reply with the
	 *                                port used for communication by the partner
	 * @param idxPasswordReply        index of the packet holding the reply with the
	 *                                password used for communication by the partner
	 */
	private void initializeData(String testFileName, int idxDeviceTouchInfoReply, int idxPortReply,
			int idxPasswordReply) {
		String testDir = this.getClass().getResource(".").getPath();
		readPCAPDumpHeaderFileIntoByteBufferArray(testDir + testFileName);
		// the dump file should have an alternating sequence of received messages and
		// the corresponding reply.
		// sometimes the systa comfort send a group of packets and the S-Touch app sends
		// a group of replies
		// for easier handling in tests, the dump is changed to restore the alternating
		// sequence
		for (int i = 0; i < packetData.size() - 1; i++) {
			if (packetData.get(i).tx) {
				// skip the tx packets at the beginning
				continue;
			}
			// packetData.get(i) is an rx packet
			// check if packetData.get(i + 1) is a tx packet, if not search for the next one
			// and sort it to the front
			// make sure that only command packets get resorted
			if (!packetData.get(i + 1).tx && packetData.get(i + 1).packet.getData()[0] == 9) {
				int offset = i + 2;

				while (offset < packetData.size() && !packetData.get(offset).tx) {
					offset++;
				}
				// check if a tx packet was found and sort it to the front
				if (offset < packetData.size()) {
					// System.out.println("resort #" + offset + " to #" + (i + 1));
					PcapPacket txPacket = packetData.remove(offset);
					packetData.add(i + 1, txPacket);
				}
			}
		}

		setDeviceInfoFromPacketData(idxDeviceTouchInfoReply, idxPortReply, idxPasswordReply);
	}

	/**
	 * @param idxDeviceTouchInfoReply index of the packet holding the reply with the
	 *                                info string about the communication partner
	 * @param idxPortReply            index of the packet holding the reply with the
	 *                                port used for communication by the partner
	 * @param idxPasswordReply        index of the packet holding the reply with the
	 *                                password used for communication by the partner
	 */
	private void setDeviceInfoFromPacketData(int idxDeviceTouchInfoReply, int idxPortReply, int idxPasswordReply) {
		String rxMessage = DeviceTouchSearch.getSearchReplyString(packetData.get(idxDeviceTouchInfoReply).packet);
		deviceInfo = DeviceTouchSearch.parseDeviceTouchInfoString(rxMessage);
		rxMessage = DeviceTouchSearch.getPortReplyString(packetData.get(idxPortReply).packet);
		deviceInfo.port = DeviceTouchSearch.parsePortReplyString(rxMessage);
		deviceInfo.stouchSupported = true;
		rxMessage = DeviceTouchSearch.getPasswordReplyString(packetData.get(idxPasswordReply).packet);
		deviceInfo.password = DeviceTouchSearch.parsePasswordReplyString(rxMessage);
	}

	/**
	 * loads pcap dump of the communication flow between the S-Touch app and a
	 * compatible device. The dump need to be saved as "C" header file holding the
	 * packets as hexText bytes in "C" arrays. A hexText byte is the hexadecimal
	 * asci text representing the value of the transmitted byte. The size of the
	 * ByteBuffer has to match the Hex Stream in the file. No checks are done.
	 * 
	 * @param pcapDumpHeaderFile path to the file that should be loaded
	 */
	private void readPCAPDumpHeaderFileIntoByteBufferArray(String pcapDumpHeaderFile) {
		packetData = new ArrayList<PcapPacket>();
		try {
			File file = new File(pcapDumpHeaderFile);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			ArrayList<Byte> currentArray = null;

			while ((line = bufferedReader.readLine()) != null) {
				// remove all comments
				line = line.replaceAll("//.*", "").trim();
				if (line.isEmpty()) {
					continue;
				}
				if (line.contains("{")) {
					// new array
					currentArray = new ArrayList<Byte>();
					continue;
				}
				if (line.contains("}")) {
					// end of array
					packetData.add(byteArrayToDatagramPacket(currentArray));
					continue;
				}
				// parse the line into the current array
				String[] hexValues = line.split(",");
				for (String hexValue : hexValues) {
					if (hexValue.isEmpty()) {
						continue;
					}
					currentArray.add(Byte.valueOf((byte) Integer.parseInt(hexValue.replaceAll("0x", "").trim(), 16)));
				}
			}
			bufferedReader.close();

		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	private void printByteArrayAsHex(byte[] bytes) {
		BigInteger bigInteger = new BigInteger(1, bytes);
		System.out.println(String.format("%0" + (bytes.length << 1) + "X", bigInteger));
	}

	private PcapPacket byteArrayToDatagramPacket(ArrayList<Byte> bytes) {
		// the byte array should have the following structure
		// 00 6 byte Ethernet destination
		// 06 6 byte Ethernet source
		// 12 2 byte Ether type 0x0800 IPv4
		// 14 1 byte Protocol version + Header length 0x45
		// 15 1 byte differentiated services 0x00
		// 16 2 byte total length
		// 18 2 byte packet id
		// 20 2 byte fragmentation flags
		// 22 1 byte TTL
		// 23 1 byte protocol 0x11 UDP
		// 24 2 byte header checksum
		// 26 4 byte source ip
		// 30 4 byte dest ip
		// 34 2 byte source port
		// 36 2 byte destination port
		// 38 2 byte data length (-8 to get payload data length)
		// 40 2 byte checksum
		// 42 x byte UDP data
		// x byte padding
		try {
			boolean tx = false;

			byte[] srcIpBytes = new byte[4];
			for (int i = 0; i < 4; i++) {
				srcIpBytes[i] = bytes.get(26 + i).byteValue();
			}
			InetAddress srcIp = InetAddress.getByAddress(srcIpBytes);

			byte[] dstIpBytes = new byte[4];
			for (int i = 0; i < 4; i++) {
				dstIpBytes[i] = bytes.get(30 + i).byteValue();
			}
			InetAddress dstIp = InetAddress.getByAddress(dstIpBytes);

			if (Arrays.equals(localIpAddrBytes, srcIpBytes)) {
				tx = true;
			}

			int srcPort = ((bytes.get(34) & 0xff) << 8) | (bytes.get(35) & 0xff);
			int dstPort = ((bytes.get(36) & 0xff) << 8) | (bytes.get(37) & 0xff);
			int length = (((bytes.get(38) & 0xff) << 8) | (bytes.get(39) & 0xff)) - 8;

			byte[] payload = new byte[length];
			for (int i = 0; i < length; i++) {
				payload[i] = bytes.get(42 + i).byteValue();
			}
			InetSocketAddress remoteSocket;
			if (tx) {
				remoteSocket = new InetSocketAddress(dstIp, dstPort);
			} else {
				remoteSocket = new InetSocketAddress(srcIp, srcPort);
			}
			DatagramPacket packet = new DatagramPacket(payload, 0, length, remoteSocket);

			return new PcapPacket(tx, packet);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Test
	void findSystaComfort() {
		this.localIpAddrBytes = new byte[] { (byte) 0xc0, (byte) 0xa8, 0x0b, 0x01 };
		this.initializeData("s-touch-test-search-connect-disconnect.h");
		// 0 message should be tx search broadcast
		byte[] result = packetData.get(0).packet.getData();
		InetAddress bcastAddr = packetData.get(0).packet.getAddress();
		byte[] createdMessage = DeviceTouchSearch.createSearchMessage(bcastAddr).getData();
		assertArrayEquals(result, createdMessage);
		// 1 message should be the reply
		String rxMessage = DeviceTouchSearch.getSearchReplyString(packetData.get(1).packet);
		DeviceTouchDeviceInfo info = DeviceTouchSearch.parseDeviceTouchInfoString(rxMessage);
		// if the parsing of the info message is correct, is validated indirectly if the
		// following requests are valid and are matching the dumped packets
		// 2 message should be tx request port
		result = packetData.get(2).packet.getData();
		createdMessage = DeviceTouchSearch.createPortRequestMessage(info.mac, bcastAddr).getData();
		assertArrayEquals(result, createdMessage);
		// 3 message should be the reply
		rxMessage = DeviceTouchSearch.getPortReplyString(packetData.get(3).packet);
		info.port = DeviceTouchSearch.parsePortReplyString(rxMessage);
		assertEquals(info.port, 3477); // never saw a different port in returns
		// 4 request password
		result = packetData.get(4).packet.getData();
		createdMessage = DeviceTouchSearch.createPasswordRequestMessage(info.mac, bcastAddr).getData();
		assertArrayEquals(result, createdMessage);
		// 5 should be the password
		rxMessage = DeviceTouchSearch.getPasswordReplyString(packetData.get(5).packet);
		info.password = DeviceTouchSearch.parsePasswordReplyString(rxMessage);
		assertEquals(info.password, "1234"); // never saw a different password in returns
	}

	@Test
	void testCommunication() {
		this.localIpAddrBytes = new byte[] { (byte) 0xc0, (byte) 0xa8, 0x0b, 0x01 };
		this.initializeData("s-touch-test-communication.h");
		FakeSTouch stouch = new FakeSTouch();
		// stouch.setDebug(true);
		// configure stouch to be in the state after a successfull getPassAndConnect
		stouch.setInfo(deviceInfo);
		// 6 is the password reply for requesting a connection with the systa comfort
		byte[] result = packetData.get(6).packet.getData();
		byte[] createdMessage = stouch.createConnectionRequestMessage().getData();
		assertArrayEquals(result, createdMessage);
		// this is the point where getPassAndConnect finishes and all remaining
		// communication is handled by the connect() method
		int i = 7;
		boolean buttonIsActive = false;
		while (i + 1 < packetData.size()) {
			if (Arrays.equals(packetData.get(i).packet.getData(), stouch.createDisconnectRequestMessage().getData())) {
				break;
			}
			;
			if (packetData.get(i).tx) {
				// to start we need a rx packet.
				// ignore this packet
				// i += 2;
				i++;
				continue;
			} else {
				// sometimes the Systa Comfort is sending two or three commands before the
				// S-touch answers.
				// We need to resort in this case to have the expected command reply sequence
				/*
				 * if (!packetData.get(i + 1).tx && packetData.get(i + 1).packet.getData()[0] ==
				 * 9) { if (i + 2 < packetData.size() && packetData.get(i + 2).tx) { PcapPacket
				 * txPacket = packetData.remove(i + 2); packetData.add(i + 1, txPacket); } else
				 * if (i + 3 < packetData.size() && packetData.get(i + 3).tx) { PcapPacket
				 * txPacket = packetData.remove(i + 3); packetData.add(i + 1, txPacket); } else
				 * if (i + 4 < packetData.size() && packetData.get(i + 4).tx) { PcapPacket
				 * txPacket = packetData.remove(i + 4); packetData.add(i + 1, txPacket); } }
				 */

				// button presses are added to the reply
				// The endpoint under test doesn't know that this event is in the recorded data,
				// it has to be injected based on the messages from the dump
				result = packetData.get(i + 1).packet.getData();
				if (packetData.get(i + 1).tx) {
					if (packetData.get(i + 1).packet.getData()[0] == 8) {
						// the next message is a connection message
						// no need to check the reply
						i += 1;
						continue;
					}
					if (result[0] == 9) {
						// only command messages have the piggybacked event
						int len = result.length;
						if (result[len - 11] != -1 || result[len - 10] != -1 || result[len - 9] != -1
								|| result[len - 8] != -1 || result[len - 7] != -1) {
							// this message holds a press event
							byte buttonId = result[len - 11];
							int x = (result[len - 9]) << 8 | (result[len - 10] & 0xff);
							int y = (result[len - 7]) << 8 | (result[len - 8] & 0xff);
							stouch.getDisplay().setTouch(buttonId, x, y);
							buttonIsActive = true;
						} else if (result[len - 11] == -1 && buttonIsActive) {
							buttonIsActive = false;
							stouch.getDisplay().setTouch(-1, -1);
						}
					}

				}
				DatagramPacket reply = stouch.processCommands(packetData.get(i).packet);
				// saveDisplayAsPNG(stouch, String.format("testCommunication_%05d", (i+1)));
				// //+1 to match the pcap frame numbering
				if (reply == null) {
					// if no reply is generated, there should also not be a reply (tx message) in
					// the dump
					assertFalse(packetData.get(i + 1).tx);
					i += 1;
					continue;
				} else {
					// if a reply is generated, there should also be a reply (tx message) in the
					// dump
					try {
						assertTrue(packetData.get(i + 1).tx);
					} catch (AssertionError e) {
						System.out.println("Assert failure for packet " + (i + 1));
						printByteArrayAsHex(result);
						throw e;
					}
					try {
						assertEquals(result.length, reply.getLength(), "result and reply have different length");
					} catch (AssertionError e) {
						System.out.println("result and reply have different length");
						System.out.print("result: ");
						printByteArrayAsHex(result);
						createdMessage = new byte[reply.getLength()];
						System.arraycopy(reply.getData(), reply.getOffset(), createdMessage, 0, reply.getLength());
						System.out.print("reply:  ");
						printByteArrayAsHex(createdMessage);
						throw e;
					}
					createdMessage = new byte[reply.getLength()];
					System.arraycopy(reply.getData(), reply.getOffset(), createdMessage, 0, reply.getLength());
					// mask the dynamic data
					if (createdMessage[0] == 9) {
						// replies on commands have the free commands memory in the reply, which is not
						// deterministic
						// so we set it to all 0
						int len = createdMessage.length;
						createdMessage[len - 4] = 0;
						createdMessage[len - 3] = 0;
						result[len - 4] = 0;
						result[len - 3] = 0;
					}
					try {
						assertArrayEquals(result, createdMessage);
					} catch (AssertionError e) {
						System.out.println("Assert failure for packet " + (i + 1));
						System.out.println("reply.getLength(): " + reply.getLength() + ", createdMessage.length:"
								+ createdMessage.length);
						System.out.println("Expected Result: ");
						printByteArrayAsHex(result);
						System.out.println("Created Message: ");
						printByteArrayAsHex(createdMessage);
						throw e;
					}
					i += 2;
					continue;
				}
			}
		}
	}

	/**
	 * Save the {@code objectTree} from the display of the passed {@link FakeSTouch}
	 * as excalidraw.png-file
	 * 
	 * @param stouch
	 * @param filename
	 */
	@SuppressWarnings("unused")
	private void saveDisplayAsExcalidraw(FakeSTouch stouch, String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename + ".excalidraw.png"));
			writer.write(stouch.getDisplay().getContentAsExcalidrawJSON());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Save the {@code objectTree} from the display of the passed {@link FakeSTouch}
	 * as png-file
	 * 
	 * @param stouch
	 * @param filename
	 */
	@SuppressWarnings("unused")
	private void saveDisplayAsPNG(FakeSTouch stouch, String filename) {
		try {
			BufferedImage image = stouch.getDisplay().getContentAsImage();
			// Save image as PNG
			ImageIO.write(image, "PNG", new File(filename + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
