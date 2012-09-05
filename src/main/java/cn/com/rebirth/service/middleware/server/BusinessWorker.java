/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-service-middleware-server BusinessWorker.java 2012-7-17 12:40:09 l.xue.nong$$
 */
package cn.com.rebirth.service.middleware.server;

import org.jboss.netty.channel.Channel;

import cn.com.rebirth.service.middleware.commons.protocol.RmiRequest;

/**
 * The Interface BusinessWorker.
 *
 * @author l.xue.nong
 */
public interface BusinessWorker {

	/**
	 * Work.
	 *
	 * @param req the req
	 * @param channel the channel
	 */
	void work(RmiRequest req, Channel channel);
}
