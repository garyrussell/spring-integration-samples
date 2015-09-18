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

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.support.management.Statistics;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public class MessageHandlerInfo {

	private final AbstractMessageHandler handler;

	private final String input;

	private final String output;

	public MessageHandlerInfo(AbstractMessageHandler handler, String input, String output) {
		this.handler = handler;
		this.input = input;
		this.output = output;
	}

	public String getInput() {
		return this.input;
	}

	public String getOutput() {
		return this.output;
	}

	public boolean isLoggingEnabled() {
		return this.handler.isLoggingEnabled();
	}

	public String getComponentType() {
		return this.handler.getComponentType();
	}

	public final String getComponentName() {
		return this.handler.getComponentName();
	}

	public long getHandleCountLong() {
		return this.handler.getHandleCountLong();
	}

	public long getErrorCountLong() {
		return this.handler.getErrorCountLong();
	}

	public double getMeanDuration() {
		return this.handler.getMeanDuration();
	}

	public double getMinDuration() {
		return this.handler.getMinDuration();
	}

	public double getMaxDuration() {
		return this.handler.getMaxDuration();
	}

	public double getStandardDeviationDuration() {
		return this.handler.getStandardDeviationDuration();
	}

	public long getActiveCountLong() {
		return this.handler.getActiveCountLong();
	}

	public Statistics getDuration() {
		return this.handler.getDuration();
	}

	public boolean isStatsEnabled() {
		return this.handler.isStatsEnabled();
	}

	public boolean isCountsEnabled() {
		return this.handler.isCountsEnabled();
	}

}

