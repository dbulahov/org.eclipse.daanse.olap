/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.testkit.function.contracts;

import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.testkit.function.eval.CalcAssertions.assertThatScalarExpr;

import org.eclipse.daanse.olap.function.def.sum.SumFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/** The contract of the MDX function {@code Sum}. */
public final class SumContract {

    private SumContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Sum")
            .signatures("<Numeric Expression> Sum(<Set>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(1, 2)

            .resolvesTo(SumFunDef.class, SET)
            .resolvesTo(SumFunDef.class, SET, NUMERIC)
            .resolvesWithCost(3, SumFunDef.class, SET, MEMBER)   // Member -> Numeric, cost 3
            .rejects(NUMERIC)
            .rejects()

            .autoEdgeCases()
            .edgeCaseMdx("empty set",              "Sum({})")
            .edgeCaseMdx("empty set with measure", "Sum({}, [Measures].[Unit Sales])")
            .edgeCaseMdx("value expression zero",       "Sum([Gender].Members, 0)")
            .edgeCaseMdx("value expression negative",   "Sum([Gender].Members, -1)")
            .edgeCaseMdx("value expression fractional", "Sum([Gender].Members, 0.5)")
            .edgeCaseMdx("value expression 1E308",      "Sum([Gender].Members, 1E308)")
            .edgeCaseMdx("null value expression",  "Sum([Gender].Members, NULL)")

            // MDX distinguishes Sum({}) = NULL from Sum({0}) = 0.
            .value("Sum({[Gender].[F], [Gender].[M]}, [Measures].[Unit Sales])", "266,773")
            // A constant value expression is evaluated once per member of the set: two
            // [Gender] members times a constant -1 sums to -2.
            .value("Sum([Gender].Members, -1)", "-2")
            .valueIsNull("Sum({})")
            .valueIsNull("Sum({}, [Measures].[Unit Sales])")

            // The heart of Sum: the set's hierarchy is bound, the value expression's is not.
            .scalarDependsOn("Sum([Gender].Members, [Measures].[Unit Sales])",
                             "[Measures].[Measures]")               // NOT [Gender].[Gender]
            .scalarDependsOn("Sum([Gender].Members)", "[Measures].[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "returns a scalar; the set argument's result shape is Aggregate's business")
            .build();
}