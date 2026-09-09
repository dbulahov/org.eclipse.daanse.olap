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
import static org.eclipse.daanse.olap.api.DataType.INTEGER;
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;

import org.eclipse.daanse.olap.function.def.ancestor.AncestorLevelFunDef;
import org.eclipse.daanse.olap.function.def.ancestor.AncestorNumericFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Ancestor(<Member>, <Level>|<Numeric Expression>)}.
 * {@code AncestorResolver} is an {@code AbstractFunctionDefinitionMultiResolver} over two
 * declared overloads (by-Level and by-distance) — {@code resolve()} delegates to the generic
 * {@code FunctionMetaDataMatcher.match}, so there is no hand-written resolver code that could
 * diverge from the declared signature or throw instead of returning empty.
 *
 * <p>{@code AncestorNumericCalc.evaluateInternal} used to pass the compiled {@code Integer}
 * distance straight into {@code FunUtil.ancestor(Evaluator, Member, int, Level)} — a
 * primitive-{@code int} parameter — so {@code Ancestor(member, NULL)} unboxed a {@code null}
 * into a {@code NullPointerException}. Fixed to treat a {@code NULL} distance the same way
 * {@code FunUtil.ancestor} itself treats a negative one: "no valid ancestor", returning {@code
 * member.getHierarchy().getNullMember()} directly.
 *
 * <p>Both {@code AncestorLevelFunDef.compileCall} and {@code AncestorNumericFunDef.compileCall}
 * construct an {@code OlapRuntimeException} for a type mismatch on the second argument but
 * never {@code throw} it — dead code, and harmless: by the time {@code compileCall} runs for
 * either overload, {@code FunctionMetaDataMatcher.match} has already guaranteed that argument's
 * category, so the check can never actually fail.
 *
 * <p>Both {@code AncestorLevelFunDef} and {@code AncestorNumericFunDef} were package-private
 * (unlike most other {@code FunDef} classes this suite references) — made public so this
 * contract can name them in {@code resolvesTo(...)}, the same visibility fix {@link
 * PeriodsToDateContract} needed for {@code PeriodsToDateFunDef}.
 */
public final class AncestorContract {

    private AncestorContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Ancestor")
            .signatures(
                    "<Member> Ancestor(<Member>, <Level>)",
                    "<Member> Ancestor(<Member>, <Numeric Expression>)")
            .returns(MEMBER)
            .arity(2, 2)

            .resolvesTo(AncestorLevelFunDef.class, MEMBER, LEVEL)
            .resolvesTo(AncestorNumericFunDef.class, MEMBER, NUMERIC)
            .resolvesTo(AncestorNumericFunDef.class, MEMBER, INTEGER)   // Integer -> Numeric, free
            .resolvesWithCost(1, AncestorLevelFunDef.class, HIERARCHY, LEVEL)    // Hierarchy -> Member
            .resolvesWithCost(2, AncestorLevelFunDef.class, DIMENSION, LEVEL)    // Dimension -> Member
            .rejects(SET, LEVEL)       // Set does not convert to Member
            .rejects(MEMBER, SET)       // Set converts to neither Level nor Numeric
            .rejects(MEMBER, STRING)    // String does not convert to Level or Numeric
            .rejects()                  // arity 0
            .rejects(MEMBER)             // arity 1
            .rejects(MEMBER, LEVEL, LEVEL)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("level form",       "Ancestor([Gender].[F], [Gender].[F].Parent.Level)")
            .edgeCaseMdx("distance 1",       "Ancestor([Gender].[F], 1)")
            .edgeCaseMdx("distance 0",       "Ancestor([Gender].[F], 0)")
            .edgeCaseMdx("distance negative", "Ancestor([Gender].[F], -1)")
            .edgeCaseMdx("distance NULL",    "Ancestor([Gender].[F], NULL)")

            // [Gender] is flat (hasAll=true): F's only real ancestor, at distance 1 or at the
            // All member's own level, is the All member — the same member CurrentMemberContract
            // establishes via [Gender].[F].Parent.
            .value("(Ancestor([Gender].[F], [Gender].[F].Parent.Level) IS [Gender].[F].Parent)", "true")
            .value("(Ancestor([Gender].[F], 1) IS [Gender].[F].Parent)", "true")
            .value("(Ancestor([Gender].[F], 0) IS [Gender].[F])", "true")

            .dependsOn("Ancestor([Gender].[F], 1)", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")
            .build();
}
