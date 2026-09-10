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

import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;
import static org.eclipse.daanse.olap.api.DataType.VALUE;

import org.eclipse.daanse.olap.function.def.member.members.MembersFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Members(<String Expression>)} — not to be confused
 * with the unrelated {@link MembersContract}, which shares the bare name {@code "Members"} but
 * is a wholly different {@code PlainPropertyOperationAtom} property (own package, own three
 * resolvers, {@code <Hierarchy|Level|Dimension>.Members}).
 *
 * <p>{@link MembersFunDef} is resolved by a plain {@code
 * ParametersCheckingFunctionDefinitionResolver} over a single {@code STRING} parameter — {@code
 * resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, and {@code
 * createCall} is not overridden (the inherited {@code AbstractFunctionDefinition} default), so
 * there is no hand-written resolver or validation code that could diverge from the declared
 * signature or throw instead of returning empty — the same {@code STRING}-parameter cost ladder
 * as {@link LenContract} (both accept the same parameter category).
 *
 * <p>{@code MembersFunDef}'s declared description ("Returns the last child of the parent of a
 * member") is a stale copy-paste leftover unrelated to what the function actually does — noted
 * here, not fixed, since {@code FunctionMetaData} description text is outside this contract's
 * scope (promises 1–8 never assert on it).
 *
 * <p>RESULT/DEPENDENCIES/RESULT_SHAPE are waived: {@code MembersFunDef.compileCall} throws
 * {@code UnsupportedOperationException} unconditionally — the same "declared and resolvable,
 * but never actually compiled" shape {@link ParamRefContract}/{@link ParameterContract} document
 * for their own atoms, just without a {@code createCall} precondition to also waive
 * RESOLUTION/SIGNATURE/EDGE for.
 */
public final class MembersFunctionContract {

    private MembersFunctionContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Members")
            .signatures("<Member> Members(<String>)")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(MembersFunDef.class, STRING)
            .resolvesWithCost(2, MembersFunDef.class, VALUE)    // Value -> String
            .resolvesWithCost(4, MembersFunDef.class, MEMBER)   // Member -> String
            .resolvesWithCost(4, MembersFunDef.class, TUPLE)    // Tuple -> String
            .rejects(NUMERIC)          // Numeric converts to Value/Logical/Integer, never String
            .rejects(SET)               // Set converts to nothing (convertFromSet always false)
            .rejects()                  // arity 0
            .rejects(STRING, STRING)    // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty string",      "Members(\"\")")
            .edgeCaseMdx("whitespace only",    "Members(\"   \")")
            .edgeCaseMdx("special characters", "Members(\"[a]b&c\")")
            .edgeCaseMdx("String NULL",        "Members(NULL)")

            .waive(Promise.RESULT,
                    "MembersFunDef.compileCall throws UnsupportedOperationException "
                            + "unconditionally — see the class Javadoc.")
            .waive(Promise.DEPENDENCIES,
                    "same reasoning as Promise.RESULT above: no compiled Calc.")
            .waive(Promise.RESULT_SHAPE,
                    "same reasoning as Promise.RESULT above: no compiled Calc.")

            .build();
}
