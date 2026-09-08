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
package org.eclipse.daanse.rolap.function.contract;

import java.util.Optional;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.testkit.function.AbstractFunctionContractTest;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.HeadContract;
import org.eclipse.daanse.rolap.testkit.junit.api.RolapContextTest;
import org.junit.jupiter.api.Disabled;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.FoodmartTestInstance;

@Disabled("disabled ubtil testkit implementation finished")
@RolapContextTest(FoodmartTestInstance.class)
class RolapHeadContractTest extends AbstractFunctionContractTest {

    private Connection connection;

    RolapHeadContractTest(Connection connection) {
        this.connection = connection;
    }

    @Override
    protected FunctionContract contract() {
        return HeadContract.CONTRACT;   // the same data as in the olap repo
    }

    @Override
    protected Optional<Connection> connection() {
        return Optional.of(connection);
    }

    @Override
    protected String cubeName() {
        return "Sales";
    }
}