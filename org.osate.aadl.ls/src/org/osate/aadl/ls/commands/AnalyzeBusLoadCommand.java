/*******************************************************************************
 * Copyright (c) 2004-2026 Carnegie Mellon University and others. (see Contributors file). 
 * All Rights Reserved.
 *
 * NO WARRANTY. ALL MATERIAL IS FURNISHED ON AN "AS-IS" BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE
 * OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT
 * MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Created, in part, with funding and support from the United States Government. (see Acknowledgments file).
 *
 * This program includes and/or can make use of certain third party source code, object code, documentation and other
 * files ("Third Party Software"). The Third Party Software that is used by this program is dependent upon your system
 * configuration. By using this program, You agree to comply with any and all relevant Third Party Software terms and
 * conditions contained in any such Third Party Software or separate license file distributed with such Third Party
 * Software. The parties who own the Third Party Software ("Third Party Licensors") are intended third party beneficiaries
 * to this license with respect to the terms applicable to their Third Party Software. Third Party Software licenses
 * only apply to the Third Party Software and not any other portion of this program or this program as a whole.
 *******************************************************************************/
package org.osate.aadl.ls.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.xtext.ide.server.ILanguageServerAccess;
import org.eclipse.xtext.util.CancelIndicator;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.analysis.resource.budgets.busload.NewBusLoadAnalysis;

import com.google.common.collect.Iterables;
import com.google.gson.JsonPrimitive;

final class AnalyzeBusLoadCommand implements Command {

	static final String NAME = "aadl.analyze.busLoad";
	private static final String REPORTS_DIR = "reports";
	private static final String ANALYSIS_DIR = "BusLoad";
	private static final String REPORT_NAME_TAIL = "__BusLoad.csv";

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public Object execute(ExecuteCommandParams params, ILanguageServerAccess access, CancelIndicator cancelIndicator) {
		var arg1 = (JsonPrimitive) Iterables.getFirst(params.getArguments(), null);
		String iuri = arg1.getAsString();
		if (iuri == null) {
			return "Param Uri Missing";
		}

		var uri = URI.createURI(iuri);
		Resource res = new ResourceSetImpl().getResource(uri, true);
		if (res.getContents().isEmpty() || !(res.getContents().get(0) instanceof SystemInstance instance)) {
			return "Error: " + iuri + " does not contain a system instance";
		}
		var componentImplementation = instance.getComponentImplementation();
		var duri = componentImplementation == null || componentImplementation.eResource() == null
				? uri
				: componentImplementation.eResource().getURI();
		try {
			return access.doRead(duri.toString(), ctx -> runAnalysis(uri, iuri)).get();
		} catch (InterruptedException | ExecutionException e) {
			return e.getMessage();
		}
	}

	private static String runAnalysis(URI uri, String iuri) {
		var output = new StringBuilder();
		output.append("Ran bus load analysis of ").append(iuri).append('\n');

		var resource = new ResourceSetImpl().getResource(uri, true);
		if (resource.getContents().isEmpty() || !(resource.getContents().get(0) instanceof SystemInstance inst)) {
			return "Error: " + iuri + " does not contain a system instance";
		}

		try {
			createReportDirectory(uri);
		} catch (IOException e) {
			output.append("Exception: ").append(e.getMessage());
			return output.toString();
		}

		var analysisResult = new NewBusLoadAnalysis().invoke(null, inst);
		var csvURI = uri.trimSegments(1)
				.appendSegment(REPORTS_DIR)
				.appendSegment(ANALYSIS_DIR)
				.appendSegment(uri.trimFileExtension().lastSegment() + REPORT_NAME_TAIL);
		output.append(CommandUtil.toFsPath(csvURI)).append('\n');

		if (analysisResult != null) {
			var instancePath = CommandUtil.toFsPath(inst.eResource().getURI());
			CommandUtil.appendInstanceDiagnostics(output, analysisResult, instancePath);
		}
		return output.toString();
	}

	private static void createReportDirectory(URI instanceURI) throws IOException {
		var reportURI = instanceURI.trimSegments(1).appendSegment(REPORTS_DIR).appendSegment(ANALYSIS_DIR);
		if (reportURI.isFile()) {
			Files.createDirectories(Path.of(CommandUtil.toFsPath(reportURI)));
		}
	}
}
