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
import static org.eclipse.daanse.olap.api.DataType.VALUE;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.order.OrderFunDef;

/**
 * The contract of the MDX function {@code Order(<Set>, <Value>[, <Symbol>]...)}. {@code
 * OrderResolver} is hand-written but, like {@link ExtractContract}'s fixed resolver, is a
 * pure predicate: it walks the arguments greedily — {@code args[0]} must convert to {@code
 * SET}; every subsequent argument must convert to {@code VALUE} (a sort key), optionally
 * followed by one that converts to {@code SYMBOL} (a sort flag — if the next argument does
 * not convert to {@code SYMBOL} it is simply left for the next key/flag pair, defaulting the
 * previous key to {@code ASC}) — returning {@code Optional.empty()} the moment any key
 * argument fails to convert to {@code VALUE}, never throwing.
 *
 * <p>{@code SYMBOL} only ever matches a bare reserved-word token (nothing else converts to
 * it, see {@code TypeUtil.canConvert}), so {@code Order}'s own reserved words ({@code ASC},
 * {@code DESC}, {@code BASC}, {@code BDESC}) are what let a flag parse as {@code SYMBOL} in
 * the first place — correctly declared via {@code OrderResolver.getReservedWords()} (compare
 * {@link HierarchizeContract}'s "PRE"/"POST", the only other contract so far with its own
 * correctly-declared reserved words rather than a borrowed cross-function one).
 *
 * <p>Because {@code VALUE} does not accept {@code SET}, {@code LEVEL}, {@code HIERARCHY} or
 * {@code DIMENSION} (only {@code MEMBER}/{@code TUPLE}, at cost 4, and any true scalar, for
 * free — see {@code TypeUtil.convertFromXxx}), a second {@code Set} argument — the shape
 * {@code NonEmpty}/{@code Crossjoin} accept — is rejected here, not coerced.
 */
public final class OrderContract {

    private OrderContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Order")
            .signatures("<Set> Order(<Set>, <Value>, <Symbol>)")
            .returns(SET)
            .arity(2, Integer.MAX_VALUE)
            .reservedWords("ASC", "DESC", "BASC", "BDESC")

            .resolvesTo(OrderFunDef.class, SET, VALUE)
            .resolvesTo(OrderFunDef.class, SET, NUMERIC)   // Numeric -> Value, free
            .resolvesTo(OrderFunDef.class, SET, STRING)    // String -> Value, free
            .resolvesWithCost(4, OrderFunDef.class, SET, MEMBER)   // Member -> Value
            .resolvesWithCost(4, OrderFunDef.class, SET, TUPLE)    // Tuple -> Value
            .resolvesTo(OrderFunDef.class, SET, VALUE, SYMBOL)
            .resolvesTo(OrderFunDef.class, SET, VALUE, VALUE)      // two keys, no explicit flag
            .rejects(SET, SET)          // Set does not convert to Value
            .rejects(SET, LEVEL)        // Level does not convert to Value
            .rejects(SET, HIERARCHY)    // Hierarchy does not convert to Value
            .rejects(SET, DIMENSION)    // Dimension does not convert to Value
            .rejects()                  // arity 0
            .rejects(SET)                // arity 1, no sort key

            .autoEdgeCases()
            .edgeCaseMdx("empty set",              "Order({}, 1)")
            .edgeCaseMdx("default ASC",             "Order([Gender].Members, [Gender].CurrentMember.Name)")
            .edgeCaseMdx("explicit ASC",            "Order([Gender].Members, [Gender].CurrentMember.Name, ASC)")
            .edgeCaseMdx("explicit DESC",           "Order([Gender].Members, [Gender].CurrentMember.Name, DESC)")
            .edgeCaseMdx("break hierarchy (BASC)",  "Order([Gender].Members, [Gender].CurrentMember.Name, BASC)")
            .edgeCaseMdx("two sort keys",
                    "Order([Gender].Members, [Gender].CurrentMember.Name, ASC, [Measures].[Unit Sales], DESC)")
            // "ALL" is not one of Order's own ASC/DESC/BASC/BDESC words, but it is reserved
            // globally (ExceptResolver registers it — see DescendantsContract's matching
            // note), so it still parses as a SYMBOL literal here, consumed by the resolver as
            // a sort flag. FunUtil.getLiteralArg then throws a diagnosed exception for the
            // mismatch — but only inside compileCall (Stage B), never during resolve().
            .edgeCaseMdx("symbol reserved by another function",
                    "Order([Gender].Members, [Gender].CurrentMember.Name, ALL)")

            .value("Count(Order([Gender].Members, [Gender].CurrentMember.Name))", "2")
            .value("Order([Gender].Members, [Gender].CurrentMember.Name).Item(0).Name", "F")
            .value("Order([Gender].Members, [Gender].CurrentMember.Name, ASC).Item(0).Name", "F")
            .value("Order([Gender].Members, [Gender].CurrentMember.Name, DESC).Item(0).Name", "M")

            .dependsOn("Order([Gender].Members, [Gender].CurrentMember.Name)", "[Gender].[Gender]")
            .dependsOn("Order([Gender].Members, [Measures].[Unit Sales], DESC)",
                       "[Gender].[Gender]", "[Measures].[Measures]")

            .resultStyle("Order([Gender].Members, [Gender].CurrentMember.Name)",
                         ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("Order([Gender].Members, [Gender].CurrentMember.Name)",
                         ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("Order([Gender].Members, [Gender].CurrentMember.Name)")

            .build();
}
