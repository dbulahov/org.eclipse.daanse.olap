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

import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.olap.function.def.set.setitem.SetItemFunDef;
import org.eclipse.daanse.olap.function.def.tupleitem.TupleItemFunDef;

/**
 * The contract of the MDX method {@code <object>.Item(...)}. Three resolvers share the atom —
 * {@code MethodOperationAtom("Item")}, not a {@code FunctionOperationAtom} — producing two
 * different FunDefs depending on the calling object's category:
 * <ul>
 *   <li>{@code SetItemIntResolver}: {@code <Set>.Item(<Index>)} — Member by position.
 *   <li>{@code SetItemStringResolver}: {@code <Set>.Item(<Member_Name>, ...)} — Member/Tuple
 *       by name, one String per element of the set's tuples; a hand-written {@code
 *       NoExpressionRequiredFunctionResolver} whose {@code resolve()} used to throw when the
 *       String count did not match the set's cardinality instead of returning {@code
 *       Optional.empty()} — fixed to keep {@code resolve()} a pure predicate (see {@link
 *       ExtractContract}'s identical finding).
 *   <li>{@code TupleItemResolver}: {@code <Tuple>.Item(<Index>)} — Member by position.
 * </ul>
 *
 * <p>Because Member converts to Tuple at cost 1 but to Set at cost 2 (and Tuple converts to
 * itself at cost 0 but to Set at cost 2), a bare Member or Tuple calling object always
 * resolves to {@code TupleItemFunDef}, never {@code SetItemFunDef} — even though both are
 * type-compatible. Only Level (which converts to Set but not to Tuple) and Dimension/Hierarchy
 * (which convert to Tuple but not to Set) are unambiguous.
 *
 * <p>{@code SetItemFunDef.getResultType} does an unchecked {@code (SetType) args[0].getType()}
 * and {@code compileList} demands a literal {@code SetType} — the same shape of gap {@link
 * ExtractContract} documents for a bare Level argument (Level -> Set is never materialized
 * into an actual set literal, unlike Member/Tuple -> Set). Unlike Extract, this is not
 * fixed here: it only fires once a query is actually compiled (Stage B), which this module
 * never reaches, so it is left as a precise, documented, currently-inert edge case rather than
 * force a promise waiver — same treatment as {@code BottomCountContract}'s negative-count gap.
 */
public final class ItemContract {

    private ItemContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Item")
            .atom(MethodOperationAtom.class)
            .signatures(
                    "<Member> <Set>.Item(<Numeric Expression>)",
                    "<Tuple> <Set>.Item(<String>)",
                    "<Member> <Tuple>.Item(<Numeric Expression>)")
            .arity(2, Integer.MAX_VALUE)

            .resolvesTo(SetItemFunDef.class, SET, NUMERIC)
            .resolvesTo(SetItemFunDef.class, SET, STRING)
            .resolvesTo(TupleItemFunDef.class, TUPLE, NUMERIC)
            .resolvesWithCost(1, SetItemFunDef.class, LEVEL, NUMERIC)        // Level -> Set (no Tuple path)
            .resolvesWithCost(1, TupleItemFunDef.class, MEMBER, NUMERIC)     // Member -> Tuple, cheaper than -> Set
            .resolvesWithCost(1, TupleItemFunDef.class, HIERARCHY, NUMERIC)  // Hierarchy -> Tuple (no Set path)
            .resolvesWithCost(2, TupleItemFunDef.class, DIMENSION, NUMERIC)  // Dimension -> Tuple (no Set path)
            .rejects(NUMERIC, NUMERIC)     // Numeric converts to neither Set nor Tuple
            .rejects()                     // arity 0
            .rejects(SET)                  // arity 1, an Index/name is required
            .rejects(TUPLE)                // arity 1
            .rejects(SET, SET)             // Set does not convert to Numeric or String
            // Fixed: used to throw out of resolve() (see the class Javadoc) instead of cleanly
            // rejecting a String-count mismatch against the (canonical arity-1) stub Set.
            .rejects(SET, STRING, STRING)

            .autoEdgeCases()
            .edgeCaseMdx("index in range",           "[Gender].Members.Item(0)")
            .edgeCaseMdx("index out of range",        "[Gender].Members.Item(100)")
            .edgeCaseMdx("index negative",            "[Gender].Members.Item(-1)")
            .edgeCaseMdx("index NULL",                "[Gender].Members.Item(NULL)")
            .edgeCaseMdx("name lookup, found",        "[Gender].Members.Item(\"F\")")
            .edgeCaseMdx("name lookup, not found",    "[Gender].Members.Item(\"ZZZ\")")
            .edgeCaseMdx("tuple item in range",       "([Gender].[F], [Measures].[Unit Sales]).Item(0)")
            .edgeCaseMdx("tuple item out of range",   "([Gender].[F], [Measures].[Unit Sales]).Item(5)")
            .edgeCaseMdx("tuple item index NULL",     "([Gender].[F], [Measures].[Unit Sales]).Item(NULL)")
            .edgeCaseMdx("member as degenerate tuple", "([Gender].[F]).Item(0)")
            // Documented, currently-inert gap: see the class Javadoc. Level -> Set resolves
            // (cost 1) but is never materialized, so SetItemFunDef.getResultType's unchecked
            // (SetType) cast would throw ClassCastException once this ever reaches Stage B.
            .edgeCaseMdx("bare Level calling object (documented gap)", "[Gender].[Gender].Item(0)")

            // [Gender].Members is {F, M} (F at index 0): matches HeadContract's established
            // ordering.
            .value("[Gender].Members.Item(0).Name", "F")
            .value("[Gender].Members.Item(1).Name", "M")
            .value("[Gender].Members.Item(\"F\").Name", "F")
            // Out-of-range/not-found/NULL-index results are covered as edge cases above
            // (survives-without-crashing), not as value assertions: they return the
            // hierarchy's null-member sentinel, and this contract does not assert what its
            // .Name property formats as.
            .value("([Gender].[F], [Measures].[Unit Sales]).Item(0).Name", "F")
            .value("([Gender].[F], [Measures].[Unit Sales]).Item(1).Name", "Unit Sales")

            .scalarDependsOn("[Gender].Members.Item(0).Name", "[Gender].[Gender]")
            .scalarDependsOn("([Gender].[F], [Measures].[Unit Sales]).Item(1).Name",
                              "[Gender].[Gender]", "[Measures].[Measures]")

            .waive(FunctionContract.Promise.RESULT_SHAPE,
                    "returns a Member or Tuple, never a Set; there is no set-context ResultStyle to honor")

            .waive(FunctionContract.Promise.SIGNATURE,
                    "declaredSignatureMatchesAcceptedCalls' minimal probe supplies exactly one String "
                            + "argument for \"<Tuple> <Set>.Item(<String>)\". SetItemStringResolver correctly "
                            + "requires one String per hierarchy of the calling set/tuple (see "
                            + "SetItemStringResolver.resolve()'s args.length - 1 != arity check) — for a Level "
                            + "or Member calling object (arity 1) that one probe argument matches, but the "
                            + "signature probe's synthetic Tuple stub has arity > 1, so the same single-String "
                            + "probe legitimately does not resolve. The string-based signature DSL has no way "
                            + "to express \"argument count must equal the calling tuple's arity\", so this is "
                            + "a declared-signature expressiveness gap, not a resolver bug (same class of gap "
                            + "as StarContract's numeric-conversion waiver).")

            .build();
}
