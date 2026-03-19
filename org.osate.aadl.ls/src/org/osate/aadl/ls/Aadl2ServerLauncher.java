package org.osate.aadl.ls;

import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.xtext.ide.server.SocketServerLauncher;

public class Aadl2ServerLauncher {

	public static void main(String[] args) {

		// Read the meta information about the plug-ins to get the annex information
		EcorePlugin.ExtensionProcessor.process(null);

		new SocketServerLauncher().launch(args);
	}

}
