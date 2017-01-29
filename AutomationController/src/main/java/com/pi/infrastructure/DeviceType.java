/**
 * 
 */
package com.pi.infrastructure;

import com.pi.devices.DeviceDetector.DeviceDetectorConfig;
import com.pi.devices.Led.LedConfig;
import com.pi.devices.MotionSensor.MotionSensorConfig;
import com.pi.devices.Outlet.OutletConfig;
import com.pi.devices.Switch.SwitchConfig;
import com.pi.devices.TemperatureSensor.TemperatureSensorConfig;
import com.pi.devices.Thermostat.ThermostatConfig;
import com.pi.devices.WeatherSensor.WeatherSensorConfig;
import com.pi.infrastructure.RemoteDevice.RemoteDeviceConfig;

/**
 * @author Christian Everett Class for devices types
 */
public abstract class DeviceType
{
	
	public static final String LED = "led";
	public static final String OUTLET = "outlet";
	public static final String SWITCH = "switch";
	public static final String THERMOSTAT = "thermostat";
	public static final String TEMP_SENSOR = "temp_sensor";
	public static final String MOTION_SENSOR = "motion_sensor";
	public static final String WEATHER_SENSOR = "weather_sensor";
	public static final String DEVICE_SENSOR = "device_sensor";
	
	public static final String REMOTE_DEVICE = "remote_device";
	public static final String PROCESSOR = "processor";
	
	public static void registerAllDeviceConfigs()
	{
		Device.registerDevice(DeviceType.REMOTE_DEVICE, RemoteDeviceConfig.class);
		
		Device.registerDevice(DeviceType.LED, LedConfig.class);
		Device.registerDevice(DeviceType.MOTION_SENSOR, MotionSensorConfig.class);
		Device.registerDevice(DeviceType.OUTLET, OutletConfig.class);
		Device.registerDevice(DeviceType.SWITCH, SwitchConfig.class);
		Device.registerDevice(DeviceType.TEMP_SENSOR, TemperatureSensorConfig.class);
		Device.registerDevice(DeviceType.THERMOSTAT, ThermostatConfig.class);
		Device.registerDevice(DeviceType.WEATHER_SENSOR, WeatherSensorConfig.class);
		Device.registerDevice(DeviceType.DEVICE_SENSOR, DeviceDetectorConfig.class);
	}
}
