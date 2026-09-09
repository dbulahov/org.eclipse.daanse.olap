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
import static org.eclipse.daanse.olap.api.DataType.SYMBOL;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.drilldownlevel.DrilldownLevelFunDef;

/**
 * The contract of the MDX function {@code DrilldownLevel}. Five overloads share the atom:
 * {@code (Set)}, {@code (Set, Level)}, and three that use an {@code <Empty>}-typed parameter
 * to mark an elided position — {@code (Set, , Index)}, {@code (Set, , Index, Symbol)} and
 * {@code (Set, , , Symbol)} — the classic "double comma" MDX syntax for skipping the Level
 * argument while still supplying an Index and/or the {@code INCLUDE_CALC_MEMBERS} flag. An
 * {@code <Empty>}-typed argument only ever matches an {@code <Empty>} parameter: unlike every
 * other category, {@code TypeUtil.canConvert} never coerces it to anything else.
 */
public final class DrilldownLevelContract {

    private DrilldownLevelContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("DrilldownLevel")
            .signatures(
                    "<Set> DrilldownLevel(<Set>)",
                    "<Set> DrilldownLevel(<Set>, <Level>)",
                    "<Set> DrilldownLevel(<Set>, <Empty>, <Numeric Expression>)",
                    "<Set> DrilldownLevel(<Set>, <Empty>, <Numeric Expression>, <Symbol>)",
                    "<Set> DrilldownLevel(<Set>, <Empty>, <Empty>, <Symbol>)")
            .returns(SET)
            .arity(1, 4)
            .reservedWords("INCLUDE_CALC_MEMBERS")

            .resolvesTo(DrilldownLevelFunDef.class, SET)
            .resolvesTo(DrilldownLevelFunDef.class, SET, LEVEL)
            .resolvesTo(DrilldownLevelFunDef.class, SET, EMPTY, NUMERIC)
            .resolvesTo(DrilldownLevelFunDef.class, SET, EMPTY, NUMERIC, SYMBOL)
            .resolvesTo(DrilldownLevelFunDef.class, SET, EMPTY, EMPTY, SYMBOL)
            .resolvesWithCost(2, DrilldownLevelFunDef.class, MEMBER)          // Member -> Set
            .resolvesWithCost(1, DrilldownLevelFunDef.class, LEVEL)           // Level -> Set
            .resolvesWithCost(3, DrilldownLevelFunDef.class, SET, DIMENSION)  // Dimension -> Level
            .rejects(NUMERIC)              // arity 1, not Set-convertible
            .rejects(SET, STRING)          // arity 2, String does not convert to Level
            .rejects(SET, NUMERIC)         // an Index needs the elided-Level ", ," form
            .rejects(SET, LEVEL, NUMERIC)  // a real Level can't be combined with an Index
            .rejects()                     // arity 0
            .rejects(SET, EMPTY, NUMERIC, NUMERIC, SYMBOL)   // arity 5

            .autoEdgeCases()
            .edgeCaseMdx("no level or index",                     "DrilldownLevel([Gender].Members)")
            .edgeCaseMdx("with level",                            "DrilldownLevel([Gender].Members, [Gender].[F].Level)")
            .edgeCaseMdx("with index",                            "DrilldownLevel([Gender].Members, , 0)")
            .edgeCaseMdx("index one",                              "DrilldownLevel([Gender].Members, , 1)")
            .edgeCaseMdx("negative index",                        "DrilldownLevel([Gender].Members, , -1)")
            .edgeCaseMdx("index MAX_VALUE",                        "DrilldownLevel([Gender].Members, , 2147483647)")
            .edgeCaseMdx("index MIN_VALUE",                        "DrilldownLevel([Gender].Members, , -2147483648)")
            // A null Index is treated as out of range (set returned unchanged) —
            // DrilldownLevelWithIndexCalc checks "index == null || index < 0 || index >= arity"
            // before ever unboxing index.
            .edgeCaseMdx("index NULL",                             "DrilldownLevel([Gender].Members, , NULL)")
            .edgeCaseMdx("with index and INCLUDE_CALC_MEMBERS",   "DrilldownLevel([Gender].Members, , 0, INCLUDE_CALC_MEMBERS)")
            .edgeCaseMdx("INCLUDE_CALC_MEMBERS only",              "DrilldownLevel([Gender].Members, , , INCLUDE_CALC_MEMBERS)")
            // "RECURSIVE" is not DrilldownLevel's own reserved word, but reserved-word
            // recognition is global (see DescendantsContract's matching case): DrilldownMember
            // registers it, so it still parses as a SYMBOL literal here. Unlike Descendants/
            // DrilldownMember, this neither crashes nor is validated: DrilldownLevelFunDef
            // checks "INCLUDE_CALC_MEMBERS.equals(literal.getValue())" directly (no
            // FunUtil.getLiteralArg call), so any other symbol just silently means
            // includeCalcMembers = false — a wrong flag is misread, not rejected.
            .edgeCaseMdx("symbol reserved by another function",
                         "DrilldownLevel([Gender].Members, , 0, RECURSIVE)")
            .edgeCaseMdx("empty set",                              "DrilldownLevel({})")

            // [Gender] has one real level beneath (All); its members are leaves, so drilling
            // down without a Level argument (which searches the deepest depth already present)
            // finds no children to add and returns the set unchanged.
            .value("Count(DrilldownLevel([Gender].Members))", "2")
            .value("SetToStr(DrilldownLevel([Gender].Members))", "{[Gender].[F], [Gender].[M]}")
            .value("Count(DrilldownLevel({}))", "0")
            .value("Count(DrilldownLevel([Gender].Members, , NULL))", "2")

            .dependsOn("DrilldownLevel([Gender].Members)", "[Gender].[Gender]")
            .dependsOn("DrilldownLevel([Gender].Members, [Gender].[F].Level)", "[Gender].[Gender]")

            .resultStyle("DrilldownLevel([Gender].Members)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("DrilldownLevel([Gender].Members)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("DrilldownLevel([Gender].Members)")

            .build();
}
