package org.osate.aadl.ls.tests.lsp;

import java.util.List;

import org.eclipse.lsp4j.DocumentSymbol;
import org.junit.Assert;
import org.junit.Test;

public class DocumentSymbolLspTest extends AbstractAadlLanguageServerTest {

	@Test
	public void symbolTreeUsesCustomNameProvider() {
		testDocumentSymbol(cfg -> {
			cfg.setFilePath("symbols.aadl");
			cfg.setModel("""
					package symbols_pkg
					public
						system sys
						end sys;

						system implementation sys.impl
						end sys.impl;
					end symbols_pkg;
					""");
			cfg.setAssertSymbols(either -> {
				Assert.assertEquals("Expected single top-level symbol", 1, either.size());
				DocumentSymbol pkg = either.get(0).getRight();
				Assert.assertNotNull("Hierarchical symbols expected", pkg);
				Assert.assertEquals("symbols_pkg", pkg.getName());

				List<DocumentSymbol> children = pkg.getChildren();
				Assert.assertNotNull(children);
				Assert.assertTrue("Expected at least one child symbol",
						children.stream().anyMatch(s -> "public".equals(s.getName())));

				DocumentSymbol pub = children.stream()
						.filter(s -> "public".equals(s.getName()))
						.findFirst()
						.orElseThrow();
				List<DocumentSymbol> classifiers = pub.getChildren();
				Assert.assertNotNull(classifiers);
				Assert.assertTrue("Expected classifier 'sys' as simple name",
						classifiers.stream().anyMatch(s -> "sys".equals(s.getName())));
				Assert.assertTrue("Expected classifier 'sys.impl' as simple name",
						classifiers.stream().anyMatch(s -> "sys.impl".equals(s.getName())));
			});
			cfg.setInitializer(params -> {
				if (params.getCapabilities() == null) {
					params.setCapabilities(new org.eclipse.lsp4j.ClientCapabilities());
				}
				if (params.getCapabilities().getTextDocument() == null) {
					params.getCapabilities()
							.setTextDocument(new org.eclipse.lsp4j.TextDocumentClientCapabilities());
				}
				org.eclipse.lsp4j.DocumentSymbolCapabilities symCaps = new org.eclipse.lsp4j.DocumentSymbolCapabilities();
				symCaps.setHierarchicalDocumentSymbolSupport(true);
				params.getCapabilities().getTextDocument().setDocumentSymbol(symCaps);
			});
		});
	}
}
