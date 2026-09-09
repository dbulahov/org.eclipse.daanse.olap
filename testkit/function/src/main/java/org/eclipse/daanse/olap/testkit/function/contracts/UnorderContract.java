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
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.unorder.UnorderFunDef;

/**
 * The contract of the MDX function {@code Unorder}. One overload, {@code (Set)}. {@code
 * UnorderFunDef.compileCall} does {@code return compiler.compile(call.getArg(0))} — it does
 * not wrap the argument in any Calc of its own (the doc comment: "Currently Unorder has no
 * effect. In future, we may use the function as a marker..."). So today {@code
 * Unorder(X)} compiles to exactly the same Calc {@code X} alone would, and its dependsOn,
 * ResultStyle and value are all simply whatever {@code X} already produces — there is no
 * Unorder-specific behavior to test beyond resolution.
 */
public final class UnorderContract {

    private UnorderContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Unorder")
            .signatures("<Set> Unorder(<Set>)")
            .returns(SET)
            .arity(1, 1)

            .resolvesTo(UnorderFunDef.class, SET)
            .resolvesWithCost(2, UnorderFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, UnorderFunDef.class, TUPLE)    // Tuple -> Set
            .resolvesWithCost(1, UnorderFunDef.class, LEVEL)    // Level -> Set
            .rejects(DIMENSION)   // Dimension converts to Member/Hierarchy/Level, never Set
            .rejects(HIERARCHY)   // Hierarchy converts to Member/Dimension/Tuple, never Set
            .rejects(NUMERIC)
            .rejects()             // arity 0
            .rejects(SET, SET)     // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty set",               "Unorder({})")
            .edgeCaseMdx("already-ordered set",      "Unorder([Gender].Members)")
            .edgeCaseMdx("member operand (lenient)", "Unorder([Gender].[F])")

            // A pure pass-through today: the order the underlying set already has is
            // preserved exactly, nothing is shuffled.
            .value("Count(Unorder([Gender].Members))", "2")
            .value("SetToStr(Unorder([Gender].Members))", "{[Gender].[F], [Gender].[M]}")
            .value("Count(Unorder({}))", "0")

            .dependsOn("Unorder([Gender].Members)", "[Gender].[Gender]")

            .resultStyle("Unorder([Gender].Members)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("Unorder([Gender].Members)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("Unorder([Gender].Members)")

            .build();
}
