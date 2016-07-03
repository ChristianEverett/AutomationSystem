/**
 * 
 */
package com.pi.backgroundprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.collect.Lists;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

/**
 * @author Christian Everett
 *
 */
class GPIOController
{
	private static GPIOController singleton = null;
	
	public static final String LED_ONE = "led1";
	public static final String LED_TWO = "led2";
	
	public static final boolean RELAY_ON = false;
	public static final boolean RELAY_OFF = true;
	
	private Runtime rt = Runtime.getRuntime();
	
	private HashMap<String, GpioPinDigitalOutput> gpioPins;
	
	private GPIOController() throws IOException
	{
		GpioController gpio = GpioFactory.getInstance();
		
		Process pr = rt.exec("sudo pigpiod");
		
		gpioPins = new HashMap<>();
		
		gpioPins.put("switch1", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_08, "switch1", PinState.HIGH));
		gpioPins.put("switch2", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_09, "switch2", PinState.HIGH));
		gpioPins.put("switch3", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07, "switch3", PinState.HIGH));
		gpioPins.put("switch4", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "switch4", PinState.HIGH));
	}
	
	public void setPin(String switch_number, boolean state) throws IOException
	{
		GpioPinDigitalOutput pin = null;
		
		if((pin = gpioPins.get(switch_number)) == null)
			throw new IOException("Pin not enabled");
		else
		{
			pin.setState(state);
		}
	}
	
	public void pulseWidthModulate(String led, int RED, int GREEN, int BLUE) throws IOException
	{	
		int RED_PIN, GREEN_PIN, BLUE_PIN;
		
		switch (led)
		{
		case LED_ONE:
			RED_PIN = LED_ONE_PINS.RED_PIN;
			GREEN_PIN = LED_ONE_PINS.GREEN_PIN;
			BLUE_PIN = LED_ONE_PINS.BLUE_PIN;
			break;
		case LED_TWO:
			RED_PIN = LED_TWO_PINS.RED_PIN;
			GREEN_PIN = LED_TWO_PINS.GREEN_PIN;
			BLUE_PIN = LED_TWO_PINS.BLUE_PIN;
			break;
		default:
			throw new IOException("Led not supported");
		}
		
		rt.exec("pigs p " + RED_PIN + " " + (255 - RED) + " &");
		rt.exec("pigs p " + GREEN_PIN + " " + (255 - GREEN) + " &");
		rt.exec("pigs p " + BLUE_PIN + " " + (255 - BLUE) + " &");
	}
	
	public void close()
	{
		ArrayList<GpioPinDigitalOutput> allPins = Lists.newArrayList(gpioPins.values());
		
		for(int x = 0; x < allPins.size(); x++)
		{
			allPins.get(x).setState(RELAY_OFF);
		}
		
		try
		{
			pulseWidthModulate(LED_ONE, 0, 0, 0);
			pulseWidthModulate(LED_TWO, 0, 0, 0);
		}
		catch (IOException e)
		{
		}
		
	}
	
	public static GPIOController loadGPIOController() throws IOException
	{
		if(singleton == null)
			singleton = new GPIOController();
		
		return singleton;
	}
	
	private interface LED_ONE_PINS
	{
		public static final int RED_PIN = 13;
		public static final int GREEN_PIN = 19;
		public static final int BLUE_PIN = 26;
	}
	
	private interface LED_TWO_PINS
	{
		public static final int RED_PIN = 20;
		public static final int GREEN_PIN = 16;
		public static final int BLUE_PIN = 21;
	}
}
