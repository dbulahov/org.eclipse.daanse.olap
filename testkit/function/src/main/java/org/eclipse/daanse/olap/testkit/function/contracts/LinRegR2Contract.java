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
 * The contract of the MDX function {@code LinRegR2(<Set>, <Numeric Expression Y>[, <Numeric
 * Expression X>])}, the coefficient of determination (R²) of the least-squares regression line
 * {@code y = ax + b} fitted over a set (X defaults to the current value/index when omitted).
 * {@code LinRegR2Resolver} shares the same {@code LinRegFunDef} class and single-overload,
 * one-trailing-optional shape {@link LinRegInterceptContract} documents as inherently safe —
 * only the {@code regType} constant passed to the constructor differs ({@code
 * LinRegFunDef.R2}).
 *
 * <p>Same {@code dependsOn} behavior as {@link LinRegInterceptContract}: {@code LinRegCalc}
 * does not override it, so the call depends on every hierarchy the {@code Set}/{@code Y}/{@code
 * X} children depend on, with no exclusion for the set's own hierarchy.
 *
 * <p><b>{@code LinRegCalc.linearReg} never computes R²</b>: it constructs a {@code
 * linreg.Value} via {@code new Value(intercept, slope, xlist, ylist)}, but that constructor
 * only sets {@code intercept}/{@code slope} — {@code rSquared} keeps the field initializer
 * {@code Double.MAX_VALUE} from {@code Value}, and {@code Value.setRSquared} is never called
 * anywhere in the codebase (confirmed by a full-repo search). So {@code LinRegR2(...)}
 * unconditionally returns {@code Double.MAX_VALUE} for every input, including a mathematically
 * perfect fit where the correct answer is exactly {@code 1} — verified against a real
 * connection. This is a real, pre-existing bug in {@code LinRegCalc}, not a test-authoring
 * mistake; the value case below documents the actual (broken) behavior rather than the
 * documented one, so a future fix to {@code linearReg} will fail this contract and must update
 * it deliberately.
 */
public final class LinRegR2Contract {

    /** {@code Double.MAX_VALUE}, formatted by the default MDX number format (comma-grouped,
     * fully expanded rather than scientific notation) — verified against a real connection. */
    private static final String DOUBLE_MAX_VALUE_FORMATTED =
            "179,769,313,486,231,570,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,"
            + "000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,"
            + "000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,"
            + "000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,"
            + "000,000,000";

    private LinRegR2Contract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("LinRegR2")
            .signatures("<Numeric Expression> LinRegR2(<Set>, <Numeric Expression>, <Numeric Expression>)")
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
                    "LinRegR2([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("set, Y and X",
                    "LinRegR2([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                            + "[Measures].[Unit Sales])")
            .edgeCaseMdx("empty set",
                    "LinRegR2({}, [Measures].[Unit Sales], [Measures].[Unit Sales])")

            // The mathematically correct answer for a perfect Y = X fit is exactly 1 (see the
            // class Javadoc's LinRegInterceptContract cross-reference for why the series varies
            // over the set), but LinRegCalc.linearReg never computes R² at all — Value.rSquared
            // keeps its Double.MAX_VALUE field initializer. This documents the real, current
            // (broken) behavior, verified against a real connection.
            .value("LinRegR2([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                    + "[Measures].[Unit Sales])", DOUBLE_MAX_VALUE_FORMATTED)

            .scalarDoesNotDependOn("LinRegR2([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales], [Measures].[Unit Sales])", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
