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
import org.eclipse.daanse.olap.function.def.set.stripcalculatedmembers.StripCalculatedMembersFunDef;

/**
 * The contract of the MDX function {@code StripCalculatedMembers(<Set>)}. {@code
 * StripCalculatedMembersFunDef} is resolved by a plain {@code
 * ParametersCheckingFunctionDefinitionResolver} over a single {@code SET} parameter — {@code
 * resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, so
 * there is no hand-written resolver code that could diverge from the declared signature or
 * throw instead of returning empty.
 *
 * <p>{@code StripCalculatedMembersCalc} delegates straight to {@code
 * FunUtil.removeCalculatedMembers} — this test kit's {@code [Gender]} fixture has no
 * calculated members to strip, so the only value assertion grounded here is the identity
 * case: a set of ordinary members passes through unchanged.
 */
public final class StripCalculatedMembersContract {

    private StripCalculatedMembersContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("StripCalculatedMembers")
            .signatures("<Set> StripCalculatedMembers(<Set>)")
            .returns(SET)
            .arity(1, 1)

            .resolvesTo(StripCalculatedMembersFunDef.class, SET)
            .resolvesWithCost(2, StripCalculatedMembersFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, StripCalculatedMembersFunDef.class, TUPLE)    // Tuple -> Set
            .resolvesWithCost(1, StripCalculatedMembersFunDef.class, LEVEL)    // Level -> Set
            .rejects(DIMENSION)   // Dimension converts to Member/Hierarchy/Level, never Set
            .rejects(HIERARCHY)   // Hierarchy converts to Member/Dimension/Tuple, never Set
            .rejects(NUMERIC)
            .rejects()             // arity 0
            .rejects(SET, SET)     // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty set",              "StripCalculatedMembers({})")
            .edgeCaseMdx("no calculated members",  "StripCalculatedMembers([Gender].Members)")
            .edgeCaseMdx("member operand (lenient)", "StripCalculatedMembers([Gender].[F])")

            .value("Count(StripCalculatedMembers([Gender].Members))", "3")
            .value("Count(StripCalculatedMembers({}))", "0")

            .dependsOn("StripCalculatedMembers([Gender].Members)")

            .resultStyle("StripCalculatedMembers([Gender].Members)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("StripCalculatedMembers([Gender].Members)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("StripCalculatedMembers([Gender].Members)")

            .build();
}
