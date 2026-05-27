package org.osate.aadl.ls.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.xtext.ide.server.ILanguageServerAccess;
import org.eclipse.xtext.ide.server.commands.IExecutableCommandService;
import org.eclipse.xtext.util.CancelIndicator;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.modelsupport.errorreporting.AnalysisErrorReporterManager;
import org.osate.aadl2.modelsupport.errorreporting.QueuingAnalysisErrorReporter;
import org.osate.analysis.flows.FlowLatencyAnalysisSwitch;
import org.osate.result.AnalysisResult;
import org.osate.result.Result;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonPrimitive;

public class CommandService implements IExecutableCommandService {
	@Override
	public List<String> initialize() {
		return Lists.newArrayList("aadl.instantiate", "aadl.analyze.latency");
	}

	@Override
	public Object execute(ExecuteCommandParams params, ILanguageServerAccess access, CancelIndicator cancelIndicator) {
		if ("aadl.instantiate".equals(params.getCommand())) {
			var arg1 = (JsonPrimitive) Iterables.getFirst(params.getArguments(), null);
			var arg2 = (JsonPrimitive) Iterables.get(params.getArguments(), 1, null);
			String uri = arg1.getAsString();
			String name = arg2.getAsString();
			if (uri != null) {
				try {
					return access.doRead(uri, (ILanguageServerAccess.Context it) -> {
						var contents = it.getResource().getContents();
						if (contents.isEmpty() || !(contents.get(0) instanceof AadlPackage pkg)) {
							return "Error: " + uri + " does not contain an AADL package";
						}
						int sep = name.lastIndexOf("::");
						String simpleName = sep < 0 ? name : name.substring(sep + 2);
						String prefix = sep < 0 ? null : name.substring(0, sep);
						if (prefix != null && !prefix.equalsIgnoreCase(pkg.getName())) {
							return "Error: component implementation " + name + " not found.";
						}
						ComponentImplementation ci = null;
						var sections = new java.util.ArrayList<org.osate.aadl2.PackageSection>();
						if (pkg.getOwnedPublicSection() != null) {
							sections.add(pkg.getOwnedPublicSection());
						}
						if (pkg.getOwnedPrivateSection() != null) {
							sections.add(pkg.getOwnedPrivateSection());
						}
						for (var section : sections) {
							for (var cls : section.getOwnedClassifiers()) {
								if (cls instanceof ComponentImplementation && simpleName.equalsIgnoreCase(cls.getName())) {
									ci = (ComponentImplementation) cls;
									break;
								}
							}
							if (ci != null) {
								break;
							}
						}
						if (ci != null) {
							var result = new StringBuilder();
							try {
								var errorManager = new AnalysisErrorReporterManager(
										QueuingAnalysisErrorReporter.factory);
								var si = InstantiateModel.instantiate(ci, errorManager);
								si.eResource().save(null);
								result.append("Instantiated " + name + " as " + si.getName() + "\n");

								var diags = ((QueuingAnalysisErrorReporter) errorManager.getReporter(si.eResource()))
										.getErrors();
								for (var d : diags) {
									result.append(si.eResource().getURI().toFileString().replaceAll("^/+", "/"));
									result.append(':');
									var e = d.where;
									while (Objects.nonNull(e) && !(e instanceof InstanceObject)) {
										e = e.getOwner();
									}
									var io = (InstanceObject)e;
									result.append(io.getComponentInstancePath());
									result.append(": ");
									result.append(d.kind.toLowerCase(Locale.ROOT));
									result.append(": ");
									result.append(d.message);
									result.append('\n');
								}
							} catch (Exception e) {
								result.append("Exception: " + e.getMessage());
								e.printStackTrace();
							}
							return result.toString();
						}
						return "Error: component implementation " + name + " not found.";
					}).get();
				} catch (InterruptedException | ExecutionException e) {
					return e.getMessage();
				}
			} else {
				return "Param Uri Missing";
			}
		} else if ("aadl.analyze.latency".equals(params.getCommand())) {
			var arg1 = (JsonPrimitive) Iterables.getFirst(params.getArguments(), null);
			String iuri = arg1.getAsString();
			if (iuri != null) {
				var args = params.getArguments();
				boolean asynchronousSystem = optBool(args, 1, true);
				boolean majorFrameDelay = optBool(args, 2, true);
				boolean worstCaseDeadline = optBool(args, 3, true);
				boolean bestCaseEmptyQueue = optBool(args, 4, true);
				boolean disableQueuingLatency = optBool(args, 5, false);

				var uri = URI.createURI(iuri);
				Resource res = new ResourceSetImpl().getResource(uri, true);
				SystemInstance instance = (SystemInstance) res.getContents().get(0);
				var duri = instance.getComponentImplementation().eResource().getURI();
				try {
					return access.doRead(duri.toString(), (ILanguageServerAccess.Context it) -> {
						var resource = (new ResourceSetImpl()).getResource(uri, true);
						EList<EObject> rl = resource.getContents();

						var output = new StringBuilder();
						output.append("Ran latency analysis of ").append(iuri).append('\n');
						if (!rl.isEmpty() && rl.get(0) instanceof Element) {
							var inst = (SystemInstance) rl.get(0);
							var checker = new FlowLatencyAnalysisSwitch(inst);
							AnalysisResult ar = checker.invokeAndSaveResult(inst, null, asynchronousSystem,
									majorFrameDelay, worstCaseDeadline, bestCaseEmptyQueue, disableQueuingLatency);

							var resultURI = ar.eResource().getURI();
							var csvURI = resultURI.trimFileExtension().appendFileExtension("csv");
							output.append(resultURI.toFileString().replaceAll("^/+", "/")).append('\n');
							output.append(csvURI.toFileString().replaceAll("^/+", "/")).append('\n');

							var instancePath = inst.eResource().getURI().toFileString().replaceAll("^/+", "/");
							var diagLines = new ArrayList<String>();
							for (Result r : ar.getResults()) {
								collectDiagnostics(r, instancePath, diagLines);
							}
							Collections.sort(diagLines);
							for (var line : diagLines) {
								output.append(line).append('\n');
							}
						}
						return output.toString();
					}).get();
				} catch (InterruptedException | ExecutionException e) {
					return e.getMessage();
				}
			} else {
				return "Param Uri Missing";
			}
		}
		return "Bad Command";
	}

	private static boolean optBool(List<Object> args, int index, boolean defaultValue) {
		if (args == null || index >= args.size()) {
			return defaultValue;
		}
		var arg = args.get(index);
		if (arg instanceof JsonPrimitive p && p.isBoolean()) {
			return p.getAsBoolean();
		}
		return defaultValue;
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
