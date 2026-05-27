package org.osate.aadl.ls.commands;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.xtext.ide.server.ILanguageServerAccess;
import org.eclipse.xtext.ide.server.commands.IExecutableCommandService;
import org.eclipse.xtext.util.CancelIndicator;

public class CommandService implements IExecutableCommandService {

	private final Map<String, Command> commands = new LinkedHashMap<>();

	public CommandService() {
		register(new InstantiateCommand());
		register(new AnalyzeLatencyCommand());
	}

	private void register(Command command) {
		commands.put(command.name(), command);
	}

	@Override
	public List<String> initialize() {
		return List.copyOf(commands.keySet());
	}

	@Override
	public Object execute(ExecuteCommandParams params, ILanguageServerAccess access, CancelIndicator cancelIndicator) {
		var command = commands.get(params.getCommand());
		if (command == null) {
			return "Bad Command";
		}
		return command.execute(params, access, cancelIndicator);
	}
}
