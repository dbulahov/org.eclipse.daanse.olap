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

import org.eclipse.daanse.olap.function.def.correlation.CorrelationFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Correlation(<Set>, <Numeric Expression>[, <Numeric
 * Expression>])}, the Pearson correlation coefficient of two numeric series evaluated over a
 * set (the second series defaults to the current measure when omitted). {@code
 * CorrelationResolver} is an {@code AbstractFunctionDefinitionMultiResolver} over a single
 * declared {@code FunctionMetaData} with {@code SET} and one {@code NUMERIC} parameter required,
 * followed by one trailing optional {@code NUMERIC} — the same "single required prefix, one
 * trailing optional" shape {@link AggregateContract} documents as inherently safe: a mismatched
 * final argument that cannot bind the optional parameter is simply skipped, which then fails the
 * overall argument-count check and rejects the call cleanly, with nothing later in the parameter
 * list for it to fall through into.
 *
 * <p>{@code CorrelationCalc}/the anonymous {@code compileCall} calc both delegate to {@code
 * FunUtil.correlation} and override {@code dependsOn} with {@code
 * HierarchyDependsChecker.checkAnyDependsButFirst} — the same {@code Aggregate}/{@code Avg}
 * shape: depends on whatever the two numeric-expression arguments depend on, except the
 * hierarchies the set spans.
 *
 * <p>Pearson correlation of a series with itself is exactly {@code 1} whenever the series has
 * nonzero variance over the set — a data-independent identity that needs no known fact-table
 * figure, only that {@code [Measures].[Unit Sales]} actually varies across {@code [Gender]}'s
 * two children in the shared fixture (verified against a real connection).
 */
public final class CorrelationContract {

    private CorrelationContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Correlation")
            .signatures("<Numeric Expression> Correlation(<Set>, <Numeric Expression>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(2, 3)

            .resolvesTo(CorrelationFunDef.class, SET, NUMERIC)
            .resolvesWithCost(1, CorrelationFunDef.class, LEVEL, NUMERIC)    // Level -> Set
            .resolvesWithCost(2, CorrelationFunDef.class, MEMBER, NUMERIC)   // Member -> Set
            .resolvesWithCost(2, CorrelationFunDef.class, TUPLE, NUMERIC)    // Tuple -> Set
            .rejects(DIMENSION, NUMERIC)   // Dimension does not convert to Set
            .rejects(HIERARCHY, NUMERIC)   // Hierarchy does not convert to Set
            .rejects(NUMERIC, NUMERIC)
            .rejects(STRING, NUMERIC)

            .resolvesWithCost(3, CorrelationFunDef.class, SET, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, CorrelationFunDef.class, SET, TUPLE)    // Tuple -> Numeric
            .rejects(SET, SET)
            .rejects(SET, STRING)
            .rejects(SET, LEVEL)        // Level does not convert to Numeric
            .rejects(SET, HIERARCHY)    // Hierarchy does not convert to Numeric
            .rejects(SET, DIMENSION)    // Dimension does not convert to Numeric

            .resolvesTo(CorrelationFunDef.class, SET, NUMERIC, NUMERIC)
            .resolvesWithCost(3, CorrelationFunDef.class, SET, NUMERIC, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, CorrelationFunDef.class, SET, NUMERIC, TUPLE)    // Tuple -> Numeric
            .rejects(SET, NUMERIC, SET)
            .rejects(SET, NUMERIC, STRING)
            .rejects(SET, NUMERIC, LEVEL)
            .rejects(SET, NUMERIC, HIERARCHY)
            .rejects(SET, NUMERIC, DIMENSION)

            .rejects()                                    // arity 0
            .rejects(SET)                                  // arity 1
            .rejects(SET, NUMERIC, NUMERIC, NUMERIC)       // arity 4

            .autoEdgeCases()
            .edgeCaseMdx("set and one series (implicit current measure)",
                    "Correlation([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("set and two series",
                    "Correlation([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                            + "[Measures].[Unit Sales])")
            .edgeCaseMdx("empty set",
                    "Correlation({}, [Measures].[Unit Sales], [Measures].[Unit Sales])")

            // Pearson correlation of a series with itself is exactly 1 whenever it has nonzero
            // variance over the set — [Measures].[Unit Sales] does vary across [Gender]'s two
            // children in the shared fixture, so no real fact-table figure needs to be known.
            .value("Correlation([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                    + "[Measures].[Unit Sales])", "1")

            .scalarDoesNotDependOn("Correlation([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales], [Measures].[Unit Sales])", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
