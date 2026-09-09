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

import org.eclipse.daanse.olap.function.def.kpi.KPIWeightFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code KPIWeight(<String>)} — a member of the KPI accessor
 * family; see {@link KPICurrentTimeMemberContract} for the shared resolver/Calc shape and the
 * reasoning behind this contract's RESULT/DEPENDENCIES/RESULT_SHAPE waivers. {@code
 * KPIWeightCalc.evaluateInternal} parses the matched KPI's {@code getWeight()} string.
 */
public final class KPIWeightContract {

    private static final String NO_KPI_FIXTURE =
            "evaluator.getCube().getKPIs() needs a real cube whose schema defines a named <Kpi>; "
                    + "this test kit has no such fixture (the shared [Gender]/FoodMart-derived cube "
                    + "declares none), and KPIWeightCalcTest only mocks a KPI instance, so there is no "
                    + "real name/field pair to ground a genuine .value(...)/.dependsOn(...) MDX "
                    + "assertion on. Moot in practice anyway: this module never reaches Stage B (no "
                    + "test overrides connection()), so these promises would be skipped either way.";

    private KPIWeightContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("KPIWeight")
            .signatures("<Member> KPIWeight(<String>)")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(KPIWeightFunDef.class, STRING)
            .resolvesWithCost(2, KPIWeightFunDef.class, VALUE)    // Value -> String
            .resolvesWithCost(4, KPIWeightFunDef.class, MEMBER)   // Member -> String
            .resolvesWithCost(4, KPIWeightFunDef.class, TUPLE)    // Tuple -> String
            .rejects(NUMERIC)          // Numeric converts to Value/Logical/Integer, never String
            .rejects(SET)               // Set converts to nothing (convertFromSet always false)
            .rejects()                  // arity 0
            .rejects(STRING, STRING)    // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty string",        "KPIWeight(\"\")")
            .edgeCaseMdx("whitespace only",      "KPIWeight(\"   \")")
            .edgeCaseMdx("special characters",   "KPIWeight(\"[a]b&c\")")
            .edgeCaseMdx("Kpi NULL",             "KPIWeight(NULL)")
            // Documented, currently-inert: throws a diagnosed OlapRuntimeException from
            // KPIWeightCalc.evaluateInternal (Stage B), never reached here.
            .edgeCaseMdx("unknown Kpi name (documented gap)", "KPIWeight(\"NonExistentKpi\")")

            .waive(Promise.RESULT, NO_KPI_FIXTURE)
            .waive(Promise.DEPENDENCIES, NO_KPI_FIXTURE)
            .waive(Promise.RESULT_SHAPE, NO_KPI_FIXTURE)

            .build();
}
