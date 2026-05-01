package org.osate.aadl.ls.tests.unit;

import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osate.aadl.ls.tests.AadlLsInjectorProvider;
import org.osate.aadl2.AadlPackage;

import com.google.inject.Inject;

@RunWith(XtextRunner.class)
@InjectWith(AadlLsInjectorProvider.class)
public class Aadl2LsGlobalScopeProviderTest {

	@BeforeClass
	public static void discoverPluginContributions() {
		EcorePlugin.ExtensionProcessor.process(null);
	}

	@Inject
	private ParseHelper<AadlPackage> parseHelper;

	@Inject
	private ValidationTestHelper validationHelper;

	@Test
	public void predeclaredTimingPropertiesResolveWithoutWithClause() throws Exception {
		AadlPackage pkg = parseHelper.parse("""
				package UsesPredeclared
				public
					thread t
						properties
							Period => 10 ms;
					end t;
				end UsesPredeclared;
				""");
		validationHelper.assertNoErrors(pkg);
	}
}
