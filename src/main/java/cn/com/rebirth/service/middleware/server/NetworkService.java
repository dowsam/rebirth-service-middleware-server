package cn.com.rebirth.service.middleware.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import cn.com.rebirth.commons.component.AbstractComponent;
import cn.com.rebirth.commons.settings.Settings;
import cn.com.rebirth.commons.unit.ByteSizeUnit;
import cn.com.rebirth.commons.unit.ByteSizeValue;
import cn.com.rebirth.commons.unit.TimeValue;
import cn.com.rebirth.commons.utils.NetworkUtils;

public class NetworkService extends AbstractComponent {

	/** The Constant LOCAL. */
	public static final String LOCAL = "#local#";

	/** The Constant GLOBAL_NETWORK_HOST_SETTING. */
	private static final String GLOBAL_NETWORK_HOST_SETTING = "network.host";

	/** The Constant GLOBAL_NETWORK_BINDHOST_SETTING. */
	private static final String GLOBAL_NETWORK_BINDHOST_SETTING = "network.bind_host";

	/** The Constant GLOBAL_NETWORK_PUBLISHHOST_SETTING. */
	private static final String GLOBAL_NETWORK_PUBLISHHOST_SETTING = "network.publish_host";

	/**
	 * The Class TcpSettings.
	 *
	 * @author l.xue.nong
	 */
	public static final class TcpSettings {

		/** The Constant TCP_NO_DELAY. */
		public static final String TCP_NO_DELAY = "network.tcp.no_delay";

		/** The Constant TCP_KEEP_ALIVE. */
		public static final String TCP_KEEP_ALIVE = "network.tcp.keep_alive";

		/** The Constant TCP_REUSE_ADDRESS. */
		public static final String TCP_REUSE_ADDRESS = "network.tcp.reuse_address";

		/** The Constant TCP_SEND_BUFFER_SIZE. */
		public static final String TCP_SEND_BUFFER_SIZE = "network.tcp.send_buffer_size";

		/** The Constant TCP_RECEIVE_BUFFER_SIZE. */
		public static final String TCP_RECEIVE_BUFFER_SIZE = "network.tcp.receive_buffer_size";

		/** The Constant TCP_BLOCKING. */
		public static final String TCP_BLOCKING = "network.tcp.blocking";

		/** The Constant TCP_BLOCKING_SERVER. */
		public static final String TCP_BLOCKING_SERVER = "network.tcp.blocking_server";

		/** The Constant TCP_BLOCKING_CLIENT. */
		public static final String TCP_BLOCKING_CLIENT = "network.tcp.blocking_client";

		/** The Constant TCP_CONNECT_TIMEOUT. */
		public static final String TCP_CONNECT_TIMEOUT = "network.tcp.connect_timeout";

		/** The Constant TCP_DEFAULT_SEND_BUFFER_SIZE. */
		public static final ByteSizeValue TCP_DEFAULT_SEND_BUFFER_SIZE = new ByteSizeValue(32, ByteSizeUnit.KB);

		/** The Constant TCP_DEFAULT_RECEIVE_BUFFER_SIZE. */
		public static final ByteSizeValue TCP_DEFAULT_RECEIVE_BUFFER_SIZE = new ByteSizeValue(32, ByteSizeUnit.KB);

		/** The Constant TCP_DEFAULT_CONNECT_TIMEOUT. */
		public static final TimeValue TCP_DEFAULT_CONNECT_TIMEOUT = new TimeValue(30, TimeUnit.SECONDS);
	}

	/**
	 * The Interface CustomNameResolver.
	 *
	 * @author l.xue.nong
	 */
	public static interface CustomNameResolver {

		/**
		 * Resolve default.
		 *
		 * @return the inet address
		 */
		InetAddress resolveDefault();

		/**
		 * Resolve if possible.
		 *
		 * @param value the value
		 * @return the inet address
		 */
		InetAddress resolveIfPossible(String value);
	}

	/** The custom name resolvers. */
	private final List<CustomNameResolver> customNameResolvers = new CopyOnWriteArrayList<CustomNameResolver>();

	/**
	 * Instantiates a new network service.
	 *
	 * @param settings the settings
	 */
	public NetworkService(Settings settings) {
		super(settings);
	}

	/**
	 * Adds the custom name resolver.
	 *
	 * @param customNameResolver the custom name resolver
	 */
	public void addCustomNameResolver(CustomNameResolver customNameResolver) {
		customNameResolvers.add(customNameResolver);
	}

