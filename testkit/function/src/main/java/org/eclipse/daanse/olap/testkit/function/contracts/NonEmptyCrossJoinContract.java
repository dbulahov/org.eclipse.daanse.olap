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
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.nonemptycrossjoin.NonEmptyCrossJoinFunDef;

/**
 * The contract of the MDX function {@code NonEmptyCrossJoin(<Set1>, <Set2>)} — a two-argument
 * cross join, unlike {@code Crossjoin} (which accepts any number via a repeatable second
 * parameter, see {@link CrossjoinContract}) — {@code NonEmptyCrossJoinResolver} declares
 * exactly two required {@code Set} parameters, so a third argument is rejected outright.
 *
 * <p>{@code NonEmptyCrossJoinFunDef} extends {@code CrossJoinFunDef} (inheriting its {@code
 * getResultType}) but overrides {@code compileCall} to read {@code call.getArg(0)}/{@code (1)}
 * unconditionally — safe here only because the declared signature's fixed arity of 2 is
 * enforced generically by {@code FunctionMetaDataMatcher.match} before {@code compileCall} is
 * ever reached (Stage B, not exercised in this test kit anyway).
 */
public final class NonEmptyCrossJoinContract {

    private NonEmptyCrossJoinContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("NonEmptyCrossJoin")
            .signatures("<Set> NonEmptyCrossJoin(<Set>, <Set>)")
            .returns(SET)
            .arity(2, 2)

            .resolvesTo(NonEmptyCrossJoinFunDef.class, SET, SET)
            .resolvesWithCost(2, NonEmptyCrossJoinFunDef.class, MEMBER, SET)   // Member -> Set
            .resolvesWithCost(2, NonEmptyCrossJoinFunDef.class, SET, TUPLE)    // Tuple -> Set
            .resolvesWithCost(1, NonEmptyCrossJoinFunDef.class, LEVEL, SET)    // Level -> Set
            .rejects(DIMENSION, SET)
            .rejects(NUMERIC, SET)
            .rejects(SET)                // arity 1
            .rejects()                   // arity 0
            .rejects(SET, SET, SET)      // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("first set empty",     "NonEmptyCrossJoin({}, [Gender].Members)")
            .edgeCaseMdx("second set empty",    "NonEmptyCrossJoin([Gender].Members, {})")
            .edgeCaseMdx("both sets empty",     "NonEmptyCrossJoin({}, {})")
            .edgeCaseMdx("member operand",      "NonEmptyCrossJoin([Gender].[F], {[Measures].[Unit Sales]})")

            .value("Count(NonEmptyCrossJoin([Gender].Members, {[Measures].[Unit Sales]}))", "2")
            .value("Count(NonEmptyCrossJoin({}, [Gender].Members))", "0")

            .dependsOn("NonEmptyCrossJoin([Gender].Members, {[Measures].[Unit Sales]})",
                       "[Gender].[Gender]", "[Measures].[Measures]")

            .resultStyle("NonEmptyCrossJoin([Gender].Members, {[Measures].[Unit Sales]})",
                         ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("NonEmptyCrossJoin([Gender].Members, {[Measures].[Unit Sales]})",
                         ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("NonEmptyCrossJoin([Gender].Members, {[Measures].[Unit Sales]})")

            .build();
}
