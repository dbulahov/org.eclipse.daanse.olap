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
import java.util.Objects;
import java.util.SortedSet;

import org.eclipse.daanse.olap.common.Util;
import  org.eclipse.daanse.olap.util.ArraySortedSet;


/**
 * Constrained columns are part of the SegmentHeader and SegmentCache.
 * They uniquely identify a constrained column within a segment.
 * Each segment can have many constrained columns. Each column can
 * be constrained by multiple values at once (similar to a SQL in()
 * predicate).
 *
 * <p>They are immutable and serializable.
 */
public class SegmentColumn implements Serializable {
    // wire-format generation 2
    private static final long serialVersionUID = 2L;
    public final String columnExpression;
    public final long valueCount;
    public final SortedSet<Comparable> values;
    private final int hashCode;

    /**
     * Creates a SegmentColumn.
     *
     * @param columnExpression SQL expression for the column. Unique within the
     *     star schema, including accesses to the same physical column via
     *     different join paths.
     *
     * @param valueCount Number of distinct values of this column in the
     *     database. For these purposes, null is counted as a value. If there
     *     are N distinct values of the column, and we have a collection of
     *     segments that cover N values, then The engine assumes that it is safe
     *     to roll up.
     *
     * @param valueList List of values to constrain the
     *     column to, or null if unconstrained. Values must be
     *     {@link Comparable} and immutable. For example, Integer, Boolean,
     *     String or Double.
     */
    public SegmentColumn(
        String columnExpression,
        long valueCount,
        SortedSet<Comparable> valueList)
    {
        this.columnExpression = columnExpression;
        this.valueCount = valueCount;
        this.values = valueList;
        this.hashCode = computeHashCode();
    }

    private int computeHashCode() {
        return Util.hash(
            this.columnExpression.hashCode(),
            this.values);
    }


    /**
     * Returns the column expression of this constrained column.
     * @return A column expression.
     */
    public String getColumnExpression() {
        return columnExpression;
    }

    /**
     * Returns an array of predicate values for this column.
     * @return An array of object values.
     */
    public SortedSet<Comparable> getValues() {
        return values;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SegmentColumn that)) {
            return false;
        }
        // two wildcard columns are only equal over the same column expression
        return this.columnExpression.equals(that.columnExpression)
            && Objects.equals(this.values, that.values);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Returns the number of distinct values that occur for this column in the
     * database.
     *
     * <p>The engine uses this to know that it can safely combine multiple
     * segments to roll up. For example, if for the "quarter" column, one
     * segment has values {"Q1", "Q2"} and another has values {"Q3", "Q4"},
     * and Mondrian knows that there are 4 values, then it can roll up.
     *
     * <p>If this method returns a value that is too low, Mondrian may generate
     * incorrect results. If you don't know the number of values, return -1.</p>
     *
     * @return Number of distinct values, including null, that occur for this
     * column
     */
    public long getValueCount() {
        return valueCount;
    }
}
