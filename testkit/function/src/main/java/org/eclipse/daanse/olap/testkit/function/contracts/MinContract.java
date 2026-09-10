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
 * The contract of the MDX function {@code Min(<Set>[, <Numeric Expression>])}, the minimum
 * value of a numeric expression evaluated over a set (the expression defaults to the current
 * measure when omitted). {@code MinResolver} shares the exact same {@code MinMaxFunDef}/{@code
 * MinMaxCalc} pair {@link MaxContract} documents — the same single declared {@code
 * FunctionMetaData} with {@code SET} required and one trailing optional {@code NUMERIC}, and
 * the same {@code checkAnyDependsButFirst} dependency rule. {@code MinMaxFunDef}'s constructor
 * distinguishes the two only by checking whether {@code
 * functionMetaData.operationAtom().name()} equals {@code "Max"} — {@code MinMaxCalc.max} is
 * {@code false} here, so {@code evaluateInternal} calls {@code FunUtil.min} instead of {@code
 * FunUtil.max}. See {@link MaxContract}'s Javadoc for the full dependency/compile-call
 * reasoning; only the value cases below differ (minimum instead of maximum).
 *
 * <p>The minimum of a constant numeric expression over any non-empty set is that constant
 * itself, regardless of the set's actual members — no real fact-table figure needed. A second
 * value case varies the expression per member via {@code IIf([Gender].CurrentMember IS ...)}
 * (F maps to 1, M maps to 2, so the minimum is 1) to exercise a genuine per-tuple minimum
 * without needing to know any real fact-table figure either.
 */
public final class MinContract {

    private MinContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Min")
            .signatures("<Numeric Expression> Min(<Set>, <Numeric Expression>)")
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
                    "Min([Gender].[Gender].[All Gender].Children)")
            .edgeCaseMdx("set and explicit numeric expression",
                    "Min([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("empty set", "Min({})")
            .edgeCaseMdx("singleton set", "Min({[Gender].[F]}, 5)")

            // The minimum of a constant expression over any non-empty set is the constant
            // itself, regardless of the set's actual members.
            .value("Min([Gender].[Gender].[All Gender].Children, 5)", "5")
            // A per-member expression that is not actually constant: F maps to 1, M maps to 2,
            // so the minimum over both is 1 — no real fact-table figure needs to be known.
            .value("Min([Gender].[Gender].[All Gender].Children, "
                    + "IIf([Gender].CurrentMember IS [Gender].[F], 1, 2))", "1")

            .scalarDoesNotDependOn("Min([Gender].[Gender].[All Gender].Children)", "[Gender].[Gender]")
            .scalarDependsOn("Min([Gender].[Gender].[All Gender].Children, 5)")
            .scalarDoesNotDependOn("Min([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales])", "[Gender].[Gender]", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
