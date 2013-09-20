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

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JmsTemplate}.
 * 
 * @author Greg Turnquist
 */
@Configuration
@ConditionalOnClass({ JmsTemplate.class, ConnectionFactory.class })
public class JmsTemplateAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(JmsTemplate.class)
	@EnableConfigurationProperties(JmsTemplateProperties.class)
	protected static class JmsTemplateCreator {
		
		@Autowired
		private JmsTemplateProperties config;

		@Autowired
		private ConnectionFactory connectionFactory;

		@Bean
		public JmsTemplate jmsTemplate() {
			JmsTemplate jmsTemplate = new JmsTemplate(this.connectionFactory);
			jmsTemplate.setPubSubDomain(this.config.isPubSubDomain());
			return jmsTemplate;
		}

	}
	
	@ConfigurationProperties(name = "spring.jms")
	public static class JmsTemplateProperties {
		
		private boolean pubSubDomain = true;

		public boolean isPubSubDomain() {
			return pubSubDomain;
		}

		public void setPubSubDomain(boolean pubSubDomain) {
			this.pubSubDomain = pubSubDomain;
		}
		
	}

	@Configuration
	@ConditionalOnClass(ActiveMQConnectionFactory.class)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	@EnableConfigurationProperties(ActiveMQConnectionFactoryProperties.class)
	protected static class ActiveMQConnectionFactoryCreator {
		
		@Autowired
		private ActiveMQConnectionFactoryProperties config;
		
		@Bean
		ConnectionFactory connectionFactory() {
			if (this.config.isPooled()) {
				PooledConnectionFactory pool = new PooledConnectionFactory();
				pool.setConnectionFactory(new ActiveMQConnectionFactory(this.config.getBrokerURL()));
				return pool;
			} else {
				return new ActiveMQConnectionFactory(this.config.getBrokerURL());
			}
		}

	}
	
	@ConfigurationProperties(name = "spring.activemq")
	public static class ActiveMQConnectionFactoryProperties {
		
		private String brokerURL = "tcp://localhost:61616";
		
		private boolean inMemory = true;
		
		private boolean pooled = false;
		
		// Will override brokerURL if inMemory is set to true
		public String getBrokerURL() {
			if (this.inMemory) {
				return "vm://localhost";
			} else {
				return this.brokerURL;
			}
		}

		public void setBrokerURL(String brokerURL) {
			this.brokerURL = brokerURL;
		}

		public boolean isInMemory() {
			return inMemory;
		}

		public void setInMemory(boolean inMemory) {
			this.inMemory = inMemory;
		}

		public boolean isPooled() {
			return pooled;
		}

		public void setPooled(boolean pooled) {
			this.pooled = pooled;
		}
		
	}

}
