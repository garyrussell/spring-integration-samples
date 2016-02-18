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
package org.springframework.integration.samples;

import org.springframework.integration.endpoint.AbstractMessageSource;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public class MessageSourceInfo {

	private final AbstractMessageSource<?> source;

	private final String output;

	public MessageSourceInfo(AbstractMessageSource<?> source, String output) {
		this.source = source;
		this.output = output;
	}

	public String getComponentType() {
		return this.source.getComponentType();
	}

	public String getComponentName() {
		return this.source.getComponentName();
	}

	public long getMessageCount() {
		return this.source.getMessageCountLong();
	}

	public String getOutput() {
		return this.output;
	}
}

