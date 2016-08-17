/**
 * 
 */
package com.pi.infrastructure;

import java.io.IOException;
import java.util.HashMap;

import javax.persistence.criteria.CriteriaBuilder.In;

import org.w3c.dom.Element;

import com.pi.Application;
import com.pi.devices.Led;
import com.pi.devices.Outlet;
import com.pi.devices.Switch;
import com.pi.devices.TempatureSensor;
import com.pi.devices.thermostat.ThermostatController;
import com.pi.repository.Action;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

/**
 * @author Christian Everett
 *
 */
public abstract class Device
{
	//Map GPIO Headers to BCM/WiringPI GPIO pins
	protected final static HashMap<Integer, GPIO_PIN> pins = new HashMap<>();
	protected static Runtime rt = Runtime.getRuntime();
	protected static GpioController gpioController = GpioFactory.getInstance();
	
	protected final String name;
	protected boolean isClosed = false;
	protected int headerPin = -1;
	
	public Device(String name) throws IOException
	{
		this.name = name;
	}
	
	/**
	 * 
	 * @param action
	 * @throws IOException
	 */
	public abstract void performAction(Action action);
	
	/**
	 * @return action representing the current state of the device.
	 * If device is closed returns null
	 */
	public abstract Action getState();
	
	/**
	 * Shutdown device and release resources.
	 * All future calls to performAction will do nothing
	 */
	public abstract void close();
	
	public static Device CreateNewDevice(String name, String type, Element element)
	{
		String header;
	
		try
		{
			switch (type)
			{
			case DeviceType.LED:
				String red = element.getElementsByTagName("red").item(0).getTextContent();
				String green = element.getElementsByTagName("green").item(0).getTextContent();
				String blue = element.getElementsByTagName("blue").item(0).getTextContent();

				return new Led(name, Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
				
			case DeviceType.SWITCH:
				header = element.getElementsByTagName("header").item(0).getTextContent();

				return new Switch(name, Integer.parseInt(header));
				
			case DeviceType.OUTLET:
				header = element.getElementsByTagName("header").item(0).getTextContent();
				String onCode = element.getElementsByTagName("onCode").item(0).getTextContent();
				String offCode = element.getElementsByTagName("offCode").item(0).getTextContent();
				
				return new Outlet(name, Integer.parseInt(header), Integer.parseInt(onCode), Integer.parseInt(offCode));
				
			case DeviceType.THERMOSTAT:
				String url = element.getElementsByTagName("url").item(0).getTextContent();
				String sensorDevice = element.getElementsByTagName("sensorDevice").item(0).getTextContent();
				String maxTemp = element.getElementsByTagName("maxTempF").item(0).getTextContent();
				String minTemp = element.getElementsByTagName("minTempF").item(0).getTextContent();
				String turnOffDelay = element.getElementsByTagName("turnOffDelay").item(0).getTextContent();
				
				return new ThermostatController(name, url, sensorDevice, Integer.parseInt(maxTemp), Integer.parseInt(minTemp),
						Integer.parseInt(turnOffDelay));
				
			case DeviceType.TEMP_SENSOR:
				header = element.getElementsByTagName("header").item(0).getTextContent();
				String location = element.getElementsByTagName("location").item(0).getTextContent();
				
				return new TempatureSensor(name, Integer.parseInt(header), location);
	
			default:
				Application.LOGGER.severe("Unknown device: " + name + " Off Type: " + type);
			}
		}
		catch (Exception e)
		{
			Application.LOGGER.severe("Error Loading Device: " + name + ". Exception: " + e.getMessage());
		}
		
		return null;
	}
	
	protected static class GPIO_PIN
	{
		private int BCM_Pin;
		private Pin WiringPI_Pin;
		
		public GPIO_PIN(int BCM, Pin WiringPI)
		{
			this.BCM_Pin = BCM;
			this.WiringPI_Pin = WiringPI;
		}
		
		/**
		 * @return the bCM_Pin
		 */
		public int getBCM_Pin()
		{
			return BCM_Pin;
		}
		/**
		 * @return the wiringPI_Pin
		 */
		public Pin getWiringPI_Pin()
		{
			return WiringPI_Pin;
		}
	}
	
	static
	{
		pins.put(1, null);
		pins.put(2, null);
		pins.put(3, new GPIO_PIN(2, RaspiPin.GPIO_08));
		pins.put(4, null);
		pins.put(5, new GPIO_PIN(3, RaspiPin.GPIO_09));
		pins.put(6, null);
		pins.put(7, new GPIO_PIN(4, RaspiPin.GPIO_07));
		pins.put(8, new GPIO_PIN(14, RaspiPin.GPIO_15));
		pins.put(9, null);
		pins.put(10, new GPIO_PIN(15, RaspiPin.GPIO_16));
		pins.put(11, new GPIO_PIN(17, RaspiPin.GPIO_00));
		pins.put(12, new GPIO_PIN(18, RaspiPin.GPIO_01));
		pins.put(13, new GPIO_PIN(27, RaspiPin.GPIO_02));
		pins.put(14, null);
		pins.put(15, new GPIO_PIN(22, RaspiPin.GPIO_03));
		pins.put(16, new GPIO_PIN(23, RaspiPin.GPIO_04));
		pins.put(17, null);
		pins.put(18, new GPIO_PIN(24, RaspiPin.GPIO_05));
		pins.put(19, new GPIO_PIN(10, RaspiPin.GPIO_12));
		pins.put(20, null);
		pins.put(21, new GPIO_PIN(9, RaspiPin.GPIO_13));
		pins.put(22, new GPIO_PIN(25, RaspiPin.GPIO_06));
		pins.put(23, new GPIO_PIN(11, RaspiPin.GPIO_14));
		pins.put(24, new GPIO_PIN(8, RaspiPin.GPIO_10));
		pins.put(25, null);
		pins.put(26, new GPIO_PIN(7, RaspiPin.GPIO_11));
		pins.put(27, null);
		pins.put(28, null);
		pins.put(29, new GPIO_PIN(5, RaspiPin.GPIO_21));
		pins.put(30, null);
		pins.put(31, new GPIO_PIN(6, RaspiPin.GPIO_22));
		pins.put(32, new GPIO_PIN(12, RaspiPin.GPIO_26));
		pins.put(33, new GPIO_PIN(13, RaspiPin.GPIO_23));
		pins.put(34, null);
		pins.put(35, new GPIO_PIN(19, RaspiPin.GPIO_24));
		pins.put(36, new GPIO_PIN(16, RaspiPin.GPIO_27));
		pins.put(37, new GPIO_PIN(26, RaspiPin.GPIO_25));
		pins.put(38, new GPIO_PIN(20, RaspiPin.GPIO_28));
		pins.put(39, null);
		pins.put(40, new GPIO_PIN(21, RaspiPin.GPIO_29));
	}
}
