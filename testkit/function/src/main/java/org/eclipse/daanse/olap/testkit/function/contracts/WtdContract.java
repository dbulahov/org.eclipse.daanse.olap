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

import org.eclipse.daanse.olap.function.def.periodstodate.xtd.XtdFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Wtd(<Member>)} — a shortcut for {@code
 * PeriodsToDate(<Level Week>, <Member>)}: {@code WtdMultiResolver} wraps a single {@code
 * XtdFunDef} bound to {@code LevelType.TIME_WEEKS}, the same shape {@link MtdContract}
 * documents in full for {@code LevelType.TIME_MONTHS} — see that contract's Javadoc for the
 * resolver-safety and Stage-A/B reasoning shared by the whole {@code Mtd}/{@code Qtd}/
 * {@code Wtd}/{@code Ytd} family.
 *
 * <p>Unlike {@link MtdContract}/{@link QtdContract}, no assertion here names a concrete
 * Week-level member: the {@code [Time]} default hierarchy this test kit's fixture is known to
 * use for Month/Quarter (Year &gt; Quarter &gt; Month) has no Week level at all — a real Week
 * hierarchy, if this fixture even declares one, would be a differently-shaped, unverified
 * hierarchy this module has no schema resource to confirm. RESULT and RESULT_SHAPE are waived
 * outright (no member this contract can safely name, and no known value for the zero-arg form
 * either without a real query context); the zero-argument form needs no member reference, so
 * it is the only DEPENDENCIES case exercised below (mirroring {@code
 * XtdWithoutMemberCalc.dependsOn}'s "depends on every Time-typed hierarchy" override that
 * {@link MtdContract} documents in full).
 */
public final class WtdContract {

    private WtdContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Wtd")
            .signatures("<Set> Wtd(<Member>)")
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
            .edgeCaseMdx("no argument (implicit current Time member)", "Wtd()")
            .edgeCaseMdx("member NULL",                                "Wtd(NULL)")

            .dependsOn("Wtd()", "[Time].[Time]")

            .waive(Promise.RESULT,
                    "no Week-level member is known to exist/be nameable in this test kit's Time "
                            + "fixture (only Year > Quarter > Month is established, see the class "
                            + "Javadoc) — the zero-arg form has no value to assert either, since its "
                            + "result depends on the query's implicit current Time member, which this "
                            + "cube-free module never sets up. Moot regardless: this module never "
                            + "reaches Stage B (no test overrides connection()).")
            .waive(Promise.RESULT_SHAPE,
                    "same reasoning as Promise.RESULT above: no member argument this contract can "
                            + "safely name, and Stage B never runs here regardless.")

            .build();
}
