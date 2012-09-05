/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-service-middleware-server RebirthServiceMiddlewareServerVersion.java 2012-7-17 11:46:18 l.xue.nong$$
 */
package cn.com.rebirth.service.middleware.server;

import cn.com.rebirth.commons.AbstractVersion;
import cn.com.rebirth.commons.Version;

/**
 * The Class RebirthServiceMiddlewareServerVersion.
 *
 * @author l.xue.nong
 */
public class RebirthServiceMiddlewareServerVersion extends AbstractVersion implements Version {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 5651092788870918843L;

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.Version#getModuleName()
	 */
	@Override
	public String getModuleName() {
		return "rebirth-service-middleware-server";
	}

}
