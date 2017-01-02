/**
 * 
 */
package com.pi.devices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.devices.TemperatureSensor.TempatureState;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.HttpClient;
import com.pi.model.Action;
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
		
		FAN = gpioController.provisionDigitalOutputPin(pins.get(fanHeader).getWiringPI_Pin(), PinState.HIGH);
		COMPRESSOR = gpioController.provisionDigitalOutputPin(pins.get(compressorHeader).getWiringPI_Pin(), PinState.HIGH);
		HEAT = gpioController.provisionDigitalOutputPin(pins.get(heatHeader).getWiringPI_Pin(), PinState.HIGH);
		
		this.modeChangeDelay = modeChangeDelay;
		
		updateTask = createTask(() ->
		{
			update();
		}, 60L, INTERVAL, TimeUnit.SECONDS);
	}

	@Override
	public void performAction(Action action)
	{
		HashMap<String, String> responseParams = HttpClient.URLEncodedDataToHashMap(action.getData());
		
		int targetTemp = (int) Float.parseFloat(responseParams.get(QueryParams.TARGET_TEMP));
		ThermostatMode mode = ThermostatMode.valueOf(responseParams.get(QueryParams.TARGET_MODE).toUpperCase());

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
		ThermostatState state = new ThermostatState(name, targetTempInFehrenheit, currentMode.toString(), targetMode.toString());
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
		
		if (!targetTempatureReached())
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
			TemperatureDevice state = (TemperatureDevice) Device.getDeviceState(sensor);
			
			if(state != null)
			{
				if(compareTemperatures(state.getTemperature()))
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
		 * @return the tempature
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
	
	public static class ThermostatState extends DeviceState
	{
		private int targetTemperature;
		private String mode;
		private String targetMode;

		public ThermostatState(String deviceName, int targetTemperature, String mode, String targetMode)
		{
			super(deviceName);
			this.targetTemperature = targetTemperature;
			this.mode = mode;
			this.targetMode = targetMode;
		}

		public int getTargetTemperature()
		{
			return targetTemperature;
		}

		public String getMode()
		{
			return mode;
		}	
		
		public String getTargetMode()
		{
			return targetMode;
		}

		@Override
		public String getType()
		{
			return DeviceType.THERMOSTAT;
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
	
	public interface TemperatureDevice
	{
		public int getTemperature();
	}
	
	private interface QueryParams
	{
		public static final String TARGET_TEMP = "target_temp";
		public static final String TARGET_MODE = "target_mode";
	}
}
