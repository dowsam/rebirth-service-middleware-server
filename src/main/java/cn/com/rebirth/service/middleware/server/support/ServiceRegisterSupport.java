/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-service-middleware-server ServiceRegisterSupport.java 2012-7-17 14:42:34 l.xue.nong$$
 */
package cn.com.rebirth.service.middleware.server.support;

import static java.lang.String.format;

import java.util.List;
import java.util.Map;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.rebirth.commons.component.AbstractLifecycleComponent;
import cn.com.rebirth.commons.exception.RebirthException;
import cn.com.rebirth.commons.search.config.support.ZooKeeperExpand;
import cn.com.rebirth.commons.settings.Settings;
import cn.com.rebirth.service.middleware.commons.Message;
import cn.com.rebirth.service.middleware.commons.MessageSubscriber;
import cn.com.rebirth.service.middleware.commons.Messages;
import cn.com.rebirth.service.middleware.server.ProviderService;
import cn.com.rebirth.service.middleware.server.message.RegisterServiceMessage;

import com.google.common.collect.Maps;

/**
 * The Class ServiceRegisterSupport.
 *
 * @author l.xue.nong
 */
public class ServiceRegisterSupport extends AbstractLifecycleComponent<ServiceRegisterSupport> implements
		MessageSubscriber {

	/** The messages. */
	private final Messages messages;

	/** The server support. */
	private final ServerSupport serverSupport;

	/**
	 * Instantiates a new service register support.
	 *
	 * @param settings the settings
	 * @param messages the messages
	 * @param serverSupport the server support
	 */
	protected ServiceRegisterSupport(Settings settings, Messages messages, ServerSupport serverSupport) {
		super(settings);
		this.messages = messages;
		this.serverSupport = serverSupport;
	}

	/** The logger. */
	private final Logger logger = LoggerFactory.getLogger(ServiceRegisterSupport.class);

	/** The services. */
	private Map<String, ProviderService> services;

	/* (non-Javadoc)
	 * @see cn.com.rebirth.service.middleware.commons.MessageSubscriber#receive(cn.com.rebirth.service.middleware.commons.Message)
	 */
	@Override
	public void receive(Message<?> msg) throws RebirthException {
		if (!(msg instanceof RegisterServiceMessage)) {
			return;
		}
		final ProviderService service = ((RegisterServiceMessage) msg).getContent();
		final String key = service.getKey();

		if (services.containsKey(key)) {
			if (logger.isInfoEnabled()) {
				logger.info(format("service:%s already registed.", service));
			}
		}

		final String pref = format("/rebirth/service/middleware/nondurable/%s/%s/%s", service.getGroup(),
				service.getVersion(), service.getSign());

		try {
			String _p = format("%s%s", pref, serverSupport.getPublishAddress());
			ZooKeeperExpand.getInstance().create(_p);
			services.put(key, service);
		} catch (Exception e) {
			logger.warn(format("create service:%s path failed", service), e);
		}

	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStart()
	 */
	@Override
	protected void doStart() throws RebirthException {
		Messages.register(this, RegisterServiceMessage.class);
		services = Maps.newConcurrentMap();

		ZooKeeperExpand.getInstance().getZkClient().subscribeStateChanges(new IZkStateListener() {

			@Override
			public void handleStateChanged(KeeperState state) throws Exception {
				if (KeeperState.SyncConnected.equals(state)) {
					if (logger.isInfoEnabled()) {
						logger.info("zk-server reconnected, must reRegister right now.");
					}
					for (ProviderService providerService : services.values()) {
						messages.post(new RegisterServiceMessage(providerService));
					}
				}
			}

			@Override
			public void handleNewSession() throws Exception {

			}
		});
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStop()
	 */
	@Override
	protected void doStop() throws RebirthException {
		if (services != null && !services.isEmpty()) {
			for (Map.Entry<String, ProviderService> entry : services.entrySet()) {
				ProviderService service = entry.getValue();
				final String pref = format("/rebirth/service/middleware/nondurable/%s/%s/%s", service.getGroup(),
						service.getVersion(), service.getSign());
				ZooKeeperExpand.getInstance().delete(format("%s%s", pref, serverSupport.getPublishAddress()));
			}
		}
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doClose()
	 */
	@Override
	protected void doClose() throws RebirthException {

	}

	public static void main(String[] args) {
		System.setProperty("zk.zkConnect", "192.168.2.179:2181");
		System.setProperty("rebirth.service.middleware.container.className",
				"cn.com.rebirth.knowledge.scheduler.InitContainer");
		System.setProperty("rebirth.service.middleware.container.time.consuming.className",
				"cn.com.rebirth.knowledge.scheduler.InitContainer$TimeConsumingInitContainer");
		System.setProperty("rebirth.service.middleware.development.model", "true");
		List<String> a = ZooKeeperExpand.getInstance().list(
				"/rebirth/service/middleware/nondurable/DefaultGroup/defalut/36ae6073d9ddb59a1b348a08695f9011");
		for (String string : a) {
			System.out.println(string);
		}
		ZooKeeperExpand
				.getInstance()
				.getZkClient()
				.subscribeChildChanges(
						"/rebirth/service/middleware/nondurable/DefaultGroup/defalut/36ae6073d9ddb59a1b348a08695f9011",
						new IZkChildListener() {

							@Override
							public void handleChildChange(String parentPath, List<String> currentChilds)
									throws Exception {
								System.out.println(currentChilds);
							}
						});
		ZooKeeperExpand
				.getInstance()
				.create("/rebirth/service/middleware/nondurable/DefaultGroup/defalut/36ae6073d9ddb59a1b348a08695f9011/192.168.2.99:9701");

		a = ZooKeeperExpand.getInstance().list(
				"/rebirth/service/middleware/nondurable/DefaultGroup/defalut/36ae6073d9ddb59a1b348a08695f9011");
		for (String string : a) {
			System.out.println(string);
		}
	}
}
