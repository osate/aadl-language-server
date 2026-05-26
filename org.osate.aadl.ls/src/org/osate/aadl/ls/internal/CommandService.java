package org.osate.aadl.ls.internal;

import java.util.List;
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
						var pkg = (AadlPackage) it.getResource().getContents().get(0);
						var pub = pkg.getOwnedPublicSection();
						ComponentImplementation ci = null;
						if (pub != null) {
							for (var cls : pub.getOwnedClassifiers()) {
								if (cls instanceof ComponentImplementation) {
									if (name.endsWith(cls.getName())) {
										ci = (ComponentImplementation) cls;
									}
								}
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
									result.append(d.kind);
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
				var uri = URI.createURI(iuri);
				Resource res = new ResourceSetImpl().getResource(uri, true);
				SystemInstance instance = (SystemInstance) res.getContents().get(0);
				var duri = instance.getComponentImplementation().eResource().getURI();
				try {
					return access.doRead(duri.toString(), (ILanguageServerAccess.Context it) -> {
						var resource = (new ResourceSetImpl()).getResource(uri, true);
						EList<EObject> rl = resource.getContents();

						if (!rl.isEmpty() && rl.get(0) instanceof Element) {
							var inst = (SystemInstance) rl.get(0);
							var checker = new FlowLatencyAnalysisSwitch(inst);
							checker.invokeAndSaveResult(inst, null, true, true, true, true, false);
						}
						return "Ran latency analysis of " + iuri;
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

}
