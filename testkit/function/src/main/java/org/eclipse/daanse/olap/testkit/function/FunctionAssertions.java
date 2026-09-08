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

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;

/** Entry points for function contract assertions. */
public final class FunctionAssertions {

    private FunctionAssertions() {
    }

    /**
     * Starts an assertion for the function registered under {@code name}.
     *
     * <p>Fails fast when the name is registered under more than one kind of
     * {@link OperationAtom} — {@code Members} exists both as a plain property
     * ({@code <Level>.Members}) and as a method ({@code <Set>.Members(...)}), and they are
     * distinct registrations.
     */
    public static FunctionAssert assertThatFunction(FunctionService functionService, String name) {
        List<OperationAtom> atoms = FunctionRegistryIndex.atomsNamed(functionService, name);
        if (atoms.size() > 1) {
            throw AssertionMessagesSupport.ambiguousName(functionService, name, atoms);
        }
        return new FunctionAssert(functionService,
                atoms.isEmpty() ? new FunctionOperationAtom(name) : atoms.get(0));
    }

    /** Starts an assertion for a specific atom — use when the name alone is ambiguous. */
    public static FunctionAssert assertThatFunction(FunctionService functionService, OperationAtom atom) {
        return new FunctionAssert(functionService, atom);
    }

    /** Starts an assertion for {@code name} under a specific atom kind. */
    public static FunctionAssert assertThatFunction(FunctionService functionService, String name,
            Class<? extends OperationAtom> atomClass) {
        OperationAtom atom = FunctionRegistryIndex.atomsNamed(functionService, name).stream()
                .filter(a -> atomClass.isInstance(a))
                .findFirst()
                .orElseGet(() -> FunctionRegistryIndex.syntheticAtom(name, atomClass));
        return new FunctionAssert(functionService, atom);
    }

    /** Starts a registry-wide assertion. */
    public static FunctionRegistryAssert assertThatFunctions(FunctionService functionService) {
        return new FunctionRegistryAssert(functionService);
    }

    /** All resolvers registered under an atom, never null. */
    static List<FunctionResolver> resolversOf(FunctionService functionService, OperationAtom atom) {
        return functionService.getResolvers(atom);
    }
}