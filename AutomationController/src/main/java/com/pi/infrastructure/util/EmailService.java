package com.pi.infrastructure.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService
{
	private static final String AUTOMATION_SYSTEM = "Automation-System";

	@Autowired
	private JavaMailSender mailSender;

	public Email createEmail(String... to)
	{
		return new Email(this, to);
	}
	
	public void send(SimpleMailMessage mail)
	{
		mailSender.send(mail);
	}
	
	public static class Email
	{
		private SimpleMailMessage message = new SimpleMailMessage();
		private EmailService mailService;
		
		public Email(EmailService emailService, String... to)
		{
			this.mailService = emailService;
			message.setTo(to);
			message.setFrom(AUTOMATION_SYSTEM);
		}

		public Email setSubject(String subject)
		{
			message.setSubject(subject);

			return this;
		}

		public Email setMessageBody(String text)
		{
			message.setText(text);

			return this;
		}

		public void send()
		{
			mailService.send(message);
		}
	}
}
