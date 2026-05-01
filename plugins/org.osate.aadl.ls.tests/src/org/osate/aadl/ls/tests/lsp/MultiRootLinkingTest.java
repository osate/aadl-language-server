package org.osate.aadl.ls.tests.lsp;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.Test;

public class MultiRootLinkingTest extends AbstractAadlLanguageServerTest {

	@Test
	public void crossProjectWithClauseLinks() throws Exception {
		File projA = new File(root, "A");
		File projB = new File(root, "B");
		projA.mkdirs();
		projB.mkdirs();

		Files.writeString(new File(projA, ".project").toPath(), """
				<?xml version="1.0" encoding="UTF-8"?>
				<projectDescription>
					<name>A</name>
					<projects></projects>
				</projectDescription>
				""", StandardCharsets.UTF_8);
		Files.writeString(new File(projB, ".project").toPath(), """
				<?xml version="1.0" encoding="UTF-8"?>
				<projectDescription>
					<name>B</name>
					<projects>
						<project>A</project>
					</projects>
				</projectDescription>
				""", StandardCharsets.UTF_8);
		Files.writeString(new File(projA, "a_pkg.aadl").toPath(), """
				package A_Pkg
				public
					system AType
					end AType;
				end A_Pkg;
				""", StandardCharsets.UTF_8);
		String bPkgSource = """
				package B_Pkg
				public
					with A_Pkg;

					system BType extends A_Pkg::AType
					end BType;
				end B_Pkg;
				""";
		File bPkgFile = new File(projB, "b_pkg.aadl");
		Files.writeString(bPkgFile.toPath(), bPkgSource, StandardCharsets.UTF_8);

		initialize(params -> {
			List<WorkspaceFolder> folders = new ArrayList<>();
			folders.add(new WorkspaceFolder(projA.toURI().toString(), "A"));
			folders.add(new WorkspaceFolder(projB.toURI().toString(), "B"));
			params.setWorkspaceFolders(folders);
		});

		String bUri = bPkgFile.toURI().toString();
		open(bUri, bPkgSource);

		Map<String, List<Diagnostic>> diagnostics = getDiagnostics();
		List<Diagnostic> forFile = diagnostics.getOrDefault(bUri, List.of());
		boolean hasError = forFile.stream()
				.anyMatch(d -> d.getSeverity() == DiagnosticSeverity.Error);
		assertFalse("Expected cross-project reference to resolve, got errors: " + forFile, hasError);
	}
}
