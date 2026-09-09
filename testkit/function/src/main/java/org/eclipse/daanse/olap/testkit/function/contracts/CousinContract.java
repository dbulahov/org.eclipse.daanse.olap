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
import static org.eclipse.daanse.olap.api.DataType.SET;

import org.eclipse.daanse.olap.function.def.member.cousin.CousinFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Cousin(<Member>, <Ancestor Member>)} — the member
 * under {@code <Ancestor Member>} at the same relative position {@code <Member>} has under its
 * own parent (e.g. {@code Cousin([Feb 2001], [Q3 2001])} is {@code [August 2001]}). {@code
 * CousinFunDef} is resolved by a plain {@code ParametersCheckingFunctionDefinitionResolver}
 * over two {@code MEMBER} parameters — {@code resolve()} delegates entirely to the generic
 * {@code FunctionMetaDataMatcher.match}, so there is no hand-written resolver code that could
 * diverge from the declared signature or throw instead of returning empty.
 *
 * <p>{@code FunUtil.cousin} is already null-safe and well-behaved: a {@code NULL} (or any
 * null-sentinel) ancestor is returned as-is; a member shallower than the ancestor returns the
 * hierarchy's null member; a genuine hierarchy mismatch between the two arguments throws a
 * diagnosed {@code CousinHierarchyMismatchException} — but only from {@code
 * CousinCalc.evaluateInternal} (Stage B), never {@code resolve()} (Stage A), so it is inert in
 * this test kit regardless of the (cube-free) stub category probed.
 *
 * <p>{@code [Gender]} is flat (only one real level beneath {@code All}), so there is no
 * genuinely different branch to cross — the one non-degenerate value case here is
 * {@code Cousin(member, member's-own-parent)}, which the recursion works out to be the
 * original member itself (asking "who is under {@code All} at the same position {@code F} is
 * under {@code All}" is just {@code F} again).
 */
public final class CousinContract {

    private CousinContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Cousin")
            .signatures("<Member> Cousin(<Member>, <Member>)")
            .returns(MEMBER)
            .arity(2, 2)

            .resolvesTo(CousinFunDef.class, MEMBER, MEMBER)
            .resolvesWithCost(1, CousinFunDef.class, HIERARCHY, MEMBER)   // Hierarchy -> Member, position 0
            .resolvesWithCost(1, CousinFunDef.class, MEMBER, HIERARCHY)   // Hierarchy -> Member, position 1
            .resolvesWithCost(2, CousinFunDef.class, DIMENSION, MEMBER)   // Dimension -> Member, position 0
            .rejects(SET, MEMBER)       // Set does not convert to Member
            .rejects(MEMBER, SET)
            .rejects(MEMBER, LEVEL)      // Level does not convert to Member
            .rejects()                   // arity 0
            .rejects(MEMBER)              // arity 1
            .rejects(MEMBER, MEMBER, MEMBER)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("self cousin (ancestor is own parent)", "Cousin([Gender].[F], [Gender].[F].Parent)")
            .edgeCaseMdx("ancestor NULL",                 "Cousin([Gender].[F], [Gender].[F].Parent.Parent)")
            .edgeCaseMdx("member shallower than ancestor", "Cousin([Gender].[F].Parent, [Gender].[F])")
            // Documented, currently-inert (see the class Javadoc): throws a diagnosed
            // CousinHierarchyMismatchException, but only inside evaluateInternal (Stage B).
            .edgeCaseMdx("hierarchy mismatch (documented gap)",
                    "Cousin([Gender].[F], [Measures].[Unit Sales])")

            .value("(Cousin([Gender].[F], [Gender].[F].Parent) IS [Gender].[F])", "true")
            .value("(Cousin([Gender].[F], [Gender].[F].Parent.Parent) IS [Gender].[F].Parent.Parent)", "true")
            .value("(Cousin([Gender].[F].Parent, [Gender].[F]) IS [Gender].[F].Parent.Parent)", "true")

            .dependsOn("Cousin([Gender].[F], [Gender].[F].Parent)", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")
            .build();
}
