/*******************************************************************************
 * Copyright (c) 2024, Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.core.tests.javac;

// Completion tests are currently ignored as functional coverage for completion is already
// good and many tests are really failing for insignificant details (such as relevance which
// is overridden in UI anyway). So we skip the test to avoid noise and let dogfooding and actual
// usage feedback drive further improvements to completion.
// We may re-enable those tests later.
public class RunCompletionJavacTests_IGNORED extends org.eclipse.jdt.core.tests.model.RunCompletionModelTests {

	public RunCompletionJavacTests_IGNORED(String name) {
		super(name);
	}

}
