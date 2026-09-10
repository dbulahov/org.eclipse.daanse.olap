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
import org.eclipse.daanse.olap.function.def.numeric.ordinal.OrdinalFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code Ordinal}: {@code <Level>.Ordinal} — the zero-based
 * depth of a level. A wholly separate atom from {@link LevelNumberContract}'s {@code
 * "Level_Number"} (easy to confuse by meaning — both ultimately report a depth — but a distinct
 * {@code PlainPropertyOperationAtom}, own package, own resolver, and a different calling
 * category: {@code Level_Number} takes a {@code Member}, {@code Ordinal} takes the {@code Level}
 * itself). A single {@code ParametersCheckingFunctionDefinitionResolver} over one {@code LEVEL}
 * parameter — {@code resolve()} delegates entirely to the generic {@code
 * FunctionMetaDataMatcher.match}, so there is no hand-written resolver code to diverge from the
 * declared signature or throw instead of returning empty. {@code OrdinalCalc.evaluateInternal}
 * is a plain {@code level.getDepth()} pass-through — the same trivial structural navigation
 * {@link LevelNumberContract} already exercises (and asserts) for {@code Member.getLevel()
 * .getDepth()} — so RESULT is not waived.
 *
 * <p>Same {@code MEMBER}/{@code DIMENSION} cost ladder as {@link PeriodsToDateContract}'s own
 * {@code LEVEL}-parameter conversions ({@code Member -> Level} cost 1, {@code Dimension -> Level}
 * cost 3); {@code Hierarchy} does not convert to {@code Level} at all ({@link CaptionContract}
 * establishes the same fact for its own four-way overlap).
 */
public final class OrdinalContract {

    private OrdinalContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Ordinal")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Level>.Ordinal")
            .returns(NUMERIC)
            .arity(1, 1)

            .resolvesTo(OrdinalFunDef.class, LEVEL)
            .resolvesWithCost(1, OrdinalFunDef.class, MEMBER)      // Member -> Level
            .resolvesWithCost(3, OrdinalFunDef.class, DIMENSION)   // Dimension -> Level
            .rejects(HIERARCHY)   // Hierarchy does not convert to Level at all
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                   // arity 0
            .rejects(LEVEL, LEVEL)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("leaf level",                    "[Gender].[F].Level.Ordinal")
            .edgeCaseMdx("root level (All)",               "[Gender].[F].Parent.Level.Ordinal")
            .edgeCaseMdx("member reference",               "[Gender].[F].Ordinal")
            .edgeCaseMdx("dimension reference",            "[Gender].Dimension.Ordinal")

            // [Gender] is a one-level hierarchy under an All member: the All member's level
            // sits at depth 0, and its (only) real level of children — F, M — sits at depth 1
            // (the same numbering LevelNumberContract establishes for [Gender].[F].Level_Number).
            .value("[Gender].[F].Level.Ordinal", "1")
            .value("[Gender].[F].Parent.Level.Ordinal", "0")

            .scalarDependsOn("[Gender].[F].Level.Ordinal")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
