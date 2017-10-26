package com.pi.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.google.common.base.Objects;

@Entity
public class MacAddress extends Model
{
	private static final String MAC_ADDRESS_REGEX = "([0-9A-Fa-f]{2}[:]){5}([0-9A-Fa-f]{2})$";
	private static Pattern regex = Pattern.compile(MAC_ADDRESS_REGEX);
	
	@Id
	private String address;
	private boolean isBluetoothAddress = false;
	
	public MacAddress()
	{
		
	}
	
	public MacAddress(String address)
	{
		this(address, false);
	}
	
	public MacAddress(String address, boolean isBluetoothAddress)
	{
		Matcher match = regex.matcher(address);
		
		if (!match.matches())
			throw new RuntimeException("Invalid MAC Address: " + address);
		
		this.address = address;
		this.isBluetoothAddress = isBluetoothAddress;
	}
	
	public String getAddressString()
	{
		return address;
	}

	public boolean isBluetoothAddress()
	{
		return isBluetoothAddress;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(address);
	}

	@Override
	public boolean equals(Object obj)
	{
		return hashCode() == obj.hashCode();
	}
}
