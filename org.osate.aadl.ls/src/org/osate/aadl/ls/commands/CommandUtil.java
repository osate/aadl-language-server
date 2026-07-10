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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.result.AnalysisResult;
import org.osate.result.Diagnostic;
import org.osate.result.Result;

import com.google.gson.JsonPrimitive;

final class CommandUtil {

	private CommandUtil() {
	}

	static boolean optBool(List<Object> args, int index, boolean defaultValue) {
		if (args == null || index >= args.size()) {
			return defaultValue;
		}
		var arg = args.get(index);
		if (arg instanceof JsonPrimitive p && p.isBoolean()) {
			return p.getAsBoolean();
		}
		return defaultValue;
	}

	static String toFsPath(URI uri) {
		return uri.toFileString().replaceAll("^/+", "/");
	}

	static void appendInstanceDiagnostics(StringBuilder output, AnalysisResult analysisResult, String instancePath) {
		var diagLines = new ArrayList<String>();
		collectDiagnostics(analysisResult.getModelElement(), analysisResult.getDiagnostics(), instancePath, diagLines);
		for (Result r : analysisResult.getResults()) {
			collectInstanceDiagnostics(r, instancePath, diagLines);
		}
		Collections.sort(diagLines);
		for (var line : diagLines) {
			output.append(line).append('\n');
		}
	}

	private static void collectInstanceDiagnostics(Result r, String instancePath, List<String> lines) {
		collectDiagnostics(r.getModelElement(), r.getDiagnostics(), instancePath, lines);
		for (var sub : r.getSubResults()) {
			collectInstanceDiagnostics(sub, instancePath, lines);
		}
	}

	private static void collectDiagnostics(EObject modelElement, Iterable<Diagnostic> diagnostics, String instancePath,
			List<String> lines) {
		var elementPath = elementPath(modelElement, "<unknown>");
		for (var d : diagnostics) {
			var path = elementPath(d.getModelElement(), elementPath);
			lines.add(instancePath + ":" + path + ": "
					+ d.getDiagnosticType().getName().toLowerCase(Locale.ROOT)
					+ ": " + d.getMessage());
		}
	}

	private static String elementPath(EObject element, String fallback) {
		if (element instanceof InstanceObject io) {
			return io.getComponentInstancePath();
		}
		if (element instanceof NamedElement ne && ne.getName() != null) {
			return ne.getName();
		}
		return fallback;
	}
}
