package org.osate.aadl.ls.core.internal;

import java.util.Objects;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ide.server.symbol.DocumentSymbolMapper.DocumentSymbolNameProvider;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.osate.aadl2.Aadl2Package;

import com.google.inject.Inject;

public class AadlSymbolNameProvider extends DocumentSymbolNameProvider {

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	IQualifiedNameConverter converter;

	@Override
	public String getName(EObject object) {
		return getName(object != null ? qualifiedNameProvider.getFullyQualifiedName(object) : null, object.eClass());
	}

	@Override
	public String getName(IEObjectDescription description) {
		return getName(description != null ? description.getName() : null, description.getEClass());
	}

	private String getName(QualifiedName qualifiedName, EClass eclass) {
		if (qualifiedName != null) {
			String name;
			if (Objects.equals(eclass, Aadl2Package.eINSTANCE.getAadlPackage())) {
				name = converter.toString(qualifiedName);
			} else {
				name = qualifiedName.getLastSegment();
				if (name.endsWith("_public")) {
					name = "public";
				} else if (name.endsWith("_private")) {
					name = "private";
				}
			}
			return name;
		}
		return null;
	}

}
