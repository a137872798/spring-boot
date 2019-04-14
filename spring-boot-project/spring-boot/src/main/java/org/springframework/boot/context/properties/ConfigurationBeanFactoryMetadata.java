/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class to memorize {@code @Bean} definition meta data during initialization of
 * the bean factory.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
public class ConfigurationBeanFactoryMetadata implements BeanFactoryPostProcessor {

	/**
	 * The bean name that this class is registered with.
	 */
	public static final String BEAN_NAME = ConfigurationBeanFactoryMetadata.class
			.getName();

	private ConfigurableListableBeanFactory beanFactory;

	private final Map<String, FactoryMetadata> beansFactoryMetadata = new HashMap<>();

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		this.beanFactory = beanFactory;
		for (String name : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(name);
			String method = definition.getFactoryMethodName();
			String bean = definition.getFactoryBeanName();
			if (method != null && bean != null) {
				//每次含有工厂方法的bean 被创建时 都会缓存在这里   每个 @Bean 注解对应的方法 都是一个工厂名 和工厂方法名
				this.beansFactoryMetadata.put(name, new FactoryMetadata(bean, method));
			}
		}
	}

	/**
	 * 将所有携带指定注解的 java bean 返回
	 * @param type
	 * @param <A>
	 * @return
	 */
	public <A extends Annotation> Map<String, Object> getBeansWithFactoryAnnotation(
			Class<A> type) {
		Map<String, Object> result = new HashMap<>();
		for (String name : this.beansFactoryMetadata.keySet()) {
			if (findFactoryAnnotation(name, type) != null) {
				result.put(name, this.beanFactory.getBean(name));
			}
		}
		return result;
	}

	/**
	 * 找到工厂方法上对应的注解
	 * @param beanName
	 * @param type
	 * @param <A>
	 * @return
	 */
	public <A extends Annotation> A findFactoryAnnotation(String beanName,
			Class<A> type) {
		Method method = findFactoryMethod(beanName);
		return (method != null) ? AnnotationUtils.findAnnotation(method, type) : null;
	}

	/**
	 * 根据 beanName 找到对应的  工厂方法
	 * @param beanName
	 * @return
	 */
	public Method findFactoryMethod(String beanName) {
		if (!this.beansFactoryMetadata.containsKey(beanName)) {
			return null;
		}
		AtomicReference<Method> found = new AtomicReference<>(null);
		FactoryMetadata metadata = this.beansFactoryMetadata.get(beanName);
		//获取bean 的类型
		Class<?> factoryType = this.beanFactory.getType(metadata.getBean());
		//找到工厂方法名
		String factoryMethod = metadata.getMethod();
		if (ClassUtils.isCglibProxyClass(factoryType)) {
			factoryType = factoryType.getSuperclass();
		}
		//通过反射从class 中获取方法
		ReflectionUtils.doWithMethods(factoryType, (method) -> {
			if (method.getName().equals(factoryMethod)) {
				found.compareAndSet(null, method);
			}
		});
		return found.get();
	}

	private static class FactoryMetadata {

		private final String bean;

		private final String method;

		FactoryMetadata(String bean, String method) {
			this.bean = bean;
			this.method = method;
		}

		public String getBean() {
			return this.bean;
		}

		public String getMethod() {
			return this.method;
		}

	}

}
