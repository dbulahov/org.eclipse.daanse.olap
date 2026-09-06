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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.daanse.olap.api.agg.OlapAggregationManager;
import org.eclipse.daanse.olap.api.cache.CacheCommand;
import org.eclipse.daanse.olap.api.cache.OlapSegmentCacheIndexRegistry;
import org.eclipse.daanse.olap.api.cache.OlapSegmentCacheManager;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.core.AbstractBasicContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * A query on an isolated session (pending writeback / session caching)
 * registers its segment requests in the OVERLAY's index. The
 * end-of-query sweep must cancel them there too - it used to resolve
 * the manager with a null connection, always sweeping only the shared
 * registry, so overlay registrations (and any linked SQL statement)
 * survived for the life of the overlay.
 */
class ExecutionSegmentSweepTest {

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void theSweepReachesSharedAndSessionRegistries() {
        OlapSegmentCacheIndexRegistry sharedRegistry =
            mock(OlapSegmentCacheIndexRegistry.class);
        OlapSegmentCacheManager shared = mock(OlapSegmentCacheManager.class);
        when(shared.getIndexRegistry()).thenReturn(sharedRegistry);
        // the actor: run the command synchronously
        when(shared.execute(any())).thenAnswer(inv -> {
            try {
                return ((CacheCommand) inv.getArgument(0)).call();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        OlapSegmentCacheIndexRegistry overlayRegistry =
            mock(OlapSegmentCacheIndexRegistry.class);
        OlapSegmentCacheManager overlay = mock(OlapSegmentCacheManager.class);
        when(overlay.getIndexRegistry()).thenReturn(overlayRegistry);

        OlapAggregationManager aggregationManager = mock(OlapAggregationManager.class);
        when(aggregationManager.getSegmentCacheManager()).thenReturn(shared);
        when(aggregationManager.peekSegmentCacheManager(any())).thenReturn(overlay);

        AbstractBasicContext context = mock(AbstractBasicContext.class);
        when(context.getAggregationManager()).thenReturn(aggregationManager);
        Connection connection = mock(Connection.class);
        when(connection.getContext()).thenReturn((org.eclipse.daanse.olap.api.Context) context);
        AbstractStatement statement =
            mock(AbstractStatement.class, Mockito.RETURNS_DEEP_STUBS);
        when(statement.getConnection()).thenReturn(connection);

        ExecutionImpl execution = new ExecutionImpl(statement, Optional.empty());
        execution.unregisterSegmentRequests();

        verify(sharedRegistry).cancelExecutionSegments(execution);
        verify(overlayRegistry)
            .cancelExecutionSegments(execution);
    }
}
