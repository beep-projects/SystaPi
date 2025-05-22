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

/**
 * Represents the status of a water heater, designed to be compatible with the
 * Home Assistant water heater entity model.
 * <p>
 * This class serves as a data holder for various attributes of a water heater,
 * such as current and target temperatures, operational modes, and supported features.
 * It is typically populated by {@link FakeSystaWeb} with data derived from the
 * Paradigma SystaComfort heating system.
 * </p>
 * <p>
 * Note: Fields are currently public for direct access. Future enhancements might
 * include using getter and setter methods for better encapsulation.
 * </p>
 *
 * @see <a href="https://developers.home-assistant.io/docs/core/entity/water-heater/">Home Assistant Water Heater Entity</a>
 */
public class SystaWaterHeaterStatus {

    /**
     * Defines the temperature units that can be used by the water heater.
     * Corresponds to Home Assistant's temperature unit constants.
     */
    public enum tempUnit {
        /** Temperature in Celsius. */
        TEMP_CELSIUS,
        /** Temperature in Fahrenheit. */
        TEMP_FAHRENHEIT,
        /** Temperature in Kelvin. */
        TEMP_KELVIN
    }

    // The 'operationMode' enum was commented out in the original and seems to be replaced by
    // String-based currentOperation and operationList for Home Assistant compatibility.
    // If it were to be used, its Javadoc would be:
    // /**
    // * Defines possible specific operation modes for a water heater.
    // * Note: Home Assistant typically uses a list of string modes.
    // */
    // public enum operationMode {
    //  OPERATION_MODE_HEAT, // Example: Actively heating
    //  OPERATION_MODE_OFF   // Example: Heater is off
    // }

    /**
     * The minimum temperature that can be set for the water heater.
     * Corresponds to Home Assistant's {@code min_temp} attribute.
     * Unit is defined by {@link #temperatureUnit}.
     */
    public double minTemp;

    /**
     * The maximum temperature that can be set for the water heater.
     * Corresponds to Home Assistant's {@code max_temp} attribute.
     * Unit is defined by {@link #temperatureUnit}.
     */
    public double maxTemp;

    /**
     * The current temperature of the water.
     * Corresponds to Home Assistant's {@code current_temperature} attribute.
     * Unit is defined by {@link #temperatureUnit}.
     */
    public double currentTemperature;

    /**
     * The target temperature the water heater is trying to reach.
     * Corresponds to Home Assistant's {@code temperature} (target temperature) attribute.
     * Unit is defined by {@link #temperatureUnit}.
     */
    public double targetTemperature;

    /**
     * The upper bound of the target temperature range, if applicable (e.g., for systems with hysteresis).
     * Corresponds to Home Assistant's {@code target_temp_high} attribute.
     * Unit is defined by {@link #temperatureUnit}.
     */
    public double targetTemperatureHigh;

    /**
     * The lower bound of the target temperature range, if applicable (e.g., for systems with hysteresis).
     * Corresponds to Home Assistant's {@code target_temp_low} attribute.
     * Unit is defined by {@link #temperatureUnit}.
     */
    public double targetTemperatureLow;

    /**
     * The unit of measurement for temperature values (e.g., Celsius, Fahrenheit).
     * Must be one of the values defined in the {@link tempUnit} enum.
     * Corresponds to Home Assistant's {@code temperature_unit} attribute.
     */
    public tempUnit temperatureUnit;

    /**
     * The current operational mode of the water heater (e.g., "off", "heat", "eco", "performance").
     * This string value should be one of the modes listed in {@link #operationList}.
     * Corresponds to Home Assistant's {@code operation_mode} attribute.
     */
    public String currentOperation;

    /**
     * An array of strings representing the available operational modes for the water heater.
     * (e.g., ["off", "heat", "eco"]).
     * Corresponds to Home Assistant's {@code operation_list} attribute.
     */
    public String[] operationList;

    /**
     * An array of strings representing the features supported by the water heater entity.
     * (e.g., "TARGET_TEMPERATURE", "OPERATION_MODE", "AWAY_MODE").
     * These correspond to constants defined in Home Assistant for water heater features.
     */
    public String[] supportedFeatures;

    /**
     * Indicates whether the "away mode" is currently active on the water heater.
     * Corresponds to Home Assistant's {@code away_mode} attribute (true if on, false if off).
     */
    public boolean is_away_mode_on; // Snake case matches Home Assistant attribute names.

    /**
     * Timestamp of when this status data was generated or received, in milliseconds from the epoch (UTC).
     */
    public long timestamp;

    /**
     * ISO 8601 compliant textual representation of the {@link #timestamp}
     * (e.g., "2023-10-27T10:30:00.123+02:00").
     */
    public String timestampString;
}
