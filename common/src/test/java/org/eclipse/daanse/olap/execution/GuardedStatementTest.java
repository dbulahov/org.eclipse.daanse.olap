/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.execution;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Statement;

import org.eclipse.daanse.olap.api.execution.GuardedStatement;
import org.junit.jupiter.api.Test;

/**
 * The guard's ownership handshake: after markClosed a cancel must be a
 * pure no-op with NO JDBC call - the pooled connection behind the closed
 * statement may already serve another query, and drivers that cancel by
 * connection id would kill it.
 */
class GuardedStatementTest {

    @Test
    void cancelAfterMarkClosedNeverTouchesJdbc() throws Exception {
        Statement stmt = mock(Statement.class);
        GuardedStatement guard = new GuardedStatement(stmt);

        guard.markClosed();
        guard.cancel();

        verify(stmt, never()).cancel();
    }

    @Test
    void cancelBeforeCloseCancelsExactlyOnce() throws Exception {
        Statement stmt = mock(Statement.class);
        GuardedStatement guard = new GuardedStatement(stmt);

        guard.cancel();
        guard.cancel();
        guard.markClosed();
        guard.cancel();

        verify(stmt, times(1)).cancel();
    }

    @Test
    void throwingDriverCancelIsCrushed() throws Exception {
        Statement stmt = mock(Statement.class);
        doThrow(new RuntimeException("driver dislikes cancel")).when(stmt).cancel();
        GuardedStatement guard = new GuardedStatement(stmt);

        guard.cancel();

        verify(stmt).cancel();
    }
}
