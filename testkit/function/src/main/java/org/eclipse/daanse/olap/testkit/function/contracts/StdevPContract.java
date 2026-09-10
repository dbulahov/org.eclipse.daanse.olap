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

import org.eclipse.daanse.olap.function.def.stdev.StdevPFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code StdevP(<Set>[, <Numeric Expression>])}, the biased
 * (population, divide by {@code n}) standard deviation of a numeric expression evaluated over a
 * set (the expression defaults to the current measure when omitted). {@code StdevPResolver} is
 * an {@code AbstractFunctionDefinitionMultiResolver} over a single declared {@code
 * FunctionMetaData} with {@code SET} required and one trailing optional {@code NUMERIC} — the
 * same inherently-safe shape {@link StdevContract} documents. {@code StddevPResolver} declares
 * the same call shape under the {@code StddevP} atom, sharing this same {@code StdevPFunDef}
 * class verbatim (see {@link StddevPContract}).
 *
 * <p>{@code StdevPCalc.evaluateInternal} delegates to {@code FunUtil.stdev(evaluator, list,
 * calc, true)} — the same helper {@link StdevContract} documents, but with {@code biased =
 * true}: {@code FunUtil.var}'s divisor stays {@code n} instead of {@code n - 1}. {@code
 * StdevPFunDef.compileCall} wraps the returned {@code StdevPCalc} in an anonymous subclass that
 * adds nothing (no method is overridden) — the same harmless, no-op wrapping shape {@link
 * ParentContract}/{@link PercentileContract} document for their own siblings. {@code dependsOn}
 * delegates to {@code HierarchyDependsChecker.checkAnyDependsButFirst}, the same rule {@link
 * StdevContract} already establishes.
 *
 * <p>The standard deviation of a constant numeric expression over any non-empty set is exactly
 * {@code 0} — every deviation from the mean is {@code 0} — so this does not depend on any real
 * fact-table figure. Unlike {@link StdevContract}'s unbiased sibling, a singleton set does
 * <em>not</em> divide by zero here (the divisor stays {@code n = 1}, not {@code n - 1 = 0}), so
 * {@code StdevP({member}, constant)} is a well-defined {@code 0} rather than {@code NaN}.
 */
public final class StdevPContract {

    private StdevPContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("StdevP")
            .signatures("<Numeric Expression> StdevP(<Set>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(1, 2)

            .resolvesTo(StdevPFunDef.class, SET)
            .resolvesWithCost(1, StdevPFunDef.class, LEVEL)    // Level -> Set
            .resolvesWithCost(2, StdevPFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, StdevPFunDef.class, TUPLE)    // Tuple -> Set
            .rejects(DIMENSION)   // Dimension does not convert to Set
            .rejects(HIERARCHY)   // Hierarchy does not convert to Set
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                    // arity 0

            .resolvesTo(StdevPFunDef.class, SET, NUMERIC)
            .resolvesWithCost(3, StdevPFunDef.class, SET, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, StdevPFunDef.class, SET, TUPLE)    // Tuple -> Numeric
            .rejects(SET, SET)
            .rejects(SET, STRING)
            .rejects(SET, LEVEL)
            .rejects(SET, HIERARCHY)
            .rejects(SET, DIMENSION)
            .rejects(SET, NUMERIC, NUMERIC)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("set only (implicit current measure)",
                    "StdevP([Gender].[Gender].[All Gender].Children)")
            .edgeCaseMdx("set and explicit numeric expression",
                    "StdevP([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("empty set", "StdevP({})")
            .edgeCaseMdx("singleton set", "StdevP({[Gender].[F]}, 5)")

            // The standard deviation of a constant expression over any non-empty set is
            // exactly 0 — every deviation from the mean is 0 — no real fact-table figure
            // needed. Unlike Stdev, a singleton set is well-defined here too (see the class
            // Javadoc): the divisor is n = 1, not n - 1 = 0.
            .value("StdevP([Gender].[Gender].[All Gender].Children, 5)", "0")
            .value("StdevP({[Gender].[F]}, 5)", "0")

            .scalarDoesNotDependOn("StdevP([Gender].[Gender].[All Gender].Children)", "[Gender].[Gender]")
            .scalarDependsOn("StdevP([Gender].[Gender].[All Gender].Children, 5)")
            .scalarDoesNotDependOn("StdevP([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales])", "[Gender].[Gender]", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
