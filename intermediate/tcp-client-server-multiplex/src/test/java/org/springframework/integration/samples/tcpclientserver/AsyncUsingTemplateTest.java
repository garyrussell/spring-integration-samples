/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.samples.tcpclientserver;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.samples.tcpclientserver.support.CustomTestContextLoader;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
@ContextConfiguration(loader=CustomTestContextLoader.class, locations={"/META-INF/spring/integration/tcpClientServerDemo-Template-context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncUsingTemplateTest {

	@Autowired
	MessageChannel input;

	@Autowired
	QueueChannel replyChannel;

	@Test
	public void test() {
		MessagingTemplate template = new MessagingTemplate();
		for (int i = 0; i < 5; i++) {
			Message<String> message = MessageBuilder.withPayload("foo")
					.setCorrelationId("bar")
					.setSequenceNumber(i+1)
					.setSequenceSize(5)
					.build();
			template.send(input, message);
		}
		Message<?> reply = template.receive(replyChannel);
		assertEquals(5, ((Collection<?>) reply.getPayload()).size());
	}

}
