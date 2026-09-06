/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
 * 
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.api.monitor.event;

public sealed interface CellCacheEvent extends ExecutionEvent
        permits CellCacheSegmentCreateEvent, CellCacheSegmentDeleteEvent {
    /**
     * Enumeration of sources of a cell cache segment.
     */
    public enum Source {
        /**
         * A segment that arrived from an external segment cache: another
         * instance published it, or an attached store announced it
         * (external-created events, star priming on attach).
         */
        EXTERNAL,

        /**
         * A segment that has been loaded in response to a user query, and populated by
         * generating and executing a SQL statement.
         */
        SQL,

        /**
         * a segment that has been loaded in response to a user query, and populated by
         * rolling up existing cache segments.
         */
        ROLLUP,

        /**
         * a segment that has been deleted by a call through the CacheControl API.
         */
        CACHE_CONTROL,
    }
}
