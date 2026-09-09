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

import org.eclipse.daanse.olap.function.def.openingclosingperiod.OpeningClosingPeriodFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code ClosingPeriod([<Level>[, <Member>]])} — the last
 * descendant of a member at a level. {@code ClosingPeriodResolved} is an {@code
 * AbstractFunctionDefinitionMultiResolver} over four declared overloads (0-arg, {@code
 * (Level)}, {@code (Level, Member)}, {@code (Member)}), all sharing one {@code
 * OpeningClosingPeriodFunDef} class (constructed with different metadata).
 *
 * <p>{@code AbstractFunctionDefinitionMultiResolver.resolve()} returns the <em>first</em>
 * declaration in its list that {@code FunctionMetaDataMatcher.match} accepts — it does not
 * compare conversion cost across the whole list the way resolution across separate resolvers
 * does. A {@code Member} argument converts to {@code Level} at cost 1 ({@code
 * TypeUtil.convertFromMember}), so with the declared order {@code (Level)} before {@code
 * (Member)} (both arity 1), a genuine {@code ClosingPeriod(<Member>)} call was being captured
 * by the {@code (Level)} overload — treating the sole argument as a Level and substituting the
 * default Time hierarchy's current member for the intended Member argument — instead of the
 * {@code (Member)} overload it actually matches exactly. Fixed by reordering the list so
 * {@code (Member)} is tried before {@code (Level)}; a {@code Level} argument never converts to
 * {@code Member} ({@code TypeUtil.convertFromLevel} has no {@code MEMBER} case), so the
 * {@code (Level)} overload's own resolution is unaffected by the reordering. The 1-arg {@code
 * (Member)} value case below exercises exactly the call shape this bug broke.
 *
 * <p>{@code OpeningClosingPeriodFunDef.getResultType}/{@code compileCall} touch {@code
 * evaluator.getCube()}/{@code validator.getQuery().getCube()} for the 0- and 1-arg forms
 * (defaulting to the cube's Time hierarchy) — inert here since {@code CallAssert} only ever
 * calls {@code resolve()}, never {@code getResultType}, and this module never reaches Stage B.
 */
public final class ClosingPeriodContract {

    private ClosingPeriodContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("ClosingPeriod")
            .signatures(
                    "<Member> ClosingPeriod()",
                    "<Member> ClosingPeriod(<Member>)",
                    "<Member> ClosingPeriod(<Level>)",
                    "<Member> ClosingPeriod(<Level>, <Member>)")
            .returns(MEMBER)
            .arity(0, 2)

            .resolvesTo(OpeningClosingPeriodFunDef.class)                 // 0-arg
            .resolvesTo(OpeningClosingPeriodFunDef.class, MEMBER)
            .resolvesTo(OpeningClosingPeriodFunDef.class, LEVEL)
            .resolvesTo(OpeningClosingPeriodFunDef.class, LEVEL, MEMBER)
            .resolvesWithCost(1, OpeningClosingPeriodFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, OpeningClosingPeriodFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(SET)          // Set converts to neither Member nor Level
            .rejects(NUMERIC)
            .rejects(MEMBER, MEMBER, MEMBER)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("no arguments (implicit current Time member)", "ClosingPeriod()")
            .edgeCaseMdx("member only",         "ClosingPeriod([Gender].[F].Parent)")
            .edgeCaseMdx("member with no descendant (null result)", "ClosingPeriod([Gender].[F])")
            .edgeCaseMdx("level and member",    "ClosingPeriod([Gender].[Gender], [Gender].[F].Parent)")

            // [Gender] is flat (hasAll=true, 2 levels: All and Gender). Children of the All
            // member are {F, M} in that order (HeadContract's established ordering), so the
            // *last* descendant at the Gender level is M — for both the explicit (Level,
            // Member) form and the 1-arg (Member) form (which derives "member's level + 1"
            // itself). The 1-arg case is exactly the shape the resolution-order bug above broke.
            .value("(ClosingPeriod([Gender].[Gender], [Gender].[F].Parent) IS [Gender].[M])", "true")
            .value("(ClosingPeriod([Gender].[F].Parent) IS [Gender].[M])", "true")
            // F is already the deepest real level: there is no level below it to descend to.
            .value("(ClosingPeriod([Gender].[F]) IS [Gender].[F].Parent.Parent)", "true")

            .dependsOn("ClosingPeriod([Gender].[Gender], [Gender].[F].Parent)", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")
            .build();
}
