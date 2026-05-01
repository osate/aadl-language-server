package org.osate.aadl.ls.tests.lsp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.Diagnostic;
import org.junit.Test;

public class DiagnosticsSmokeTest extends AbstractAadlLanguageServerTest {

	@Test
	public void validPackageHasNoDiagnostics() {
		initialize();
		String uri = writeFile("valid.aadl", """
				package valid
				public
					system sys
					end sys;
				end valid;
				""");
		open(uri, """
				package valid
				public
					system sys
					end sys;
				end valid;
				""");

		Map<String, List<Diagnostic>> diagnostics = getDiagnostics();
		List<Diagnostic> forFile = diagnostics.getOrDefault(uri, List.of());
		boolean hasError = forFile.stream()
				.anyMatch(d -> d.getSeverity() != null && d.getSeverity().getValue() == 1);
		assertFalse("Expected no error diagnostics, got: " + forFile, hasError);
	}

	@Test
	public void unresolvedReferenceProducesDiagnostic() {
		initialize();
		String uri = writeFile("broken.aadl", """
				package broken
				public
					system Foo extends NonExistent
					end Foo;
				end broken;
				""");
		open(uri, """
				package broken
				public
					system Foo extends NonExistent
					end Foo;
				end broken;
				""");

		Map<String, List<Diagnostic>> diagnostics = getDiagnostics();
		List<Diagnostic> forFile = diagnostics.getOrDefault(uri, List.of());
		assertTrue("Expected at least one diagnostic for unresolved reference, got: " + forFile,
				forFile.stream().anyMatch(d -> d.getMessage() != null
						&& d.getMessage().toLowerCase().contains("nonexistent")));
	}
}
