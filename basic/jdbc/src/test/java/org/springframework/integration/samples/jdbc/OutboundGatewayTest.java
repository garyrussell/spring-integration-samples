/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.integration.samples.jdbc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Calendar;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.samples.jdbc.domain.Gender;
import org.springframework.integration.samples.jdbc.domain.Person;
import org.springframework.integration.samples.jdbc.service.PersonService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * The test class for jdbc outbound gateway
 * @author Amol Nayak
 *
 */
public class OutboundGatewayTest {

	private final Logger logger = Logger.getLogger(OutboundGatewayTest.class);

	@Test
	public void insertPersonRecord() {
		ConfigurableApplicationContext context
        = new ClassPathXmlApplicationContext("/META-INF/spring/integration/spring-integration-context.xml");
		PersonService service = context.getBean(PersonService.class);
		logger.info("Creating person Instance");
		Person person = new Person();
		Calendar dateOfBirth = Calendar.getInstance();
		dateOfBirth.set(1980, 0, 1);
		person.setDateOfBirth(dateOfBirth.getTime());
		person.setName("Name Of The Person");
		person.setGender(Gender.MALE);
		person = service.createPerson(person);

		person.setName("Name Of The Second Person");
		person = service.createPerson(person);
		Assert.assertNotNull("Expected a non null instance of Person, got null", person);
		logger.info("\n\tGenerated person with id: " + person.getPersonId() + ", with name: " + person.getName());

		QueueChannel errors = context.getBean("errors", QueueChannel.class);
		Message<?> received = errors.receive(10000);
		assertNotNull(received);
		assertThat(((MessagingException) received.getPayload()).getFailedMessage().toString(),
				containsString("NAME OF THE PERSON"));
		received = errors.receive(10000);
		assertNotNull(received);
		assertThat(((MessagingException) received.getPayload()).getFailedMessage().toString(),
				containsString("NAME OF THE SECOND PERSON"));
		logger.info("\n\tRetry and Error Recovery worked as expected");
		context.close();
	}

}
