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

import org.eclipse.daanse.olap.function.def.tupletostr.TupleToStrFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code TupleToStr(<Tuple>)}. {@code TupleToStrFunDef} is
 * resolved by a plain {@code ParametersCheckingFunctionDefinitionResolver} over a single {@code
 * TUPLE} parameter — {@code resolve()} delegates entirely to the generic {@code
 * FunctionMetaDataMatcher.match}, so there is no hand-written resolver code that could diverge
 * from the declared signature or throw instead of returning empty.
 *
 * <p>{@code TupleToStrFunDef.compileCall} branches at compile time on {@code
 * TypeUtil.couldBeMember(call.getArg(0).getType())}: a {@code Member}/{@code Hierarchy}/{@code
 * Dimension}-typed argument (converted to {@code TUPLE} only via the single-member-tuple
 * shorthand, cost 1/1/2 — {@code Level} does not convert to {@code Tuple} at all) compiles to
 * {@code TupleToStrMemberCalc}, formatting as the bare {@code member.getUniqueName()} with no
 * parentheses; a genuine multi-member tuple expression compiles to {@code TupleToStrCalc},
 * formatting as {@code "(a, b, ...)"} via {@code FunUtil.appendTuple} — the same tuple-portion
 * format {@link SetToStrContract} uses inside its own {@code "{...}"} wrapping. The two value
 * assertions below cover both shapes.
 */
public final class TupleToStrContract {

    private TupleToStrContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("TupleToStr")
            .signatures("<String> TupleToStr(<Tuple>)")
            .returns(STRING)
            .arity(1, 1)

            .resolvesTo(TupleToStrFunDef.class, TUPLE)
            .resolvesWithCost(1, TupleToStrFunDef.class, MEMBER)      // Member -> Tuple
            .resolvesWithCost(1, TupleToStrFunDef.class, HIERARCHY)   // Hierarchy -> Tuple
            .resolvesWithCost(2, TupleToStrFunDef.class, DIMENSION)   // Dimension -> Tuple
            .rejects(LEVEL)     // Level does not convert to Tuple
            .rejects(SET)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                  // arity 0
            .rejects(TUPLE, TUPLE)      // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("single member (member shortcut)",     "TupleToStr([Gender].[F])")
            .edgeCaseMdx("hierarchy reference (member shortcut)", "TupleToStr([Gender])")
            .edgeCaseMdx("genuine tuple",
                    "TupleToStr(([Gender].[F], [Measures].[Unit Sales]))")
            .edgeCaseMdx("null member",                         "TupleToStr([Gender].[F].Parent.Parent)")

            .value("TupleToStr([Gender].[F])", "[Gender].[Gender].[F]")
            .value("TupleToStr(([Gender].[F], [Measures].[Unit Sales]))",
                   "([Gender].[Gender].[F], [Measures].[Unit Sales])")
            .valueIsNull("TupleToStr([Gender].[F].Parent.Parent)")

            .scalarDependsOn("TupleToStr([Gender].[F])")
            .scalarDependsOn("TupleToStr(([Gender].[F], [Measures].[Unit Sales]))")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
