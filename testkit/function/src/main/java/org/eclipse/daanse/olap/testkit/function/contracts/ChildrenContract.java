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
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.set.children.ChildrenFunDef;

/** The contract of the MDX property {@code Children}: {@code <Member>.Children}. */
public final class ChildrenContract {

    private ChildrenContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Children")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.Children")
            .returns(SET)
            .arity(1, 1)

            .resolvesTo(ChildrenFunDef.class, MEMBER)
            .resolvesWithCost(1, ChildrenFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, ChildrenFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level converts to Hierarchy/Set/Dimension, never Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("leaf member",             "[Gender].[F].Children")
            .edgeCaseMdx("member with children",     "[Gender].[F].Parent.Children")
            .edgeCaseMdx("null member",              "[Gender].[F].Parent.Parent.Children")

            // [Gender] is a one-level hierarchy under an All member: a leaf has no children,
            // and the All member's children are exactly the two established Gender members.
            .value("Count([Gender].[F].Children)",           "0")
            .value("SetToStr([Gender].[F].Parent.Children)", "{[Gender].[Gender].[F], [Gender].[Gender].[M]}")

            .dependsOn("[Gender].[F].Children")
            .dependsOn("[Gender].[F].Parent.Children")

            // ChildrenCalc explicitly passes mutable = false to AbstractProfilingNestedTupleListCalc
            // ("The list is immutable, hence 'false' above" — its own comment), so it reports
            // ResultStyle.LIST, not MUTABLE_LIST: no independentMutableList() claim here.
            .resultStyle("[Gender].[F].Children", ResultStyle.LIST, ResultStyle.LIST)
            .resultStyle("[Gender].[F].Children", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .build();
}
