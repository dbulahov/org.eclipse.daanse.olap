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

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.set.range.RangeFunDef;

/** The contract of the MDX operator {@code :} (Range). */
public final class RangeContract {

    private RangeContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of(":")
            .atom(InfixOperationAtom.class)
            .signatures("<Member> : <Member>")
            .returns(SET)
            .arity(2, 2)

            .resolvesTo(RangeFunDef.class, MEMBER, MEMBER)
            .resolvesWithCost(1, RangeFunDef.class, HIERARCHY, MEMBER)   // Hierarchy -> Member
            .resolvesWithCost(2, RangeFunDef.class, DIMENSION, MEMBER)   // Dimension -> Member
            .rejects(LEVEL, MEMBER)    // Level converts to Hierarchy/Set/Dimension, never Member
            .rejects(SET, MEMBER)
            .rejects(NUMERIC, MEMBER)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER, MEMBER)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("same member",             "[Gender].[F] : [Gender].[F]")
            .edgeCaseMdx("reversed order",           "[Gender].[M] : [Gender].[F]")
            .edgeCaseMdx("different levels",         "[Gender].[F] : [Measures].[Unit Sales]")
            .edgeCaseMdx("one side runtime null",    "[Gender].[F].PrevMember : [Gender].[M]")
            .edgeCaseMdx("one side compile-time NULL", "NULL : [Gender].[F]")
            // RangeFunDef.compileMembers throws a diagnosed OlapRuntimeException
            // ("Function does not support two NULL member parameters") at compile time.
            .edgeCaseMdx("both sides NULL",          "NULL : NULL")

            .value("Count([Gender].[F] : [Gender].[M])",   "2")
            .value("SetToStr([Gender].[F] : [Gender].[F])", "{[Gender].[F]}")
            // RangeCalc.evaluateInternal returns {} whenever either endpoint is the null member,
            // whether that null came in as a compile-time NULL literal or a runtime .PrevMember.
            .value("Count([Gender].[F].PrevMember : [Gender].[M])", "0")
            .value("Count(NULL : [Gender].[F])",            "0")

            .dependsOn("[Gender].[F] : [Gender].[M]", "[Gender].[Gender]")
            .dependsOn("[Gender].[F] : [Gender].CurrentMember", "[Gender].[Gender]")

            .resultStyle("[Gender].[F] : [Gender].[M]", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("[Gender].[F] : [Gender].[M]", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("[Gender].[F] : [Gender].[M]")
            .build();
}
