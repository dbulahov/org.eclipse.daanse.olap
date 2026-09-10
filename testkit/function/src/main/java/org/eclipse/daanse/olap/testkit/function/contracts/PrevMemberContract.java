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
import org.eclipse.daanse.olap.function.def.member.prevmember.PrevMemberFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code PrevMember}: {@code <Member>.PrevMember} — the mirror
 * of {@link NextMemberContract}'s {@code <Member>.NextMember}, both wrapping the same {@code
 * CatalogReader.getLeadMember(member, n)} navigation ({@code n=-1} here instead of {@code n=1}).
 * A single {@code ParametersCheckingFunctionDefinitionResolver} over one {@code MEMBER}
 * parameter — {@code resolve()} delegates entirely to the generic {@code
 * FunctionMetaDataMatcher.match}, so there is no hand-written resolver code to diverge from the
 * declared signature or throw instead of returning empty. Same {@code MEMBER}/{@code
 * HIERARCHY}/{@code DIMENSION} cost ladder as {@link FirstChildContract}/{@link
 * NextMemberContract}.
 */
public final class PrevMemberContract {

    private PrevMemberContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("PrevMember")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.PrevMember")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(PrevMemberFunDef.class, MEMBER)
            .resolvesWithCost(1, PrevMemberFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, PrevMemberFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level does not convert to Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("member with a previous sibling", "[Gender].[M].PrevMember")
            .edgeCaseMdx("first member in level",          "[Gender].[F].PrevMember")
            .edgeCaseMdx("root member (All)",              "[Gender].[F].Parent.PrevMember")
            .edgeCaseMdx("null member",                    "[Gender].[F].Parent.Parent.PrevMember")

            // [Gender] is a one-level hierarchy under an All member: F is index 0, M is index 1
            // (same ordering NextMemberContract/LeadContract/FirstSiblingContract rely on).
            // [Gender].[F].Parent.Parent is the established null-member reference (FirstChildContract/
            // ParentContract rely on the same trick); the All member is the only member of its
            // level, so it too has no previous.
            .value("([Gender].[M].PrevMember IS [Gender].[F])", "true")
            .value("([Gender].[F].PrevMember IS [Gender].[F].Parent.Parent)", "true")
            .value("([Gender].[F].Parent.PrevMember IS [Gender].[F].Parent.Parent)", "true")

            .dependsOn("[Gender].[M].PrevMember")
            .dependsOn("[Gender].[F].PrevMember")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")

            .build();
}
