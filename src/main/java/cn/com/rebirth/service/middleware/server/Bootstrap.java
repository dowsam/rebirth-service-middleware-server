/*
 * Copyright (c) 2005-2012 www.summall.com.cn All rights reserved
 * Info:summall-search-server Bootstrap.java 2012-3-29 17:43:21 l.xue.nong$$
 */

package cn.com.rebirth.service.middleware.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.rebirth.commons.RebirthContainer;
import cn.com.rebirth.commons.component.LifecycleListener;
import cn.com.rebirth.commons.exception.ExceptionsHelper;
import cn.com.rebirth.commons.jna.Natives;
import cn.com.rebirth.commons.settings.ImmutableSettings;
import cn.com.rebirth.commons.settings.Settings;
import cn.com.rebirth.commons.utils.NonceUtils;

import com.google.common.collect.Lists;

/**
 * The Class Bootstrap.
 *
 * @author l.xue.nong
 */
public final class Bootstrap {

	/** The logger. */
	private static Logger logger = LoggerFactory.getLogger(Bootstrap.class);

	/** The keep alive thread. */
	private static volatile Thread keepAliveThread;

	/** The keep alive latch. */
	private static volatile CountDownLatch keepAliveLatch;

	/** The bootstrap. */
	private static Bootstrap bootstrap;
	private ProviderProxyFactory providerProxyFactory;

