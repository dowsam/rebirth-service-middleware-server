/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-service-middleware-server ServerSupport.java 2012-7-17 12:53:14 l.xue.nong$$
 */
package cn.com.rebirth.service.middleware.server.support;

import static java.lang.String.format;
import static java.util.concurrent.Executors.newCachedThreadPool;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.rebirth.commons.PortsRange;
import cn.com.rebirth.commons.component.AbstractLifecycleComponent;
import cn.com.rebirth.commons.exception.RebirthException;
import cn.com.rebirth.commons.settings.Settings;
import cn.com.rebirth.service.middleware.commons.protocol.RmiRequest;
import cn.com.rebirth.service.middleware.commons.protocol.coder.ProtocolDecoder;
import cn.com.rebirth.service.middleware.commons.protocol.coder.ProtocolEncoder;
import cn.com.rebirth.service.middleware.commons.protocol.coder.RmiDecoder;
import cn.com.rebirth.service.middleware.commons.protocol.coder.RmiEncoder;
import cn.com.rebirth.service.middleware.commons.serializer.SerializerFactory;
import cn.com.rebirth.service.middleware.server.BusinessWorker;
import cn.com.rebirth.service.middleware.server.NetworkService;

/**
 * The Class ServerSupport.
 *
 * @author l.xue.nong
 */
public class ServerSupport extends AbstractLifecycleComponent<ServerSupport> {

	/** The serializer factory. */
	private final SerializerFactory serializerFactory;
	/** The port. */
	private final String port;

	/** The bind host. */
	private final String bindHost;
	private final NetworkService networkService;
	private final String publishHost;

	/**
	 * Instantiates a new server support.
	 *
	 * @param settings the settings
	 */
	protected ServerSupport(Settings settings, NetworkService networkService) {
		super(settings);
		this.networkService = networkService;
		this.serializerFactory = new SerializerFactory(settings);
		this.port = componentSettings.get("port", settings.get("http.port", "9700-9900"));
		this.bindHost = componentSettings.get("bind_host", settings.get("http.bind_host", settings.get("http.host")));
		this.publishHost = componentSettings.get("publish_host",
				settings.get("http.publish_host", settings.get("http.host")));
	}

	/** The logger. */
	private final Logger logger = LoggerFactory.getLogger(ServerSupport.class);

	/** The address. */
	private InetSocketAddress publishAddress;

	/** The business worker. */
	private BusinessWorker businessWorker;

	/** The bootstrap. */
	private ServerBootstrap bootstrap;

	/** The channel group. */
	private volatile Channel serverChannel;

	/** The channel pipeline factory. */
	private ChannelPipelineFactory channelPipelineFactory = new ChannelPipelineFactory() {

		@Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeline = Channels.pipeline();
			pipeline.addLast("protocol-decoder", new ProtocolDecoder());
			pipeline.addLast("rmi-decoder", new RmiDecoder(serializerFactory));
			pipeline.addLast("business-handler", businessHandler);
			pipeline.addLast("protocol-encoder", new ProtocolEncoder());
			pipeline.addLast("rmi-encoder", new RmiEncoder(serializerFactory));
			return pipeline;
		}

	};

	/** The business handler. */
	private SimpleChannelUpstreamHandler businessHandler = new SimpleChannelUpstreamHandler() {

		@Override
		public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			super.channelConnected(ctx, e);
			if (logger.isInfoEnabled()) {
				logger.info(format("client:%s was connected.", ctx.getChannel().getRemoteAddress()));
			}
		}

		@Override
		public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			super.channelDisconnected(ctx, e);
			if (logger.isInfoEnabled()) {
				logger.info(format("client:%s was disconnected.", ctx.getChannel().getRemoteAddress()));
			}
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

			if (null == e.getMessage() || !(e.getMessage() instanceof RmiRequest)) {
				super.messageReceived(ctx, e);
			}

			final RmiRequest req = (RmiRequest) e.getMessage();
			businessWorker.work(req, ctx.getChannel());
		}

	};

	/**
	 * Sets the business worker.
	 *
	 * @param businessWorker the new business worker
	 */
	public void setBusinessWorker(BusinessWorker businessWorker) {
		this.businessWorker = businessWorker;
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStart()
	 */
	@Override
	protected void doStart() throws RebirthException {
		bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(newCachedThreadPool(), newCachedThreadPool()));
		bootstrap.setPipelineFactory(channelPipelineFactory);
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);
		InetAddress hostAddressX;
		try {
			hostAddressX = networkService.resolveBindHostAddress(bindHost);
		} catch (IOException e) {
			throw new RebirthException("Failed to resolve host [" + bindHost + "]", e);
		}
		final InetAddress hostAddress = hostAddressX;
		PortsRange portsRange = new PortsRange(port);
		final AtomicReference<Exception> lastException = new AtomicReference<Exception>();
		boolean success = portsRange.iterate(new PortsRange.PortCallback() {
			@Override
			public boolean onPortNumber(int portNumber) {
				try {
					serverChannel = (bootstrap.bind(new InetSocketAddress(hostAddress, portNumber)));
				} catch (Exception e) {
					lastException.set(e);
					return false;
				}
				return true;
			}
		});
		if (!success) {
			throw new RebirthException("Failed to bind to [" + port + "]", lastException.get());
		}
		InetSocketAddress boundAddress = (InetSocketAddress) serverChannel.getLocalAddress();
		InetSocketAddress publishAddress;
		try {
			publishAddress = new InetSocketAddress(networkService.resolvePublishHostAddress(publishHost),
					boundAddress.getPort());
		} catch (Exception e) {
			throw new RebirthException("Failed to resolve publish address", e);
		}
		this.publishAddress = publishAddress;
		if (logger.isInfoEnabled()) {
			logger.info(format("server was started at %s", publishAddress));
		}
	}

	public InetSocketAddress getPublishAddress() {
		return publishAddress;
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doStop()
	 */
	@Override
	protected void doStop() throws RebirthException {
		if (serverChannel != null) {
			serverChannel.close().awaitUninterruptibly();
			serverChannel = null;
		}
		if (null != bootstrap) {
			bootstrap.releaseExternalResources();
		}
		if (logger.isInfoEnabled()) {
			logger.info("server was shutdown.");
		}
	}

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.component.AbstractLifecycleComponent#doClose()
	 */
	@Override
	protected void doClose() throws RebirthException {

	}

}
