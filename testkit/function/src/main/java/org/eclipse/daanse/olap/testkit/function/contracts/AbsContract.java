/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.testkit.function.contracts;

import static org.eclipse.daanse.olap.api.DataType.INTEGER;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;

import org.eclipse.daanse.olap.function.def.vba.abs.AbsFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/** The contract of the MDX function {@code Abs}. */
public final class AbsContract {

    private AbsContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Abs")
            .signatures("<Numeric Expression> Abs(<Numeric Expression>)")
            .returns(NUMERIC)
            .arity(1, 1)

            .resolvesTo(AbsFunDef.class, NUMERIC)
            .resolvesTo(AbsFunDef.class, INTEGER)          // Integer -> Numeric, free
            .resolvesWithCost(3, AbsFunDef.class, MEMBER)  // Member -> Numeric, cost 3
            .rejects(SET)
            .rejects(STRING)
            .rejects()                                     // arity 0
            .rejects(NUMERIC, NUMERIC)                     // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("negative zero", "Abs(-0.0)")
            .edgeCaseMdx("largest double", "Abs(1.7976931348623157E308)")
            .edgeCaseMdx("null argument", "Abs(NULL)")

            .value("Abs(-3)", "3")
            .value("Abs(3)", "3")
            .value("Abs(0)", "0")
            .value("Abs(-2.5)", "2.5")
            .valueIsNull("Abs(NULL)")

            .scalarDependsOn("Abs(1)")                                        // depends on nothing
            .scalarDependsOn("Abs([Measures].[Unit Sales])", "[Measures].[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")
            .build();
}