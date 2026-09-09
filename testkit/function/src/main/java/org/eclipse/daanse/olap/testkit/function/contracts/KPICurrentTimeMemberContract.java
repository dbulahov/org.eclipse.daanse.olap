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

import org.eclipse.daanse.olap.function.def.kpi.KPICurrentTimeMemberFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code KPICurrentTimeMember(<String>)} — one of a family of
 * six identically-shaped KPI accessor functions ({@link KPIGoalContract}, {@link
 * KPIStatusContract}, {@link KPITrendContract}, {@link KPIValueContract}, {@link
 * KPIWeightContract}) that differ only in which {@code KPI} field they read.
 *
 * <p>{@code KPICurrentTimeMemberFunDef} is resolved by a plain {@code
 * ParametersCheckingFunctionDefinitionResolver} over a single {@code STRING} parameter —
 * {@code resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match},
 * the exact matcher {@code declaredSignatureMatchesAcceptedCalls()} checks against, so
 * (unlike {@code Item} or {@code Extract}) there is no hand-written resolver code here that
 * could diverge from the declared signature or throw instead of returning empty.
 *
 * <p>{@code KPICurrentTimeMemberCalc.evaluateInternal} looks up {@code
 * evaluator.getCube().getKPIs()} by name and parses the matched KPI's {@code
 * getCurrentTimeMember()} string into a {@code Member} via {@code FunUtil.parseMember}; an
 * unmatched name throws a diagnosed {@code OlapRuntimeException} via {@code
 * FunUtil.newEvalException}. That lookup and throw only happen inside the compiled {@code
 * Calc} (Stage B), never during {@code resolve()} (Stage A) — this test kit module has no
 * cube and never reaches Stage B (no test overrides {@code connection()}), so the throw is
 * inert here regardless.
 *
 * <p>RESULT/DEPENDENCIES/RESULT_SHAPE are waived rather than backed by (inert, never-executed)
 * placeholder cases: unlike the shared {@code [Gender]}/{@code [Measures].[Unit Sales]}
 * fixture other contracts in this suite build on, no KPI is defined anywhere this module can
 * reference — the FoodMart-derived cube these functions would run against declares none, and
 * {@code KPICurrentTimeMemberCalcTest} (the existing unit test) only mocks a {@code KPI}
 * instance, so there is no real name/field pair to ground a genuine {@code .value(...)} or
 * {@code .dependsOn(...)} MDX assertion on.
 */
public final class KPICurrentTimeMemberContract {

    private static final String NO_KPI_FIXTURE =
            "evaluator.getCube().getKPIs() needs a real cube whose schema defines a named <Kpi>; "
                    + "this test kit has no such fixture (the shared [Gender]/FoodMart-derived cube "
                    + "declares none), and KPICurrentTimeMemberCalcTest only mocks a KPI instance, so "
                    + "there is no real name/field pair to ground a genuine .value(...)/.dependsOn(...) "
                    + "MDX assertion on. Moot in practice anyway: this module never reaches Stage B "
                    + "(no test overrides connection()), so these promises would be skipped either way.";

    private KPICurrentTimeMemberContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("KPICurrentTimeMember")
            .signatures("<Member> KPICurrentTimeMember(<String>)")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(KPICurrentTimeMemberFunDef.class, STRING)
            .resolvesWithCost(2, KPICurrentTimeMemberFunDef.class, VALUE)    // Value -> String
            .resolvesWithCost(4, KPICurrentTimeMemberFunDef.class, MEMBER)   // Member -> String
            .resolvesWithCost(4, KPICurrentTimeMemberFunDef.class, TUPLE)    // Tuple -> String
            .rejects(NUMERIC)          // Numeric converts to Value/Logical/Integer, never String
            .rejects(SET)               // Set converts to nothing (convertFromSet always false)
            .rejects()                  // arity 0
            .rejects(STRING, STRING)    // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty string",        "KPICurrentTimeMember(\"\")")
            .edgeCaseMdx("whitespace only",      "KPICurrentTimeMember(\"   \")")
            .edgeCaseMdx("special characters",   "KPICurrentTimeMember(\"[a]b&c\")")
            .edgeCaseMdx("Kpi NULL",             "KPICurrentTimeMember(NULL)")
            // Documented, currently-inert: throws a diagnosed OlapRuntimeException from
            // KPICurrentTimeMemberCalc.evaluateInternal (Stage B), never reached here.
            .edgeCaseMdx("unknown Kpi name (documented gap)", "KPICurrentTimeMember(\"NonExistentKpi\")")

            .waive(Promise.RESULT, NO_KPI_FIXTURE)
            .waive(Promise.DEPENDENCIES, NO_KPI_FIXTURE)
            .waive(Promise.RESULT_SHAPE, NO_KPI_FIXTURE)

            .build();
}
