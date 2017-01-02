
package com.pi.devices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.devices.Thermostat.TemperatureDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.HttpClient;
import com.pi.model.Action;
import com.pi.model.DeviceState;

/**
 * @author Christian Everett
 *
 */
public class TemperatureSensor extends Device
{
	private static final long sensorUpdateFrequency = 25L;
	private Task tempatureReadingTask;
	private int headerPin = -1;

	private int sensorTempature = -1;
	private int sensorHumidity = -1;
	
	public TemperatureSensor(String name, int headerPin, int sensortype) throws IOException
	{
		super(name);
		this.headerPin = headerPin;

		tempatureReadingTask = createTask(()->
		{
			try
			{
				SensorReading reading = readSensor(pins.get(headerPin).getBCM_Pin(), sensortype);
				sensorTempature = Math.round((1.8F * reading.getTempature()) + 32);
				sensorHumidity = Math.round(reading.getHumidity());
			}
			catch (Throwable e)
			{
				Application.LOGGER.severe(e.getClass() + " - " + e.getMessage());
			}

		}, 5L, sensorUpdateFrequency, TimeUnit.SECONDS);
	}

	@Override
	public void performAction(Action action)
	{
	}

	@Override
	public DeviceState getState()
	{
		return new TempatureState(name, sensorTempature, sensorHumidity);
	}

	@Override
	public void close()
	{
		tempatureReadingTask.cancel();
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
	
	public static class TempatureState extends DeviceState implements TemperatureDevice
	{
		private int sensorTemperature = -1;
		private int sensorHumidity = -1;
		
		public TempatureState(String deviceName, int sensorTemperature, int sensorHumidity)
		{
			super(deviceName);
			this.sensorTemperature = sensorTemperature;
			this.sensorHumidity = sensorHumidity;
		}

		/**
		 * @return the sensorTempature
		 */
		public int getTemperature()
		{
			return sensorTemperature;
		}

		/**
		 * @return the sensorHumidity
		 */
		public int getHumidity()
		{
			return sensorHumidity;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(sensorTemperature, sensorHumidity, super.hashCode());
		}

		@Override
		public boolean equals(Object object)
		{
			if(!(object instanceof TempatureState))
				return false;
			TempatureState tempatureState = (TempatureState) object;
			
			return Objects.equals(sensorTemperature, tempatureState.sensorTemperature) &&
					Objects.equals(sensorHumidity, tempatureState.sensorHumidity) &&
					super.equals(object);
		}

		@Override
		public String getType()
		{
			return DeviceType.TEMP_SENSOR;
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
