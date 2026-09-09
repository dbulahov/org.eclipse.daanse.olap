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

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.set.siblings.SiblingsFunDef;

/**
 * The contract of the MDX property {@code <Member>.Siblings}. {@code SiblingsFunDef} is
 * resolved by a plain {@code ParametersCheckingFunctionDefinitionResolver} over a single
 * {@code MEMBER} parameter — {@code resolve()} delegates entirely to the generic {@code
 * FunctionMetaDataMatcher.match}, so there is no hand-written resolver code that could diverge
 * from the declared signature or throw instead of returning empty.
 *
 * <p>{@code SiblingsCalc} includes the member itself among its siblings (per the declared
 * description) — a root member (no parent) gets the hierarchy's root members instead of its
 * (nonexistent) parent's children, and the null member (e.g. {@code
 * [Gender].[F].Parent.Parent} on this flat, {@code hasAll=true} fixture) has no siblings at
 * all, not even itself — {@code SiblingsCalc.memberSiblings} special-cases {@code
 * member.isNull()} to return an empty list before touching {@code getParentMember()}.
 */
public final class SiblingsContract {

    private SiblingsContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Siblings")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.Siblings")
            .returns(SET)
            .arity(1, 1)

            .resolvesTo(SiblingsFunDef.class, MEMBER)
            .resolvesWithCost(1, SiblingsFunDef.class, HIERARCHY)   // Hierarchy -> Member (implicit CurrentMember)
            .resolvesWithCost(2, SiblingsFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(SET)          // Set does not convert to Member
            .rejects(LEVEL)         // Level does not convert to Member
            .rejects(NUMERIC)
            .rejects(MEMBER, MEMBER)   // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("ordinary member",  "[Gender].[F].Siblings")
            .edgeCaseMdx("root member (All)", "[Gender].[F].Parent.Siblings")
            .edgeCaseMdx("null member",       "[Gender].[F].Parent.Parent.Siblings")

            // [Gender] is flat: F and M are both children of the All member, so Siblings of
            // either (which includes the member itself) is the full two-member set.
            .value("Count([Gender].[F].Siblings)", "2")
            // hasAll=true: the All member is the hierarchy's only root, so its own Siblings
            // (a root member's siblings are the hierarchy's root members) is itself alone.
            .value("Count([Gender].[F].Parent.Siblings)", "1")
            // The null member has no siblings at all, not even itself.
            .value("Count([Gender].[F].Parent.Parent.Siblings)", "0")

            .dependsOn("[Gender].[F].Siblings", "[Gender].[Gender]")

            .resultStyle("[Gender].[F].Siblings", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("[Gender].[F].Siblings", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("[Gender].[F].Siblings")

            .build();
}
