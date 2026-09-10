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

import org.eclipse.daanse.olap.function.def.nthquartile.NthQuartileFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code FirstQ(<Set>[, <Numeric Expression>])}, the 1st
 * quartile of a numeric expression evaluated over a set (the expression defaults to the current
 * measure when omitted). {@code FirstQResolver} is an {@code
 * AbstractFunctionDefinitionMultiResolver} over a single declared {@code FunctionMetaData} with
 * {@code SET} required and one trailing optional {@code NUMERIC} — the same
 * inherently-safe shape {@link AggregateContract}/{@link AvgContract}/{@link
 * CorrelationContract} document.
 *
 * <p>{@code ThirdQResolver} declares the sibling {@code ThirdQ} atom (3rd quartile) over the
 * exact same {@code NthQuartileFunDef} class — the constructor reads {@code
 * functionMetaData.operationAtom().name()} to pick {@code range=1} ({@code FirstQ}) or {@code
 * range=3} ({@code ThirdQ}). Only {@code FirstQ} is covered here.
 *
 * <p>{@code NthQuartileCalc} delegates to {@code FunUtil.quartile} and overrides {@code
 * dependsOn} with {@code HierarchyDependsChecker.checkAnyDependsButFirst} — the same {@link
 * AggregateContract}/{@link AvgContract} shape: depends on whatever the numeric-expression
 * argument depends on, except the hierarchies the set spans.
 *
 * <p>Any quartile of a constant series is trivially that same constant, for any non-empty set —
 * the same data-independent identity {@link AvgContract} uses for its own mean: {@code
 * FirstQ(set, 5)} is exactly {@code 5}, and (since a constant expression depends on nothing at
 * all) the whole call depends on no hierarchy whatsoever, not even ones unrelated to the set.
 */
public final class FirstQContract {

    private FirstQContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("FirstQ")
            .signatures("<Numeric Expression> FirstQ(<Set>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(1, 2)

            .resolvesTo(NthQuartileFunDef.class, SET)
            .resolvesWithCost(1, NthQuartileFunDef.class, LEVEL)    // Level -> Set
            .resolvesWithCost(2, NthQuartileFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, NthQuartileFunDef.class, TUPLE)    // Tuple -> Set
            .rejects(DIMENSION)   // Dimension does not convert to Set
            .rejects(HIERARCHY)   // Hierarchy does not convert to Set
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                    // arity 0

            .resolvesTo(NthQuartileFunDef.class, SET, NUMERIC)
            .resolvesWithCost(3, NthQuartileFunDef.class, SET, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, NthQuartileFunDef.class, SET, TUPLE)    // Tuple -> Numeric
            .rejects(SET, SET)
            .rejects(SET, STRING)
            .rejects(SET, LEVEL)        // Level does not convert to Numeric
            .rejects(SET, HIERARCHY)    // Hierarchy does not convert to Numeric
            .rejects(SET, DIMENSION)    // Dimension does not convert to Numeric
            .rejects(SET, NUMERIC, NUMERIC)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("set only (implicit current measure)",
                    "FirstQ([Gender].[Gender].[All Gender].Children)")
            .edgeCaseMdx("set and constant numeric expression",
                    "FirstQ([Gender].[Gender].[All Gender].Children, 5)")
            .edgeCaseMdx("empty set", "FirstQ({}, 5)")
            .edgeCaseMdx("singleton set", "FirstQ({[Gender].[Gender].[F]}, 5)")

            // A constant numeric expression is its own quartile over any non-empty set,
            // regardless of the set's actual members — no real fact-table figure needed.
            .value("FirstQ([Gender].[Gender].[All Gender].Children, 5)", "5")

            .scalarDoesNotDependOn("FirstQ([Gender].[Gender].[All Gender].Children, 5)", "[Gender].[Gender]")
            .scalarDoesNotDependOn("FirstQ([Gender].[Gender].[All Gender].Children, 5)", "[Time].[Time]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
