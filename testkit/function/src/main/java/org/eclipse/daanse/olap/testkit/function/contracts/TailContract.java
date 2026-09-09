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

import static org.eclipse.daanse.olap.api.DataType.INTEGER;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.headtail.HeadTailFunDef;

/**
 * The contract of the MDX function {@code Tail} — {@link HeadContract}'s mirror image, sharing
 * the same {@code HeadTailFunDef} (which switches on {@code operationAtom().name()} to pick
 * "first N" vs "last N"). Unlike {@link BottomCountContract}'s {@code TopBottomCountCalc},
 * {@code TailCalc.tail(Integer count, ...)} checks {@code count == null || count <= 0} before
 * ever unboxing or using {@code count} arithmetically, so every INTEGER boundary here —
 * including a negative count and {@code Integer.MIN_VALUE} — is safe.
 */
public final class TailContract {

    private TailContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Tail")
            .signatures("<Set> Tail(<Set>, <Numeric Expression>)")
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
            .edgeCaseMdx("empty set",           "Tail({}, 2)")
            .edgeCaseMdx("empty set, no count", "Tail({})")
            .edgeCaseMdx("count zero",          "Tail([Gender].Members, 0)")
            .edgeCaseMdx("count one",           "Tail([Gender].Members, 1)")
            .edgeCaseMdx("count negative",      "Tail([Gender].Members, -1)")
            .edgeCaseMdx("count beyond end",    "Tail([Gender].Members, 1000)")
            .edgeCaseMdx("count MAX_VALUE",     "Tail([Gender].Members, 2147483647)")
            .edgeCaseMdx("count MIN_VALUE",     "Tail([Gender].Members, -2147483648)")
            .edgeCaseMdx("count NULL",          "Tail([Gender].Members, NULL)")

            // Tail keeps the set's natural order and takes from the end — the mirror image of
            // Head, whose order is already pinned down by HeadContract.
            .value("SetToStr(Tail([Gender].Members, 1))", "{[Gender].[M]}")
            .value("SetToStr(Tail([Gender].Members))",    "{[Gender].[M]}")   // default count is 1
            .value("Count(Tail([Gender].Members, 0))",    "0")
            .value("Count(Tail({}, 5))",                  "0")
            .value("Count(Tail([Gender].Members, 1000))", "2")
            .value("Count(Tail([Gender].Members, NULL))", "0")

            .dependsOn("Tail([Gender].Members, 2)", "[Gender].[Gender]")
            .dependsOn("Tail([Gender].Members, [Measures].[Unit Sales])",
                       "[Gender].[Gender]", "[Measures].[Measures]")

            .resultStyle("Tail([Gender].Members, 2)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("Tail([Gender].Members, 2)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("Tail([Gender].Members, 2)")
            .independentMutableList("Tail(Order([Gender].Members, [Measures].[Unit Sales]), 1)")
            .build();
}
