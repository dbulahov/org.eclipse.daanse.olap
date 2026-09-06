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

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * One excluded region of a segment: a box in cell space, spanning one or
 * more constrained columns. A cell lies in the region only when it matches
 * EVERY column of the box — a flush of (year 2025 x region West) excludes
 * exactly that combination, not each value on its own axis.
 */
public record SegmentRegion(List<SegmentColumn> columns) implements Serializable {

    private static final long serialVersionUID = 1L;

    public SegmentRegion {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("a region spans at least one column");
        }
        columns = columns.stream()
                .sorted(Comparator.comparing(c -> c.columnExpression))
                .toList();
    }

    /**
     * Whether the coordinates lie inside this box. A coordinate missing for
     * one of the box columns counts as inside — the caller cannot prove the
     * cell is outside, and treating it as excluded only causes a reload.
     */
    public boolean contains(Map<String, Comparable> coordinates) {
        for (SegmentColumn column : columns) {
            if (!coordinates.containsKey(column.columnExpression)) {
                continue;
            }
            Comparable value = coordinates.get(column.columnExpression);
            if (column.values != null && !column.values.contains(value)) {
                return false;
            }
        }
        return true;
    }
}
