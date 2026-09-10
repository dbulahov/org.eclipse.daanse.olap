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

import org.eclipse.daanse.olap.function.def.linreg.PointFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code LinRegPoint(<Numeric Expression xPoint>, <Set>,
 * <Numeric Expression Y>[, <Numeric Expression X>])}, the {@code y} value the least-squares
 * regression line {@code y = ax + b} (fitted over the set, exactly as {@link
 * LinRegInterceptContract} documents) predicts at {@code xPoint}. Unlike its four siblings
 * ({@code LinRegIntercept}/{@code LinRegSlope}/{@code LinRegR2}/{@code LinRegVariance}, all
 * sharing {@code LinRegFunDef} with {@code Set} as the <em>first</em> parameter), {@code
 * LinRegPointResolver} declares a distinct {@code PointFunDef} (a {@code LinRegFunDef} subclass)
 * whose declared parameter order is {@code (xPoint, Set, Y, X)} — {@code Set} is second, not
 * first — compiling to its own {@code PointCalc} rather than {@code LinRegCalc}. Only the
 * trailing {@code X} parameter is optional, so the same "single trailing optional after required
 * parameters" shape {@link LinRegInterceptContract}/{@link AggregateContract} document as
 * inherently safe still applies.
 *
 * <p>Like {@code LinRegCalc}, {@code PointCalc} does not override {@code dependsOn} — the
 * generic "depends on hierarchy if any child calc does" walk now runs over four children ({@code
 * xPoint}, {@code Set}, {@code Y}, {@code X}) instead of three, but the conclusion is unchanged
 * from {@link LinRegInterceptContract}: the {@code Set} argument genuinely depends on its own
 * hierarchy (no {@code checkAnyDependsButFirst} exclusion), a literal {@code Y}/{@code X} member
 * argument depends on everything except the hierarchy it fixes, and a constant {@code xPoint}
 * depends on nothing — so the whole call still depends on every hierarchy except the one a
 * literal {@code Y}/{@code X} member argument fixes (verified against a real connection).
 *
 * <p>{@code Y = X} still regresses to the identity line {@code y = x} (slope {@code 1},
 * intercept {@code 0} exactly — {@link LinRegInterceptContract} establishes why), so {@code
 * LinRegPoint(xPoint, set, X, X)} evaluates that line at {@code xPoint} and returns exactly
 * {@code xPoint} back, for any {@code xPoint} — no real fact-table figure needs to be known.
 */
public final class LinRegPointContract {

    private LinRegPointContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("LinRegPoint")
            .signatures("<Numeric Expression> LinRegPoint(<Numeric Expression>, <Set>, "
                    + "<Numeric Expression>, <Numeric Expression>)")
            .returns(NUMERIC)
            .arity(3, 4)

            // Position 0: xPoint (NUMERIC)
            .resolvesTo(PointFunDef.class, NUMERIC, SET, NUMERIC)
            .resolvesWithCost(3, PointFunDef.class, MEMBER, SET, NUMERIC)   // Member -> Numeric
            .resolvesWithCost(3, PointFunDef.class, TUPLE, SET, NUMERIC)    // Tuple -> Numeric
            .rejects(SET, SET, NUMERIC)
            .rejects(STRING, SET, NUMERIC)
            .rejects(LEVEL, SET, NUMERIC)        // Level does not convert to Numeric
            .rejects(HIERARCHY, SET, NUMERIC)    // Hierarchy does not convert to Numeric
            .rejects(DIMENSION, SET, NUMERIC)    // Dimension does not convert to Numeric

            // Position 1: the Set
            .resolvesWithCost(1, PointFunDef.class, NUMERIC, LEVEL, NUMERIC)    // Level -> Set
            .resolvesWithCost(2, PointFunDef.class, NUMERIC, MEMBER, NUMERIC)   // Member -> Set
            .resolvesWithCost(2, PointFunDef.class, NUMERIC, TUPLE, NUMERIC)    // Tuple -> Set
            .rejects(NUMERIC, DIMENSION, NUMERIC)   // Dimension does not convert to Set
            .rejects(NUMERIC, HIERARCHY, NUMERIC)   // Hierarchy does not convert to Set
            .rejects(NUMERIC, NUMERIC, NUMERIC)
            .rejects(NUMERIC, STRING, NUMERIC)

            // Position 2: Y (NUMERIC)
            .resolvesWithCost(3, PointFunDef.class, NUMERIC, SET, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, PointFunDef.class, NUMERIC, SET, TUPLE)    // Tuple -> Numeric
            .rejects(NUMERIC, SET, SET)
            .rejects(NUMERIC, SET, STRING)
            .rejects(NUMERIC, SET, LEVEL)
            .rejects(NUMERIC, SET, HIERARCHY)
            .rejects(NUMERIC, SET, DIMENSION)

            // Arity 4: trailing optional X (NUMERIC)
            .resolvesTo(PointFunDef.class, NUMERIC, SET, NUMERIC, NUMERIC)
            .resolvesWithCost(3, PointFunDef.class, NUMERIC, SET, NUMERIC, MEMBER)   // Member -> Numeric
            .resolvesWithCost(3, PointFunDef.class, NUMERIC, SET, NUMERIC, TUPLE)    // Tuple -> Numeric
            .rejects(NUMERIC, SET, NUMERIC, SET)
            .rejects(NUMERIC, SET, NUMERIC, STRING)
            .rejects(NUMERIC, SET, NUMERIC, LEVEL)
            .rejects(NUMERIC, SET, NUMERIC, HIERARCHY)
            .rejects(NUMERIC, SET, NUMERIC, DIMENSION)

            .rejects()                                          // arity 0
            .rejects(NUMERIC)                                    // arity 1
            .rejects(NUMERIC, SET)                                // arity 2
            .rejects(NUMERIC, SET, NUMERIC, NUMERIC, NUMERIC)    // arity 5

            .autoEdgeCases()
            .edgeCaseMdx("xPoint, set and Y only (implicit current value as X)",
                    "LinRegPoint(7, [Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("xPoint, set, Y and X",
                    "LinRegPoint(7, [Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                            + "[Measures].[Unit Sales])")
            .edgeCaseMdx("empty set",
                    "LinRegPoint(7, {}, [Measures].[Unit Sales], [Measures].[Unit Sales])")

            // Y = X regresses to the identity line y = x (see the class Javadoc), so evaluating
            // it at xPoint = 7 returns exactly 7 — no real fact-table figure needs to be known.
            .value("LinRegPoint(7, [Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                    + "[Measures].[Unit Sales])", "7")

            .scalarDoesNotDependOn("LinRegPoint(7, [Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales], [Measures].[Unit Sales])", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
