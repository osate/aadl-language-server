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
package org.osate.aadl.ls.tests.lsp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.ExecuteCommandCapabilities;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonPrimitive;

public class CommandServiceReachabilityTest extends AbstractAadlLanguageServerTest {

	@Test
	public void reachabilityCommandRunsAnalysisAndFormatsDiagnostics() throws Exception {
		initialize(params -> {
			ClientCapabilities caps = params.getCapabilities();
			if (caps == null) {
				caps = new ClientCapabilities();
				params.setCapabilities(caps);
			}
			WorkspaceClientCapabilities workspace = caps.getWorkspace();
			if (workspace == null) {
				workspace = new WorkspaceClientCapabilities();
				caps.setWorkspace(workspace);
			}
			workspace.setExecuteCommand(new ExecuteCommandCapabilities());
		});
		String source = """
				package modetest
				public
					system S
						features
							e0: in event port;
							e1: in event port;
					end S;

					system implementation S.i
						subcomponents
							s0: system R.i1 in modes (m1 => m12);
							s1: system S.i2;
						connections
							c00: port e1 -> s0.e1;
							c01: port e0 -> s1.e1;
						modes
							m0: initial mode;
							m1: mode;
							m0 -[e0]-> m1;
							m1 -[e0]-> m0;
					end S.i;

					system R extends S
						requires modes
							m10: mode;
							m11: mode;
							m12: mode;
					end R;

					system implementation R.i1
						subcomponents
							a: system S.i2 in modes (m11);
							b: system S.i2 in modes (m12);
						connections
							c11: port e1 -> a.e1;
							c12: port e1 -> b.e1;
					end R.i1;

					system implementation S.i2
						modes
							m20: initial mode;
							m21: mode;
							m22: mode;
							m20 -[e1]-> m21;
							m21 -[e1]-> m22;
					end S.i2;
				end modetest;
				""";
		String uri = writeFile("modetest.aadl", source);
		open(uri, source);

		ExecuteCommandParams instantiateParams = new ExecuteCommandParams();
		instantiateParams.setCommand("aadl.instantiate");
		instantiateParams.setArguments(List.of(new JsonPrimitive(uri), new JsonPrimitive("modetest::S.i")));

		CompletableFuture<Object> instantiateFuture = languageServer.getWorkspaceService().executeCommand(
				instantiateParams);
		Object instantiateResult = instantiateFuture.get();
		Assert.assertTrue("expected instantiation success, got: " + instantiateResult,
				instantiateResult instanceof String s && s.startsWith("Instantiated modetest::S.i"));

		Path instanceFile = findInstanceFile(Path.of(java.net.URI.create(uri)).getParent());

		ExecuteCommandParams defaultsParams = new ExecuteCommandParams();
		defaultsParams.setCommand("aadl.analyze.reachability");
		defaultsParams.setArguments(List.of(new JsonPrimitive(instanceFile.toUri().toString())));

		Object defaultsResult = languageServer.getWorkspaceService().executeCommand(defaultsParams).get();
		Assert.assertTrue("expected string result, got: " + defaultsResult, defaultsResult instanceof String);
		var defaultsLines = nonEmptyLines((String) defaultsResult);
		Assert.assertFalse("expected output: " + defaultsResult, defaultsLines.isEmpty());
		Assert.assertTrue("expected reachability header, got: " + defaultsLines.get(0),
				defaultsLines.get(0).startsWith("Ran mode reachability analysis of "));
		Assert.assertTrue("default command should not emit report paths: " + defaultsLines,
				reportLines(defaultsLines).isEmpty());
		assertDiagnostics(defaultsLines.subList(1, defaultsLines.size()));

		ExecuteCommandParams reportParams = new ExecuteCommandParams();
		reportParams.setCommand("aadl.analyze.reachability");
		reportParams.setArguments(List.of(new JsonPrimitive(instanceFile.toUri().toString()),
				new JsonPrimitive(true), new JsonPrimitive(true), new JsonPrimitive(true)));

		Object reportResult = languageServer.getWorkspaceService().executeCommand(reportParams).get();
		Assert.assertTrue("expected string result, got: " + reportResult, reportResult instanceof String);
		var reportOutputLines = nonEmptyLines((String) reportResult);
		var reports = reportLines(reportOutputLines);
		Assert.assertEquals("expected html, dot, and smv report paths: " + reportResult, 3, reports.size());
		Assert.assertTrue("expected html report: " + reports, reports.stream().anyMatch(l -> l.endsWith(".html")));
		Assert.assertTrue("expected dot report: " + reports, reports.stream().anyMatch(l -> l.endsWith(".dot")));
		Assert.assertTrue("expected smv report: " + reports, reports.stream().anyMatch(l -> l.endsWith(".smv")));
		for (var report : reports) {
			Assert.assertTrue("expected report file on disk: " + report, Files.isRegularFile(Path.of(report)));
		}
		var diagLines = reportOutputLines.stream()
				.filter(l -> !l.startsWith("Ran mode reachability analysis of "))
				.filter(l -> !reports.contains(l))
				.toList();
		assertDiagnostics(diagLines);
	}

	private static Path findInstanceFile(Path root) throws Exception {
		try (var stream = Files.walk(root)) {
			return stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().endsWith(".aaxl2"))
					.findFirst()
					.orElseThrow(() -> new AssertionError("expected .aaxl2 file under " + root));
		}
	}

	private static List<String> nonEmptyLines(String s) {
		var out = new ArrayList<String>();
		for (var line : s.split("\\R")) {
			if (!line.isEmpty()) {
				out.add(line);
			}
		}
		return out;
	}

	private static List<String> reportLines(List<String> lines) {
		return lines.stream()
				.filter(l -> l.endsWith(".html") || l.endsWith(".dot") || l.endsWith(".smv"))
				.toList();
	}

	private static void assertDiagnostics(List<String> diagLines) {
		var diagPattern = Pattern.compile("^.+\\.aaxl2:[^:]+: (error|warning|info|hint): .+$");
		Assert.assertFalse("expected reachability diagnostics", diagLines.isEmpty());
		for (var line : diagLines) {
			Assert.assertTrue("unexpected diagnostic line: " + line, diagPattern.matcher(line).matches());
		}
		Assert.assertTrue("expected unreachable SOM diagnostic, got: " + diagLines,
				diagLines.stream().anyMatch(l -> l.contains("is not reachable")));
	}
}
