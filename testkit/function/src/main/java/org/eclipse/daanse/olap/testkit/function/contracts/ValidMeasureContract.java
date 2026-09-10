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

import org.eclipse.daanse.olap.function.def.member.validmeasure.ValidMeasureFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code ValidMeasure(<Tuple>)}, which forces dimensions that
 * do not join a virtual cube's base cube to their top (all) level before evaluating a measure.
 * {@code ValidMeasureFunDef} is resolved by a plain {@code
 * ParametersCheckingFunctionDefinitionResolver} over a single {@code TUPLE} parameter — {@code
 * resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, so there
 * is no hand-written resolver code that could diverge from the declared signature or throw
 * instead of returning empty. Same {@code TUPLE}/{@code MEMBER}/{@code HIERARCHY}/{@code
 * DIMENSION} cost ladder as {@link TupleToStrContract} — {@code ValidMeasureFunDef.compileCall}
 * branches on the same {@code TypeUtil.couldBeMember(...)} compile-time shortcut.
 *
 * <p>{@code ValidMeasureCalc.evaluateInternal} only exercises its dimension-forcing logic when
 * {@code evaluator.getCube()} is <em>not</em> a {@code PhysicalCube} — this test kit's shared
 * Sales/FoodMart-derived fixture is a physical cube, so that branch is unreachable here; the
 * function instead degenerates to a transparent passthrough ({@code
 * evaluator.setContext(members); return evaluator.evaluateCurrent()}), re-evaluating the current
 * measure sliced by whatever member/tuple was passed in — the same value the bare member/tuple
 * expression would already produce on its own. The value assertion below checks exactly that
 * equivalence (null-safe, since it does not depend on knowing any real fact-table figure), not a
 * specific virtual-cube-forcing result — this module has no virtual cube to provide one.
 *
 * <p>{@code ValidMeasureCalc.dependsOn} overrides the default child-calc walk with {@code
 * HierarchyDependsChecker.butDepends}: true if the child calc itself dynamically depends on the
 * hierarchy, false if the child calc's own <em>type</em> already uses that hierarchy (a literal
 * member/tuple reference fixes it), true for every other hierarchy otherwise — the same "depends
 * on everything except what the wrapped expression fixes" shape {@code
 * HierarchyDependsChecker.checkAnyDependsButFirst}'s own Javadoc describes for {@code
 * Aggregate}. A bare member reference like {@code [Measures].[Unit Sales]} does not itself
 * dynamically depend on {@code Measures}, but its type does use it, so {@code
 * ValidMeasure([Measures].[Unit Sales])} does <em>not</em> depend on {@code Measures} — it does
 * still depend on every other hierarchy in the cube (e.g. {@code Gender}). Asserted below with
 * {@code scalarDoesNotDependOn} only, in both directions ({@code Measures} member vs. {@code
 * Gender} member) — the positive contrast is real but too large a set for an exhaustive
 * {@code dependsOnExactly} check to state safely.
 */
public final class ValidMeasureContract {

    private ValidMeasureContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("ValidMeasure")
            .signatures("<Numeric Expression> ValidMeasure(<Tuple>)")
            .returns(NUMERIC)
            .arity(1, 1)

            .resolvesTo(ValidMeasureFunDef.class, TUPLE)
            .resolvesWithCost(1, ValidMeasureFunDef.class, MEMBER)      // Member -> Tuple
            .resolvesWithCost(1, ValidMeasureFunDef.class, HIERARCHY)   // Hierarchy -> Tuple
            .resolvesWithCost(2, ValidMeasureFunDef.class, DIMENSION)   // Dimension -> Tuple
            .rejects(LEVEL)     // Level does not convert to Tuple
            .rejects(SET)
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                  // arity 0
            .rejects(TUPLE, TUPLE)      // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("single measure member (member shortcut, physical cube passthrough)",
                    "ValidMeasure([Measures].[Unit Sales])")
            .edgeCaseMdx("non-measure member (member shortcut, physical cube passthrough)",
                    "ValidMeasure([Gender].[F])")
            .edgeCaseMdx("genuine tuple (physical cube passthrough)",
                    "ValidMeasure(([Gender].[F], [Measures].[Unit Sales]))")

            // Physical-cube passthrough (see the class Javadoc): ValidMeasure re-evaluates the
            // current measure sliced by the given member/tuple, the same result the bare
            // expression already produces — checked null-safely so this does not depend on any
            // real fact-table figure.
            .value("IIf(IsEmpty(ValidMeasure([Measures].[Unit Sales])) "
                    + "AND IsEmpty([Measures].[Unit Sales]), \"true\", "
                    + "IIf(ValidMeasure([Measures].[Unit Sales]) = [Measures].[Unit Sales], \"true\", \"false\"))",
                    "true")

            .scalarDoesNotDependOn("ValidMeasure([Measures].[Unit Sales])", "[Measures]")
            .scalarDoesNotDependOn("ValidMeasure([Gender].[F])", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
