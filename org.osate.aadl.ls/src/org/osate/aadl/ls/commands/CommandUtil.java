package org.osate.aadl.ls.commands;

import java.util.List;

import org.eclipse.emf.common.util.URI;

import com.google.gson.JsonPrimitive;

final class CommandUtil {

	private CommandUtil() {
	}

	static boolean optBool(List<Object> args, int index, boolean defaultValue) {
		if (args == null || index >= args.size()) {
			return defaultValue;
		}
		var arg = args.get(index);
		if (arg instanceof JsonPrimitive p && p.isBoolean()) {
			return p.getAsBoolean();
		}
		return defaultValue;
	}

	static String toFsPath(URI uri) {
		return uri.toFileString().replaceAll("^/+", "/");
	}
}
