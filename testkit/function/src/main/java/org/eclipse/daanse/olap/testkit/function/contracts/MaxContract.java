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

import org.eclipse.daanse.olap.function.def.minmax.MinMaxFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Max(<Set>[, <Numeric Expression>])}, the maximum
 * value of a numeric expression evaluated over a set (the expression defaults to the current
 * measure when omitted). {@code MaxResolver} is an {@code AbstractFunctionDefinitionMultiResolver}
 * over a single declared {@code FunctionMetaData} with {@code SET} required and one trailing
 * optional {@code NUMERIC} — the same inherently-safe shape {@link AggregateContract}/{@link
 * AvgContract} document. {@code MinResolver} shares the exact same {@code MinMaxFunDef} class,
 * distinguished only by {@code MinMaxFunDef}'s constructor checking whether {@code
 * functionMetaData.operationAtom().name()} equals {@code "Max"}; only {@code Max} is covered
 * here.
 *
 * <p>{@code MinMaxCalc.dependsOn} delegates to {@code
 * HierarchyDependsChecker.checkAnyDependsButFirst}, the same "depends on whatever the
 * value-expression depends on, except the set's own hierarchy" rule {@link AggregateContract}/
 * {@link AvgContract} already establish. With the implicit current-measure form (one argument),
 * the value expression is a {@code CurrentValueUnknownCalc}, which unconditionally depends on
 * everything, so {@code Max([Gender].[Gender].[All Gender].Children)} depends on every
 * hierarchy except {@code Gender} — asserted below with {@code scalarDoesNotDependOn} only, the
 * same reasoning {@link AggregateContract} gives for why an exhaustive positive check does not
 * fit. With an explicit constant numeric expression, the value expression depends on nothing at
 * all, so the two-arg constant form depends on no hierarchy whatsoever.
 *
 * <p>The maximum of a constant numeric expression over any non-empty set is that constant
 * itself, regardless of the set's actual members — no real fact-table figure needed. A second
 * value case varies the expression per member via {@code IIf([Gender].CurrentMember IS ...)} to
 * exercise a genuine per-tuple maximum without needing to know any real fact-table figure either.
 */
public final class MaxContract {

    private MaxContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Max")
            .signatures("<Numeric Expression> Max(<Set>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(1, 2)

            .resolvesTo(MinMaxFunDef.class, SET)
            .resolvesWithCost(1, MinMaxFunDef.class, LEVEL)    // Level -> Set
            .resolvesWithCost(2, MinMaxFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, MinMaxFunDef.class, TUPLE)    // Tuple -> Set
            .rejects(DIMENSION)   // Dimension does not convert to Set
            .rejects(HIERARCHY)   // Hierarchy does not convert to Set
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                    // arity 0

            .resolvesTo(MinMaxFunDef.class, SET, NUMERIC)
            .resolvesWithCost(3, MinMaxFunDef.class, SET, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, MinMaxFunDef.class, SET, TUPLE)    // Tuple -> Numeric
            .rejects(SET, SET)
            .rejects(SET, STRING)
            .rejects(SET, LEVEL)
            .rejects(SET, HIERARCHY)
            .rejects(SET, DIMENSION)
            .rejects(SET, NUMERIC, NUMERIC)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("set only (implicit current measure)",
                    "Max([Gender].[Gender].[All Gender].Children)")
            .edgeCaseMdx("set and explicit numeric expression",
                    "Max([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("empty set", "Max({})")
            .edgeCaseMdx("singleton set", "Max({[Gender].[F]}, 5)")

            // The maximum of a constant expression over any non-empty set is the constant
            // itself, regardless of the set's actual members.
            .value("Max([Gender].[Gender].[All Gender].Children, 5)", "5")
            // A per-member expression that is not actually constant: F maps to 1, M maps to 2,
            // so the maximum over both is 2 — no real fact-table figure needs to be known.
            .value("Max([Gender].[Gender].[All Gender].Children, "
                    + "IIf([Gender].CurrentMember IS [Gender].[F], 1, 2))", "2")

            .scalarDoesNotDependOn("Max([Gender].[Gender].[All Gender].Children)", "[Gender].[Gender]")
            .scalarDependsOn("Max([Gender].[Gender].[All Gender].Children, 5)")
            .scalarDoesNotDependOn("Max([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales])", "[Gender].[Gender]", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
