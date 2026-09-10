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
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code Caption} — {@code <Dimension|Hierarchy|Level|
 * Member>.Caption}. Four independent {@code ParametersCheckingFunctionDefinitionResolver}s
 * (one per category, each in its own {@code caption.<category>} package, each wrapping a
 * same-simple-named-but-distinct {@code CaptionFunDef}) share one {@code
 * PlainPropertyOperationAtom("Caption")}. Each resolver's {@code resolve()} delegates to the
 * generic {@code FunctionMetaDataMatcher.match}, so none can throw or diverge from its own
 * declared signature; the interesting question is whether the four overlap ambiguously once
 * {@code TypeUtil.canConvert} is factored in. They do not: for every argument category, the
 * exact-match overload is strictly cheaper than every other overload it could also reach via
 * conversion —
 * <ul>
 *   <li>{@code MEMBER}: exact (cost 0) beats Hierarchy/Dimension/Level (all cost 1, since
 *       Member converts to each of those).
 *   <li>{@code HIERARCHY}: exact (0) beats Member/Dimension (both cost 1); Hierarchy does not
 *       convert to Level at all.
 *   <li>{@code DIMENSION}: exact (0) beats Hierarchy/Member (cost 2) and Level (cost 3).
 *   <li>{@code LEVEL}: exact (0) beats Hierarchy (cost 1) and Dimension (cost 2); Level does
 *       not convert to Member at all.
 * </ul>
 * so {@code declaredSignatureMatchesAcceptedCalls} has a single, unambiguous winner for every
 * probed category — no overload competition to reconcile.
 *
 * <p>RESULT is waived: {@code OlapElement.getCaption()} is a bare interface method with no
 * default implementation in this repository (it lives in the Rolap engine), so neither the
 * exact caption string nor its fallback-to-name behavior (if any) can be verified here —
 * unlike, say, {@link CurrentMemberContract}'s default-member fact, there is no code path in
 * this repository to trace a caption value from.
 */
public final class CaptionContract {

    private CaptionContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Caption")
            .atom(PlainPropertyOperationAtom.class)
            .signatures(
                    "<Dimension>.Caption",
                    "<Hierarchy>.Caption",
                    "<Level>.Caption",
                    "<Member>.Caption")
            .returns(STRING)
            .arity(1, 1)

            .resolvesTo(org.eclipse.daanse.olap.function.def.caption.dimension.CaptionFunDef.class, DIMENSION)
            .resolvesTo(org.eclipse.daanse.olap.function.def.caption.hierarchy.CaptionFunDef.class, HIERARCHY)
            .resolvesTo(org.eclipse.daanse.olap.function.def.caption.level.CaptionFunDef.class, LEVEL)
            .resolvesTo(org.eclipse.daanse.olap.function.def.caption.member.CaptionFunDef.class, MEMBER)
            .rejects(SET)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects(TUPLE)      // Tuple converts to Set/Numeric/String/Value only
            .rejects()            // arity 0
            .rejects(MEMBER, MEMBER)   // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("hierarchy/dimension reference", "[Gender].Caption")
            .edgeCaseMdx("member reference",               "[Gender].[F].Caption")
            .edgeCaseMdx("level reference",                "[Gender].[F].Level.Caption")

            .waive(Promise.RESULT,
                    "OlapElement.getCaption() is a bare interface method with no default "
                            + "implementation in this repository (see the class Javadoc) — neither the "
                            + "exact caption string nor a fallback-to-name convention can be verified here.")

            .scalarDependsOn("[Gender].[F].Caption")
            .scalarDependsOn("[Gender].[F].Level.Caption")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
