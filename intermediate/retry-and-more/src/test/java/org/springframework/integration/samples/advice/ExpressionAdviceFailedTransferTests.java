/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.samples.advice;

import static org.mockito.Mockito.when;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration(locations="/META-INF/spring/integration/expression-advice-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class ExpressionAdviceFailedTransferTests {

	@Autowired
	private SessionFactory<FTPFile> sessionFactory;

	@Autowired
	private SourcePollingChannelAdapter fileInbound;

	@Autowired
	private PollableChannel testWaitForDoneChannel;

	@Before
	public void setup() {
		when(sessionFactory.getSession()).thenThrow(new RuntimeException("Force Failure"));
		fileInbound.start();
	}

	@Test
	public void test() {
		System.out.println("Put a file ending .txt in ${java.io.tmpdir}/adviceDemo\n" +
				"Test will terminate in 60 seconds if no file found");
		testWaitForDoneChannel.receive(60000);
	}

}
