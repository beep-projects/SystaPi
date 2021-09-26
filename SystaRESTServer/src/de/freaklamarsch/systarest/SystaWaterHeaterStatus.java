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
	};

	/**
	 * possible operation modes of Home Assistant Water Heaters^
	 */
	public enum operationMode {
		OPERATION_MODE_HEAT, OPERATION_MODE_OFF
	};

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
