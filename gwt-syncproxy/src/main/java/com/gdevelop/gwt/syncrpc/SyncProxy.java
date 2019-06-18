/*
 * Copyright www.gdevelop.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.gdevelop.gwt.syncrpc;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gdevelop.gwt.syncrpc.exception.SyncProxyException;
import com.gdevelop.gwt.syncrpc.exception.SyncProxyException.InfoType;
import com.google.gwt.user.client.rpc.HasRpcToken;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * Sync Proxy for GWT RemoteService Usage: MyServiceInterface myService =
 * newProxyInstance(MyServiceInterface.class, "http://localhost:8888/myapp/",
 * "myServiceServlet", policyName); where policyName is the file name (with
 * gwt.rpc extenstion) generated by GWT RPC backend
 *
 * Or MyServiceInterface myService = newProxyInstance(MyServiceInterface.class,
 * "http://localhost:8888/myapp/", "myServiceServlet"); In this case, the
 * SyncProxy search for the appropriate policyName file in the system classpath
 *
 * If not specified, SyncProxy uses a <em>default</em> {@link CookieManager} to
 * manage client-server communication session
 *
 * To perform multi-session: CookieManager cookieManager =
 * LoginUtils.loginAppEngine(...); MyServiceInterface myService =
 * newProxyInstance(MyServiceInterface.class, "http://localhost:8888/myapp/",
 * "myServiceServlet", cookieManager);
 *
 * @see com.gdevelop.gwt.syncrpc.test.poj.ProfileServiceTest example
 */
public class SyncProxy {

	/**
	 * Similar action to Gwt.create(). This method assumes your service is
	 * annotated with {@link RemoteServiceRelativePath} and that you have
	 * appropriately set the base url: {@link #setBaseURL(String)}. See
	 * {@link #suppressRelativePathWarning(boolean)} in the event your service
	 * is not annotated with {@link RemoteServiceRelativePath}.
	 *
	 * @since 0.5
	 * @param serviceIntf
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <ServiceIntfAsync, ServiceIntf extends RemoteService> ServiceIntfAsync create(
			Class<ServiceIntf> serviceIntf) {
		logger.config("Create service: " + serviceIntf.getName());

		Class<ServiceIntfAsync> asyncServiceIntf;

		try {
			asyncServiceIntf = (Class<ServiceIntfAsync>) Class
					.forName(serviceIntf.getName() + ASYNC_POSTFIX);
		} catch (ClassNotFoundException e) {
			throw new SyncProxyException(serviceIntf, InfoType.SERVICE_BASE);
		}

		logger.config("Creating Async Service: " + asyncServiceIntf.getName());

		return createProxy(asyncServiceIntf, new ProxySettings());
	}

	/**
	 * Creates the actual Sync and Async ProxyInterface for the service with the
	 * specified options.This method assumes your service is annotated with
	 * {@link RemoteServiceRelativePath} and that you have appropriately set the
	 * base url: {@link #setBaseURL(String)}. See
	 * {@link #suppressRelativePathWarning(boolean)} in the event your service
	 * is not annotated with {@link RemoteServiceRelativePath}.
	 *
	 * @since 0.5
	 * @param serviceIntf
	 *            the service to create a proxy for
	 * @param moduleBaseUrl
	 *            the server's base url
	 * @param remoteServiceRelativePath
	 *            the relative path for this service
	 * @param policyName
	 *            Policy name (*.gwt.rpc) generated by GWT RPC backend
	 * @param cookieManager
	 *            the cookie manager
	 * @param waitForInvocation
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <ServiceIntf> ServiceIntf createProxy(
			Class<ServiceIntf> serviceIntf, ProxySettings settings) {
		logger.config("Setting up Proxy: " + serviceIntf.getName());
		defaultUnsetSettings(serviceIntf, settings);
		return (ServiceIntf) Proxy.newProxyInstance(
				SyncProxy.class.getClassLoader(), new Class[] { serviceIntf,
					ServiceDefTarget.class, HasRpcToken.class,
					SerializationStreamFactory.class,
						HasProxySettings.class },
				new RemoteServiceInvocationHandler(settings));
	}

	/**
	 * Similar action to Gwt.create() except that this creates a sync'ed proxy.
	 * This method assumes your service is annotated with
	 * {@link RemoteServiceRelativePath} and that you have appropriately set the
	 * base url: {@link #setBaseURL(String)}. See
	 * {@link #suppressRelativePathWarning(boolean)} in the event your service
	 * is not annotated with {@link RemoteServiceRelativePath}.
	 *
	 * @since 0.5
	 * @param serviceIntf
	 * @return
	 */
	public static <ServiceIntf extends RemoteService> ServiceIntf createSync(
			Class<ServiceIntf> serviceIntf) {
		logger.config("Create Sync Service: " + serviceIntf.getName());
		return createProxy(serviceIntf, new ProxySettings());
	}

