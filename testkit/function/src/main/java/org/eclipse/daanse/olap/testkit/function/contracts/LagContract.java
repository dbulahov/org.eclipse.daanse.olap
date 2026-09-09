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
 * The contract of the MDX method {@code <Member>.Lag(<Numeric Expression>)} — a member further
 * back along the specified member's level ({@code Lag(m, n)} is the same navigation as
 * {@code Lead(m, -n)}). {@code LagResolver}/{@code LeadResolver} each wrap a single {@code
 * LeadLagFunDef} instance under {@code AbstractFunctionDefinitionMultiResolver} with their own
 * atom/{@code FunctionMetaData} ("Lag" vs "Lead") — a one-element multi-resolver, so there is no
 * declaration-order risk (see {@link ClosingPeriodContract}'s finding on that bug class);
 * {@code LeadLagFunDef.compileCall} then reads back {@code
 * call.getFunDef().getFunctionMetaData().operationAtom().name()} to pick the sign. {@code
 * resolve()} itself delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, so
 * it cannot throw or diverge from its own declared signature.
 *
 * <p>Fixed: {@code LeadLagCalc.evaluateInternal} unboxed the {@code Integer} amount calc
 * directly into {@code n == Integer.MIN_VALUE}/{@code -n} before ever checking it for
 * {@code null} — a bare {@code Lag(<Member>, NULL)} would NPE instead of being treated as "no
 * movement". Same bug class as {@code AncestorNumericCalc}/{@code AncestorsCalc} earlier in
 * this series; fixed the same way (null amount treated as {@code 0}), preserving the existing
 * {@code Integer.MIN_VALUE} overflow guard for non-null amounts.
 *
 * <p>{@code CatalogReader.getLeadMember(Member, int)} is a bare interface method with no
 * implementation in this repository (only pass-through delegates), but — unlike {@code
 * getCaption()}/{@code getDataMember()} — it is core navigation machinery already exercised
 * (and asserted) via {@link LastPeriodsContract}'s {@code getLeadMember}-backed value
 * assertions, so RESULT is not waived here either; only the out-of-range boundary (whether it
 * clamps or returns the null-member sentinel) is left unasserted, same treatment
 * {@link ItemContract} gives an out-of-range index.
 */
public final class LagContract {

    private LagContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Lag")
            .atom(MethodOperationAtom.class)
            .signatures("<Member> <Member>.Lag(<Numeric Expression>)")
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
            .edgeCaseMdx("zero lag",                 "[Gender].[F].Lag(0)")
            .edgeCaseMdx("positive lag within range", "[Gender].[M].Lag(1)")
            .edgeCaseMdx("negative lag (= lead)",     "[Gender].[F].Lag(-1)")
            .edgeCaseMdx("lag out of range",          "[Gender].[F].Lag(1)")
            // Fixed: used to NPE unboxing a NULL amount before the null check existed.
            .edgeCaseMdx("NULL amount (fixed)",       "[Gender].[F].Lag(NULL)")
            // The explicit Integer.MIN_VALUE overflow guard in LeadLagCalc.
            .edgeCaseMdx("Integer.MIN_VALUE amount",  "[Gender].[F].Lag(-2147483648)")

            // [Gender] is a one-level hierarchy under an All member: F is index 0, M is index 1
            // (same ordering FirstChildContract/FirstSiblingContract rely on).
            .value("([Gender].[F].Lag(0) IS [Gender].[F])", "true")
            .value("([Gender].[M].Lag(1) IS [Gender].[F])", "true")
            .value("([Gender].[F].Lag(-1) IS [Gender].[M])", "true")

            .dependsOn("[Gender].[F].Lag(1)", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")

            .build();
}
