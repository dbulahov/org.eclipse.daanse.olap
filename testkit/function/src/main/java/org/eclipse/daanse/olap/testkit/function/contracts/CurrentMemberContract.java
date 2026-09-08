/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
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
import org.eclipse.daanse.olap.function.def.hierarchy.member.HierarchyCurrentMemberFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/** The contract of the MDX property {@code CurrentMember}: {@code <Hierarchy>.CurrentMember}. */
public final class CurrentMemberContract {

    private CurrentMemberContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("CurrentMember")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Hierarchy>.CurrentMember")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(HierarchyCurrentMemberFunDef.class, HIERARCHY)
            .resolvesWithCost(1, HierarchyCurrentMemberFunDef.class, MEMBER)   // Member -> Hierarchy
            .resolvesWithCost(1, HierarchyCurrentMemberFunDef.class, LEVEL)    // Level -> Hierarchy
            .resolvesWithCost(2, HierarchyCurrentMemberFunDef.class, DIMENSION) // Dimension -> Hierarchy
            .rejects(SET)      // Set never converts to Hierarchy
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                       // arity 0
            .rejects(HIERARCHY, HIERARCHY)   // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("hierarchy reference",  "[Gender].CurrentMember")
            .edgeCaseMdx("member reference",     "[Gender].[F].CurrentMember")
            .edgeCaseMdx("level reference",      "[Gender].[F].Level.CurrentMember")

            // With no override in scope, the current member of a hierarchy defaults to its
            // All member — the same member [Gender].[F].Parent already established elsewhere.
            .value("([Gender].CurrentMember IS [Gender].[F].Parent)", "true")

            // HierarchyCurrentMemberFixedCalc.dependsOn(h) is literally "h == this.hierarchy":
            // the textbook single-hierarchy dependency every other contract in this suite
            // relies on but none has tested directly until now.
            .dependsOn("[Gender].CurrentMember", "[Gender].[Gender]")
            .dependsOn("[Gender].[F].Level.CurrentMember", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")
            .build();
}
