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
 * The contract of the MDX function {@code DrilldownLevelTop} — the {@code top=true} sibling
 * of {@link DrilldownLevelBottomContract}, sharing the same {@code DrilldownLevelTopBottomFunDef}
 * and overload shapes under a different atom. See {@code DrilldownLevelBottomContract} for the
 * "double comma" elided-Level overload and the {@code checkAnyDependsButFirst} dependency
 * behavior, both identical here.
 */
public final class DrilldownLevelTopContract {

    private DrilldownLevelTopContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("DrilldownLevelTop")
            .signatures(
                    "<Set> DrilldownLevelTop(<Set>, <Numeric Expression>)",
                    "<Set> DrilldownLevelTop(<Set>, <Numeric Expression>, <Level>)",
                    "<Set> DrilldownLevelTop(<Set>, <Numeric Expression>, <Level>, <Numeric Expression>)",
                    "<Set> DrilldownLevelTop(<Set>, <Numeric Expression>, <Empty>, <Numeric Expression>)")
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
            .edgeCaseMdx("count zero",                 "DrilldownLevelTop([Gender].Members, 0)")
            .edgeCaseMdx("count one",                   "DrilldownLevelTop([Gender].Members, 1)")
            .edgeCaseMdx("count negative",              "DrilldownLevelTop([Gender].Members, -1)")
            .edgeCaseMdx("count MAX_VALUE",             "DrilldownLevelTop([Gender].Members, 2147483647)")
            .edgeCaseMdx("count MIN_VALUE",             "DrilldownLevelTop([Gender].Members, -2147483648)")
            .edgeCaseMdx("count NULL",                  "DrilldownLevelTop([Gender].Members, NULL)")
            .edgeCaseMdx("with level",                  "DrilldownLevelTop([Gender].Members, 5, [Gender].[F].Level)")
            .edgeCaseMdx("with level and order",        "DrilldownLevelTop([Gender].Members, 5, [Gender].[F].Level, [Measures].[Unit Sales])")
            .edgeCaseMdx("elided level with order",     "DrilldownLevelTop([Gender].Members, 5, , [Measures].[Unit Sales])")
            .edgeCaseMdx("empty set",                   "DrilldownLevelTop({}, 5)")

            // [Gender] has one real level beneath (All); its members are leaves, so there are
            // no children to drill down to and the set comes back unchanged either way.
            .value("Count(DrilldownLevelTop([Gender].Members, 5))", "2")
            .value("SetToStr(DrilldownLevelTop([Gender].Members, 5))", "{[Gender].[F], [Gender].[M]}")
            .value("Count(DrilldownLevelTop([Gender].Members, 0))", "2")   // n <= 0 short-circuits
            .value("Count(DrilldownLevelTop({}, 5))", "0")

            .dependsOn("DrilldownLevelTop([Gender].Members, 5)")   // depends on nothing: see class comment
            .dependsOn("DrilldownLevelTop([Gender].Members, 5, , [Measures].[Unit Sales])",
                       "[Measures].[Measures]")                    // NOT [Gender].[Gender]

            .resultStyle("DrilldownLevelTop([Gender].Members, 5)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("DrilldownLevelTop([Gender].Members, 5)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("DrilldownLevelTop([Gender].Members, 5)")

            .build();
}
