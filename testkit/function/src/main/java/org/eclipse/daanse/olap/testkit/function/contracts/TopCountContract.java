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
import org.eclipse.daanse.olap.function.def.topbottomcount.TopBottomCountFunDef;

/**
 * The contract of the MDX function {@code TopCount}, the mirror image of {@link
 * BottomCountContract} — both share {@code TopBottomCountFunDef}/{@code TopBottomCountCalc},
 * distinguished only by the {@code top} constructor flag ({@code true} here).
 *
 * <p>Without an order expression, {@code TopBottomCountCalc} takes the set's natural-order
 * <em>head</em> for {@code top=true} ({@code list.subList(0, n)}) instead of {@link
 * BottomCountContract}'s tail ({@code list.subList(list.size() - n, list.size())}). {@code n}
 * itself is null-safe ({@code n == 0 || n == null} returns empty first), but a negative
 * {@code n} still reaches {@code subList} un-clamped — here as a directly negative
 * {@code toIndex} ({@code list.subList(0, n)} with {@code n < 0}), throwing {@code
 * IndexOutOfBoundsException} immediately rather than via {@code BottomCount}'s {@code
 * list.size() - n} arithmetic overflow. Same class of documented-but-unfixed gap, left as an
 * edge case rather than a promise waiver, matching {@code BottomCountContract}'s precedent.
 */
public final class TopCountContract {

    private TopCountContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("TopCount")
            .signatures("<Set> TopCount(<Set>, <Numeric Expression>, <Numeric Expression>)")
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
            .edgeCaseMdx("empty set",              "TopCount({}, 2)")
            .edgeCaseMdx("count zero",              "TopCount([Gender].Members, 0)")
            .edgeCaseMdx("count one",               "TopCount([Gender].Members, 1)")
            // Documented, currently-inert (see the class Javadoc): list.subList(0, n) with a
            // negative n throws IndexOutOfBoundsException immediately.
            .edgeCaseMdx("count negative",          "TopCount([Gender].Members, -1)")
            .edgeCaseMdx("count beyond end",        "TopCount([Gender].Members, 1000)")
            // Safe: "list instanceof AbstractList && list.size() <= n" is true here (2 <=
            // MAX_VALUE), so it takes the early-return branch and never reaches subList.
            .edgeCaseMdx("count MAX_VALUE",         "TopCount([Gender].Members, 2147483647)")
            .edgeCaseMdx("count MIN_VALUE",         "TopCount([Gender].Members, -2147483648)")
            .edgeCaseMdx("count NULL",              "TopCount([Gender].Members, NULL)")
            .edgeCaseMdx("with order expression",   "TopCount([Gender].Members, 1, [Measures].[Unit Sales])")

            // Without an order expression, TopCount keeps the set's natural order and takes
            // the head — matching HeadContract's established [Gender].Members ordering (F at
            // index 0, M at index 1).
            .value("SetToStr(TopCount([Gender].Members, 1))", "{[Gender].[Gender].[All Gender]}")
            .value("Count(TopCount([Gender].Members, 0))",    "0")
            .value("Count(TopCount({}, 5))",                  "0")
            .value("Count(TopCount([Gender].Members, 1000))", "3")
            .value("Count(TopCount([Gender].Members, 1, [Measures].[Unit Sales]))", "1")

            .dependsOn("TopCount([Gender].Members, 2)")
            .doesNotDependOn("TopCount([Gender].Members, 1, [Measures].[Unit Sales])",
                       "[Gender].[Gender]", "[Measures]")

            .resultStyle("TopCount([Gender].Members, 2)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("TopCount([Gender].Members, 2)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("TopCount([Gender].Members, 2)")
            .independentMutableList("TopCount([Gender].Members, 1, [Measures].[Unit Sales])")
            .build();
}
