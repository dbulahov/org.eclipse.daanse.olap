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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Statement;
import java.time.Duration;
import java.util.Optional;

import org.eclipse.daanse.olap.api.execution.ExecutionContext;
import org.eclipse.daanse.olap.api.execution.ExecutionMetadata;
import org.eclipse.daanse.olap.api.execution.GuardedStatement;
import org.eclipse.daanse.olap.api.execution.QueryCanceledException;
import org.eclipse.daanse.olap.api.execution.QueryTimeoutException;
import org.junit.jupiter.api.Test;

/**
 * The context deadline is resolved ONCE at the root and shared by the
 * whole tree: an empty timeout means unlimited (executeDuration <= 0 -
 * it must never silently become a default budget), children inherit the
 * root's absolute deadline instead of restarting the clock, and a
 * timeout noticed anywhere marks the ROOT so siblings and late
 * registrations see it.
 */
class ExecutionContextDeadlineTest {

    private static ExecutionMetadata meta() {
        return ExecutionMetadata.of("t", "t", null, 0);
    }

    /** Red before: empty timeout silently became a five-minute budget. */
    @Test
    void emptyTimeoutMeansUnlimited() {
        ExecutionContext root = ExecutionContext.root(Optional.empty(), meta());
        assertThatCode(root::checkCancelOrTimeout).doesNotThrowAnyException();
    }

    /**
     * Red before: a child's own TIGHTENED budget CAS'd the ROOT to
     * TIMEOUT and cancelled the whole tree's statements - a 30s
     * statistics probe killed a 5-minute query with the real budget
     * nearly untouched.
     */
    @Test
    void aChildsTightenedBudgetFailsOnlyTheChild() throws Exception {
        ExecutionContext root = ExecutionContext.root(
            Optional.of(Duration.ofMinutes(5)), meta());
        ExecutionContext probe = root.createChild(meta(),
            Optional.of(Duration.ofMillis(30)));
        ExecutionContext sibling = root.createChild(meta(), Optional.empty());
        Thread.sleep(60);

        assertThatThrownBy(probe::checkCancelOrTimeout)
            .isInstanceOf(QueryTimeoutException.class);
        // the tree survives the probe's timeout
        assertThatCode(root::checkCancelOrTimeout).doesNotThrowAnyException();
        assertThatCode(sibling::checkCancelOrTimeout).doesNotThrowAnyException();
    }

    /** Red before: each child restarted the clock, stretching the budget. */
    @Test
    void childrenInheritTheRootDeadline() throws Exception {
        ExecutionContext root =
            ExecutionContext.root(Optional.of(Duration.ofMillis(50)), meta());
        Thread.sleep(80);
        ExecutionContext child = root.createChild(meta(), Optional.empty());

        assertThatThrownBy(child::checkCancelOrTimeout)
            .isInstanceOf(QueryTimeoutException.class);
    }

    /**
     * Red before: a timeout noticed on child A set only A's state; a
     * statement registered on sibling B afterwards escaped cancellation
     * and the query kept issuing SQL after its own timeout.
     */
    @Test
    void timeoutNoticedOnOneChildGatesSiblingRegistrations() throws Exception {
        ExecutionContext root =
            ExecutionContext.root(Optional.of(Duration.ofMillis(50)), meta());
        ExecutionContext childA = root.createChild(meta(), Optional.empty());
        ExecutionContext childB = root.createChild(meta(), Optional.empty());
        Thread.sleep(80);
        assertThatThrownBy(childA::checkCancelOrTimeout)
            .isInstanceOf(QueryTimeoutException.class);

        Statement stmt = mock(Statement.class);
        childB.registerStatement(new GuardedStatement(stmt));

        verify(stmt).cancel();
    }

    /**
     * Red before: cancel() - which runs as the timeout's own side effect -
     * overwrote TIMEOUT with CANCELED, so the same event reported as a
     * timeout to the first poller and as a cancel to every later one.
     */
    @Test
    void timeoutIsNotRewrittenIntoACancel() throws Exception {
        ExecutionContext root =
            ExecutionContext.root(Optional.of(Duration.ofMillis(50)), meta());
        Thread.sleep(80);
        assertThatThrownBy(root::checkCancelOrTimeout)
            .isInstanceOf(QueryTimeoutException.class);

        root.cancel();

        assertThatThrownBy(root::checkCancelOrTimeout)
            .isInstanceOf(QueryTimeoutException.class);
    }

    /** A plain cancel still reports as a cancel. */
    @Test
    void cancelReportsAsCancel() {
        ExecutionContext root = ExecutionContext.root(Optional.empty(), meta());
        root.cancel();
        assertThatThrownBy(root::checkCancelOrTimeout)
            .isInstanceOf(QueryCanceledException.class);
    }

    /** An explicit child timeout tightens but never extends the budget. */
    @Test
    void childTimeoutNeverExtendsPastTheRoot() throws Exception {
        ExecutionContext root =
            ExecutionContext.root(Optional.of(Duration.ofMillis(50)), meta());
        ExecutionContext child =
            root.createChild(meta(), Optional.of(Duration.ofMinutes(10)));
        Thread.sleep(80);

        assertThatThrownBy(child::checkCancelOrTimeout)
            .isInstanceOf(QueryTimeoutException.class);

        // and a registration on the elapsed tree cancels immediately even
        // though nobody marked the state yet
        ExecutionContext fresh =
            ExecutionContext.root(Optional.of(Duration.ofMillis(50)), meta());
        Thread.sleep(80);
        Statement stmt = mock(Statement.class);
        fresh.registerStatement(new GuardedStatement(stmt));
        verify(stmt).cancel();
    }
}
