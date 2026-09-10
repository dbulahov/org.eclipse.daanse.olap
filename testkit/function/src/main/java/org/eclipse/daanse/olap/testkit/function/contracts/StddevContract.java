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
 * The contract of the MDX function {@code Stddev(<Set>[, <Numeric Expression>])} — {@code
 * StddevResolver}'s own declared description is literally {@code "Alias for Stdev."}: it shares
 * the exact same {@code StdevFunDef}/{@code StdevCalc} pair {@link StdevContract} documents,
 * under a different {@code FunctionOperationAtom} name. See {@link StdevContract}'s Javadoc for
 * the full compile-call/dependency/variance reasoning; only the atom name differs here.
 */
public final class StddevContract {

    private StddevContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Stddev")
            .signatures("<Numeric Expression> Stddev(<Set>, <Numeric Expression>)")
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
                    "Stddev([Gender].[Gender].[All Gender].Children)")
            .edgeCaseMdx("set and explicit numeric expression",
                    "Stddev([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("empty set", "Stddev({})")
            .edgeCaseMdx("singleton set (divides by n - 1 = 0)", "Stddev({[Gender].[F]}, 5)")

            // The standard deviation of a constant expression over a two-member set is exactly
            // 0 — every deviation from the mean is 0 — no real fact-table figure needed.
            .value("Stddev([Gender].[Gender].[All Gender].Children, 5)", "0")

            .scalarDoesNotDependOn("Stddev([Gender].[Gender].[All Gender].Children)", "[Gender].[Gender]")
            .scalarDependsOn("Stddev([Gender].[Gender].[All Gender].Children, 5)")
            .scalarDoesNotDependOn("Stddev([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales])", "[Gender].[Gender]", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
