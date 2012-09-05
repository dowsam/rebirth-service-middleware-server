/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-service-middleware-server TargetObjectContainer.java 2012-8-1 14:03:50 l.xue.nong$$
 */
package cn.com.rebirth.service.middleware.server.support;

/**
 * The Interface TargetObjectContainer.
 *
 * @param <T> the generic type
 * @author l.xue.nong
 */
public interface TargetObjectContainer<T> {

	/**
	 * Find.
	 *
	 * @param targetObjectClass the target object class
	 * @return the t
	 */
	T find(Class<?> targetObjectClass);
}
