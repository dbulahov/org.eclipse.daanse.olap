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

import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;
import static org.eclipse.daanse.olap.api.DataType.VALUE;

import org.eclipse.daanse.olap.function.def.string.UCaseFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code UCase(<String>)}. {@code UCaseFunDef} is resolved
 * by a plain {@code ParametersCheckingFunctionDefinitionResolver} over a single {@code
 * STRING} parameter — {@code resolve()} delegates entirely to the generic {@code
 * FunctionMetaDataMatcher.match}, so there is no hand-written resolver code that could diverge
 * from the declared signature or throw instead of returning empty (the same shape as {@link
 * LenContract}).
 *
 * <p>{@code UCaseFunDef.compileCall} explicitly rejects a literal {@code NULL} argument with
 * a diagnosed {@code OlapRuntimeException} ("No method with the signature UCase(NULL) matches
 * known functions") — deliberate, and only reachable at Stage B (compile time), never during
 * this test kit's {@code resolve()}-only probing. That check only catches a
 * <em>literally</em>-typed {@code NULL} argument, though: {@code UCaseCalc.evaluateInternal}
 * used to call {@code value.toUpperCase(locale)} unconditionally, so any other expression that
 * merely evaluates to a runtime {@code null} (a property lookup, an {@code IIf} branch, …)
 * threw a bare {@code NullPointerException} instead of the same diagnosed rejection. Fixed:
 * {@code evaluateInternal} now returns {@code null} for a {@code null} String, propagating it
 * like every other scalar function here does, rather than crashing.
 */
public final class UCaseContract {

    private UCaseContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("UCase")
            .signatures("<String> UCase(<String>)")
            .returns(STRING)
            .arity(1, 1)

            .resolvesTo(UCaseFunDef.class, STRING)
            .resolvesWithCost(2, UCaseFunDef.class, VALUE)    // Value -> String
            .resolvesWithCost(4, UCaseFunDef.class, MEMBER)   // Member -> String
            .resolvesWithCost(4, UCaseFunDef.class, TUPLE)    // Tuple -> String
            .rejects(NUMERIC)          // Numeric converts to Value/Logical/Integer, never String
            .rejects(SET)               // Set converts to nothing (convertFromSet always false)
            .rejects()                  // arity 0
            .rejects(STRING, STRING)    // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty string",      "UCase(\"\")")
            .edgeCaseMdx("whitespace only",    "UCase(\"   \")")
            .edgeCaseMdx("special characters", "UCase(\"[a]b&c\")")
            // Documented: UCaseFunDef.compileCall rejects a *literal* NULL outright (see the
            // class Javadoc) — inert here since compileCall only runs at Stage B.
            .edgeCaseMdx("String NULL literal (documented, rejected at compile)", "UCase(NULL)")

            .value("UCase(\"abc\")",    "ABC")
            .value("UCase(\"\")",       "")
            .value("UCase(\"   \")",    "   ")
            .value("UCase(\"[a]b&c\")", "[A]B&C")

            .scalarDependsOn("UCase(\"abc\")")                                        // depends on nothing
            .scalarDoesNotDependOn("UCase([Measures].[Unit Sales])", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
