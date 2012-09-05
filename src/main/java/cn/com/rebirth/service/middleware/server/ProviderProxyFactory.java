/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-service-middleware-server ProviderProxyFactory.java 2012-7-17 14:07:25 l.xue.nong$$
 */
package cn.com.rebirth.service.middleware.server;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import cn.com.rebirth.commons.component.AbstractLifecycleComponent;
import cn.com.rebirth.commons.exception.RebirthException;
import cn.com.rebirth.commons.exception.RebirthIllegalArgumentException;
import cn.com.rebirth.commons.settings.Settings;
import cn.com.rebirth.service.middleware.commons.Messages;
import cn.com.rebirth.service.middleware.commons.utils.SerializerUtils;
import cn.com.rebirth.service.middleware.commons.utils.SignatureUtils;
import cn.com.rebirth.service.middleware.server.message.RegisterServiceMessage;
import cn.com.rebirth.service.middleware.server.support.ProviderSupport;

/**
 * A factory for creating ProviderProxy objects.
 */
public class ProviderProxyFactory extends AbstractLifecycleComponent<ProviderProxyFactory> {

	/** The messages. */
	private final Messages messages;

	/**
	 * Instantiates a new provider proxy factory.
	 *
	 * @param settings the settings
	 */
	public ProviderProxyFactory(Settings settings) {
		super(settings);
		this.messages = new Messages(settings);
	}

	/** The support. */
	private ProviderSupport support;

	/**
	 * Creates a new ProviderProxy object.
	 *
	 * @param targetObject the target object
	 * @return the invocation handler
	 */
	private InvocationHandler createProviderProxyHandler(final Object targetObject) {
		return new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return method.invoke(targetObject, args);
			}

		};
	}

	/**
	 * Proxy.
	 *
	 * @param <T> the generic type
	 * @param targetInterface the target interface
	 * @param targetObject the target object
	 * @param group the group
	 * @param version the version
	 * @param defaultTimeout the default timeout
	 * @param methodTimeoutMap the method timeout map
	 * @return the t
	 * @throws Exception the exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T proxy(Class<T> targetInterface, Object targetObject, String group, String version,
			long defaultTimeout, Map<String, Long> methodTimeoutMap) throws RebirthException {

		check(targetInterface, targetObject, group, version);

		final InvocationHandler providerProxyHandler = createProviderProxyHandler(targetObject);

		Object proxyObject = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
				new Class<?>[] { targetInterface }, providerProxyHandler);

		publishService(targetInterface, group, version, proxyObject, defaultTimeout, methodTimeoutMap);

		return (T) proxyObject;
	}

	/**
	 * Check.
	 *
	 * @param targetInterface the target interface
	 * @param targetObject the target object
	 * @param group the group
	 * @param version the version
	 */
	private void check(Class<?> targetInterface, Object targetObject, String group, String version) {
		if (StringUtils.isBlank(version)) {
			throw new RebirthIllegalArgumentException("version is blank");
		}
		if (StringUtils.isBlank(group)) {
			throw new RebirthIllegalArgumentException("group is blank");
		}
		if (null == targetInterface) {
			throw new RebirthIllegalArgumentException("targetInterface is null");
		}
		if (null == targetObject) {
			throw new RebirthIllegalArgumentException("targetObject is null");
		}

		if (!targetInterface.isAssignableFrom(targetObject.getClass())) {
			throw new RebirthIllegalArgumentException(String.format(
					"targetObject[%s] does not instanceof proxyInterface[%s]", targetObject.getClass(),
					targetInterface.getClass()));
		}

		Method[] methods = targetInterface.getMethods();
		if (null != methods) {
			for (Method method : methods) {
				//				if (!SerializerUtils.isSerializableType(method.getReturnType())) {
				//					throw new RebirthIllegalArgumentException("method returnType is not serializable");
				//				}
				if (!SerializerUtils.isSerializableType(method.getParameterTypes())) {
					throw new RebirthIllegalArgumentException("method parameter is not serializable");
				}
			}//for
		}//if

	}

	/**
	 * Gets the fix timeout.
	 *
	 * @param method the method
	 * @param defaultTimeout the default timeout
	 * @param methodTimeoutMap the method timeout map
	 * @return the fix timeout
	 */
	private long getFixTimeout(Method method, long defaultTimeout, Map<String, Long> methodTimeoutMap) {

		if (methodTimeoutMap != null && !methodTimeoutMap.isEmpty() && methodTimeoutMap.containsKey(method.getName())) {
			Long timeout = methodTimeoutMap.get(method.getName());
			if (null != timeout && timeout > 0) {
				return timeout;
			}//if
		}//if

		return defaultTimeout > 0 ? defaultTimeout : 500;

	}

	/**
	 * Publish service.
	 *
	 * @param targetInterface the target interface
	 * @param group the group
	 * @param version the version
	 * @param proxyObject the proxy object
	 * @param defaultTimeout the default timeout
	 * @param methodTimeoutMap the method timeout map
	 */
	private void publishService(Class<?> targetInterface, String group, String version, Object proxyObject,
			long defaultTimeout, Map<String, Long> methodTimeoutMap) {
		Method[] methods = targetInterface.getMethods();
		if (null == methods) {
			return;
		}

		for (Method method : methods) {
			final String sign = SignatureUtils.signature(targetInterface,method);
			final ProviderService providerService = new ProviderService(group, version, sign, proxyObject, method,
					getFixTimeout(method, defaultTimeout, methodTimeoutMap));
			messages.post(new RegisterServiceMessage(providerService));
		}
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStart()
	 */
	@Override
	protected void doStart() throws RebirthException {
		support = new ProviderSupport(settings, this.messages, this);
		support.start();
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStop()
	 */
	@Override
	protected void doStop() throws RebirthException {
		if (support != null) {
			support.stop();
		}
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doClose()
	 */
	@Override
	protected void doClose() throws RebirthException {
		if (support != null) {
			support.close();
		}
	}

}
