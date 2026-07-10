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

import java.util.concurrent.ExecutionException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.xtext.ide.server.ILanguageServerAccess;
import org.eclipse.xtext.util.CancelIndicator;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.analysis.modes.reachability.ReachabilityAnalyzer;
import org.osate.analysis.modes.reachability.ReachabilityConfiguration;
import org.osate.result.AnalysisResult;
import org.osate.result.ResultType;
import org.osate.result.StringValue;

import com.google.common.collect.Iterables;
import com.google.gson.JsonPrimitive;

final class AnalyzeReachabilityCommand implements Command {

	static final String NAME = "aadl.analyze.reachability";

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public Object execute(ExecuteCommandParams params, ILanguageServerAccess access, CancelIndicator cancelIndicator) {
		var arg1 = (JsonPrimitive) Iterables.getFirst(params.getArguments(), null);
		String iuri = arg1 == null ? null : arg1.getAsString();
		if (iuri == null) {
			return "Param Uri Missing";
		}
		var args = params.getArguments();
		boolean generateDot = CommandUtil.optBool(args, 1, false);
		boolean generateHtml = CommandUtil.optBool(args, 2, false);
		boolean generateSmv = CommandUtil.optBool(args, 3, false);

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
			return access.doRead(duri.toString(), ctx -> runAnalysis(uri, iuri, generateDot, generateHtml, generateSmv))
					.get();
		} catch (InterruptedException | ExecutionException e) {
			return e.getMessage();
		}
	}

	private static String runAnalysis(URI uri, String iuri, boolean generateDot, boolean generateHtml,
			boolean generateSmv) {
		var output = new StringBuilder();
		output.append("Ran mode reachability analysis of ").append(iuri).append('\n');

		var resource = new ResourceSetImpl().getResource(uri, true);
		if (resource.getContents().isEmpty() || !(resource.getContents().get(0) instanceof SystemInstance inst)) {
			return "Error: " + iuri + " does not contain a system instance";
		}

		var analysisResult = analyze(inst, generateDot, generateHtml, generateSmv);
		appendReportPaths(output, analysisResult);

		var instancePath = CommandUtil.toFsPath(inst.eResource().getURI());
		CommandUtil.appendInstanceDiagnostics(output, analysisResult, instancePath);
		return output.toString();
	}

	private static AnalysisResult analyze(SystemInstance inst, boolean generateDot, boolean generateHtml,
			boolean generateSmv) {
		var config = new ReachabilityConfiguration();
		if (generateDot) {
			config.withDot();
		}
		if (generateHtml) {
			config.withHTML();
		}
		if (generateSmv) {
			config.withSMV();
		}

		var analyzer = new ReachabilityAnalyzer(config, inst);
		var result = analyzer.analyzeModel();
		if (result.getResultType() == ResultType.SUCCESS && (generateDot || generateHtml || generateSmv)) {
			analyzer.writeReports();
		}
		return result;
	}

	private static void appendReportPaths(StringBuilder output, AnalysisResult result) {
		for (var r : result.getResults()) {
			if (isReportUriResult(r.getMessage())) {
				for (var v : r.getValues()) {
					if (v instanceof StringValue sv) {
						output.append(toReportPath(sv.getValue())).append('\n');
					}
				}
			}
		}
	}

	private static boolean isReportUriResult(String message) {
		return "DOT file URI".equals(message) || "HTML file URI".equals(message) || "SMV file URI".equals(message);
	}

	private static String toReportPath(String uriText) {
		var uri = URI.createURI(uriText);
		return uri.isFile() ? CommandUtil.toFsPath(uri) : uriText;
	}
}
