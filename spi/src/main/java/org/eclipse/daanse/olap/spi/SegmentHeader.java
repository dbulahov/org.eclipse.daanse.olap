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
import java.util.SortedSet;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.key.BitKey;
import org.eclipse.daanse.olap.util.ByteString;

/**
 * SegmentHeaders are the key objects used to retrieve the segments
 * from the segment cache.
 *
 * The segment header objects are immutable and fully serializable.
 *
 * Each header has an ID: a SHA-256 digest over schema name and checksum,
 * cube, measure, the sorted constrained columns with their values, the
 * sorted excluded regions and the canonical form of the typed compound
 * predicates. See {@link SegmentHeader#getUniqueID()}.
 *
 * @author LBoudreau
 */
public class SegmentHeader implements Serializable {
    // Java-serialization stream id (the TYPE_JAVA fallback only); the
    // primary wire form is versioned by SegmentWire.GENERATION
    private static final long serialVersionUID = 4L;
    private final SegmentIdentity identity;
    private final int arity;
    private final List<SegmentColumn> constrainedColumns;
    private final List<SegmentRegion> excludedRegions;
    // aliases of the identity's components
    public final List<SegmentPredicate> compoundPredicates;
    public final String measureName;
    public final String cubeName;
    public final String schemaName;
    public final String rolapStarFactTableName;
    public final BitKey constrainedColsBitKey;
    private final int hashCode;
    // lazily materialized; never part of the wire form. Volatile: headers
    // are shared across threads, the race must publish a fully built value
    private transient volatile ByteString uniqueID;
    private transient volatile Map<String, SegmentColumn> columnsByExpression;
    public final ByteString schemaChecksum;

    /**
     * Creates a segment header.
     *
     * @param schemaName The name of the schema which this
     * header belongs to.
     * @param schemaChecksum Schema checksum
     * @param cubeName The name of the cube this segment belongs to.
     * @param measureName The name of the measure which defines
     * this header.
     * @param constrainedColumns An array of constrained columns
     * objects which define the predicated of this segment header.
     * @param compoundPredicates Compound predicates (Must not be null, but
     * typically empty.)
     * @param rolapStarFactTableName Star fact table name
     * @param constrainedColsBitKey Constrained columns bit key
     * @param excludedRegions Excluded regions. (Must not be null, but typically
     */
    public SegmentHeader(
        String schemaName,
        ByteString schemaChecksum,
        String cubeName,
        String measureName,
        List<SegmentColumn> constrainedColumns,
        List<SegmentPredicate> compoundPredicates,
        String rolapStarFactTableName,
        BitKey constrainedColsBitKey,
        List<SegmentRegion> excludedRegions)
    {
        assert schemaChecksum != null;
        this.identity = new SegmentIdentity(schemaName, schemaChecksum, cubeName,
                rolapStarFactTableName, measureName, compoundPredicates, constrainedColsBitKey);
        this.constrainedColumns = List.copyOf(constrainedColumns);
        this.excludedRegions = List.copyOf(excludedRegions);
        this.schemaName = identity.schemaName();
        this.schemaChecksum = identity.schemaChecksum();
        this.cubeName = identity.cubeName();
        this.measureName = identity.measureName();
        this.compoundPredicates = identity.compoundPredicates();
        this.rolapStarFactTableName = identity.rolapStarFactTableName();
        this.constrainedColsBitKey = identity.constrainedColsBitKey();
        this.arity = constrainedColumns.size();
        // Hash code might be used extensively. Better compute
        // it up front.
        this.hashCode = computeHashCode();
    }

    private int computeHashCode() {
        // order-independent over the columns (a sum of their cached
        // hashes): equality is defined over the sorted digest, which
        // ignores column order too — and the constructor needs no sorted
        // copies or value arrays just to hash
        int hash = 42;
        hash = Util.hash(hash, schemaName);
        hash = Util.hash(hash, schemaChecksum);
        hash = Util.hash(hash, cubeName);
        hash = Util.hash(hash, rolapStarFactTableName);
        hash = Util.hash(hash, measureName);
        int columnsHash = 0;
        for (SegmentColumn col : constrainedColumns) {
            columnsHash += col.hashCode();
        }
        for (SegmentRegion region : excludedRegions) {
            for (SegmentColumn col : region.columns()) {
                columnsHash += col.hashCode();
            }
        }
        hash = Util.hash(hash, columnsHash);
        hash = Util.hash(hash, compoundPredicates);
        return hash;
    }

