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

import java.util.Optional;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.testkit.function.AbstractFunctionContractTest;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.TopCountContract;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.FoodmartTestInstance;

import org.eclipse.daanse.rolap.testkit.junit.api.InjectRolap;
import org.eclipse.daanse.rolap.testkit.junit.api.RolapContextTest;
import org.junit.jupiter.api.Disabled;

@Disabled
@RolapContextTest(value = FoodmartTestInstance.class)
class RolapTopCountContractTest extends AbstractFunctionContractTest {

    @InjectRolap
    Connection connection;

    @Override
    protected FunctionContract contract() {
        return TopCountContract.CONTRACT;
    }

    @Override
    protected Optional<Connection> connection() {
        return Optional.of(connection);
    }
}
