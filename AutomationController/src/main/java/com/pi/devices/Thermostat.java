/**
 * 
 */
package com.pi.devices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.Application;
import com.pi.SystemLogger;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.util.GPIO_PIN;
import com.pi.model.DeviceState;
import com.pi.services.TaskExecutorService.Task;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;

/**
 * @author Christian Everett
 *
 */
public class Thermostat extends Device
{
	private List<String> temperatureSensors = null;
	private Task updateTask = null;
	private Task fanTurnOffDelayTask = null;
	
	private GpioPinDigitalOutput FAN = null;
	private GpioPinDigitalOutput COMPRESSOR = null;
	private GpioPinDigitalOutput HEAT = null;
	
	private final long INTERVAL = 5;
	private long modeChangeDelay = 0;
	private long fanTurnOffDelay = 45;
	private AtomicBoolean fanLock = new AtomicBoolean(false);
	private AtomicBoolean modeChangeLock = new AtomicBoolean(false);
	
	private final int MAX_TEMP = 80;
	private final int MIN_TEMP = 60;
	
	private final PinState ON = PinState.LOW;
	private final PinState OFF = PinState.HIGH;
	
	private ThermostatMode currentMode = ThermostatMode.OFF_MODE;
	private ThermostatMode targetMode = ThermostatMode.OFF_MODE;
	
	private int targetTempInFehrenheit = 68;
	
	public Thermostat
	(String name, List<String> sensors, int fanHeader, int compressorHeader, int heatHeader, long modeChangeDelay) throws IOException
	{
		super(name);
		temperatureSensors = sensors;
		
		//Make task non-null
		fanTurnOffDelayTask = createTask(() -> {}, 0L, TimeUnit.MILLISECONDS);
		
		FAN = gpioController.provisionDigitalOutputPin(GPIO_PIN.getWiringPI_Pin(fanHeader), PinState.HIGH);
		COMPRESSOR = gpioController.provisionDigitalOutputPin(GPIO_PIN.getWiringPI_Pin(compressorHeader), PinState.HIGH);
		HEAT = gpioController.provisionDigitalOutputPin(GPIO_PIN.getWiringPI_Pin(heatHeader), PinState.HIGH);
		
		this.modeChangeDelay = modeChangeDelay;
		
		updateTask = createTask(() ->
		{
			try
			{
				process();
			}
			catch (Exception e)
			{
				SystemLogger.getLogger().severe(e.getMessage());
			}
		}, INTERVAL, INTERVAL, TimeUnit.SECONDS);
	}

	@Override
	protected void performAction(DeviceState state)
	{
		Integer targetTemp = (Integer) state.getParam(Params.TARGET_TEMPATURE, false);
		ThermostatMode mode = ThermostatMode.valueOf(((String) state.getParamNonNull(Params.TARGET_MODE)).toUpperCase());
		
		if (targetTemp == null || targetTemp < MAX_TEMP && targetTemp > MIN_TEMP)
		{
			synchronized (this)
			{
				if(targetTemp != null)
					targetTempInFehrenheit = targetTemp;
				targetMode = mode;
			}
			process();
		}
	}
	
	@Override
	public DeviceState getState(Boolean forDatabase)
	{
		DeviceState state = Device.createNewDeviceState(name);
		state.setParam(Params.TARGET_TEMPATURE, targetTempInFehrenheit);
		state.setParam(Params.MODE, currentMode.toString());
		state.setParam(Params.TARGET_MODE, targetMode.toString());

		return state;
	}

	@Override
	protected void tearDown()
	{
		updateTask.cancel();
		turnOff();
	}
	
	/**
	 * Check the status of the temperature Sensor and update the state of the thermostat
	 */
	private synchronized void process()
	{
		if (targetTempatureReached() && !modeChangeLock.get() && targetMode != ThermostatMode.FAN_MODE)
		{			
			turnOff();
			lockThermostat();
		}
		else 
		{
			if (currentMode != targetMode)
			{
				switch (targetMode)
				{
				case OFF_MODE:
					turnOff();
					break;
				case FAN_MODE:
					
					if((COMPRESSOR.isState(ON) || HEAT.isState(ON)) && !fanLock.get())
					{
						fanLock.set(true);
						createTask(()->fanLock.set(false), fanTurnOffDelay, TimeUnit.SECONDS);
					}
					
					FAN.setState(ON);
					COMPRESSOR.setState(OFF);
					HEAT.setState(OFF);
					currentMode = ThermostatMode.FAN_MODE;
					break;
				case COOL_MODE:
					if (!modeChangeLock.get())
					{
						FAN.setState(ON);
						COMPRESSOR.setState(ON);
						HEAT.setState(OFF);
						currentMode = ThermostatMode.COOL_MODE;
						//lockThermostat();
					}
					break;
				case HEAT_MODE:
					if (!modeChangeLock.get())
					{
						FAN.setState(ON);
						COMPRESSOR.setState(OFF);
						HEAT.setState(ON);
						currentMode = ThermostatMode.HEAT_MODE;
						//lockThermostat();
					}
					break;
				default:
					break;
				}
			}	
		}	
	}

