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
 * The contract of the MDX property {@code Name} — {@code <Dimension|Hierarchy|Level|
 * Member>.Name}. The exact same shape as {@link CaptionContract} — four independent {@code
 * ParametersCheckingFunctionDefinitionResolver}s (one per category, each in its own {@code
 * name.<category>} package, each wrapping a same-simple-named-but-distinct {@code NameFunDef})
 * share one {@code PlainPropertyOperationAtom("Name")}. Each resolver's {@code resolve()}
 * delegates to the generic {@code FunctionMetaDataMatcher.match}, so none can throw or diverge
 * from its own declared signature; the same cost ladder as {@code Caption} resolves the same
 * four-way overlap without ambiguity — see that contract's Javadoc for the full per-category
 * reasoning.
 *
 * <p>RESULT is waived: {@code OlapElement.getName()} is a bare interface method with no default
 * implementation in this repository (it lives in the Rolap engine), the same reasoning {@link
 * CaptionContract} gives for the sibling {@code OlapElement.getCaption()} — neither the exact
 * name string nor any fallback behavior can be verified here.
 */
public final class NameContract {

    private NameContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Name")
            .atom(PlainPropertyOperationAtom.class)
            .signatures(
                    "<Dimension>.Name",
                    "<Hierarchy>.Name",
                    "<Level>.Name",
                    "<Member>.Name")
            .returns(STRING)
            .arity(1, 1)

            .resolvesTo(org.eclipse.daanse.olap.function.def.name.dimension.NameFunDef.class, DIMENSION)
            .resolvesTo(org.eclipse.daanse.olap.function.def.name.hierarchy.NameFunDef.class, HIERARCHY)
            .resolvesTo(org.eclipse.daanse.olap.function.def.name.level.NameFunDef.class, LEVEL)
            .resolvesTo(org.eclipse.daanse.olap.function.def.name.member.NameFunDef.class, MEMBER)
            .rejects(SET)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects(TUPLE)      // Tuple converts to Set/Numeric/String/Value only
            .rejects()            // arity 0
            .rejects(MEMBER, MEMBER)   // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("hierarchy/dimension reference", "[Gender].Name")
            .edgeCaseMdx("member reference",               "[Gender].[F].Name")
            .edgeCaseMdx("level reference",                "[Gender].[F].Level.Name")

            .waive(Promise.RESULT,
                    "OlapElement.getName() is a bare interface method with no default "
                            + "implementation in this repository (see the class Javadoc) — the exact "
                            + "name string cannot be verified here.")

            .scalarDependsOn("[Gender].[F].Name")
            .scalarDependsOn("[Gender].[F].Level.Name")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