	/**
	 * Sets default values to the settings parameters that are not yet set
	 *
	 * @param settings
	 */
	protected static <ServiceIntf> ProxySettings defaultUnsetSettings(
			Class<ServiceIntf> serviceIntf, ProxySettings settings) {
		logger.info("Updating Default Settings for Unset Values");
		if (settings.getModuleBaseUrl() == null) {
			if (moduleBaseURL == null) {
				throw new SyncProxyException(serviceIntf,
						InfoType.MODULE_BASE_URL);
			}
			logger.config("Setting server base to module: " + moduleBaseURL);
			settings.setModuleBaseUrl(moduleBaseURL);
		}
		logger.finer("Server Base Url: " + settings.getModuleBaseUrl());
		if (settings.getRemoteServiceRelativePath() == null) {
			logger.config("Setting Service Relative Path by Annotation");
			settings.setRemoteServiceRelativePath(getRemoteServiceRelativePathFromAnnotation(serviceIntf));
		}
		logger.finer("Remote Service Relative path: "
				+ settings.getRemoteServiceRelativePath());
		if (settings.getPolicyName() == null) {
			logger.config("Setting Policy Name by Map");
			settings.setPolicyName(POLICY_MAP.get(serviceIntf.getName()));
		}
		if (settings.getPolicyName() == null) {
			throw new SyncProxyException(serviceIntf,
					InfoType.POLICY_NAME_MISSING);
		}
		logger.finer("Service Policy name: " + settings.getPolicyName());
		if (settings.getCookieManager() == null) {
			logger.config("Setting Cookie Manager to Default");
			settings.setCookieManager(DEFAULT_COOKIE_MANAGER);
		}
		return settings;
	}

	public static Class<?>[] getLoggerClasses() {
		return spClazzes;
	}

	protected static Level getLoggingLevel() {
		return level;
	}

	/**
	 * Attempts to ascertain the remote service's relative path by retrieving
	 * the {@link RemoteServiceRelativePath} annotation value from the base
	 * interface. This method can take either the base interface or the Async
	 * interface class.
	 *
	 * @since 0.5
	 * @param serviceIntf
	 *            either the base or Async interface class
	 * @return
	 */
	protected static <ServiceIntf> String getRemoteServiceRelativePathFromAnnotation(
			Class<ServiceIntf> serviceIntf) {
		Class<?> baseServiceIntf = serviceIntf;
		if (serviceIntf.getName().endsWith(ASYNC_POSTFIX)) {
			// Try determine remoteServiceRelativePath from the 'sync' version
			// of the Async one
			String className = serviceIntf.getName();
			try {
				baseServiceIntf = Class.forName(className.substring(0,
						className.length() - ASYNC_POSTFIX.length()));
			} catch (ClassNotFoundException e) {
				throw new SyncProxyException(baseServiceIntf,
						InfoType.SERVICE_BASE, e);
			}
		}
		if (baseServiceIntf.getAnnotation(RemoteServiceRelativePath.class) == null) {
			if (isSuppressRelativePathWarning()) {
				logger.info("Suppressed warning for lack of RemoteServiceRelativePath annotation on service: "
						+ baseServiceIntf);
				return "";
			}
			throw new SyncProxyException(baseServiceIntf,
					InfoType.REMOTE_SERVICE_RELATIVE_PATH);
		}
		return baseServiceIntf.getAnnotation(RemoteServiceRelativePath.class)
				.value();
	}

