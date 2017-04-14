/**
 * 
 */
package com.pi.infrastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pi.devices.Led.LedConfig;
import com.pi.devices.Outlet.OutletConfig;
import com.pi.devices.PILed.PILedConfig;
import com.pi.devices.Switch.SwitchConfig;
import com.pi.devices.Thermostat.ThermostatConfig;
import com.pi.devices.Thermostat.ThermostatMode;
import com.pi.devices.asynchronousdevices.Timer;
import com.pi.devices.asynchronousdevices.BluetoothAdapter.BluetoothAdapterConfig;
import com.pi.devices.asynchronousdevices.DeviceDetector.DeviceDetectorConfig;
import com.pi.devices.asynchronousdevices.MotionSensor.MotionSensorConfig;
import com.pi.devices.asynchronousdevices.TemperatureSensor.TemperatureSensorConfig;
import com.pi.devices.asynchronousdevices.Timer.TimerConfig;
import com.pi.devices.asynchronousdevices.WeatherSensor.WeatherSensorConfig;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.RemoteDevice.RemoteDeviceConfig;

/**
 * @author Christian Everett Class for devices types
 */
public abstract class DeviceType
{
	public static final String PI_LED = "pi_led";
	public static final String LED = "led";
	public static final String OUTLET = "outlet";
	public static final String SWITCH = "switch";
	public static final String THERMOSTAT = "thermostat";
	public static final String TEMP_SENSOR = "temp_sensor";
	public static final String MOTION_SENSOR = "motion_sensor";
	public static final String WEATHER_SENSOR = "weather_sensor";
	public static final String DEVICE_SENSOR = "device_sensor";
	public static final String BLUETOOTH_ADAPTER = "bluetooth_adapter";
	public static final String TIMER = "timer";

	public static final String REMOTE_DEVICE = "remote_device";
	public static final String PROCESSOR = "processor";

	public static final String UNKNOWN = "unknown";

	public static Map<String, Class<?>> registerAllDeviceConfigs()
	{
		HashMap<String, Class<?>> map = new HashMap<>();
		map.put(DeviceType.REMOTE_DEVICE, RemoteDeviceConfig.class);

		map.put(DeviceType.PI_LED, PILedConfig.class);
		map.put(DeviceType.LED, LedConfig.class);
		map.put(DeviceType.MOTION_SENSOR, MotionSensorConfig.class);
		map.put(DeviceType.OUTLET, OutletConfig.class);
		map.put(DeviceType.SWITCH, SwitchConfig.class);
		map.put(DeviceType.TEMP_SENSOR, TemperatureSensorConfig.class);
		map.put(DeviceType.THERMOSTAT, ThermostatConfig.class);
		map.put(DeviceType.WEATHER_SENSOR, WeatherSensorConfig.class);
		map.put(DeviceType.DEVICE_SENSOR, DeviceDetectorConfig.class);
		map.put(DeviceType.BLUETOOTH_ADAPTER, BluetoothAdapterConfig.class);
		map.put(DeviceType.TIMER, TimerConfig.class);
		
		return map;
	}

	public static interface Params
	{
		public final static String RED = "red";
		public final static String GREEN = "green";
		public final static String BLUE = "blue";

		public final static String IS_ON = "isOn";

		public final static String TEMPATURE = "temperature";
		public final static String HUMIDITY = "humidity";
		public final static String IS_DARK = "is_dark";

		public final static String TARGET_TEMPATURE = "target_temp";
		public final static String MODE = "mode";
		public final static String TARGET_MODE = "target_mode";

		public final static String MACS = "macs";
		public final static String SCAN = "scan";
		
		public final static String TIME = "time";
	}

	public static HashMap<String, Class<?>> paramTypes = new HashMap<String, Class<?>>()
	{
		{
			put(Params.RED, Integer.class);
			put(Params.GREEN, Integer.class);
			put(Params.BLUE, Integer.class);
			
			put(Params.IS_ON, Boolean.class);
			
			put(Params.TEMPATURE, Integer.class);
			put(Params.HUMIDITY, Integer.class);
			put(Params.IS_DARK, Boolean.class);
			
			put(Params.TARGET_TEMPATURE, Integer.class);
			put(Params.MODE, String.class);
			put(Params.TARGET_MODE, String.class);
			
			put(Params.MACS, List.class);
			put(Params.SCAN, Boolean.class);
			
			put(Params.TIME, String.class);
		}
	};
}
