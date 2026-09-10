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

import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Avg(<Set>[, <Numeric Expression>])}, which computes
 * the arithmetic mean of a numeric expression evaluated over a set (defaulting to the current
 * measure when the expression is omitted). {@code AvgResolver} is an {@code
 * AbstractFunctionDefinitionMultiResolver} over two separate declared overloads — {@code
 * AvgFunDef} ({@code SET} only, arity 1) and {@code AvgNumericFunDef} ({@code SET}, {@code
 * NUMERIC}, arity 2) — the same "one metadata per arity" shape {@link ClosingPeriodContract}
 * documents, except the two never share an arity, so there is no first-match-wins ordering to
 * get wrong the way {@code ClosingPeriodResolved} once did.
 *
 * <p>Both {@code AvgFunDef} and {@code AvgNumericFunDef} (and the {@code AvgCalc} they compile
 * to) are package-private — unlike every other atom in this suite, this contract cannot name
 * them in {@code resolvesTo(...)}/{@code resolvesWithCost(...)}. It uses {@code
 * resolvesToOverload(...)} instead, which asserts the matched <em>declared signature text</em>
 * rather than the implementing class; since the two overloads never tie (they differ in arity),
 * there is no cost-based arbitration to verify either; here.
 *
 * <p>{@code AvgCalc.evaluateInternal} calls {@code FunUtil.avg}, not the ambient aggregator
 * {@link AggregateContract} relies on — so a constant numeric expression is trivially its own
 * average regardless of the set's contents (as long as it is non-empty): {@code
 * Avg([Gender].Members, 5)} is exactly {@code 5}. The value assertion below uses that identity
 * instead of {@link AggregateContract}/{@link ValidMeasureContract}'s null-safe fact-table
 * comparison, since it needs no real fact-table figure at all.
 *
 * <p>{@code AvgCalc.dependsOn} delegates directly to {@code
 * HierarchyDependsChecker.checkAnyDependsButFirst} with no {@code Measures}-always-true override
 * (unlike {@link AggregateContract}'s {@code AggregateCalc}): it depends on whatever the
 * value-expression argument depends on, except the hierarchies the set spans. With the implicit
 * current-measure form (one argument), the value expression is a {@code
 * CurrentValueUnknownCalc}, which unconditionally depends on everything, so {@code
 * Avg([Gender].Members)} depends on every hierarchy except {@code Gender} — asserted below with
 * {@code scalarDoesNotDependOn} only, the same reasoning {@link AggregateContract} gives for why
 * an exhaustive positive check does not fit. With an explicit constant numeric expression, the value
 * expression depends on nothing at all, so {@code Avg([Gender].Members, 5)} depends on no
 * hierarchy whatsoever, not even ones unrelated to the set.
 */
public final class AvgContract {

    private AvgContract() {
    }

    private static final String AVG_SET =
            "<Numeric Expression> Avg(<Set>)";
    private static final String AVG_SET_NUMERIC =
            "<Numeric Expression> Avg(<Set>, <Numeric Expression>)";

    public static final FunctionContract CONTRACT = FunctionContract.of("Avg")
            .signatures(AVG_SET, AVG_SET_NUMERIC)
            .returns(NUMERIC)
            .arity(1, 2)

            .resolvesToOverload(AVG_SET, SET)
            .resolvesToOverload(AVG_SET, LEVEL)     // Level -> Set
            .resolvesToOverload(AVG_SET, MEMBER)    // Member -> Set
            .resolvesToOverload(AVG_SET, TUPLE)     // Tuple -> Set
            .rejects(DIMENSION)   // Dimension does not convert to Set
            .rejects(HIERARCHY)   // Hierarchy does not convert to Set
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                    // arity 0

            .resolvesToOverload(AVG_SET_NUMERIC, SET, NUMERIC)
            .resolvesToOverload(AVG_SET_NUMERIC, SET, MEMBER)   // Member -> Numeric
            .resolvesToOverload(AVG_SET_NUMERIC, SET, TUPLE)    // Tuple -> Numeric
            .rejects(SET, SET)
            .rejects(SET, STRING)
            .rejects(SET, LEVEL)
            .rejects(SET, HIERARCHY)
            .rejects(SET, DIMENSION)
            .rejects(SET, NUMERIC, NUMERIC)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("set only (implicit current measure)", "Avg([Gender].Members)")
            .edgeCaseMdx("set and constant numeric expression", "Avg([Gender].Members, 5)")
            .edgeCaseMdx("empty set", "Avg({}, 5)")
            .edgeCaseMdx("singleton set", "Avg({[Gender].[F]}, 5)")

            // A constant numeric expression is its own average over any non-empty set,
            // regardless of the set's actual members — no real fact-table figure needed.
            .value("Avg([Gender].Members, 5)", "5")
            .value("Avg({[Gender].[F]}, 7)", "7")

            .scalarDoesNotDependOn("Avg([Gender].Members)", "[Gender].[Gender]")
            .scalarDoesNotDependOn("Avg([Gender].Members, 5)", "[Gender].[Gender]")
            .scalarDoesNotDependOn("Avg([Gender].Members, 5)", "[Time].[Time]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
