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
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.set.distinct.DistinctFunDef;

/** The contract of the MDX function {@code Distinct}. */
public final class DistinctContract {

    private DistinctContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Distinct")
            .signatures("<Set> Distinct(<Set>)")
            .returns(SET)
            .arity(1, 1)

            .resolvesTo(DistinctFunDef.class, SET)
            .resolvesWithCost(2, DistinctFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, DistinctFunDef.class, TUPLE)    // Tuple -> Set
            .resolvesWithCost(1, DistinctFunDef.class, LEVEL)    // Level -> Set
            .rejects(DIMENSION)   // Dimension converts to Member/Hierarchy/Level, never Set
            .rejects(HIERARCHY)   // Hierarchy converts to Member/Dimension/Tuple, never Set
            .rejects(NUMERIC)
            .rejects()             // arity 0
            .rejects(SET, SET)     // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty set",              "Distinct({})")
            .edgeCaseMdx("already distinct",        "Distinct([Gender].Members)")
            .edgeCaseMdx("set with duplicates",     "Distinct({[Gender].[F], [Gender].[F], [Gender].[M]})")
            .edgeCaseMdx("member operand (lenient)", "Distinct([Gender].[F])")

            .value("Count(Distinct({[Gender].[F], [Gender].[F], [Gender].[M]}))", "2")
            .value("SetToStr(Distinct({[Gender].[F], [Gender].[F], [Gender].[M]}))",
                   "{[Gender].[Gender].[F], [Gender].[Gender].[M]}")
            .value("Count(Distinct({}))", "0")

            .dependsOn("Distinct([Gender].Members)")

            .resultStyle("Distinct([Gender].Members)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("Distinct([Gender].Members)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("Distinct([Gender].Members)")

            .build();
}
