/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 */
package org.eclipse.daanse.olap.testkit.function.contracts;

import static org.eclipse.daanse.olap.api.DataType.DIMENSION;
import static org.eclipse.daanse.olap.api.DataType.HIERARCHY;
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.mdx.model.api.expression.operation.PrefixOperationAtom;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.set.existing.ExistingFunDef;

/**
 * The contract of the MDX prefix operator {@code EXISTING <Set>} ({@code ExistingFunDef} via
 * {@code ExistingResolver}, a {@code PrefixOperationAtom}, not a {@code FunctionOperationAtom}
 * — there is no {@code Existing(...)} call syntax). {@code FunctionPrinter}'s prefix-atom case
 * renders the declaration as {@code "Existing <Set>"}, with no leading return-category (unlike
 * the default function-call rendering).
 *
 * <p>{@code ExistingCalc.dependsOn(Hierarchy)} is overridden to {@code
 * myType.usesHierarchy(hierarchy, false)} — the <em>declared type</em> of the Set argument,
 * i.e. only its own hierarchy/hierarchies. At evaluation time, though, {@code
 * evaluateInternal} filters the set against {@code evaluator.getMembers()} — the current
 * member of <em>every</em> hierarchy in context, not just the argument's own. The value cases
 * below only probe the case where that wider context is exactly the cube's default members
 * (an ancestor of everything), where the two views agree and the set passes through
 * unchanged; they do not exercise a slicer-restricted context.
 */
public final class ExistingContract {

    private ExistingContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Existing")
            .atom(PrefixOperationAtom.class)
            .signatures("Existing <Set>")
            .returns(SET)
            .arity(1, 1)

            .resolvesTo(ExistingFunDef.class, SET)
            .resolvesWithCost(2, ExistingFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, ExistingFunDef.class, TUPLE)    // Tuple -> Set
            .resolvesWithCost(1, ExistingFunDef.class, LEVEL)    // Level -> Set
            .rejects(DIMENSION)     // Dimension converts to Member/Hierarchy/Level, never Set
            .rejects(HIERARCHY)     // Hierarchy converts to Member/Dimension/Tuple, never Set
            .rejects(NUMERIC)
            .rejects()               // arity 0
            .rejects(SET, SET)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty set",   "EXISTING {}")
            .edgeCaseMdx("full set",    "EXISTING [Gender].Members")
            .edgeCaseMdx("single member", "EXISTING {[Gender].[F]}")

            // Outside a slicer/subselect that actually restricts a hierarchy, the current
            // member of every hierarchy in context is its default (usually the All member,
            // an ancestor of everything), so nothing is filtered out.
            .value("Count(EXISTING [Gender].Members)", "2")
            .value("SetToStr(EXISTING [Gender].Members)", "{[Gender].[F], [Gender].[M]}")
            .value("Count(EXISTING {})", "0")

            .dependsOn("EXISTING [Gender].Members", "[Gender].[Gender]")

            .resultStyle("EXISTING [Gender].Members", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("EXISTING [Gender].Members", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("EXISTING [Gender].Members")

            .build();
}
