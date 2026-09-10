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
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.calc.ResultStyle.ITERABLE;
import static org.eclipse.daanse.olap.api.calc.ResultStyle.MUTABLE_LIST;

import org.eclipse.daanse.olap.function.def.periodstodate.PeriodsToDateFunDef;

/**
 * The contract of the MDX function {@code PeriodsToDate([<Level>[, <Member>]])} — the general
 * form {@code Mtd}/{@code Qtd}/{@code Wtd}/{@code Ytd} (see {@link MtdContract}, {@link
 * QtdContract}) are fixed-level shortcuts for.
 *
 * <p>{@code PeriodsToDateResolver}'s declared parameters used to be {@code SET} and {@code
 * NUMERIC} — copy-paste leftovers that did not match what {@code
 * PeriodsToDateFunDef.compileCall} actually compiles them as ({@code compiler.compileLevel}
 * on arg 0, {@code compiler.compileMember} on arg 1) or the {@code "fxlm"} (Level, Member)
 * signature the source comment documents. A plain Level/Member call still resolved by
 * accident — Level converts to Set at cost 1 and Member converts to Numeric at cost 3, and
 * (per {@link ExtractContract}'s finding) neither conversion is ever materialized, so the
 * expressions {@code compileLevel}/{@code compileMember} received were still genuinely
 * Level/Member-typed — but the wrong declaration also let a genuine {@code Set} or {@code
 * Numeric} argument (e.g. {@code PeriodsToDate([Time].[1997], 5)}) match at cost 0 and then
 * crash inside {@code compileCall} instead of being cleanly rejected at resolution, and it
 * misdeclared the signature MDSCHEMA_FUNCTIONS advertises. Fixed to declare {@code LEVEL} and
 * {@code MEMBER}, matching actual usage. {@code PeriodsToDateFunDef} itself was
 * package-private (unlike its sibling {@code XtdFunDef}) — made public so this contract can
 * name it in {@code resolvesTo(...)}.
 *
 * <p>{@code PeriodsToDateResolver} is an {@code AbstractFunctionDefinitionMultiResolver} —
 * {@code resolve()} delegates to the generic {@code FunctionMetaDataMatcher.match}, so (once
 * the parameter categories above are correct) there is no hand-written resolver code that
 * could diverge from the declared signature or throw instead of returning empty.
 *
 * <p>Like {@code XtdFunDef} ({@link MtdContract}), both {@code getResultType} (for the
 * zero-argument form) and {@code compileCall} touch {@code evaluator.getCube()} /
 * {@code validator.getQuery().getCube()} — inert here since {@code CallAssert} only ever
 * calls {@code resolve()}, never {@code getResultType}, and this module never reaches Stage B.
 * {@code PeriodsToDateCalc.dependsOn} mirrors {@code XtdWithoutMemberCalc}'s override: with an
 * explicit Member argument it reports no dependency beyond the generic child-calc check
 * (a concrete member removes the ambiguity); with no Member it depends on the given Level's
 * hierarchy, or on the cube's entire default Time hierarchy if no Level is given either.
 */
public final class PeriodsToDateContract {

    private PeriodsToDateContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("PeriodsToDate")
            .signatures("<Set> PeriodsToDate(<Level>, <Member>)")
            .returns(SET)
            .arity(0, 2)

            .resolvesTo(PeriodsToDateFunDef.class)                 // zero-arg
            .resolvesTo(PeriodsToDateFunDef.class, LEVEL)
            .resolvesTo(PeriodsToDateFunDef.class, LEVEL, MEMBER)
            .resolvesWithCost(1, PeriodsToDateFunDef.class, MEMBER)      // Member -> Level (single arg)
            .resolvesWithCost(3, PeriodsToDateFunDef.class, DIMENSION)   // Dimension -> Level (single arg)
            .resolvesWithCost(1, PeriodsToDateFunDef.class, LEVEL, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, PeriodsToDateFunDef.class, LEVEL, DIMENSION)   // Dimension -> Member
            .rejects(SET)                 // Set does not convert to Level
            .rejects(NUMERIC)             // Numeric does not convert to Level
            .rejects(LEVEL, SET)          // Set does not convert to Member
            .rejects(LEVEL, STRING)       // String does not convert to Member
            .rejects(LEVEL, MEMBER, MEMBER)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("no arguments (implicit current Time member)", "PeriodsToDate()")
            .edgeCaseMdx("level only (implicit current member)",        "PeriodsToDate([Time].[Quarter])")
            .edgeCaseMdx("level and member",
                    "PeriodsToDate([Time].[Quarter], [Time].[1997].[Q2])")
            .edgeCaseMdx("member NULL", "PeriodsToDate([Time].[Quarter], NULL)")

            // The member argument is itself at the Quarter level, so — same reasoning as
            // MtdContract's Month-level case — FunUtil.periodsToDate's climb/descend/range
            // degenerates to the single-element list [member], not a multi-quarter range.
            .value("Count(PeriodsToDate([Time].[Quarter], [Time].[1997].[Q2]))", "1")

            .dependsOn("PeriodsToDate([Time].[Quarter], [Time].[1997].[Q2])")
            .dependsOn("PeriodsToDate()", "[Time].[Time]")

            .resultStyle("PeriodsToDate([Time].[Quarter], [Time].[1997].[Q2])", MUTABLE_LIST, MUTABLE_LIST)
            .resultStyle("PeriodsToDate([Time].[Quarter], [Time].[1997].[Q2])", ITERABLE, ITERABLE)
            .independentMutableList("PeriodsToDate([Time].[Quarter], [Time].[1997].[Q2])")

            .build();
}
