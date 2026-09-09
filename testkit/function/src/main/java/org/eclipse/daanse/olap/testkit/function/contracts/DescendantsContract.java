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
import static org.eclipse.daanse.olap.api.DataType.SYMBOL;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.descendants.DescendantsByLevelFunDef;

/**
 * The contract of the MDX function {@code Descendants}. Two resolvers share the atom:
 * {@code DescendantsMemberResolver} (first argument a Member, 6 overloads) and
 * {@code DescendantsSetResolver} (first argument a Set, 6 overloads) — both produce the same
 * {@code DescendantsByLevelFunDef}. Only {@code DescendantsSetResolver} declares the DESC_FLAG
 * reserved words (SELF, AFTER, BEFORE, ...); {@code DescendantsMemberResolver} does not
 * override {@code getReservedWords()}, but the words are declared once per atom, not per
 * resolver, so every call site still gets them.
 */
public final class DescendantsContract {

    private DescendantsContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Descendants")
            .signatures(
                    "<Set> Descendants(<Member>)",
                    "<Set> Descendants(<Member>, <Level>)",
                    "<Set> Descendants(<Member>, <Level>, <Symbol>)",
                    "<Set> Descendants(<Member>, <Numeric Expression>)",
                    "<Set> Descendants(<Member>, <Numeric Expression>, <Symbol>)",
                    "<Set> Descendants(<Member>, <Empty>, <Symbol>)",
                    "<Set> Descendants(<Set>)",
                    "<Set> Descendants(<Set>, <Level>)",
                    "<Set> Descendants(<Set>, <Level>, <Symbol>)",
                    "<Set> Descendants(<Set>, <Numeric Expression>)",
                    "<Set> Descendants(<Set>, <Numeric Expression>, <Symbol>)",
                    "<Set> Descendants(<Set>, <Empty>, <Symbol>)")
            .returns(SET)
            .arity(1, 3)
            .reservedWords("SELF", "AFTER", "BEFORE", "BEFORE_AND_AFTER", "SELF_AND_AFTER",
                    "SELF_AND_BEFORE", "SELF_BEFORE_AFTER", "LEAVES")

            .resolvesTo(DescendantsByLevelFunDef.class, MEMBER)
            .resolvesTo(DescendantsByLevelFunDef.class, SET)
            .resolvesTo(DescendantsByLevelFunDef.class, MEMBER, LEVEL)
            .resolvesTo(DescendantsByLevelFunDef.class, SET, LEVEL)
            .resolvesTo(DescendantsByLevelFunDef.class, MEMBER, NUMERIC)
            .resolvesTo(DescendantsByLevelFunDef.class, SET, NUMERIC)
            .resolvesTo(DescendantsByLevelFunDef.class, MEMBER, LEVEL, SYMBOL)
            .resolvesTo(DescendantsByLevelFunDef.class, SET, LEVEL, SYMBOL)
            .resolvesTo(DescendantsByLevelFunDef.class, MEMBER, NUMERIC, SYMBOL)
            .resolvesTo(DescendantsByLevelFunDef.class, SET, NUMERIC, SYMBOL)
            .resolvesWithCost(1, DescendantsByLevelFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, DescendantsByLevelFunDef.class, DIMENSION)   // Dimension -> Member
            .resolvesWithCost(1, DescendantsByLevelFunDef.class, LEVEL)       // Level -> Set (no Member path)
            .resolvesWithCost(2, DescendantsByLevelFunDef.class, TUPLE)       // Tuple -> Set (no Member path)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects(MEMBER, STRING)
            .rejects()                             // arity 0
            .rejects(MEMBER, LEVEL, SYMBOL, SYMBOL) // arity 4

            .autoEdgeCases()
            .edgeCaseMdx("member, no level or depth", "Descendants([Gender].[F])")
            .edgeCaseMdx("member at its own level",   "Descendants([Gender].[F], [Gender].[F].Level)")
            .edgeCaseMdx("explicit SELF flag",        "Descendants([Gender].[F], [Gender].[F].Level, SELF)")
            .edgeCaseMdx("BEFORE from a level above",
                         "Descendants([Gender].[F].Parent, [Gender].[F].Level, BEFORE)")
            .edgeCaseMdx("depth zero",                "Descendants([Gender].[F], 0)")
            .edgeCaseMdx("depth one",                  "Descendants([Gender].[F], 1)")
            .edgeCaseMdx("depth negative",             "Descendants([Gender].[F], -1)")
            .edgeCaseMdx("depth MAX_VALUE",            "Descendants([Gender].[F], 2147483647)")
            .edgeCaseMdx("depth MIN_VALUE",            "Descendants([Gender].[F], -2147483648)")
            // A literal NULL depth resolves to the (Member, Numeric) overload (Null -> Numeric
            // is accepted by TypeUtil.canConvert), but its type stays NullType at compile time
            // (unlike Member/Tuple -> Set, that conversion is never materialized). Neither
            // depthSpecified nor depthEmpty is true, so DescendantsByLevelFunDef.compileCall
            // falls through to compiler.compileLevel(NULL), which has no NullType case and
            // throws a bare IllegalArgumentException — not a diagnosed OlapRuntimeException.
            .edgeCaseMdx("depth NULL",                 "Descendants([Gender].[F], NULL)")
            .edgeCaseMdx("LEAVES with depth",          "Descendants([Gender].[F], 0, LEAVES)")
            // "RECURSIVE" is not one of Descendants' own DESC_FLAG words, but reserved-word
            // recognition is global (FunctionServiceImpl.isReservedWord is one flat set across
            // every registered resolver, per IdImpl.accept): DrilldownMember registers it, so
            // the bare word still parses as a SYMBOL literal here, not a failed member lookup.
            // It then reaches FunUtil.getLiteralArg(call, 2, Flag.SELF, Flag.class), which
            // throws a diagnosed DaanseEvaluationException("Allowed values are: {SELF, AFTER,
            // ...}") — a proper OlapRuntimeException — for the unmatched symbol.
            .edgeCaseMdx("symbol reserved by another function",
                         "Descendants([Gender].[F], [Gender].[F].Level, RECURSIVE)")
            .edgeCaseMdx("null member",                "Descendants([Gender].[F].Parent.Parent)")
            .edgeCaseMdx("set of members",             "Descendants([Gender].Members, [Gender].[F].Level)")
            .edgeCaseMdx("empty set",                  "Descendants({}, [Gender].[F].Level)")

            // With no third argument, the default flag is SELF (not SELF_BEFORE_AFTER, which
            // only applies when the level/depth argument is omitted too) — so a member at
            // exactly the given level yields just itself.
            .value("Count(Descendants([Gender].[F], [Gender].[F].Level))", "1")
            .value("Count(Descendants([Gender].[F].Parent, [Gender].[F].Level))", "2")
            .value("SetToStr(Descendants([Gender].[F].Parent, [Gender].[F].Level))",
                   "{[Gender].[F], [Gender].[M]}")

            .dependsOn("Descendants([Gender].[F].Parent, [Gender].[F].Level)", "[Gender].[Gender]")
            .dependsOn("Descendants([Gender].Members, [Gender].[F].Level)", "[Gender].[Gender]")

            .resultStyle("Descendants([Gender].[F].Parent, [Gender].[F].Level)",
                         ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("Descendants([Gender].[F].Parent, [Gender].[F].Level)",
                         ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("Descendants([Gender].[F].Parent, [Gender].[F].Level)")

            .build();
}
