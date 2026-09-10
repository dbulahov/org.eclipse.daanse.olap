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
 * The contract of the MDX property {@code UniqueName} — {@code <Dimension|Hierarchy|Level|
 * Member>.UniqueName}. The exact same shape as {@link CaptionContract}/{@link NameContract} —
 * four independent {@code ParametersCheckingFunctionDefinitionResolver}s (one per category, each
 * in its own {@code uniquename.<category>} package, each wrapping a
 * same-simple-named-but-distinct {@code UniqueNameFunDef}) share one {@code
 * PlainPropertyOperationAtom("UniqueName")}. Each resolver's {@code resolve()} delegates to the
 * generic {@code FunctionMetaDataMatcher.match}, so none can throw or diverge from its own
 * declared signature; the same cost ladder as {@code Caption}/{@code Name} resolves the same
 * four-way overlap without ambiguity — see {@link CaptionContract}'s Javadoc for the full
 * per-category reasoning.
 *
 * <p>Unlike {@code OlapElement.getCaption()}/{@code getName()} — which {@link CaptionContract}/
 * {@link NameContract} decline to assert a value for, since neither has a default implementation
 * in this repository — {@code getUniqueName()} is already exercised for real elsewhere in this
 * suite: {@link TupleToStrContract}/{@link SetToStrContract}'s own value assertions both rely on
 * {@code member.getUniqueName()} producing exactly {@code "[Gender].[F]"}. RESULT is asserted
 * here, not waived, building on that already-verified fact.
 */
public final class UniqueNameContract {

    private UniqueNameContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("UniqueName")
            .atom(PlainPropertyOperationAtom.class)
            .signatures(
                    "<Dimension>.UniqueName",
                    "<Hierarchy>.UniqueName",
                    "<Level>.UniqueName",
                    "<Member>.UniqueName")
            .returns(STRING)
            .arity(1, 1)

            .resolvesTo(org.eclipse.daanse.olap.function.def.uniquename.dimension.UniqueNameFunDef.class, DIMENSION)
            .resolvesTo(org.eclipse.daanse.olap.function.def.uniquename.hierarchy.UniqueNameFunDef.class, HIERARCHY)
            .resolvesTo(org.eclipse.daanse.olap.function.def.uniquename.level.UniqueNameFunDef.class, LEVEL)
            .resolvesTo(org.eclipse.daanse.olap.function.def.uniquename.member.UniqueNameFunDef.class, MEMBER)
            .rejects(SET)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects(TUPLE)      // Tuple converts to Set/Numeric/String/Value only
            .rejects()            // arity 0
            .rejects(MEMBER, MEMBER)   // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("dimension reference",  "[Gender].Dimension.UniqueName")
            .edgeCaseMdx("hierarchy reference",   "[Gender].UniqueName")
            .edgeCaseMdx("member reference",      "[Gender].[F].UniqueName")
            .edgeCaseMdx("level reference",       "[Gender].[F].Level.UniqueName")

            .value("[Gender].[F].UniqueName", "[Gender].[Gender].[F]")
            .value("[Gender].UniqueName", "[Gender]")
            .value("[Gender].Dimension.UniqueName", "[Gender]")
            .value("[Gender].[F].Level.UniqueName", "[Gender].[Gender].[Gender]")

            .scalarDependsOn("[Gender].[F].UniqueName")
            .scalarDependsOn("[Gender].[F].Level.UniqueName")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
