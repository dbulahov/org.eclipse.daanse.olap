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
import org.eclipse.daanse.olap.function.def.topbottomcount.TopBottomCountFunDef;

/** The contract of the MDX function {@code BottomCount}. */
public final class BottomCountContract {

    private BottomCountContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("BottomCount")
            .signatures("<Set> BottomCount(<Set>, <Numeric Expression>, <Numeric Expression>)")
            .returns(SET)
            .arity(2, 3)                          // the third parameter is .asOptional()

            .resolvesTo(TopBottomCountFunDef.class, SET, NUMERIC)
            .resolvesTo(TopBottomCountFunDef.class, SET, NUMERIC, NUMERIC)
            .resolvesTo(TopBottomCountFunDef.class, SET, INTEGER)
            .resolvesWithCost(2, TopBottomCountFunDef.class, MEMBER, NUMERIC)   // Member -> Set
            .rejects(NUMERIC, SET)
            .rejects(SET)                          // count is required, not optional
            .rejects(SET, NUMERIC, NUMERIC, NUMERIC)   // arity 4
            .rejects()

            .autoEdgeCases()
            .edgeCaseMdx("empty set",              "BottomCount({}, 2)")
            .edgeCaseMdx("count zero",              "BottomCount([Gender].Members, 0)")
            // TopBottomCountCalc, unlike HeadCalc/TailCalc, does not clamp a negative count
            // before calling TupleList.subList: for BottomCount([Gender].Members, -1) it computes
            // list.subList(list.size() + 1, list.size()), a fromIndex past the list's size. That
            // is an IndexOutOfBoundsException, not a diagnosed OlapRuntimeException.
            .edgeCaseMdx("count negative",          "BottomCount([Gender].Members, -1)")
            .edgeCaseMdx("count beyond end",        "BottomCount([Gender].Members, 1000)")
            .edgeCaseMdx("count NULL",              "BottomCount([Gender].Members, NULL)")
            .edgeCaseMdx("with order expression",   "BottomCount([Gender].Members, 1, [Measures].[Unit Sales])")

            // Without an order expression, BottomCount keeps the set's natural order and takes
            // the tail — the mirror image of Head/Tail, whose order is already pinned down by
            // HeadContract.
            .value("SetToStr(BottomCount([Gender].Members, 1))", "{[Gender].[M]}")
            .value("Count(BottomCount([Gender].Members, 0))",    "0")
            .value("Count(BottomCount({}, 5))",                  "0")
            .value("Count(BottomCount([Gender].Members, 1000))", "2")
            .value("Count(BottomCount([Gender].Members, 1, [Measures].[Unit Sales]))", "1")

            .dependsOn("BottomCount([Gender].Members, 2)", "[Gender].[Gender]")
            .dependsOn("BottomCount([Gender].Members, 1, [Measures].[Unit Sales])",
                       "[Gender].[Gender]", "[Measures].[Measures]")

            .resultStyle("BottomCount([Gender].Members, 2)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("BottomCount([Gender].Members, 2)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("BottomCount([Gender].Members, 2)")
            .independentMutableList("BottomCount([Gender].Members, 1, [Measures].[Unit Sales])")
            .build();
}
