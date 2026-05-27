package org.osate.aadl.ls.tests.unit;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.osate.aadl.ls.commands.CommandService;

public class CommandServiceInitializeTest {

	@Test
	public void registersInstantiateAndLatencyCommands() {
		List<String> commands = new CommandService().initialize();
		assertEquals(List.of("aadl.instantiate", "aadl.analyze.latency"), commands);
	}
}
