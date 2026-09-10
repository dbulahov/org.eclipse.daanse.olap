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
 * The contract of the MDX function {@code LinRegSlope(<Set>, <Numeric Expression Y>[, <Numeric
 * Expression X>])}, the {@code a} term of the least-squares regression line {@code y = ax + b}
 * fitted over a set (X defaults to the current value/index when omitted). {@code
 * LinRegSlopeResolver} shares the same {@code LinRegFunDef} class and single-overload,
 * one-trailing-optional shape {@link LinRegInterceptContract} documents as inherently safe —
 * only the {@code regType} constant passed to the constructor differs ({@code
 * LinRegFunDef.SLOPE}).
 *
 * <p>Same {@code dependsOn} behavior as {@link LinRegInterceptContract}: {@code LinRegCalc}
 * does not override it, so the call depends on every hierarchy the {@code Set}/{@code Y}/{@code
 * X} children depend on, with no exclusion for the set's own hierarchy.
 *
 * <p>Unlike {@link LinRegR2Contract}'s {@code R2}/{@code Variance} siblings — where {@code
 * LinRegCalc.linearReg} never calls {@code Value.setRSquared}/{@code setVariance}, leaving them
 * stuck at the {@code Double.MAX_VALUE} field initializer — {@code slope} (and {@code
 * intercept}) are set directly by the {@code Value} constructor {@code linearReg} calls, so
 * this one is genuinely computed. Regressing a series against itself ({@code Y = X}) always
 * yields slope exactly {@code 1} — the standard {@code y = x} identity line — as long as the
 * series has more than one distinct value over the set (checked already: {@link
 * CorrelationContract}'s self-correlation value {@code 1} establishes that {@code
 * [Measures].[Unit Sales]} does vary across {@code [Gender]}'s two children in the shared
 * fixture). No real fact-table figure needs to be known.
 */
public final class LinRegSlopeContract {

    private LinRegSlopeContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("LinRegSlope")
            .signatures("<Numeric Expression> LinRegSlope(<Set>, <Numeric Expression>, <Numeric Expression>)")
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
                    "LinRegSlope([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("set, Y and X",
                    "LinRegSlope([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                            + "[Measures].[Unit Sales])")
            .edgeCaseMdx("empty set",
                    "LinRegSlope({}, [Measures].[Unit Sales], [Measures].[Unit Sales])")

            // Y = X regresses to the identity line y = x: slope exactly 1 — no real
            // fact-table figure needs to be known (see the class Javadoc).
            .value("LinRegSlope([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                    + "[Measures].[Unit Sales])", "1")

            .scalarDoesNotDependOn("LinRegSlope([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales], [Measures].[Unit Sales])", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
