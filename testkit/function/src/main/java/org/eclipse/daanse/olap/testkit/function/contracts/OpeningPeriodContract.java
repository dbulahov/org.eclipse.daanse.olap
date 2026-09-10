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
 * The contract of the MDX function {@code OpeningPeriod([<Level>[, <Member>]])} — the first
 * descendant of a member at a level. Shares {@code OpeningClosingPeriodFunDef}/{@code
 * OpeningClosingPeriodCalc} with {@link ClosingPeriodContract} (constructed with {@code
 * opening=true} instead of {@code false}) — see that contract's Javadoc for the calc mechanics.
 * {@code OpeningPeriodResolved} is, like {@code ClosingPeriodResolved}, an {@code
 * AbstractFunctionDefinitionMultiResolver} over four declared overloads (0-arg, {@code (Level)},
 * {@code (Level, Member)}, {@code (Member)}), all sharing one {@code OpeningClosingPeriodFunDef}
 * class (constructed with different metadata).
 *
 * <p>{@code OpeningPeriodResolved} used to declare a single {@code FunctionMetaData} with two
 * trailing optional parameters ({@code LEVEL}, then {@code MEMBER}) instead — the same shape
 * {@link PeriodsToDateContract} documents for {@code PeriodsToDateResolver}, and the same shape
 * {@code ClosingPeriodResolved} itself used to have before its own fix (see that contract's
 * Javadoc): {@code FunctionMetaDataMatcher.match}'s positional, greedy walk always tries the
 * first optional parameter before the second, so a genuine one-arg {@code Member} call —
 * e.g. {@code OpeningPeriod([Gender].[F].Parent)} — was captured by the {@code LEVEL} slot via
 * {@code Member -> Level} conversion (cost 1) instead of the {@code MEMBER} slot it actually
 * matches exactly. Fixed identically to {@code ClosingPeriodResolved}: split into four separate
 * declared overloads with {@code (Member)} listed before {@code (Level)} (both arity 1), so
 * {@code AbstractFunctionDefinitionMultiResolver.resolve()}'s first-match-wins list walk tries
 * the exact {@code Member} match before ever reaching the {@code Level} overload's conversion
 * path; a {@code Level} argument never converts to {@code Member}, so the {@code (Level)}
 * overload's own resolution is unaffected. The 1-arg {@code (Member)} value case below exercises
 * exactly the call shape this bug broke.
 *
 * <p>{@code OpeningClosingPeriodFunDef.getResultType}/{@code compileCall} touch {@code
 * evaluator.getCube()}/{@code validator.getQuery().getCube()} for the 0-arg form and the
 * {@code (Level)} 1-arg form (defaulting to the cube's Time hierarchy) — inert here for
 * resolution (Stage A), but real once a connection reaches Stage B.
 *
 * <p>{@code compileCall} used to switch purely on {@code args.length}: <em>any</em> one-arg
 * call — including the genuine {@code (Member)} overload the resolver fix above routes
 * correctly — was compiled as {@code (Level, implicit default-Time-member)}, discarding the
 * actual Member argument. Fixed identically to {@link ClosingPeriodContract}: {@code
 * compileCall} now checks {@code getFunctionMetaData().parameters()[0].dataType()} for the
 * arity-1 case, telling {@code (Member)} apart from {@code (Level)} at compile time. The 1-arg
 * {@code (Member)} value cases below (verified against a real connection) exercise exactly the
 * call shape this bug broke.
 */
public final class OpeningPeriodContract {

    private OpeningPeriodContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("OpeningPeriod")
            .signatures(
                    "<Member> OpeningPeriod()",
                    "<Member> OpeningPeriod(<Member>)",
                    "<Member> OpeningPeriod(<Level>)",
                    "<Member> OpeningPeriod(<Level>, <Member>)")
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
            .edgeCaseMdx("no arguments (implicit current Time member)", "OpeningPeriod()")
            .edgeCaseMdx("member only",         "OpeningPeriod([Gender].[F].Parent)")
            .edgeCaseMdx("member with no descendant (null result)", "OpeningPeriod([Gender].[F])")
            .edgeCaseMdx("level and member",    "OpeningPeriod([Gender].[F].Level, [Gender].[F].Parent)")

            // [Gender] is flat (hasAll=true, 2 levels: All and Gender). Children of the All
            // member are {F, M} in that order (HeadContract's established ordering), so the
            // *first* descendant at the Gender level is F — for both the explicit (Level,
            // Member) form and the 1-arg (Member) form (which derives "member's level + 1"
            // itself). The 1-arg case is exactly the shape the resolution-order bug above broke.
            .value("(OpeningPeriod([Gender].[F].Level, [Gender].[F].Parent) IS [Gender].[F])", "true")
            .value("(OpeningPeriod([Gender].[F].Parent) IS [Gender].[F])", "true")
            // F is already the deepest real level: there is no level below it to descend to.
            .value("(OpeningPeriod([Gender].[F]) IS [Gender].[F].Parent.Parent)", "true")

            .dependsOn("OpeningPeriod([Gender].[F].Level, [Gender].[F].Parent)")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")
            .build();
}
