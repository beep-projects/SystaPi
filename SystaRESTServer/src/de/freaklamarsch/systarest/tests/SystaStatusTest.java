package de.freaklamarsch.systarest.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import de.freaklamarsch.systarest.SystaStatus;

class SystaStatusTest {

    @Test
    void testInitialization() {
        SystaStatus status = new SystaStatus();
        status.outsideTemp = 15.5;
        status.circuit1FlowTemp = 45.0;
        status.circuit1ReturnTemp = 40.0;
        status.hotWaterTemp = 50.0;

        assertEquals(15.5, status.outsideTemp);
        assertEquals(45.0, status.circuit1FlowTemp);
        assertEquals(40.0, status.circuit1ReturnTemp);
        assertEquals(50.0, status.hotWaterTemp);
    }

    @Test
    void testEdgeCases() {
        SystaStatus status = new SystaStatus();
        status.outsideTemp = -273.15; // Absolute zero
        status.circuit1FlowTemp = Double.MAX_VALUE;
        status.circuit1ReturnTemp = Double.MIN_VALUE;

        assertEquals(-273.15, status.outsideTemp);
        assertEquals(Double.MAX_VALUE, status.circuit1FlowTemp);
        assertEquals(Double.MIN_VALUE, status.circuit1ReturnTemp);
    }
}