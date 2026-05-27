package org.osate.aadl.ls.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.xtext.ide.server.ILanguageServerAccess;
import org.eclipse.xtext.util.CancelIndicator;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.analysis.flows.FlowLatencyAnalysisSwitch;
import org.osate.result.AnalysisResult;
import org.osate.result.Result;

import com.google.common.collect.Iterables;
import com.google.gson.JsonPrimitive;

final class AnalyzeLatencyCommand implements Command {

	static final String NAME = "aadl.analyze.latency";

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
		var args = params.getArguments();
		boolean asynchronousSystem = CommandUtil.optBool(args, 1, true);
		boolean majorFrameDelay = CommandUtil.optBool(args, 2, true);
		boolean worstCaseDeadline = CommandUtil.optBool(args, 3, true);
		boolean bestCaseEmptyQueue = CommandUtil.optBool(args, 4, true);
		boolean disableQueuingLatency = CommandUtil.optBool(args, 5, false);

		var uri = URI.createURI(iuri);
		Resource res = new ResourceSetImpl().getResource(uri, true);
		SystemInstance instance = (SystemInstance) res.getContents().get(0);
		var duri = instance.getComponentImplementation().eResource().getURI();
		try {
			return access.doRead(duri.toString(), ctx -> runAnalysis(uri, iuri, asynchronousSystem, majorFrameDelay,
					worstCaseDeadline, bestCaseEmptyQueue, disableQueuingLatency)).get();
		} catch (InterruptedException | ExecutionException e) {
			return e.getMessage();
		}
	}

	private static String runAnalysis(URI uri, String iuri, boolean asynchronousSystem, boolean majorFrameDelay,
			boolean worstCaseDeadline, boolean bestCaseEmptyQueue, boolean disableQueuingLatency) {
		var resource = new ResourceSetImpl().getResource(uri, true);
		EList<EObject> rl = resource.getContents();

		var output = new StringBuilder();
		output.append("Ran latency analysis of ").append(iuri).append('\n');
		if (rl.isEmpty() || !(rl.get(0) instanceof Element)) {
			return output.toString();
		}
		var inst = (SystemInstance) rl.get(0);
		var checker = new FlowLatencyAnalysisSwitch(inst);
		AnalysisResult ar = checker.invokeAndSaveResult(inst, null, asynchronousSystem, majorFrameDelay,
				worstCaseDeadline, bestCaseEmptyQueue, disableQueuingLatency);

		var resultURI = ar.eResource().getURI();
		var csvURI = resultURI.trimFileExtension().appendFileExtension("csv");
		output.append(CommandUtil.toFsPath(resultURI)).append('\n');
		output.append(CommandUtil.toFsPath(csvURI)).append('\n');

		var instancePath = CommandUtil.toFsPath(inst.eResource().getURI());
		var diagLines = new ArrayList<String>();
		for (Result r : ar.getResults()) {
			collectDiagnostics(r, instancePath, diagLines);
		}
		Collections.sort(diagLines);
		for (var line : diagLines) {
			output.append(line).append('\n');
		}
		return output.toString();
	}

	private static void collectDiagnostics(Result r, String instancePath, List<String> lines) {
		String elementPath = "<unknown>";
		var modelElement = r.getModelElement();
		if (modelElement instanceof InstanceObject io) {
			elementPath = io.getComponentInstancePath();
		}
		for (var d : r.getDiagnostics()) {
			var path = elementPath;
			var diagElement = d.getModelElement();
			if (diagElement instanceof InstanceObject io) {
				path = io.getComponentInstancePath();
			}
			lines.add(instancePath + ":" + path + ": "
					+ d.getDiagnosticType().getName().toLowerCase(Locale.ROOT)
					+ ": " + d.getMessage());
		}
		for (var sub : r.getSubResults()) {
			collectDiagnostics(sub, instancePath, lines);
		}
	}
}
