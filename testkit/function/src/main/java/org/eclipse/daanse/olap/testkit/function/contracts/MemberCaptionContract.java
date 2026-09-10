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
import org.eclipse.daanse.olap.function.def.caption.member.MemberCaptionFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code Member_Caption}: {@code <Member>.Member_Caption} —
 * a wholly separate atom from {@link CaptionContract}'s {@code "Caption"} (easy to confuse by
 * name, but a distinct {@code PlainPropertyOperationAtom}, own package, own resolver), and one
 * of only two categories {@code Caption} exposes for {@code MEMBER} — this one without the
 * three-way {@code Dimension}/{@code Hierarchy}/{@code Level} overload competition {@link
 * CaptionContract} has to reason about. {@code MemberCaptionFunDef} is resolved by a plain
 * {@code ParametersCheckingFunctionDefinitionResolver} over a single {@code MEMBER} parameter —
 * {@code resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, so
 * there is no hand-written resolver code to diverge from the declared signature or throw
 * instead of returning empty. Same {@code MEMBER}/{@code HIERARCHY}/{@code DIMENSION} cost
 * ladder as {@link LevelNumberContract}/{@link DataMemberContract}: {@code Hierarchy}/{@code
 * Dimension} convert to {@code Member} (cost 1/2), {@code Level} does not convert to {@code
 * Member} at all.
 *
 * <p>RESULT is waived: {@code MemberCaptionCalc.evaluateInternal} is a bare {@code
 * member.getCaption()} pass-through, and {@code OlapElement.getCaption()} is a bare interface
 * method with no default implementation in this repository (it lives in the Rolap engine) —
 * the same reasoning {@link CaptionContract} gives for its own {@code MEMBER} branch, which
 * shares this exact code shape.
 */
public final class MemberCaptionContract {

    private MemberCaptionContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Member_Caption")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.Member_Caption")
            .returns(STRING)
            .arity(1, 1)

            .resolvesTo(MemberCaptionFunDef.class, MEMBER)
            .resolvesWithCost(1, MemberCaptionFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, MemberCaptionFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level does not convert to Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("member reference",              "[Gender].[F].Member_Caption")
            .edgeCaseMdx("hierarchy/dimension reference",  "[Gender].Member_Caption")

            .waive(Promise.RESULT,
                    "OlapElement.getCaption() is a bare interface method with no default "
                            + "implementation in this repository (see the class Javadoc) — neither the "
                            + "exact caption string nor a fallback-to-name convention can be verified here.")

            .scalarDependsOn("[Gender].[F].Member_Caption")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
