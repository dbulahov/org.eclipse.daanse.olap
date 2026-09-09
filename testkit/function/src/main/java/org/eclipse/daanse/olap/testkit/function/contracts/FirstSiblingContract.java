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
import org.eclipse.daanse.olap.function.def.member.firstsibling.FirstSiblingFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code FirstSibling}: {@code <Member>.FirstSibling} —
 * returns the first child of the member's parent. Same shape as {@link FirstChildContract}:
 * a single {@code ParametersCheckingFunctionDefinitionResolver} over one {@code MEMBER}
 * parameter, {@code resolve()} delegating entirely to the generic
 * {@code FunctionMetaDataMatcher.match}, no hand-written resolver code to diverge or throw.
 *
 * <p>{@code FirstSiblingCalc.firstSibling} has three branches: a root member (no parent, not
 * null) uses {@code getHierarchyRootMembers}; a non-root member uses
 * {@code getMemberChildren(parent)}; the hierarchy's own null member (no parent, {@code
 * isNull()}) short-circuits and returns itself, avoiding the empty-list case that the other two
 * branches structurally cannot hit — the member being queried is always itself one of the
 * children/root-members it looks up, so {@code children.get(0)} never indexes an empty list.
 */
public final class FirstSiblingContract {

    private FirstSiblingContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("FirstSibling")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.FirstSibling")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(FirstSiblingFunDef.class, MEMBER)
            .resolvesWithCost(1, FirstSiblingFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, FirstSiblingFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level converts to Hierarchy/Set/Dimension, never Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("leaf member with siblings", "[Gender].[M].FirstSibling")
            .edgeCaseMdx("root member (All)",          "[Gender].[F].Parent.FirstSibling")
            .edgeCaseMdx("null member",                "[Gender].[F].Parent.Parent.FirstSibling")

            // [Gender] is a one-level hierarchy under an All member: F is index 0, M is index 1
            // (same ordering HeadContract/ItemContract/FirstChildContract rely on).
            .value("([Gender].[M].FirstSibling IS [Gender].[F])", "true")
            .value("([Gender].[F].FirstSibling IS [Gender].[F])", "true")
            .value("([Gender].[F].Parent.FirstSibling IS [Gender].[F].Parent)", "true")
            .value("([Gender].[F].Parent.Parent.FirstSibling IS [Gender].[F].Parent.Parent)", "true")

            .dependsOn("[Gender].[M].FirstSibling", "[Gender].[Gender]")
            .dependsOn("[Gender].[F].Parent.FirstSibling", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")

            .build();
}
