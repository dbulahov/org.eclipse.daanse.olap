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
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.DataType.SYMBOL;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.toggledrillstate.ToggleDrillStateFunDef;

/**
 * The contract of the MDX function {@code ToggleDrillState(<Set1>, <Set2>[, RECURSIVE])}.
 * {@code ToggleDrillStateResolver} is an {@code AbstractFunctionDefinitionMultiResolver} over
 * one declared overload with an optional {@code RECURSIVE} symbol and its own correctly
 * declared reserved word (compare {@link HierarchizeContract}'s "PRE"/"POST") — {@code
 * resolve()} delegates to the generic {@code FunctionMetaDataMatcher.match}, so there is no
 * hand-written resolver code that could diverge from the declared signature or throw instead
 * of returning empty.
 *
 * <p>{@code ToggleDrillStateFunDef.compileCall} unconditionally throws a diagnosed {@code
 * OlapRuntimeException} ("'RECURSIVE' is not supported") for any 3-argument call — the
 * declared signature advertises the {@code RECURSIVE} overload as accepted, but it can never
 * actually be used. That throw is in {@code compileCall} (Stage B), never {@code resolve()}
 * (Stage A), so — like {@code XtdFunDef}'s cube-touching {@code getResultType} — it is inert
 * in this test kit regardless of the (cube-free) stub category probed; documented as an edge
 * case below rather than a promise waiver.
 *
 * <p>{@code ToggleDrillStateCalc} always keeps each processed tuple from {@code Set1} first,
 * then — for a tuple whose relevant member is in {@code Set2} — either skips its already-drilled
 * descendants (collapse) or appends its children (expand), decided by whether the *next* tuple
 * in {@code Set1} is a descendant of that member. The two value assertions below build both
 * states directly from {@code [Gender]}'s flat, {@code hasAll=true} shape via {@code .Parent}
 * (same technique {@link HierarchizeContract} uses to reach the All member without depending
 * on its schema-specific name).
 */
public final class ToggleDrillStateContract {

    private ToggleDrillStateContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("ToggleDrillState")
            .signatures("<Set> ToggleDrillState(<Set>, <Set>, <Symbol>)")
            .returns(SET)
            .arity(2, 3)
            .reservedWords("RECURSIVE")

            .resolvesTo(ToggleDrillStateFunDef.class, SET, SET)
            .resolvesTo(ToggleDrillStateFunDef.class, SET, SET, SYMBOL)
            .resolvesWithCost(2, ToggleDrillStateFunDef.class, MEMBER, SET)   // Member -> Set
            .resolvesWithCost(1, ToggleDrillStateFunDef.class, LEVEL, SET)    // Level -> Set
            .rejects(DIMENSION, SET)   // Dimension does not convert to Set
            .rejects(SET)               // arity 1
            .rejects()                  // arity 0
            .rejects(SET, SET, STRING)  // String does not convert to Symbol
            .rejects(SET, SET, SYMBOL, SYMBOL)   // arity 4

            .autoEdgeCases()
            .edgeCaseMdx("both sets empty",   "ToggleDrillState({}, {})")
            .edgeCaseMdx("first set empty",   "ToggleDrillState({}, [Gender].Members)")
            .edgeCaseMdx("second set empty",  "ToggleDrillState([Gender].Members, {})")
            // Documented, currently-inert (see the class Javadoc): compileCall rejects any
            // 3-argument call outright, regardless of which reserved symbol is used.
            .edgeCaseMdx("with RECURSIVE (documented gap)",
                    "ToggleDrillState([Gender].Members, [Gender].Members, RECURSIVE)")
            // "ALL" is not RECURSIVE, but it is reserved globally (ExceptResolver registers
            // it — see DescendantsContract's matching note), so it still parses as a SYMBOL
            // literal here — hitting the very same argCount > 2 rejection either way.
            .edgeCaseMdx("symbol reserved by another function (documented gap)",
                    "ToggleDrillState([Gender].Members, [Gender].Members, ALL)")

            .value("Count(ToggleDrillState([Gender].Members, {}))", "2")
            .value("Count(ToggleDrillState({}, [Gender].Members))", "0")
            .value("SetToStr(ToggleDrillState([Gender].Members, {}))", "{[Gender].[F], [Gender].[M]}")
            // Expand: Set1 is just [All], Set2 = {All} — All is found but has no next tuple to
            // test as its descendant, so isDrilledDown stays false and All's children (F, M)
            // are appended after it.
            .value("Count(ToggleDrillState({[Gender].[F].Parent}, {[Gender].[F].Parent}))", "3")
            // Collapse: Set1 is [All, F, M] (already "drilled down"), Set2 = {All} — All is
            // found, its next tuple F is its descendant (isDrilledDown = true), so F and M are
            // both skipped, leaving only All.
            .value("Count(ToggleDrillState({[Gender].[F].Parent, [Gender].[F], [Gender].[M]}, "
                    + "{[Gender].[F].Parent}))", "1")

            .dependsOn("ToggleDrillState([Gender].Members, {})", "[Gender].[Gender]")

            .resultStyle("ToggleDrillState([Gender].Members, {})", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("ToggleDrillState([Gender].Members, {})", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("ToggleDrillState([Gender].Members, {})")

            .build();
}
