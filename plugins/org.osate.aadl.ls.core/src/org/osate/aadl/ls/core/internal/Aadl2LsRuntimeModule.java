package org.osate.aadl.ls.core.internal;

import org.osate.aadl2.modelsupport.scoping.IEClassGlobalScopeProvider;
import org.osate.xtext.aadl2.Aadl2RuntimeModule;

public class Aadl2LsRuntimeModule extends Aadl2RuntimeModule {

	@Override
	public Class<? extends org.eclipse.xtext.scoping.IGlobalScopeProvider> bindIGlobalScopeProvider() {
		return Aadl2LsGlobalScopeProvider.class;
	}

	public Class<? extends IEClassGlobalScopeProvider> bindIEClassGlobalScopeProvider() {
		return Aadl2LsGlobalScopeProvider.class;
	}

}
