/*******************************************************************************
 * Copyright (c) 2004-2026 Carnegie Mellon University and others. (see Contributors file).
 * All Rights Reserved.
 *
 * NO WARRANTY. ALL MATERIAL IS FURNISHED ON AN "AS-IS" BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE
 * OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM USE OF THE MATERIAL.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.osate.aadl.ls.tests.lsp;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.Test;
import org.osate.aadl.ls.AadlLanguageServer;

public class BehaviorAnnexParsingTest extends AbstractAadlLanguageServerTest {

	@Test
	public void reservedTransitionNameAfterValidTransitionProducesDiagnostic() {
		org.junit.Assert.assertTrue(languageServer instanceof AadlLanguageServer);
		initialize();
		String source = """
				package behavior_annex_test
				public

					thread t
					end t;

					thread implementation t.i
						annex behavior_specification {**
							states
								idle: initial complete state;
								transmitting: complete state;
							transitions
								start [2]: idle -[]-> transmitting;
								complete [3]: transmitting -[]-> idle;
						**};
					end t.i;

				end behavior_annex_test;
				""";
		String uri = writeFile("behavior-annex.aadl", source);
		open(uri, source);

		Map<String, List<Diagnostic>> diagnostics = getDiagnostics();
		List<Diagnostic> forFile = diagnostics.getOrDefault(uri, List.of());
		Diagnostic reservedNameError = forFile.stream()
				.filter(d -> d.getSeverity() == DiagnosticSeverity.Error && d.getMessage().contains("'complete'"))
				.findFirst()
				.orElse(null);
		assertNotNull("Expected reserved transition name diagnostic, got: " + forFile, reservedNameError);

		List<String> lines = source.lines().toList();
		int line = 0;
		while (!lines.get(line).contains("complete [3]")) {
			line++;
		}
		int character = lines.get(line).indexOf("complete");
		org.junit.Assert.assertEquals(
				new Range(new Position(line, character), new Position(line, character + "complete".length())),
				reservedNameError.getRange());
	}
}
