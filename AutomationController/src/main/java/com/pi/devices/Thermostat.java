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
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.util.GPIO_PIN;
import com.pi.model.DeviceState;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;

/**
 * @author Christian Everett
 *
 */
public class Thermostat extends Device
{
	private List<String> temperatureSensors = null;
	private List<Integer> cachedTempatures = null;
	private Task updateTask = null;
	private Task fanTurnOffDelayTask = null;
	private Lock lock = new ReentrantLock();
	
	private final GpioPinDigitalOutput FAN;
	private final GpioPinDigitalOutput COMPRESSOR;
	private final GpioPinDigitalOutput HEAT;
	
	private final long INTERVAL = 5;
	private long modeChangeDelay = 0;
	private long fanTurnOffDelay = 40;
	private AtomicBoolean fanLock = new AtomicBoolean(false);
	private AtomicBoolean modeChangeLock = new AtomicBoolean(false);
	
	private final int MAX_TEMP = 75;
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
		cachedTempatures = new ArrayList<>(sensors.size());
		
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
				update();
			}
			catch (Exception e)
			{
				Application.LOGGER.severe(e.getMessage());
			}
		}, INTERVAL, INTERVAL, TimeUnit.SECONDS);
	}

	@Override
	public void performAction(DeviceState state)
	{
		Integer targetTemp = (Integer) state.getParam(DeviceState.TARGET_TEMPATURE);
		ThermostatMode mode = ThermostatMode.valueOf(((String) state.getParam(DeviceState.TARGET_MODE)).toUpperCase());
		
		if (targetTemp < MAX_TEMP && targetTemp > MIN_TEMP)
		{
			lock.lock();
			targetTempInFehrenheit = targetTemp;
			targetMode = mode;
			lock.unlock();
			update();
		}
	}
	
	@Override
	public DeviceState getState()
	{
		lock.lock();
		DeviceState state = new DeviceState(name);
		state.setParam(DeviceState.TARGET_TEMPATURE, targetTempInFehrenheit);
		state.setParam(DeviceState.MODE, currentMode.toString());
		state.setParam(DeviceState.TARGET_MODE, targetMode.toString());
		lock.unlock();

		return state;
	}

	@Override
	public void close()
	{
		lock.lock();
		updateTask.cancel();
		turnOff();
		lock.unlock();
	}
	
	/**
	 * Check the status of the temperature Sensor and update the state of the thermostat
	 */
	private void update()
	{
		lock.lock();
		
		if (!targetTempatureReached() && !modeChangeLock.get())
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
				FAN.setState(ON);
				COMPRESSOR.setState(ON);
				HEAT.setState(OFF);
				currentMode = ThermostatMode.COOL_MODE;
				break;
			case HEAT_MODE:
				FAN.setState(ON);
				COMPRESSOR.setState(OFF);
				HEAT.setState(ON);
				currentMode = ThermostatMode.HEAT_MODE;
				break;
			default:
				break;
			}
		}
		else 
		{
			turnOff();
			modeChangeLock.set(true);
			
			createTask(() -> {modeChangeLock.set(false);}, modeChangeDelay, TimeUnit.MINUTES); 
		}
		
		lock.unlock();
	}
	
	private void turnOff()
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
		for(String sensor : temperatureSensors)
		{
			DeviceState state = (DeviceState) Device.getDeviceState(sensor);
			
			if(state != null)
			{
				Integer temperature = (Integer)state.getParam(DeviceState.TEMPATURE);
				if(temperature != null && compareTemperatures(temperature))
					return true;
			}
		}
		
		return false;
	}
	
	private boolean compareTemperatures(int temperature)
	{
		return (targetMode.equals(ThermostatMode.COOL_MODE) && temperature <= targetTempInFehrenheit)
				|| (targetMode.equals(ThermostatMode.HEAT_MODE) && temperature >= targetTempInFehrenheit);
	}
	
	@Override
	public String getType()
	{
		return DeviceType.THERMOSTAT;
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
