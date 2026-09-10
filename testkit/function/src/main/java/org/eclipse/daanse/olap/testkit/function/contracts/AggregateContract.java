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

import org.eclipse.daanse.olap.function.def.aggregate.AggregateFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Aggregate(<Set>[, <Numeric Expression>])}, which
 * rolls up a set using the current (or an explicitly named) measure's own aggregator.
 * {@code AggregateResolver} is an {@code AbstractFunctionDefinitionMultiResolver} over a single
 * declared {@code FunctionMetaData} with one required {@code SET} parameter followed by one
 * optional {@code NUMERIC} parameter. Unlike {@link ParallelPeriodContract}'s three-optional-slot
 * shape, a single trailing optional parameter after a required one carries no "wrong slot"
 * resolution risk: {@code FunctionMetaDataMatcher.match} can only skip the optional parameter
 * when there is no second argument left to consume, in which case the argument count itself
 * fails to match and the call is rejected cleanly — there is nothing later in the parameter list
 * for a mismatched second argument to fall through into.
 *
 * <p>{@code AggregateCalc.evaluateInternal} looks up {@code
 * StandardProperty.AGGREGATION_TYPE} from the evaluation context to pick an {@code Aggregator}
 * to roll up with — for the shared fixture cube's default (additive, {@code SUM}-aggregated)
 * {@code [Measures].[Unit Sales]}, {@code Aggregate([Gender].Members, [Measures].[Unit Sales])}
 * equals the grand total {@code ([Gender].DefaultMember, [Measures].[Unit Sales])} directly — a
 * standard rollup identity for a complete, non-overlapping partition ({@code F}/{@code M}) under
 * an {@code All} member. The value assertion below checks that identity null-safely, the same
 * pattern {@link ValidMeasureContract}/{@link ValueContract} use, so it does not depend on
 * knowing any real fact-table figure.
 *
 * <p>{@code AggregateCalc.dependsOn} delegates to {@code
 * HierarchyDependsChecker.checkAnyDependsButFirst} — whose own Javadoc states the rule this
 * contract is named after: "{@code Aggregate({Set}, {Value Expression})} depends upon everything
 * {@code {Value Expression}} depends upon, except the dimensions of {@code {Set}}." With no
 * explicit second argument, the implicit {@code CurrentValueUnknownCalc} unconditionally depends
 * on every hierarchy, so {@code Aggregate([Gender].Members)} ends up depending on every
 * hierarchy in the cube <em>except</em> {@code Gender} (the set's own hierarchy) — asserted below
 * with {@code scalarDoesNotDependOn}, the only direction {@code dependsOnExactly}'s exhaustive
 * comparison can express safely against a hierarchy list this large (verified against a real
 * connection).
 */
public final class AggregateContract {

    private AggregateContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Aggregate")
            .signatures("<Numeric Expression> Aggregate(<Set>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(1, 2)

            .resolvesTo(AggregateFunDef.class, SET)
            .resolvesWithCost(1, AggregateFunDef.class, LEVEL)    // Level -> Set
            .resolvesWithCost(2, AggregateFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, AggregateFunDef.class, TUPLE)    // Tuple -> Set
            .rejects(DIMENSION)   // Dimension does not convert to Set
            .rejects(HIERARCHY)   // Hierarchy does not convert to Set
            .rejects(NUMERIC)
            .rejects(STRING)
            .rejects()                    // arity 0

            .resolvesTo(AggregateFunDef.class, SET, NUMERIC)
            .resolvesWithCost(3, AggregateFunDef.class, SET, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, AggregateFunDef.class, SET, TUPLE)    // Tuple -> Numeric
            .rejects(SET, SET)
            .rejects(SET, STRING)
            .rejects(SET, LEVEL)
            .rejects(SET, HIERARCHY)
            .rejects(SET, DIMENSION)
            .rejects(SET, NUMERIC, NUMERIC)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("set only (implicit current measure)", "Aggregate([Gender].Members)")
            .edgeCaseMdx("set and explicit measure",
                    "Aggregate([Gender].Members, [Measures].[Unit Sales])")
            .edgeCaseMdx("empty set", "Aggregate({})")
            .edgeCaseMdx("singleton set", "Aggregate({[Gender].[F]})")

            // Standard additive rollup identity: F + M over a complete, hasAll=true partition
            // equals the All member's own total for the same (SUM-aggregated) measure — checked
            // null-safely so this does not depend on any real fact-table figure. Aggregating
            // over [Gender].[Gender].[All Gender].Children (just F and M), not [Gender].Members
            // (which also includes the All member itself and would double-count it).
            .value("IIf(IsEmpty(Aggregate([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])) "
                    + "AND IsEmpty(([Gender].DefaultMember, [Measures].[Unit Sales])), \"true\", "
                    + "IIf(Aggregate([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales]) "
                    + "= ([Gender].DefaultMember, [Measures].[Unit Sales]), \"true\", \"false\"))",
                    "true")

            .scalarDoesNotDependOn("Aggregate([Gender].Members)", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
