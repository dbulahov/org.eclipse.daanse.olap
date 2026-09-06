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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.Statement;
import java.util.Optional;

import org.eclipse.daanse.olap.api.execution.ExecutionContext;
import org.eclipse.daanse.olap.api.execution.ExecutionMetadata;
import org.eclipse.daanse.olap.api.execution.GuardedStatement;
import org.junit.jupiter.api.Test;

/**
 * Statements register on the ABSOLUTE ROOT context: almost all SQL runs
 * on child contexts (member loads, tuple reads, statistics, drillthrough),
 * and a cancel that walked only its own list reached none of them - those
 * statements were effectively unkillable and pinned shepherd pool slots
 * for their full database duration.
 */
class ExecutionContextStatementRegistryTest {

    private static ExecutionContext root() {
        return ExecutionContext.root(Optional.empty(),
                ExecutionMetadata.of("test", "test", null, 0));
    }

    private static ExecutionContext grandChild(ExecutionContext root) {
        ExecutionMetadata metadata = ExecutionMetadata.of("child", "child", null, 0);
        return root.createChild(metadata, Optional.empty())
                .createChild(metadata, Optional.empty());
    }

    /** Red before root registration: the root's cancel found an empty list. */
    @Test
    void rootCancelReachesChildRegisteredStatements() throws Exception {
        ExecutionContext root = root();
        Statement stmt = mock(Statement.class);
        grandChild(root).registerStatement(new GuardedStatement(stmt));

        root.cancel();

        verify(stmt).cancel();
    }

    /**
     * The timeout path cancels on whichever CHILD noticed the deadline -
     * that cancel must reach the tree's statements too.
     */
    @Test
    void childCancelReachesSiblingRegisteredStatements() throws Exception {
        ExecutionContext root = root();
        ExecutionMetadata metadata = ExecutionMetadata.of("child", "child", null, 0);
        ExecutionContext childA = root.createChild(metadata, Optional.empty());
        ExecutionContext childB = root.createChild(metadata, Optional.empty());
        Statement stmt = mock(Statement.class);
        childA.registerStatement(new GuardedStatement(stmt));

        childB.cancel();

        verify(stmt).cancel();
    }

    /** Registration after cancel must not create an unkillable statement. */
    @Test
    void registerAfterCancelCancelsImmediately() throws Exception {
        ExecutionContext root = root();
        root.cancel();

        Statement stmt = mock(Statement.class);
        grandChild(root).registerStatement(new GuardedStatement(stmt));

        verify(stmt).cancel();
    }

    /** An unregistered (closed) statement is left alone by a later cancel. */
    @Test
    void unregisteredStatementIsNotCancelled() throws Exception {
        ExecutionContext root = root();
        Statement stmt = mock(Statement.class);
        GuardedStatement guard = new GuardedStatement(stmt);
        ExecutionContext child = grandChild(root);
        child.registerStatement(guard);
        guard.markClosed();
        child.unregisterStatement(guard);

        root.cancel();

        verify(stmt, never()).cancel();
    }
}
