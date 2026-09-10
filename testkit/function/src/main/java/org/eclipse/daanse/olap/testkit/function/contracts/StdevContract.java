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

import org.eclipse.daanse.olap.function.def.stdev.StdevFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Stdev(<Set>[, <Numeric Expression>])}, the unbiased
 * (sample, divide by {@code n - 1}) standard deviation of a numeric expression evaluated over a
 * set (the expression defaults to the current measure when omitted). {@code StdevResolver} is
 * an {@code AbstractFunctionDefinitionMultiResolver} over a single declared {@code
 * FunctionMetaData} with {@code SET} required and one trailing optional {@code NUMERIC} — the
 * same inherently-safe shape {@link AggregateContract}/{@link MaxContract} document. {@code
 * StddevResolver} declares the same call shape under the {@code Stddev} atom, sharing this same
 * {@code StdevFunDef} class verbatim (see {@link StddevContract}).
 *
 * <p>{@code StdevCalc.evaluateInternal} delegates to {@code FunUtil.stdev(evaluator, list,
 * calc, false)}, which itself delegates to {@code FunUtil.var(..., biased = false)}: the sum of
 * squared deviations from the mean, divided by {@code n - 1} (not {@code n} — see {@link
 * StdevPContract} for the biased sibling), then square-rooted. {@code dependsOn} delegates to
 * {@code HierarchyDependsChecker.checkAnyDependsButFirst}, the same "depends on whatever the
 * value-expression depends on, except the set's own hierarchy" rule {@link AggregateContract}
 * already establishes.
 *
 * <p>The standard deviation of a constant numeric expression over any non-empty set (with two
 * or more members) is exactly {@code 0} — every deviation from the mean is {@code 0} — so this
 * does not depend on any real fact-table figure. A singleton set divides by {@code n - 1 = 0}
 * (see the edge case below); {@code FunUtil.var} does not special-case it, so the result is
 * whatever Java's {@code 0.0 / 0} floating-point division produces ({@code NaN}) rather than a
 * diagnosed exception — surfaced here as an edge case, not asserted as a specific value.
 */
public final class StdevContract {

    private StdevContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Stdev")
            .signatures("<Numeric Expression> Stdev(<Set>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(1, 2)

            .resolvesTo(StdevFunDef.class, SET)
            .resolvesWithCost(1, StdevFunDef.class, LEVEL)    // Level -> Set
            .resolvesWithCost(2, StdevFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, StdevFunDef.class, TUPLE)    // Tuple -> Set
            .rejects(DIMENSION)   // Dimension does not convert to Set
            .rejects(HIERARCHY)   // Hierarchy does not convert to Set
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                    // arity 0

            .resolvesTo(StdevFunDef.class, SET, NUMERIC)
            .resolvesWithCost(3, StdevFunDef.class, SET, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, StdevFunDef.class, SET, TUPLE)    // Tuple -> Numeric
            .rejects(SET, SET)
            .rejects(SET, STRING)
            .rejects(SET, LEVEL)
            .rejects(SET, HIERARCHY)
            .rejects(SET, DIMENSION)
            .rejects(SET, NUMERIC, NUMERIC)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("set only (implicit current measure)",
                    "Stdev([Gender].[Gender].[All Gender].Children)")
            .edgeCaseMdx("set and explicit numeric expression",
                    "Stdev([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("empty set", "Stdev({})")
            .edgeCaseMdx("singleton set (divides by n - 1 = 0)", "Stdev({[Gender].[F]}, 5)")

            // The standard deviation of a constant expression over a two-member set is exactly
            // 0 — every deviation from the mean is 0 — no real fact-table figure needed.
            .value("Stdev([Gender].[Gender].[All Gender].Children, 5)", "0")

            .scalarDoesNotDependOn("Stdev([Gender].[Gender].[All Gender].Children)", "[Gender].[Gender]")
            .scalarDependsOn("Stdev([Gender].[Gender].[All Gender].Children, 5)")
            .scalarDoesNotDependOn("Stdev([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales])", "[Gender].[Gender]", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
