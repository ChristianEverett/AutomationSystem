
package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.util.GPIO_PIN;
import com.pi.model.DeviceState;

/**
 * @author Christian Everett
 *
 */
public class TemperatureSensor extends AsynchronousDevice
{
	private static final long sensorUpdateFrequency = 25L;
	private Task tempatureReadingTask;
	private int headerPin = -1;

	private int sensorTempature = -1;
	private int sensorHumidity = -1;
	
	private int sensorType = 11;
	
	public TemperatureSensor(String name, int headerPin, int sensorType) throws IOException
	{
		super(name, 1L, sensorUpdateFrequency, TimeUnit.SECONDS);
		this.headerPin = headerPin;
		this.sensorType = sensorType;
	}

	@Override
	protected void update() throws Exception
	{
		SensorReading reading = readSensor(GPIO_PIN.getBCM_Pin(headerPin), sensorType);
		
		if((sensorTempature != reading.getTempature()) || (sensorHumidity != reading.getHumidity()))
			update(getState(false));
		
		sensorTempature = Math.round((1.8F * reading.getTempature()) + 32);
		sensorHumidity = Math.round(reading.getHumidity());		
	}
	
	@Override
	protected void performAction(DeviceState state)
	{
	}

	@Override
	public DeviceState getState(Boolean forDatabase)
	{
		DeviceState state = Device.createNewDeviceState(name);
		state.setParam(Params.TEMPATURE, sensorTempature);
		state.setParam(Params.HUMIDITY, sensorHumidity);
		
		return state;
	}

	@Override
	protected void tearDown()
	{
		tempatureReadingTask.cancel();
	}
	
	@Override
	public List<String> getExpectedParams()
	{
		return new ArrayList<String> ();
	}

	private int celsiusToFahrenheit(float c)
	{
		return (int) (Math.round(c * 1.8) + 32);
	}

	private float fahrenheitToCelsius(int f)
	{
		return (float) ((f - 32) / 1.8);
	}
	
	@Override
	public String getType()
	{
		return DeviceType.TEMP_SENSOR;
	}
	
	private native SensorReading readSensor(int pin, int type);

	static
	{
		System.loadLibrary("TempDriver");
	}
	
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
	
	@XmlRootElement(name = DEVICE)
	public static class TemperatureSensorConfig extends DeviceConfig
	{
		private int headerPin, sensortype;

		@Override
		public Device buildDevice() throws IOException
		{
			return new TemperatureSensor(name, headerPin, sensortype);
		}

		@XmlElement
		public void setHeader(int headerPin)
		{
			this.headerPin = headerPin;
		}

		@XmlElement
		public void setSensorType(int sensortype)
		{
			this.sensortype = sensortype;
		}
	}
}
