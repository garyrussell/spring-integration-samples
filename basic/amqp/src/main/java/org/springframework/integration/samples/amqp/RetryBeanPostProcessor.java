/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.samples.amqp;

import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.PatternMatchUtils;



/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class RetryBeanPostProcessor implements BeanPostProcessor {

	private volatile String[] beanNamePatterns = new String[] {"*"};

	private final static Log logger = LogFactory.getLog(RetryBeanPostProcessor.class);

	/**
	 * @param beanNamePatterns an array of patterns used to match
	 * {@link MessageHandler} bean names to which the circuit breaker
	 * will be applied.
	 */
	public void setBeanNamePatterns(String[] beanNamePatterns) {
		this.beanNamePatterns = beanNamePatterns;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (!(bean instanceof MessageHandler)) {
			return bean;
		}
		if (!PatternMatchUtils.simpleMatch(this.beanNamePatterns, beanName)) {
			return bean;
		}
		RetryInterceptor interceptor = new RetryInterceptor();
		NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(interceptor);
		advisor.addMethodName("handleMessage");
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (AopUtils.canApply(advisor.getPointcut(), targetClass)) {
			if (bean instanceof Advised) {
				((Advised) bean).addAdvisor(advisor);
				return bean;
			}
			else {
				ProxyFactory proxyFactory = new ProxyFactory(bean);
				proxyFactory.addAdvisor(advisor);
				return proxyFactory.getProxy(bean.getClass().getClassLoader());
			}
		}
		return bean;
	}

	private class RetryInterceptor implements MethodInterceptor {

		private static final int TRIES = 5;

		public Object invoke(MethodInvocation invocation) throws Throwable {

			for (int i = 0; i < TRIES; i++) {
				try {
					if (i > 0) {
						logger.debug("Retrying...");
					}
					Object result = invocation.proceed();
					return result;
				}
				catch (MessagingException e) {
					e.printStackTrace();
					if (i >= TRIES) {
						throw e;
					}
					Thread.sleep(i + 1000);
				}
			}
			return null;
		}
	}
}
