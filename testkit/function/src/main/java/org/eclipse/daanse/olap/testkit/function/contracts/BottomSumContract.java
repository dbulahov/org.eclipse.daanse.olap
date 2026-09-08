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

import static org.eclipse.daanse.olap.api.DataType.INTEGER;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.topbottompercentsum.TopBottomPercentSumFunDef;

/** The contract of the MDX function {@code BottomSum}. */
public final class BottomSumContract {

    private BottomSumContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("BottomSum")
            .signatures("<Set> BottomSum(<Set>, <Numeric Expression>, <Numeric Expression>)")
            .returns(SET)
            .arity(3, 3)                          // unlike BottomCount, the value expression is required

            .resolvesTo(TopBottomPercentSumFunDef.class, SET, NUMERIC, NUMERIC)
            .resolvesTo(TopBottomPercentSumFunDef.class, SET, INTEGER, NUMERIC)
            .resolvesWithCost(2, TopBottomPercentSumFunDef.class, MEMBER, NUMERIC, NUMERIC)   // Member -> Set
            .rejects(NUMERIC, SET, NUMERIC)
            .rejects(SET, NUMERIC)                 // the value expression is required, not optional
            .rejects(SET, NUMERIC, NUMERIC, NUMERIC)   // arity 4
            .rejects()

            .autoEdgeCases()
            .edgeCaseMdx("empty set",              "BottomSum({}, 50, [Measures].[Unit Sales])")
            .edgeCaseMdx("target zero",             "BottomSum([Gender].Members, 0, [Measures].[Unit Sales])")
            .edgeCaseMdx("target negative",         "BottomSum([Gender].Members, -10, [Measures].[Unit Sales])")
            .edgeCaseMdx("target beyond total",     "BottomSum([Gender].Members, 1000000, [Measures].[Unit Sales])")
            .edgeCaseMdx("target NULL",             "BottomSum([Gender].Members, NULL, [Measures].[Unit Sales])")
            .edgeCaseMdx("value expression NULL",   "BottomSum([Gender].Members, 50, NULL)")

            // TopBottomPercentSumCalc starts runningTotal at 0 and stops as soon as
            // runningTotal >= target, checked *before* the first member is added: a target of
            // 0 (or a NULL target, treated as 0) or below therefore yields the empty set before
            // any member is examined, regardless of the cube's data.
            .value("Count(BottomSum([Gender].Members, 0, [Measures].[Unit Sales]))",    "0")
            .value("Count(BottomSum([Gender].Members, -10, [Measures].[Unit Sales]))",  "0")
            .value("Count(BottomSum([Gender].Members, NULL, [Measures].[Unit Sales]))", "0")
            .value("Count(BottomSum({}, 50, [Measures].[Unit Sales]))",                 "0")
            // SumContract established Sum({[Gender].[F], [Gender].[M]}, [Measures].[Unit Sales])
            // = 266,773 — a target well beyond that total is unreachable, so the loop runs to
            // completion and keeps every member.
            .value("Count(BottomSum([Gender].Members, 1000000, [Measures].[Unit Sales]))", "2")

            .dependsOn("BottomSum([Gender].Members, 50, [Measures].[Unit Sales])",
                       "[Gender].[Gender]", "[Measures].[Measures]")
            .dependsOn("BottomSum([Gender].Members, 50, 1)", "[Gender].[Gender]")

            .resultStyle("BottomSum([Gender].Members, 50, [Measures].[Unit Sales])",
                         ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("BottomSum([Gender].Members, 50, [Measures].[Unit Sales])",
                         ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("BottomSum([Gender].Members, 50, [Measures].[Unit Sales])")
            .build();
}
