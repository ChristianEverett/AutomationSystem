/**
 * 
 */
package com.pi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.pi.backgroundprocessor.Processor;
import com.pi.infrastructure.Email;
import static com.pi.infrastructure.PropertyManger.loadProperty;
import static com.pi.infrastructure.PropertyManger.PropertyKeys;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * @author Christian Everett
 *
 */

// Tell Spring to automatically inject any dependencies that are marked in
// our classes with @Autowired
@EnableAutoConfiguration
// Tell Spring to automatically create a JPA implementation of our
@EnableJpaRepositories
// Tell Spring to turn on WebMVC (e.g., it should enable the DispatcherServlet
// so that requests can be routed to our Controllers)
@EnableWebMvc
// Tell Spring that this object represents a Configuration for the
// application
@Configuration
// Tell Spring to go and scan our controller package (and all sub packages) to
// find any Controllers or other components that are part of our application.
// Any class in this package that is annotated with @Controller is going to be
// automatically discovered and connected to the DispatcherServlet.
@ComponentScan
// We use the @Import annotation to include our OAuth2SecurityConfiguration
// as part of this configuration so that we can have security and oauth
// setup by Spring
// @Import(OAuth2SecurityConfiguration.class)
public class Application extends WebMvcAutoConfiguration
{
	public static final Logger LOGGER = Logger.getLogger("SystemLogger");

	public static void main(String[] args)
	{
		try
		{
			if (args.length != 0)
				LOGGER.setUseParentHandlers(false);
			else
				LOGGER.setLevel(Level.SEVERE);
			LOGGER.addHandler(new FileHandler(loadProperty(PropertyKeys.LOGFILE)));
		}
		catch (Exception e)
		{
			System.out.println("Can't open log file");
		}

		LOGGER.info("Starting Processing Service");
		try
		{
			// Run the background processor
			Processor.createBackgroundProcessor();
			Processor processor = Processor.getBackgroundProcessor();
			processor.setPriority(Thread.MAX_PRIORITY);

			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				@Override
				public void run()
				{
					Application.LOGGER.severe("Shutdown Hook Running");
					processor.shutdownBackgroundProcessor();
				}
			});

			// Run the Spring Dispatcher
			SpringApplication.run(Application.class, args);

			processor.start();
			LOGGER.info("------------Service Running-------------");

			processor.join();
			Email.create(loadProperty(PropertyKeys.ADMIN_EMAIL)).setSubject("Automation System Shutting down").setMessageBody(
					"Shutting down at: " + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date())).send();
		}
		catch (Exception e)
		{
			LOGGER.severe(e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			System.exit(1);
		}
	}
}
