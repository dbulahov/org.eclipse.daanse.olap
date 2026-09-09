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
 * The contract of the MDX function {@code Qtd(<Member>)} — a shortcut for {@code
 * PeriodsToDate(<Level Quarter>, <Member>)}: {@code QtdMultiResolver} wraps a single {@code
 * XtdFunDef} bound to {@code LevelType.TIME_QUARTERS}, the same shape {@link MtdContract}
 * documents in full for {@code LevelType.TIME_MONTHS} — see that contract's Javadoc for the
 * resolver-safety and Stage-A/B reasoning shared by the whole {@code Mtd}/{@code Qtd}/
 * {@code Wtd}/{@code Ytd} family.
 *
 * <p>Unlike {@code Mtd} (whose Month argument is always this test kit's finest Time level, so
 * {@code FunUtil.periodsToDate} always degenerates to a single-element list), {@code Qtd}'s
 * Quarter level has a finer level (Month) beneath it in this fixture — passing a Month-level
 * member would exercise the real multi-element "quarter to date" range, but {@code
 * CatalogReader.getMemberRange}'s exact traversal has no implementation in this repository to
 * trace (it lives in the Rolap engine), so — matching {@code MtdContract}'s same choice for
 * the same reason — the one asserted value here uses a Quarter-level argument instead,
 * which degenerates to a single-element list exactly like {@code Mtd}'s does.
 */
public final class QtdContract {

    private QtdContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Qtd")
            .signatures("<Set> Qtd(<Member>)")
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
            .edgeCaseMdx("no argument (implicit current Time member)", "Qtd()")
            .edgeCaseMdx("quarter-level member", "Qtd([Time].[1997].[Q2])")
            .edgeCaseMdx("member NULL",          "Qtd(NULL)")

            .value("Count(Qtd([Time].[1997].[Q2]))", "1")

            .dependsOn("Qtd([Time].[1997].[Q2])", "[Time].[Time]")
            .dependsOn("Qtd()", "[Time].[Time]")

            .resultStyle("Qtd([Time].[1997].[Q2])", MUTABLE_LIST, MUTABLE_LIST)
            .resultStyle("Qtd([Time].[1997].[Q2])", ITERABLE, ITERABLE)
            .independentMutableList("Qtd([Time].[1997].[Q2])")

            .build();
}
