/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;

/**
 * Execution context using JDK 25 ScopedValues for thread-safe, immutable
 * context propagation.
 *
 * <p>
 * Replaces the ThreadLocal-based Locus/LocusImpl system with modern ScopedValue
 * approach. This provides better performance, memory characteristics, and
 * clearer lifecycle management compared to ThreadLocal.
 * </p>
 *
 * <p>
 * ExecutionContext tracks the state of a query execution including:
 * <ul>
 * <li>Absolute deadline (null = unlimited), resolved once at the root</li>
 * <li>Execution state (RUNNING, CANCELED, TIMEOUT), read and marked on the
 * root of the tree</li>
 * <li>Parent execution for nested queries</li>
 * <li>SQL statements registered for cancellation (held on the root context of the tree)</li>
 * <li>Metadata for tracing and monitoring</li>
 * </ul>
 *
 * <p>
 * Usage example:
 *
 * <pre>
 * // Create root context with metadata; an empty timeout means UNLIMITED
 * ExecutionMetadata rootMetadata = ExecutionMetadata.of("QueryExecution", "MDX Query", null, 0);
 * ExecutionContext ctx = ExecutionContext.root(Optional.of(Duration.ofMinutes(5)), rootMetadata);
 *
 * ExecutionContext.where(ctx, () -> {
 *     // Your code here has access to ctx via ExecutionContext.current()
 *     ExecutionContext current = ExecutionContext.current();
 *     current.checkCancelOrTimeout();
 *
 *     // Create child context inheriting the parent's ABSOLUTE deadline
 *     // (the clock does not restart per child)
 *     ExecutionMetadata childMetadata = ExecutionMetadata.of("Component", "message", null, 0);
 *     ExecutionContext child1 = current.createChild(childMetadata, Optional.empty());
 *
 *     // An explicit child timeout only TIGHTENS the budget - a longer one
 *     // is silently clamped to the parent's deadline
 *     ExecutionMetadata fastMetadata = ExecutionMetadata.of("FastOp", "quick operation", null, 0);
 *     ExecutionContext child2 = current.createChild(fastMetadata, Optional.of(Duration.ofSeconds(30)));
 *
 *     return performQuery();
 * });
 * </pre>
 *
 * @see ScopedValue
 */
public final class ExecutionContext {
    private static final ScopedValue<ExecutionContext> CURRENT = ScopedValue.newInstance();

    // Core state
    // Absolute deadline of this context; null = unlimited. Resolved ONCE:
    // children inherit the parent's deadline verbatim (the budget is per
    // query tree - a child restarting the clock stretched it arbitrarily);
    // an explicit child timeout may only tighten it.
    private final Instant deadline;
    // whether the deadline is the tree-wide (root/inherited) budget - a
    // child-tightened budget fails only the child on elapse
    private final boolean treeDeadline;
    private final AtomicReference<State> state;

    // Hierarchy
    private final ExecutionContext parent;

    // SQL statements for cancellation. Registration and cancellation both
    // resolve to the ABSOLUTE ROOT context's list: almost all SQL runs on
    // child contexts (member loads, tuple reads, statistics, drillthrough),
    // and a cancel that only walked its own list reached none of them.
    private final List<GuardedStatement> sqlStatements;

    // Execution reference (for compatibility with legacy code during migration)
    private Execution execution;

    // Metadata for tracing, monitoring, and debugging (OpenTelemetry support)
    private final ExecutionMetadata metadata;

    /**
     * Unified constructor for both root and child contexts.
     *
     * @param parent   the parent context (null for root contexts)
     * @param metadata the metadata (defaults to empty if null)
     * @param timeout  empty inherits the parent's absolute deadline (root:
     *                 unlimited); a present value resolves to
     *                 min(now + timeout, parent deadline)
     */
    private ExecutionContext(ExecutionContext parent, ExecutionMetadata metadata, Optional<Duration> timeout) {
        this.parent = parent;
        this.state = new AtomicReference<>(State.RUNNING);
        // only the ROOT context ever holds statements (registration walks to
        // it); children get an immutable empty list so nothing can bypass
        // the root registry by touching a child's list directly
        this.sqlStatements = parent == null
                ? Collections.synchronizedList(new ArrayList<>())
                : List.of();

        // Deadline: an EMPTY timeout means unlimited (executeDuration <= 0
        // maps to empty - it must never silently become a default budget);
        // children inherit the parent's absolute deadline; an explicit
        // child timeout only tightens, never extends past the parent.
        // treeDeadline records WHOSE budget this is: the root's/inherited
        // one times the whole tree out, a child's own tightened budget
        // fails only the child (see checkCancelOrTimeout).
        Instant inherited = parent == null ? null : parent.deadline;
        if (timeout.isPresent()) {
            Instant own = Instant.now().plus(timeout.get());
            if (inherited != null && inherited.isBefore(own)) {
                this.deadline = inherited;
                this.treeDeadline = true;
            } else {
                this.deadline = own;
                this.treeDeadline = parent == null;
            }
        } else {
            this.deadline = inherited;
            this.treeDeadline = true;
        }

        // Metadata: explicit > empty
        this.metadata = metadata != null ? metadata : ExecutionMetadata.empty();
    }

