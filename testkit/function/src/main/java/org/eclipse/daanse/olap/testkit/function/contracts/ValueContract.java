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

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.function.def.numeric.value.ValueFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code Value}: {@code <Member>.Value}. A single {@code
 * ParametersCheckingFunctionDefinitionResolver} over one {@code MEMBER} parameter — {@code
 * resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, so there
 * is no hand-written resolver code to diverge from the declared signature or throw instead of
 * returning empty. Same {@code MEMBER}/{@code HIERARCHY}/{@code DIMENSION} cost ladder as {@link
 * FirstChildContract}/{@link NextMemberContract}/{@link ParentContract}.
 *
 * <p>{@code ValueCalc.evaluateInternal} sets the evaluation context to the given member and
 * re-evaluates the current calculation, restoring the prior context afterward — the same
 * "re-evaluate sliced by this member" mechanics {@link ValidMeasureContract} documents for its
 * own physical-cube passthrough, just for a single {@code Member} argument instead of a {@code
 * Tuple}/member shortcut, and with an explicit save/restore instead of relying on the caller.
 * The value assertion below checks the same null-safe equivalence {@link ValidMeasureContract}
 * uses, for the same reason: it does not depend on knowing any real fact-table figure.
 *
 * <p>{@code ValueCalc.dependsOn} overrides the default child-calc walk: depends on a hierarchy
 * if the generic check already says so, or if the member argument's own type does <em>not</em>
 * already use that hierarchy — the same "depends on everything except what the argument fixes"
 * shape {@link ValidMeasureContract} documents via {@code HierarchyDependsChecker.butDepends},
 * just inlined here for a single child rather than delegated to that helper. Asserted below with
 * {@code scalarDoesNotDependOn} only, in both directions ({@code Measures} member vs. {@code
 * Gender} member) — the positive contrast ("depends on every other hierarchy") is real but too
 * large a set for an exhaustive {@code dependsOnExactly} check to state safely.
 */
public final class ValueContract {

    private ValueContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Value")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Member>.Value")
            .returns(NUMERIC)
            .arity(1, 1)

            .resolvesTo(ValueFunDef.class, MEMBER)
            .resolvesWithCost(1, ValueFunDef.class, HIERARCHY)   // Hierarchy -> Member
            .resolvesWithCost(2, ValueFunDef.class, DIMENSION)   // Dimension -> Member
            .rejects(LEVEL)     // Level does not convert to Member
            .rejects(SET)
            .rejects(TUPLE)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                     // arity 0
            .rejects(MEMBER, MEMBER)       // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("measure member",                "[Measures].[Unit Sales].Value")
            .edgeCaseMdx("non-measure member",             "[Gender].[F].Value")
            .edgeCaseMdx("hierarchy/dimension reference",  "[Gender].Value")

            // ValueCalc re-evaluates the current calculation sliced by the given member, the
            // same result the bare member reference already produces — checked null-safely so
            // this does not depend on any real fact-table figure (see the class Javadoc).
            .value("IIf(IsEmpty([Measures].[Unit Sales].Value) "
                    + "AND IsEmpty([Measures].[Unit Sales]), \"true\", "
                    + "IIf([Measures].[Unit Sales].Value = [Measures].[Unit Sales], \"true\", \"false\"))",
                    "true")

            .scalarDoesNotDependOn("[Measures].[Unit Sales].Value", "[Measures]")
            .scalarDoesNotDependOn("[Gender].[F].Value", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
