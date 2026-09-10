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
import org.eclipse.daanse.olap.function.def.member.nextmember.NextMemberFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code NextMember}: {@code <Member>.NextMember} — the member
 * that follows a specified member in the same level. A single {@code
 * ParametersCheckingFunctionDefinitionResolver} over one {@code MEMBER} parameter — {@code
 * resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, so there is
 * no hand-written resolver code to diverge from the declared signature or throw instead of
 * returning empty. Same {@code MEMBER}/{@code HIERARCHY}/{@code DIMENSION} cost ladder as {@link
 * FirstChildContract}.
 *
 * <p>{@code NextMemberCalc.evaluateInternal} is {@code
 * evaluator.getCatalogReader().getLeadMember(member, 1)} — exactly {@link LeadContract}'s {@code
 * <Member>.Lead(1)} (both share the same {@code CatalogReader.getLeadMember} navigation this test
 * kit already exercises for real, not waived), just without the numeric offset argument. Past the
 * last member of a level it returns the hierarchy's null-member sentinel, the same fallback
 * {@link FirstChildContract} documents for {@code <Member>.FirstChild}.
 */
public final class NextMemberContract {

    private NextMemberContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("NextMember")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.NextMember")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(NextMemberFunDef.class, MEMBER)
            .resolvesWithCost(1, NextMemberFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, NextMemberFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level does not convert to Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("member with a next sibling", "[Gender].[F].NextMember")
            .edgeCaseMdx("last member in level",       "[Gender].[M].NextMember")
            .edgeCaseMdx("root member (All)",          "[Gender].[F].Parent.NextMember")
            .edgeCaseMdx("null member",                "[Gender].[F].Parent.Parent.NextMember")

            // [Gender] is a one-level hierarchy under an All member: F is index 0, M is index 1
            // (same ordering LeadContract/FirstSiblingContract rely on). [Gender].[F].Parent.Parent
            // is the established null-member reference (FirstChildContract relies on the same
            // trick); the All member is the only member of its level, so it too has no next.
            .value("([Gender].[F].NextMember IS [Gender].[M])", "true")
            .value("([Gender].[M].NextMember IS [Gender].[F].Parent.Parent)", "true")
            .value("([Gender].[F].Parent.NextMember IS [Gender].[F].Parent.Parent)", "true")

            .dependsOn("[Gender].[F].NextMember")
            .dependsOn("[Gender].[M].NextMember")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")

            .build();
}
