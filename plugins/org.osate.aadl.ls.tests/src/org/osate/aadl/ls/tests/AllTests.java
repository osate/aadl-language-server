package org.osate.aadl.ls.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.osate.aadl.ls.tests.lsp.CommandServiceInstantiateTest;
import org.osate.aadl.ls.tests.lsp.DiagnosticsSmokeTest;
import org.osate.aadl.ls.tests.lsp.DocumentSymbolLspTest;
import org.osate.aadl.ls.tests.lsp.Emv2ParsingTest;
import org.osate.aadl.ls.tests.lsp.MultiRootLinkingTest;
import org.osate.aadl.ls.tests.unit.Aadl2LsGlobalScopeProviderTest;
import org.osate.aadl.ls.tests.unit.Aadl2LsProjectDescriptionFactoryTest;
import org.osate.aadl.ls.tests.unit.AadlSymbolNameProviderTest;
import org.osate.aadl.ls.tests.unit.CommandServiceInitializeTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		CommandServiceInitializeTest.class,
		Aadl2LsProjectDescriptionFactoryTest.class,
		AadlSymbolNameProviderTest.class,
		Aadl2LsGlobalScopeProviderTest.class,
		DiagnosticsSmokeTest.class,
		DocumentSymbolLspTest.class,
		CommandServiceInstantiateTest.class,
		MultiRootLinkingTest.class,
		Emv2ParsingTest.class
})
public class AllTests {
}
