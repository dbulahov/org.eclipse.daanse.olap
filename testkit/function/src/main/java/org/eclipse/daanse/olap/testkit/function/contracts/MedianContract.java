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

import org.eclipse.daanse.olap.function.def.aggregate.median.MedianFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Median(<Set>[, <Numeric Expression>])}, the median
 * value of a numeric expression evaluated over a set (the expression defaults to the current
 * measure when omitted). {@code MedianResolver} is an {@code
 * AbstractFunctionDefinitionMultiResolver} over a single declared {@code FunctionMetaData} with
 * {@code SET} required and one trailing optional {@code NUMERIC} — the same inherently-safe
 * shape {@link AggregateContract}/{@link AvgContract}/{@link MaxContract} document.
 *
 * <p>{@code MedianResolver} names the optional parameter {@code "Percentile"}, which is
 * misleading: {@code MedianFunDef.compileCall} passes it straight through to {@code
 * MedianCalc} as the value expression to aggregate (identical to {@link MaxContract}'s second
 * argument), not an actual percentile fraction — {@code MedianCalc.evaluateInternal} always
 * calls {@code FunUtil.percentile(evaluator, list, calc, 0.5)} with the percentile hardcoded to
 * {@code 0.5}. There is no way to ask this function for any percentile other than the median.
 *
 * <p>{@code MedianCalc.dependsOn} delegates to {@code
 * HierarchyDependsChecker.checkAnyDependsButFirst}, the same "depends on whatever the
 * value-expression depends on, except the set's own hierarchy" rule {@link AggregateContract}/
 * {@link AvgContract}/{@link MaxContract} already establish. With the implicit current-measure
 * form (one argument), the value expression is a {@code CurrentValueUnknownCalc}, which
 * unconditionally depends on everything, so {@code
 * Median([Gender].[Gender].[All Gender].Children)} depends on every hierarchy except {@code
 * Gender} — asserted below with {@code scalarDoesNotDependOn} only, the same reasoning {@link
 * AggregateContract} gives for why an exhaustive positive check does not fit. With an explicit
 * constant numeric expression, the value expression depends on nothing at all, so the two-arg
 * constant form depends on no hierarchy whatsoever.
 *
 * <p>The median of a constant numeric expression over any non-empty set is that constant
 * itself, regardless of the set's actual members — no real fact-table figure needed.
 *
 * <p><b>A synthetic per-member value expression — e.g. {@code IIf([Gender].CurrentMember IS
 * [Gender].[F], 1, 2)} — evaluates wrong here, verified against a real connection</b>: {@code
 * Median([Gender].[Gender].[All Gender].Children, IIf(...))} returns {@code 2} (the {@code M}
 * branch's value) instead of the mathematically correct {@code 1.5}, even though the very same
 * expression evaluates correctly per member when driven through an unrelated iteration
 * function like {@code Generate} ({@code Generate(..., Str(IIf(...)), ", ")} correctly prints
 * {@code "1, 2"}). {@code Avg} and {@code Percentile(..., 50)} exhibit the identical wrong
 * result over the identical set — all three route through the same {@code
 * AbstractAggregateFunDef.evaluateCurrentList} plus {@code FunUtil.evaluateSet}/{@code
 * percentile}/{@code avg} pipeline — while {@link MaxContract}/{@link MinContract} and plain
 * {@code Sum} give the mathematically correct answer for the exact same synthetic expression
 * over the exact same set. The failure is not simply "small integer literals": {@code
 * Median(..., IIf(..., 1, 3))} happens to land on the correct answer ({@code 2}) while {@code
 * Median(..., IIf(..., 0, 1))} and {@code Median(..., IIf(..., 2, 1))} do not, so whatever
 * caches or short-circuits the per-member re-evaluation here is not a clean, easily-isolated
 * off-by-one — it looks like a real, narrow correctness bug in this codebase's {@code Avg}/
 * {@code Median}/{@code Percentile} evaluation path, not a test-authoring mistake, but its root
 * cause was not pinned down within this pass. The value case below asserts the mathematically
 * correct answer ({@code 1.5}), not the current (broken, {@code 2}) one, so it is expected to
 * fail until this bug is fixed — that failure is the point: this contract is meant to catch it.
 */
public final class MedianContract {

    private MedianContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Median")
            .signatures("<Numeric Expression> Median(<Set>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(1, 2)

            .resolvesTo(MedianFunDef.class, SET)
            .resolvesWithCost(1, MedianFunDef.class, LEVEL)    // Level -> Set
            .resolvesWithCost(2, MedianFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, MedianFunDef.class, TUPLE)    // Tuple -> Set
            .rejects(DIMENSION)   // Dimension does not convert to Set
            .rejects(HIERARCHY)   // Hierarchy does not convert to Set
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                    // arity 0

            .resolvesTo(MedianFunDef.class, SET, NUMERIC)
            .resolvesWithCost(3, MedianFunDef.class, SET, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, MedianFunDef.class, SET, TUPLE)    // Tuple -> Numeric
            .rejects(SET, SET)
            .rejects(SET, STRING)
            .rejects(SET, LEVEL)
            .rejects(SET, HIERARCHY)
            .rejects(SET, DIMENSION)
            .rejects(SET, NUMERIC, NUMERIC)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("set only (implicit current measure)",
                    "Median([Gender].[Gender].[All Gender].Children)")
            .edgeCaseMdx("set and explicit numeric expression",
                    "Median([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("empty set", "Median({})")
            .edgeCaseMdx("singleton set", "Median({[Gender].[F]}, 5)")

            // The median of a constant expression over any non-empty set is the constant
            // itself, regardless of the set's actual members.
            .value("Median([Gender].[Gender].[All Gender].Children, 5)", "5")
            // F maps to 1, M maps to 2: the mathematically correct median of two values is
            // their arithmetic mean, 1.5. MedianCalc actually returns 2 here — a real,
            // confirmed engine bug (see the class Javadoc) — so this assertion is expected to
            // fail until that bug is fixed; it documents the correct behavior, not the current
            // (broken) one.
            .value("Median([Gender].[Gender].[All Gender].Children, "
                    + "IIf([Gender].CurrentMember IS [Gender].[F], 1, 2))", "1.5")

            .scalarDoesNotDependOn("Median([Gender].[Gender].[All Gender].Children)", "[Gender].[Gender]")
            .scalarDependsOn("Median([Gender].[Gender].[All Gender].Children, 5)")
            .scalarDoesNotDependOn("Median([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales])", "[Gender].[Gender]", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
