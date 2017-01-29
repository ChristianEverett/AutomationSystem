/**
 * 
 */
package com.pi.infrastructure.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.internet.AddressException;
import javax.xml.bind.DatatypeConverter;

import com.pi.Application;

/**
 * @author Christian Everett
 *
 */
public class CryptoService
{
	private static IvParameterSpec INIT_VECTOR = null;
	private static MessageDigest digest = null;
	private static Cipher cipher = null;

	static
	{
		try
		{
			INIT_VECTOR = new IvParameterSpec("5687415896543584".getBytes("UTF-8"));
			digest = MessageDigest.getInstance("SHA-256");
			cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		}
		catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}

	public static String encrypt_AES(String key, String value) throws NoSuchAlgorithmException, NoSuchPaddingException, 
	UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
	{
		SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, INIT_VECTOR);

		byte[] encrypted = cipher.doFinal(value.getBytes());

		return DatatypeConverter.printBase64Binary(encrypted);
	}

	public static String decrypt_AES(String key, String encrypted) throws UnsupportedEncodingException, InvalidKeyException, 
	InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
	{
		SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

		cipher.init(Cipher.DECRYPT_MODE, skeySpec, INIT_VECTOR);

		byte[] original = cipher.doFinal(DatatypeConverter.parseBase64Binary(encrypted));

		return new String(original);
	}

	public static byte[] SHA_HashString(String input)
	{
		return digest.digest(input.getBytes(StandardCharsets.UTF_8));
	}
}
