/*
* Copyright (c) 2022 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.olap.api.execution;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.eclipse.daanse.olap.api.calc.profile.ProfileHandler;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.result.Result;

public interface Statement {

    Connection getConnection();

    CellSet executeQuery(String statement);

    CellSet executeQuery(Query query);

    ResultSet executeQuery(String statement, Optional<String> tabFields, int[] rowCountSlot);

    ResultSet executeQuery(QueryComponent queryComponent, Optional<String> tabFields, int[] rowCountSlot);

    Query getQuery();

    /**
     * Closes this statement.
     */
    void close();

    /**
     * Returns this statement's schema reader.
     *
     * @return Schema reader, not null
     */
    CatalogReader getCatalogReader();

    /**
     * Returns this statement's Catalog.
     *
     * @return Catalog, not null
     */
    Catalog getCatalog();

    /**
     * Returns this statement's connection.
     *
     * @return connection
     */
    Connection getDaanseConnection();

    void setQuery(Query query);

    /**
     * Enables profiling.
     *
     * Profiling information will be sent to the given writer when
     * org.eclipse.daanse.olap.api.result.Result#close() is called.
     *
     * If profileHandler is null, disables profiling.
     *
     * @param profileHandler Writer to which to send profiling information
     */
    void enableProfiling(ProfileHandler profileHandler);

    ProfileHandler getProfileHandler();

    /**
     * Returns the query timeout of this statement, in milliseconds.
     *
     * Zero means no timeout.
     *
     * Contrast with JDBC's java.sql.Statement#getQueryTimeout() method, which uses
     * an {@code int} value and a granularity of seconds.
     *
     * @return Timeout in milliseconds
     */
    long getQueryTimeoutMillis();

    /**
     * Issues a cancel request on this statement.
     *
     * Once the thread running the statement detects the cancel request, execution
     * will throw an exception. See BasicQueryTest.testCancel for an example of
     * usage of this method.
     *
     * @throws java.sql.SQLException on error
     */
    void cancel() throws SQLException;

    /**
     * Returns execution context if currently executing, null otherwise.
     *
     * @return Execution context
     */
    Execution getCurrentExecution();

    /**
     * Ends the current execution.
     *
     * @param execution Execution; must match the execution that was started
     *
     * @throws IllegalArgumentException if not started, or if execution does not
     *                                  match
     */
    void end(Execution execution);

    /**
     * Starts an execution.
     *
     * @param execution Execution context
     */
    void start(Execution execution);

    /**
     * Returns the ID of this statement, unique within the JVM.
     *
     * @return Unique ID of this statement
     */
    long getId();
}
