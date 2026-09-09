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
import static org.eclipse.daanse.olap.api.DataType.HIERARCHY;
import static org.eclipse.daanse.olap.api.DataType.INTEGER;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.lastperiods.LastPeriodsFunDef;

/**
 * The contract of the MDX function {@code LastPeriods}. One overload, {@code (Index,
 * Member?)}: with the Member argument omitted, {@code getResultType}/{@code compileCall} fall
 * back to {@code Cube.getTimeHierarchy(...).CurrentMember} — real cube metadata, so that path
 * is only exercised through the {@code Member}-supplied edge cases and values below; the
 * default-Time edge case is included purely for resolution/structural coverage.
 *
 * <p>{@code LastPeriodsCalc.lastPeriods} used to check {@code indexValue == 0} — auto-unboxing
 * a null {@code Integer} — before a null Index was guarded; a literal NULL Index now returns
 * the empty set like {@code indexValue == 0} does, matching {@link HeadContract}/{@link
 * TailContract}'s {@code count == null} guard.
 */
public final class LastPeriodsContract {

    private LastPeriodsContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("LastPeriods")
            .signatures("<Set> LastPeriods(<Numeric Expression>, <Member>)")
            .returns(SET)
            .arity(1, 2)                          // the second parameter is .asOptional()

            .resolvesTo(LastPeriodsFunDef.class, NUMERIC)
            .resolvesTo(LastPeriodsFunDef.class, NUMERIC, MEMBER)
            .resolvesTo(LastPeriodsFunDef.class, INTEGER)          // Integer -> Numeric, free
            .resolvesWithCost(1, LastPeriodsFunDef.class, NUMERIC, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, LastPeriodsFunDef.class, NUMERIC, DIMENSION)   // Dimension -> Member
            .rejects(SET)                  // Set does not convert to Numeric
            .rejects(NUMERIC, SET)         // Set does not convert to Member
            .rejects()                     // arity 0
            .rejects(NUMERIC, MEMBER, MEMBER)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("index zero",                 "LastPeriods(0, [Gender].[F])")
            .edgeCaseMdx("index one",                   "LastPeriods(1, [Gender].[F])")
            .edgeCaseMdx("index negative one",          "LastPeriods(-1, [Gender].[F])")
            .edgeCaseMdx("index NULL",                  "LastPeriods(NULL, [Gender].[F])")
            .edgeCaseMdx("index beyond the start",      "LastPeriods(1000, [Gender].[M])")
            .edgeCaseMdx("negative index beyond the end", "LastPeriods(-1000, [Gender].[F])")
            .edgeCaseMdx("null member",                 "LastPeriods(2, [Gender].[F].Parent.Parent)")
            .edgeCaseMdx("default member (Time.CurrentMember)", "LastPeriods(3)")

            // [Gender] is a single flat level with two members, F (index 0) then M (index 1);
            // LastPeriods treats it as an ordered "period" axis just like Time. A positive
            // Index counts backward from Member; a negative Index counts forward from Member.
            .value("Count(LastPeriods(0, [Gender].[F]))",   "0")
            .value("Count(LastPeriods(1, [Gender].[F]))",   "1")
            .value("Count(LastPeriods(NULL, [Gender].[F]))", "0")
            .value("SetToStr(LastPeriods(2, [Gender].[M]))", "{[Gender].[F], [Gender].[M]}")
            .value("SetToStr(LastPeriods(-2, [Gender].[F]))", "{[Gender].[F], [Gender].[M]}")
            // Falling off the start/end of the level clamps to the first/last member instead
            // of returning fewer periods than asked for.
            .value("SetToStr(LastPeriods(1000, [Gender].[M]))", "{[Gender].[F], [Gender].[M]}")

            .dependsOn("LastPeriods(2, [Gender].[M])", "[Gender].[Gender]")

            .resultStyle("LastPeriods(2, [Gender].[M])", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("LastPeriods(2, [Gender].[M])", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("LastPeriods(2, [Gender].[M])")

            .build();
}
