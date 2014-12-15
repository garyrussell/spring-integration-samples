/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.samples.amqp;

import java.io.Serializable;

import org.apache.log4j.Logger;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * Starts the Spring Context and will initialize the Spring Integration message flow.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 */
public final class SampleSimple {

	private static final Logger LOGGER = Logger.getLogger(SampleSimple.class);

	private SampleSimple() { }

	/**
	 * Load the Spring Integration Application Context
	 *
	 * @param args - command line arguments
	 */
	public static void main(final String... args) {

		LOGGER.info("\n========================================================="
				  + "\n                                                         "
				  + "\n          Welcome to Spring Integration!                 "
				  + "\n                                                         "
				  + "\n    For more information please visit:                   "
				  + "\n    http://www.springsource.org/spring-integration       "
				  + "\n                                                         "
				  + "\n=========================================================" );

		@SuppressWarnings("resource")
		final AbstractApplicationContext context =
				new ClassPathXmlApplicationContext("classpath:META-INF/spring/integration/spring-integration-context.xml");

		context.registerShutdownHook();

		context.getBean("toRabbit", MessageChannel.class).send(new GenericMessage<Foo>(new Foo("bar")));

		LOGGER.info("\n========================================================="
				  + "\n                                                          "
				  + "\n    This is the AMQP Sample -                             "
				  + "\n                                                          "
				  + "\n    Please enter some text and press return. The entered  "
				  + "\n    Message will be sent to the configured RabbitMQ Queue,"
				  + "\n    then again immediately retrieved from the Message     "
				  + "\n    Broker and ultimately printed to the command line.    "
				  + "\n                                                          "
				  + "\n=========================================================" );

	}

	@SuppressWarnings("serial")
	public static class Foo implements Serializable {

		private final String foo;

		public Foo(String foo) {
			this.foo = foo;
		}

		protected String getFoo() {
			return foo;
		}

		@Override
		public String toString() {
			return "Foo [foo=" + foo + "]";
		}

	}

}
