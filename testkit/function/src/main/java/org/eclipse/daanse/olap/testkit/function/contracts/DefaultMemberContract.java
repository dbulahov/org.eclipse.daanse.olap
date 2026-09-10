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

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.function.def.member.defaultmember.DefaultMemberFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code DefaultMember}: {@code <Hierarchy>.DefaultMember}.
 * Two resolvers share the {@code PlainPropertyOperationAtom("DefaultMember")} atom:
 * <ul>
 *   <li>{@code DefaultMemberResolver} — the real resolver, wrapping {@code
 *       DefaultMemberFunDef} over a single {@code HIERARCHY} parameter. {@code resolve()}
 *       delegates to the generic {@code FunctionMetaDataMatcher.match}, so it is a pure
 *       predicate; a {@code Dimension}, {@code Member} or {@code Level} calling object also
 *       resolves here, via the ordinary {@code TypeUtil.canConvert}-to-{@code Hierarchy} path
 *       (same as {@link CurrentMemberContract}'s cost table for {@code HierarchyCurrentMemberFunDef}).
 *   <li>{@code NonFunctionDefaultMemberResolver} — a {@code NonFunctionResolver} whose {@code
 *       resolve()} always returns {@code Optional.empty()} (matching {@code Builder.neverResolves()}'s
 *       exact reasoning). It contributes only a second declared overload, {@code
 *       "<Dimension>.DefaultMember"}, to {@code getRepresentativeFunctionMetaDatas()} — for
 *       MDSCHEMA_FUNCTIONS/documentation purposes — while the real dispatch for a Dimension
 *       calling object still goes through {@code DefaultMemberResolver} above (Dimension
 *       converts to Hierarchy at cost 2). No {@code .neverResolves()} waiver is needed here:
 *       every type the fictitious {@code "<Dimension>.DefaultMember"} overload's matcher
 *       accepts (anything convertible to {@code DIMENSION} — Member, Hierarchy, Level all are)
 *       is also accepted by the real {@code "<Hierarchy>.DefaultMember"} overload's matcher
 *       (the same types all convert to {@code HIERARCHY} too), so
 *       {@code declaredSignatureMatchesAcceptedCalls} has nothing to reconcile.
 * </ul>
 */
public final class DefaultMemberContract {

    private DefaultMemberContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("DefaultMember")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Hierarchy>.DefaultMember", "<Dimension>.DefaultMember")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(DefaultMemberFunDef.class, HIERARCHY)
            .resolvesWithCost(1, DefaultMemberFunDef.class, MEMBER)     // Member -> Hierarchy
            .resolvesWithCost(1, DefaultMemberFunDef.class, LEVEL)      // Level -> Hierarchy
            .resolvesWithCost(2, DefaultMemberFunDef.class, DIMENSION)  // Dimension -> Hierarchy
            .rejects(SET)      // Set never converts to Hierarchy
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                       // arity 0
            .rejects(HIERARCHY, HIERARCHY)   // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("hierarchy/dimension reference", "[Gender].DefaultMember")
            .edgeCaseMdx("member reference",     "[Gender].[F].DefaultMember")
            .edgeCaseMdx("level reference",      "[Gender].[F].Level.DefaultMember")

            // hasAll=true and no explicit defaultMember configured: the default member of
            // [Gender] is its All member — the same member CurrentMemberContract establishes
            // via [Gender].[F].Parent.
            .value("([Gender].DefaultMember IS [Gender].[F].Parent)", "true")

            .dependsOn("[Gender].DefaultMember")
            .dependsOn("[Gender].[F].Level.DefaultMember")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")
            .build();
}
