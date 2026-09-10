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
 * The contract of the MDX function {@code LinRegVariance(<Set>, <Numeric Expression Y>[,
 * <Numeric Expression X>])}, the variance associated with the least-squares regression line
 * {@code y = ax + b} fitted over a set (X defaults to the current value/index when omitted).
 * {@code LinRegVarianceResolver} shares the same {@code LinRegFunDef} class and single-overload,
 * one-trailing-optional shape {@link LinRegInterceptContract} documents as inherently safe —
 * only the {@code regType} constant passed to the constructor differs ({@code
 * LinRegFunDef.VARIANCE}).
 *
 * <p>Same {@code dependsOn} behavior as {@link LinRegInterceptContract}: {@code LinRegCalc}
 * does not override it, so the call depends on every hierarchy the {@code Set}/{@code Y}/{@code
 * X} children depend on, with no exclusion for the set's own hierarchy.
 *
 * <p><b>{@code LinRegCalc.linearReg} never computes the variance</b> — the exact same bug
 * {@link LinRegR2Contract} documents for its {@code R2} sibling: {@code linearReg} constructs a
 * {@code linreg.Value} via {@code new Value(intercept, slope, xlist, ylist)}, a constructor
 * that only sets {@code intercept}/{@code slope}. {@code Value.variance} keeps its {@code
 * Double.MAX_VALUE} field initializer, and {@code Value.setVariance} is never called anywhere
 * in the codebase (confirmed by a full-repo search) — {@code LinRegVariance(...)}
 * unconditionally returns {@code Double.MAX_VALUE} for every input, regardless of how much the
 * data actually varies around the regression line. This is a real, pre-existing bug in {@code
 * LinRegCalc}, not a test-authoring mistake; the value case below documents the actual (broken)
 * behavior rather than a computed one, so a future fix to {@code linearReg} will fail this
 * contract and must update it deliberately.
 */
public final class LinRegVarianceContract {

    /** {@code Double.MAX_VALUE}, formatted by the default MDX number format (comma-grouped,
     * fully expanded rather than scientific notation) — verified against a real connection. */
    private static final String DOUBLE_MAX_VALUE_FORMATTED =
            "179,769,313,486,231,570,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,"
            + "000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,"
            + "000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,"
            + "000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,"
            + "000,000,000";

    private LinRegVarianceContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("LinRegVariance")
            .signatures("<Numeric Expression> LinRegVariance(<Set>, <Numeric Expression>, <Numeric Expression>)")
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
                    "LinRegVariance([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales])")
            .edgeCaseMdx("set, Y and X",
                    "LinRegVariance([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                            + "[Measures].[Unit Sales])")
            .edgeCaseMdx("empty set",
                    "LinRegVariance({}, [Measures].[Unit Sales], [Measures].[Unit Sales])")

            // LinRegCalc.linearReg never computes the variance at all — Value.variance keeps
            // its Double.MAX_VALUE field initializer (see the class Javadoc). This documents
            // the real, current (broken) behavior, verified against a real connection.
            .value("LinRegVariance([Gender].[Gender].[All Gender].Children, [Measures].[Unit Sales], "
                    + "[Measures].[Unit Sales])", DOUBLE_MAX_VALUE_FORMATTED)

            .scalarDoesNotDependOn("LinRegVariance([Gender].[Gender].[All Gender].Children, "
                    + "[Measures].[Unit Sales], [Measures].[Unit Sales])", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
