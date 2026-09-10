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
package org.eclipse.daanse.olap.testkit.function;

import static org.eclipse.daanse.olap.testkit.function.eval.CalcAssertions.assertThatScalarExpr;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.SumContract;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class SumContractTest extends AbstractFunctionContractTest {

    @Override
    protected FunctionContract contract() {
        return SumContract.CONTRACT;
    }

    /**
     * The binding promise, stated negatively - the very property that makes
     * expression caching possible in the first place.
     */
    @Test
    void bindsTheHierarchiesOfItsSetArgument() {
        Connection connection = connection().orElse(null);
        Assumptions.assumeTrue(connection != null, "stage B: no Connection supplied");

        assertThatScalarExpr(connection, cubeName(), "Sum([Gender].Members, [Measures].[Unit Sales])")
                .doesNotDependOn("[Gender].[Gender]", "[Measures]");

        // Unlike ".Members" (a constant enumeration), a literal CurrentMember set argument
        // genuinely varies with the outer Gender context, so it is not excluded here.
        assertThatScalarExpr(connection, cubeName(), "Sum({[Gender].CurrentMember}, [Measures].[Unit Sales])")
                .dependsOn("[Gender].[Gender]");
    }
}