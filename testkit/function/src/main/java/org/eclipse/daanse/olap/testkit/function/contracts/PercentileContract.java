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
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.olap.function.def.percentile.PercentileFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Percentile(<Set>, <Numeric Expression>, <Numeric
 * Expression Percent>)} — the value of the tuple at a given percentile of a set. {@code
 * PercentileResolver} is an {@code AbstractFunctionDefinitionMultiResolver} over a single
 * declared {@code FunctionMetaData} with all three parameters required (unlike {@link
 * MedianContract}'s trailing-optional value expression) — there is no arity ambiguity to guard
 * against here.
 *
 * <p>{@code PercentileFunDef.compileCall} returns an anonymous {@code PercentileCalc} subclass
 * that overrides {@code evaluateInternal}/{@code dependsOn} to duplicate exactly what the base
 * {@code PercentileCalc} class already does — the base class is never actually instantiated as
 * itself, only subclassed inline. Harmless (both do the identical, correct thing) and out of
 * scope for this contract to clean up — the same shape of redundancy {@link ParentContract}/
 * {@link DataMemberContract} document for their own sibling Calcs.
 *
 * <p>{@code PercentileCalc.evaluateInternal} scales the third argument from a {@code 0..100}
 * percent into the {@code 0.0..1.0} fraction {@code FunUtil.percentile} expects ({@code percent
 * * 0.01}), then delegates to the exact same {@code FunUtil.percentile} helper {@link
 * MedianContract} documents (hardcoded to {@code p = 0.5} there). {@code dependsOn} delegates
 * to {@code HierarchyDependsChecker.checkAnyDependsButFirst} — the same "depends on whatever
 * the value-expression/percent depend on, except the set's own hierarchy" rule {@link
 * AggregateContract}/{@link MedianContract} already establish.
 *
 * <p>{@code FunUtil.percentile} clamps {@code p <= 0.0} to the sorted minimum and {@code p >=
 * 1.0} to the sorted maximum, so {@code Percentile(set, expr, 0)}/{@code Percentile(set, expr,
 * 100)} equal {@link MinContract}/{@link MaxContract}'s minimum/maximum, and {@code
 * Percentile(set, expr, 50)} is exactly {@link MedianContract}'s median (the arithmetic mean of
 * the two middle values for an even-length set) — all exercised below via the same {@code
 * [Gender].[Gender].[All Gender].Children} membership (F maps to {@code 1}, M maps to {@code
 * 2}) so no real fact-table figure needs to be known. The percentile of a constant expression
 * is that constant itself, at any percent, over any non-empty set.
 */
public final class PercentileContract {

    private PercentileContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Percentile")
            .signatures("<Numeric Expression> Percentile(<Set>, <Numeric Expression>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(3, 3)

            .resolvesTo(PercentileFunDef.class, SET, NUMERIC, NUMERIC)
            .resolvesWithCost(1, PercentileFunDef.class, LEVEL, NUMERIC, NUMERIC)    // Level -> Set
            .resolvesWithCost(2, PercentileFunDef.class, MEMBER, NUMERIC, NUMERIC)   // Member -> Set
            .resolvesWithCost(2, PercentileFunDef.class, TUPLE, NUMERIC, NUMERIC)    // Tuple -> Set
            .rejects(DIMENSION, NUMERIC, NUMERIC)   // Dimension does not convert to Set
            .rejects(HIERARCHY, NUMERIC, NUMERIC)   // Hierarchy does not convert to Set
            .rejects(NUMERIC, NUMERIC, NUMERIC)
            .rejects(STRING, NUMERIC, NUMERIC)

            .resolvesWithCost(3, PercentileFunDef.class, SET, MEMBER, NUMERIC)   // Member -> Numeric
            .resolvesWithCost(3, PercentileFunDef.class, SET, TUPLE, NUMERIC)    // Tuple -> Numeric
            .rejects(SET, SET, NUMERIC)
            .rejects(SET, STRING, NUMERIC)
            .rejects(SET, LEVEL, NUMERIC)
            .rejects(SET, HIERARCHY, NUMERIC)
            .rejects(SET, DIMENSION, NUMERIC)

            .resolvesWithCost(3, PercentileFunDef.class, SET, NUMERIC, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, PercentileFunDef.class, SET, NUMERIC, TUPLE)    // Tuple -> Numeric
            .rejects(SET, NUMERIC, SET)
            .rejects(SET, NUMERIC, STRING)
            .rejects(SET, NUMERIC, LEVEL)
            .rejects(SET, NUMERIC, HIERARCHY)
            .rejects(SET, NUMERIC, DIMENSION)

            .rejects()                                          // arity 0
            .rejects(SET)                                        // arity 1
            .rejects(SET, NUMERIC)                                // arity 2
            .rejects(SET, NUMERIC, NUMERIC, NUMERIC)             // arity 4

            .autoEdgeCases()
            .edgeCaseMdx("minimum (percent 0)",
                    "Percentile([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], 0)")
            .edgeCaseMdx("median (percent 50)",
                    "Percentile([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], 50)")
            .edgeCaseMdx("maximum (percent 100)",
                    "Percentile([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], 100)")
            .edgeCaseMdx("percent NULL", "Percentile([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales], NULL)")
            .edgeCaseMdx("empty set", "Percentile({}, [Measures].[Unit Sales], 50)")

            // The percentile of a constant expression over any non-empty set is the constant
            // itself, at any percent.
            .value("Percentile([Gender].[Gender].[All Gender].Children, 5, 50)", "5")
            // F maps to 1, M maps to 2 (see the class Javadoc): percent 0/50/100 give the
            // sorted minimum/median/maximum — no real fact-table figure needs to be known.
            .value("Percentile([Gender].[Gender].[All Gender].Children, "
                    + "IIf([Gender].CurrentMember IS [Gender].[F], 1, 2), 0)", "1")
            .value("Percentile([Gender].[Gender].[All Gender].Children, "
                    + "IIf([Gender].CurrentMember IS [Gender].[F], 1, 2), 50)", "1.5")
            .value("Percentile([Gender].[Gender].[All Gender].Children, "
                    + "IIf([Gender].CurrentMember IS [Gender].[F], 1, 2), 100)", "2")
            .valueIsNull("Percentile([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales], NULL)")

            .scalarDependsOn("Percentile([Gender].[Gender].[All Gender].Children, 5, 50)")
            .scalarDoesNotDependOn("Percentile([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales], 50)", "[Gender].[Gender]", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
