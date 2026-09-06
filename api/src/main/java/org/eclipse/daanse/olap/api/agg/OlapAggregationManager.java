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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.api.agg;

import java.io.PrintWriter;

import org.eclipse.daanse.olap.api.cache.CacheControl;
import org.eclipse.daanse.olap.api.cache.OlapSegmentCacheManager;
import org.eclipse.daanse.olap.api.connection.Connection;

public interface OlapAggregationManager {

    void shutdown();

    OlapSegmentCacheManager getSegmentCacheManager(Connection connection);

    /** Removes and shuts down the session cache of the connection, if any. */
    void removeSegmentCacheManager(Connection connection);

    /**
     * The connection's session cache manager if one EXISTS, else the
     * shared manager - never creates one. Cancel/cleanup paths use this:
     * a query on an isolated session registered its segment requests in
     * the OVERLAY's index, and sweeping only the shared one left them
     * behind for the life of the overlay.
     */
    default OlapSegmentCacheManager peekSegmentCacheManager(Connection connection) {
        return getSegmentCacheManager();
    }

    CacheControl getCacheControl(Connection rolapConnection, PrintWriter pw);

    OlapSegmentCacheManager getSegmentCacheManager();

    /**
     * A context-free teardown for ORPHANED contexts (dropped without an
     * explicit shutdown): releases threads and in-JVM stores. CONTRACT:
     * the returned runnable must not capture the manager or anything
     * that transitively reaches the Context - it is held by a
     * {@link java.lang.ref.Cleaner} registered ON the context, and any
     * strong path back keeps the context reachable forever, so the
     * cleanup can never fire. The default is a no-op for managers
     * without own resources.
     */
    default Runnable orphanCleanup() {
        return () -> { };
    }

}
