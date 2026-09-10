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
import org.eclipse.daanse.olap.function.def.member.parentcalc.ParentFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code Parent}: {@code <Member>.Parent}. A single {@code
 * ParametersCheckingFunctionDefinitionResolver} over one {@code MEMBER} parameter — {@code
 * resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, so there
 * is no hand-written resolver code to diverge from the declared signature or throw instead of
 * returning empty. Same {@code MEMBER}/{@code HIERARCHY}/{@code DIMENSION} cost ladder as {@link
 * FirstChildContract}/{@link NextMemberContract}.
 *
 * <p>{@code ParentFunDef.compileCall} returns an anonymous {@code ParentCalc} subclass that
 * overrides {@code evaluateInternal} to duplicate exactly what the base {@code ParentCalc}
 * class already does — the base class is never actually instantiated as itself, only
 * subclassed inline. Harmless (both do the identical, correct thing) and out of scope for this
 * contract to clean up — the same shape of redundancy {@link DataMemberContract} found in its
 * own sibling {@code DataMemberCalc}.
 *
 * <p>{@code ParentCalc.memberParent} falls back to {@code member.getHierarchy().getNullMember()}
 * when {@code CatalogReader.getMemberParent} returns {@code null} (the All/root member has no
 * parent) — the same null-member sentinel {@link FirstChildContract}/{@link
 * DataMemberContract}/{@link NextMemberContract} already rely on and helped establish.
 */
public final class ParentContract {

    private ParentContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Parent")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.Parent")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(ParentFunDef.class, MEMBER)
            .resolvesWithCost(1, ParentFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, ParentFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level does not convert to Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("leaf member",          "[Gender].[F].Parent")
            .edgeCaseMdx("root member (All)",     "[Gender].[F].Parent.Parent")
            .edgeCaseMdx("null member",           "[Gender].[F].Parent.Parent.Parent")

            // [Gender] is a one-level hierarchy under an All member: a leaf's parent is the All
            // member — the same member DefaultMemberContract independently establishes as
            // [Gender].DefaultMember (hasAll=true, no explicit default configured). The All
            // member itself has no parent, falling back to the null-member sentinel — the same
            // instance regardless of which leaf's grandparent reaches it.
            .value("([Gender].[F].Parent IS [Gender].DefaultMember)", "true")
            .value("([Gender].[F].Parent.Parent IS [Gender].[M].Parent.Parent)", "true")

            .dependsOn("[Gender].[F].Parent")
            .dependsOn("[Gender].[F].Parent.Parent")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")

            .build();
}
