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

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.olap.function.def.operators.minus.MinusOperatorDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the infix MDX operator {@code -} ({@code MinusOperatorDef} via
 * {@code MinusResolver}): {@code <Numeric Expression> - <Numeric Expression>}.
 *
 * <p>Unary negation ({@code - <Numeric Expression>}) is a separate atom —
 * {@code MinusPrefixOperatorDef} is a {@code PrefixOperationAtom("-")}, not this
 * {@code InfixOperationAtom("-")} — and has its own contract.
 *
 * <p>{@code MinusCalc} does not override {@code dependsOn}; it uses the generic "depends on
 * hierarchy if any child calc does" walk. A literal member operand like {@code
 * [Measures].[Unit Sales]} is coerced to a scalar via an implicit {@code MemberValueCalc}-style
 * wrapper, which — the same "depends on everything except the hierarchy it fixes" shape {@link
 * ValueContract}/{@link ValidMeasureContract}/{@link CalculatedChildContract} document —
 * therefore does <em>not</em> depend on {@code Measures} itself, but does depend on every other
 * hierarchy in the cube. Asserted below with {@code scalarDoesNotDependOn} only, for the same
 * reason those contracts give: a positive {@code scalarDependsOn} would need every other
 * hierarchy in the fixture cube listed to satisfy {@code dependsOnExactly}.
 */
public final class MinusContract {

    private MinusContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("-")
            .atom(InfixOperationAtom.class)
            .signatures("<Numeric Expression> - <Numeric Expression>")
            .returns(NUMERIC)
            .arity(2, 2)

            .resolvesTo(MinusOperatorDef.class, NUMERIC, NUMERIC)
            .resolvesTo(MinusOperatorDef.class, INTEGER, NUMERIC)          // Integer -> Numeric, free
            .resolvesWithCost(3, MinusOperatorDef.class, MEMBER, NUMERIC)  // Member -> Numeric, cost 3
            .rejects(STRING, NUMERIC)
            .rejects(SET, NUMERIC)
            .rejects()                          // arity 0
            .rejects(NUMERIC, NUMERIC, NUMERIC)  // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("zero",              "0 - 5")
            .edgeCaseMdx("negative operand",   "-1 - 0")
            .edgeCaseMdx("fractional operand", "0.5 - 0")
            .edgeCaseMdx("negative result",    "3 - 10")
            .edgeCaseMdx("null left operand",  "NULL - 5")
            .edgeCaseMdx("null right operand", "5 - NULL")
            .edgeCaseMdx("largest double",     "1.7976931348623157E308 - -1.7976931348623157E308")
            .edgeCaseMdx("1E308 operand",      "1E308 - 0")
            .edgeCaseMdx("member operand",     "[Measures].[Unit Sales] - 1")

            .value("5 - 3",   "2")
            .value("0 - 5",   "-5")
            .value("3 - 10",  "-7")
            .value("-1 - 0",  "-1")
            .value("NULL - 5",  "-5")
            .value("5 - NULL",  "5")

            .scalarDependsOn("5 - 3")                                        // depends on nothing
            .scalarDoesNotDependOn("[Measures].[Unit Sales] - 1", "[Measures]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")
            .build();
}
