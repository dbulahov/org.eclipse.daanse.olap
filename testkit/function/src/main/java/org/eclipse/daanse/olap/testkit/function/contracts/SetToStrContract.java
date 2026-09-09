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

import org.eclipse.daanse.olap.function.def.settostr.SetToStrFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code SetToStr(<Set>)}. {@code SetToStrFunDef} is
 * resolved by a plain {@code ParametersCheckingFunctionDefinitionResolver} over a single
 * {@code SET} parameter — {@code resolve()} delegates entirely to the generic {@code
 * FunctionMetaDataMatcher.match}, so there is no hand-written resolver code that could diverge
 * from the declared signature or throw instead of returning empty.
 *
 * <p>{@code SetToStrCalc.evaluateInternal} formats a member-arity-1 set as {@code
 * "{unique, name, ...}"} and a tuple-arity set as {@code "{(a, b), (c, d), ...}"} — the two
 * value assertions below cover both shapes, grounded in the same {@code [Gender].[F]} /
 * {@code [Measures].[Unit Sales]} facts {@link AsContract} and {@link CrossjoinContract} use.
 */
public final class SetToStrContract {

    private SetToStrContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("SetToStr")
            .signatures("<String> SetToStr(<Set>)")
            .returns(STRING)
            .arity(1, 1)

            .resolvesTo(SetToStrFunDef.class, SET)
            .resolvesWithCost(2, SetToStrFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, SetToStrFunDef.class, TUPLE)    // Tuple -> Set
            .resolvesWithCost(1, SetToStrFunDef.class, LEVEL)    // Level -> Set
            .rejects(DIMENSION)   // Dimension converts to Member/Hierarchy/Level, never Set
            .rejects(HIERARCHY)   // Hierarchy converts to Member/Dimension/Tuple, never Set
            .rejects(NUMERIC)
            .rejects()             // arity 0
            .rejects(SET, SET)     // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty set",              "SetToStr({})")
            .edgeCaseMdx("member set",              "SetToStr([Gender].Members)")
            .edgeCaseMdx("tuple set",                "SetToStr({([Gender].[F], [Measures].[Unit Sales])})")
            .edgeCaseMdx("member operand (lenient)", "SetToStr([Gender].[F])")

            .value("SetToStr({})", "{}")
            .value("SetToStr({[Gender].[F]})", "{[Gender].[F]}")
            .value("SetToStr({([Gender].[F], [Measures].[Unit Sales])})",
                   "{([Gender].[F], [Measures].[Unit Sales])}")

            .scalarDependsOn("SetToStr([Gender].Members)", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
