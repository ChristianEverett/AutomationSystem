/**
 * 
 */
package com.pi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import com.pi.infrastructure.MySQLHandler;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.services.PrimaryNodeControllerImpl;

import static com.pi.infrastructure.util.PropertyManger.PropertyKeys;

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
	public static void main(String[] args)
	{
		try
		{
			long start = System.currentTimeMillis();
			
			String dbuser = PropertyManger.loadPropertyNotNull(PropertyKeys.DBUSER);
			String dbpass = PropertyManger.loadPropertyNotNull(PropertyKeys.DBPASS);
			String dbname = PropertyManger.loadPropertyNotNull(PropertyKeys.DATABASE_NAME);
			
			MySQLHandler dbHandler = new MySQLHandler(dbuser, dbpass);
			dbHandler.loadDatabase(dbname);
			
			SystemLogger.getLogger().info("Starting Application");
			
			// Run the Spring Dispatcher
			ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
			PrimaryNodeControllerImpl nodeController = context.getBean(PrimaryNodeControllerImpl.class);
			
			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				@Override
				public void run()
				{
					SystemLogger.getLogger().severe("Shutdown Hook Running");
					nodeController.shutdown();
				}
			});
			
			nodeController.loadDevices();
				
			long end = System.currentTimeMillis();
			SystemLogger.getLogger().info("-----Service Running Took " + (end - start)/1000 + " Seconds-----");

			synchronized (nodeController)
			{
				nodeController.wait();
			}
					
			//			Email.create(loadPropertyNotNull(PropertyKeys.ADMIN_EMAIL)).setSubject("Automation System Shutting down").setMessageBody(
			//					"Shutting down at: " + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date())).send();
						SystemLogger.getLogger().info("----------Service Stopped----------");
		}
		catch (Throwable e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			System.exit(1);
		}
	}
	
	@Configuration
	@EnableWebSocketMessageBroker
	public class SocketConfigure extends AbstractWebSocketMessageBrokerConfigurer
	{
		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry)
		{
			registry.addEndpoint("/system").withSockJS();
		}
	
		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry)
		{
			registry.enableSimpleBroker("/topic");
			registry.setApplicationDestinationPrefixes("/app");
		}
	}
}
