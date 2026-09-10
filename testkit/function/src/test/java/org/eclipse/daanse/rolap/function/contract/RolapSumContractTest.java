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
package org.eclipse.daanse.rolap.function.contract;

import static org.eclipse.daanse.olap.testkit.function.eval.CalcAssertions.assertThatScalarExpr;

import java.util.Optional;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.testkit.function.AbstractFunctionContractTest;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.SumContract;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.FoodmartTestInstance;

import org.eclipse.daanse.rolap.testkit.junit.api.InjectRolap;
import org.eclipse.daanse.rolap.testkit.junit.api.RolapContextTest;
import org.junit.jupiter.api.Test;

@RolapContextTest(value = FoodmartTestInstance.class)
class RolapSumContractTest extends AbstractFunctionContractTest {

    @InjectRolap
    Connection connection;

    @Override
    protected FunctionContract contract() {
        return SumContract.CONTRACT;
    }

    @Override
    protected Optional<Connection> connection() {
        return Optional.of(connection);
    }

    /**
     * The binding promise, stated negatively - the very property that makes
     * expression caching possible in the first place. {@code SumContractTest} carries the
     * same test for the never-connected olap-repo run; this is the one that actually runs it.
     */
    @Test
    void bindsTheHierarchiesOfItsSetArgument() {
        assertThatScalarExpr(connection, cubeName(), "Sum([Gender].Members, [Measures].[Unit Sales])")
                .doesNotDependOn("[Gender].[Gender]", "[Measures]");

        // Unlike ".Members" (a constant enumeration), a literal CurrentMember set argument
        // genuinely varies with the outer Gender context, so it is not excluded here.
        assertThatScalarExpr(connection, cubeName(), "Sum({[Gender].CurrentMember}, [Measures].[Unit Sales])")
                .dependsOn("[Gender].[Gender]");
    }
}
