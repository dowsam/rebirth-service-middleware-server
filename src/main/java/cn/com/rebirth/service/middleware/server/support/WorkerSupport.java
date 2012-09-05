/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-service-middleware-server WorkerSupport.java 2012-7-17 12:47:48 l.xue.nong$$
 */
package cn.com.rebirth.service.middleware.server.support;

import static java.lang.String.format;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.rebirth.commons.component.AbstractLifecycleComponent;
import cn.com.rebirth.commons.exception.RebirthException;
import cn.com.rebirth.commons.settings.Settings;
import cn.com.rebirth.service.middleware.commons.Message;
import cn.com.rebirth.service.middleware.commons.MessageSubscriber;
import cn.com.rebirth.service.middleware.commons.Messages;
import cn.com.rebirth.service.middleware.commons.protocol.RmiRequest;
import cn.com.rebirth.service.middleware.commons.protocol.RmiResponse;
import cn.com.rebirth.service.middleware.server.BusinessWorker;
import cn.com.rebirth.service.middleware.server.ProviderService;
import cn.com.rebirth.service.middleware.server.message.RegisterServiceMessage;

import com.google.common.collect.Maps;

/**
 * The Class WorkerSupport.
 *
 * @author l.xue.nong
 */
public class WorkerSupport extends AbstractLifecycleComponent<WorkerSupport> implements MessageSubscriber,
		BusinessWorker {

	/**
	 * Instantiates a new worker support.
	 *
	 * @param settings the settings
	 */
	protected WorkerSupport(Settings settings) {
		super(settings);
		this.poolSize = componentSettings.getAsInt("provider.workers", 250);
	}

	/** The logger. */
	private final Logger logger = LoggerFactory.getLogger(WorkerSupport.class);

	/** The pool size. */
	private final int poolSize;

	/** The business executor. */
	private ExecutorService businessExecutor;

	/** The semaphore. */
	private Semaphore semaphore;

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
		RegisterServiceMessage rsMsg = (RegisterServiceMessage) msg;
		ProviderService service = rsMsg.getContent();
		services.put(service.getKey(), service);

	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.service.middleware.server.BusinessWorker#work(cn.com.rebirth.service.middleware.commons.protocol.RmiRequest, org.jboss.netty.channel.Channel)
	 */
	@Override
	public void work(final RmiRequest req, final Channel channel) {
		final String key = req.getKey();
		final ProviderService service = services.get(key);
		if (null == service) {
			handleServiceNotFound(req, channel);
			return;
		}
		if (!settings.getAsBoolean("development.model", false)) {
			if (isReqTimeout(req.getTimestamp(), req.getTimeout(), service.getTimeout())) {
				handleTimeout(req, channel);
				return;
			}
		}

		if (!semaphore.tryAcquire()) {
			handleOverflow(req, channel);
			return;
		}

		businessExecutor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					Object serviceObj = service.getServiceObject();
					Method serviceMtd = service.getServiceMethod();
					Serializable returnObj = (Serializable) serviceMtd.invoke(serviceObj, (Object[]) req.getArgs());
					handleNormal(returnObj, req, channel);
				} catch (Throwable t) {
					handleThrowable(t.getCause().getCause().getCause(), req, channel);
				} finally {
					semaphore.release();
				}//try

			}

		});

	}

	/**
	 * Handle service not found.
	 *
	 * @param req the req
	 * @param channel the channel
	 */
	private void handleServiceNotFound(RmiRequest req, Channel channel) {
		RmiResponse respEvt = new RmiResponse(req, RmiResponse.RESULT_CODE_FAILED_SERVICE_NOT_FOUND);
		channel.write(respEvt);
	}

	/**
	 * Checks if is req timeout.
	 *
	 * @param reqTimestamp the req timestamp
	 * @param reqTimeout the req timeout
	 * @param proTimeout the pro timeout
	 * @return true, if is req timeout
	 */
	private boolean isReqTimeout(long reqTimestamp, long reqTimeout, long proTimeout) {
		final long nowTimestamp = System.currentTimeMillis();
		return nowTimestamp - reqTimestamp > Math.min(reqTimeout, proTimeout);
	}

	/**
	 * Handle timeout.
	 *
	 * @param req the req
	 * @param channel the channel
	 */
	private void handleTimeout(RmiRequest req, Channel channel) {
		RmiResponse respEvt = new RmiResponse(req, RmiResponse.RESULT_CODE_FAILED_TIMEOUT);
		channel.write(respEvt);
	}

	/**
	 * Handle normal.
	 *
	 * @param returnObj the return obj
	 * @param req the req
	 * @param channel the channel
	 */
	private void handleNormal(Serializable returnObj, RmiRequest req, Channel channel) {
		RmiResponse respEvt = new RmiResponse(req, RmiResponse.RESULT_CODE_SUCCESSED_RETURN, returnObj);
		channel.write(respEvt);
	}

	/**
	 * Handle throwable.
	 *
	 * @param returnObj the return obj
	 * @param req the req
	 * @param channel the channel
	 */
	private void handleThrowable(Serializable returnObj, RmiRequest req, Channel channel) {
		RmiResponse respEvt = new RmiResponse(req, RmiResponse.RESULT_CODE_SUCCESSED_THROWABLE, returnObj);
		channel.write(respEvt);
	}

	/**
	 * Handle overflow.
	 *
	 * @param req the req
	 * @param channel the channel
	 */
	private void handleOverflow(RmiRequest req, Channel channel) {
		RmiResponse respEvt = new RmiResponse(req, RmiResponse.RESULT_CODE_FAILED_BIZ_THREAD_POOL_OVERFLOW);
		channel.write(respEvt);
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStart()
	 */
	@Override
	protected void doStart() throws RebirthException {
		Messages.register(this, RegisterServiceMessage.class);

		// 初始化服务列表
		services = Maps.newConcurrentMap();

		// 执行线程池
		businessExecutor = Executors.newCachedThreadPool(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "worker");
			}

		});

		// 初始化信号量
		semaphore = new Semaphore(poolSize);

		if (logger.isInfoEnabled()) {
			logger.info(format("worker init thread_pool size:%s", poolSize));
		}
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStop()
	 */
	@Override
	protected void doStop() throws RebirthException {
		if (null != businessExecutor) {
			businessExecutor.shutdown();
		}
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doClose()
	 */
	@Override
	protected void doClose() throws RebirthException {

	}

}
