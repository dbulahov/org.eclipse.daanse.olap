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

import org.eclipse.daanse.olap.function.def.member.strtomember.StrToMemberFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code StrToMember(<String>)}, which parses a
 * unique-name string into the member it identifies. {@code StrToMemberFunDef} is resolved by a
 * plain {@code ParametersCheckingFunctionDefinitionResolver} over a single {@code STRING}
 * parameter — {@code resolve()} delegates entirely to the generic {@code
 * FunctionMetaDataMatcher.match}, and {@code createCall} is not overridden, so there is no
 * hand-written resolver or validation code that could diverge from the declared signature or
 * throw instead of returning empty — the same {@code STRING}-parameter cost ladder as {@link
 * LenContract} (both accept the same parameter category). Unlike {@link
 * MembersFunctionContract}'s sibling {@code FunctionOperationAtom("Members")}, {@code
 * StrToMemberFunDef.compileCall} is a real, working implementation, not a stub that throws
 * {@code UnsupportedOperationException} — so RESULT/DEPENDENCIES/RESULT_SHAPE are asserted here,
 * not waived.
 *
 * <p>{@code StrToMemberCalc.evaluateInternal} delegates to {@code FunUtil.parseMember}, which
 * throws a diagnosed {@code MdxChildObjectNotFoundException} for a string that does not name a
 * real member, and {@code EmptyExpressionWasSpecifiedException} (wrapped as a diagnosed {@code
 * DaanseEvaluationException}) for a {@code NULL} string — both exercised as edge cases, not
 * value assertions, for exactly that reason.
 */
public final class StrToMemberContract {

    private StrToMemberContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("StrToMember")
            .signatures("<Member> StrToMember(<String>)")
            .returns(MEMBER)
            .arity(1, 1)

            .resolvesTo(StrToMemberFunDef.class, STRING)
            .resolvesWithCost(2, StrToMemberFunDef.class, VALUE)    // Value -> String
            .resolvesWithCost(4, StrToMemberFunDef.class, MEMBER)   // Member -> String
            .resolvesWithCost(4, StrToMemberFunDef.class, TUPLE)    // Tuple -> String
            .rejects(NUMERIC)          // Numeric converts to Value/Logical/Integer, never String
            .rejects(SET)               // Set converts to nothing (convertFromSet always false)
            .rejects()                  // arity 0
            .rejects(STRING, STRING)    // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("valid unique name",   "StrToMember(\"[Gender].[F]\")")
            // Documented: FunUtil.parseMember throws a diagnosed MdxChildObjectNotFoundException
            // for a name that resolves to no real member — an allowed edge-case outcome (see the
            // class Javadoc), not a crash.
            .edgeCaseMdx("unresolvable name (documented, diagnosed exception)",
                    "StrToMember(\"[Gender].[NoSuchMember]\")")
            // Documented: StrToMemberCalc throws a diagnosed DaanseEvaluationException for a
            // NULL string, same allowed-outcome reasoning.
            .edgeCaseMdx("String NULL (documented, diagnosed exception)", "StrToMember(NULL)")
            .edgeCaseMdx("empty string (documented, diagnosed exception)", "StrToMember(\"\")")

            .value("(StrToMember(\"[Gender].[F]\") IS [Gender].[F])", "true")

            .dependsOn("StrToMember(\"[Gender].[F]\")")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")

            .build();
}
