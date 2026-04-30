package org.osate.aadl.ls;

import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.xtext.ide.server.ServerLauncher;
import org.osate.aadl.ls.core.AadlServerModule;

public class RunAadl2Server {

	public static void main(String[] args) {
		// Read the meta information about the plug-ins to get the annex information
		EcorePlugin.ExtensionProcessor.process(null);

		ServerLauncher.launch(RunAadl2Server.class.getName(), args, new AadlServerModule());
	}

}
