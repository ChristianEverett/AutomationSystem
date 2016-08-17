/**
 * 
 */
package com.thermostat;

/**
 * @author Christian Everett
 *
 */
public enum ThermostatMode
{
	OFF_MODE("off_mode"), HEAT_MODE("heat_mode"), COOL_MODE("cool_mode"), FAN_MODE("fan_mode");
	
	private String name;
	
	private ThermostatMode(String name)
	{
		this.name = name;
	}
	
	@Override
	public String toString()
	{
		return this.name;
	}
}
