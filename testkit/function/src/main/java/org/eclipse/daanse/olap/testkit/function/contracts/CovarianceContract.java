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
 * The contract of the MDX function {@code Covariance(<Set>, <Numeric Expression1>[, <Numeric
 * Expression2>])}, the <em>biased</em> (population) covariance of two numeric series evaluated
 * over a set (the second series defaults to the current measure when omitted). {@code
 * CovarianceResolver} is an {@code AbstractFunctionDefinitionMultiResolver} over a single
 * declared {@code FunctionMetaData} with {@code SET} and one {@code NUMERIC} parameter required,
 * followed by one trailing optional {@code NUMERIC} — the same shape {@link
 * CorrelationContract}/{@link AggregateContract} document as inherently safe against the
 * "wrong slot" resolution-order bug {@link ParallelPeriodContract} found and fixed elsewhere.
 *
 * <p>{@code CovarianceNResolver} declares the sibling {@code CovarianceN} atom (unbiased,
 * dividing by {@code n-1} instead of {@code n}) over the exact same {@code CovarianceFunDef}
 * class — the constructor reads {@code functionMetaData.operationAtom().name()} to decide the
 * bias flag, so the two atoms are otherwise indistinguishable by class alone; see {@link
 * CovarianceNContract} for that one.
 *
 * <p>{@code CovarianceCalc} delegates to {@code FunUtil.covariance} and overrides {@code
 * dependsOn} with {@code HierarchyDependsChecker.checkAnyDependsButFirst} — the same {@link
 * CorrelationContract}/{@link AggregateContract}/{@link AvgContract} shape: depends on whatever
 * the two numeric-expression arguments depend on, except the hierarchies the set spans.
 *
 * <p>Unlike {@code Correlation}'s self-value (always exactly {@code 1}), covariance's
 * self-value ({@code Covariance(set, X, X)}) is the population variance of {@code X} — a real
 * number that depends on the actual data, not a universal constant. The data-independent
 * identity used here instead is symmetry: {@code Cov(X, Y) = Cov(Y, X)} for any {@code X}/{@code
 * Y}, true of the covariance formula regardless of what the series actually contain — verified
 * against a real connection.
 */
public final class CovarianceContract {

    private CovarianceContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Covariance")
            .signatures("<Numeric Expression> Covariance(<Set>, <Numeric Expression>, <Numeric Expression>)")
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
                    "Covariance([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("set and two series",
                    "Covariance([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                            + "[Measures].[Store Sales])")
            .edgeCaseMdx("empty set",
                    "Covariance({}, [Measures].[Unit Sales], [Measures].[Store Sales])")

            // Cov(X, Y) = Cov(Y, X) regardless of what the series actually contain — no real
            // fact-table figure needs to be known (see the class Javadoc).
            .value("(Covariance([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                    + "[Measures].[Store Sales]) = "
                    + "Covariance([Gender].[Gender].[All Gender].Children, [Measures].[Store Sales], "
                    + "[Measures].[Unit Sales]))", "true")

            .scalarDoesNotDependOn("Covariance([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales], [Measures].[Store Sales])", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
