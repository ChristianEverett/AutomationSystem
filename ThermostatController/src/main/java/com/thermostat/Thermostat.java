/**
 * 
 */
package com.thermostat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

/**
 * @author Christian Everett
 *
 */
public class Thermostat
{
	private final GpioController gpioController = GpioFactory.getInstance();
	private ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private static Thermostat singlton = null;
	
	public static final int MAX_TEMP = 76;
	public static final int MIN_TEMP = 70;
	
	private int targetTemp = 70;
	private double currentTemp = 70;
	
	private ThermostatMode mode = ThermostatMode.OFF_MODE;
	
	private GpioPinDigitalOutput FAN;
	private GpioPinDigitalOutput COMPRESSOR;
	private GpioPinDigitalOutput HEAT;
	
	static
	{
		System.loadLibrary("TempDriver");
	}
	
	private Thermostat()
	{
		FAN = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_27, PinState.HIGH);
		COMPRESSOR = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_28, PinState.HIGH);
		HEAT = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_29, PinState.HIGH);
		
		exec.scheduleAtFixedRate(() -> 
		{
			checkStatus();
		}, 1, 1, TimeUnit.MINUTES);
	}
	
	public ThermostatSetting performAction(ThermostatSetting setting)
	{
		int temp = setting.getTemp();
		ThermostatMode smode = setting.getMode();
		
		if(temp < MAX_TEMP && temp > MIN_TEMP)
		{
			this.targetTemp = temp;
			this.mode = smode;
			
			return setting;
		}
		else
		{
			return new ThermostatSetting(mode, targetTemp);
		}
	}
	
	private void checkStatus()
	{
		this.currentTemp = (int)(1.8 * readSensor()) + 32;
		
		switch (mode)
		{
		case OFF_MODE:
			setState(false, false, false);
			break;
		case FAN_MODE:
			setState(true, false, false);	
			break;
		case COOL_MODE:
			if(targetTemp >= currentTemp)
				setState(false, false, false);
			else
				setState(true, true, false);	
			break;
		case HEAT_MODE:
			if(targetTemp <= currentTemp)
				setState(false, false, false);
			else
				setState(true, false, true);	
			break;
		default:
			break;
		}
	}
	
	/**
	 * 
	 * @param fan
	 * @param compressor
	 * @param heat
	 */
	private void setState(boolean fan, boolean compressor, boolean heat)
	{
		FAN.setState(!fan);
		COMPRESSOR.setState(!compressor);
		HEAT.setState(!heat);
	}
	
	public double getTempature()
	{
		return currentTemp;
	}
	
	public static Thermostat getInstance()
	{
		if(singlton == null)
			singlton = new Thermostat();
		
		return singlton;
	}
	
	private native double readSensor();
	
	public static class ThermostatSetting
	{
		private final ThermostatMode mode;
		private final int temp;
		
		public ThermostatSetting(ThermostatMode mode, int temp)
		{
			super();
			this.mode = mode;
			this.temp = temp;
		}
		/**
		 * @return the mode
		 */
		public ThermostatMode getMode()
		{
			return mode;
		}
		/**
		 * @return the temp
		 */
		public int getTemp()
		{
			return temp;
		}
	}
}
