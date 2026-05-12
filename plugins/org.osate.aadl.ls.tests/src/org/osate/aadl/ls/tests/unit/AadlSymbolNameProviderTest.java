package org.osate.aadl.ls.tests.unit;

import static org.junit.Assert.assertEquals;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osate.aadl.ls.internal.AadlSymbolNameProvider;
import org.osate.aadl.ls.tests.AadlLsInjectorProvider;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.PackageSection;

import com.google.inject.Inject;

@RunWith(XtextRunner.class)
@InjectWith(AadlLsInjectorProvider.class)
public class AadlSymbolNameProviderTest {

	@Inject
	private ParseHelper<AadlPackage> parseHelper;

	@Inject
	private AadlSymbolNameProvider nameProvider;

	@Test
	public void packageShowsFullyQualifiedName() throws Exception {
		AadlPackage pkg = parseHelper.parse("""
				package Sample
				public
					system sys
					end sys;
				end Sample;
				""");
		assertEquals("Sample", nameProvider.getName(pkg));
	}

	@Test
	public void publicSectionShowsLiteralPublic() throws Exception {
		AadlPackage pkg = parseHelper.parse("""
				package Sample
				public
					system sys
					end sys;
				end Sample;
				""");
		PackageSection pub = pkg.getOwnedPublicSection();
		assertEquals("public", nameProvider.getName(pub));
	}

	@Test
	public void privateSectionShowsLiteralPrivate() throws Exception {
		AadlPackage pkg = parseHelper.parse("""
				package Sample
				private
					system sys
					end sys;
				end Sample;
				""");
		PackageSection priv = pkg.getOwnedPrivateSection();
		assertEquals("private", nameProvider.getName(priv));
	}

	@Test
	public void classifierShowsSimpleName() throws Exception {
		AadlPackage pkg = parseHelper.parse("""
				package Sample
				public
					system sys
					end sys;
				end Sample;
				""");
		Classifier sys = pkg.getOwnedPublicSection().getOwnedClassifiers().get(0);
		assertEquals("sys", nameProvider.getName(sys));
	}
}
