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
package org.eclipse.daanse.olap.api.execution;

import java.util.Objects;

/**
 * Ownership handshake around a JDBC statement between the thread that runs
 * and closes it (the loader) and the threads that may cancel it (execution
 * cancel, timeout, segment index cancel).
 *
 * <p>
 * The race this closes: a raw {@code java.sql.Statement} reference captured
 * for a later cancel can outlive the statement - the loader closes it and
 * returns the pooled connection, and a {@code cancel()} on the dead
 * reference reaches whatever query the pool gave that connection next
 * (drivers that cancel by connection id kill a foreign query). An
 * {@code isClosed()} pre-check is no defense: drivers synchronize it with
 * the running query, so the check blocks until the query completes.
 * </p>
 *
 * <p>
 * The guard's monitor serializes the two sides instead:
 * <ul>
 * <li>{@link #cancel()} issues at most ONE JDBC cancel, and none at all
 * once {@link #markClosed()} ran - after that it is a pure no-op with no
 * JDBC call, so a recycled connection is unreachable.</li>
 * <li>{@link #markClosed()} must be called by the owning thread BEFORE the
 * statement (or its connection) is actually closed. It may block for one
 * in-flight cancel round-trip (bounded by the driver's cancel timeout);
 * the cancel side never waits for the closer, so there is no cycle.</li>
 * </ul>
 */
public final class GuardedStatement {

    private final java.sql.Statement statement;
    private boolean closed;
    private boolean cancelled;

    public GuardedStatement(java.sql.Statement statement) {
        this.statement = Objects.requireNonNull(statement, "statement");
    }

    /**
     * Cancels the statement once, best-effort. A no-op after
     * {@link #markClosed()} (no JDBC call) and after a previous cancel.
     * This is a network round-trip on most drivers - call it off any
     * latency-sensitive thread.
     */
    public void cancel() {
        synchronized (this) {
            if (closed || cancelled) {
                return;
            }
            cancelled = true;
            try {
                statement.cancel();
            } catch (Throwable crush) {
                // Cancel is best-effort: many drivers complain when cancel
                // races completion, and some (Hive) even throw
                // OutOfMemoryError on canceled queries. Never let that
                // escape into the canceling thread.
            }
        }
    }

    /**
     * Marks the statement closed. Call BEFORE actually closing the
     * statement or its connection; afterwards {@link #cancel()} is a
     * guaranteed no-op. Waits for an in-flight cancel to finish (bounded
     * by the driver's cancel round-trip). Idempotent.
     */
    public void markClosed() {
        synchronized (this) {
            closed = true;
        }
    }
}
