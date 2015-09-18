/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.samples.barrier;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Gary Russell
 * @since 4.2
 */
@SpringBootApplication
@ImportResource("/META-INF/spring/integration/server-context.xml")
public class Application {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext server = SpringApplication.run(Application.class, args);

		// https://github.com/spring-projects/spring-boot/issues/3945
		CachingConnectionFactory connectionFactory = server.getBean(CachingConnectionFactory.class);
		connectionFactory.setPublisherConfirms(true);
		connectionFactory.resetConnection();
		// https://github.com/spring-projects/spring-boot/issues/3945

		ConfigurableApplicationContext client
			= new SpringApplicationBuilder("/META-INF/spring/integration/client-context.xml")
				.web(false)
				.run(args);
		RequestGateway requestGateway = client.getBean("requestGateway", RequestGateway.class);
		String request = "A,B,C";
		System.out.println("\n\n++++++++++++ Sending: " + request + " ++++++++++++\n");
		String reply = requestGateway.echo(request);
		System.out.println("\n\n++++++++++++ Replied with: " + reply + " ++++++++++++\n");
		System.out.println("\nHit Enter to exit\n"
				+ "Point your browser to http://localhost:8080/getGateway "
				+ "to send more requests\n"
				+ "Channel stats, etc, at http://localhost:8080/integration");
		System.in.read();
		client.close();
		server.close();
		System.exit(0); // AMQP-519
	}

}

@RestController
class IntegrationController implements SmartInitializingSingleton {

	@Autowired
	private AbstractApplicationContext context;

	private final Map<String, Object> integrationEntities = new LinkedHashMap<String, Object>();

	@RequestMapping("/integration")
	public Map<?, ?> integration() {
		return this.integrationEntities;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		Map<String, AbstractMessageChannel> channels = this.context.getBeansOfType(AbstractMessageChannel.class);
		Map<String, SourcePollingChannelAdapter> spcas = this.context.getBeansOfType(SourcePollingChannelAdapter.class);
		Map<String, MessagingGatewaySupport> gateways = this.context.getBeansOfType(MessagingGatewaySupport.class);
		Map<String, PollingConsumer> polling = this.context.getBeansOfType(PollingConsumer.class);
		Map<String, EventDrivenConsumer> eventDriven = this.context.getBeansOfType(EventDrivenConsumer.class);
		for (AbstractMessageChannel channel : channels.values()) {
			this.integrationEntities.put(channel.getComponentName(), new MessageChannelInfo(channel));
		}
		for (SourcePollingChannelAdapter adapter : spcas.values()) {
			map.put(adapter.getComponentName(), sourceInfo(adapter));
		}
		for (MessagingGatewaySupport gateway : gateways.values()) {
			map.put(gateway.getComponentName(), gatewayInfo(gateway));
		}
		for (PollingConsumer cons : polling.values()) {
			map.put(cons.getComponentName(), consumerInfo(cons));
		}
		for (EventDrivenConsumer cons : eventDriven.values()) {
			map.put(cons.getComponentName(), consumerInfo(cons));
		}
		this.integrationEntities.putAll(map);
	}

	private MessageGatewayInfo gatewayInfo(MessagingGatewaySupport gateway) {
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		NamedComponent named = (NamedComponent) accessor.getPropertyValue("requestChannel");
		String output = null;
		if (named != null) {
			output = named.getComponentName();
		}
		return new MessageGatewayInfo(gateway, output);
	}

	private MessageSourceInfo sourceInfo(SourcePollingChannelAdapter adapter) {
		DirectFieldAccessor accessor = new DirectFieldAccessor(adapter);
		NamedComponent named = (NamedComponent) accessor.getPropertyValue("outputChannel");
		String output = null;
		if (named != null) {
			output = named.getComponentName();
		}
		AbstractMessageSource<?> source = (AbstractMessageSource<?>) accessor.getPropertyValue("source");
		return new MessageSourceInfo(source, output);
	}

	private MessageHandlerInfo consumerInfo(Object cons) {
		DirectFieldAccessor accessor = new DirectFieldAccessor(cons);
		NamedComponent named = (NamedComponent) accessor.getPropertyValue("inputChannel");
		String input = null;
		if (named != null) {
			input = named.getComponentName();
		}
		AbstractMessageHandler handler = (AbstractMessageHandler) accessor.getPropertyValue("handler");
		String output = null;
		if (handler instanceof AbstractMessageProducingHandler) {
			accessor = new DirectFieldAccessor(handler);
			named = (NamedComponent) accessor.getPropertyValue("outputChannel");
			if (named != null) {
				output = named.getComponentName();
			}
		}
		MessageHandlerInfo mhi = new MessageHandlerInfo(handler, input, output);
		return mhi;
	}

}

