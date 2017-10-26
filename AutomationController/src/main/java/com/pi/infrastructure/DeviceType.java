/**
 * 
 */
package com.pi.infrastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.pi.devices.Led;
import com.pi.devices.Led.LedConfig;
import com.pi.devices.Outlet;
import com.pi.devices.Outlet.OutletConfig;
import com.pi.devices.PILed;
import com.pi.devices.PILed.PILedConfig;
import com.pi.devices.Switch;
import com.pi.devices.Switch.SwitchConfig;
import com.pi.devices.Thermostat;
import com.pi.devices.Thermostat.ThermostatConfig;
import com.pi.devices.asynchronousdevices.Timer;
import com.pi.devices.asynchronousdevices.AmazonEcho;
import com.pi.devices.asynchronousdevices.AmazonEcho.AmazonEchoSwitchConfig;
import com.pi.devices.asynchronousdevices.BluetoothAdapter;
import com.pi.devices.asynchronousdevices.BluetoothAdapter.BluetoothAdapterConfig;
import com.pi.devices.asynchronousdevices.DeviceDetector;
import com.pi.devices.asynchronousdevices.DeviceDetector.DeviceDetectorConfig;
import com.pi.devices.asynchronousdevices.MotionSensor;
import com.pi.devices.asynchronousdevices.MotionSensor.MotionSensorConfig;
import com.pi.devices.asynchronousdevices.TemperatureSensor;
import com.pi.devices.asynchronousdevices.TemperatureSensor.TemperatureSensorConfig;
import com.pi.devices.asynchronousdevices.Timer.TimerConfig;
import com.pi.devices.asynchronousdevices.WeatherSensor;
import com.pi.devices.asynchronousdevices.WeatherSensor.WeatherSensorConfig;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.RemoteDeviceProxy.RemoteDeviceConfig;
import com.pi.model.DeviceState;

/**
 * @author Christian Everett Class for devices types
 */
public abstract class DeviceType
{
	private DeviceType(){}
	
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
	public static final String ECHO = "echo";

	public static final String REMOTE_DEVICE = "remote_device";
	public static final String PROCESSOR = "processor";

	public static final String UNKNOWN = "unknown";

	public static interface Params
	{
		public final static String LOCK = "lock";
		public final static String RED = "red";
		public final static String GREEN = "green";
		public final static String BLUE = "blue";
		public final static String SEQUENCE_RECORD = "sequence_record";
		public final static String NAME = "name";
		public final static String SEQUENCES = "sequences";
		public final static String LOOP = "loop";
		public final static String INTERVAL = "interval";

		public final static String ON = "on";

		public final static String TEMPATURE = "temperature";
		public final static String HUMIDITY = "humidity";
		public final static String IS_DARK = "is_dark";

		public final static String TARGET_TEMPATURE = "target_temp";
		public final static String MODE = "mode";
		public final static String TARGET_MODE = "target_mode";

		public final static String MACS = "macs";
		public final static String MAC = "mac";
		public final static String SCAN = "scan";
		
		public final static String TIME = "time";
		
		public final static String ACTION_PROFILE_NAMES = "action_profile_names";
	}

	public static class DeviceTypeMap
	{
		private static final Map<String, Class<?>> paramTypes = new HashMap<>();
		private static final Map<Class<?>, String> typeToId = new HashMap<>();
		private static Map<String, Class<?>> idToConfig = new HashMap<>();
		
		static
		{
			paramTypes.put(Params.RED, Integer.class);
			paramTypes.put(Params.GREEN, Integer.class);
			paramTypes.put(Params.BLUE, Integer.class);
			paramTypes.put(Params.SEQUENCE_RECORD, Boolean.class);
			paramTypes.put(Params.NAME, String.class);
			paramTypes.put(Params.SEQUENCES, List.class);
			paramTypes.put(Params.LOOP, Boolean.class);
			paramTypes.put(Params.INTERVAL, Integer.class);	
			paramTypes.put(Params.ON, Boolean.class);	
			paramTypes.put(Params.TEMPATURE, Integer.class);
			paramTypes.put(Params.HUMIDITY, Integer.class);
			paramTypes.put(Params.IS_DARK, Boolean.class);	
			paramTypes.put(Params.TARGET_TEMPATURE, Integer.class);
			paramTypes.put(Params.MODE, String.class);
			paramTypes.put(Params.TARGET_MODE, String.class);		
			paramTypes.put(Params.MACS, List.class);
			paramTypes.put(Params.SCAN, Boolean.class);
			paramTypes.put(Params.TIME, String.class);
			
			idToConfig.put(DeviceType.REMOTE_DEVICE, RemoteDeviceConfig.class);
			idToConfig.put(DeviceType.PI_LED, PILedConfig.class);
			idToConfig.put(DeviceType.LED, LedConfig.class);
			idToConfig.put(DeviceType.MOTION_SENSOR, MotionSensorConfig.class);
			idToConfig.put(DeviceType.OUTLET, OutletConfig.class);
			idToConfig.put(DeviceType.SWITCH, SwitchConfig.class);
			idToConfig.put(DeviceType.TEMP_SENSOR, TemperatureSensorConfig.class);
			idToConfig.put(DeviceType.THERMOSTAT, ThermostatConfig.class);
			idToConfig.put(DeviceType.WEATHER_SENSOR, WeatherSensorConfig.class);
			idToConfig.put(DeviceType.DEVICE_SENSOR, DeviceDetectorConfig.class);
			idToConfig.put(DeviceType.BLUETOOTH_ADAPTER, BluetoothAdapterConfig.class);
			idToConfig.put(DeviceType.TIMER, TimerConfig.class);
			idToConfig.put(DeviceType.ECHO, AmazonEchoSwitchConfig.class);
			
			typeToId.put(RemoteDeviceProxy.class, DeviceType.REMOTE_DEVICE);
			typeToId.put(PILed.class, DeviceType.PI_LED);
			typeToId.put(Led.class, DeviceType.LED);
			typeToId.put(MotionSensor.class, DeviceType.MOTION_SENSOR);
			typeToId.put(Outlet.class, DeviceType.OUTLET);
			typeToId.put(Switch.class, DeviceType.SWITCH);
			typeToId.put(TemperatureSensor.class, DeviceType.TEMP_SENSOR);
			typeToId.put(Thermostat.class, DeviceType.THERMOSTAT);
			typeToId.put(WeatherSensor.class, DeviceType.WEATHER_SENSOR);
			typeToId.put(DeviceDetector.class, DeviceType.DEVICE_SENSOR);
			typeToId.put(BluetoothAdapter.class, DeviceType.BLUETOOTH_ADAPTER);
			typeToId.put(Timer.class, DeviceType.TIMER);
			typeToId.put(AmazonEcho.class, DeviceType.ECHO);
		}
		
		public static Class<?> getConfig(String type)
		{
			return idToConfig.get(type);
		}
		
		public static String getType(Class<?> classType)
		{
			return typeToId.get(classType);
		}
		
		public static Class<?> getParamType(String param)
		{
			return paramTypes.get(param);
		}
	};
}
