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
package org.eclipse.daanse.olap.spi;

/**
 * The one wire generation of the segment cache. It stamps the codec frame
 * and the segment id marker; bumping it deterministically invalidates every
 * stored entry (readers degrade foreign generations to a miss). The
 * serialVersionUIDs of the wire classes are per-class Java serialization
 * versions and deliberately not tied to this number.
 */
public final class SegmentWire {

    /** Bump on any change to keys, ids or the payload encoding. */
    public static final int GENERATION = 7;

    private SegmentWire() {
    }
}
