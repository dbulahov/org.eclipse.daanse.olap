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

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.function.def.uniquename.member.Unique_NameFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code Unique_Name}: {@code <Member>.Unique_Name} — a wholly
 * separate atom from {@link UniqueNameContract}'s {@code "UniqueName"} (easy to confuse by name,
 * but a distinct {@code PlainPropertyOperationAtom}, own resolver), and one of only two
 * categories {@code UniqueName} exposes for {@code MEMBER} — this one without the three-way
 * {@code Dimension}/{@code Hierarchy}/{@code Level} overload competition {@link
 * UniqueNameContract} has to reason about. The exact same shape as {@link
 * MemberCaptionContract}'s relationship to {@code Caption}. {@code Unique_NameFunDef} is
 * resolved by a plain {@code ParametersCheckingFunctionDefinitionResolver} over a single {@code
 * MEMBER} parameter — {@code resolve()} delegates entirely to the generic {@code
 * FunctionMetaDataMatcher.match}, so there is no hand-written resolver code to diverge from the
 * declared signature or throw instead of returning empty. Same {@code MEMBER}/{@code
 * HIERARCHY}/{@code DIMENSION} cost ladder as {@link LevelNumberContract}/{@link
 * DataMemberContract}.
 *
 * <p>{@code Unique_NameCalc.evaluateInternal} is a bare {@code member.getUniqueName()}
 * pass-through — the exact same accessor {@link UniqueNameContract} already asserts a real value
 * for, so RESULT is asserted here too, not waived.
 */
public final class MemberUniqueNameContract {

    private MemberUniqueNameContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Unique_Name")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.Unique_Name")
            .returns(STRING)
            .arity(1, 1)

            .resolvesTo(Unique_NameFunDef.class, MEMBER)
            .resolvesWithCost(1, Unique_NameFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, Unique_NameFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level does not convert to Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("member reference",              "[Gender].[F].Unique_Name")
            .edgeCaseMdx("hierarchy/dimension reference",  "[Gender].Unique_Name")

            .value("[Gender].[F].Unique_Name", "[Gender].[Gender].[F]")
            .value("[Gender].Unique_Name", "[Gender].[Gender].[All Gender]")

            .scalarDependsOn("[Gender].[F].Unique_Name")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
