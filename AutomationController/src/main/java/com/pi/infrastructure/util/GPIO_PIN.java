package com.pi.infrastructure.util;

import java.util.HashMap;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

public class GPIO_PIN
{
	// Map GPIO Headers to BCM/WiringPI GPIO pins
	protected static final HashMap<Integer, GPIO_PIN> pins = new HashMap<>();
	
	private int BCM_Pin;
	private Pin WiringPI_Pin;

	private GPIO_PIN(int BCM, Pin WiringPI)
	{
		this.BCM_Pin = BCM;
		this.WiringPI_Pin = WiringPI;
	}

	/**
	 * @return the bCM_Pin
	 */
	public static int getBCM_Pin(Integer header)
	{
		GPIO_PIN pin = pins.get(header);
		
		if(pin == null)
			throw new RuntimeException("Header: " + header + " is not a GPIO");
		
		return pin.BCM_Pin;
	}

	/**
	 * @return the wiringPI_Pin
	 */
	public static Pin getWiringPI_Pin(Integer header)
	{
		GPIO_PIN pin = pins.get(header);
		
		if(pin == null)
			throw new RuntimeException("Header: " + header + " is not a GPIO");
		
		return pin.WiringPI_Pin;
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