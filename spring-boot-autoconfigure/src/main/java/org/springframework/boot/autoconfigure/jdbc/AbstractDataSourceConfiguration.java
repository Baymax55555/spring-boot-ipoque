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

package org.springframework.boot.autoconfigure.jdbc;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Base class for configuration of a database pool.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(path = "spring.datasource")
public abstract class AbstractDataSourceConfiguration implements BeanClassLoaderAware {

	private String driverClassName;

	private String url;

	private String username = "sa";

	private String password = "";

	private int maxActive = 8;

	private int maxIdle = 8;

	private int minIdle = 8;

	private String validationQuery;

	private boolean testOnBorrow = false;

	private boolean testOnReturn = false;

	private ClassLoader classLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	protected String getDriverClassName() {
		if (StringUtils.hasText(this.driverClassName)) {
			return this.driverClassName;
		}
		EmbeddedDatabaseConnection embeddedDatabaseConnection = EmbeddedDatabaseConnection
				.get(this.classLoader);
		this.driverClassName = embeddedDatabaseConnection.getDriverClassName();
		if (!StringUtils.hasText(this.driverClassName)) {
			throw new BeanCreationException(
					"Cannot determine embedded database driver class for database type "
							+ embeddedDatabaseConnection + ". If you want an embedded "
							+ "database please put a supoprted one on the classpath.");
		}
		return this.driverClassName;
	}

	protected String getUrl() {
		if (StringUtils.hasText(this.url)) {
			return this.url;
		}
		EmbeddedDatabaseConnection embeddedDatabaseConnection = EmbeddedDatabaseConnection
				.get(this.classLoader);
		this.url = embeddedDatabaseConnection.getUrl();
		if (!StringUtils.hasText(this.url)) {
			throw new BeanCreationException(
					"Cannot determine embedded database url for database type "
							+ embeddedDatabaseConnection + ". If you want an embedded "
							+ "database please put a supported on on the classpath.");
		}
		return this.url;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setMaxActive(int maxActive) {
		this.maxActive = maxActive;
	}

	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}

	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	public void setTestOnBorrow(boolean testOnBorrow) {
		this.testOnBorrow = testOnBorrow;
	}

	public void setTestOnReturn(boolean testOnReturn) {
		this.testOnReturn = testOnReturn;
	}

	protected String getUsername() {
		return this.username;
	}

	protected String getPassword() {
		return this.password;
	}

	protected int getMaxActive() {
		return this.maxActive;
	}

	protected int getMaxIdle() {
		return this.maxIdle;
	}

	protected int getMinIdle() {
		return this.minIdle;
	}

	protected String getValidationQuery() {
		return this.validationQuery;
	}

	protected boolean isTestOnBorrow() {
		return this.testOnBorrow;
	}

	protected boolean isTestOnReturn() {
		return this.testOnReturn;
	}

}
