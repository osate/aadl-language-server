package org.osate.aadl.ls.tests.lsp;

import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.xtext.ide.server.concurrent.IRequestManager;
import org.eclipse.xtext.testing.AbstractLanguageServerTest;
import org.eclipse.xtext.util.Modules2;
import org.junit.BeforeClass;
import org.osate.aadl.ls.core.AadlServerModule;

public abstract class AbstractAadlLanguageServerTest extends AbstractLanguageServerTest {

	public AbstractAadlLanguageServerTest() {
		super("aadl");
	}

	@BeforeClass
	public static void discoverPluginContributions() {
		EcorePlugin.ExtensionProcessor.process(null);
	}

	@Override
	protected com.google.inject.Module getServerModule() {
		return Modules2.mixin(new AadlServerModule(),
				(com.google.inject.Binder binder) -> binder.bind(IRequestManager.class)
						.to(AbstractLanguageServerTest.DirectRequestManager.class));
	}
}
