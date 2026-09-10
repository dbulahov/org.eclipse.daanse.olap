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
import org.eclipse.daanse.olap.function.def.topbottompercentsum.TopBottomPercentSumFunDef;

/**
 * The contract of the MDX function {@code TopPercent} — the mirror image of {@link
 * BottomPercentContract}, sharing {@code TopBottomPercentSumFunDef}/{@code
 * TopBottomPercentSumCalc} (constructed with {@code top=true, percent=true}).
 *
 * <p>The target/running-total stopping logic ({@code runningTotal >= target} checked before
 * each member is added, a {@code NULL} target treated as {@code 0}) does not depend on the
 * {@code top} flag at all — only which end of the sorted list {@code Sorter.sortTuples} sorts
 * toward does — so every count-based finding {@link BottomPercentContract} establishes
 * (target ≤ 0 or {@code NULL} ⇒ empty; a cumulative percentage can never exceed ~100, so an
 * unreachable target keeps every member) applies identically here.
 */
public final class TopPercentContract {

    private TopPercentContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("TopPercent")
            .signatures("<Set> TopPercent(<Set>, <Numeric Expression>, <Numeric Expression>)")
            .returns(SET)
            .arity(3, 3)                          // unlike TopCount, the value expression is required

            .resolvesTo(TopBottomPercentSumFunDef.class, SET, NUMERIC, NUMERIC)
            .resolvesTo(TopBottomPercentSumFunDef.class, SET, INTEGER, NUMERIC)
            .resolvesWithCost(2, TopBottomPercentSumFunDef.class, MEMBER, NUMERIC, NUMERIC)   // Member -> Set
            .rejects(NUMERIC, SET, NUMERIC)
            .rejects(SET, NUMERIC)                 // the value expression is required, not optional
            .rejects(SET, NUMERIC, NUMERIC, NUMERIC)   // arity 4
            .rejects()

            .autoEdgeCases()
            .edgeCaseMdx("empty set",              "TopPercent({}, 50, [Measures].[Unit Sales])")
            .edgeCaseMdx("target zero",             "TopPercent([Gender].Members, 0, [Measures].[Unit Sales])")
            .edgeCaseMdx("target -1",               "TopPercent([Gender].Members, -1, [Measures].[Unit Sales])")
            .edgeCaseMdx("target negative",         "TopPercent([Gender].Members, -10, [Measures].[Unit Sales])")
            .edgeCaseMdx("target fractional",       "TopPercent([Gender].Members, 0.5, [Measures].[Unit Sales])")
            .edgeCaseMdx("target beyond 100",       "TopPercent([Gender].Members, 1000, [Measures].[Unit Sales])")
            .edgeCaseMdx("target 1E308",            "TopPercent([Gender].Members, 1E308, [Measures].[Unit Sales])")
            .edgeCaseMdx("target NULL",             "TopPercent([Gender].Members, NULL, [Measures].[Unit Sales])")
            .edgeCaseMdx("value expression zero",       "TopPercent([Gender].Members, 50, 0)")
            .edgeCaseMdx("value expression negative",   "TopPercent([Gender].Members, 50, -1)")
            .edgeCaseMdx("value expression fractional", "TopPercent([Gender].Members, 50, 0.5)")
            .edgeCaseMdx("value expression 1E308",      "TopPercent([Gender].Members, 50, 1E308)")
            .edgeCaseMdx("value expression NULL",   "TopPercent([Gender].Members, 50, NULL)")

            .value("Count(TopPercent([Gender].Members, 0, [Measures].[Unit Sales]))",    "0")
            .value("Count(TopPercent([Gender].Members, -1, [Measures].[Unit Sales]))",   "0")
            .value("Count(TopPercent([Gender].Members, -10, [Measures].[Unit Sales]))",  "0")
            .value("Count(TopPercent([Gender].Members, NULL, [Measures].[Unit Sales]))", "0")
            .value("Count(TopPercent({}, 50, [Measures].[Unit Sales]))",                 "0")
            .value("Count(TopPercent([Gender].Members, 1000, [Measures].[Unit Sales]))", "3")

            .doesNotDependOn("TopPercent([Gender].Members, 50, [Measures].[Unit Sales])",
                       "[Gender].[Gender]", "[Measures]")
            .dependsOn("TopPercent([Gender].Members, 50, 1)")

            .resultStyle("TopPercent([Gender].Members, 50, [Measures].[Unit Sales])",
                         ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("TopPercent([Gender].Members, 50, [Measures].[Unit Sales])",
                         ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("TopPercent([Gender].Members, 50, [Measures].[Unit Sales])")
            .build();
}
