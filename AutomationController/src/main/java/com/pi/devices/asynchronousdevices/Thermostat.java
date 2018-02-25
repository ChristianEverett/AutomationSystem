/**
 * 
 */
package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
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
public class Thermostat extends AsynchronousDevice
{
	private List<String> temperatureSensors = null;
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
	
	private ThermostatMode currentMode = ThermostatMode.OFF;
	private ThermostatMode targetMode = ThermostatMode.OFF;
	
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
		
		start(INTERVAL, INTERVAL, TimeUnit.SECONDS);
	}

	/**
	 * Check the status of the temperature Sensor and update the state of the thermostat
	 */
	@Override
	protected synchronized void update() throws Exception
	{
		if (targetTempatureReached() && !modeChangeLock.get() && targetMode != ThermostatMode.FAN)
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
				case OFF:
					turnOff();
					break;
				case FAN:

					if ((COMPRESSOR.isState(ON) || HEAT.isState(ON)) && !fanLock.get())
					{
						fanLock.set(true);
						createTask(() -> fanLock.set(false), fanTurnOffDelay, TimeUnit.SECONDS);
					}

					FAN.setState(ON);
					COMPRESSOR.setState(OFF);
					HEAT.setState(OFF);
					currentMode = ThermostatMode.FAN;
					break;
				case COOL:
					if (!modeChangeLock.get())
					{
						FAN.setState(ON);
						COMPRESSOR.setState(ON);
						HEAT.setState(OFF);
						currentMode = ThermostatMode.COOL;
						//lockThermostat();
					}
					break;
				case HEAT:
					if (!modeChangeLock.get())
					{
						FAN.setState(ON);
						COMPRESSOR.setState(OFF);
						HEAT.setState(ON);
						currentMode = ThermostatMode.HEAT;
						//lockThermostat();
					}
					break;
				default:
					break;
				}
			}
		}			
	}
	
	@Override
	protected void performAction(DeviceState state)
	{
		synchronized (this)
		{
			if(state.contains(Params.TARGET_TEMPATURE))
			{
				Integer targetTemp = (Integer) state.getParam(Params.TARGET_TEMPATURE);
				targetTempInFehrenheit = targetTemp < MAX_TEMP && targetTemp > MIN_TEMP ? targetTemp : targetTempInFehrenheit;
			}
			
			if(state.contains(Params.TARGET_MODE))
			{
				targetMode = ThermostatMode.valueOf(((String) state.getParam(Params.TARGET_MODE)).toUpperCase());
				modeChangeLock.set(false);
			}
				
			try
			{
				update();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}	
		}	
	}
	
	@Override
	public DeviceState getState(DeviceState state)
	{
		state.setParam(Params.TARGET_TEMPATURE, targetTempInFehrenheit);
		state.setParam(Params.MODE, currentMode.toString());
		state.setParam(Params.TARGET_MODE, targetMode.toString());
		state.setParam(Params.TIME, fanTurnOffDelayTask.minutesUntilExecution());
		state.setParam(Params.ON, !modeChangeLock.get());
		
		return state;
	}

	@Override
	protected void tearDown()
	{
		turnOff();
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
		if (currentMode != ThermostatMode.OFF)
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
					if (COMPRESSOR.isState(OFF) && HEAT.isState(OFF) && !targetMode.equals(ThermostatMode.FAN)) 
						FAN.setState(OFF);
					fanLock.set(false);
				}, fanTurnOffDelay, TimeUnit.SECONDS);
			}
			else if(!fanLock.get())
			{
				FAN.setState(OFF);
			}

			currentMode = ThermostatMode.OFF;
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
		return (targetMode.equals(ThermostatMode.COOL) && temperature <= targetTempInFehrenheit)
				|| (targetMode.equals(ThermostatMode.HEAT) && temperature >= targetTempInFehrenheit);
	}

	@Override
	public boolean isAsynchronousDevice()
	{
		// Thermostat is Async-Sync device
		return false;
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
		OFF("off"), HEAT("heat"), COOL("cool"), FAN("fan");
		
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
