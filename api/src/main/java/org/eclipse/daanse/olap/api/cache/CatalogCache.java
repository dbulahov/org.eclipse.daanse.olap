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
*
*/
package org.eclipse.daanse.olap.api.cache;

public interface CatalogCache {

    void clear();

    /**
     * Snapshot of every catalog currently pooled (live and parked tiers).
     * The engine iterates this for cross-catalog work - segment-store
     * priming, checksum-wide event fan-out, stats capture - instead of
     * downcasting the cache to its implementation. Default empty for
     * caches without a pool - note that an implementation WITH pooled
     * catalogs must override this, or priming and event fan-out silently
     * see nothing.
     */
    default java.util.List<? extends org.eclipse.daanse.olap.api.element.Catalog> getCachedCatalogs() {
        return java.util.List.of();
    }

}
