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
import static org.eclipse.daanse.olap.api.DataType.VALUE;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.function.def.member.memberorderkey.MemberOrderKeyFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code OrderKey}: {@code <Member>.OrderKey}. A single {@code
 * ParametersCheckingFunctionDefinitionResolver} over one {@code MEMBER} parameter — {@code
 * resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, so there
 * is no hand-written resolver code to diverge from the declared signature or throw instead of
 * returning empty. Same {@code MEMBER}/{@code HIERARCHY}/{@code DIMENSION} cost ladder as {@link
 * FirstChildContract}/{@link NextMemberContract}.
 *
 * <p>Unlike an ordinary scalar property, {@code MemberOrderKeyCalc.evaluateInternal} does not
 * return a displayable value at all: it wraps the member in a {@code
 * org.eclipse.daanse.olap.fun.sort.OrderKey} — a bare {@code Comparable} with no {@code
 * toString()} override, used exclusively as an internal sort-key marker ({@code Sorter}/{@code
 * OrderCurrentMemberCalc} both special-case {@code instanceof MemberOrderKeyCalc} to recognize a
 * {@code <Member>.OrderKey} sort expression passed to {@code Order(...)}). RESULT is waived for
 * that reason: there is no meaningful formatted value to assert — Java's default {@code
 * Object.toString()} is the only thing a generic Stage B evaluation could produce, and this atom
 * is never meant to be evaluated standalone as a displayable cell value, only passed through as
 * an opaque sort key.
 */
public final class OrderKeyContract {

    private OrderKeyContract() {
    }

    private static final String INTERNAL_SORT_KEY_ONLY =
            "MemberOrderKeyCalc.evaluateInternal wraps the member in an OrderKey — a bare "
                    + "Comparable with no toString() override, used exclusively as an internal sort-key "
                    + "marker for Order(...) (see the class Javadoc). There is no meaningful formatted "
                    + "value to assert here.";

    public static final FunctionContract CONTRACT = FunctionContract.of("OrderKey")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.OrderKey")
            .returns(VALUE)
            .arity(1, 1)

            .resolvesTo(MemberOrderKeyFunDef.class, MEMBER)
            .resolvesWithCost(1, MemberOrderKeyFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, MemberOrderKeyFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level does not convert to Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("member reference",              "[Gender].[F].OrderKey")
            .edgeCaseMdx("hierarchy/dimension reference",  "[Gender].OrderKey")

            .waive(Promise.RESULT, INTERNAL_SORT_KEY_ONLY)

            .scalarDependsOn("[Gender].[F].OrderKey")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
