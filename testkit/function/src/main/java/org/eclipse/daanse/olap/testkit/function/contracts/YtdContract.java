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
 * The contract of the MDX function {@code Ytd(<Member>)} — a shortcut for {@code
 * PeriodsToDate(<Level Year>, <Member>)}: {@code YtdMultiResolver} wraps a single {@code
 * XtdFunDef} bound to {@code LevelType.TIME_YEARS}, the same shape {@link MtdContract}
 * documents in full for {@code LevelType.TIME_MONTHS} — see that contract's Javadoc for the
 * resolver-safety and Stage-A/B reasoning shared by the whole {@code Mtd}/{@code Qtd}/
 * {@code Wtd}/{@code Ytd} family.
 *
 * <p>Year is the top level of this test kit's {@code [Time]} fixture (Year &gt; Quarter &gt;
 * Month), so — exactly like {@link MtdContract}'s Month-level argument and {@link
 * QtdContract}'s Quarter-level one — a Year-level argument is already at {@code Ytd}'s own
 * target level: {@code FunUtil.periodsToDate}'s climb/descend/range degenerates to the
 * single-element list {@code [member]}, regardless of whether {@code [Time]} even has a
 * parent above Year (an {@code All} member or none) — nothing above the target level is ever
 * consulted in this trace.
 */
public final class YtdContract {

    private YtdContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Ytd")
            .signatures("<Set> Ytd(<Member>)")
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
            .edgeCaseMdx("no argument (implicit current Time member)", "Ytd()")
            .edgeCaseMdx("year-level member",   "Ytd([Time].[1997])")
            .edgeCaseMdx("member NULL",         "Ytd(NULL)")

            .value("Count(Ytd([Time].[1997]))", "1")

            .dependsOn("Ytd([Time].[1997])")
            .dependsOn("Ytd()", "[Time].[Time]", "[Time].[Weekly]")

            .resultStyle("Ytd([Time].[1997])", MUTABLE_LIST, MUTABLE_LIST)
            .resultStyle("Ytd([Time].[1997])", ITERABLE, ITERABLE)
            .independentMutableList("Ytd([Time].[1997])")

            .build();
}
