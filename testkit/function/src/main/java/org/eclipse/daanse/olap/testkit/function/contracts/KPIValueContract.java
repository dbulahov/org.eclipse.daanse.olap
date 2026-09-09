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

import org.eclipse.daanse.olap.function.def.kpi.KPIValueFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code KPIValue(<String>)} — a member of the KPI accessor
 * family; see {@link KPICurrentTimeMemberContract} for the shared resolver/Calc shape and the
 * reasoning behind this contract's RESULT/DEPENDENCIES/RESULT_SHAPE waivers. {@code
 * KPIValueCalc.evaluateInternal} parses the matched KPI's {@code getValue()} string.
 */
public final class KPIValueContract {

    private static final String NO_KPI_FIXTURE =
            "evaluator.getCube().getKPIs() needs a real cube whose schema defines a named <Kpi>; "
                    + "this test kit has no such fixture (the shared [Gender]/FoodMart-derived cube "
                    + "declares none), and KPIValueCalcTest only mocks a KPI instance, so there is no "
                    + "real name/field pair to ground a genuine .value(...)/.dependsOn(...) MDX "
                    + "assertion on. Moot in practice anyway: this module never reaches Stage B (no "
                    + "test overrides connection()), so these promises would be skipped either way.";

    private KPIValueContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("KPIValue")
            .signatures("<Member> KPIValue(<String>)")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(KPIValueFunDef.class, STRING)
            .resolvesWithCost(2, KPIValueFunDef.class, VALUE)    // Value -> String
            .resolvesWithCost(4, KPIValueFunDef.class, MEMBER)   // Member -> String
            .resolvesWithCost(4, KPIValueFunDef.class, TUPLE)    // Tuple -> String
            .rejects(NUMERIC)          // Numeric converts to Value/Logical/Integer, never String
            .rejects(SET)               // Set converts to nothing (convertFromSet always false)
            .rejects()                  // arity 0
            .rejects(STRING, STRING)    // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty string",        "KPIValue(\"\")")
            .edgeCaseMdx("whitespace only",      "KPIValue(\"   \")")
            .edgeCaseMdx("special characters",   "KPIValue(\"[a]b&c\")")
            .edgeCaseMdx("Kpi NULL",             "KPIValue(NULL)")
            // Documented, currently-inert: throws a diagnosed OlapRuntimeException from
            // KPIValueCalc.evaluateInternal (Stage B), never reached here.
            .edgeCaseMdx("unknown Kpi name (documented gap)", "KPIValue(\"NonExistentKpi\")")

            .waive(Promise.RESULT, NO_KPI_FIXTURE)
            .waive(Promise.DEPENDENCIES, NO_KPI_FIXTURE)
            .waive(Promise.RESULT_SHAPE, NO_KPI_FIXTURE)

            .build();
}
