package org.osate.aadl.ls.tests;

import org.eclipse.xtext.util.Modules2;
import org.osate.aadl.ls.internal.Aadl2LsIdeModule;
import org.osate.aadl.ls.internal.Aadl2LsRuntimeModule;
import org.osate.aadl.ls.internal.Aadl2LsSetup;
import org.osate.testsupport.Aadl2InjectorProvider;
import org.osate.xtext.aadl2.Aadl2RuntimeModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class AadlLsInjectorProvider extends Aadl2InjectorProvider {

	@Override
	protected Injector internalCreateInjector() {
		return new Aadl2LsSetup() {
			@Override
			public Injector createInjector() {
				Aadl2RuntimeModule runtime = new Aadl2LsRuntimeModule() {
					@Override
					public ClassLoader bindClassLoaderToInstance() {
						return AadlLsInjectorProvider.class.getClassLoader();
					}
				};
				return Guice.createInjector(Modules2.mixin(runtime, new Aadl2LsIdeModule()));
			}
		}.createInjectorAndDoEMFRegistration();
	}
}
