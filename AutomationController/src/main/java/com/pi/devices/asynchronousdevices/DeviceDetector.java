/**
 * 
 */
package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.model.DeviceState;
import com.pi.model.MacAddress;
import com.pi.model.repository.RepositoryType;

/**
 * @author Christian Everett
 *
 */
public class DeviceDetector extends AsynchronousDevice
{
	private HashSet<MacAddress> registeredMACs = new HashSet<>();
	private String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
	private String lastMAC = "";
	
	public DeviceDetector(String name) throws IOException
	{
		super(name);
		setupScanner();
		
		Collection<MacAddress> addresses = getRepositoryValues(RepositoryType.MACAddress);
		addresses = addresses.stream().filter(address -> !address.isBluetoothAddress()).collect(Collectors.toList());
		registeredMACs.addAll(addresses);
		
		createTask(10000L, 10L, TimeUnit.MILLISECONDS);
	}

	private void registerMACAddress(String address)
	{
		MacAddress macAddress = new MacAddress(address);
		registerAddress(address);
		registeredMACs.add(macAddress);
		setRepositoryValue(RepositoryType.MACAddress, macAddress.getAddressString(), macAddress);
	}

	@Override
	protected void update() throws Exception
	{
		String MAC = scan();
		lastMAC = MAC;
	}

	@Override
	protected void performAction(DeviceState state)
	{
		@SuppressWarnings("unchecked")
		List<String> addresses = (List<String>) state.getParamNonNull(Params.MACS);
		
		for (String element : addresses)
		{
			registerMACAddress(element);
		}
	}

	@Override
	public DeviceState getState(DeviceState state)
	{
		state.setParam(Params.MAC, lastMAC);
	
		return state;
	}

	@Override
	protected void tearDown()
	{
		stopScanning();
	}

	private native void setupScanner();
	private native void registerAddress(String address);
	private native String scan();
	private native void stopScanning();
	
	@XmlRootElement(name = DEVICE)
	public static class DeviceDetectorConfig extends DeviceConfig
	{
		private List<String> addresses;

		@Override
		public Device buildDevice() throws IOException
		{
			return new DeviceDetector(name);
		}

		@XmlElement
		public void setAddress(List<String> addresses)
		{
			this.addresses = addresses;
		}
		
		public List<String> getAddress()
		{
			return addresses;
		}
	}
}
