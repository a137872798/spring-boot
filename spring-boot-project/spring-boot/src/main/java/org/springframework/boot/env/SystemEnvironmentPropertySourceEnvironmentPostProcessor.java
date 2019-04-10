/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.env;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.SystemEnvironmentOrigin;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * An {@link EnvironmentPostProcessor} that replaces the systemEnvironment
 * {@link SystemEnvironmentPropertySource} with an
 * {@link OriginAwareSystemEnvironmentPropertySource} that can track the
 * {@link SystemEnvironmentOrigin} for every system environment property.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class SystemEnvironmentPropertySourceEnvironmentPostProcessor
		implements EnvironmentPostProcessor, Ordered {

	/**
	 * The default order for the processor.
	 */
	public static final int DEFAULT_ORDER = SpringApplicationJsonEnvironmentPostProcessor.DEFAULT_ORDER
			- 1;

	private int order = DEFAULT_ORDER;

	/**
	 * 当环境初始化完成时的后置函数
	 * @param environment the environment to post-process
	 * @param application the application to which the environment belongs
	 */
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		//获取标准的环境名字
		String sourceName = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
		//当环境被初始化时 会自动加载  environment 中的各个属性 以及 System.getProperties() 的属性
		PropertySource<?> propertySource = environment.getPropertySources()
				.get(sourceName);
		//这里会返回 SystemEnvironmentPropertySource
		if (propertySource != null) {
			replacePropertySource(environment, sourceName, propertySource);
		}
	}

	/**
	 * 这里将 SystemEnvironmentPropertySource 替换成了 OriginAwareSystemEnvironmentPropertySource 后重新设置到了 environment中
	 * OriginAwareSystemEnvironmentPropertySource 多实现了一个接口
	 * @param environment
	 * @param sourceName
	 * @param propertySource
	 */
	@SuppressWarnings("unchecked")
	private void replacePropertySource(ConfigurableEnvironment environment,
			String sourceName, PropertySource<?> propertySource) {
		Map<String, Object> originalSource = (Map<String, Object>) propertySource
				.getSource();
		SystemEnvironmentPropertySource source = new OriginAwareSystemEnvironmentPropertySource(
				sourceName, originalSource);
		environment.getPropertySources().replace(sourceName, source);
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * {@link SystemEnvironmentPropertySource} that also tracks {@link Origin}.
	 */
	protected static class OriginAwareSystemEnvironmentPropertySource
			extends SystemEnvironmentPropertySource implements OriginLookup<String> {

		OriginAwareSystemEnvironmentPropertySource(String name,
				Map<String, Object> source) {
			super(name, source);
		}

		@Override
		public Origin getOrigin(String key) {
			//通过该key 找到该属性
			String property = resolvePropertyName(key);
			if (super.containsProperty(property)) {
				return new SystemEnvironmentOrigin(property);
			}
			return null;
		}

	}

}
