/**
 * 
 */
package com.thermostat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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

	private float targetTemp = -1;
	private float currentTemp = -1;

	private ThermostatMode currentMode = ThermostatMode.OFF_MODE;
	private ThermostatMode targetMode = ThermostatMode.OFF_MODE;

	private float currentHumidity = -1;

	private GpioPinDigitalOutput FAN;
	private GpioPinDigitalOutput COMPRESSOR;
	private GpioPinDigitalOutput HEAT;

	private final PinState ON = PinState.LOW;
	private final PinState OFF = PinState.HIGH;

	private int reActivationDelayInSeconds = 120;

	private boolean isInDelayPeriod = false;
	private boolean autoModeEnabled = false;

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
		}, 1, 5, TimeUnit.SECONDS);
	}

	public void performAction(ThermostatSetting setting)
	{
		float temp = setting.getTemp();
		ThermostatMode newMode = setting.getMode();

		this.targetTemp = temp;
		this.targetMode = newMode;
	}

	private void checkStatus()
	{
		SensorReading reading = readSensor();
		this.currentTemp = reading.getTempature();
		this.currentHumidity = reading.getHumidity();
		

		if (!isInDelayPeriod || targetMode.equals(ThermostatMode.OFF_MODE))
		{
			switch (targetMode)
			{
			case OFF_MODE:
				turnOff();
				break;
			case FAN_MODE:
				FAN.setState(ON);
				COMPRESSOR.setState(OFF);
				HEAT.setState(OFF);
				currentMode = ThermostatMode.FAN_MODE;
				break;
			case COOL_MODE:
				if (!hasReachedTargetTempature())
				{
					FAN.setState(ON);
					COMPRESSOR.setState(ON);
					HEAT.setState(OFF);
					currentMode = ThermostatMode.COOL_MODE;
				}
				break;
			case HEAT_MODE:
				if (!hasReachedTargetTempature())
				{
					FAN.setState(ON);
					COMPRESSOR.setState(OFF);
					HEAT.setState(ON);
					currentMode = ThermostatMode.HEAT_MODE;
				}
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Method not in use
	 * @return
	 */
	private boolean hasReachedTargetTempature()
	{
		if (currentMode.equals(ThermostatMode.COOL_MODE) && targetTemp >= currentTemp && autoModeEnabled)
		{
			turnOff();
			exec.schedule(() ->
			{
				isInDelayPeriod = false;
			}, reActivationDelayInSeconds, TimeUnit.SECONDS);
			isInDelayPeriod = true;
			return true;
		}
		else if (currentMode.equals(ThermostatMode.HEAT_MODE) && targetTemp <= currentTemp && autoModeEnabled)
		{
			turnOff();
			exec.schedule(() ->
			{
				isInDelayPeriod = false;
			}, reActivationDelayInSeconds, TimeUnit.SECONDS);
			isInDelayPeriod = true;
			return true;
		}
		else
		{
			return false;
		}
	}

	private void turnOff()
	{
		if (currentMode != ThermostatMode.OFF_MODE)
		{
			if (COMPRESSOR.isState(ON) || HEAT.isState(ON))
			{
				COMPRESSOR.setState(OFF);
				HEAT.setState(OFF);

				exec.schedule(() ->
				{
					if (COMPRESSOR.isState(OFF) && HEAT.isState(OFF) && !targetMode.equals(ThermostatMode.FAN_MODE)) 
						FAN.setState(OFF);
				}, 35, TimeUnit.SECONDS);
			}
			else
			{
				FAN.setState(OFF);
			}

			currentMode = ThermostatMode.OFF_MODE;
		}
	}

	public ThermostatMode getMode()
	{
		return currentMode;
	}

	public ThermostatMode getTargetMode()
	{
		return targetMode;
	}

	public float getTempature()
	{
		return currentTemp;
	}

	public float getTargetTemp()
	{
		return targetTemp;
	}

	public float getHumidity()
	{
		return currentHumidity;
	}

	public void setAutoMode(boolean autoModeEnabled)
	{
		this.autoModeEnabled = autoModeEnabled;
	}

	public void close()
	{
		try
		{
			currentMode = ThermostatMode.OFF_MODE;

			if (COMPRESSOR.isState(ON) || HEAT.isState(ON))
			{
				COMPRESSOR.setState(OFF);
				HEAT.setState(OFF);
				Thread.sleep(10000);
			}
		}
		catch (Exception e)
		{
			Main.LOGGER.severe(e.getMessage());
		}
		finally
		{
			COMPRESSOR.setState(OFF);
			HEAT.setState(OFF);
			FAN.setState(OFF);
		}
	}

	public static Thermostat getInstance()
	{
		if (singlton == null) singlton = new Thermostat();

		return singlton;
	}

	private native SensorReading readSensor();

	public class SensorReading
	{
		private float tempature = -1;
		private float humidity = -1;

		public SensorReading(float tempature, float humidity)
		{
			this.tempature = tempature;
			this.humidity = humidity;
		}

		/**
		 * @return the tempature
		 */
		public float getTempature()
		{
			return tempature;
		}

		/**
		 * @return the humidity
		 */
		public float getHumidity()
		{
			return humidity;
		}
	}

	public static class ThermostatSetting
	{
		private final ThermostatMode mode;
		private final float temp;

		public ThermostatSetting(ThermostatMode mode, float temp)
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
		public float getTemp()
		{
			return temp;
		}
	}
}
