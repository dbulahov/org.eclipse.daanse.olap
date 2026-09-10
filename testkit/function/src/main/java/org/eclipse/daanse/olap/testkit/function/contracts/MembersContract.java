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
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.set.level.LevelMembersFunDef;
import org.eclipse.daanse.olap.function.def.set.members.MembersFunDef;

/**
 * The contract of the MDX property {@code Members} — {@code <Hierarchy|Level|Dimension>.Members}
 * — not to be confused with the unrelated {@link MembersFunctionContract}, which shares the bare
 * name {@code "Members"} but is a wholly different {@code FunctionOperationAtom} (own package,
 * own resolver, own signature {@code Members(<String Expression>)}).
 *
 * <p>Three resolvers share this {@link PlainPropertyOperationAtom}:
 * <ul>
 *   <li>{@code set.members.MembersResolver} — the real resolver for {@code
 *       "<Hierarchy>.Members"}, wrapping {@link MembersFunDef} over a single {@code HIERARCHY}
 *       parameter.
 *   <li>{@code set.level.LevelMembersResolver} — the real resolver for {@code
 *       "<Level>.Members"}, wrapping {@link LevelMembersFunDef} over a single {@code LEVEL}
 *       parameter.
 *   <li>{@code member.members.NonFunctionMembersResolver} — a {@code NonFunctionResolver} whose
 *       {@code resolve()} always returns {@code Optional.empty()} (matching {@code
 *       Builder.neverResolves()}'s exact reasoning). It contributes only a third declared
 *       overload, {@code "<Dimension>.Members"}, to {@code getRepresentativeFunctionMetaDatas()}
 *       — for MDSCHEMA_FUNCTIONS/documentation purposes — while the real dispatch for a
 *       Dimension calling object still goes through {@code MembersResolver} above (Dimension
 *       converts to Hierarchy at cost 2, cheaper than the cost-3 conversion to Level) — the
 *       same reasoning {@link DefaultMemberContract} gives for its own fictitious {@code
 *       NonFunctionDefaultMemberResolver} overload.
 * </ul>
 * All three {@code resolve()} implementations delegate entirely to the generic {@code
 * FunctionMetaDataMatcher.match} (or, for the {@code NonFunctionResolver}, a constant {@code
 * empty()}), so none can throw or diverge from its own declared signature.
 *
 * <p>A {@code Member} calling object is genuinely ambiguous and is not asserted below — the
 * same shape of gap {@link AllMembersContract} documents for its own {@code Member} argument:
 * Member -&gt; Hierarchy and Member -&gt; Level both cost 1, the two overloads tie, and {@code
 * ValidatorImpl.getDef} fails with "More than one function matches signature". This DSL has no
 * case for "resolves, but ambiguously" ({@code isRejected()} needs zero best matches, {@code
 * resolvesTo(...)} needs exactly one), so there is no case for it here.
 *
 * <p>{@code MembersCalc}/{@code LevelMembersCalc} delegate to {@code FunUtil.hierarchyMembers}/
 * {@code FunUtil.levelMembers} with {@code includeCalculated=false}; the sibling {@code
 * AllMembersCalc} classes pass {@code true} for the same parameter — the one behavioral
 * difference between {@code Members} and {@code AllMembers}. {@code [Gender]} has no calculated
 * members in the Sales cube, so the two properties happen to produce the same set here, exactly
 * as {@link AllMembersContract} notes for the reverse comparison.
 */
public final class MembersContract {

    private MembersContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Members")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Hierarchy>.Members", "<Level>.Members", "<Dimension>.Members")
            .returns(SET)
            .arity(1, 1)

            // Both overloads live under the same atom; the validator keeps whichever is
            // cheaper when a type matches both. A <Level> argument type-matches <Hierarchy>
            // too (Level -> Hierarchy, cost 1) but resolves to the <Level> overload, cost 0.
            .resolvesTo(MembersFunDef.class, HIERARCHY)
            .resolvesTo(LevelMembersFunDef.class, LEVEL)
            .resolvesWithCost(2, MembersFunDef.class, DIMENSION)   // Dimension -> Hierarchy;
                                                                     // cheaper than -> Level (cost 3)
            .rejects(NUMERIC)
            .rejects(SET)
            .rejects(STRING)
            .rejects(TUPLE)
            .rejects()                       // arity 0
            .rejects(HIERARCHY, HIERARCHY)   // arity 2

            // NOTE (verified against StandardFunctions.standard(), not asserted below): a
            // <Member> argument, e.g. [Gender].[F].Members, is genuinely ambiguous. Member ->
            // Hierarchy and Member -> Level both cost 1, so the two overloads tie and
            // ValidatorImpl.getDef fails with "More than one function matches signature". This
            // DSL has no case for "resolves, but ambiguously" (isRejected() needs zero best
            // matches, resolvesTo(...) needs exactly one), so there is no case for it here.

            .autoEdgeCases()
            .edgeCaseMdx("hierarchy",           "[Gender].Members")
            .edgeCaseMdx("level",                "[Gender].[F].Level.Members")
            .edgeCaseMdx("dimension reference",  "[Gender].Dimension.Members")

            // [Gender] has no calculated members in the Sales cube: same members as .AllMembers.
            .value("SetToStr([Gender].Members)",
                    "{[Gender].[Gender].[All Gender], [Gender].[Gender].[F], [Gender].[Gender].[M]}")
            .value("SetToStr([Gender].[F].Level.Members)",
                    "{[Gender].[Gender].[F], [Gender].[Gender].[M]}")
            .value("SetToStr([Gender].Dimension.Members)",
                    "{[Gender].[Gender].[All Gender], [Gender].[Gender].[F], [Gender].[Gender].[M]}")

            .dependsOn("[Gender].Members")
            .dependsOn("[Gender].[F].Level.Members")
            .dependsOn("[Gender].Dimension.Members")

            .resultStyle("[Gender].Members", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("[Gender].Members", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("[Gender].Members")
            .independentMutableList("[Gender].[F].Level.Members")
            .build();
}
