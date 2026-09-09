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
import org.eclipse.daanse.olap.function.def.hierarchy.level.LevelHierarchyFunDef;
import org.eclipse.daanse.olap.function.def.hierarchy.member.MemberHierarchyFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code Hierarchy}: {@code <Level|Member>.Hierarchy}. Two
 * independent {@code ParametersCheckingFunctionDefinitionResolver}s (one per category) share
 * one {@code PlainPropertyOperationAtom("Hierarchy")}; both {@code resolve()}s delegate to the
 * generic {@code FunctionMetaDataMatcher.match}, so neither can throw or diverge from its own
 * declared signature. Unlike {@link CaptionContract}/{@link DimensionContract} there is no
 * {@code <Dimension>.Hierarchy} overload at all — {@code TypeUtil.convertFromDimension} still
 * lets a Dimension argument reach {@code HIERARCHY} implicitly (through the {@code Member}
 * overload, see below), it is just never registered as its own explicit FunDef.
 *
 * <p>No ambiguity between the two registered overloads:
 * <ul>
 *   <li>{@code LEVEL}: exact (0) — Level does not convert to Member at all
 *       ({@code convertFromLevel} has no {@code MEMBER} case), so only this overload matches.
 *   <li>{@code MEMBER}: exact (0) beats Level (cost 1, since Member converts to Level).
 *   <li>{@code HIERARCHY}: converts to Member only (cost 1) — Hierarchy does not convert to
 *       Level at all ({@code convertFromHierarchy} has no {@code LEVEL} case) — so this
 *       resolves unambiguously to {@code MemberHierarchyFunDef}.
 *   <li>{@code DIMENSION}: converts to Member (cost 2) more cheaply than to Level (cost 3), so
 *       this too resolves to {@code MemberHierarchyFunDef}.
 * </ul>
 * {@code SET}/{@code TUPLE} never reach either param category ({@code convertFromSet} is
 * always false; {@code convertFromTuple} only reaches NUMERIC/SET/STRING/VALUE), nor do
 * {@code NUMERIC}/{@code STRING}.
 *
 * <p>Unlike {@link CaptionContract}/{@link DataMemberContract}, RESULT is not waived: {@code
 * Level.getHierarchy()}/{@code Member.getHierarchy()} are plain structural navigation, the same
 * kind of trivial accessor already exercised (and asserted) for {@link DimensionContract}.
 */
public final class HierarchyContract {

    private HierarchyContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Hierarchy")
            .atom(PlainPropertyOperationAtom.class)
            .signatures(
                    "<Level>.Hierarchy",
                    "<Member>.Hierarchy")
            .returns(HIERARCHY)
            .arity(1, 1)

            .resolvesTo(LevelHierarchyFunDef.class, LEVEL)
            .resolvesTo(MemberHierarchyFunDef.class, MEMBER)
            .resolvesWithCost(1, MemberHierarchyFunDef.class, HIERARCHY)   // Hierarchy -> Member (no Level path)
            .resolvesWithCost(2, MemberHierarchyFunDef.class, DIMENSION)   // Dimension -> Member, cheaper than -> Level (3)
            .rejects(SET)           // Set does not convert to anything
            .rejects(TUPLE)          // Tuple converts to Set/Numeric/String/Value only
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()               // arity 0
            .rejects(MEMBER, MEMBER)    // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("level reference",               "[Gender].[F].Level.Hierarchy")
            .edgeCaseMdx("member reference",               "[Gender].[F].Hierarchy")
            .edgeCaseMdx("hierarchy/dimension reference",  "[Gender].Hierarchy")

            .value("([Gender].[F].Hierarchy IS [Gender])", "true")
            .value("([Gender].[F].Level.Hierarchy IS [Gender])", "true")

            .dependsOn("[Gender].[F].Hierarchy", "[Gender].[Gender]")
            .dependsOn("[Gender].[F].Level.Hierarchy", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Hierarchy, not a set; the Set ResultStyle promise does not apply")

            .build();
}
