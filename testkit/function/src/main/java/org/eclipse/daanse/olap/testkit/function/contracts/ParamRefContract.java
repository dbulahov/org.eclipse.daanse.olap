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

import static org.eclipse.daanse.olap.api.DataType.VALUE;

import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code ParamRef(<String>)}, which returns the current
 * value of a query parameter declared elsewhere via {@code Parameter(...)}.
 *
 * <p>{@code ParamRefResolver.createFunDef} used to call {@code
 * ParameterFunDef.getParameterName(args)} unconditionally, which throws {@code
 * Util.newInternal("Parameter name must be a string constant")} whenever {@code args[0]} is
 * not a string {@code Literal} — a precondition only the parser guarantees for real {@code
 * ParamRef('x')} MDX. Since {@code createFunDef} is reached from {@code
 * AbstractMetaDataMultiResolver.resolve()} once the generic type/category check already
 * matches, this made {@code resolve()} throw instead of returning {@code Optional.empty()}
 * for any Name argument that is STRING-typed but not a literal — this test kit's stub-based
 * probing ({@code TypedExpressionStub}) is never a {@code Literal}, so every probed call hit
 * this. Fixed to check {@code args[0] instanceof Literal} itself and return {@code null}
 * (declining the match) instead of throwing — see {@code CallAssert.resolutionDoesNotThrow}'s
 * "resolve() must be a pure predicate" rule. Real {@code ParamRef("Foo")} MDX is unaffected
 * (the parser always produces a literal there); {@code QueryImpl} still raises a proper
 * diagnosed error later for a genuinely non-literal Name in real validation.
 *
 * <p>That fix is necessary but not sufficient to let generic stub-based resolution probing
 * (promises 3/4/6's declared-vs-accepted comparisons) exercise this atom meaningfully: no
 * stub this test kit can construct is ever a {@code Literal}, so the resolver now declines
 * every probed call cleanly rather than crashing on it — {@code resolvesDeclaredCalls} and
 * {@code declaredSignatureMatchesAcceptedCalls} are waived for the same reason {@link
 * AsContract} waives them (a real precondition only the parser can satisfy), though unlike
 * {@code AsAliasResolver}'s crash, the fix above means {@code survivesEdgeCases}' stub sweep
 * now runs unwaived and green: {@code resolve()} no longer throws for any probed shape.
 *
 * <p>RESULT/DEPENDENCIES/RESULT_SHAPE are also waived: {@code ParameterFunDef.compileCall}
 * throws {@code UnsupportedOperationException} unconditionally — a {@code ParamRef} call is
 * never compiled directly, it is rewritten during validation ({@code createCall}) into a
 * {@code ParameterExpressionImpl} wrapping a real query {@code Parameter}, machinery this
 * test kit's cube-free Stage A cannot exercise and whose real behavior depends on a query
 * actually declaring the referenced parameter via {@code Parameter(...)} — a fixture this
 * module has no schema to provide (same reasoning as the KPI accessor family, e.g. {@link
 * KPIGoalContract}), and moot regardless since this module never reaches Stage B.
 */
public final class ParamRefContract {

    private static final String STUB_CANNOT_BE_A_LITERAL =
            "ParamRefResolver.createFunDef requires args[0] to be a string Literal — a "
                    + "precondition only the parser can satisfy for a real 'ParamRef(\"x\")' call. "
                    + "This test kit's generic stub-based probing (TypedExpressionStub) is never a "
                    + "Literal, so the resolver declines every probed call regardless of category; "
                    + "see the class Javadoc for the resolve()-safety fix this contract is built on.";

    private ParamRefContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("ParamRef")
            .signatures("<Value> ParamRef(<String>)")
            .returns(VALUE)
            .arity(1, 1)

            .waive(Promise.RESOLUTION, STUB_CANNOT_BE_A_LITERAL)
            .waive(Promise.SIGNATURE, STUB_CANNOT_BE_A_LITERAL
                    + " hasDeclaredSignature() itself would pass; it is waived only because "
                    + "declaredSignatureMatchesAcceptedCalls() shares this promise and cannot run.")

            .autoEdgeCases()
            .edgeCaseMdx("simple name",       "ParamRef(\"SomeParam\")")
            .edgeCaseMdx("empty string name",  "ParamRef(\"\")")
            .edgeCaseMdx("whitespace name",    "ParamRef(\"   \")")
            .edgeCaseMdx("special characters", "ParamRef(\"[a]b&c\")")
            // A NULL literal's category is NULL, not STRING, so this does not resolve either
            // (same as before the fix, minus the crash) — a NULL parameter name is
            // meaningless, and now fails cleanly instead of throwing.
            .edgeCaseMdx("name NULL (documented, does not resolve)", "ParamRef(NULL)")

            .waive(Promise.RESULT,
                    "ParameterFunDef.compileCall throws UnsupportedOperationException "
                            + "unconditionally — a ParamRef call is rewritten during validation into a "
                            + "real query Parameter reference, never compiled directly; this module has "
                            + "no schema declaring such a parameter and never reaches Stage B regardless.")
            .waive(Promise.DEPENDENCIES,
                    "same reasoning as Promise.RESULT above: no compiled Calc, no fixture parameter.")
            .waive(Promise.RESULT_SHAPE,
                    "same reasoning as Promise.RESULT above: no compiled Calc, no fixture parameter.")

            .build();
}
