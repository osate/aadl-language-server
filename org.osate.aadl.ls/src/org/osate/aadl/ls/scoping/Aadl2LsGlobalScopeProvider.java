package org.osate.aadl.ls.scoping;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.LoadOnDemandResourceDescriptions;
import org.eclipse.xtext.scoping.impl.SelectableBasedScope;
import org.osate.aadl2.modelsupport.scoping.EClassGlobalScopeProvider;
import org.osate.pluginsupport.PluginSupportUtil;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class Aadl2LsGlobalScopeProvider extends EClassGlobalScopeProvider {

	@Inject
	private Provider<LoadOnDemandResourceDescriptions> loadOnDemandDescriptions;

	@Override
	protected IScope getScope(Resource context, boolean ignoreCase, EClass type,
			Predicate<IEObjectDescription> filter) {
		final List<URI> contributed = PluginSupportUtil.getContributedAadl();

		final IResourceDescriptions result = getResourceDescriptions(context);
		final LoadOnDemandResourceDescriptions demandDescriptions = loadOnDemandDescriptions.get();
		demandDescriptions.initialize(result, contributed, context);
		IScope scope = SelectableBasedScope.createScope(IScope.NULLSCOPE, demandDescriptions, type, ignoreCase);

		return getScope(scope, context, ignoreCase, type, filter);
	}
}
