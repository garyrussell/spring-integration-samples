package org.springframework.integration.samples;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.http.Http;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableIntegrationManagement
public class HttpBootApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(HttpBootApplication.class, args);
		RestTemplate template = new RestTemplate();
		String result = template.postForObject("http://localhost:8080/receiveGateway", "foo,bar,baz", String.class);
		System.out.println(result);
		System.out.println("Visit http://localhost:8080/integration to view the JSON object model");
		System.out.println("Hit enter to terminate");
		System.in.read();
		context.close();
	}

	@Bean
	public IntegrationFlow httpFlow() {
		return IntegrationFlows.from(Http.inboundGateway("/receiveGateway")
					.requestMapping(m -> m.methods(HttpMethod.POST))
					.requestPayloadType(String.class))
			.split(commaSplitter())
			.<String, String>transform(p -> p + " from the other side")
			.<String, String>transform(String::toUpperCase)
			.aggregate()
			.<Collection<String>, String>transform(Collection::toString)
			.get();
	}

	@Bean
	public DefaultMessageSplitter commaSplitter() {
		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setDelimiters(",");
		return splitter;
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
		return new MessageGatewayInfo(gateway, output, (AbstractMessageChannel) named); // TODO check type
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
