/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-service-middleware-server RegisterServiceMessage.java 2012-7-17 12:39:10 l.xue.nong$$
 */
package cn.com.rebirth.service.middleware.server.message;

import cn.com.rebirth.service.middleware.commons.Message;
import cn.com.rebirth.service.middleware.server.ProviderService;

/**
 * The Class RegisterServiceMessage.
 *
 * @author l.xue.nong
 */
public class RegisterServiceMessage extends Message<ProviderService> {

	/**
	 * Instantiates a new register service message.
	 *
	 * @param t the t
	 */
	public RegisterServiceMessage(ProviderService t) {
		super(t);
	}

}
