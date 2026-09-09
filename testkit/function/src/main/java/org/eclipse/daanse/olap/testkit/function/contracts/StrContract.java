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

import static org.eclipse.daanse.olap.api.DataType.INTEGER;
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;
import static org.eclipse.daanse.olap.api.DataType.VALUE;

import org.eclipse.daanse.olap.function.def.vba.str.StrFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the VBA-compatible MDX function {@code Str(<Value>)} — a Variant-String
 * representation of a number: a leading space (reserving the sign position) for a
 * non-negative number, none for a negative one. {@code StrFunDef} is resolved by a plain
 * {@code ParametersCheckingFunctionDefinitionResolver} over a single {@code VALUE} parameter
 * — {@code resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match},
 * so there is no hand-written resolver code that could diverge from the declared signature or
 * throw instead of returning empty.
 *
 * <p>{@code StrCalc.evaluateInternal} calls {@code number.toString()} on whatever {@code
 * compiler.compile(call.getArg(0))} (a plain, untyped compile — not {@code compileDouble})
 * produces at runtime, and throws a diagnosed {@code InvalidArgumentException} (a proper
 * {@code OlapRuntimeException} subclass) if that value is not a {@code Number} — including
 * {@code null}, since the argument's declared category being {@code VALUE} does not guarantee
 * a runtime {@code Number}. Both cases are Stage B (evaluation) only, never {@code resolve()}
 * (Stage A), so inert in this test kit regardless of the (cube-free) stub category probed.
 *
 * <p>{@code NumericLiteralImpl.accept(ExpressionCompiler)} — the path every bare numeric MDX
 * literal takes — always compiles to a {@code ConstantDoubleCalc}, i.e. a boxed {@code
 * Double}, regardless of whether the literal was written with a decimal point. {@code
 * Double.toString()} always includes one, so {@code Str(5)} is {@code " 5.0"}, not the
 * classic VBA {@code " 5"} a whole-number {@code Integer} argument would have produced — a
 * real, traced behavioral quirk of this generic-compile path, not a guess.
 */
public final class StrContract {

    private StrContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Str")
            .signatures("<String> Str(<Value>)")
            .returns(STRING)
            .arity(1, 1)

            .resolvesTo(StrFunDef.class, VALUE)
            .resolvesTo(StrFunDef.class, NUMERIC)   // Numeric -> Value, free
            .resolvesTo(StrFunDef.class, STRING)    // String -> Value, free
            .resolvesTo(StrFunDef.class, INTEGER)   // Integer -> Value, free
            .resolvesWithCost(4, StrFunDef.class, MEMBER)   // Member -> Value
            .resolvesWithCost(4, StrFunDef.class, TUPLE)    // Tuple -> Value
            .rejects(SET)          // Set converts to nothing (convertFromSet always false)
            .rejects(LEVEL)         // Level does not convert to Value
            .rejects()              // arity 0
            .rejects(NUMERIC, NUMERIC)   // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("positive number", "Str(5)")
            .edgeCaseMdx("negative number", "Str(-5)")
            .edgeCaseMdx("zero",            "Str(0)")
            .edgeCaseMdx("fractional",      "Str(0.5)")
            // Documented, currently-inert (see the class Javadoc): a String literal converts
            // to Value at resolution time, but StrCalc's runtime "instanceof Number" check
            // rejects it with a diagnosed InvalidArgumentException.
            .edgeCaseMdx("string operand (documented gap)", "Str(\"abc\")")
            .edgeCaseMdx("value NULL (documented gap)",      "Str(NULL)")

            // See the class Javadoc: every bare numeric literal compiles to a boxed Double,
            // and Double.toString() always includes a decimal point.
            .value("Str(5)",   " 5.0")
            .value("Str(-5)",  "-5.0")
            .value("Str(0)",   " 0.0")
            .value("Str(0.5)", " 0.5")

            .scalarDependsOn("Str(5)")                                        // depends on nothing
            .scalarDependsOn("Str([Measures].[Unit Sales])", "[Measures].[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
