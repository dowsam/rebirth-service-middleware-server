/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-service-middleware-server ProviderSupport.java 2012-7-17 14:59:34 l.xue.nong$$
 */
package cn.com.rebirth.service.middleware.server.support;

import cn.com.rebirth.commons.component.AbstractLifecycleComponent;
import cn.com.rebirth.commons.exception.RebirthException;
import cn.com.rebirth.commons.settings.Settings;
import cn.com.rebirth.service.middleware.commons.Messages;
import cn.com.rebirth.service.middleware.server.NetworkService;
import cn.com.rebirth.service.middleware.server.ProviderProxyFactory;

/**
 * The Class ProviderSupport.
 *
 * @author l.xue.nong
 */
public class ProviderSupport extends AbstractLifecycleComponent<ProviderSupport> {

	/** The messages. */
	private final Messages messages;

	/**
	 * Instantiates a new provider support.
	 *
	 * @param settings the settings
	 * @param messages the messages
	 * @param providerProxyFactory the provider proxy factory
	 */
	public ProviderSupport(Settings settings, Messages messages, ProviderProxyFactory providerProxyFactory) {
		super(settings);
		this.messages = messages;
		this.providerProxyFactory = providerProxyFactory;
	}

	/** The worker support. */
	private WorkerSupport workerSupport;

	/** The server support. */
	private ServerSupport serverSupport;

	/** The service register support. */
	private ServiceRegisterSupport serviceRegisterSupport;

	/** The provider proxy factory. */
	private final ProviderProxyFactory providerProxyFactory;
	private ScanRmiSupport scanRmiSupport;

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStart()
	 */
	@Override
	protected void doStart() throws RebirthException {
		// new
		workerSupport = new WorkerSupport(settings);
		serverSupport = new ServerSupport(settings, new NetworkService(settings));
		scanRmiSupport = new ScanRmiSupport(settings, providerProxyFactory);
		serviceRegisterSupport = new ServiceRegisterSupport(settings, messages, serverSupport);

		// setter
		serverSupport.setBusinessWorker(workerSupport);

		workerSupport.start();
		serverSupport.start();
		serviceRegisterSupport.start();
		scanRmiSupport.start();
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStop()
	 */
	@Override
	protected void doStop() throws RebirthException {
		if (null != workerSupport) {
			workerSupport.stop();
		}
		if (scanRmiSupport != null) {
			scanRmiSupport.stop();
		}
		if (null != serverSupport) {
			serverSupport.stop();
		}
		if (null != serviceRegisterSupport) {
			serviceRegisterSupport.stop();
		}

	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doClose()
	 */
	@Override
	protected void doClose() throws RebirthException {
		if (null != workerSupport) {
			workerSupport.close();
		}
		if (scanRmiSupport != null) {
			scanRmiSupport.close();
		}
		if (null != serverSupport) {
			serverSupport.close();
		}
		if (null != serviceRegisterSupport) {
			serviceRegisterSupport.close();
		}
	}

}
