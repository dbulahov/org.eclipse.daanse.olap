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
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.nonempty.NonEmptyFunDef;

/**
 * The contract of the MDX function {@code NonEmpty(<Set1>, <Set2>?)}. {@code NonEmptyResolver}
 * is an {@code AbstractFunctionDefinitionMultiResolver} over one declared overload with an
 * optional second Set — {@code resolve()} delegates to the generic {@code
 * FunctionMetaDataMatcher.match}, so there is no hand-written resolver code that could diverge
 * from the declared signature or throw instead of returning empty.
 *
 * <p>{@code NonEmptyCalc} filters {@code Set1} down to the tuples that are non-empty — when
 * {@code Set2} is supplied, a {@code Set1} tuple survives if it is non-empty when crossed with
 * <em>any</em> tuple of {@code Set2} (an existential test, not a literal cross join: the
 * result still has {@code Set1}'s own arity, not {@code Set1}'s arity plus {@code Set2}'s).
 */
public final class NonEmptyContract {

    private NonEmptyContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("NonEmpty")
            .signatures("<Set> NonEmpty(<Set>, <Set>)")
            .returns(SET)
            .arity(1, 2)

            .resolvesTo(NonEmptyFunDef.class, SET)
            .resolvesTo(NonEmptyFunDef.class, SET, SET)
            .resolvesWithCost(2, NonEmptyFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, NonEmptyFunDef.class, TUPLE)    // Tuple -> Set
            .resolvesWithCost(1, NonEmptyFunDef.class, LEVEL)    // Level -> Set
            .rejects(DIMENSION)   // Dimension converts to Member/Hierarchy/Level, never Set
            .rejects(HIERARCHY)   // Hierarchy converts to Member/Dimension/Tuple, never Set
            .rejects(NUMERIC)
            .rejects()                 // arity 0
            .rejects(SET, SET, SET)    // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("empty first set",        "NonEmpty({}, [Gender].Members)")
            .edgeCaseMdx("empty second set",       "NonEmpty([Gender].Members, {})")
            .edgeCaseMdx("both sets empty",        "NonEmpty({}, {})")
            .edgeCaseMdx("single-set form",        "NonEmpty([Gender].Members)")
            .edgeCaseMdx("member operand",         "NonEmpty([Gender].[F])")

            .value("Count(NonEmpty([Gender].Members, {[Measures].[Unit Sales]}))", "2")
            .value("Count(NonEmpty({}, [Gender].Members))", "0")
            .value("Count(NonEmpty([Gender].Members, {}))", "0")

            .dependsOn("NonEmpty([Gender].Members, {[Measures].[Unit Sales]})",
                       "[Gender].[Gender]", "[Measures].[Measures]")

            .resultStyle("NonEmpty([Gender].Members, {[Measures].[Unit Sales]})",
                         ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("NonEmpty([Gender].Members, {[Measures].[Unit Sales]})",
                         ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("NonEmpty([Gender].Members, {[Measures].[Unit Sales]})")

            .build();
}
