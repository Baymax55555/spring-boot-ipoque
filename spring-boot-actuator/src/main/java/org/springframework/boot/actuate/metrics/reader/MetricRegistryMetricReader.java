/*
 * Copyright 2013-2104 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.actuate.metrics.reader;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Timer;

/**
 * A Spring Boot {@link MetricReader} that reads metrics from a Codahale
 * {@link MetricRegistry}. Gauges and Counters are reflected as a single value. Timers,
 * Meters and Histograms are expanded into sets of metrics containing all the properties
 * of type Number.
 * 
 * @author Dave Syer
 *
 */
public class MetricRegistryMetricReader implements MetricReader, MetricRegistryListener {

	private static Map<Class<?>, Set<String>> numberKeys = new ConcurrentHashMap<Class<?>, Set<String>>();

	private MetricRegistry registry;

	private Map<String, String> names = new HashMap<String, String>();

	private MultiValueMap<String, String> reverse = new LinkedMultiValueMap<String, String>();

	public MetricRegistryMetricReader(MetricRegistry registry) {
		this.registry = registry;
		registry.addListener(this);
	}

	@Override
	public Metric<?> findOne(String metricName) {
		if (!names.containsKey(metricName)) {
			return null;
		}
		com.codahale.metrics.Metric metric = registry.getMetrics().get(
				names.get(metricName));
		if (metric instanceof Counter) {
			Counter counter = (Counter) metric;
			return new Metric<Number>(metricName, counter.getCount());
		}
		if (metric instanceof Gauge) {
			@SuppressWarnings("unchecked")
			Gauge<Number> value = (Gauge<Number>) metric;
			return new Metric<Number>(metricName, value.getValue());
		}
		if (metric instanceof Sampling) {
			if (metricName.contains(".snapshot.")) {
				Number value = getMetric(((Sampling) metric).getSnapshot(), metricName);
				if (metric instanceof Timer) {
					// convert back to MILLISEC
					value = TimeUnit.MILLISECONDS.convert(value.longValue(),
							TimeUnit.NANOSECONDS);
				}
				return new Metric<Number>(metricName, value);
			}
		}
		return new Metric<Number>(metricName, getMetric(metric, metricName));
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		return new Iterable<Metric<?>>() {
			@Override
			public Iterator<Metric<?>> iterator() {
				return new MetricRegistryIterator();
			}
		};
	}

	@Override
	public long count() {
		return names.size();
	}

	@Override
	public void onGaugeAdded(String name, Gauge<?> gauge) {
		names.put(name, name);
		reverse.add(name, name);
	}

	@Override
	public void onGaugeRemoved(String name) {
		remove(name);
	}

	public void onCounterAdded(String name, Counter counter) {
		names.put(name, name);
		reverse.add(name, name);
	}

	@Override
	public void onCounterRemoved(String name) {
		remove(name);
	}

	@Override
	public void onHistogramAdded(String name, Histogram histogram) {
		for (String key : getNumberKeys(histogram)) {
			String metricName = name + "." + key;
			names.put(metricName, name);
			reverse.add(name, metricName);
		}
		for (String key : getNumberKeys(histogram.getSnapshot())) {
			String metricName = name + ".snapshot." + key;
			names.put(metricName, name);
			reverse.add(name, metricName);
		}
	}

	@Override
	public void onHistogramRemoved(String name) {
		remove(name);
	}

	@Override
	public void onMeterAdded(String name, Meter meter) {
		for (String key : getNumberKeys(meter)) {
			String metricName = name + "." + key;
			names.put(metricName, name);
			reverse.add(name, metricName);
		}
	}

	@Override
	public void onMeterRemoved(String name) {
		remove(name);
	}

	@Override
	public void onTimerAdded(String name, Timer timer) {
		for (String key : getNumberKeys(timer)) {
			String metricName = name + "." + key;
			names.put(metricName, name);
			reverse.add(name, metricName);
		}
		for (String key : getNumberKeys(timer.getSnapshot())) {
			String metricName = name + ".snapshot." + key;
			names.put(metricName, name);
			reverse.add(name, metricName);
		}
	}

	@Override
	public void onTimerRemoved(String name) {
		remove(name);
	}

	private void remove(String name) {
		for (String key : reverse.get(name)) {
			names.remove(name + "." + key);
		}
		reverse.remove(name);
	}

	private class MetricRegistryIterator implements Iterator<Metric<?>> {

		private Iterator<String> iterator;

		public MetricRegistryIterator() {
			this.iterator = new HashSet<String>(
					MetricRegistryMetricReader.this.names.keySet()).iterator();
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Metric<?> next() {
			String name = iterator.next();
			return MetricRegistryMetricReader.this.findOne(name);
		}

	}

	private static Set<String> getNumberKeys(Object metric) {
		Set<String> result = numberKeys.containsKey(metric.getClass()) ? numberKeys
				.get(metric.getClass()) : new HashSet<String>();
		if (result.isEmpty()) {
			for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(metric
					.getClass())) {
				if (ClassUtils.isAssignable(Number.class, descriptor.getPropertyType())) {
					result.add(descriptor.getName());
				}
			}
			numberKeys.put(metric.getClass(), result);
		}
		return result;
	}

	private static Number getMetric(Object metric, String metricName) {
		String key = StringUtils.getFilenameExtension(metricName);
		return (Number) new BeanWrapperImpl(metric).getPropertyValue(key);
	}

}
