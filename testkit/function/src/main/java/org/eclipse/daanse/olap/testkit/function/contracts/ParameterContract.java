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

import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Parameter(<Name>, <Type>, <DefaultValue>[,
 * <Description>])}, which declares a query parameter — eight overloads, one triple
 * ({@code Type} category, {@code DefaultValue} category, return category) per parameter kind:
 * NUMERIC/STRING (via a {@code NUMERIC}/{@code STRING} {@code Symbol} literal for {@code
 * Type}) or a dimension/hierarchy/level (a {@code Member} default) or a set (a {@code Set}
 * default), each with an optional trailing {@code Description}. No single return category
 * applies across all eight (same reasoning as {@link StarContract}), so none is declared.
 *
 * <p>{@code ParameterResolver.createFunDef} — reached from {@code
 * AbstractMetaDataMultiResolver.resolve()} once the generic type/category match against one
 * of the eight overloads already succeeds — used to have two crash points fixed here, the
 * same "resolve() must be a pure predicate" violation {@link ParamRefContract} found and
 * fixed in the sibling {@code ParamRef} function (both share {@code
 * ParameterFunDef.getParameterName}):
 * <ul>
 *   <li>{@code getParameterName(args)} threw {@code Util.newInternal(...)} whenever {@code
 *       args[0]} (the parameter Name) was not a string {@code Literal} — fixed identically to
 *       {@code ParamRefResolver}: {@code createFunDef} now checks {@code args[0] instanceof
 *       Literal} itself and returns {@code null} (declines the match) instead.
 *   <li>The {@code SYMBOL} branch (for the NUMERIC/STRING {@code Type} argument) cast {@code
 *       typeArg} to {@code Literal} unconditionally — {@code ClassCastException} for any
 *       SYMBOL-typed argument the parser did not itself produce as a literal. Fixed the same
 *       way: an {@code instanceof} check that declines the match instead of casting blindly.
 * </ul>
 *
 * <p>Those two fixes are not sufficient to make generic stub-based resolution probing safe
 * for this atom, though — {@code createFunDef} has several more throw points past them that
 * a real precondition only the parser satisfies, not a rewrite this contract attempts:
 * <ul>
 *   <li>The {@code DIMENSION}/{@code HIERARCHY}/{@code LEVEL} {@code Type} branch requires
 *       {@code ParameterFunDef.isConstant(typeArg)} — true only for a handful of concrete AST
 *       node shapes ({@code LevelExpression}, {@code HierarchyExpressionImpl}, {@code
 *       DimensionExpression}, or a specific {@code Hierarchy(CurrentMember(...))} call) — and
 *       throws a diagnosed {@code OlapRuntimeException} otherwise; a generic stub is none of
 *       those shapes.
 *   <li>The default-value argument is cross-checked against the resolved parameter type
 *       (dimension/hierarchy/level consistency) and throws on a mismatch a stub cannot avoid
 *       triggering meaningfully.
 *   <li>A supplied {@code Description} argument must itself be a string {@code Literal} or it
 *       throws — the same shape of precondition as the Name fix above, just not fixed here
 *       (this one is response-shape validation deep in default-value semantics, not a bare
 *       unguarded cast).
 * </ul>
 * RESOLUTION, SIGNATURE and EDGE are waived for the combined reason above — the same
 * "generic stub probing cannot supply a real precondition-satisfying AST node" situation
 * {@link AsContract} documents for {@code AsAliasResolver}, just spread across more throw
 * points here instead of one.
 *
 * <p>RESULT/DEPENDENCIES/RESULT_SHAPE are waived too: {@code ParameterFunDef.compileCall}
 * throws {@code UnsupportedOperationException} unconditionally — a {@code Parameter} call is
 * never compiled directly, it is rewritten during validation ({@code createCall}) into a real
 * query {@code Parameter} object, the same reasoning {@link ParamRefContract} gives (their
 * {@code FunDef} classes are identical on this point).
 */
public final class ParameterContract {

    private static final String STUB_CANNOT_SATISFY_PRECONDITIONS =
            "ParameterResolver.createFunDef requires args[0] to be a string Literal and, for a "
                    + "NUMERIC/STRING Type, args[1] to be a Symbol Literal too — see ParamRefContract's "
                    + "identical Name-literal finding, fixed the same way here. For a dimension/"
                    + "hierarchy/level Type it additionally requires ParameterFunDef.isConstant(args[1]), "
                    + "true only for a few concrete AST node shapes (LevelExpression, "
                    + "HierarchyExpressionImpl, DimensionExpression, or a specific Hierarchy(CurrentMember"
                    + "(...)) call) that a generic stub is never one of. This test kit's stub-based "
                    + "probing (TypedExpressionStub) can satisfy none of these preconditions, so the "
                    + "resolver declines (or, before the two local fixes documented in the class Javadoc, "
                    + "threw on) every probed call regardless of category.";

    private ParameterContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Parameter")
            .signatures(
                    "<String> Parameter(<String>, <Symbol>, <String>, <String>)",
                    "<String> Parameter(<String>, <Symbol>, <String>)",
                    "<Numeric Expression> Parameter(<String>, <Symbol>, <Numeric Expression>, <String>)",
                    "<Numeric Expression> Parameter(<String>, <Symbol>, <Numeric Expression>)",
                    "<Member> Parameter(<String>, <Hierarchy>, <Member>, <String>)",
                    "<Member> Parameter(<String>, <Hierarchy>, <Member>)",
                    "<Set> Parameter(<String>, <Hierarchy>, <Set>, <String>)",
                    "<Set> Parameter(<String>, <Hierarchy>, <Set>)")
            .arity(3, 4)

            .waive(Promise.RESOLUTION, STUB_CANNOT_SATISFY_PRECONDITIONS)
            .waive(Promise.SIGNATURE, STUB_CANNOT_SATISFY_PRECONDITIONS
                    + " hasDeclaredSignature() itself would pass; it is waived only because "
                    + "declaredSignatureMatchesAcceptedCalls() shares this promise and cannot run.")
            .waive(Promise.EDGE, STUB_CANNOT_SATISFY_PRECONDITIONS
                    + " Unlike ParamRefContract, the two local fixes here are not enough on their own — "
                    + "the isConstant()/default-value/description throw points past them are still "
                    + "reachable from resolve() for most probed shapes, so survivesEdgeCases' stub sweep "
                    + "cannot run clean without a much larger rewrite of createFunDef this contract does "
                    + "not attempt (see the class Javadoc).")

            .waive(Promise.RESULT,
                    "ParameterFunDef.compileCall throws UnsupportedOperationException "
                            + "unconditionally — a Parameter call is rewritten during validation into a "
                            + "real query Parameter object, never compiled directly; this module has no "
                            + "schema declaring such a parameter and never reaches Stage B regardless.")
            .waive(Promise.DEPENDENCIES,
                    "same reasoning as Promise.RESULT above: no compiled Calc, no fixture parameter.")
            .waive(Promise.RESULT_SHAPE,
                    "same reasoning as Promise.RESULT above: no compiled Calc, no fixture parameter.")

            .build();
}
