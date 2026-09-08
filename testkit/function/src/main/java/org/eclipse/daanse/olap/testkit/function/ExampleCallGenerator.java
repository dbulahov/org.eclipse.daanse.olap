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

import org.eclipse.daanse.mdx.model.api.expression.operation.*;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionParameter;

/**
 * Builds an example call for a declared overload — the seed of a minimal contract.
 *
 * <p>The call is syntactically valid, not semantically sensible: for
 * {@code ParallelPeriod([Dim].[Level], 2, [Dim].[Level].[A])} on a non-time hierarchy the
 * generator happily emits nonsense. That is enough for promise 3 ("it resolves") and not
 * enough for promise 5 ("it returns the right value").
 */
public final class ExampleCallGenerator {

    private ExampleCallGenerator() {
    }

    /** The argument categories of a canonical call to this overload. */
    public static DataType[] argumentsFor(FunctionMetaData metaData) {
        java.util.List<DataType> args = new java.util.ArrayList<>();
        int emitted = 0;
        int minArity = metaData.minArity();
        int countedGroup = -1;

        for (FunctionParameter parameter : metaData.parameters()) {
            if (parameter.repeatGroup() > 0) {
                // emit a repeat group exactly twice, so the group logic is exercised
                if (parameter.repeatGroup() != countedGroup) {
                    countedGroup = parameter.repeatGroup();
                    args.add(parameter.dataType());
                    args.add(parameter.dataType());
                    emitted += 2;
                }
                continue;
            }
            if ((parameter.optional() || parameter.skippable()) && emitted >= minArity) {
                continue;   // trailing optionals stay out of the canonical call
            }
            args.add(parameter.dataType());
            emitted++;
        }
        return args.toArray(DataType[]::new);
    }

    public static String mdxShapeOf(FunctionMetaData metaData, String... argumentTexts) {
        OperationAtom atom = metaData.operationAtom();
        String name = atom.name();
        java.util.List<String> a = java.util.List.of(argumentTexts);
        return switch (atom) {
        case FunctionOperationAtom ignored      -> name + "(" + String.join(", ", a) + ")";
        case MethodOperationAtom ignored        -> a.get(0) + "." + name + "("
                                                     + String.join(", ", a.subList(1, a.size())) + ")";
        case PlainPropertyOperationAtom ignored -> a.get(0) + "." + name;
        case InfixOperationAtom ignored         -> a.get(0) + " " + name + " " + a.get(1);
        case PrefixOperationAtom ignored        -> name + " " + a.get(0);
        case PostfixOperationAtom ignored       -> a.get(0) + " " + name;
        case BracesOperationAtom ignored        -> "{" + String.join(", ", a) + "}";
        case ParenthesesOperationAtom ignored   -> "(" + String.join(", ", a) + ")";
        default -> throw new IllegalArgumentException(
                "no example shape for " + atom.getClass().getSimpleName()
                        + " — Cast, Case and Empty need a hand-written contract");
        };
    }
}