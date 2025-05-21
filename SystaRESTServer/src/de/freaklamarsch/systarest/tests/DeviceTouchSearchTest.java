package de.freaklamarsch.systarest.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.net.DatagramPacket;
import java.net.InetAddress;

import org.junit.jupiter.api.Test;
import de.freaklamarsch.systarest.DeviceTouchSearch;

class DeviceTouchSearchTest {

    @Test
    void testCreateSearchMessage() throws Exception {
        InetAddress bcastAddress = InetAddress.getByName("255.255.255.255");
        DatagramPacket packet = DeviceTouchSearch.createSearchMessage(bcastAddress);

        assertNotNull(packet);
        assertEquals("0 1 A", new String(packet.getData()).trim());
        assertEquals(8001, packet.getPort());
        assertEquals(bcastAddress, packet.getAddress());
    }

    @Test
    void testParsePasswordReplyString() {
        String rxMessage = "0 7 1234";
        String password = DeviceTouchSearch.parsePasswordReplyString(rxMessage);

        assertEquals("1234", password);
    }

    @Test
    void testGetSearchReplyString() {
        byte[] data = "SC2 1 192.168.11.23 255.255.255.0 192.168.11.1".getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length);

        String reply = DeviceTouchSearch.getSearchReplyString(packet);
        assertEquals("SC2 1 192.168.11.23 255.255.255.0 192.168.11.1", reply);
    }
}