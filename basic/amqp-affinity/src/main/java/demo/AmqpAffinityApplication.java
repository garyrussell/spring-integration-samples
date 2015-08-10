package demo;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.LocalizedQueueConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

@SpringBootApplication
@EnableRabbit
@EnableConfigurationProperties(RabbitProperties.class)
@EnableAutoConfiguration(exclude=RabbitAutoConfiguration.class)
public class AmqpAffinityApplication {

	@Autowired
	private RabbitProperties props;

	private final String[] adminUris = { "http://localhost:15672", "http://localhost:15672" };

	private final String[] nodes = { "rabbit@localhost", "rabbit@localhost" };

	public static void main(String[] args) {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(AmqpAffinityApplication.class)
			.web(false)
			.run(args);
		RabbitTemplate template = context.getBean(RabbitTemplate.class);
		// force the queue creation before we start the container.
		template.getConnectionFactory().createConnection();
		RabbitListenerEndpointRegistry registry = context.getBean(RabbitListenerEndpointRegistry.class);
		registry.getListenerContainers().iterator().next().start();
		System.out.println(template.convertSendAndReceive("affinity.exch", "affinity", "foo"));
		context.close();
	}

	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() throws Exception {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(queueAffinityCF(defaultConnectionFactory()));
		factory.setAutoStartup(false);
		return factory;
	}

	@Bean
	public RabbitTemplate rabbitTemplate(@Qualifier("defaultConnectionFactory") ConnectionFactory defaultCF) {
		return new RabbitTemplate(defaultCF);
	}

	@Bean
	public RabbitAdmin rabbitAdmin(@Qualifier("defaultConnectionFactory") ConnectionFactory defaultCF) {
		return new RabbitAdmin(defaultCF);
	}

	@Bean
	public ConnectionFactory defaultConnectionFactory() {
		CachingConnectionFactory cf = new CachingConnectionFactory();
		cf.setAddresses(this.props.getAddresses());
		cf.setUsername(this.props.getUsername());
		cf.setPassword(this.props.getPassword());
		cf.setVirtualHost(this.props.getVirtualHost());
		return cf;
	}

	@Bean
	public ConnectionFactory queueAffinityCF(@Qualifier("defaultConnectionFactory") ConnectionFactory defaultCF) {
		return new LocalizedQueueConnectionFactory(defaultCF,
				StringUtils.commaDelimitedListToStringArray(this.props.getAddresses()),
				this.adminUris, this.nodes,
				this.props.getVirtualHost(), this.props.getUsername(), this.props.getPassword(),
				false, null);
	}

	@Bean
	public Listener listener() {
		return new Listener();
	}

	public static class Listener {

		@RabbitListener(bindings = @QueueBinding
				(value = @Queue(exclusive="false"),
				exchange = @Exchange(value = "affinity.exch", autoDelete = "true"),
				key = "affinity"))
		public String listen(String foo) {
			return foo.toUpperCase();
		}

	}

}
