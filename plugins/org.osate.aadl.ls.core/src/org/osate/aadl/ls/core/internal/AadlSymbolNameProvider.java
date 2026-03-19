package org.osate.aadl.ls.core.internal;

import org.eclipse.xtext.ide.server.symbol.DocumentSymbolMapper.DocumentSymbolNameProvider;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;

import com.google.inject.Inject;

public class AadlSymbolNameProvider extends DocumentSymbolNameProvider {

	@Inject
	IQualifiedNameConverter converter;

	@Override
	protected String getName(QualifiedName qualifiedName) {
		if (qualifiedName != null) {
			return converter.toString(qualifiedName);
		}
		return null;
	}

}
