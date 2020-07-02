/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.samples.dynamictcp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableMessageHistory;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.TcpConnectionCloseEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

@SpringBootApplication
@EnableMessageHistory
public class DynamicTcpClientApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(DynamicTcpClientApplication.class, args);
		context.close();
	}

	// Client side

	@MessagingGateway(defaultRequestChannel = "toTcp.input")
	public interface ToTCP {

		public void send(String data, @Header("host") String host, @Header("port") int port);

	}

	@Bean
	public IntegrationFlow toTcp() {
		return f -> f.route(new TcpRouter());
	}

	@ServiceActivator(inputChannel = "fromTcp")
	public void receiver(String in, @Header(IpHeaders.CONNECTION_ID) String connectionId) {
		System.out.println("Received " + in + " from " + connectionId);
	}

	// Two servers

	@Bean
	public TcpNetServerConnectionFactory cfOne() {
		return new TcpNetServerConnectionFactory(1234);
	}

	@Bean
	public IntegrationFlow inOne(TcpNetServerConnectionFactory cfOne) {
		return IntegrationFlows.from(Tcp.inboundAdapter(cfOne))
			.transform(Transformers.objectToString())
			.handle(System.out::println)
			.get();
	}

	@Bean
	public IntegrationFlow outOne(TcpNetServerConnectionFactory cfOne) {
		return f -> f.handle(Tcp.outboundAdapter(cfOne));
	}

	@Bean
	public TcpNetServerConnectionFactory cfTwo() {
		return new TcpNetServerConnectionFactory(5678);
	}

	@Bean
	public IntegrationFlow inTwo(TcpNetServerConnectionFactory cfTwo) {
		return IntegrationFlows.from(Tcp.inboundAdapter(cfTwo))
				.transform(Transformers.objectToString())
				.handle(System.out::println)
				.get();
	}

	@Bean
	public IntegrationFlow outTwo(TcpNetServerConnectionFactory cfTwo) {
		return f -> f.handle(Tcp.outboundAdapter(cfTwo));
	}

	private final Set<String> connections = ConcurrentHashMap.newKeySet();

	@EventListener
	public void openEvents(TcpConnectionOpenEvent event) {
		if (event.getConnectionFactoryName().startsWith("cf")) {
			System.out.println("Connection opened " + event.getConnectionId());
			this.connections.add(event.getConnectionId());
		}
	}

	@EventListener
	public void closeEvents(TcpConnectionCloseEvent event) {
		if (event.getConnectionFactoryName().startsWith("cf")) {
			this.connections.remove(event.getConnectionId());
		}
	}

	@Bean
	@DependsOn({ "outOne", "outTwo", "inOne", "inTwo" })
	public ApplicationRunner runner(ToTCP toTcp,
			@Qualifier("outOne.input") MessageChannel outOne,
			@Qualifier("outTwo.input") MessageChannel outTwo) {

		return args -> {
			toTcp.send("foo", "localhost", 1234);
			toTcp.send("foo", "localhost", 5678);
			for (int i = 0; i < 10; i++) {
				Thread.sleep(3000);
				this.connections.forEach(conn -> {
					try {
						if (conn.contains("1234")) {
							outOne.send(MessageBuilder.withPayload("some data")
									.setHeader(IpHeaders.CONNECTION_ID, conn)
									.build());
						}
						else {
							outTwo.send(MessageBuilder.withPayload("some other data")
									.setHeader(IpHeaders.CONNECTION_ID, conn)
									.build());
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		};
	}

	public static class TcpRouter extends AbstractMessageRouter {

		private final static int MAX_CACHED = 10; // When this is exceeded, we remove the LRU.

		@SuppressWarnings("serial")
		private final LinkedHashMap<String, MessageChannel> subFlows =
				new LinkedHashMap<String, MessageChannel>(MAX_CACHED, .75f, true) {

					@Override
					protected boolean removeEldestEntry(Entry<String, MessageChannel> eldest) {
						if (size() > MAX_CACHED) {
							removeSubFlow(eldest);
							return true;
						}
						else {
							return false;
						}
					}

				};

		@Autowired
		private IntegrationFlowContext flowContext;

		@Override
		protected synchronized Collection<MessageChannel> determineTargetChannels(Message<?> message) {
			MessageChannel channel = this.subFlows
					.get(message.getHeaders().get("host", String.class) + message.getHeaders().get("port") + ".out");
			if (channel == null) {
				channel = createNewSubflow(message);
			}
			return Collections.singletonList(channel);
		}

		private MessageChannel createNewSubflow(Message<?> message) {
			String host = (String) message.getHeaders().get("host");
			Integer port = (Integer) message.getHeaders().get("port");
			Assert.state(host != null && port != null, "host and/or port header missing");
			String hostPort = host + port;

			TcpNetClientConnectionFactory cf = new TcpNetClientConnectionFactory(host, port);
			TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
			handler.setConnectionFactory(cf);

			IntegrationFlow out = f -> f.handle(handler);
			IntegrationFlowContext.IntegrationFlowRegistration flowRegistration =
					this.flowContext.registration(out)
							.addBean(cf)
							.id(hostPort + ".out")
							.register();
			MessageChannel inputChannel = flowRegistration.getInputChannel();
			this.subFlows.put(hostPort + ".out", inputChannel);

			TcpReceivingChannelAdapter receiver = new TcpReceivingChannelAdapter();
			receiver.setConnectionFactory(cf);
			IntegrationFlow in = IntegrationFlows.from(receiver)
					.transform(Transformers.objectToString())
					.channel("fromTcp")
					.get();
			flowRegistration =
					this.flowContext.registration(in)
							.id(hostPort + ".in")
							.register();

			return inputChannel;
		}

		private void removeSubFlow(Entry<String, MessageChannel> eldest) {
			String hostPort = eldest.getKey();
			this.flowContext.remove(hostPort + ".out");
			this.flowContext.remove(hostPort + ".in");
		}

	}

}
