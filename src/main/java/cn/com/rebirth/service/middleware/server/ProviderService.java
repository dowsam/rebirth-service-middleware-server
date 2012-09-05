/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-service-middleware-server ProviderService.java 2012-7-17 15:09:05 l.xue.nong$$
 */
package cn.com.rebirth.service.middleware.server;

import java.lang.reflect.Method;

/**
 * The Class ProviderService.
 *
 * @author l.xue.nong
 */
public final class ProviderService {

	/** The key. */
	private final String key; //group+version+sign

	/** The group. */
	private final String group; //服务分组

	/** The version. */
	private final String version; //服务版本

	/** The sign. */
	private final String sign; //服务签名

	/** The service object. */
	private final Object serviceObject; //服务对象

	/** The service method. */
	private final Method serviceMethod; //服务方法

	/** The timeout. */
	private final long timeout; //超时时间

	/**
	 * Instantiates a new provider service.
	 *
	 * @param group the group
	 * @param version the version
	 * @param sign the sign
	 * @param serviceObject the service object
	 * @param serviceMethod the service method
	 * @param timeout the timeout
	 */
	public ProviderService(String group, String version, String sign, Object serviceObject, Method serviceMethod,
			long timeout) {
		this.group = group;
		this.version = version;
		this.sign = sign;
		this.serviceObject = serviceObject;
		this.serviceMethod = serviceMethod;
		this.timeout = timeout;
		this.key = group + version + sign;
	}

	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Gets the group.
	 *
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Gets the version.
	 *
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Gets the sign.
	 *
	 * @return the sign
	 */
	public String getSign() {
		return sign;
	}

	/**
	 * Gets the service object.
	 *
	 * @return the service object
	 */
	public Object getServiceObject() {
		return serviceObject;
	}

	/**
	 * Gets the service method.
	 *
	 * @return the service method
	 */
	public Method getServiceMethod() {
		return serviceMethod;
	}

	/**
	 * Gets the timeout.
	 *
	 * @return the timeout
	 */
	public long getTimeout() {
		return timeout;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("group=%s;version=%s;sign=%s;timeout=%s", group, version, sign, timeout);
	}

}
