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
 * Provides the status information of a Home Assistant @see
 * <a href="https://www.home-assistant.io/integrations/water_heater/">Water
 * Heater</a>
 */
public class SystaWaterHeaterStatus {
	/**
	 * the temperature unit used by this Water Heater
	 */
	public enum tempUnit {
		TEMP_CELSIUS, TEMP_FAHRENHEIT, TEMP_KELVIN
	}

	/**
	 * possible operation modes of Home Assistant Water Heaters^
	 */
	public enum operationMode {
		OPERATION_MODE_HEAT, OPERATION_MODE_OFF
	}

	/**
	 * The minimum temperature that can be set.
	 */
	public double minTemp;
	/**
	 * The maximum temperature that can be set.
	 */
	public double maxTemp;
	/**
	 * The current temperature
	 */
	public double currentTemperature;
	/**
	 * The temperature we are trying to reach.
	 */
	public double targetTemperature;
	/**
	 * Upper bound of the temperature we are trying to reach.
	 */
	public double targetTemperatureHigh;
	/**
	 * Lower bound of the temperature we are trying to reach.
	 */
	public double targetTemperatureLow;
	/**
	 * One of TEMP_CELSIUS, TEMP_FAHRENHEIT, or TEMP_KELVIN.
	 */
	public tempUnit temperatureUnit;
	/**
	 * The current operation mode.
	 */
	public String currentOperation;
	/**
	 * List of possible operation modes.
	 */
	public String[] operationList;
	/**
	 * List of supported features.
	 */
	public String[] supportedFeatures;
	/**
	 * The current status of away mode.
	 */
	public boolean is_away_mode_on;
	/**
	 * The timestamp for this status.
	 */
	public long timestamp;
	/**
	 * The timestamp for this status as human readable string
	 */
	public String timestampString;
}
