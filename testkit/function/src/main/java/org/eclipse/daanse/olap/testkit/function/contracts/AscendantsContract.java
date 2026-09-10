/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
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

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.set.ascendants.AscendantsFunDef;

/** The contract of the MDX function {@code Ascendants}. */
public final class AscendantsContract {

    private AscendantsContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Ascendants")
            .signatures("<Set> Ascendants(<Member>)")
            .returns(SET)
            .arity(1, 1)

            .resolvesTo(AscendantsFunDef.class, MEMBER)
            .resolvesWithCost(1, AscendantsFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, AscendantsFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level converts to Hierarchy/Set/Dimension, never Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("leaf member",           "Ascendants([Gender].[F])")
            .edgeCaseMdx("top-of-hierarchy member", "Ascendants([Gender].[F].Parent)")
            .edgeCaseMdx("null member",            "Ascendants([Gender].[F].Parent.Parent)")

            // The member itself always comes first.
            .value("Ascendants([Gender].[F]).Item(0).Name", "F")
            // .Parent.Parent from a member of a shallow (one real level, +/- an All member)
            // hierarchy always bottoms out at the null member; AscendantsCalc returns {} for it
            // regardless of whether [Gender] happens to have an All level.
            .value("Count(Ascendants([Gender].[F].Parent.Parent))", "0")

            .dependsOn("Ascendants([Gender].[F])")
            .dependsOn("Ascendants([Gender].[F].Parent)")

            .resultStyle("Ascendants([Gender].[F])", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("Ascendants([Gender].[F])", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("Ascendants([Gender].[F])")
            .build();
}
