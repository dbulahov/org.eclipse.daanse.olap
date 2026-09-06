 /*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */
package org.eclipse.daanse.olap.spi;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.util.ByteString;

/**
 * SPI definition of the segments cache.
 *
 * Lookups are performed using {@link SegmentHeader}s and
 * {@link SegmentBody}s. Both are immutable and fully serializable.
 *
 * Implementations register as OSGi services of this type (the engine binds
 * and unbinds them dynamically) or attach programmatically via the cache
 * manager. One cache instance may serve several engine instances at once;
 * the provider owns the cache lifecycle, the engine never calls
 * {@link #tearDown} on a cache it merely detaches.
 *
 * Implementations are expected to be thread-safe: multiple requests arrive
 * concurrently from different threads.
 *
 * Implementations must implement a time-out policy, if needed. The engine
 * calls the cache from worker threads and would otherwise wait forever; a
 * call that hangs should return null or throw so the segment can be loaded
 * another way.
 *
 * @author LBoudreau
 */
public interface SegmentCache {
    /**
     * Returns a SegmentBody once the
     * cache has returned any results, or null if no
     * segment corresponding to the header could be found.
     *
     * Cache implementations are at liberty to 'forget' segments. Therefore
     * it is allowable for this method to return null at any time
     *
     * @param header The header of the segment to find.
     * Consider this as a key.
     *
     * @return A SegmentBody, or <code>null</code>
     * if no corresponding segment could be found in cache.
     */
    SegmentBody get(SegmentHeader header);

    /**
     * Returns a list of all segments present in the cache.
     *
     * @return A List of segment headers describing the
     * contents of the cache.
     */
    List<SegmentHeader> getSegmentHeaders();

    /**
     * One star a cache holds segments for: the catalog checksum in its hex
     * form plus the fact table name — together the prefix of every segment
     * key of that star.
     */
    record StarKey(String schemaChecksum, String rolapStarFactTableName)
            implements Serializable {

        public static StarKey of(SegmentHeader header) {
            return new StarKey(header.schemaChecksum.toString(),
                    header.rolapStarFactTableName);
        }
    }

    /**
     * The stars this cache holds segments for. Attaching a cache asks THIS
     * instead of pulling the full header inventory over the wire; stores
     * answer it from their keys alone (a distinct scan or key-prefix walk),
     * never fetching headers or bodies. The default derives it from the
     * full listing, for stores without a cheaper answer.
     */
    default Set<StarKey> knownStars() {
        Set<StarKey> stars = new LinkedHashSet<>();
        for (SegmentHeader header : getSegmentHeaders()) {
            stars.add(StarKey.of(header));
        }
        return stars;
    }

    /**
     * Returns the headers of one star's segments: those whose schema
     * checksum and fact table match. The default filters the full listing;
     * stores whose keys carry both values answer with a prefix match
     * instead of shipping the whole inventory.
     *
     * @param schemaChecksum the catalog content checksum
     * @param rolapStarFactTableName the star's fact table alias
     * @return matching headers
     */
    default List<SegmentHeader> getSegmentHeaders(
            ByteString schemaChecksum,
            String rolapStarFactTableName) {
        return getSegmentHeaders().stream()
                .filter(header -> header.schemaChecksum.equals(schemaChecksum)
                        && header.rolapStarFactTableName.equals(rolapStarFactTableName))
                .toList();
    }

    /**
     * Moves a segment to a new header carrying the identical body — the
     * flush-constrain path shrinks a header without touching the cells.
     * The default copies through get/remove/put; stores with key-level
     * rename skip the body transfer.
     *
     * @param oldHeader the header the body is stored under
     * @param newHeader the header it moves to
     * @return whether the body actually moved - false when the store held
     *         no body under the old header, or the target was already taken
     */
    default boolean rename(SegmentHeader oldHeader, SegmentHeader newHeader) {
        final SegmentBody body = get(oldHeader);
        final boolean existed = remove(oldHeader);
        if (body != null) {
            put(newHeader, body);
        }
        // success means the body MOVED. Reporting a bodiless removal (the
        // body was evicted between get and remove) as true made the caller
        // publish the birth of a header no store holds.
        return existed && body != null;
    }

    /**
     * Stores a segment data in the cache.
     *
     * @return Whether the cache write succeeded
     * @param header The header of the segment.
     * @param body The segment body to cache.
     */
    boolean put(SegmentHeader header, SegmentBody body);

    /**
     * Removes a segment from the cache.
     *
     * @param header The header of the segment we want to remove.
     *
     * @return True if the segment was found and removed,
     * false otherwise.
     */
    boolean remove(SegmentHeader header);

    /**
     * Closes this cache instance and releases its resources (connections,
     * listeners, local buffers). A shared backing store keeps its entries —
     * other instances on the same store continue to serve them. Idempotent;
     * after tearDown every read is a miss and every write returns false.
     */
    void tearDown();

    /**
     * Adds a listener to this segment cache implementation.
     * The listener will get notified via
     * {@link SegmentCacheListener.SegmentCacheEvent} instances.
     *
     * @param listener The listener to attach to this cache.
     */
    void addListener(SegmentCacheListener listener);

    /**
     * Unregisters a listener from this segment cache implementation.
     *
     * @param listener The listener to remove.
     */
    void removeListener(SegmentCacheListener listener);


    /**
     * {@link SegmentCacheListener} objects are used to listen
     * to the state of the cache and be notified of changes to its
     * state or its entries.
     *
     * A cache fires its own put/remove synchronously with
     * {@code isLocal() == true}; changes made by other nodes or third
     * parties arrive as foreign events with {@code isLocal() == false},
     * and the own-node echo of a distributed store is filtered out. The
     * engine ignores local events (it made the change itself) and applies
     * foreign ones to its index.
     */
    interface SegmentCacheListener {
        /**
         * Handle an event
         * @param e Event to handle.
         */
        void handle(SegmentCacheEvent e);

        /**
         * Defines the event types that a listener can look for.
         */
        interface SegmentCacheEvent {
            /**
             * Defined the possible types of events used by
             * the {@link SegmentCacheListener} class.
             */
            enum EventType {
                /**
                 * An Entry was created in cache.
                 */
                ENTRY_CREATED,
                /**
                 * An entry was deleted from the cache.
                 */
                ENTRY_DELETED
            }

            /**
             * Returns the event type of the current SegmentCacheEvent
             * instance.
             */
            EventType getEventType();

            /**
             * Returns the segment header at the source of the event.
             */
            SegmentHeader getSource();

            /**
             * True for an event fired by this cache instance's own
             * put/remove; false for a change made by a remote node or a
             * third party. The engine ignores local events.
             */
            boolean isLocal();
        }
    }

}
