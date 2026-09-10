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

import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.olap.function.def.leadlag.LeadLagFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX method {@code <Member>.Lead(<Numeric Expression>)} — a member further
 * along the specified member's level ({@code Lead(m, n)} is the same navigation as
 * {@code Lag(m, -n)}; see {@link LagContract} for the full resolver/registration analysis, which
 * applies identically here — {@code LeadResolver} wraps the very same {@code LeadLagFunDef}
 * class, differentiated only by its own {@code MethodOperationAtom}/{@code FunctionMetaData}
 * ("Lead" instead of "Lag"), which {@code LeadLagFunDef.compileCall} reads back at compile time
 * to decide the calc's sign).
 *
 * <p>The {@code LeadLagCalc} null-amount unboxing fix documented in {@link LagContract} was
 * made directly in the shared calc class, so it already covers {@code Lead} too — nothing
 * further to fix here.
 */
public final class LeadContract {

    private LeadContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Lead")
            .atom(MethodOperationAtom.class)
            .signatures("<Member> <Member>.Lead(<Numeric Expression>)")
            .returns(MEMBER)
            .arity(2, 2)

            .resolvesTo(LeadLagFunDef.class, MEMBER, NUMERIC)
            .resolvesWithCost(1, LeadLagFunDef.class, HIERARCHY, NUMERIC)   // Hierarchy -> Member
            .resolvesWithCost(2, LeadLagFunDef.class, DIMENSION, NUMERIC)   // Dimension -> Member
            .rejects(SET, NUMERIC)      // Set does not convert to Member
            .rejects(LEVEL, NUMERIC)     // Level does not convert to Member
            .rejects(MEMBER, STRING)     // String does not convert to Numeric
            .rejects()                   // arity 0
            .rejects(MEMBER)              // arity 1
            .rejects(MEMBER, NUMERIC, NUMERIC)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("zero lead",                 "[Gender].[F].Lead(0)")
            .edgeCaseMdx("positive lead within range", "[Gender].[F].Lead(1)")
            .edgeCaseMdx("negative lead (= lag)",      "[Gender].[M].Lead(-1)")
            .edgeCaseMdx("lead out of range",          "[Gender].[M].Lead(1)")
            // Fixed (see LagContract): used to NPE unboxing a NULL amount before the null
            // check existed in the shared LeadLagCalc.
            .edgeCaseMdx("NULL amount (fixed)",        "[Gender].[F].Lead(NULL)")
            .edgeCaseMdx("Integer.MIN_VALUE amount",   "[Gender].[F].Lead(-2147483648)")

            // [Gender] is a one-level hierarchy under an All member: F is index 0, M is index 1
            // (same ordering LagContract/FirstSiblingContract rely on).
            .value("([Gender].[F].Lead(0) IS [Gender].[F])", "true")
            .value("([Gender].[F].Lead(1) IS [Gender].[M])", "true")
            .value("([Gender].[M].Lead(-1) IS [Gender].[F])", "true")

            .dependsOn("[Gender].[F].Lead(1)")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")

            .build();
}
