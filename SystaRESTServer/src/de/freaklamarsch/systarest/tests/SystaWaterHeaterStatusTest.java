package de.freaklamarsch.systarest.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import de.freaklamarsch.systarest.SystaWaterHeaterStatus;
import de.freaklamarsch.systarest.SystaWaterHeaterStatus.tempUnit;

class SystaWaterHeaterStatusTest {

    @Test
    void testInitialization() {
        SystaWaterHeaterStatus status = new SystaWaterHeaterStatus();
        status.minTemp = 10.0;
        status.maxTemp = 60.0;
        status.currentTemperature = 45.0;
        status.targetTemperature = 50.0;
        status.temperatureUnit = tempUnit.TEMP_CELSIUS;
        status.currentOperation = "HEAT";
        status.is_away_mode_on = false;

        assertEquals(10.0, status.minTemp);
        assertEquals(60.0, status.maxTemp);
        assertEquals(45.0, status.currentTemperature);
        assertEquals(50.0, status.targetTemperature);
        assertEquals(tempUnit.TEMP_CELSIUS, status.temperatureUnit);
        assertEquals("HEAT", status.currentOperation);
        assertFalse(status.is_away_mode_on);
    }

    @Test
    void testEdgeCases() {
        SystaWaterHeaterStatus status = new SystaWaterHeaterStatus();
        status.minTemp = -273.15; // Absolute zero in Celsius
        status.maxTemp = Double.MAX_VALUE;
        status.currentTemperature = Double.NaN;

        assertEquals(-273.15, status.minTemp);
        assertEquals(Double.MAX_VALUE, status.maxTemp);
        assertTrue(Double.isNaN(status.currentTemperature));
    }
}