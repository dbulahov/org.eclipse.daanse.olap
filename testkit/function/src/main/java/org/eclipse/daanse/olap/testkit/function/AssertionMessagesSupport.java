/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.testkit.function;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.function.FunctionService;

/** Support for {@link FunctionAssertions#assertThatFunction(FunctionService, String)}. */
public class AssertionMessagesSupport {

	private static final String NL = System.lineSeparator();

	private AssertionMessagesSupport() {
	}

	/**
	 * A name that resolves to more than one kind of {@link OperationAtom} — for example
	 * {@code Members} exists both as a plain property ({@code <Level>.Members}) and as a
	 * method ({@code <Set>.Members(...)}), and they are distinct registrations.
	 * {@code assertThatFunction(functionService, name)} cannot pick one on its own; the
	 * caller must disambiguate by atom kind.
	 */
	public static RuntimeException ambiguousName(FunctionService functionService, String name,
			List<OperationAtom> atoms) {
		String atomList = atoms.stream().map(atom -> "  " + SignatureText.ofAtom(atom))
				.collect(Collectors.joining(NL));
		return new IllegalStateException("\"" + name + "\" is ambiguous: it is registered under " + atoms.size()
				+ " different kinds of atom:" + NL + atomList + NL
				+ "Use assertThatFunction(functionService, \"" + name
				+ "\", <OperationAtom subclass>.class) to pick one, "
				+ "or assertThatFunction(functionService, atom) with the exact atom.");
	}
}
