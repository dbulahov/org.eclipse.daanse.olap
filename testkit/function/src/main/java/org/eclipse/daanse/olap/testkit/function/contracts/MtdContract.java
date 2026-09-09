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
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.calc.ResultStyle.ITERABLE;
import static org.eclipse.daanse.olap.api.calc.ResultStyle.MUTABLE_LIST;

import org.eclipse.daanse.olap.function.def.periodstodate.xtd.XtdFunDef;

/**
 * The contract of the MDX function {@code Mtd(<Member>)} — a shortcut for {@code
 * PeriodsToDate(<Level Month>, <Member>)}: {@code MtdMultiResolver} wraps a single {@code
 * XtdFunDef} bound to {@code LevelType.TIME_MONTHS}, exactly like {@code Qtd}/{@code Wtd}/
 * {@code Ytd} bind the Quarter/Week/Year level types instead.
 *
 * <p>{@code MtdMultiResolver} is an {@code AbstractFunctionDefinitionMultiResolver} over one
 * declared overload with an optional {@code Member} parameter — {@code resolve()} delegates
 * to the generic {@code FunctionMetaDataMatcher.match}, so there is no hand-written resolver
 * code that could diverge from the declared signature or throw instead of returning empty.
 *
 * <p>{@code XtdFunDef.getResultType} and {@code compileCall} both touch {@code
 * evaluator.getCube()} — for a zero-argument call, {@code getResultType} calls {@code
 * validator.getQuery().getCube()} to guess the default Time hierarchy, and for any argument
 * whose dimension is not {@code TIME_DIMENSION} it throws a diagnosed {@code
 * OlapRuntimeException}. Neither of those runs during {@code resolve()} (Stage A) — this test
 * kit's {@code CallAssert} only ever calls {@code resolve()}, never {@code getResultType} —
 * so both are inert here regardless of the (cube-free) stub category probed, the same
 * treatment as {@code XtdFunDef.getFirstTimeLevel}'s cube walk in {@code compileCall}.
 *
 * <p>{@code XtdWithMemberCalc} evaluates via {@code FunUtil.periodsToDate(evaluator, level,
 * member)} with {@code level} fixed to the cube's Month level: it climbs {@code member} up to
 * its Month-level ancestor, descends back down to {@code member}'s own original level, then
 * ranges from the first such descendant to {@code member}. When the argument is itself a
 * Month-level member (as it always is for this test kit's Time fixture, which has no level
 * below Month) that range degenerates to the single-element list {@code [member]} — {@code
 * Count(Mtd(<a Month member>))} is always {@code 1} here, not the usual multi-day range a
 * finer-grained Time hierarchy would produce. {@code XtdWithoutMemberCalc.dependsOn} is
 * overridden to report a dependency on every Time-typed hierarchy, not just the one the
 * implicit current member comes from.
 */
public final class MtdContract {

    private MtdContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Mtd")
            .signatures("<Set> Mtd(<Member>)")
            .returns(SET)
            .arity(0, 1)

            .resolvesTo(XtdFunDef.class)              // zero-arg: implicit current Time member
            .resolvesTo(XtdFunDef.class, MEMBER)
            .resolvesWithCost(1, XtdFunDef.class, HIERARCHY)   // Hierarchy -> Member (implicit CurrentMember)
            .resolvesWithCost(2, XtdFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(NUMERIC)          // Numeric does not convert to Member
            .rejects(SET)               // Set converts to nothing (convertFromSet always false)
            .rejects(LEVEL)              // Level converts to Dimension/Hierarchy/Set, never Member
            .rejects(MEMBER, MEMBER)    // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("no argument (implicit current Time member)", "Mtd()")
            .edgeCaseMdx("month-level member",  "Mtd([Time].[1997].[Q1].[2])")
            .edgeCaseMdx("member NULL",         "Mtd(NULL)")

            .value("Count(Mtd([Time].[1997].[Q1].[2]))", "1")

            .dependsOn("Mtd([Time].[1997].[Q1].[2])", "[Time].[Time]")
            .dependsOn("Mtd()", "[Time].[Time]")

            .resultStyle("Mtd([Time].[1997].[Q1].[2])", MUTABLE_LIST, MUTABLE_LIST)
            .resultStyle("Mtd([Time].[1997].[Q1].[2])", ITERABLE, ITERABLE)
            .independentMutableList("Mtd([Time].[1997].[Q1].[2])")

            .build();
}
