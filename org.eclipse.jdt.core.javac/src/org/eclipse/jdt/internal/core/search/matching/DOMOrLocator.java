/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.matching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.internal.core.search.DOMPatternLocatorFactory;
import org.eclipse.jdt.internal.core.search.LocatorResponse;

public class DOMOrLocator extends DOMPatternLocator {

	private final DOMPatternLocator[] children;
	private List<DOMPatternLocator> currentlyMatching = List.of();

	public DOMOrLocator(OrLocator orLocator, OrPattern pattern) {
		super(pattern);
		children = Arrays.stream(children(orLocator))
			.map(child -> DOMPatternLocatorFactory.createWrapper(child, null)) // usually pattern would be inferred from locator so we can pass null
			.toArray(DOMPatternLocator[]::new);
	}

	private static PatternLocator[] children(OrLocator orLocator) {
		try {
			var locatorsField = OrLocator.class.getDeclaredField("patternLocators");
			locatorsField.setAccessible(true);
			return (PatternLocator[])locatorsField.get(orLocator);
		} catch (Exception ex) {
			ILog.get().error(ex.getMessage(), ex);
			return new PatternLocator[0];
		}
	}

	public LocatorResponse match(org.eclipse.jdt.core.dom.Annotation node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(org.eclipse.jdt.core.dom.ASTNode node, NodeSetWrapper nodeSet, MatchLocator locator) { // needed for some generic nodes
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(org.eclipse.jdt.core.dom.Expression node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(org.eclipse.jdt.core.dom.FieldDeclaration node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(org.eclipse.jdt.core.dom.LambdaExpression node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(VariableDeclaration node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(org.eclipse.jdt.core.dom.MethodDeclaration node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(org.eclipse.jdt.core.dom.MemberValuePair node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(MethodInvocation node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(org.eclipse.jdt.core.dom.ModuleDeclaration node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(Name node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(FieldAccess node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(AbstractTypeDeclaration node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(org.eclipse.jdt.core.dom.TypeParameter node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse match(Type node, NodeSetWrapper nodeSet, MatchLocator locator) {
		return or(child -> child.match(node, nodeSet, locator));
	}
	public LocatorResponse resolveLevel(org.eclipse.jdt.core.dom.ASTNode node, MatchLocator locator) {
		return or(child -> child.resolveLevel(node, locator), this.currentlyMatching);
	}
	public LocatorResponse resolveLevel(org.eclipse.jdt.core.dom.ASTNode node, IBinding binding, MatchLocator locator) {
		return or(child -> child.resolveLevel(node, binding, locator), this.currentlyMatching);
	}

	private static LocatorResponse or(java.util.function.Function<DOMPatternLocator, LocatorResponse> query, Collection<DOMPatternLocator> locators) {
		return locators.stream()
					.map(query::apply)
					.max(Comparator.comparingInt(LocatorResponse::level))
					.orElse(toResponse(PatternLocator.IMPOSSIBLE_MATCH));
	}

	private LocatorResponse or(java.util.function.Function<DOMPatternLocator, LocatorResponse> query) {
		List<DOMPatternLocator> matching = new ArrayList<>();
		LocatorResponse best = toResponse(PatternLocator.IMPOSSIBLE_MATCH);
		for (DOMPatternLocator child : this.children) {
			var current = query.apply(child);
			if (current.level() != PatternLocator.IMPOSSIBLE_MATCH) {
				matching.add(child);
			}
			if (current.level() > best.level()) {
				best = current;
			}
		}
		this.currentlyMatching = matching;
		return best;
	}

	@Override
	public void initializePolymorphicSearch(MatchLocator locator) {
		for (PatternLocator patternLocator : this.children)
			patternLocator.initializePolymorphicSearch(locator);
	}

}
