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

import org.eclipse.daanse.olap.function.def.covariance.CovarianceFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code CovarianceN(<Set>, <Numeric Expression>[, <Numeric
 * Expression>])}, the <em>unbiased</em> (sample, dividing by {@code n-1}) covariance of two
 * numeric series evaluated over a set — the sibling {@link CovarianceContract} documents in
 * full: {@code CovarianceNResolver} wraps the very same {@code CovarianceFunDef} class as {@code
 * CovarianceResolver}, differentiated only by its own {@code FunctionMetaData}/{@code
 * FunctionOperationAtom} ("CovarianceN" instead of "Covariance"), which {@code
 * CovarianceFunDef}'s constructor reads back ({@code
 * functionMetaData.operationAtom().name().equals("Covariance")}) to decide the bias flag. Same
 * single-required-prefix-plus-one-trailing-optional resolver shape, same {@code SET}/{@code
 * NUMERIC} cost ladders, and the same {@code HierarchyDependsChecker.checkAnyDependsButFirst}
 * {@code dependsOn} override — nothing here differs from {@link CovarianceContract} except the
 * bias of the underlying formula, which the symmetry identity below does not care about.
 *
 * <p>{@code Cov(X, Y) = Cov(Y, X)} regardless of whether the {@code n} or {@code n-1} divisor is
 * used — the same data-independent identity {@link CovarianceContract} uses, verified here
 * against a real connection too.
 */
public final class CovarianceNContract {

    private CovarianceNContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("CovarianceN")
            .signatures("<Numeric Expression> CovarianceN(<Set>, <Numeric Expression>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(2, 3)

            .resolvesTo(CovarianceFunDef.class, SET, NUMERIC)
            .resolvesWithCost(1, CovarianceFunDef.class, LEVEL, NUMERIC)    // Level -> Set
            .resolvesWithCost(2, CovarianceFunDef.class, MEMBER, NUMERIC)   // Member -> Set
            .resolvesWithCost(2, CovarianceFunDef.class, TUPLE, NUMERIC)    // Tuple -> Set
            .rejects(DIMENSION, NUMERIC)   // Dimension does not convert to Set
            .rejects(HIERARCHY, NUMERIC)   // Hierarchy does not convert to Set
            .rejects(NUMERIC, NUMERIC)
            .rejects(STRING, NUMERIC)

            .resolvesWithCost(3, CovarianceFunDef.class, SET, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, CovarianceFunDef.class, SET, TUPLE)    // Tuple -> Numeric
            .rejects(SET, SET)
            .rejects(SET, STRING)
            .rejects(SET, LEVEL)        // Level does not convert to Numeric
            .rejects(SET, HIERARCHY)    // Hierarchy does not convert to Numeric
            .rejects(SET, DIMENSION)    // Dimension does not convert to Numeric

            .resolvesTo(CovarianceFunDef.class, SET, NUMERIC, NUMERIC)
            .resolvesWithCost(3, CovarianceFunDef.class, SET, NUMERIC, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, CovarianceFunDef.class, SET, NUMERIC, TUPLE)    // Tuple -> Numeric
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
                    "CovarianceN([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("set and two series",
                    "CovarianceN([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                            + "[Measures].[Store Sales])")
            .edgeCaseMdx("empty set",
                    "CovarianceN({}, [Measures].[Unit Sales], [Measures].[Store Sales])")

            // Cov(X, Y) = Cov(Y, X) regardless of what the series actually contain — no real
            // fact-table figure needs to be known (see the class Javadoc).
            .value("(CovarianceN([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                    + "[Measures].[Store Sales]) = "
                    + "CovarianceN([Gender].[Gender].[All Gender].Children, [Measures].[Store Sales], "
                    + "[Measures].[Unit Sales]))", "true")

            .scalarDoesNotDependOn("CovarianceN([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales], [Measures].[Store Sales])", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
