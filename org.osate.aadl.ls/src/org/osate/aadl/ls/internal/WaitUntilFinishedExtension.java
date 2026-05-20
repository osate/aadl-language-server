package org.osate.aadl.ls.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.xtext.ide.server.ILanguageServerAccess;
import org.eclipse.xtext.ide.server.ILanguageServerExtension;
import org.eclipse.xtext.resource.IResourceDescription;

/**
 * Custom JSON-RPC endpoint {@code aadlServer/waitUntilFinished} used by the OSATE CLI workspace
 * server to block until Xtext's next background build completes. Each request returns a fresh
 * promise; {@link #afterBuild} resolves every promise queued at that moment.
 *
 * Since LSP4J dispatches inbound JSON-RPC messages serially on the reader thread, any
 * {@code didOpen} / {@code didChangeWatchedFiles} arriving before this request has already
 * submitted its {@code runBuildable} to the request manager by the time our handler runs, so
 * the next {@code afterBuild} is the one we want. The caller MUST only send
 * {@code aadlServer/waitUntilFinished} after a notification that triggers a build; otherwise
 * the promise will not resolve.
 */
public class WaitUntilFinishedExtension implements ILanguageServerExtension, ILanguageServerAccess.IBuildListener {

	private final List<CompletableFuture<Boolean>> pendingPromises = new ArrayList<>();

	@Override
	public void initialize(ILanguageServerAccess access) {
		access.addBuildListener(this);
	}

	@JsonRequest("aadlServer/waitUntilFinished")
	public synchronized CompletableFuture<Boolean> waitUntilFinished() {
		var promise = new CompletableFuture<Boolean>();
		pendingPromises.add(promise);
		return promise;
	}

	@Override
	public synchronized void afterBuild(List<IResourceDescription.Delta> deltas) {
		for (var promise : pendingPromises) {
			if (!promise.isDone()) {
				promise.complete(Boolean.TRUE);
			}
		}
		pendingPromises.clear();
	}
}
