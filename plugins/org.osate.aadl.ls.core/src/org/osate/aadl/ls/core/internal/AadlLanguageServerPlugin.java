/*******************************************************************************
 * Copyright (c) 2016-2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.osate.aadl.ls.core.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.xtext.ide.server.LanguageServerImpl;
import org.eclipse.xtext.ide.server.ServerModule;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class AadlLanguageServerPlugin extends Plugin {

	private static final String LOGBACK_CONFIG_FILE_PROPERTY = "logback.configurationFile";
	private static final String LOGBACK_DEFAULT_FILENAME = "logback.xml";

	/**
	 * Source string send to clients for messages such as diagnostics.
	 **/
	public static final String SERVER_SOURCE_ID = "AADL";

	public static final String PLUGIN_ID = "org.osate.aadl.ls";

	private static AadlLanguageServerPlugin pluginInstance;
	private static BundleContext context;
	private static InputStream in;
	private static PrintStream out;
	private static PrintStream err;

	private AadlServer languageServer;

	public static AadlServer getLanguageServer() {
		return pluginInstance == null ? null : pluginInstance.languageServer;
	}

	public static BundleContext getBundleContext() {
		return AadlLanguageServerPlugin.context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		try {
			Platform.getBundle(ResourcesPlugin.PI_RESOURCES).start(Bundle.START_TRANSIENT);
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceDescription description = workspace.getDescription();
			description.setAutoBuilding(false);
			workspace.setDescription(description);
		} catch (BundleException e) {
			logException(e.getMessage(), e);
		}
		boolean isDebug = Boolean.getBoolean("aadl.ls.debug");
		try {
			redirectStandardStreams(isDebug);
		} catch (FileNotFoundException e) {
			logException(e.getMessage(), e);
		}
		context = bundleContext;
		pluginInstance = this;

		if (isDebug && System.getProperty(LOGBACK_CONFIG_FILE_PROPERTY) == null) {
			File stateDir = getStateLocation().toFile();
			File configFile = new File(stateDir, LOGBACK_DEFAULT_FILENAME);
			if (!configFile.isFile()) {
				try (InputStream is = bundleContext.getBundle().getEntry(LOGBACK_DEFAULT_FILENAME).openStream(); FileOutputStream fos = new FileOutputStream(configFile)) {
					for (byte[] buffer = new byte[1024 * 4];;) {
						int n = is.read(buffer);
						if (n < 0) {
							break;
						}
						fos.write(buffer, 0, n);
					}
				}
			}
			// ContextInitializer.CONFIG_FILE_PROPERTY
			System.setProperty(LOGBACK_CONFIG_FILE_PROPERTY, configFile.getAbsolutePath());
		}
	}

	public void startConnection() throws InterruptedException, IOException {
		Injector injector = Guice.createInjector(new ServerModule());
		LanguageServerImpl languageServer = injector.getInstance(LanguageServerImpl.class);
		Function<MessageConsumer, MessageConsumer> wrapper = consumer -> {
			MessageConsumer result = consumer;
			return result;
		};
		Launcher<LanguageClient> launcher = createSocketLauncher(languageServer, LanguageClient.class,
				new InetSocketAddress("localhost", 6215), Executors.newCachedThreadPool(), wrapper);
		languageServer.connect(launcher.getRemoteProxy());
		Future<?> future = launcher.startListening();
	}

	private <T> Launcher<T> createSocketLauncher(Object localService, Class<T> remoteInterface,
			SocketAddress socketAddress, ExecutorService executorService,
			Function<MessageConsumer, MessageConsumer> wrapper) throws IOException {
		AsynchronousServerSocketChannel serverSocket = AsynchronousServerSocketChannel.open().bind(socketAddress);
		AsynchronousSocketChannel socketChannel;
		try {
			socketChannel = serverSocket.accept().get();
			return Launcher.createIoLauncher(localService, remoteInterface, Channels.newInputStream(socketChannel),
					Channels.newOutputStream(socketChannel), executorService, wrapper);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		logInfo(getClass() + " is stopping:");
		AadlLanguageServerPlugin.pluginInstance = null;
		AadlLanguageServerPlugin.context = null;
		ResourcesPlugin.getWorkspace().removeSaveParticipant(PLUGIN_ID);
		languageServer = null;
	}

	public static AadlLanguageServerPlugin getInstance() {
		return pluginInstance;
	}

	public static void log(IStatus status) {
		if (context != null) {
			Platform.getLog(AadlLanguageServerPlugin.context.getBundle()).log(status);
		}
	}

	public static void log(CoreException e) {
		log(e.getStatus());
	}

	public static void logError(String message) {
		if (context != null) {
			log(new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), message));
		}
	}

	public static void logInfo(String message) {
		if (context != null) {
			log(new Status(IStatus.INFO, context.getBundle().getSymbolicName(), message));
		}
	}

	public static void logException(Throwable ex) {
		if (context != null) {
			String message = ex.getMessage();
			if (message == null) {
				message = Throwables.getStackTraceAsString(ex);
			}
			logException(message, ex);
		}
	}

	public static void logException(String message, Throwable ex) {
		if (context != null) {
			log(new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), message, ex));
		}
	}

	static void startLanguageServer(AadlServer newLanguageServer) throws IOException, InterruptedException {
		if (pluginInstance != null) {
			pluginInstance.languageServer = newLanguageServer;
			pluginInstance.startConnection();
		}
	}

	/**
	 * @return the Java Language Server version
	 */
	public static String getVersion() {
		return context == null ? "Unknown" : context.getBundle().getVersion().toString();
	}

	private static void redirectStandardStreams(boolean isDebug) throws FileNotFoundException {
		in = System.in;
		out = System.out;
		err = System.err;
		System.setIn(new ByteArrayInputStream(new byte[0]));
		if (isDebug) {
			String id = "jdt.ls-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			File workspaceFile = root.getRawLocation().makeAbsolute().toFile();
			File rootFile = new File(workspaceFile, ".metadata");
			rootFile.mkdirs();
			File outFile = new File(rootFile, ".out-" + id + ".log");
			FileOutputStream stdFileOut = new FileOutputStream(outFile);
			System.setOut(new PrintStream(stdFileOut));
			File errFile = new File(rootFile, ".error-" + id + ".log");
			FileOutputStream stdFileErr = new FileOutputStream(errFile);
			System.setErr(new PrintStream(stdFileErr));
		} else {
			System.setOut(new PrintStream(new ByteArrayOutputStream()));
			System.setErr(new PrintStream(new ByteArrayOutputStream()));
		}
	}

	public static InputStream getIn() {
		return in;
	}

	public static PrintStream getOut() {
		return out;
	}

	public static PrintStream getErr() {
		return err;
	}

}
