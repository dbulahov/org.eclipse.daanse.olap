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
import static org.eclipse.daanse.olap.api.DataType.INTEGER;
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.function.def.level.numeric.LevelNumberFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code Level_Number}: {@code <Member>.Level_Number} —
 * returns the zero-based depth of a member's level. A wholly separate atom from {@link
 * LevelContract}'s {@code "Level"} (easy to confuse by name, but a distinct
 * {@code PlainPropertyOperationAtom}, own package, own resolver). A single {@code
 * ParametersCheckingFunctionDefinitionResolver} over one {@code MEMBER} parameter — {@code
 * resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, so there
 * is no hand-written resolver code to diverge from the declared signature or throw instead of
 * returning empty. {@code LevelNumberCalc.evaluateInternal} is a plain
 * {@code member.getLevel().getDepth()} pass-through — the same kind of trivial structural
 * navigation already exercised (and asserted) for {@link LevelContract}/{@link
 * DimensionContract}/{@link HierarchyContract} — so RESULT is not waived. Same
 * {@code MEMBER}/{@code HIERARCHY}/{@code DIMENSION} cost ladder as {@link LevelContract} (only
 * the return category differs: {@code INTEGER} here, {@code LEVEL} there).
 */
public final class LevelNumberContract {

    private LevelNumberContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Level_Number")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.Level_Number")
            .returns(INTEGER)
            .arity(1, 1)

            .resolvesTo(LevelNumberFunDef.class, MEMBER)
            .resolvesWithCost(1, LevelNumberFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, LevelNumberFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level does not convert to Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("leaf member",                 "[Gender].[F].Level_Number")
            .edgeCaseMdx("root member (All)",             "[Gender].[F].Parent.Level_Number")
            .edgeCaseMdx("null member",                   "[Gender].[F].Parent.Parent.Level_Number")
            .edgeCaseMdx("hierarchy/dimension reference", "[Gender].Level_Number")

            // [Gender] is a one-level hierarchy under an All member: the All member sits at
            // depth 0, and its (only) real level of children — F, M — sits at depth 1.
            .value("[Gender].[F].Level_Number", "1")
            .value("[Gender].[M].Level_Number", "1")
            .value("[Gender].[F].Parent.Level_Number", "0")

            .scalarDependsOn("[Gender].[F].Level_Number")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