    /**
     * Creates a root execution context with the specified timeout and metadata.
     *
     * @param timeout  the timeout duration; EMPTY means unlimited
     *                 (executeDuration <= 0 maps to empty and must never
     *                 become a default budget)
     * @param metadata the metadata for the root context
     * @return a new root ExecutionContext
     */
    public static ExecutionContext root(Optional<Duration> timeout, ExecutionMetadata metadata) {
        return new ExecutionContext(null, Objects.requireNonNull(metadata, "metadata"), timeout);
    }

    /**
     * Returns the current execution context.
     *
     * @return the current ExecutionContext
     * @throws NoExecutionContextException if no context is bound to the current
     *                                     scope
     */
    public static ExecutionContext current() {
        if (!CURRENT.isBound()) {
            throw new NoExecutionContextException();
        }
        return CURRENT.get();
    }

    /**
     * Returns the current execution context, or null if not in execution scope. Use
     * this method only when you explicitly need to handle the case where no context
     * is available.
     *
     * @return the current ExecutionContext, or null if outside execution scope
     */
    public static ExecutionContext currentOrNull() {
        return CURRENT.isBound() ? CURRENT.get() : null;
    }


    /**
     * Executes the given task within the scope of this execution context. The
     * context will be available via {@link #current()} during task execution.
     *
     * @param ctx  the execution context to bind
     * @param task the task to execute
     * @param <R>  the result type
     * @param <X>  the exception type
     * @return the result of the task
     * @throws X if the task throws an exception
     */
    public static <R, X extends Throwable> R where(ExecutionContext ctx, CallableTask<R, X> task) throws X {
        return ScopedValue.where(CURRENT, ctx).call(() -> task.call());
    }

    /**
     * Executes the given task within the scope of this execution context. The
     * context will be available via {@link #current()} during task execution.
     *
     * @param ctx  the execution context to bind
     * @param task the task to execute
     */
    public static void where(ExecutionContext ctx, Runnable task) {
        ScopedValue.where(CURRENT, ctx).run(task);
    }

    /**
     * Functional interface for tasks that can throw checked exceptions.
     *
     * @param <R> the result type
     * @param <X> the exception type
     */
    @FunctionalInterface
    public interface CallableTask<R, X extends Throwable> {
        /**
         * Executes the task.
         *
         * @return the result
         * @throws X if an error occurs
         */
        R call() throws X;
    }

    /**
     * Checks if this execution has been canceled or timed out. Throws an exception
     * if the execution has been canceled or exceeded its timeout.
     *
     * <p>State is read and marked on the ROOT, so a timeout noticed on one
     * child is visible to every sibling and to later statement
     * registrations; the first observer of an elapsed deadline CAS-marks
     * TIMEOUT and cancels the tree's registered statements.</p>
     *
     * @throws QueryCanceledException if the execution was canceled
     * @throws QueryTimeoutException  if the execution has timed out
     */
    public void checkCancelOrTimeout() {
        // state lives on the ROOT: a timeout noticed on one child must be
        // visible to every sibling (and to registerStatement) - previously
        // each child had its own state and a query kept issuing SQL after
        // it "timed out"
        final ExecutionContext root = rootContext();
        State currentState = root.state.get();

        if (currentState == State.CANCELED) {
            throw new QueryCanceledException("Query canceled");
        }

        if (currentState == State.TIMEOUT) {
            throw new QueryTimeoutException("Query timeout");
        }

        // Check timeout; null deadline = unlimited
        if (deadline != null && Instant.now().isAfter(deadline)) {
            // only the ROOT'S budget (or the inherited copy of it) times
            // the whole tree out. A child's own TIGHTENED budget fails
            // just that child: a 30s statistics probe inside a 5-minute
            // query must not CAS the root to TIMEOUT and cancel every
            // sibling's SQL with most of the real budget left.
            if (treeDeadline
                    && root.state.compareAndSet(State.RUNNING, State.TIMEOUT)) {
                cancelRegisteredStatements();
            }
            throw new QueryTimeoutException("Query timeout");
        }
    }

    /**
     * Cancels this execution and all SQL statements registered anywhere in
     * this context tree. Statements live on the root context's list (see
     * {@link #registerStatement}), so canceling a child - the timeout path
     * calls {@code this.cancel()} on whichever context noticed - reaches
     * them all. Idempotent; a closed statement's guard is inert.
     */
    public void cancel() {
        // CAS: a TIMEOUT already recorded stays a TIMEOUT - cancel() runs
        // as the timeout's own side effect, and overwriting the state made
        // the same event report as a timeout to the first poller and as a
        // cancel to every later one
        rootContext().state.compareAndSet(State.RUNNING, State.CANCELED);
        cancelRegisteredStatements();
    }

