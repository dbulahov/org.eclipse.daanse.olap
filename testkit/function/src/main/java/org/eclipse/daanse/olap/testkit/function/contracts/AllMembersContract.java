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
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.set.hierarchy.AllMembersFunDef;

/** The contract of the MDX property {@code AllMembers}: {@code <Hierarchy>.AllMembers} and
 * {@code <Level>.AllMembers}. Both overloads share one {@link PlainPropertyOperationAtom}. */
public final class AllMembersContract {

    private AllMembersContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("AllMembers")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Hierarchy>.AllMembers", "<Level>.AllMembers")
            .returns(SET)
            .arity(1, 1)

            // Both overloads live under the same atom; the validator keeps whichever is
            // cheaper when a type matches both. A <Level> argument type-matches <Hierarchy>
            // too (Level -> Hierarchy, cost 1) but resolves to the <Level> overload, cost 0.
            .resolvesTo(AllMembersFunDef.class, HIERARCHY)
            .resolvesTo(org.eclipse.daanse.olap.function.def.set.level.AllMembersFunDef.class, LEVEL)
            .resolvesWithCost(2, AllMembersFunDef.class, DIMENSION)   // Dimension -> Hierarchy;
                                                                       // cheaper than -> Level (cost 3)
            .rejects(NUMERIC)
            .rejects(SET)
            .rejects(STRING)
            .rejects()                       // arity 0
            .rejects(HIERARCHY, HIERARCHY)   // arity 2

            // NOTE (verified against StandardFunctions.standard(), not asserted below): a
            // <Member> argument, e.g. [Gender].[F].AllMembers, is genuinely ambiguous. Member ->
            // Hierarchy and Member -> Level both cost 1, so the two overloads tie and
            // ValidatorImpl.getDef fails with "More than one function matches signature". This
            // DSL has no case for "resolves, but ambiguously" (isRejected() needs zero best
            // matches, resolvesTo(...) needs exactly one), so there is no case for it here.

            .autoEdgeCases()
            .edgeCaseMdx("hierarchy", "[Gender].AllMembers")
            .edgeCaseMdx("level",     "[Gender].[F].Level.AllMembers")

            // [Gender] has no calculated members in the Sales cube: same members as .Members.
            .value("SetToStr([Gender].AllMembers)",                 "{[Gender].[F], [Gender].[M]}")
            .value("SetToStr([Gender].[F].Level.AllMembers)",       "{[Gender].[F], [Gender].[M]}")

            .dependsOn("[Gender].AllMembers", "[Gender].[Gender]")
            .dependsOn("[Gender].[F].Level.AllMembers", "[Gender].[Gender]")

            .resultStyle("[Gender].AllMembers", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("[Gender].AllMembers", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("[Gender].AllMembers")
            .independentMutableList("[Gender].[F].Level.AllMembers")
            .build();
}
