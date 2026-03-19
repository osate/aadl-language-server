package org.osate.aadl.ls.core.internal;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class AadlServer implements IApplication {

	private volatile boolean shutdown;
	private long parentProcessId;
	private final Object waitLock = new Object();

	@Override
	public Object start(IApplicationContext context) throws Exception {

		AadlLanguageServerPlugin.startLanguageServer(this);
		synchronized (waitLock) {
			while (!shutdown) {
				try {
					context.applicationRunning();
					AadlLanguageServerPlugin.logInfo("Main thread is waiting");
					waitLock.wait();
				} catch (InterruptedException e) {
					AadlLanguageServerPlugin.logException(e.getMessage(), e);
				}
			}
		}
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		synchronized (waitLock) {
			waitLock.notifyAll();
		}
	}

	public void exit() {
		shutdown = true;
		AadlLanguageServerPlugin.logInfo("Shutdown received... waking up main thread");
		synchronized (waitLock) {
			waitLock.notifyAll();
		}
	}

	public void setParentProcessId(long pid) {
		this.parentProcessId = pid;
	}

	/**
	 * @return the parentProcessId
	 */
	long getParentProcessId() {
		return parentProcessId;
	}
}
