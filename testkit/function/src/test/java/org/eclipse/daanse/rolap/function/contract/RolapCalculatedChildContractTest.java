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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.execution.ExecutionImpl;
import org.eclipse.daanse.olap.testkit.function.AbstractFunctionContractTest;
import org.eclipse.daanse.olap.testkit.function.contracts.CalculatedChildContract;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.FoodmartTestInstance;

import org.eclipse.daanse.rolap.testkit.junit.api.InjectRolap;
import org.eclipse.daanse.rolap.testkit.junit.api.RolapContextTest;
import org.junit.jupiter.api.Test;

@RolapContextTest(value = FoodmartTestInstance.class)
class RolapCalculatedChildContractTest extends AbstractFunctionContractTest {

    @InjectRolap
    Connection connection;

    @Override
    protected FunctionContract contract() {
        return CalculatedChildContract.CONTRACT;
    }

    @Override
    protected Optional<Connection> connection() {
        return Optional.of(connection);
    }

    /**
     * {@code CalculatedChild} finds an <em>existing</em> calculated member — the shared
     * {@code MdxValues}/{@code CalcAssertions} single-expression helpers cannot exercise this
     * because their generated wrapper already owns the query's one {@code WITH MEMBER
     * [Measures].[Foo]} clause, so a real query with its own {@code WITH MEMBER} on another
     * dimension is built and executed directly here instead (the same shape {@code
     * SumContractTest.bindsTheHierarchiesOfItsSetArgument} uses for a promise beyond the
     * standard eight).
     */
    @Test
    void findsAnExistingCalculatedMember() {
        String mdx = "WITH MEMBER [Gender].[Gender].[All Gender].[Both] AS "
                + "'Aggregate([Gender].[Gender].[All Gender].Children)' "
                + "MEMBER [Measures].[Foo] AS "
                + "'[Gender].[Gender].[All Gender].CalculatedChild(\"Both\").Name' "
                + "SELECT {[Measures].[Foo]} ON COLUMNS FROM [Sales]";
        var query = connection.parseQuery(mdx);
        var statement = query.getStatement();
        var result = statement.getDaanseConnection().execute(new ExecutionImpl(statement, Optional.empty()));
        Cell cell = result.getCell(new int[] { 0 });
        assertThat(cell.getFormattedValue()).isEqualTo("Both");
    }
}