	private synchronized void lockThermostat()
	{
		modeChangeLock.set(true);

		createTask(() ->
		{
			modeChangeLock.set(false);
		}, modeChangeDelay, TimeUnit.MINUTES);
	}
	
	private synchronized void turnOff()
	{
		if (currentMode != ThermostatMode.OFF_MODE)
		{
			if (COMPRESSOR.isState(ON) || HEAT.isState(ON))
			{
				COMPRESSOR.setState(OFF);
				HEAT.setState(OFF);
				fanLock.set(true);

				if(!fanTurnOffDelayTask.isDone() && !fanTurnOffDelayTask.isCancelled())
					fanTurnOffDelayTask.cancel();
				
				fanTurnOffDelayTask = createTask(() ->
				{
					if (COMPRESSOR.isState(OFF) && HEAT.isState(OFF) && !targetMode.equals(ThermostatMode.FAN_MODE)) 
						FAN.setState(OFF);
					fanLock.set(false);
				}, fanTurnOffDelay, TimeUnit.SECONDS);
			}
			else if(!fanLock.get())
			{
				FAN.setState(OFF);
			}

			currentMode = ThermostatMode.OFF_MODE;
		}
	}
	
	private boolean targetTempatureReached()
	{
		boolean sensorFound = false;
		
		if(temperatureSensors == null || temperatureSensors.isEmpty())
			return true;
		
		for(String sensor : temperatureSensors)
		{
			DeviceState state = (DeviceState) Device.getDeviceState(sensor);
			
			if(state != null)
			{
				sensorFound = true;
				Integer temperature = (Integer)state.getParamNonNull(Params.TEMPATURE);
				if(compareTemperatures(temperature))
					return true;
			}
		}
		
		return !sensorFound;
	}
	
	private boolean compareTemperatures(int temperature)
	{
		return (targetMode.equals(ThermostatMode.COOL_MODE) && temperature <= targetTempInFehrenheit)
				|| (targetMode.equals(ThermostatMode.HEAT_MODE) && temperature >= targetTempInFehrenheit);
	}

	public class SensorReading
	{
		private float temperature = 0;
		private float humidity = 0;

		public SensorReading(float temperature, float humidity)
		{
			this.temperature = temperature;
			this.humidity = humidity;
		}

		/**
		 * @return the temperature
		 */
		public float getTemperature()
		{
			return temperature;
		}

		/**
		 * @return the humidity
		 */
		public float getHumidity()
		{
			return humidity;
		}
	}
	
	@XmlRootElement(name = DEVICE)
	public static class ThermostatConfig extends DeviceConfig
	{
		private List<String> sensors;
		private int fanHeader, compressorHeader, heatHeader; 
		private long modeChangeDelay;

		@Override
		public Device buildDevice() throws IOException
		{
			return new Thermostat(name, sensors, fanHeader, compressorHeader, heatHeader, modeChangeDelay);
		}

		public void setSensor(List<String> sensors)
		{
			this.sensors = sensors;
		}

		//XML parser throws NullPointerException if accessor is left out
		public List<String> getSensor()
		{
			return sensors;
		}
		
		@XmlElement
		public void setFan(int fanHeader)
		{
			this.fanHeader = fanHeader;
		}

		@XmlElement
		public void setCompressor(int compressorHeader)
		{
			this.compressorHeader = compressorHeader;
		}

		@XmlElement
		public void setHeat(int heatHeader)
		{
			this.heatHeader = heatHeader;
		}

		@XmlElement
		public void setModeChangeDelay(long modeChangeDelay)
		{
			this.modeChangeDelay = modeChangeDelay;
		}
	}
	
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
}
