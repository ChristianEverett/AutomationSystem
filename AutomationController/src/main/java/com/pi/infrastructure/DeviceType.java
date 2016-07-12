/**
 * 
 */
package com.pi.infrastructure;

/**
 * @author Christian Everett
 * Class for devices types
 */
public interface DeviceType
{
	public static final String RUN_ECHO_SERVER = "run_echo_server";
	public static final String LED = "led";
	public static final String OUTLET = "outlet";
	public static final String SWITCH= "switch";
	public static final String RELOAD_DEVICES = "reload_devices";
	public static final String SHUTDOWN = "shutdown";
}