	/**
	 * Resolve bind host address.
	 *
	 * @param bindHost the bind host
	 * @return the inet address
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public InetAddress resolveBindHostAddress(String bindHost) throws IOException {
		return resolveBindHostAddress(bindHost, null);
	}

	/**
	 * Resolve bind host address.
	 *
	 * @param bindHost the bind host
	 * @param defaultValue2 the default value2
	 * @return the inet address
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public InetAddress resolveBindHostAddress(String bindHost, String defaultValue2) throws IOException {
		return resolveInetAddress(bindHost,
				settings.get(GLOBAL_NETWORK_BINDHOST_SETTING, settings.get(GLOBAL_NETWORK_HOST_SETTING)), defaultValue2);
	}

	/**
	 * Resolve publish host address.
	 *
	 * @param publishHost the publish host
	 * @return the inet address
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public InetAddress resolvePublishHostAddress(String publishHost) throws IOException {
		InetAddress address = resolvePublishHostAddress(publishHost, null);

		if (address == null || address.isAnyLocalAddress()) {
			address = NetworkUtils.getFirstNonLoopbackAddress(NetworkUtils.StackType.IPv4);
			if (address == null) {
				address = NetworkUtils.getFirstNonLoopbackAddress(NetworkUtils.getIpStackType());
				if (address == null) {
					address = NetworkUtils.getLocalAddress();
					if (address == null) {
						return NetworkUtils.getLocalhost(NetworkUtils.StackType.IPv4);
					}
				}
			}
		}
		return address;
	}

	/**
	 * Resolve publish host address.
	 *
	 * @param publishHost the publish host
	 * @param defaultValue2 the default value2
	 * @return the inet address
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public InetAddress resolvePublishHostAddress(String publishHost, String defaultValue2) throws IOException {
		return resolveInetAddress(publishHost,
				settings.get(GLOBAL_NETWORK_PUBLISHHOST_SETTING, settings.get(GLOBAL_NETWORK_HOST_SETTING)),
				defaultValue2);
	}

	/**
	 * Resolve inet address.
	 *
	 * @param host the host
	 * @param defaultValue1 the default value1
	 * @param defaultValue2 the default value2
	 * @return the inet address
	 * @throws UnknownHostException the unknown host exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public InetAddress resolveInetAddress(String host, String defaultValue1, String defaultValue2)
			throws UnknownHostException, IOException {
		if (host == null) {
			host = defaultValue1;
		}
		if (host == null) {
			host = defaultValue2;
		}
		if (host == null) {
			for (CustomNameResolver customNameResolver : customNameResolvers) {
				InetAddress inetAddress = customNameResolver.resolveDefault();
				if (inetAddress != null) {
					return inetAddress;
				}
			}
			return null;
		}
		String origHost = host;
		if ((host.startsWith("#") && host.endsWith("#")) || (host.startsWith("_") && host.endsWith("_"))) {
			host = host.substring(1, host.length() - 1);

			for (CustomNameResolver customNameResolver : customNameResolvers) {
				InetAddress inetAddress = customNameResolver.resolveIfPossible(host);
				if (inetAddress != null) {
					return inetAddress;
				}
			}

			if (host.equals("local")) {
				return NetworkUtils.getLocalAddress();
			} else if (host.startsWith("non_loopback")) {
				if (host.toLowerCase().endsWith(":ipv4")) {
					return NetworkUtils.getFirstNonLoopbackAddress(NetworkUtils.StackType.IPv4);
				} else if (host.toLowerCase().endsWith(":ipv6")) {
					return NetworkUtils.getFirstNonLoopbackAddress(NetworkUtils.StackType.IPv6);
				} else {
					return NetworkUtils.getFirstNonLoopbackAddress(NetworkUtils.getIpStackType());
				}
			} else {
				NetworkUtils.StackType stackType = NetworkUtils.getIpStackType();
				if (host.toLowerCase().endsWith(":ipv4")) {
					stackType = NetworkUtils.StackType.IPv4;
					host = host.substring(0, host.length() - 5);
				} else if (host.toLowerCase().endsWith(":ipv6")) {
					stackType = NetworkUtils.StackType.IPv6;
					host = host.substring(0, host.length() - 5);
				}
				Collection<NetworkInterface> allInterfs = NetworkUtils.getAllAvailableInterfaces();
				for (NetworkInterface ni : allInterfs) {
					if (!ni.isUp() || ni.isLoopback()) {
						continue;
					}
					if (host.equals(ni.getName()) || host.equals(ni.getDisplayName())) {
						return NetworkUtils.getFirstNonLoopbackAddress(ni, stackType);
					}
				}
			}
			throw new IOException("Failed to find network interface for [" + origHost + "]");
		}
		return InetAddress.getByName(host);
	}
}
