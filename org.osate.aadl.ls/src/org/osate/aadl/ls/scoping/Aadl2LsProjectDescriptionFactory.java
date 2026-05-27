package org.osate.aadl.ls.scoping;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.xtext.ide.server.DefaultProjectDescriptionFactory;
import org.eclipse.xtext.resource.impl.ProjectDescription;
import org.eclipse.xtext.workspace.IProjectConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Aadl2LsProjectDescriptionFactory extends DefaultProjectDescriptionFactory {

	static final String PROJECT_FILE = ".project";

	@SuppressWarnings("restriction")
	@Override
	public ProjectDescription getProjectDescription(IProjectConfig project) {
		var description = super.getProjectDescription(project);

		var uri = project.getPath();
		if (Objects.nonNull(uri) && uri.isFile()) {
			String path = uri.toFileString();
			description.setDependencies(getProjectDependencies(path + PROJECT_FILE));
		}

		return description;
	}

	private List<String> getProjectDependencies(String filePath) {
		List<String> projectDependencies = new ArrayList<>();
		File xmlFile = new File(filePath);
		if (xmlFile.canRead()) {
			try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(xmlFile);

				// Normalize the XML structure
				doc.getDocumentElement().normalize();

				// Inter-project dependencies are listed inside <projects> <project> tags
				NodeList nList = doc.getElementsByTagName("project");

				for (int i = 0; i < nList.getLength(); i++) {
					Node nNode = nList.item(i);
					// Ensure we only get <project> tags that are children of <projects>
					if (nNode.getParentNode().getNodeName().equals("projects")) {
						projectDependencies.add(nNode.getTextContent().trim());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return projectDependencies;
	}
}