    /**
     * Cancels every registered statement of the tree. Snapshot, then
     * cancel outside the list lock: a cancel is a JDBC network round-trip
     * per statement, and register/unregister must not queue behind it.
     * Guards are idempotent, so racing an unregister is harmless.
     */
    private void cancelRegisteredStatements() {
        final List<GuardedStatement> list = rootContext().sqlStatements;
        final List<GuardedStatement> snapshot;
        synchronized (list) {
            snapshot = new ArrayList<>(list);
        }
        for (GuardedStatement stmt : snapshot) {
            stmt.cancel();
        }
    }

    /**
     * Registers a SQL statement for automatic cancellation when this
     * execution tree is canceled. The statement is stored on the ABSOLUTE
     * ROOT context: cancellation granularity is the whole execution tree
     * (matching {@code Execution.cancelSqlStatements()}, which already
     * cascades to parent executions). If the tree is already canceled or
     * timed out, the statement is canceled immediately - registration
     * after cancel must not create an unkillable statement.
     *
     * @param stmt the guarded SQL statement to register
     * @throws NullPointerException if stmt is null
     */
    public void registerStatement(GuardedStatement stmt) {
        Objects.requireNonNull(stmt, "statement");
        final ExecutionContext root = rootContext();
        root.sqlStatements.add(stmt);
        if (treeCancelOrTimeout(root)) {
            stmt.cancel();
        }
    }

    /**
     * Removes a statement registered with {@link #registerStatement} - the
     * statement's owner calls this on close, so the root list does not grow
     * for the lifetime of a long execution. Safe to call for a statement
     * that was never registered.
     */
    public void unregisterStatement(GuardedStatement stmt) {
        Objects.requireNonNull(stmt, "statement");
        rootContext().sqlStatements.remove(stmt);
    }

    private ExecutionContext rootContext() {
        ExecutionContext c = this;
        while (c.parent != null) {
            c = c.parent;
        }
        return c;
    }

    /** Non-throwing: is this tree canceled or timed out already? */
    private boolean treeCancelOrTimeout(ExecutionContext root) {
        State rootState = root.state.get();
        if (rootState == State.CANCELED || rootState == State.TIMEOUT) {
            return true;
        }
        // an elapsed deadline nobody polled yet still gates registration
        if (deadline != null && Instant.now().isAfter(deadline)) {
            return true;
        }
        // the Execution may have flipped its state before propagating it
        // into the context (ExecutionImpl.cancel sets state first)
        Execution ex = getExecution();
        return ex != null && ex.isCancelOrTimeout();
    }

    /**
     * Returns the Execution object associated with this context. This is provided
     * for compatibility with legacy code during migration. If this context doesn't
     * have an execution, walks up the parent chain.
     *
     * @return the Execution object, or null if not set in this context or any
     *         parent
     */
    public Execution getExecution() {
        if (execution != null) {
            return execution;
        }
        // Walk up the parent chain to find an execution
        if (parent != null) {
            return parent.getExecution();
        }
        return null;
    }

    /**
     * Sets the Execution object for this context. This is called by ExecutionImpl
     * during initialization.
     *
     * @param execution the Execution object
     */
    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    /**
     * Returns the Context this execution belongs to, or null if there is none -
     * during catalog build or in standalone tools, for instance.
     *
     * @return the Context, or null outside a statement
     */
    public Context<?> getContext() {
        Execution ex = getExecution();
        if (ex == null) {
            return null;
        }
        var stmt = ex.getDaanseStatement();
        if (stmt == null) {
            return null;
        }
        Connection connection = stmt.getDaanseConnection();
        return connection == null ? null : connection.getContext();
    }

    /**
     * Returns the metadata associated with this execution context. The metadata
     * contains component name, message, purpose, and cell request count used for
     * tracing, monitoring, and debugging (OpenTelemetry support).
     *
     * @return the ExecutionMetadata, never null
     */
    public ExecutionMetadata metadata() {
        return metadata;
    }

    /**
     * Creates a child execution context with new metadata and custom timeout.
     *
     * @param metadata the metadata for the child context
     * @param timeout  the timeout for the child context (empty inherits the
     *                 parent's absolute deadline; a present value only
     *                 tightens, never extends past the parent)
     * @return a new child ExecutionContext with the specified metadata and timeout
     */
    public ExecutionContext createChild(ExecutionMetadata metadata, Optional<Duration> timeout) {
        return new ExecutionContext(this, metadata, timeout);
    }

}
