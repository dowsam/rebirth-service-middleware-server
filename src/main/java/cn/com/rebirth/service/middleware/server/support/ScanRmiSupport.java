/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-service-middleware-server ScanRmiSupport.java 2012-7-17 15:34:16 l.xue.nong$$
 */
package cn.com.rebirth.service.middleware.server.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import cn.com.rebirth.commons.component.AbstractLifecycleComponent;
import cn.com.rebirth.commons.exception.RebirthException;
import cn.com.rebirth.commons.service.middleware.annotation.Rmi;
import cn.com.rebirth.commons.service.middleware.annotation.RmiMethod;
import cn.com.rebirth.commons.settings.Settings;
import cn.com.rebirth.commons.utils.ClassResolverUtils;
import cn.com.rebirth.commons.utils.ReflectionUtils;
import cn.com.rebirth.commons.utils.ResolverUtils;
import cn.com.rebirth.service.middleware.server.ProviderProxyFactory;

import com.google.common.collect.Maps;

/**
 * The Class ScanRmiSupport.
 *
 * @author l.xue.nong
 */
public class ScanRmiSupport extends AbstractLifecycleComponent<ScanRmiSupport> {

	/** The factory. */
	protected final ProviderProxyFactory factory;
	@SuppressWarnings("rawtypes")
	private static volatile List<TargetObjectContainer> targetObjectContainers = ClassResolverUtils
			.findImpl(TargetObjectContainer.class);

	/**
	 * Instantiates a new scan rmi support.
	 *
	 * @param settings the settings
	 * @param factory the factory
	 */
	protected ScanRmiSupport(Settings settings, ProviderProxyFactory factory) {
		super(settings);
		this.factory = factory;
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStart()
	 */
	@Override
	protected void doStart() throws RebirthException {
		ResolverUtils<Rmi> resolverUtils = new ResolverUtils<Rmi>();
		resolverUtils.findAnnotated(Rmi.class, StringUtils.EMPTY);
		Set<Class<? extends Rmi>> classes = resolverUtils.getClasses();
		for (Class<? extends Rmi> class1 : classes) {
			if (class1.isInterface()) {
				Rmi rmi = class1.getAnnotation(Rmi.class);
				Object targetObject = findImpl(rmi.targetObject());
				if (targetObject == null) {
					logger.error("Class is[{}],not find Impl", class1);
					continue;
				}
				Map<String, Long> methodTimeoutMap = findMethodTimeOut(class1);
				this.factory.proxy(class1, targetObject, rmi.group(), rmi.version(), rmi.timeOut(), methodTimeoutMap);
			}
		}
	}

	/**
	 * Find method time out.
	 *
	 * @param class1 the class1
	 * @return the map
	 */
	private Map<String, Long> findMethodTimeOut(Class<? extends Rmi> class1) {
		Map<String, Long> map = Maps.newHashMap();
		Method[] methods = class1.getMethods();
		for (Method method : methods) {
			RmiMethod rmiMethod = method.getAnnotation(RmiMethod.class);
			if (rmiMethod != null) {
				map.put(method.getName(), rmiMethod.timeOut());
			}
		}
		return map;
	}

	/**
	 * Find impl.
	 *
	 * @param targetObject the target object
	 * @return the object
	 */
	private Object findImpl(String className) {
		Class<?> targetObject = null;
		try {
			targetObject = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
		} catch (ClassNotFoundException e2) {
			return null;
		}
		//find container
		synchronized (ScanRmiSupport.class) {
			for (TargetObjectContainer<?> targetObjectContainer : targetObjectContainers) {
				Class<?> class1 = ReflectionUtils.getSuperClassGenricType(targetObjectContainer.getClass());
				if (class1 == targetObject || class1.equals(targetObject)) {
					return targetObjectContainer.find(targetObject);
				}
			}
		}
		Constructor<?> constructor = null;
		try {
			constructor = targetObject.getDeclaredConstructor(Settings.class);
			try {
				return constructor.newInstance(settings);
			} catch (Exception e) {
				return null;
			}
		} catch (SecurityException e) {
			logger.error(e.getMessage(), e);
			return null;
		} catch (NoSuchMethodException e) {
			try {
				constructor = targetObject.getDeclaredConstructor();
				try {
					return constructor.newInstance();
				} catch (Exception ee) {
					return null;
				}
			} catch (SecurityException e1) {
				logger.error(e.getMessage(), e);
				return null;
			} catch (NoSuchMethodException e1) {
				logger.error(e.getMessage(), e);
				return null;
			}
		}
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStop()
	 */
	@Override
	protected void doStop() throws RebirthException {

	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doClose()
	 */
	@Override
	protected void doClose() throws RebirthException {

	}

}
