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

import static org.eclipse.daanse.olap.api.DataType.INTEGER;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.crossjoin.CrossJoinFunDef;
import org.eclipse.daanse.olap.function.def.operators.multiply.MultiplyOperatorDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX operator {@code *}. Two unrelated functions share one atom
 * ({@code InfixOperationAtom("*")}): {@code CrossJoinFunDef} (via {@code StarCrossJoinResolver}),
 * and {@code MultiplyOperatorDef} (via {@code MultiplyResolver}). Which one a bare {@code *}
 * resolves to depends entirely on the calling context — {@code StarCrossJoinResolver} bows
 * out whenever {@code Validator.requiresExpression()} is true, i.e. in a scalar position.
 */
public final class StarContract {

    private StarContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("*")
            .atom(InfixOperationAtom.class)
            // CrossJoin and Multiply return different categories (SET vs NUMERIC); there is no
            // single expected return category to declare for the shared atom.
            .signatures("<Set> * <Set>", "<Numeric Expression> * <Numeric Expression>")
            .arity(2, Integer.MAX_VALUE)   // see the SIGNATURE waiver below: this is what is
                                            // *declared*, not what actually resolves

            // In the default (set) context, Member/Set combinations resolve to CrossJoin: it
            // matches at cost 0, cheaper than coercing both operands to Numeric for Multiply.
            .resolvesTo(CrossJoinFunDef.class, SET, SET)
            .resolvesTo(CrossJoinFunDef.class, SET, MEMBER)
            .resolvesTo(CrossJoinFunDef.class, MEMBER, SET)
            .resolvesTo(CrossJoinFunDef.class, MEMBER, MEMBER)
            .resolvesTo(MultiplyOperatorDef.class, NUMERIC, NUMERIC)
            .resolvesTo(MultiplyOperatorDef.class, INTEGER, NUMERIC)
            .resolvesWithCost(3, MultiplyOperatorDef.class, MEMBER, NUMERIC)   // Member -> Numeric
            // The crux of "*": the same Member/Member call that resolves to CrossJoin above
            // resolves to Multiply instead as soon as the position requires a scalar —
            // StarCrossJoinResolver.resolve() returns empty whenever requiresExpression() is
            // true, so only MultiplyResolver is left standing.
            .resolvesToInScalarContext(MultiplyOperatorDef.class, MEMBER, MEMBER)
            .resolvesToInScalarContext(MultiplyOperatorDef.class, NUMERIC, NUMERIC)
            .rejects(STRING, NUMERIC)
            .rejects()
            // Despite getRepresentativeFunctionMetaDatas() advertising a repeatable "Set1,
            // Set2, ..." overload, StarCrossJoinResolver.resolve() only ever matches the four
            // concrete two-argument FunDefs it was constructed with — a third argument is
            // rejected, not folded into a nested cross join.
            .rejects(SET, SET, SET)

            // edgeCaseMdx runs as a calculated member body, i.e. always in scalar context, so
            // only the Multiply side of "*" is reachable here; see the dependsOn/resultStyle
            // cases below for CrossJoin.
            .autoEdgeCases()
            .edgeCaseMdx("zero",              "0 * 5")
            .edgeCaseMdx("negative",          "-3 * 4")
            .edgeCaseMdx("null operand",      "NULL * 5")
            .edgeCaseMdx("largest double",    "1.7976931348623157E308 * 2")
            .edgeCaseMdx("member operand",    "[Measures].[Unit Sales] * 1")

            .value("2 * 3",   "6")
            .value("0 * 5",   "0")
            .value("-3 * 4",  "-12")
            .valueIsNull("NULL * 5")

            // Multiply, in scalar context.
            .scalarDependsOn("2 * 3")                                              // depends on nothing
            .scalarDependsOn("[Measures].[Unit Sales] * 2", "[Measures].[Measures]")
            // CrossJoin, in the set context a genuine axis expression provides.
            .dependsOn("[Gender].Members * [Measures].[Unit Sales]",
                       "[Gender].[Gender]", "[Measures].[Measures]")

            // Only reachable through CrossJoin: Multiply returns a scalar, so a set-context
            // ResultStyle promise does not apply to it (same reasoning as AbsContract's waiver).
            .resultStyle("[Gender].Members * [Measures].[Unit Sales]",
                         ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("[Gender].Members * [Measures].[Unit Sales]",
                         ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("[Gender].Members * [Measures].[Unit Sales]")

            .waive(Promise.SIGNATURE,
                    "two unrelated FunDefs share one atom with different return categories. The generic "
                            + "accepted-calls probe (declaredSignatureMatchesAcceptedCalls) reports dozens of "
                            + "resolver-only matches beyond that: Multiply's two <Numeric Expression> "
                            + "parameters accept every category TypeUtil.canConvert can coerce to NUMERIC "
                            + "(Dimension, Hierarchy, Level, Member, Tuple, Value, Null, Integer), none of "
                            + "which the declared \"<Numeric Expression> * <Numeric Expression>\" signature "
                            + "spells out — the same is true of every simple numeric-parameter function, "
                            + "it is just far more visible here because both parameters are numeric. "
                            + "StarCrossJoinResolver.getRepresentativeFunctionMetaDatas() additionally "
                            + "advertises a repeatable Set/Set overload that resolve() does not actually "
                            + "accept beyond two arguments — see rejects(SET, SET, SET) above.")
            .build();
}
