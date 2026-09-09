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
import static org.eclipse.daanse.olap.testkit.function.FunctionAssertions.assertThatFunction;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.headtail.HeadTailFunDef;


/** The contract of the MDX function {@code Head}. */
public final class HeadContract {

    private HeadContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Head")
            .signatures("<Set> Head(<Set>, <Numeric Expression>)")
            .returns(SET)
            .arity(1, 2)                          // the second parameter is .asOptional()

            .resolvesTo(HeadTailFunDef.class, SET)
            .resolvesTo(HeadTailFunDef.class, SET, NUMERIC)
            .resolvesTo(HeadTailFunDef.class, SET, INTEGER)
            .resolvesWithCost(2, HeadTailFunDef.class, MEMBER, NUMERIC)   // Member -> Set
            .rejects(NUMERIC, SET)
            .rejects(SET, NUMERIC, NUMERIC)
            .rejects()

            .autoEdgeCases()
            .edgeCaseMdx("empty set",           "Head({}, 2)")
            .edgeCaseMdx("empty set, no count", "Head({})")
            .edgeCaseMdx("count zero",          "Head([Gender].Members, 0)")
            .edgeCaseMdx("count one",           "Head([Gender].Members, 1)")
            .edgeCaseMdx("count negative",      "Head([Gender].Members, -1)")
            .edgeCaseMdx("count beyond end",    "Head([Gender].Members, 1000)")
            .edgeCaseMdx("count MAX_VALUE",     "Head([Gender].Members, 2147483647)")
            .edgeCaseMdx("count MIN_VALUE",     "Head([Gender].Members, -2147483648)")
            // A null count is treated like count <= 0 (empty result) — HeadCalc.head checks
            // "count == null || count <= 0" before ever unboxing count.
            .edgeCaseMdx("count NULL",          "Head([Gender].Members, NULL)")


            .value("SetToStr(Head([Gender].Members, 1))", "{[Gender].[F]}")
            .value("Count(Head([Gender].Members, 0))",    "0")
            .value("Count(Head({}, 5))",                  "0")
            .value("Count(Head([Gender].Members, 1000))", "2")
            .value("Count(Head([Gender].Members, NULL))", "0")


            .dependsOn("Head([Gender].Members, 2)", "[Gender].[Gender]")
            .dependsOn("Head([Gender].Members, [Measures].[Unit Sales])",
                       "[Gender].[Gender]", "[Measures].[Measures]")

            .resultStyle("Head([Gender].Members, 2)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("Head([Gender].Members, 2)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("Head([Gender].Members, 2)")
            .independentMutableList("Head(Order([Gender].Members, [Measures].[Unit Sales]), 1)")
            .build();
}