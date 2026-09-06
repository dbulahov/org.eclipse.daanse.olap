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
import java.util.List;
import java.util.Objects;

import org.eclipse.daanse.olap.key.BitKey;
import org.eclipse.daanse.olap.util.ByteString;

/**
 * Typed segment identity: everything the cell values depend on plus the
 * constrained-columns bit key. The index keys its maps on this class and
 * its {@link FactKey}/{@link RegionKey} projections. Immutable; the hash
 * computes once — identity values are hot map keys.
 */
public final class SegmentIdentity implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String schemaName;
    private final ByteString schemaChecksum;
    private final String cubeName;
    private final String rolapStarFactTableName;
    private final String measureName;
    private final List<SegmentPredicate> compoundPredicates;
    private final BitKey constrainedColsBitKey;
    private transient int hash;
    private transient FactKey factKey;

    public SegmentIdentity(String schemaName, ByteString schemaChecksum, String cubeName,
            String rolapStarFactTableName, String measureName, List<SegmentPredicate> compoundPredicates,
            BitKey constrainedColsBitKey) {
        this.schemaName = schemaName;
        this.schemaChecksum = schemaChecksum;
        this.cubeName = cubeName;
        this.rolapStarFactTableName = rolapStarFactTableName;
        this.measureName = measureName;
        this.compoundPredicates = List.copyOf(compoundPredicates);
        // freeze instead of defensive copy: an already-frozen key (the
        // normal case - headers and requests publish frozen) is shared as
        // is, so the per-ancestor identity lookups in the rollup search
        // stop cloning the key
        this.constrainedColsBitKey = constrainedColsBitKey == null ? null : constrainedColsBitKey.freeze();
    }


    public String schemaName() {
        return schemaName;
    }

    public ByteString schemaChecksum() {
        return schemaChecksum;
    }

    public String cubeName() {
        return cubeName;
    }

    public String rolapStarFactTableName() {
        return rolapStarFactTableName;
    }

    public String measureName() {
        return measureName;
    }

    public List<SegmentPredicate> compoundPredicates() {
        return compoundPredicates;
    }

    public BitKey constrainedColsBitKey() {
        return constrainedColsBitKey;
    }

    /** The identity without the bit key. */
    public FactKey factKey() {
        if (factKey == null) {
            factKey = new FactKey(schemaName, schemaChecksum, cubeName, rolapStarFactTableName,
                    measureName, compoundPredicates);
        }
        return factKey;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = Objects.hash(schemaName, schemaChecksum, cubeName, rolapStarFactTableName, measureName,
                    compoundPredicates, constrainedColsBitKey);
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof SegmentIdentity that
                && hashCode() == that.hashCode()
                && schemaName.equals(that.schemaName)
                && Objects.equals(schemaChecksum, that.schemaChecksum)
                && cubeName.equals(that.cubeName)
                && rolapStarFactTableName.equals(that.rolapStarFactTableName)
                && measureName.equals(that.measureName)
                && compoundPredicates.equals(that.compoundPredicates)
                && Objects.equals(constrainedColsBitKey, that.constrainedColsBitKey);
    }

    @Override
    public String toString() {
        return "SegmentIdentity[" + schemaName + ", " + schemaChecksum + ", " + cubeName + ", "
                + rolapStarFactTableName + ", " + measureName + ", " + compoundPredicates + ", "
                + constrainedColsBitKey + "]";
    }

    /** Fact-level lookup and converter key of a segment. */
    public static final class FactKey implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String schemaName;
        private final ByteString schemaChecksum;
        private final String cubeName;
        private final String rolapStarFactTableName;
        private final String measureName;
        private final List<SegmentPredicate> compoundPredicates;
        private transient int hash;
        private transient RegionKey regionKey;

        public FactKey(String schemaName, ByteString schemaChecksum, String cubeName,
                String rolapStarFactTableName, String measureName,
                List<SegmentPredicate> compoundPredicates) {
            this.schemaName = schemaName;
            this.schemaChecksum = schemaChecksum;
            this.cubeName = cubeName;
            this.rolapStarFactTableName = rolapStarFactTableName;
            this.measureName = measureName;
            this.compoundPredicates = List.copyOf(compoundPredicates);
        }


        public String schemaName() {
            return schemaName;
        }

        public ByteString schemaChecksum() {
            return schemaChecksum;
        }

        public String cubeName() {
            return cubeName;
        }

        public String rolapStarFactTableName() {
            return rolapStarFactTableName;
        }

        public String measureName() {
            return measureName;
        }

        public List<SegmentPredicate> compoundPredicates() {
            return compoundPredicates;
        }

        /** The key without the compound predicates. */
        public RegionKey regionKey() {
            if (regionKey == null) {
                regionKey = new RegionKey(schemaName, schemaChecksum, cubeName,
                        rolapStarFactTableName, measureName);
            }
            return regionKey;
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                h = Objects.hash(schemaName, schemaChecksum, cubeName, rolapStarFactTableName, measureName,
                        compoundPredicates);
                hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof FactKey that
                    && hashCode() == that.hashCode()
                    && schemaName.equals(that.schemaName)
                    && Objects.equals(schemaChecksum, that.schemaChecksum)
                    && cubeName.equals(that.cubeName)
                    && rolapStarFactTableName.equals(that.rolapStarFactTableName)
                    && measureName.equals(that.measureName)
                    && compoundPredicates.equals(that.compoundPredicates);
        }

        @Override
        public String toString() {
            return "FactKey[" + schemaName + ", " + schemaChecksum + ", " + cubeName + ", "
                    + rolapStarFactTableName + ", " + measureName + ", " + compoundPredicates + "]";
        }
    }

    /** Fact-level key without compound predicates. */
    public static final class RegionKey implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String schemaName;
        private final ByteString schemaChecksum;
        private final String cubeName;
        private final String rolapStarFactTableName;
        private final String measureName;
        private transient int hash;

        public RegionKey(String schemaName, ByteString schemaChecksum, String cubeName,
                String rolapStarFactTableName, String measureName) {
            this.schemaName = schemaName;
            this.schemaChecksum = schemaChecksum;
            this.cubeName = cubeName;
            this.rolapStarFactTableName = rolapStarFactTableName;
            this.measureName = measureName;
        }


        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                h = Objects.hash(schemaName, schemaChecksum, cubeName, rolapStarFactTableName, measureName);
                hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof RegionKey that
                    && hashCode() == that.hashCode()
                    && schemaName.equals(that.schemaName)
                    && Objects.equals(schemaChecksum, that.schemaChecksum)
                    && cubeName.equals(that.cubeName)
                    && rolapStarFactTableName.equals(that.rolapStarFactTableName)
                    && measureName.equals(that.measureName)
                    ;
        }

        @Override
        public String toString() {
            return "RegionKey[" + schemaName + ", " + schemaChecksum + ", " + cubeName + ", "
                    + rolapStarFactTableName + ", " + measureName + "]";
        }
    }
}
