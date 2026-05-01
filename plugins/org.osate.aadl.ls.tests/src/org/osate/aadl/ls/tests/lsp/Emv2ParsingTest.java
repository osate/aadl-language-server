package org.osate.aadl.ls.tests.lsp;

import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.Test;

public class Emv2ParsingTest extends AbstractAadlLanguageServerTest {

	@Test
	public void packageWithEmv2AnnexParsesWithoutErrors() {
		initialize();
		String source = """
				package errors
				public

					abstract A
						features
							f: feature;
						annex emv2 {**
							use types ErrorLibrary;
							error propagations
								f: in propagation {ValueError};
							end propagations;
						**};
					end A;

				end errors;
				""";
		String uri = writeFile("errors.aadl", source);
		open(uri, source);

		Map<String, List<Diagnostic>> diagnostics = getDiagnostics();
		List<Diagnostic> forFile = diagnostics.getOrDefault(uri, List.of());
		boolean hasError = forFile.stream()
				.anyMatch(d -> d.getSeverity() == DiagnosticSeverity.Error);
		assertFalse("Expected EMV2 annex to parse without errors, got: " + forFile, hasError);
	}
}
