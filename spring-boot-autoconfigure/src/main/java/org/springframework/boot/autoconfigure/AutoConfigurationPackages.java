/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Class for storing auto-configuration packages for reference later (e.g. by JPA entity
 * scanner).
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class AutoConfigurationPackages {

	private static Log logger = LogFactory.getLog(AutoConfigurationPackages.class);

	private static final String BEAN = AutoConfigurationPackages.class.getName();

	/**
	 * Return the auto-configuration base packages for the given bean factory
	 * @param beanFactory the source bean factory
	 * @return a list of auto-configuration packages
	 * @throws IllegalStateException if auto-configuration is not enabled
	 */
	public static List<String> get(BeanFactory beanFactory) {
		// Currently we only store a single base package, but we return a list to
		// allow this to change in the future if needed
		try {
			return beanFactory.getBean(BEAN, BasePackages.class).get();
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new IllegalStateException(
					"Unable to retrieve @EnableAutoConfiguration base packages");
		}
	}

	static void set(BeanDefinitionRegistry registry, String packageName) {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(BasePackages.class);
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0,
				packageName);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(BEAN, beanDefinition);
	}

	/**
	 * {@link ImportBeanDefinitionRegistrar} to store the base package from the importing
	 * configuration.
	 */
	@Order(Ordered.HIGHEST_PRECEDENCE)
	static class Registrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata metadata,
				BeanDefinitionRegistry registry) {
			set(registry, ClassUtils.getPackageName(metadata.getClassName()));
		}

	}

	/**
	 * Holder for the base package (name may be null to indicate no scanning).
	 */
	final static class BasePackages {

		private final List<String> packages;

		private boolean loggedBasePackageInfo;

		public BasePackages(String name) {
			this.packages = (StringUtils.hasText(name) ? Collections.singletonList(name)
					: Collections.<String> emptyList());
		}

		public List<String> get() {
			if (!this.loggedBasePackageInfo) {
				if (this.packages.isEmpty()) {
					if (logger.isWarnEnabled()) {
						logger.warn("@EnableAutoConfiguration was declared on a class "
								+ "in the default package. Automatic @Repository and "
								+ "@Entity scanning is not enabled.");
					}
				}
				else {
					if (logger.isDebugEnabled()) {
						String packageNames = StringUtils
								.collectionToCommaDelimitedString(this.packages);
						logger.debug("@EnableAutoConfiguration was declared on a class "
								+ "in the package '" + packageNames
								+ "'. Automatic @Repository and @Entity scanning is "
								+ "enabled.");
					}
				}
				this.loggedBasePackageInfo = true;
			}
			return this.packages;
		}

	}

}
