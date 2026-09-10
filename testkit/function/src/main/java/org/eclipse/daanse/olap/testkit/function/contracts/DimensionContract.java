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
import org.eclipse.daanse.olap.function.def.dimension.dimension.DimensionOfDimensionFunDef;
import org.eclipse.daanse.olap.function.def.dimension.hierarchy.DimensionOfHierarchyFunDef;
import org.eclipse.daanse.olap.function.def.dimension.level.DimensionOfLevelFunDef;
import org.eclipse.daanse.olap.function.def.dimension.member.DimensionOfMemberFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code Dimension} — {@code <Dimension|Hierarchy|Level|
 * Member>.Dimension}. Four independent {@code ParametersCheckingFunctionDefinitionResolver}s
 * (one per category, each in its own {@code dimension.<category>} package) share one {@code
 * PlainPropertyOperationAtom("Dimension")}; every {@code resolve()} delegates to the generic
 * {@code FunctionMetaDataMatcher.match}, so none can throw or diverge from its own declared
 * signature. No ambiguity between the four: for every argument category the exact-match
 * overload is strictly cheaper than any other overload it could also reach via conversion —
 * <ul>
 *   <li>{@code MEMBER}: exact (0) beats Dimension/Hierarchy/Level (all cost 1).
 *   <li>{@code HIERARCHY}: exact (0) beats Dimension/Member (both cost 1); Hierarchy does not
 *       convert to Level at all.
 *   <li>{@code LEVEL}: exact (0) beats Hierarchy (1) and Dimension (2); Level does not convert
 *       to Member at all.
 *   <li>{@code DIMENSION}: exact (0) beats Hierarchy/Member (cost 2) and Level (cost 3).
 * </ul>
 * {@code SET}/{@code TUPLE} never reach {@code DIMENSION} ({@code convertFromSet} is always
 * false; {@code convertFromTuple} only reaches NUMERIC/SET/STRING/VALUE), nor do
 * {@code NUMERIC}/{@code STRING}.
 *
 * <p>Unlike {@link CaptionContract}/{@link DataMemberContract}, RESULT is not waived:
 * {@code Member.getDimension()}/{@code Hierarchy.getDimension()}/{@code Level.getDimension()}
 * are plain structural navigation (a stored/derived back-pointer), the same kind of accessor
 * already exercised via {@code .Parent}/{@code .Level} in {@link AncestorContract} and
 * {@link CousinContract} — not a business-logic method left unimplemented outside this
 * repository.
 */
public final class DimensionContract {

    private DimensionContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Dimension")
            .atom(PlainPropertyOperationAtom.class)
            .signatures(
                    "<Dimension>.Dimension",
                    "<Hierarchy>.Dimension",
                    "<Level>.Dimension",
                    "<Member>.Dimension")
            .returns(DIMENSION)
            .arity(1, 1)

            .resolvesTo(DimensionOfDimensionFunDef.class, DIMENSION)
            .resolvesTo(DimensionOfHierarchyFunDef.class, HIERARCHY)
            .resolvesTo(DimensionOfLevelFunDef.class, LEVEL)
            .resolvesTo(DimensionOfMemberFunDef.class, MEMBER)
            .rejects(SET)          // Set does not convert to anything
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects(TUPLE)         // Tuple converts to Set/Numeric/String/Value only
            .rejects()              // arity 0
            .rejects(MEMBER, MEMBER)   // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("dimension reference", "[Gender].Dimension.Dimension")
            .edgeCaseMdx("hierarchy reference",  "[Gender].Dimension")
            .edgeCaseMdx("level reference",      "[Gender].[F].Level.Dimension")
            .edgeCaseMdx("member reference",     "[Gender].[F].Dimension")

            .value("([Gender].[F].Dimension IS [Gender].Dimension)", "true")
            .value("([Gender].[F].Level.Dimension IS [Gender].Dimension)", "true")
            .value("([Gender].Dimension.Dimension IS [Gender].Dimension)", "true")

            .dependsOn("[Gender].[F].Dimension")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Dimension, not a set; the Set ResultStyle promise does not apply")

            .build();
}
