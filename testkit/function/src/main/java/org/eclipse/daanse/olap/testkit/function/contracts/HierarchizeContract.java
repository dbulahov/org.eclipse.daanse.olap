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

import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.DataType.SYMBOL;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.hierarchize.HierarchizeFunDef;

/**
 * The contract of the MDX function {@code Hierarchize}. One overload, {@code (Set,
 * PrePost?)}. Unlike most of the two/three-argument SYMBOL-flag functions contracted so far
 * (compare {@link DescendantsContract}, {@link DrilldownMemberContract}), {@code
 * HierarchizeResolver} correctly overrides {@code getReservedWords()} with its own "PRE" and
 * "POST" — no borrowed cross-function coincidence needed for the flag to parse.
 */
public final class HierarchizeContract {

    private HierarchizeContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Hierarchize")
            .signatures("<Set> Hierarchize(<Set>, <Symbol>)")
            .returns(SET)
            .arity(1, 2)
            .reservedWords("PRE", "POST")

            .resolvesTo(HierarchizeFunDef.class, SET)
            .resolvesTo(HierarchizeFunDef.class, SET, SYMBOL)
            .resolvesWithCost(2, HierarchizeFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(1, HierarchizeFunDef.class, LEVEL)    // Level -> Set
            .rejects(NUMERIC)          // arity 1, not Set-convertible
            .rejects(SET, STRING)      // String does not convert to Symbol
            .rejects()                 // arity 0
            .rejects(SET, SYMBOL, SYMBOL)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("empty set",   "Hierarchize({})")
            .edgeCaseMdx("default PRE", "Hierarchize([Gender].Members)")
            .edgeCaseMdx("explicit PRE",  "Hierarchize([Gender].Members, PRE)")
            .edgeCaseMdx("explicit POST", "Hierarchize([Gender].Members, POST)")
            // "ALL" is not one of Hierarchize's own PRE/POST words, but it is reserved
            // globally (ExceptResolver registers it — see DescendantsContract's matching
            // note), so it still parses as a SYMBOL literal here. FunUtil.getLiteralArg then
            // throws a diagnosed DaanseEvaluationException("Allowed values are: {PRE, POST}")
            // for the mismatch.
            .edgeCaseMdx("symbol reserved by another function", "Hierarchize([Gender].Members, ALL)")

            // [Gender] is flat (F and M are siblings under (All)), so PRE vs POST does not
            // visibly reorder [Gender].Members itself; pairing a member with its own Parent
            // shows the difference without needing to know the All member's schema-specific
            // name — the IIf compares names inside the MDX itself.
            .value("Count(Hierarchize([Gender].Members))", "2")
            .value("IIf(Hierarchize({[Gender].[F], [Gender].[F].Parent}, PRE).Item(0).Name = "
                   + "[Gender].[F].Parent.Name, \"YES\", \"NO\")", "YES")
            .value("IIf(Hierarchize({[Gender].[F], [Gender].[F].Parent}, POST).Item(0).Name = "
                   + "[Gender].[F].Parent.Name, \"YES\", \"NO\")", "NO")
            .value("IIf(Hierarchize({[Gender].[F], [Gender].[F].Parent}).Item(0).Name = "
                   + "[Gender].[F].Parent.Name, \"YES\", \"NO\")", "YES")   // default is PRE

            .dependsOn("Hierarchize([Gender].Members)", "[Gender].[Gender]")
            .dependsOn("Hierarchize([Gender].Members, POST)", "[Gender].[Gender]")

            .resultStyle("Hierarchize([Gender].Members)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("Hierarchize([Gender].Members)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("Hierarchize([Gender].Members)")

            .build();
}
