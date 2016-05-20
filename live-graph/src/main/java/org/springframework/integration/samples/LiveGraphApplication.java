package org.springframework.integration.samples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.http.config.EnableIntegrationGraphController;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.management.graph.IntegrationGraphServer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.support.PeriodicTrigger;

@SpringBootApplication
@ImportResource("integration-graph-context.xml")
@EnableIntegrationGraphController(path = "/integration")
public class LiveGraphApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(LiveGraphApplication.class, args);
		System.out.println("hit enter to exit");
		System.in.read();
	}

	@Bean
	public IntegrationGraphServer server() {
		IntegrationGraphServer server = new IntegrationGraphServer();
		server.setApplicationName("myAppName:1.0");
		return server;
	}

	@Bean
	public MessageProducer producer() {
		MessageProducerSupport producer = new MessageProducerSupport() {

			@Override
			public String getComponentType() {
				return "test-producer";
			}

		};
		producer.setOutputChannelName("one");
		producer.setErrorChannelName("myErrors");
		return producer;
	}

	@Bean
	public Services services() {
		return new Services();
	}

	@Bean
	public EventDrivenConsumer foreignMessageHandlerNoStats() {
		return new EventDrivenConsumer(three(), new BareHandler());
	}

	@Bean
	public PollingConsumer polling() {
		PollingConsumer pollingConsumer = new PollingConsumer(four(), new BareHandler());
		pollingConsumer.setAutoStartup(false);
		return pollingConsumer;
	}

	@Bean
	public PollableChannel polledChannel() {
		return new QueueChannel();
	}

	@Bean
	public SubscribableChannel three() {
		return new DirectChannel();
	}

	@Bean
	public PollableChannel four() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel myErrors() {
		return new QueueChannel();
	}

	@Bean(name = PollerMetadata.DEFAULT_POLLER)
	public PollerMetadata defaultPoller() {
		PollerMetadata poller = new PollerMetadata();
		poller.setTrigger(new PeriodicTrigger(60000));
		MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
		errorHandler.setDefaultErrorChannel(myErrors());
		poller.setErrorHandler(errorHandler);
		return poller;
	}

	public static class Services {

		@ServiceActivator(inputChannel = "one", outputChannel = "polledChannel")
		public String foo(String foo) {
			return foo.toUpperCase();
		}

		@ServiceActivator(inputChannel = "polledChannel")
		public void bar(String foo) {
		}

	}

	public static class BareHandler implements MessageHandler {

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			// empty
		}

	}

}