	/**
	 * Setup.
	 *
	 * @param addShutdownHook the add shutdown hook
	 * @param tuple the tuple
	 * @throws Exception the exception
	 */
	private void setup(boolean addShutdownHook, Settings settings) throws Exception {
		if (settings.getAsBoolean("bootstrap.mlockall", false)) {
			Natives.tryMlockall();
		}
		providerProxyFactory = new ProviderProxyFactory(settings);
		if (addShutdownHook) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					providerProxyFactory.close();
				}
			});
		}
		//Listener Container
		Class<?> listenerClass = null;
		Constructor<?> constructor = null;
		Object targetObject = null;
		try {
			listenerClass = settings.getAsClass("container.className", null);
		} catch (Exception e) {
		}
		if (listenerClass != null) {
			if (listenerClass != null) {
				try {
					constructor = listenerClass.getDeclaredConstructor(Settings.class);
					targetObject = constructor.newInstance(settings);
				} catch (Exception e) {
					constructor = listenerClass.getDeclaredConstructor();
					targetObject = constructor.newInstance();
				}
			}
		}
		if (targetObject != null && targetObject instanceof LifecycleListener) {
			LifecycleListener lifecycleListener = (LifecycleListener) targetObject;
			providerProxyFactory.addLifecycleListener((lifecycleListener));
		}
	}

	class LifecycleListenerDecoration implements LifecycleListener, Runnable {
		private final LifecycleListener lifecycleListener;
		private final Thread thread;
		private final List<Method> methods = Lists.newArrayList();

		public LifecycleListenerDecoration(LifecycleListener lifecycleListener) {
			super();
			this.lifecycleListener = lifecycleListener;
			thread = new Thread(this, "Init Other Containers Thread");
		}

		@Override
		public void beforebirth() {
			thread.start();
			synchronized (methods) {
				try {
					methods.add(LifecycleListener.class.getDeclaredMethod("beforebirth"));
					methods.notifyAll();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void afterStart() {
			synchronized (methods) {
				try {
					methods.add(LifecycleListener.class.getDeclaredMethod("afterStart"));
					methods.notifyAll();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void beforeStop() {
			synchronized (methods) {
				try {
					methods.add(LifecycleListener.class.getDeclaredMethod("beforeStop"));
					methods.notifyAll();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}
		}

		@SuppressWarnings("deprecation")
		@Override
		public void afterStop() {
			synchronized (methods) {
				try {
					methods.add(LifecycleListener.class.getDeclaredMethod("afterStop"));
					methods.notifyAll();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}
			thread.stop();
		}

		@Override
		public void beforeClose() {
			synchronized (methods) {
				try {
					methods.add(LifecycleListener.class.getDeclaredMethod("beforeClose"));
					methods.notifyAll();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void afterClose() {
			synchronized (methods) {
				try {
					methods.add(LifecycleListener.class.getDeclaredMethod("afterClose"));
					methods.notifyAll();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void run() {
			synchronized (methods) {
				while (methods.isEmpty()) {
					try {
						methods.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Method method = methods.remove(0);
				try {
					method.invoke(lifecycleListener);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Initial settings.
	 *
	 * @return the tuple
	 */
	private static Settings initialSettings() {
		ImmutableSettings.Builder settingsBuilder = ImmutableSettings.settingsBuilder()
				.put(ImmutableSettings.Builder.EMPTY_SETTINGS)
				.putProperties("rebirth.service.middleware.", System.getProperties()).replacePropertyPlaceholders();

		settingsBuilder.loadFromClasspath("rebirth-service-middleware-server.properties");
		settingsBuilder.putProperties("rebirth.service.middleware.", System.getProperties())
				.putProperties("es.", System.getProperties()).replacePropertyPlaceholders();
		if (settingsBuilder.get("name") == null) {
			String name = System.getProperty("name");
			if (name == null || name.isEmpty()) {
				name = settingsBuilder.get("node.name");
				if (name == null || name.isEmpty()) {
					name = "Rebirth-Service-Midddleware-Node-" + NonceUtils.randomInt();
				}
			}

			if (name != null) {
				settingsBuilder.put("name", name);
			}
		}
		Settings v1 = settingsBuilder.build();
		settingsBuilder = ImmutableSettings.settingsBuilder().put(v1);
		return settingsBuilder.build();
	}

	/**
	 * Inits the.
	 *
	 * @param args the args
	 * @throws Exception the exception
	 */
	public void init(String[] args) throws Exception {
		Settings tuple = initialSettings();
		setup(true, tuple);
	}

	/**
	 * Start.
	 */
	public void start() {
		providerProxyFactory.start();
	}

	/**
	 * Stop.
	 */
	public void stop() {
		providerProxyFactory.stop();
	}

	/**
	 * Destroy.
	 */
	public void destroy() {
		providerProxyFactory.close();
	}

	/**
	 * Close.
	 *
	 * @param args the args
	 */
	public static void close(String[] args) {
		bootstrap.destroy();
		keepAliveLatch.countDown();
		RebirthContainer.getInstance().stop();
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		RebirthContainer.getInstance().start();
		bootstrap = new Bootstrap();

		Settings settings = null;
		try {
			settings = initialSettings();
		} catch (Exception e) {
			String errorMessage = buildErrorMessage("Setup", e);
			System.err.println(errorMessage);
			System.err.flush();
			System.exit(3);
		}

		String stage = "Initialization";
		try {
			bootstrap.setup(true, settings);

			stage = "Startup";
			bootstrap.start();

			keepAliveLatch = new CountDownLatch(1);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					keepAliveLatch.countDown();
				}
			});

			keepAliveThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						keepAliveLatch.await();
					} catch (InterruptedException e) {
						// bail out
					}
				}
			}, "Rebirth-Service-Middleware[keepAlive]");
			keepAliveThread.setDaemon(false);
			keepAliveThread.start();
		} catch (Throwable e) {
			e.printStackTrace();
			String errorMessage = buildErrorMessage(stage, e);
			System.err.println(errorMessage);
			System.err.flush();
			if (logger.isDebugEnabled()) {
				logger.debug("Exception", e);
			}
			System.exit(3);
		}
	}

	/**
	 * Builds the error message.
	 *
	 * @param stage the stage
	 * @param e the e
	 * @return the string
	 */
	private static String buildErrorMessage(String stage, Throwable e) {
		StringBuilder errorMessage = new StringBuilder("{").append(
				new RebirthServiceMiddlewareServerVersion().getModuleVersion()).append("}: ");
		errorMessage.append(stage).append(" Failed ...\n");
		errorMessage.append("- ").append(ExceptionsHelper.detailedMessage(e, true, 0));
		return errorMessage.toString();
	}
}
