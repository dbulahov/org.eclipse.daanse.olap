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
import org.eclipse.daanse.olap.function.def.member.lastchild.LastChildFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code LastChild}: {@code <Member>.LastChild}. Same shape as
 * {@link FirstChildContract} (same atom family, same MEMBER/HIERARCHY/DIMENSION cost ladder):
 * a single {@code ParametersCheckingFunctionDefinitionResolver} over one {@code MEMBER}
 * parameter, {@code resolve()} delegating entirely to the generic
 * {@code FunctionMetaDataMatcher.match}, no hand-written resolver code to diverge or throw.
 * {@code LastChildCalc.lastChild} falls back to {@code member.getHierarchy().getNullMember()}
 * when the member has no children, otherwise returns {@code children.get(children.size() - 1)}
 * — the same null-fallback {@code FirstChildCalc} uses, just indexing the other end.
 */
public final class LastChildContract {

    private LastChildContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("LastChild")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.LastChild")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(LastChildFunDef.class, MEMBER)
            .resolvesWithCost(1, LastChildFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, LastChildFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level converts to Hierarchy/Set/Dimension, never Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("leaf member",         "[Gender].[F].LastChild")
            .edgeCaseMdx("member with children", "[Gender].[F].Parent.LastChild")
            .edgeCaseMdx("null member",          "[Gender].[F].Parent.Parent.LastChild")

            // [Gender] is a one-level hierarchy under an All member: a leaf has no children
            // (falls back to the null-member sentinel), and the All member's last child is the
            // established [Gender].[M] (index 1 — same ordering FirstChildContract relies on).
            .value("([Gender].[F].LastChild IS [Gender].[F].Parent.Parent)", "true")
            .value("([Gender].[F].Parent.LastChild IS [Gender].[M])", "true")

            .dependsOn("[Gender].[F].LastChild")
            .dependsOn("[Gender].[F].Parent.LastChild")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")

            .build();
}
