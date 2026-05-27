package org.osate.aadl.ls.tests.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.ExecuteCommandCapabilities;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonPrimitive;

public class CommandServiceInstantiateTest extends AbstractAadlLanguageServerTest {

	@Test
	public void instantiateCommandIsRegisteredAndDispatches() throws Exception {
		initialize(params -> {
			ClientCapabilities caps = params.getCapabilities();
			if (caps == null) {
				caps = new ClientCapabilities();
				params.setCapabilities(caps);
			}
			WorkspaceClientCapabilities workspace = caps.getWorkspace();
			if (workspace == null) {
				workspace = new WorkspaceClientCapabilities();
				caps.setWorkspace(workspace);
			}
			workspace.setExecuteCommand(new ExecuteCommandCapabilities());
		});
		String source = """
				package sys
				public
					system sys
					end sys;

					system implementation sys.impl
					end sys.impl;
				end sys;
				""";
		String uri = writeFile("sys.aadl", source);
		open(uri, source);

		ExecuteCommandParams params = new ExecuteCommandParams();
		params.setCommand("aadl.instantiate");
		params.setArguments(List.of(new JsonPrimitive(uri), new JsonPrimitive("sys.impl")));

		CompletableFuture<Object> future = languageServer.getWorkspaceService().executeCommand(params);
		Object result = future.get();

		// Whether a sibling .aaxl is written depends on OSATE's instance-file I/O under
		// the non-UI harness; a stable signal here is that the command is dispatched end-to-end
		// through CommandService.execute() and returns its success message.
		Assert.assertTrue("expected success message, got: " + result,
				result instanceof String s && s.startsWith("Instantiated sys.impl"));
	}
}