    @Override
	public int hashCode() {
        return hashCode;
    }

    @Override
	public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // the id digests the full identity; equal ids mean equal headers
        return obj instanceof SegmentHeader that
            && hashCode == that.hashCode
            && getUniqueID().equals(that.getUniqueID());
    }

    /**
     * Checks if this header can be constrained by a given region box.
     *
     * <p>Returns false if the projection of the box onto this header's
     * constrained columns covers every axis it touches in its entirety —
     * the whole segment is inside the flushed region and must be scrapped.
     * Also returns false if the box touches none of the constrained
     * columns with a value restriction.
     *
     * A {@code true} answer permits {@link #constrain}, but does not
     * guarantee a different header: an already-excluded box returns the
     * receiver unchanged.
     */
    public boolean canConstrain(SegmentColumn[] region) {
        boolean atLeastOnePresent = false;
        boolean everyPresentAxisCovered = true;
        for (SegmentColumn ccToFlush : region) {
            SegmentColumn ccActual =
                getConstrainedColumn(ccToFlush.columnExpression);
            if (ccActual == null) {
                continue;
            }
            boolean coversAxis = ccToFlush.values == null
                || (ccActual.values != null && ccToFlush.values.containsAll(ccActual.values));
            if (!coversAxis) {
                everyPresentAxisCovered = false;
            }
            if (ccToFlush.values != null) {
                atLeastOnePresent = true;
            }
        }
        return atLeastOnePresent && !everyPresentAxisCovered;
    }

    /**
     * Returns a header with the region box excluded: cells inside the box
     * (on this header's columns) are no longer answered by the segment.
     * Returns {@code this} unchanged when the box adds nothing (already
     * subsumed by an excluded region) - callers key the no-op flush path
     * on that identity.
     *
     * @param region Region box
     * @return Header with the exclusion applied, or {@code this}
     */
    public SegmentHeader constrain(SegmentColumn[] region) {
        final List<SegmentColumn> boxColumns = new ArrayList<>();
        for (SegmentColumn col : region) {
            if (getConstrainedColumn(col.columnExpression) == null) {
                continue;
            }
            if (col.values == null) {
                // covers the whole axis: the box does not constrain it
                continue;
            }
            boxColumns.add(col);
        }
        assert !boxColumns.isEmpty() : "canConstrain must be checked first";
        // Idempotence: a box already fully inside an existing excluded
        // region excludes nothing new - the header is returned unchanged.
        // Without this, a repeated flush of the same region minted a fresh
        // uniqueID (plus a store rename and a cluster-wide publication)
        // on every run, growing the header forever.
        for (SegmentRegion existing : excludedRegions) {
            if (subsumes(existing, boxColumns)) {
                return this;
            }
        }
        final List<SegmentRegion> newRegions = new ArrayList<>(excludedRegions);
        newRegions.add(new SegmentRegion(boxColumns));
        return
            new SegmentHeader(
                schemaName,
                schemaChecksum,
                cubeName,
                measureName,
                constrainedColumns,
                compoundPredicates,
                rolapStarFactTableName,
                constrainedColsBitKey,
                newRegions);
    }

    /**
     * Whether every cell excluded by the box is already excluded by the
     * region: the region's condition on each of its columns must be
     * implied by the box (same column present, box values inside the
     * region's values; a region column without values covers its whole
     * axis and implies anything).
     */
    private static boolean subsumes(SegmentRegion region, List<SegmentColumn> box) {
        for (SegmentColumn regionColumn : region.columns()) {
            SegmentColumn boxColumn = null;
            for (SegmentColumn candidate : box) {
                if (candidate.columnExpression.equals(regionColumn.columnExpression)) {
                    boxColumn = candidate;
                    break;
                }
            }
            if (boxColumn == null) {
                return false;
            }
            if (regionColumn.values != null
                    && !regionColumn.values.containsAll(boxColumn.values)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a cell at the given coordinates lies inside one of the
     * excluded region boxes.
     */
    public boolean isCellExcluded(Map<String, Comparable> coordinates) {
        for (SegmentRegion region : excludedRegions) {
            if (region.contains(coordinates)) {
                return true;
            }
        }
        return false;
    }

    @Override
	public String toString() {
        return this.getDescription();
    }

    /**
     * Returns the arity of this SegmentHeader.
     * @return The arity as an integer number.
     */
    public int getArity() {
        return arity;
    }

    public List<SegmentRegion> getExcludedRegions() {
        return excludedRegions;
    }

    /**
     * Returns a list of constrained columns which define this segment
     * header. The caller should consider this list immutable.
     *
     * @return List of ConstrainedColumns
     */
    public List<SegmentColumn> getConstrainedColumns() {
        return constrainedColumns;
    }

    /**
     * Returns the constrained column object, if any, corresponding
     * to a column name and a table name.
     * @param columnExpression The column name we want.
     * @return A Constrained column, or null.
     */
    public SegmentColumn getConstrainedColumn(
        String columnExpression)
    {
        // typical arities are single digits: a linear scan beats building
        // and holding a map per header; wide headers keep the cached map
        if (constrainedColumns.size() <= 8) {
            for (SegmentColumn c : constrainedColumns) {
                if (c.columnExpression.equals(columnExpression)) {
                    return c;
                }
            }
            return null;
        }
        Map<String, SegmentColumn> byExpression = columnsByExpression;
        if (byExpression == null) {
            byExpression = new HashMap<>(constrainedColumns.size() * 2);
            for (SegmentColumn c : constrainedColumns) {
                byExpression.put(c.columnExpression, c);
            }
            columnsByExpression = byExpression;
        }
        return byExpression.get(columnExpression);
    }

    public BitKey getConstrainedColumnsBitKey() {
        // treated as immutable everywhere; the field is public anyway
        return this.constrainedColsBitKey;
    }

    /** The full typed identity of this header, bit key included. */
    public SegmentIdentity identity() {
        return identity;
    }

    /** The identity without the bit key; also the converter key. */
    public SegmentIdentity.FactKey factKey() {
        return identity.factKey();
    }

    /** The fact-level key without compound predicates. */
    public SegmentIdentity.RegionKey regionKey() {
        return identity.factKey().regionKey();
    }

    /**
     * Returns a unique identifier for this header. The identifier
     * can be used for storage and will be the same across segments
     * which have the same schema name, cube name, measure name,
     * and for each constrained column, the same column name, table name,
     * and predicate values.
     * @return A unique identification string.
     */
    public ByteString getUniqueID() {
        ByteString id = this.uniqueID;
        if (id == null) {
            final MessageDigest digest = sha256();
            // generation marker: changed identity rules rotate every id
            update(digest, "v" + SegmentWire.GENERATION + ":");
            update(digest, this.schemaName);
            update(digest, this.schemaChecksum.toString());
            update(digest, this.cubeName);
            update(digest, this.rolapStarFactTableName);
            update(digest, this.measureName);
            for (SegmentColumn c : getSortedColumns()) {
                update(digest, c.columnExpression);
                updateValues(digest, c.values);
            }
            for (SegmentRegion region : getSortedRegions()) {
                update(digest, "[");
                for (SegmentColumn c : region.columns()) {
                    update(digest, c.columnExpression);
                    updateValues(digest, c.values);
                }
                update(digest, "]");
            }
            for (SegmentPredicate c : compoundPredicates) {
                // structural, not the flat canonical string: separators inside
                // values must never make two predicates digest identically
                c.digest(s -> update(digest, s));
            }
            id = new ByteString(digest.digest());
            this.uniqueID = id;
        }
        return id;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** UTF-8 with a field separator — ids are portable and unambiguous. */
    private static void update(MessageDigest digest, String s) {
        digest.update(s.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    /** Values digest with their type — Integer 1 and String "1" differ. */
    private static void updateValues(MessageDigest digest, SortedSet<Comparable> values) {
        if (values == null) {
            return;
        }
        for (Object value : values) {
            update(digest, value == null ? "null" : value.getClass().getName());
            update(digest, String.valueOf(value));
        }
    }

    /**
     * This function returns a sorted view of the excluded regions
     * of this segment. We cannot sort them at construction because
     * changing their order would make it not correspond to its SegmentDataset.
     * Use this method with caution, not in tight loops.
     */
    private List<SegmentRegion> getSortedRegions() {
        // sorted view for the deterministic id; built on demand — only the
        // one-time digest and the trace description walk it. The order must
        // be TOTAL over the region content: two regions on the same column
        // with different values (two flushes on one axis) would otherwise
        // leave the digest hanging on flush order, and two nodes flushing
        // in opposite order would key the same segment differently.
        List<SegmentRegion> sorted = new ArrayList<>(excludedRegions);
        sorted.sort(SegmentHeader::compareRegions);
        return sorted;
    }

    /**
     * Structural total order - never a concatenated string key: a comma
     * inside a value made {"x,y"} collide with {"x","y"}, the tie fell
     * back to insertion (= flush) order, and the digest diverged between
     * nodes flushing in opposite order.
     */
    private static int compareRegions(SegmentRegion left, SegmentRegion right) {
        int c = Integer.compare(left.columns().size(), right.columns().size());
        if (c != 0) {
            return c;
        }
        for (int i = 0; i < left.columns().size(); i++) {
            c = compareColumns(left.columns().get(i), right.columns().get(i));
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    private static int compareColumns(SegmentColumn left, SegmentColumn right) {
        int c = left.columnExpression.compareTo(right.columnExpression);
        if (c != 0) {
            return c;
        }
        boolean leftWild = left.values == null;
        boolean rightWild = right.values == null;
        if (leftWild != rightWild) {
            return leftWild ? -1 : 1;
        }
        if (leftWild) {
            return 0;
        }
        c = Integer.compare(left.values.size(), right.values.size());
        if (c != 0) {
            return c;
        }
        var leftIt = left.values.iterator();
        var rightIt = right.values.iterator();
        while (leftIt.hasNext()) {
            c = compareValues(leftIt.next(), rightIt.next());
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    private static int compareValues(Comparable leftValue, Comparable rightValue) {
        if (leftValue == null || rightValue == null) {
            return leftValue == rightValue ? 0 : (leftValue == null ? -1 : 1);
        }
        int c = leftValue.getClass().getName().compareTo(rightValue.getClass().getName());
        if (c != 0) {
            return c;
        }
        return String.valueOf(leftValue).compareTo(String.valueOf(rightValue));
    }

    /**
     * This function returns a sorted view of the constrained columns
     * of this segment. We cannot sort them at construction because
     * changing their order would make it not correspond to its SegmentDataset.
     * Use this method with caution, not in tight loops.
     */
    private List<SegmentColumn> getSortedColumns() {
        // sorted view for the deterministic id; built on demand, see
        // getSortedRegions
        List<SegmentColumn> sorted = new ArrayList<>(constrainedColumns);
        sorted.sort(Comparator.comparing(c -> c.columnExpression));
        return sorted;
    }

    /**
     * Returns a human readable description of this
     * segment header.
     * @return A string describing the header.
     */
    public String getDescription() {
        // built per call: only trace logging and printCacheState read it,
        // and caching it would pin kilobytes per index-resident header
        StringBuilder descriptionSB = new StringBuilder();
        descriptionSB.append("*Segment Header\n");
        descriptionSB.append("Schema:[");
        descriptionSB.append(this.schemaName);
        descriptionSB.append("]\nChecksum:[");
        descriptionSB.append(this.schemaChecksum);
        descriptionSB.append("]\nCube:[");
        descriptionSB.append(this.cubeName);
        descriptionSB.append("]\nMeasure:[");
        descriptionSB.append(this.measureName);
        descriptionSB.append("]\n");
        descriptionSB.append("Axes:[");
        for (SegmentColumn c : getSortedColumns()) {
            appendColumn(descriptionSB, c);
        }
        descriptionSB.append("]\n");
        descriptionSB.append("Excluded Regions:[");
        for (SegmentRegion region : getSortedRegions()) {
            descriptionSB.append("\n  Box{");
            for (SegmentColumn c : region.columns()) {
                appendColumn(descriptionSB, c);
                }
            descriptionSB.append("}");
        }
        descriptionSB.append("]\n");
        descriptionSB.append("Compound Predicates:[");
        for (SegmentPredicate c : compoundPredicates) {
            descriptionSB.append("\n\t{");
            descriptionSB.append(c.canonical());
        }
        descriptionSB
            .append("]\n")
            .append("ID:[")
            .append(getUniqueID())
            .append("]\n");
        return descriptionSB.toString();
    }

    private static void appendColumn(StringBuilder sb, SegmentColumn c) {
        sb.append("\n    {");
        sb.append(c.columnExpression);
        sb.append("=(");
        if (c.values == null) {
            sb.append("* ");
        } else {
            for (Object value : c.values) {
                sb.append("'");
                sb.append(value);
                sb.append("',");
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(")}");
    }
}