	public static boolean isSuppressRelativePathWarning() {
		return suppressRelativePathWarning;
	}

	/**
	 *
	 * @deprecated since 0.5, use {@link #create(Class)} or
	 *             {@link #createSync(Class)}
	 */
	@Deprecated
	public static <ServiceIntf> ServiceIntf newProxyInstance(
			Class<ServiceIntf> serviceIntf, String moduleBaseURL) {
		return newProxyInstance(serviceIntf, moduleBaseURL,
				DEFAULT_COOKIE_MANAGER);
	}

	/**
	 *
	 * @deprecated since 0.5, {@link #createProxy(Class, ProxySettings)} with
	 *             {@link ProxySettings#setWaitForInvocation(boolean)}
	 */
	@Deprecated
	public static <ServiceIntf> ServiceIntf newProxyInstance(
			Class<ServiceIntf> serviceIntf, String moduleBaseURL,
			boolean waitForInvocation) {
		return newProxyInstance(serviceIntf, moduleBaseURL,
				DEFAULT_COOKIE_MANAGER, waitForInvocation);
	}

	/**
	 *
	 * @deprecated since 0.5, {@link #createProxy(Class, ProxySettings)} with
	 *             {@link ProxySettings#setCookieManager(CookieManager)}
	 */
	@Deprecated
	public static <ServiceIntf> ServiceIntf newProxyInstance(
			Class<ServiceIntf> serviceIntf, String moduleBaseURL,
			CookieManager cookieManager) {
		return newProxyInstance(serviceIntf, moduleBaseURL, cookieManager,
				false);
	}

