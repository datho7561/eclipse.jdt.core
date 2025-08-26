/*******************************************************************************
 * Copyright (c) 2025, Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.core.tests.javac;

import org.eclipse.jdt.core.tests.model.AbstractJavaSearchTests;
import org.eclipse.jdt.core.tests.model.JavaSearchBugsTests;
import org.eclipse.jdt.core.tests.model.JavaSearchBugsTests2;
import org.eclipse.jdt.core.tests.model.RunJavaSearchTests;

import junit.framework.Test;

public class RunSearchJavacTests extends RunJavaSearchTests {

	public RunSearchJavacTests(String name) {
		super(name);
	}

	public static Test suite() {
		Test res = RunJavaSearchTests.suite();
		// Adds the 2 other suites that requires JavaSearch and JavaSearch15 test projects to run well
		AbstractJavaSearchTests.JAVA_SEARCH_SUITES.add(JavaSearchBugsTests.class);
		AbstractJavaSearchTests.JAVA_SEARCH_SUITES.add(JavaSearchBugsTests2.class);
		return res;
	}

}

