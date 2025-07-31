/*******************************************************************************
 * Copyright (c) 2023, 2024 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.internal.core.search.LocatorResponse;
import org.eclipse.jdt.internal.javac.dom.JavacPackageBinding;

public class DOMPackageReferenceLocator extends DOMPatternLocator {

	private PackageReferenceLocator locator;

	public DOMPackageReferenceLocator(PackageReferenceLocator locator) {
		super(locator.pattern);
		this.locator = locator;
	}

	@Override
	public LocatorResponse match(Name node, NodeSetWrapper nodeSet, MatchLocator locator) {
		ASTNode n = node;
		while (n.getParent() instanceof Name parent) {
			n = parent;
		}
		if (n.getParent() instanceof PackageDeclaration) {
			return toResponse(IMPOSSIBLE_MATCH); // only references
		}
		return toResponse(matchesName(this.locator.pattern.pkgName, node.getFullyQualifiedName().toCharArray()) ? POSSIBLE_MATCH :IMPOSSIBLE_MATCH);
	}

	@Override
	public LocatorResponse resolveLevel(org.eclipse.jdt.core.dom.ASTNode node, IBinding binding, MatchLocator locator) {
		if (binding instanceof IPackageBinding ipb) {
			String n = ipb.getName();
			String patternName = new String(this.locator.pattern.pkgName);
			if (patternName.equals(n)) {
				if( this.locator.pattern.focus == null ) {
					// good enough
					return toResponse(ACCURATE_MATCH);
				}

				// We have a focus of a specific package fragment.
				// We want to make sure the class in this node is
				// actually from this fragment, and not a similar package
				// in a different project.
				ASTNode working = node;
				Name fullName = null;
				while( working instanceof Name fn) {
					fullName = fn;
					working = working.getParent();
				}
				if( fullName != null ) {
					String typeName = fullName.toString();
					IJavaElement je = binding instanceof JavacPackageBinding jpb ? jpb.findJavaElementForClass(typeName) : ipb.getJavaElement();
					if( je != null && !this.locator.pattern.focus.equals(je)) {
						return toResponse(IMPOSSIBLE_MATCH);
					}
					// If we can't find a java element, let's give some leeway
					return toResponse(ACCURATE_MATCH);
				}
			}
		}
		if (binding == null) {
			return toResponse(ACCURATE_MATCH);
		}
		return toResponse(IMPOSSIBLE_MATCH);
	}

}
