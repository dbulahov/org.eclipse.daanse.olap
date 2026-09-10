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
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.olap.function.def.parallelperiod.ParallelPeriodFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code ParallelPeriod([<Level>[, <Numeric Expression>[,
 * <Member>]]])} — a member from a prior period in the same relative position as a specified
 * member. {@code ParallelPeriodResolver} is, like {@code ClosingPeriodResolved}/{@code
 * OpeningPeriodResolved}, an {@code AbstractFunctionDefinitionMultiResolver} over four declared
 * overloads (0-arg, {@code (Level)}, {@code (Level, Numeric)}, {@code (Level, Numeric, Member)}),
 * all sharing one {@code ParallelPeriodFunDef} class (constructed with different metadata) — the
 * exact four shapes its own source comment already documented ({@code {"fm", "fml", "fmln",
 * "fmlnm"}}: no params, {@code Level}, {@code Level+Numeric}, {@code Level+Numeric+Member}).
 *
 * <p>{@code ParallelPeriodResolver} used to declare a single {@code FunctionMetaData} with all
 * three parameters trailing-optional instead — the same shape {@link PeriodsToDateContract}
 * documents for {@code PeriodsToDateResolver}, and the same shape {@code ClosingPeriodResolved}/
 * {@code OpeningPeriodResolved} themselves used to have before their own fixes (see {@link
 * ClosingPeriodContract}'s Javadoc): {@code FunctionMetaDataMatcher.match}'s positional, greedy
 * walk skips a parameter it cannot bind and falls through to the next one instead of rejecting
 * the call outright. With three optional slots here instead of two, this produced more instances
 * than the {@code Member}-vs-{@code Level} mixup {@link OpeningPeriodContract} found: a bare
 * one-arg {@code NUMERIC} or {@code TUPLE} call (neither converts to {@code LEVEL}) fell through
 * into the {@code NUMERIC} slot, and a bare one-arg {@code HIERARCHY} call (converts to neither
 * {@code LEVEL} nor {@code NUMERIC}) fell through all the way into the {@code MEMBER} slot — all
 * silently <em>accepted</em> at resolution instead of being rejected, only to fail later in
 * {@code ParallelPeriodFunDef.compileCall}, which unconditionally compiles a one-arg call's sole
 * argument as a {@code Level}. Fixed identically to {@code ClosingPeriodResolved}/{@code
 * OpeningPeriodResolved}: split into four separate declared overloads, each with only required
 * parameters, so a category that cannot bind the parameter at its position fails the whole
 * overload immediately instead of being skipped and retried against a later, unrelated
 * parameter. Unlike {@code ClosingPeriodResolved}'s two same-arity overloads, these four never
 * overlap in arity, so list order does not affect resolution.
 *
 * <p>A {@code Member} argument still resolves in the {@code NUMERIC} position of the two- and
 * three-arg overloads (e.g. {@code ParallelPeriod([Time].[Quarter], [Time].[1997].[Q1])}) via
 * the ordinary {@code Member -> Numeric} conversion (cost 3) — the same conversion {@link
 * AbsContract} already accepts for {@code Abs(<Member>)}. That is normal MDX type coercion, not
 * a resolution-order bug, and is unaffected by this fix.
 *
 * <p>The fully-specified three-arg {@code (Level, Numeric, Member)} form needs no cube default
 * and reaches a real, verifiable computation: {@code ParallelPeriodCalc.parallelPeriod} climbs
 * from {@code member} to an ancestor at {@code ancestorLevel}'s depth, steps that ancestor back
 * {@code lagValue} positions ({@code CatalogReader.getLeadMember(ancestor, -lagValue)} — the
 * same navigation {@link NextMemberContract}/{@link LeadContract} already exercise), then finds
 * {@code member}'s cousin under the shifted ancestor. RESULT/DEPENDENCIES/RESULT_SHAPE are
 * asserted against that form only; the 0- and 1-arg forms still touch {@code
 * evaluator.getCube()}/{@code validator.getQuery().getCube()} for their implicit Time hierarchy
 * default, inert here since this module never reaches Stage B (no test overrides {@code
 * connection()}), the same reasoning {@link ClosingPeriodContract} gives for its own cube access.
 */
public final class ParallelPeriodContract {

    private ParallelPeriodContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("ParallelPeriod")
            .signatures(
                    "<Member> ParallelPeriod()",
                    "<Member> ParallelPeriod(<Level>)",
                    "<Member> ParallelPeriod(<Level>, <Numeric Expression>)",
                    "<Member> ParallelPeriod(<Level>, <Numeric Expression>, <Member>)")
            .returns(MEMBER)
            .arity(0, 3)

            .resolvesTo(ParallelPeriodFunDef.class)                              // 0-arg
            .resolvesTo(ParallelPeriodFunDef.class, LEVEL)
            .resolvesWithCost(1, ParallelPeriodFunDef.class, MEMBER)             // Member -> Level
            .resolvesWithCost(3, ParallelPeriodFunDef.class, DIMENSION)          // Dimension -> Level
            .rejects(HIERARCHY)       // Hierarchy does not convert to Level
            .rejects(NUMERIC)         // Numeric does not convert to Level
            .rejects(TUPLE)           // Tuple does not convert to Level
            .rejects(SET)
            .rejects(STRING)

            .resolvesTo(ParallelPeriodFunDef.class, LEVEL, NUMERIC)
            .resolvesWithCost(1, ParallelPeriodFunDef.class, MEMBER, NUMERIC)    // Member -> Level
            .resolvesWithCost(3, ParallelPeriodFunDef.class, DIMENSION, NUMERIC) // Dimension -> Level
            .rejects(HIERARCHY, NUMERIC)   // Hierarchy does not convert to Level
            .rejects(LEVEL, SET)           // Set does not convert to Numeric
            .rejects(LEVEL, STRING)        // String does not convert to Numeric

            .resolvesTo(ParallelPeriodFunDef.class, LEVEL, NUMERIC, MEMBER)
            .resolvesWithCost(1, ParallelPeriodFunDef.class, LEVEL, NUMERIC, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, ParallelPeriodFunDef.class, LEVEL, NUMERIC, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL, NUMERIC, SET)     // Set does not convert to Member
            .rejects(LEVEL, NUMERIC, LEVEL)   // Level does not convert to Member

            .rejects(MEMBER, MEMBER, MEMBER, MEMBER)   // arity 4

            .autoEdgeCases()
            .edgeCaseMdx("no arguments (implicit current Time member)", "ParallelPeriod()")
            .edgeCaseMdx("level only (implicit lag 1, current member)", "ParallelPeriod([Gender].[F].Level)")
            .edgeCaseMdx("level and lag",
                    "ParallelPeriod([Gender].[F].Level, 1)")
            .edgeCaseMdx("level, lag and member",
                    "ParallelPeriod([Gender].[F].Level, 1, [Gender].[M])")
            .edgeCaseMdx("zero lag (same member)",
                    "ParallelPeriod([Gender].[F].Level, 0, [Gender].[M])")
            .edgeCaseMdx("root member (no parent, null result)",
                    "ParallelPeriod([Gender].[F].Level, 1, [Gender].[F].Parent)")

            // [Gender] is a one-level hierarchy under an All member: F is index 0, M is index 1
            // (LeadContract/FirstSiblingContract's established ordering). The ancestor level
            // equals M's own level (distance 0), so this degenerates to "the sibling 1 position
            // back" — the same navigation LeadContract's Lag(1) exercises.
            .value("(ParallelPeriod([Gender].[F].Level, 1, [Gender].[M]) IS [Gender].[F])", "true")
            .value("(ParallelPeriod([Gender].[F].Level, 0, [Gender].[M]) IS [Gender].[M])", "true")

            .dependsOn("ParallelPeriod([Gender].[F].Level, 1, [Gender].[M])")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")
            .build();
}
