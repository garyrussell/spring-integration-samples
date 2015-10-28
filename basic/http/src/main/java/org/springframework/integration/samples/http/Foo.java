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
package org.springframework.integration.samples.http;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public class Foo {

	public MultiValueMap<String, Object> handle(String in) {
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("foo", in);
		map.add("bar", "baz");
		map.add("fiz", "qux".getBytes());
		return map;
	}

}