	/**
	 *
	 * @deprecated since 0.5, {@link #createProxy(Class, ProxySettings)} with
	 *             {@link ProxySettings#setCookieManager(CookieManager)} and
	 *             {@link ProxySettings#setWaitForInvocation(boolean)}
	 */
	@Deprecated
	public static <ServiceIntf> ServiceIntf newProxyInstance(
			Class<ServiceIntf> serviceIntf, String moduleBaseURL,
			CookieManager cookieManager, boolean waitForInvocation) {
		RemoteServiceRelativePath relativePathAnn = serviceIntf
				.getAnnotation(RemoteServiceRelativePath.class);
		if (serviceIntf.getName().endsWith("Async")) {
			// Try determine remoteServiceRelativePath from the 'sync' version
			// of the Async one
			String className = serviceIntf.getName();
			try {
				Class<?> clazz = Class.forName(className.substring(0,
						className.length() - 5));
				relativePathAnn = clazz
						.getAnnotation(RemoteServiceRelativePath.class);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		if (relativePathAnn == null) {
			throw new RuntimeException(serviceIntf
					+ " does not has a RemoteServiceRelativePath annotation");
		}
		String remoteServiceRelativePath = relativePathAnn.value();
		return newProxyInstance(serviceIntf, moduleBaseURL,
				remoteServiceRelativePath,
				POLICY_MAP.get(serviceIntf.getName()), cookieManager,
				waitForInvocation);
	}

	/**
	 * Create a new Proxy for the specified service interface
	 * <code>serviceIntf</code>
	 *
	 * @param serviceIntf
	 *            The remote service interface
	 * @param moduleBaseURL
	 *            Base URL
	 * @param remoteServiceRelativePath
	 *            The remote service servlet relative path
	 * @return A new proxy object which implements the service interface
	 *         serviceIntf
	 *
	 * @deprecated since 0.5, {@link #createProxy(Class, ProxySettings)} with
	 *             {@link ProxySettings#setRemoteServiceRelativePath(String)}
	 */
	@Deprecated
	public static <ServiceIntf> ServiceIntf newProxyInstance(
			Class<ServiceIntf> serviceIntf, String moduleBaseURL,
			String remoteServiceRelativePath) {
		return newProxyInstance(serviceIntf, moduleBaseURL,
				remoteServiceRelativePath,
				POLICY_MAP.get(serviceIntf.getName()), DEFAULT_COOKIE_MANAGER);
	}

	/**
	 * Create a new Proxy for the specified service interface
	 * <code>serviceIntf</code>
	 *
	 * @param serviceIntf
	 *            The remote service interface
	 * @param moduleBaseURL
	 *            Base URL
	 * @param remoteServiceRelativePath
	 *            The remote service servlet relative path
	 * @return A new proxy object which implements the service interface
	 *         serviceIntf
	 * @deprecated since 0.5, {@link #createProxy(Class, ProxySettings)} with
	 *             {@link ProxySettings#setRemoteServiceRelativePath(String)}
	 *             and {@link ProxySettings#setWaitForInvocation(boolean)}
	 */
	@Deprecated
	public static <ServiceIntf> ServiceIntf newProxyInstance(
			Class<ServiceIntf> serviceIntf, String moduleBaseURL,
			String remoteServiceRelativePath, boolean waitForInvocation) {
		try {
			POLICY_MAP.putAll(RpcPolicyFinder
					.fetchSerializationPolicyName(moduleBaseURL,DEFAULT_COOKIE_MANAGER));
			// policyName = POLICY_MAP.get(serviceIntf.getName());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return newProxyInstance(serviceIntf, moduleBaseURL,
				remoteServiceRelativePath,
				POLICY_MAP.get(serviceIntf.getName()), DEFAULT_COOKIE_MANAGER,
				waitForInvocation);
	}

	/**
	 * Create a new Proxy for the specified service interface
	 * <code>serviceIntf</code>
	 *
	 * @param serviceIntf
	 *            The remote service interface
	 * @param moduleBaseURL
	 *            Base URL
	 * @param remoteServiceRelativePath
	 *            The remote service servlet relative path
	 * @param cookieManager
	 *            Used to perform session management such as login.
	 * @return A new proxy object which implements the service interface
	 *         serviceIntf
	 * @deprecated since 0.5, {@link #createProxy(Class, ProxySettings)} with
	 *             {@link ProxySettings#setRemoteServiceRelativePath(String)}
	 *             and {@link ProxySettings#setCookieManager(CookieManager)}
	 */
	@Deprecated
	public static <ServiceIntf> ServiceIntf newProxyInstance(
			Class<ServiceIntf> serviceIntf, String moduleBaseURL,
			String remoteServiceRelativePath, CookieManager cookieManager) {
		return newProxyInstance(serviceIntf, moduleBaseURL,
				remoteServiceRelativePath,
				POLICY_MAP.get(serviceIntf.getName()), cookieManager);
	}

	/**
	 * Create a new Proxy for the specified service interface
	 * <code>serviceIntf</code>
	 *
	 * @param serviceIntf
	 *            The remote service interface
	 * @param moduleBaseURL
	 *            Base URL
	 * @param remoteServiceRelativePath
	 *            The remote service servlet relative path
	 * @param policyName
	 *            Policy name (*.gwt.rpc) generated by GWT RPC backend
	 * @return A new proxy object which implements the service interface
	 *         serviceIntf
	 * @deprecated since 0.5, {@link #createProxy(Class, ProxySettings)} with
	 *             {@link ProxySettings#setRemoteServiceRelativePath(String)}
	 *             and {@link ProxySettings#setPolicyName(String)}
	 */
	@Deprecated
	public static <ServiceIntf> ServiceIntf newProxyInstance(
			Class<ServiceIntf> serviceIntf, String moduleBaseURL,
			String remoteServiceRelativePath, String policyName) {
		return newProxyInstance(serviceIntf, moduleBaseURL,
				remoteServiceRelativePath, policyName, DEFAULT_COOKIE_MANAGER);
	}

	/**
	 * @deprecated since 0.5, {@link #createProxy(Class, ProxySettings)} with
	 *             {@link ProxySettings#setRemoteServiceRelativePath(String)},
	 *             {@link ProxySettings#setCookieManager(CookieManager)},
	 *             {@link ProxySettings#policyName}
	 */
	@Deprecated
	public static <ServiceIntf> ServiceIntf newProxyInstance(
			Class<ServiceIntf> serviceIntf, String moduleBaseURL,
			String remoteServiceRelativePath, String policyName,
			CookieManager cookieManager) {
		return newProxyInstance(serviceIntf, moduleBaseURL,
				remoteServiceRelativePath, policyName, cookieManager, false);
	}

	/**
	 * Create a new Proxy for the specified <code>serviceIntf</code>
	 *
	 * @param serviceIntf
	 *            The remote service interface
	 * @param moduleBaseURL
	 *            Base URL
	 * @param remoteServiceRelativePath
	 *            The remote service servlet relative path
	 * @param policyName
	 *            Policy name (*.gwt.rpc) generated by GWT RPC backend
	 * @param cookieManager
	 *            Used to perform session management such as login.
	 * @param waitForInvocation
	 *            Used for Async RemoteService.
	 * @return A new proxy object which implements the service interface
	 *         serviceIntf
	 *
	 * @deprecated since 0.5, {@link #createProxy(Class, ProxySettings)} with
	 *             {@link ProxySettings#setRemoteServiceRelativePath(String)},
	 *             {@link ProxySettings#setCookieManager(CookieManager)},
	 *             {@link ProxySettings#policyName}, and
	 *             {@link ProxySettings#setWaitForInvocation(boolean)}
	 */
	@Deprecated
	public static <ServiceIntf> ServiceIntf newProxyInstance(
			Class<ServiceIntf> serviceIntf, String moduleBaseURL,
			String remoteServiceRelativePath, String policyName,
			CookieManager cookieManager, boolean waitForInvocation) {
		return newProxyInstance(serviceIntf, moduleBaseURL, moduleBaseURL,
				remoteServiceRelativePath, policyName, cookieManager,
				waitForInvocation);

	}

	/**
	 * @deprecated since 0.5, {@link #createProxy(Class, ProxySettings)} with
	 *             {@link ProxySettings#setRemoteServiceRelativePath(String)},
	 *             {@link ProxySettings#setCookieManager(CookieManager)},
	 *             {@link ProxySettings#policyName}, and
	 *             {@link ProxySettings#setWaitForInvocation(boolean)}
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public static <ServiceIntf> ServiceIntf newProxyInstance(
			Class<ServiceIntf> serviceIntf, String moduleBaseURL,
			String serverBaseUrl, String remoteServiceRelativePath,
			String policyName, CookieManager cookieManager,
			boolean waitForInvocation) {
		if (cookieManager == null) {
			cookieManager = DEFAULT_COOKIE_MANAGER;
		}

		if (policyName == null) {
			try {
				POLICY_MAP.putAll(RpcPolicyFinder
						.fetchSerializationPolicyName(moduleBaseURL, cookieManager));
				policyName = POLICY_MAP.get(serviceIntf.getName());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		ServiceIntf i = (ServiceIntf) Proxy.newProxyInstance(SyncProxy.class
				.getClassLoader(), new Class[] { serviceIntf,
				ServiceDefTarget.class, HasRpcToken.class,
				SerializationStreamFactory.class },
				new RemoteServiceInvocationHandler(serverBaseUrl,
						remoteServiceRelativePath, policyName, cookieManager,
						waitForInvocation));
		return i;
	}

	/**
	 * @since 0.6
	 * @param cookieManager
	 * 				in case of the server side checking cookie information to determine if valid request
	 * @throws SyncProxyException
	 *             if occurs during
	 *             {@link RpcPolicyFinder#fetchSerializationPolicyName(String)}
	 */
	protected static void populatePolicyMap(CookieManager cookieManager) throws SyncProxyException {
		logger.info("Populating Policy Map");
		try {
			POLICY_MAP.putAll(RpcPolicyFinder
					.fetchSerializationPolicyName(moduleBaseURL,cookieManager));
		} catch (Exception e) {
			throw new SyncProxyException(InfoType.POLICY_NAME_POPULATION, e);
		}
	}

	/**
	 * @since 0.5
	 * @param baseUrl
	 *            if null, existing {@link #POLICY_MAP} is cleared and
	 *            repopulated from ClassPath (
	 *            {@link RpcPolicyFinder#searchPolicyFileInClassPath()}). If
	 *            Non-Null, {@link #POLICY_MAP} is appended with new population
	 * @throws SyncProxyException
	 *             if occurs during {@link #populatePolicyMap()}
	 */
	public static void setBaseURL(String baseUrl) throws SyncProxyException {
		moduleBaseURL = baseUrl;		
		if (moduleBaseURL != null) {
			populatePolicyMap(DEFAULT_COOKIE_MANAGER);
		} else {
			POLICY_MAP.clear();
			POLICY_MAP.putAll(RpcPolicyFinder.searchPolicyFileInClassPath());
		}
	}
	
	/**
	 * @since 0.6
	 * @param baseUrl
	 *            if null, existing {@link #POLICY_MAP} is cleared and
	 *            repopulated from ClassPath (
	 *            {@link RpcPolicyFinder#searchPolicyFileInClassPath()}). If
	 *            Non-Null, {@link #POLICY_MAP} is appended with new population
	 * @param cookiemanager
	 *            This parameter is store the cookie information, for example using return of {@link LoginUtils#loginFormBasedJ2EE()}      
	 * @throws SyncProxyException
	 *             if occurs during {@link #populatePolicyMap()}
	 */
	public static void setBaseURL(String baseUrl, CookieManager cookiemanager) throws SyncProxyException {
		moduleBaseURL = baseUrl;
		if (moduleBaseURL != null) {
			populatePolicyMap(cookiemanager);
		} else {
			POLICY_MAP.clear();
			POLICY_MAP.putAll(RpcPolicyFinder.searchPolicyFileInClassPath());
		}
	}

	/**
	 * Sets logging level for all SyncProxy classes
	 *
	 * @param level
	 * @since 0.5
	 */
	public static void setLoggingLevel(Level level) {
		SyncProxy.level = level;
		Logger topLogger = java.util.logging.Logger.getLogger("");
		// Handler for console (reuse it if it already exists)
		Handler consoleHandler = null;
		// see if there is already a console handler
		for (Handler handler : topLogger.getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				// found the console handler
				consoleHandler = handler;
				break;
			}
		}

		if (consoleHandler == null) {
			// there was no console handler found, create a new one
			consoleHandler = new ConsoleHandler();
			topLogger.addHandler(consoleHandler);
		}
		// set the console handler to level
		consoleHandler.setLevel(level);

		for (Class<?> clazz : spClazzes) {
			Logger iLogger = Logger.getLogger(clazz.getName());
			iLogger.setLevel(level);
		}
	}

	/**
	 * Static flag to suppress the exception issued if a RemoteService does not
	 * implement the {@link RemoteServiceRelativePath} annotation
	 *
	 * @param suppressRelativePathWarning
	 *            the suppressRelativePathWarning to set
	 */
	public static void suppressRelativePathWarning(
			boolean suppressRelativePathWarning) {
		logger.info((suppressRelativePathWarning ? "" : "Not ")
				+ "Supressing Relative Path Warning");
		SyncProxy.suppressRelativePathWarning = suppressRelativePathWarning;
	}

	/**
	 * @since 0.5
	 */
	protected static Class<?>[] spClazzes = { SyncProxy.class,
		RpcPolicyFinder.class, RemoteServiceInvocationHandler.class,
			RemoteServiceSyncProxy.class,
		SyncClientSerializationStreamReader.class,
		SyncClientSerializationStreamWriter.class };
	/**
	 * @since 0.5
	 */
	static boolean suppressRelativePathWarning = false;

	static Level level;

	static Logger logger = Logger.getLogger(SyncProxy.class.getName());

	protected static final String ASYNC_POSTFIX = "Async";

	/**
	 * @since 0.5
	 */
	static protected String moduleBaseURL;

	/**
	 * Map from ServiceInterface class name to Serialization Policy name. By
	 * Default this is loaded with policy-file data from the classpath:
	 * {@link RpcPolicyFinder#searchPolicyFileInClassPath()}
	 */
	protected static final Map<String, String> POLICY_MAP = RpcPolicyFinder
			.searchPolicyFileInClassPath();

	private static final CookieManager DEFAULT_COOKIE_MANAGER = new CookieManager(
			null, CookiePolicy.ACCEPT_ALL);
}
