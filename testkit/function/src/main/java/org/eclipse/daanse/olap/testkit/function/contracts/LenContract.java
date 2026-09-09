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

import org.eclipse.daanse.olap.function.def.string.LenFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Len(<String>)}. {@code LenFunDef} is resolved by a
 * plain {@code ParametersCheckingFunctionDefinitionResolver} over a single {@code STRING}
 * parameter — {@code resolve()} delegates entirely to the generic {@code
 * FunctionMetaDataMatcher.match}, so there is no hand-written resolver code that could diverge
 * from the declared signature or throw instead of returning empty (the same shape as the KPI
 * accessor family, e.g. {@link KPIGoalContract}).
 *
 * <p>{@code LenCalc.evaluateInternal} is explicitly null-safe: a {@code NULL} String argument
 * returns {@code 0} rather than {@code null} or throwing — {@code Len} never returns MDX NULL.
 */
public final class LenContract {

    private LenContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Len")
            .signatures("<Numeric Expression> Len(<String>)")
            .returns(NUMERIC)
            .arity(1, 1)

            .resolvesTo(LenFunDef.class, STRING)
            .resolvesWithCost(2, LenFunDef.class, VALUE)    // Value -> String
            .resolvesWithCost(4, LenFunDef.class, MEMBER)   // Member -> String
            .resolvesWithCost(4, LenFunDef.class, TUPLE)    // Tuple -> String
            .rejects(NUMERIC)          // Numeric converts to Value/Logical/Integer, never String
            .rejects(SET)               // Set converts to nothing (convertFromSet always false)
            .rejects()                  // arity 0
            .rejects(STRING, STRING)    // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty string",      "Len(\"\")")
            .edgeCaseMdx("whitespace only",    "Len(\"   \")")
            .edgeCaseMdx("special characters", "Len(\"[a]b&c\")")
            .edgeCaseMdx("String NULL",        "Len(NULL)")

            .value("Len(\"abc\")",    "3")
            .value("Len(\"\")",       "0")
            .value("Len(\"   \")",    "3")
            .value("Len(\"[a]b&c\")", "6")
            .value("Len(NULL)",       "0")

            .scalarDependsOn("Len(\"abc\")")                                        // depends on nothing
            .scalarDependsOn("Len([Measures].[Unit Sales])", "[Measures].[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
