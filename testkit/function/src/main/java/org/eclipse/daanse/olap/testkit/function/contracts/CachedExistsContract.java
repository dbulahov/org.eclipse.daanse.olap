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
import static org.eclipse.daanse.olap.api.DataType.TUPLE;
import static org.eclipse.daanse.olap.api.calc.ResultStyle.ITERABLE;
import static org.eclipse.daanse.olap.api.calc.ResultStyle.MUTABLE_LIST;

import org.eclipse.daanse.olap.function.def.nonstandard.CachedExistsFunDef;

/**
 * The contract of the non-standard MDX function {@code CachedExists(<Set>, <Tuple>,
 * <String>)}, a Mondrian/Daanse extension (not part of the MDX specification) that filters a
 * "non-dynamic" set down to the tuples whose ancestor at the given subtotal tuple's depth
 * matches that subtotal — the same shape as {@code Exists(<Set>, <Tuple>)}, but memoized in a
 * query-scoped cache keyed by the given {@code String} name plus the subtotal tuple's type, so
 * repeated calls for different subtotals of the same set during one query build the filter
 * partition only once. {@code CachedExistsFunDef} is resolved by a plain {@code
 * ParametersCheckingFunctionDefinitionResolver} over three required parameters ({@code SET},
 * {@code TUPLE}, {@code STRING}, in that order, no optionals) — {@code resolve()} delegates
 * entirely to the generic {@code FunctionMetaDataMatcher.match}, so there is no hand-written
 * resolver code that could diverge from the declared signature or throw instead of returning
 * empty. The {@code SET}/{@code TUPLE}/{@code STRING} cost ladders match {@link
 * SetToStrContract}/{@link TupleToStrContract}/{@link LenContract} respectively.
 *
 * <p>{@code CachedExistsCalc.evaluateInternal} groups the input set's tuples by the ancestor
 * (climbed up to the subtotal's own depth) matching each hierarchy the subtotal touches, then
 * returns the group for the given subtotal's own key. Grounded in {@code [Gender]}'s two-level
 * ({@code All}/{@code Gender}) structure: subtotal {@code [Gender].[F]} (same depth as the set's
 * members) returns only {@code F}; subtotal {@code [Gender].DefaultMember} (the shallower
 * {@code All} member, established by {@link DefaultMemberContract}) makes every set member climb
 * to the same {@code All} ancestor, so the "filter" keeps the whole set — the same rollup-to-All
 * identity {@link AggregateContract} uses for a different reason.
 *
 * <p>{@code CachedExistsCalc} does not override {@code dependsOn}, so it uses the generic
 * "depends on hierarchy if any child calc does" walk; its first child is the {@code Set}
 * argument, so it depends on whatever hierarchy that set itself depends on — the same {@code
 * [Gender].[Gender]} dependency {@link AllMembersContract} already establishes for {@code
 * <Hierarchy>.Members}-shaped set expressions.
 */
public final class CachedExistsContract {

    private CachedExistsContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("CachedExists")
            .signatures("<Set> CachedExists(<Set>, <Tuple>, <String>)")
            .returns(SET)
            .arity(3, 3)

            .resolvesTo(CachedExistsFunDef.class, SET, TUPLE, STRING)
            .resolvesWithCost(1, CachedExistsFunDef.class, LEVEL, TUPLE, STRING)    // Level -> Set
            .resolvesWithCost(2, CachedExistsFunDef.class, MEMBER, TUPLE, STRING)   // Member -> Set
            .resolvesWithCost(2, CachedExistsFunDef.class, TUPLE, TUPLE, STRING)    // Tuple -> Set
            .rejects(DIMENSION, TUPLE, STRING)   // Dimension does not convert to Set
            .rejects(HIERARCHY, TUPLE, STRING)   // Hierarchy does not convert to Set
            .rejects(NUMERIC, TUPLE, STRING)
            .rejects(STRING, TUPLE, STRING)

            .resolvesWithCost(1, CachedExistsFunDef.class, SET, MEMBER, STRING)     // Member -> Tuple
            .resolvesWithCost(1, CachedExistsFunDef.class, SET, HIERARCHY, STRING)  // Hierarchy -> Tuple
            .resolvesWithCost(2, CachedExistsFunDef.class, SET, DIMENSION, STRING)  // Dimension -> Tuple
            .rejects(SET, LEVEL, STRING)    // Level does not convert to Tuple
            .rejects(SET, SET, STRING)

            .resolvesWithCost(4, CachedExistsFunDef.class, SET, TUPLE, MEMBER)      // Member -> String
            .rejects(SET, TUPLE, NUMERIC)   // Numeric does not convert to String
            .rejects(SET, TUPLE, SET)

            .rejects()                              // arity 0
            .rejects(SET, TUPLE, STRING, STRING)    // arity 4

            .autoEdgeCases()
            .edgeCaseMdx("subtotal at the set's own depth", "CachedExists([Gender].Members, ([Gender].[F]), \"a\")")
            .edgeCaseMdx("subtotal rolled up to the All member",
                    "CachedExists([Gender].Members, ([Gender].DefaultMember), \"b\")")
            .edgeCaseMdx("member operand (lenient tuple)",
                    "CachedExists([Gender].Members, [Gender].[M], \"c\")")

            // Subtotal at the members' own depth keeps only the matching member; a subtotal
            // rolled up to the shallower All member makes every member's climbed ancestor equal
            // it, so the whole set survives — grounded in DefaultMemberContract's established
            // [Gender].DefaultMember == the All member fact.
            .value("SetToStr(CachedExists([Gender].Members, ([Gender].[F]), \"a\"))", "{[Gender].[Gender].[F]}")
            .value("SetToStr(CachedExists([Gender].Members, ([Gender].[M]), \"a2\"))", "{[Gender].[Gender].[M]}")
            .value("SetToStr(CachedExists([Gender].Members, ([Gender].DefaultMember), \"b\"))",
                    "{[Gender].[Gender].[All Gender], [Gender].[Gender].[F], [Gender].[Gender].[M]}")

            .dependsOn("CachedExists([Gender].Members, ([Gender].[F]), \"a\")")

            .resultStyle("CachedExists([Gender].Members, ([Gender].[F]), \"a\")", MUTABLE_LIST, MUTABLE_LIST)
            .resultStyle("CachedExists([Gender].Members, ([Gender].[F]), \"a\")", ITERABLE, ITERABLE)
            .independentMutableList("CachedExists([Gender].Members, ([Gender].[F]), \"a\")")

            .build();
}
