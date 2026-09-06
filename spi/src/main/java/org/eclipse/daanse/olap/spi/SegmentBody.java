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
import java.util.BitSet;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.daanse.olap.key.CellKey;

/**
 * SegmentBody is the object which contains the cached data of a
 * Segment. They are stored inside a {@link org.eclipse.daanse.olap.spi.SegmentCache}
 * and can be retrieved by a {@link SegmentHeader} key.
 *
 * The segment body objects are immutable and fully serializable.
 *
 * @author LBoudreau
 */
public interface SegmentBody extends Serializable {
    /**
     * Converts contents of this segment into a cellkey/value map. Use only
     * for sparse segments.
     *
     * @return Map containing cell values keyed by their coordinates
     */
    Map<CellKey, Object> getValueMap();

    /**
     * Number of populated cells. Implementations count without
     * materializing the value map; counters and cache weighers use this.
     *
     * @return number of populated cells
     */
    default int cellCount() {
        return getValueMap().size();
    }

    /**
     * Returns an array of values.
     *
     * Use only for dense segments.
     *
     * @return An array of values
     */
    Object getValueArray();

    /**
     * Returns a bit-set indicating whether values are null. The ordinals in
     * the bit-set correspond to the indexes in the array returned from
     * {@link #getValueArray()}.
     *
     * <p>Use only for dense segments of native values.</p>
     *
     * @return Indicators
     */
    BitSet getNullValueIndicators();

    /**
     * Returns the cached axis value sets to be used as an
     * initializer for the segment's axis.
     *
     * @return An array of SortedSets which was cached previously.
     */
    SortedSet<Comparable>[] getAxisValueSets();

    /**
     * Returns an array of boolean values which identify which
     * axis of the cached segment contained null values.
     *
     * @return An array of boolean values.
     */
    boolean[] getNullAxisFlags();
}
