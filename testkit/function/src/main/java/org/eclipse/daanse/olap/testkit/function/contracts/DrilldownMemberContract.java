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

import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.DataType.SYMBOL;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.drilldownmember.DrilldownMemberFunDef;

/**
 * The contract of the MDX function {@code DrilldownMember}. One overload, three parameters —
 * {@code (Set1, Set2, Recursive)} — with the third, optional and {@code RECURSIVE}-only,
 * still printed in the declared signature (parameter optionality does not change how
 * {@code FunctionPrinter} renders the overload text). Unlike
 * {@link DrilldownLevelBottomContract}/{@link DrilldownLevelTopContract}, {@code
 * DrilldownMemberCalc} does not override {@code dependsOn}: both set arguments' hierarchies
 * are reported.
 */
public final class DrilldownMemberContract {

    private DrilldownMemberContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("DrilldownMember")
            .signatures("<Set> DrilldownMember(<Set>, <Set>, <Symbol>)")
            .returns(SET)
            .arity(2, 3)
            .reservedWords("RECURSIVE")

            .resolvesTo(DrilldownMemberFunDef.class, SET, SET)
            .resolvesTo(DrilldownMemberFunDef.class, SET, SET, SYMBOL)
            .resolvesWithCost(2, DrilldownMemberFunDef.class, MEMBER, SET)   // Member -> Set
            .resolvesWithCost(2, DrilldownMemberFunDef.class, SET, MEMBER)   // Member -> Set
            .resolvesWithCost(1, DrilldownMemberFunDef.class, LEVEL, SET)    // Level -> Set
            .rejects(SET)                     // arity 1, Set2 is required
            .rejects()                        // arity 0
            .rejects(SET, STRING)             // String does not convert to Set
            .rejects(SET, SET, STRING)        // third arg must be the RECURSIVE symbol
            .rejects(SET, SET, SYMBOL, SYMBOL) // arity 4

            .autoEdgeCases()
            .edgeCaseMdx("empty first set",           "DrilldownMember({}, [Gender].Members)")
            .edgeCaseMdx("empty second set",          "DrilldownMember([Gender].Members, {})")
            .edgeCaseMdx("member present in Set2",    "DrilldownMember([Gender].Members, {[Gender].[F]})")
            .edgeCaseMdx("RECURSIVE flag",            "DrilldownMember([Gender].Members, {[Gender].[F]}, RECURSIVE)")
            .edgeCaseMdx("Set2 from another hierarchy", "DrilldownMember([Gender].Members, {[Measures].[Unit Sales]})")
            // "ALL" is not DrilldownMember's own reserved word, but reserved-word recognition
            // is global (see DescendantsContract's matching case): Except registers it, so it
            // still parses as a SYMBOL literal here. DrilldownMemberFunDef.compileCall then
            // calls FunUtil.getLiteralArg(call, 2, "", List.of("RECURSIVE")), which throws a
            // diagnosed DaanseEvaluationException("Allowed values are: {RECURSIVE}") for the
            // mismatch.
            .edgeCaseMdx("symbol reserved by another function",
                         "DrilldownMember([Gender].Members, {[Gender].[F]}, ALL)")

            // [Gender] members are leaves (one real level beneath (All)): whether or not a
            // member is present in Set2, there are no children to drill down to.
            .value("Count(DrilldownMember([Gender].Members, {[Gender].[F]}))", "2")
            .value("SetToStr(DrilldownMember([Gender].Members, {[Gender].[F]}))", "{[Gender].[F], [Gender].[M]}")
            .value("Count(DrilldownMember({}, [Gender].Members))", "0")
            .value("Count(DrilldownMember([Gender].Members, {}))", "2")

            .dependsOn("DrilldownMember([Gender].Members, {[Gender].[F]})", "[Gender].[Gender]")
            .dependsOn("DrilldownMember([Gender].Members, {[Measures].[Unit Sales]})",
                       "[Gender].[Gender]", "[Measures].[Measures]")

            .resultStyle("DrilldownMember([Gender].Members, {[Gender].[F]})",
                         ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("DrilldownMember([Gender].Members, {[Gender].[F]})",
                         ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("DrilldownMember([Gender].Members, {[Gender].[F]})")

            .build();
}
