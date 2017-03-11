/**
 * 
 */
package com.pi.infrastructure;

import java.util.HashMap;

import com.pi.devices.Led.LedConfig;
import com.pi.devices.Outlet.OutletConfig;
import com.pi.devices.PILed.PILedConfig;
import com.pi.devices.Switch.SwitchConfig;
import com.pi.devices.Thermostat.ThermostatConfig;
import com.pi.devices.Thermostat.ThermostatMode;
import com.pi.devices.WeatherSensor.WeatherSensorConfig;
import com.pi.devices.asynchronousdevices.BluetoothAdapter.BluetoothAdapterConfig;
import com.pi.devices.asynchronousdevices.DeviceDetector.DeviceDetectorConfig;
import com.pi.devices.asynchronousdevices.MotionSensor.MotionSensorConfig;
import com.pi.devices.asynchronousdevices.TemperatureSensor.TemperatureSensorConfig;
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

	public static final String REMOTE_DEVICE = "remote_device";
	public static final String PROCESSOR = "processor";

	public static final String UNKNOWN = "unknown";

	public static void registerAllDeviceConfigs()
	{
		Device.registerDevice(DeviceType.REMOTE_DEVICE, RemoteDeviceConfig.class);

		Device.registerDevice(DeviceType.PI_LED, PILedConfig.class);
		Device.registerDevice(DeviceType.LED, LedConfig.class);
		Device.registerDevice(DeviceType.MOTION_SENSOR, MotionSensorConfig.class);
		Device.registerDevice(DeviceType.OUTLET, OutletConfig.class);
		Device.registerDevice(DeviceType.SWITCH, SwitchConfig.class);
		Device.registerDevice(DeviceType.TEMP_SENSOR, TemperatureSensorConfig.class);
		Device.registerDevice(DeviceType.THERMOSTAT, ThermostatConfig.class);
		Device.registerDevice(DeviceType.WEATHER_SENSOR, WeatherSensorConfig.class);
		Device.registerDevice(DeviceType.DEVICE_SENSOR, DeviceDetectorConfig.class);
		Device.registerDevice(DeviceType.BLUETOOTH_ADAPTER, BluetoothAdapterConfig.class);
	}

	public static interface Params
	{
		public final static String RED = "red";
		public final static String GREEN = "green";
		public final static String BLUE = "blue";

		public final static String IS_ON = "isOn";

		public final static String TEMPATURE = "temperature";
		public final static String HUMIDITY = "humidity";

		public final static String TARGET_TEMPATURE = "target_temp";
		public final static String MODE = "mode";
		public final static String TARGET_MODE = "target_mode";

		public final static String MAC = "mac";
		public final static String SCAN = "scan";
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
			
			put(Params.TARGET_TEMPATURE, Integer.class);
			put(Params.MODE, String.class);
			put(Params.TARGET_MODE, String.class);
			
			put(Params.MAC, String.class);
			put(Params.SCAN, Boolean.class);
		}
	};
}
