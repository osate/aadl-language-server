package org.osate.aadl.ls.commands;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.xtext.ide.server.ILanguageServerAccess;
import org.eclipse.xtext.util.CancelIndicator;

interface Command {
	String name();

	Object execute(ExecuteCommandParams params, ILanguageServerAccess access, CancelIndicator cancelIndicator);
}
