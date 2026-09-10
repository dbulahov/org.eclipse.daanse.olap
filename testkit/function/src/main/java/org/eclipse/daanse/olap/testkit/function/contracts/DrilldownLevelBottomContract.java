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
import static org.eclipse.daanse.olap.api.DataType.EMPTY;
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.drilldownleveltopbottom.DrilldownLevelTopBottomFunDef;

/**
 * The contract of the MDX function {@code DrilldownLevelBottom}. Four overloads share the
 * atom: {@code (Set, Count)}, {@code (Set, Count, Level)}, {@code (Set, Count, Level, Order)}
 * and — using the "double comma" elided-Level form, like {@link DrilldownLevelContract} —
 * {@code (Set, Count, <Empty>, Order)}.
 *
 * <p>{@code DrilldownLevelTopBottomCalc.dependsOn(Hierarchy)} is overridden to
 * {@code HierarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy)}: the
 * Set argument's own hierarchy is deliberately excluded from the reported dependencies (only
 * the Count and Order calcs count) — the function iterates the set argument internally and
 * does not need re-evaluating for a different current member of its hierarchy. When no Order
 * argument is given, {@code DrilldownLevelTopBottomFunDef.compileCall} defaults it to a
 * {@code CurrentValueUnknownCalc}, whose {@code dependsOn} unconditionally answers {@code
 * true} (it stands for "the current cell value", which genuinely varies with every
 * hierarchy) — so even the plain {@code (Set, Count)} overload ends up depending on every
 * hierarchy except the Set argument's own (verified against a real connection).
 */
public final class DrilldownLevelBottomContract {

    private DrilldownLevelBottomContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("DrilldownLevelBottom")
            .signatures(
                    "<Set> DrilldownLevelBottom(<Set>, <Numeric Expression>)",
                    "<Set> DrilldownLevelBottom(<Set>, <Numeric Expression>, <Level>)",
                    "<Set> DrilldownLevelBottom(<Set>, <Numeric Expression>, <Level>, <Numeric Expression>)",
                    "<Set> DrilldownLevelBottom(<Set>, <Numeric Expression>, <Empty>, <Numeric Expression>)")
            .returns(SET)
            .arity(2, 4)

            .resolvesTo(DrilldownLevelTopBottomFunDef.class, SET, NUMERIC)
            .resolvesTo(DrilldownLevelTopBottomFunDef.class, SET, NUMERIC, LEVEL)
            .resolvesTo(DrilldownLevelTopBottomFunDef.class, SET, NUMERIC, LEVEL, NUMERIC)
            .resolvesTo(DrilldownLevelTopBottomFunDef.class, SET, NUMERIC, EMPTY, NUMERIC)
            .resolvesWithCost(2, DrilldownLevelTopBottomFunDef.class, MEMBER, NUMERIC)         // Member -> Set
            .resolvesWithCost(1, DrilldownLevelTopBottomFunDef.class, LEVEL, NUMERIC)          // Level -> Set
            .resolvesWithCost(3, DrilldownLevelTopBottomFunDef.class, SET, NUMERIC, DIMENSION) // Dimension -> Level
            .rejects(SET)                          // arity 1, Count is required
            .rejects()                             // arity 0
            .rejects(SET, STRING)                  // String does not convert to Numeric
            .rejects(SET, NUMERIC, STRING)         // third arg must be a Level (or elided)
            .rejects(SET, NUMERIC, NUMERIC)        // an Order needs the elided-Level ", ," form
            .rejects(SET, NUMERIC, LEVEL, LEVEL)   // fourth arg must be an Order expression
            .rejects(SET, NUMERIC, LEVEL, NUMERIC, NUMERIC)   // arity 5

            .autoEdgeCases()
            .edgeCaseMdx("count zero",                 "DrilldownLevelBottom([Gender].Members, 0)")
            .edgeCaseMdx("count one",                   "DrilldownLevelBottom([Gender].Members, 1)")
            .edgeCaseMdx("count negative",              "DrilldownLevelBottom([Gender].Members, -1)")
            .edgeCaseMdx("count MAX_VALUE",             "DrilldownLevelBottom([Gender].Members, 2147483647)")
            .edgeCaseMdx("count MIN_VALUE",             "DrilldownLevelBottom([Gender].Members, -2147483648)")
            .edgeCaseMdx("count NULL",                  "DrilldownLevelBottom([Gender].Members, NULL)")
            .edgeCaseMdx("with level",                  "DrilldownLevelBottom([Gender].Members, 5, [Gender].[F].Level)")
            .edgeCaseMdx("with level and order",        "DrilldownLevelBottom([Gender].Members, 5, [Gender].[F].Level, [Measures].[Unit Sales])")
            .edgeCaseMdx("elided level with order",     "DrilldownLevelBottom([Gender].Members, 5, , [Measures].[Unit Sales])")
            .edgeCaseMdx("empty set",                   "DrilldownLevelBottom({}, 5)")

            // [Gender] has one real level beneath (All); its members are leaves, so there are
            // no children to drill down to and the set comes back unchanged either way.
            .value("Count(DrilldownLevelBottom([Gender].Members, 5))", "5")
            .value("SetToStr(DrilldownLevelBottom([Gender].Members, 5))",
                    "{[Gender].[Gender].[All Gender], [Gender].[Gender].[F], [Gender].[Gender].[M], "
                            + "[Gender].[Gender].[F], [Gender].[Gender].[M]}")
            .value("Count(DrilldownLevelBottom([Gender].Members, 0))", "3")   // n <= 0 short-circuits
            .value("Count(DrilldownLevelBottom({}, 5))", "0")

            // No order expression: DrilldownLevelTopBottomFunDef defaults it to
            // CurrentValueUnknownCalc, whose dependsOn always reports true — so the call
            // depends on every hierarchy except the Set argument's own (Gender), which
            // checkAnyDependsButFirst always excludes.
            .doesNotDependOn("DrilldownLevelBottom([Gender].Members, 5)", "[Gender].[Gender]")
            .doesNotDependOn("DrilldownLevelBottom([Gender].Members, 5, , [Measures].[Unit Sales])",
                       "[Measures]")

            .resultStyle("DrilldownLevelBottom([Gender].Members, 5)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("DrilldownLevelBottom([Gender].Members, 5)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("DrilldownLevelBottom([Gender].Members, 5)")

            .build();
}
