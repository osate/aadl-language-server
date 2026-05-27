package org.osate.aadl.ls.tests.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.impl.ProjectDescription;
import org.eclipse.xtext.workspace.FileProjectConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osate.aadl.ls.scoping.Aadl2LsProjectDescriptionFactory;

public class Aadl2LsProjectDescriptionFactoryTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private ProjectDescription describe(File projectDir) {
		FileProjectConfig config = new FileProjectConfig(URI.createFileURI(projectDir.getAbsolutePath() + "/"),
				projectDir.getName());
		return new Aadl2LsProjectDescriptionFactory().getProjectDescription(config);
	}

	private void writeProjectFile(File projectDir, String xml) throws Exception {
		Files.writeString(new File(projectDir, ".project").toPath(), xml, StandardCharsets.UTF_8);
	}

	@Test
	public void readsMultipleDependenciesFromProjectFile() throws Exception {
		File dir = folder.newFolder("ProjB");
		writeProjectFile(dir, """
				<?xml version="1.0" encoding="UTF-8"?>
				<projectDescription>
					<name>ProjB</name>
					<projects>
						<project>Dep1</project>
						<project>Dep2</project>
					</projects>
				</projectDescription>
				""");

		ProjectDescription desc = describe(dir);

		assertEquals(java.util.List.of("Dep1", "Dep2"), desc.getDependencies());
	}

	@Test
	public void returnsEmptyDependenciesWhenNoProjectsElement() throws Exception {
		File dir = folder.newFolder("ProjA");
		writeProjectFile(dir, """
				<?xml version="1.0" encoding="UTF-8"?>
				<projectDescription>
					<name>ProjA</name>
					<projects>
					</projects>
				</projectDescription>
				""");

		ProjectDescription desc = describe(dir);

		assertTrue(desc.getDependencies().isEmpty());
	}

	@Test
	public void returnsEmptyDependenciesWhenProjectFileMissing() throws Exception {
		File dir = folder.newFolder("NoProjFile");

		ProjectDescription desc = describe(dir);

		assertTrue(desc.getDependencies().isEmpty());
	}

	@Test
	public void ignoresProjectTagsOutsideProjectsParent() throws Exception {
		File dir = folder.newFolder("ProjC");
		writeProjectFile(dir, """
				<?xml version="1.0" encoding="UTF-8"?>
				<projectDescription>
					<name>ProjC</name>
					<projects>
						<project>Real</project>
					</projects>
					<comment>
						<project>NotADependency</project>
					</comment>
				</projectDescription>
				""");

		ProjectDescription desc = describe(dir);

		assertEquals(java.util.List.of("Real"), desc.getDependencies());
	}
}
