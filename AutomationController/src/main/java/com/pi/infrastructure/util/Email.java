package com.pi.infrastructure.util;

import java.io.UnsupportedEncodingException;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

import javax.activation.*;

public class Email
{
	private static final String AUTOMATION_SYSTEM = "Automation-System";

	private static final String USERNAME = "ceverettpi@gmail.com";
	private static final String PASSWORD = "01433128";

	// Assuming you are sending email from localhost
	private static final String HOST = "smtp.gmail.com";
	private static final String PORT = "587";

	private Address[] receivers = null;
	private MimeMessage mimeMessage = null;

	// Get system properties
	private static final Properties properties = System.getProperties();

	private static final Session session;

	static
	{
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true");

		// Setup mail server
		properties.put("mail.smtp.host", HOST);
		properties.put("mail.smtp.port", PORT);

		// Get the default Session object.
		session = Session.getDefaultInstance(properties, new javax.mail.Authenticator()
		{
			protected PasswordAuthentication getPasswordAuthentication()
			{
				return new PasswordAuthentication(USERNAME, PASSWORD);
			}
		});
	}

	public static Email create(String... to)
	{
		return new Email(to);
	}

	private Email(String to[])
	{
		try
		{
			this.receivers = new Address[to.length];

			for (int x = 0; x < to.length; x++)
			{
				this.receivers[x] = new InternetAddress(to[x]);
			}

			// Create a default MimeMessage object.
			mimeMessage = new MimeMessage(session);

			// Set From: header field of the header.
			mimeMessage.setFrom(new InternetAddress(AUTOMATION_SYSTEM, AUTOMATION_SYSTEM));

			// Set To: header field of the header.
			mimeMessage.addRecipients(Message.RecipientType.TO, receivers);
		}
		catch (Exception e)
		{

		}
	}

	public Email setSubject(String subject)
	{
		try
		{
			// Set Subject: header field
			mimeMessage.setSubject(subject);
		}
		catch (MessagingException e)
		{

		}

		return this;
	}

	public Email setMessageBody(String message)
	{
		try
		{
			// Now set the actual message
			mimeMessage.setText(message);
		}
		catch (MessagingException e)
		{

		}

		return this;
	}

	public boolean send()
	{
		try
		{
			// Send message
			Transport.send(mimeMessage);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
