/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.actuate.endpoint.health;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Dave Syer
 */
@Controller
public class HealthEndpoint<T> {

	private HealthIndicator<? extends T> indicator;

	/**
	 * @param indicator
	 */
	public HealthEndpoint(HealthIndicator<? extends T> indicator) {
		super();
		this.indicator = indicator;
	}

	@RequestMapping("${endpoints.health.path:/health}")
	@ResponseBody
	public T health() {
		return this.indicator.health();
	}

}
