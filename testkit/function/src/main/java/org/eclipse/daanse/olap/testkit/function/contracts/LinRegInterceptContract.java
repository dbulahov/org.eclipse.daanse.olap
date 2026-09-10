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

import org.eclipse.daanse.olap.function.def.linreg.LinRegFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code LinRegIntercept(<Set>, <Numeric Expression Y>[,
 * <Numeric Expression X>])}, the {@code b} term of the least-squares regression line {@code y =
 * ax + b} fitted over a set (X defaults to the current value/index when omitted). {@code
 * LinRegInterceptResolver} is an {@code AbstractFunctionDefinitionMultiResolver} over a single
 * declared {@code FunctionMetaData} with {@code SET} and one {@code NUMERIC} parameter required,
 * followed by one trailing optional {@code NUMERIC} — the same inherently-safe shape {@link
 * AggregateContract}/{@link CorrelationContract} document.
 *
 * <p>{@code LinRegPointResolver}/{@code LinRegR2Resolver}/{@code LinRegSlopeResolver}/{@code
 * LinRegVarianceResolver} declare the four sibling atoms over the same {@code LinRegFunDef}
 * class, distinguished only by the {@code regType} constant ({@code POINT}/{@code R2}/{@code
 * INTERCEPT}/{@code SLOPE}/{@code VARIANCE}) passed to its constructor — {@code LinRegPoint}
 * additionally needs a fourth argument and compiles to a different {@code PointFunDef}/{@code
 * PointCalc} pair. Only {@code LinRegIntercept} is covered here.
 *
 * <p>Unlike {@link AggregateContract}/{@link AvgContract}/{@link CorrelationContract}'s {@code
 * dependsOn} overrides (all {@code HierarchyDependsChecker.checkAnyDependsButFirst}), {@code
 * LinRegCalc} does not override {@code dependsOn} at all — it uses the generic "depends on
 * hierarchy if any child calc does" walk over all three children ({@code Set}, {@code Y}, {@code
 * X}), with no exclusion for the hierarchies the set itself spans: {@code
 * [Gender].[Gender].[All Gender].Children} genuinely does depend on {@code Gender} on its own
 * (verified against a real connection), unlike the {@code checkAnyDependsButFirst} siblings
 * where the set's own hierarchy is excluded. A literal {@code Y}/{@code X} member argument like
 * {@code [Measures].[Unit Sales]} is still coerced to a scalar via the same implicit {@code
 * MemberValueCalc}-style wrapper {@link MinusContract}/{@link FormatContract} document — so it
 * does not depend on {@code Measures} itself, but does depend on every other hierarchy, and that
 * propagates straight through the plain child-walk: the whole call ends up depending on every
 * hierarchy in the cube except {@code Measures}, asserted below with {@code
 * scalarDoesNotDependOn} only, for the same reason those contracts give.
 *
 * <p>Regressing a series against itself ({@code Y = X}) always yields slope {@code 1} and
 * intercept exactly {@code 0} — the standard {@code y = x} identity line — as long as the series
 * has more than one distinct value over the set (checked already: {@link
 * CorrelationContract}'s self-correlation value {@code 1} establishes that {@code [Measures].
 * [Unit Sales]} does vary across {@code [Gender]}'s two children in the shared fixture). No real
 * fact-table figure needs to be known.
 */
public final class LinRegInterceptContract {

    private LinRegInterceptContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("LinRegIntercept")
            .signatures("<Numeric Expression> LinRegIntercept(<Set>, <Numeric Expression>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(2, 3)

            .resolvesTo(LinRegFunDef.class, SET, NUMERIC)
            .resolvesWithCost(1, LinRegFunDef.class, LEVEL, NUMERIC)    // Level -> Set
            .resolvesWithCost(2, LinRegFunDef.class, MEMBER, NUMERIC)   // Member -> Set
            .resolvesWithCost(2, LinRegFunDef.class, TUPLE, NUMERIC)    // Tuple -> Set
            .rejects(DIMENSION, NUMERIC)   // Dimension does not convert to Set
            .rejects(HIERARCHY, NUMERIC)   // Hierarchy does not convert to Set
            .rejects(NUMERIC, NUMERIC)
            .rejects(STRING, NUMERIC)

            .resolvesWithCost(3, LinRegFunDef.class, SET, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, LinRegFunDef.class, SET, TUPLE)    // Tuple -> Numeric
            .rejects(SET, SET)
            .rejects(SET, STRING)
            .rejects(SET, LEVEL)        // Level does not convert to Numeric
            .rejects(SET, HIERARCHY)    // Hierarchy does not convert to Numeric
            .rejects(SET, DIMENSION)    // Dimension does not convert to Numeric

            .resolvesTo(LinRegFunDef.class, SET, NUMERIC, NUMERIC)
            .resolvesWithCost(3, LinRegFunDef.class, SET, NUMERIC, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, LinRegFunDef.class, SET, NUMERIC, TUPLE)    // Tuple -> Numeric
            .rejects(SET, NUMERIC, SET)
            .rejects(SET, NUMERIC, STRING)
            .rejects(SET, NUMERIC, LEVEL)
            .rejects(SET, NUMERIC, HIERARCHY)
            .rejects(SET, NUMERIC, DIMENSION)

            .rejects()                                    // arity 0
            .rejects(SET)                                  // arity 1
            .rejects(SET, NUMERIC, NUMERIC, NUMERIC)       // arity 4

            .autoEdgeCases()
            .edgeCaseMdx("set and Y only (implicit current value as X)",
                    "LinRegIntercept([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("set, Y and X",
                    "LinRegIntercept([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                            + "[Measures].[Unit Sales])")
            .edgeCaseMdx("empty set",
                    "LinRegIntercept({}, [Measures].[Unit Sales], [Measures].[Unit Sales])")

            // Y = X regresses to the identity line y = x: slope 1, intercept exactly 0 — no
            // real fact-table figure needs to be known (see the class Javadoc).
            .value("LinRegIntercept([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                    + "[Measures].[Unit Sales])", "0")

            .scalarDoesNotDependOn("LinRegIntercept([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales], [Measures].[Unit Sales])", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
