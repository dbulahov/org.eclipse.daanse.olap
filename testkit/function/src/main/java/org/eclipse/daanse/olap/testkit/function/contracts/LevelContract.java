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
import org.eclipse.daanse.olap.function.def.level.member.MemberLevelFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code Level}: {@code <Member>.Level}. A single {@code
 * ParametersCheckingFunctionDefinitionResolver} over one {@code MEMBER} parameter — {@code
 * resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, so there
 * is no hand-written resolver code to diverge from the declared signature or throw instead of
 * returning empty. {@code MemberLevelCalc.evaluateInternal} is a plain {@code
 * member.getLevel()} pass-through, the same kind of trivial structural navigation already
 * exercised (and asserted) for {@link DimensionContract}/{@link HierarchyContract} — so, unlike
 * {@link CaptionContract}/{@link DataMemberContract}, RESULT is not waived. Only one overload
 * exists (there is no {@code <Level_Number>} confusion here — {@code Level_Number} is a wholly
 * separate atom/property, {@code LevelNumberFunDef}, not in scope for this contract), so its
 * cost ladder for {@code HIERARCHY}/{@code DIMENSION} conversion mirrors {@link
 * FirstChildContract}/{@link LastChildContract}/{@link FirstSiblingContract}/{@link
 * LastSiblingContract} exactly (same {@code MEMBER} param, only the return category differs).
 * {@code LEVEL} itself is rejected as an argument: {@code convertFromLevel} has no {@code
 * MEMBER} case, so a bare Level cannot reach this property at all.
 */
public final class LevelContract {

    private LevelContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Level")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.Level")
            .returns(LEVEL)
            .arity(1, 1)

            .resolvesTo(MemberLevelFunDef.class, MEMBER)
            .resolvesWithCost(1, MemberLevelFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, MemberLevelFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level does not convert to Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("leaf member",                    "[Gender].[F].Level")
            .edgeCaseMdx("root member (All)",                "[Gender].[F].Parent.Level")
            .edgeCaseMdx("null member",                      "[Gender].[F].Parent.Parent.Level")
            .edgeCaseMdx("hierarchy/dimension reference",    "[Gender].Level")

            // [Gender] is a one-level hierarchy under an All member: F and M sit on the same
            // (only) real level.
            .value("([Gender].[F].Level IS [Gender].[M].Level)", "true")

            .dependsOn("[Gender].[F].Level")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Level, not a set; the Set ResultStyle promise does not apply")

            .build();
}
