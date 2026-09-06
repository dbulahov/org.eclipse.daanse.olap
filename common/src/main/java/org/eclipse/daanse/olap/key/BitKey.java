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
 * jhyde, 30 August, 2001
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


package org.eclipse.daanse.olap.key;

import java.io.Serializable;
import java.util.BitSet;
import java.util.function.IntConsumer;

/**
 * A fixed-capacity set of bit positions — in the engine: "which star
 * columns are constrained". BitKeys key the segment caches (working
 * store, index, batch identity), order the compound-predicate maps and
 * travel in the segment wire form ({@link #toLongArray()}), so value,
 * hash and order equality are defined ACROSS implementations and never
 * depend on capacity.
 *
 * Three implementations cover the widths: {@link Small} (one long, up
 * to 64 bits), {@link Mid128} (two longs, up to 128) and {@link Big}
 * (an array of 64-bit chunks). {@link Factory#makeBitKey(int)} picks
 * one; unlike {@link java.util.BitSet} a key never grows — mutations
 * outside the capacity throw.
 *
 * Life cycle: the mutable key IS the builder. Build with
 * {@link #set(int)}, then {@link #freeze()} at the publication point —
 * a frozen key refuses mutation by type, and freezing a frozen key
 * returns the same instance. A key published as a map key or identity
 * component must be frozen, or at least never mutated again; the wide
 * implementation caches its hash under this contract.
 *
 * @author Richard M. Emberson
 */
public interface BitKey
        extends Serializable, Comparable<BitKey>
{
    /**
     * The BitKey with no bits set. Shared and immutable: every mutator
     * throws {@link UnsupportedOperationException}, and deserialization
     * resolves back to this instance.
     */
    BitKey EMPTY = new Small.Empty();

    /**
     * Sets the bit at the specified position to the specified value.
     * Mutates this key in place — never call on a published key.
     * Throws {@link IllegalArgumentException} for a negative or
     * over-capacity position.
     */
    void set(int pos, boolean value);

    /**
     * Sets the bit at the specified position to <code>true</code>.
     * Mutates this key in place — never call on a published key.
     * Throws {@link IllegalArgumentException} for a negative or
     * over-capacity position.
     */
    void set(int pos);

    /**
     * Returns whether the bit at the specified position is set. A
     * negative or over-capacity position reads as false — those bits do
     * not exist and are never set.
     */
    boolean get(int pos);

    /**
     * Sets the bit at the specified position to <code>false</code>.
     * Mutates this key in place — never call on a published key.
     * Throws {@link IllegalArgumentException} for a negative or
     * over-capacity position.
     */
    void clear(int pos);

    /**
     * Sets all of the bits in this BitKey to <code>false</code>.
     * Mutates this key in place — never call on a published key.
     */
    void clear();

    /**
     * Is every bit set in the parameter bitKey also set in
     * this.
     * If one switches this with the parameter bitKey
     * one gets the equivalent of isSubSetOf.
     *
     * @param bitKey Bit key
     */
    boolean isSuperSetOf(BitKey bitKey);

    /**
     * Returns a NEW BitKey with every bit set that is set in this key or
     * the parameter; neither operand is mutated. The result carries the
     * larger operand's capacity.
     *
     * @param bitKey Bit key
     */
    BitKey or(BitKey bitKey);


    /**
     * Returns a NEW BitKey holding the boolean AND of this key and the
     * parameter; neither operand is mutated. The result keeps the
     * receiver's capacity, except on the wide implementation, which
     * narrows to the smaller operand.
     *
     * @param bitKey Bit key
     */
    BitKey and(BitKey bitKey);

    /**
     * Returns a NEW BitKey containing all of the bits in this key whose
     * corresponding bit is NOT set in the parameter; neither operand is
     * mutated. The result keeps the receiver's capacity.
     */
    BitKey andNot(BitKey bitKey);

    /**
     * Returns a copy of this BitKey.
     *
     * @return copy of BitKey
     */
    BitKey copy();

    /**
     * Returns an empty BitKey of the same type and the same capacity —
     * the same as calling {@link #copy} followed by {@link #clear()}.
     * This is the way to transport a key's capacity to a sibling key.
     *
     * @return BitKey of same type
     */
    BitKey emptyCopy();

    /**
     * Returns an immutable key with this key's bits: mutators throw
     * {@link UnsupportedOperationException}; value, hash and wire form
     * match the mutable original, and {@link #copy()}/{@link #emptyCopy()}
     * hand back ordinary mutable keys. Freezing a frozen key returns the
     * SAME instance, so publication points call freeze() unconditionally.
     * A frozen key is safe as a map key or identity component by type,
     * not by convention.
     */
    BitKey freeze();

    /**
     * Returns true if this BitKey contains no bits that are set
     * to true.
     */
    boolean isEmpty();

    /**
     * Returns whether this BitKey has any bits in common with a given BitKey.
     */
    boolean intersects(BitKey bitKey);

    /**
     * Returns the set bits as little-endian 64-bit words with trailing
     * zero words trimmed — exactly {@link BitSet#toLongArray()} of the
     * same bits. This is the canonical wire form of a key.
     */
    long[] toLongArray();

    /**
     * Returns a {@link BitSet} with the same contents as this BitKey.
     */
    default BitSet toBitSet() {
        return BitSet.valueOf(toLongArray());
    }

    /**
     * Calls the consumer for every set bit position, from smallest to
     * largest — the primitive walk over the key, built on
     * {@link #nextSetBit(int)}.
     */
    default void forEachSetBit(IntConsumer consumer) {
        for (int pos = nextSetBit(0); pos >= 0; pos = nextSetBit(pos + 1)) {
            consumer.accept(pos);
        }
    }

    /**
     * Whether every bit set in this key is also set in the parameter
     * bitKey — the mirror of {@link #isSuperSetOf(BitKey)}.
     */
    default boolean isSubSetOf(BitKey bitKey) {
        return bitKey.isSuperSetOf(this);
    }

    /**
     * Returns the index of the first bit that is set to <code>true</code>
     * that occurs on or after the specified starting index. If no such
     * bit exists then -1 is returned.
     *
     * To iterate over the true bits in a BitKey,
     * use the following loop:
     *
     * 
     * for (int i = result.nextSetBit(0); i >= 0; i = result.nextSetBit(i + 1)) {
     *     // operate on index i here
     * }
     *
     * @param   fromIndex the index to start checking from (inclusive)
     * @return  the index of the next set bit
     * @throws  IndexOutOfBoundsException if the specified index is negative
     */
    int nextSetBit(int fromIndex);

    /**
     * Returns the number of bits set.
     *
     * @return Number of bits set
     */
    int cardinality();

    public final class Factory {

        private Factory() {
            // constructor
        }

        /**
         * Creates a {@link BitKey} sized for a given number of bits. The
         * actual capacity rounds up to the chosen implementation's width
         * (64, 128, or whole 64-bit chunks); value equality never depends
         * on capacity.
         * @param size Number of bits in key
         */
        public static BitKey makeBitKey(int size) {
            return makeBitKey(size, false);
        }

        /**
         * Creates a {@link BitKey} with a capacity for a given number of bits.
         * @param size Number of bits in key
         * @param init The default value of all bits.
         */
        public static BitKey makeBitKey(int size, boolean init) {
            if (size < 0) {
                String msg = new StringBuilder("Negative size \"").append(size).append("\" not allowed").toString();
                throw new IllegalArgumentException(msg);
            }
            final BitKey result;
            if (size < 64) {
                result = new BitKey.Small();
            } else if (size < 128) {
                result = new BitKey.Mid128();
            } else {
                result = new BitKey.Big(size);
            }
            if (init) {
                for (int i = 0; i < size; i++) {
                    result.set(i, init);
                }
            }
            return result;
        }

        /**
         * Creates a {@link BitKey} from its canonical wire form — the
         * words {@link BitKey#toLongArray()} produced. Sized by the
         * highest set bit like {@link #makeBitKey(BitSet)}: the variant
         * may narrow, value and hash equality are preserved.
         */
        public static BitKey fromLongArray(long[] words) {
            return makeBitKey(BitSet.valueOf(words));
        }

        /**
         * Creates a {@link BitKey} with the same contents as a {@link BitSet}.
         */
        public static BitKey makeBitKey(BitSet bitSet) {
            BitKey bitKey = makeBitKey(bitSet.length());
            for (int i = bitSet.nextSetBit(0);
                i >= 0;
                i = bitSet.nextSetBit(i + 1))
            {
                bitKey.set(i);
            }
            return bitKey;
        }
    }

    /**
     * Shared base of the implementations: the static bit arithmetic
     * (chunk = one long of 64 bits) and the two-argument
     * {@link #set(int, boolean)}.
     */
    abstract class AbstractBitKey implements BitKey {
        private static final long serialVersionUID = -2942302671676103450L;
        /** Shift from bit position to chunk index (a chunk holds 64 bits). */
        protected static final int CHUNK_SHIFT = 6;
        /** Mask of the bit position within its chunk. */
        protected static final int CHUNK_MASK = 63;

        /**
         * Creates a chunk containing a single bit.
         */
        protected static long bit(int pos) {
            return (1L << (pos & CHUNK_MASK));
        }

        /**
         * Returns which chunk a given bit falls into.
         * Bits 0 to 63 fall in chunk 0, bits 64 to 127 fall into chunk 1.
         */
        protected static int chunkPos(int size) {
            return (size >> CHUNK_SHIFT);
        }

        /**
         * Returns the number of chunks required for a given number of bits.
         *
         * <p>0 bits requires 0 chunks; 1 - 64 bits requires 1 chunk; etc.
         */
        protected static int chunkCount(int size) {
            return (size + 63) >> CHUNK_SHIFT;
        }

        @Override
		public final void set(int pos, boolean value) {
            if (value) {
                set(pos);
            } else {
                clear(pos);
            }
        }

        protected IllegalArgumentException createException(BitKey bitKey) {
            final String msg = (bitKey == null)
                ? "Null BitKey"
                : "Bad BitKey type: " + bitKey.getClass().getName();
            return new IllegalArgumentException(msg);
        }

        /**
         * Compares a pair of {@code long} arrays, using unsigned comparison
         * semantics and padding to the left with 0s.
         *
         * Values are treated as unsigned for the purposes of comparison.
         *
         * If the arrays have different lengths, the shorter is padded with
         * 0s.
         *
         * @param a1 First array
         * @param a2 Second array
         * @return -1 if a1 compares less to a2,
         * 0 if a1 is equal to a2,
         * 1 if a1 is greater than a2
         */
        public static int compareUnsignedArrays(long[] a1, long[] a2) {
            int i1 = a1.length - 1;
            int i2 = a2.length - 1;
            if (i1 > i2) {
                do {
                    if (a1[i1] != 0) {
                        return 1;
                    }
                    --i1;
                } while (i1 > i2);
            } else if (i2 > i1) {
                do {
                    if (a2[i2] != 0) {
                        return -1;
                    }
                    --i2;
                } while (i2 > i1);
            }
            assert i1 == i2;
            for (; i1 >= 0; --i1) {
                int c = Long.compareUnsigned(a1[i1], a2[i1]);
                if (c != 0) {
                    return c;
                }
            }
            return 0;
        }

    }

    /**
     * The one-long implementation: positions 0–63, the layout almost
     * every real catalog fits (a star rarely has 64+ columns).
     */
    public class Small extends AbstractBitKey {

        private static final long serialVersionUID = -7891880560056571197L;
        private long bits;

        /**
         * Creates a Small with no bits set.
         */
        private Small() {
        }

        /**
         * Creates a Small and initializes it to the 64 bit value.
         *
         * @param bits 64 bit value
         */
        private Small(long bits) {
            this.bits = bits;
        }

        @Override
        public BitKey freeze() {
            return new Frozen(bits);
        }

        /** The immutable form of {@link Small}. */
        private static class Frozen extends Small {
            private static final long serialVersionUID = 1L;

            private Frozen(long bits) {
                super(bits);
            }

            @Override
            public void set(int pos) {
                throw new UnsupportedOperationException("frozen BitKey");
            }

            @Override
            public void clear(int pos) {
                throw new UnsupportedOperationException("frozen BitKey");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("frozen BitKey");
            }

            @Override
            public BitKey freeze() {
                return this;
            }
        }

        /**
         * The shared {@link BitKey#EMPTY} instance: a frozen empty key
         * whose deserialization resolves back to the shared instance, so
         * identity checks against EMPTY survive a round trip.
         */
        private static final class Empty extends Frozen {
            private static final long serialVersionUID = 1L;

            private Empty() {
                super(0);
            }

            private Object readResolve() {
                return BitKey.EMPTY;
            }
        }

        @Override
		public void set(int pos) {
            if (pos >= 0 && pos < 64) {
                bits |= bit(pos);
            } else {
                throw new IllegalArgumentException(
                    new StringBuilder("pos ").append(pos).append(" exceeds capacity 64").toString());
            }
        }

        @Override
		public boolean get(int pos) {
            return pos >= 0 && pos < 64 && ((bits & bit(pos)) != 0);
        }

        @Override
		public void clear(int pos) {
            if (pos >= 0 && pos < 64) {
                bits &= ~bit(pos);
            } else {
                throw new IllegalArgumentException(
                    new StringBuilder("pos ").append(pos).append(" exceeds capacity 64").toString());
            }
        }

        @Override
		public void clear() {
            bits = 0;
        }

        @Override
		public int cardinality() {
            return Long.bitCount(bits);
        }

        private void or(long bits) {
            this.bits |= bits;
        }


        private void and(long bits) {
            this.bits &= bits;
        }

        @Override
		public BitKey or(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                final BitKey.Small result = (BitKey.Small) copy();
                result.or(other.bits);
                return result;

            } else if (bitKey instanceof BitKey.Mid128 other) {
                final BitKey.Mid128 result = (BitKey.Mid128) other.copy();
                result.or(this.bits, 0);
                return result;

            } else if (bitKey instanceof BitKey.Big other) {
                final BitKey.Big result = (BitKey.Big) other.copy();
                result.or(this.bits);
                return result;
            }

            throw createException(bitKey);
        }


        @Override
		public BitKey and(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                final BitKey.Small result = (BitKey.Small) copy();
                result.and(other.bits);
                return result;

            } else if (bitKey instanceof BitKey.Mid128 other) {
                final BitKey.Small result = (BitKey.Small) copy();
                result.and(other.bits0);
                return result;

            } else if (bitKey instanceof BitKey.Big other) {
                final BitKey.Small result = (BitKey.Small) copy();
                result.and(other.bits[0]);
                return result;
            }

            throw createException(bitKey);
        }

        @Override
		public BitKey andNot(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                final BitKey.Small result = (BitKey.Small) copy();
                result.andNot(other.bits);
                return result;

            } else if (bitKey instanceof BitKey.Mid128 other) {
                final BitKey.Small result = (BitKey.Small) copy();
                result.andNot(other.bits0);
                return result;

            } else if (bitKey instanceof BitKey.Big other) {
                final BitKey.Small result = (BitKey.Small) copy();
                result.andNot(other.bits[0]);
                return result;
            }

            throw createException(bitKey);
        }

        private void andNot(long bits) {
            this.bits &= ~bits;
        }

        @Override
		public boolean isSuperSetOf(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                return ((this.bits | other.bits) == this.bits);

            } else if (bitKey instanceof BitKey.Mid128 other) {
                return ((this.bits | other.bits0) == this.bits)
                    && (other.bits1 == 0);

            } else if (bitKey instanceof BitKey.Big other) {
                if ((this.bits | other.bits[0]) != this.bits) {
                    return false;
                } else {
                    for (int i = 1; i < other.bits.length; i++) {
                        if (other.bits[i] != 0) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
		public boolean intersects(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                return (this.bits & other.bits) != 0;

            } else if (bitKey instanceof BitKey.Mid128 other) {
                return (this.bits & other.bits0) != 0;

            } else if (bitKey instanceof BitKey.Big other) {
                return (this.bits & other.bits[0]) != 0;
            }
            return false;
        }

        @Override
		public long[] toLongArray() {
            return bits == 0 ? new long[0] : new long[] {bits};
        }


        @Override
		public int nextSetBit(int fromIndex) {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException(
                    "fromIndex < 0: " + fromIndex);
            }

            if (fromIndex < 64) {
                long word = bits & (-1L << fromIndex);
                if (word != 0) {
                    return Long.numberOfTrailingZeros(word);
                }
            }
            return -1;
        }

        @Override
		public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof BitKey.Small other) {
                return (this.bits == other.bits);

            } else if (o instanceof BitKey.Mid128 other) {
                return (this.bits == other.bits0) && (other.bits1 == 0);

            } else if (o instanceof BitKey.Big other) {
                if (this.bits != other.bits[0]) {
                    return false;
                } else {
                    for (int i = 1; i < other.bits.length; i++) {
                        if (other.bits[i] != 0) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
		public int hashCode() {
            // same shape as Mid128/Big: seed XOR chunks, then fold the
            // ACCUMULATOR - folding the raw bits is value-identical only
            // while the seed fits in 32 bits; this form survives a seed change
            long h = 1234L ^ bits;
            return (int) ((h >>> 32) ^ h);
        }

        @Override
		public int compareTo(BitKey bitKey) {
            if (bitKey instanceof Small other) {
                return Long.compareUnsigned(this.bits, other.bits);
            } else if (bitKey instanceof Mid128 other) {
                if (other.bits1 != 0) {
                    return -1;
                }
                return Long.compareUnsigned(this.bits, other.bits0);
            } else {
                return compareToBig((Big) bitKey);
            }
        }

        private int compareToBig(Big other) {
            int otherLength = other.effectiveSize();
            switch (otherLength) {
            case 0:
                return this.bits == 0 ? 0 : 1;
            case 1:
                return Long.compareUnsigned(this.bits, other.bits[0]);
            default:
                return -1;
            }
        }

        @Override
		public String toString() {
            StringBuilder buf = new StringBuilder(64);
            buf.append("0x");
            for (int i = 63; i >= 0; i--) {
                buf.append((get(i)) ? '1' : '0');
            }
            return buf.toString();
        }

        @Override
		public BitKey copy() {
            return new Small(this.bits);
        }

        @Override
		public BitKey emptyCopy() {
            return new Small();
        }

        @Override
		public boolean isEmpty() {
            return bits == 0;
        }
    }

    /**
     * The two-long implementation: positions 0–127, two inline fields
     * instead of an array.
     */
    public class Mid128 extends AbstractBitKey {
        private static final long serialVersionUID = -8409143207943258659L;
        private long bits0;
        private long bits1;

        private Mid128() {
        }

        private Mid128(Mid128 mid) {
            this.bits0 = mid.bits0;
            this.bits1 = mid.bits1;
        }

        @Override
        public BitKey freeze() {
            return new Frozen(this);
        }

        /** The immutable form of {@link Mid128}. */
        private static final class Frozen extends Mid128 {
            private static final long serialVersionUID = 1L;

            private Frozen(Mid128 source) {
                super(source);
            }

            @Override
            public void set(int pos) {
                throw new UnsupportedOperationException("frozen BitKey");
            }

            @Override
            public void clear(int pos) {
                throw new UnsupportedOperationException("frozen BitKey");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("frozen BitKey");
            }

            @Override
            public BitKey freeze() {
                return this;
            }
        }

        @Override
		public void set(int pos) {
            if (pos >= 0 && pos < 64) {
                bits0 |= bit(pos);
            } else if (pos >= 64 && pos < 128) {
                bits1 |= bit(pos);
            } else {
                throw new IllegalArgumentException(
                    new StringBuilder("pos ").append(pos).append(" exceeds capacity 128").toString());
            }
        }

        @Override
		public boolean get(int pos) {
            if (pos >= 0 && pos < 64) {
                return (bits0 & bit(pos)) != 0;
            } else if (pos >= 64 && pos < 128) {
                return (bits1 & bit(pos)) != 0;
            } else {
                return false;
            }
        }

        @Override
		public void clear(int pos) {
            if (pos >= 0 && pos < 64) {
                bits0 &= ~bit(pos);
            } else if (pos >= 64 && pos < 128) {
                bits1 &= ~bit(pos);
            } else {
                throw new IllegalArgumentException(
                    new StringBuilder("pos ").append(pos).append(" exceeds capacity 128").toString());
            }
        }

        @Override
		public void clear() {
            bits0 = 0;
            bits1 = 0;
        }

        @Override
		public int cardinality() {
            return Long.bitCount(bits0)
               + Long.bitCount(bits1);
        }

        private void or(long bits0, long bits1) {
            this.bits0 |= bits0;
            this.bits1 |= bits1;
        }


        private void and(long bits0, long bits1) {
            this.bits0 &= bits0;
            this.bits1 &= bits1;
        }

        @Override
		public BitKey or(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                final BitKey.Mid128 result = (BitKey.Mid128) copy();
                result.or(other.bits, 0);
                return result;

            } else if (bitKey instanceof BitKey.Mid128 other) {
                final BitKey.Mid128 result = (BitKey.Mid128) copy();
                result.or(other.bits0, other.bits1);
                return result;

            } else if (bitKey instanceof BitKey.Big other) {
                final BitKey.Big result = (BitKey.Big) other.copy();
                result.or(this.bits0, this.bits1);
                return result;
            }

            throw createException(bitKey);
        }


        @Override
		public BitKey and(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                final BitKey.Mid128 result = (BitKey.Mid128) copy();
                result.and(other.bits, 0);
                return result;

            } else if (bitKey instanceof BitKey.Mid128 other) {
                final BitKey.Mid128 result = (BitKey.Mid128) copy();
                result.and(other.bits0, other.bits1);
                return result;

            } else if (bitKey instanceof BitKey.Big other) {
                final BitKey.Mid128 result = (BitKey.Mid128) copy();
                result.and(other.bits[0], other.bits[1]);
                return result;
            }

            throw createException(bitKey);
        }

        @Override
		public BitKey andNot(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                final BitKey.Mid128 result = (BitKey.Mid128) copy();
                result.andNot(other.bits, 0);
                return result;

            } else if (bitKey instanceof BitKey.Mid128 other) {
                final BitKey.Mid128 result = (BitKey.Mid128) copy();
                result.andNot(other.bits0, other.bits1);
                return result;

            } else if (bitKey instanceof BitKey.Big other) {
                final BitKey.Mid128 result = (BitKey.Mid128) copy();
                result.andNot(other.bits[0], other.bits[1]);
                return result;
            }

            throw createException(bitKey);
        }

        private void andNot(long bits0, long bits1) {
            this.bits0 &= ~bits0;
            this.bits1 &= ~bits1;
        }

        @Override
		public boolean isSuperSetOf(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                return ((this.bits0 | other.bits) == this.bits0);

            } else if (bitKey instanceof BitKey.Mid128 other) {
                return ((this.bits0 | other.bits0) == this.bits0)
                    && ((this.bits1 | other.bits1) == this.bits1);

            } else if (bitKey instanceof BitKey.Big other) {
                if ((this.bits0 | other.bits[0]) != this.bits0) {
                    return false;
                } else if ((this.bits1 | other.bits[1]) != this.bits1) {
                    return false;
                } else {
                    for (int i = 2; i < other.bits.length; i++) {
                        if (other.bits[i] != 0) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
		public boolean intersects(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                return (this.bits0 & other.bits) != 0;

            } else if (bitKey instanceof BitKey.Mid128 other) {
                return (this.bits0 & other.bits0) != 0
                    || (this.bits1 & other.bits1) != 0;

            } else if (bitKey instanceof BitKey.Big other) {
                if ((this.bits0 & other.bits[0]) != 0) {
                    return true;
                } else {
                    return ((this.bits1 & other.bits[1]) != 0);
                }
            }
            return false;
        }

        @Override
		public long[] toLongArray() {
            if (bits1 != 0) {
                return new long[] {bits0, bits1};
            }
            return bits0 == 0 ? new long[0] : new long[] {bits0};
        }

        @Override
		public int nextSetBit(int fromIndex) {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException(
                    "fromIndex < 0: " + fromIndex);
            }

            int u = fromIndex >> 6;
            long word;
            switch (u) {
            case 0:
                word = bits0 & (-1L << fromIndex);
                if (word != 0) {
                    return Long.numberOfTrailingZeros(word);
                }
                word = bits1;
                if (word != 0) {
                    return 64 + Long.numberOfTrailingZeros(word);
                }
                return -1;
            case 1:
                word = bits1 & (-1L << fromIndex);
                if (word != 0) {
                    return 64 + Long.numberOfTrailingZeros(word);
                }
                return -1;
            default:
                return -1;
            }
        }

        @Override
		public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof BitKey.Small other) {
                return (this.bits0 == other.bits) && (this.bits1 == 0);

            } else if (o instanceof BitKey.Mid128 other) {
                return (this.bits0 == other.bits0)
                    && (this.bits1 == other.bits1);

            } else if (o instanceof BitKey.Big other) {
                if (this.bits0 != other.bits[0]) {
                    return false;
                } else if (this.bits1 != other.bits[1]) {
                    return false;
                } else {
                    for (int i = 2; i < other.bits.length; i++) {
                        if (other.bits[i] != 0) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
		public int hashCode() {
            long h = 1234;
            h ^= bits0;
            h ^= bits1 * 2;
            return (int)((h >> 32) ^ h);
        }

        @Override
		public String toString() {
            StringBuilder buf = new StringBuilder(64);
            buf.append("0x");
            for (int i = 127; i >= 0; i--) {
                buf.append((get(i)) ? '1' : '0');
            }
            return buf.toString();
        }

        @Override
		public BitKey copy() {
            return new Mid128(this);
        }

        @Override
		public BitKey emptyCopy() {
            return new Mid128();
        }

        @Override
		public boolean isEmpty() {
            return bits0 == 0
                && bits1 == 0;
        }

        // implement Comparable (in lazy, expensive fashion)
        @Override
		public int compareTo(BitKey bitKey) {
            if (bitKey instanceof Mid128 other) {
                if (this.bits1 != other.bits1) {
                    return Long.compareUnsigned(this.bits1, other.bits1);
                }
                return Long.compareUnsigned(this.bits0, other.bits0);
            } else if (bitKey instanceof Small other) {
                if (this.bits1 != 0) {
                    return 1;
                }
                return Long.compareUnsigned(this.bits0, other.bits);
            } else {
                return compareToBig((Big) bitKey);
            }
        }

        private int compareToBig(Big other) {
            int otherLength = other.effectiveSize();
            switch (otherLength) {
            case 0:
                return this.bits1 == 0
                    && this.bits0 == 0
                    ? 0
                    : 1;
            case 1:
                if (this.bits1 != 0) {
                    return 1;
                }
                return Long.compareUnsigned(this.bits0, other.bits[0]);
            case 2:
                if (this.bits1 != other.bits[1]) {
                    return Long.compareUnsigned(this.bits1, other.bits[1]);
                }
                return Long.compareUnsigned(this.bits0, other.bits[0]);
            default:
                return -1;
            }
        }
    }

    /**
     * The wide implementation: an array of 64-bit chunks, fixed at
     * construction (no dynamic resizing), with a lazily cached hash that
     * every mutator resets.
     */
    public class Big extends AbstractBitKey {
        private static final long serialVersionUID = -3715282769845236295L;
        private long[] bits;
        /** Cached hash; 0 = not computed. Mutators reset it. */
        private transient int hash;

        private Big(int size) {
            bits = new long[chunkCount(size + 1)];
        }

        private Big(Big big) {
            bits = big.bits.clone();
        }

        @Override
        public BitKey freeze() {
            return new Frozen(this);
        }

        /** The immutable form of {@link Big}: the hash is computed eagerly. */
        private static final class Frozen extends Big {
            private static final long serialVersionUID = 1L;

            private Frozen(Big source) {
                super(source);
                // published keys pay the wide hash once, here
                hashCode();
            }

            @Override
            public void set(int pos) {
                throw new UnsupportedOperationException("frozen BitKey");
            }

            @Override
            public void clear(int pos) {
                throw new UnsupportedOperationException("frozen BitKey");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("frozen BitKey");
            }

            @Override
            public BitKey freeze() {
                return this;
            }
        }

        private int size() {
            return bits.length;
        }

        /**
         * Returns the number of chunks, ignoring any chunks on the leading
         * edge that are all zero.
         *
         * @return number of chunks that are not on the leading edge
         */
        private int effectiveSize() {
            int n = bits.length;
            while (n > 0 && bits[n - 1] == 0) {
                --n;
            }
            return n;
        }

        @Override
		public void set(int pos) {
            if (pos < 0 || chunkPos(pos) >= bits.length) {
                throw new IllegalArgumentException(
                    new StringBuilder("pos ").append(pos).append(" exceeds capacity ")
                        .append(bits.length << CHUNK_SHIFT).toString());
            }
            bits[chunkPos(pos)] |= bit(pos);
            hash = 0;
        }

        @Override
		public boolean get(int pos) {
            return pos >= 0 && chunkPos(pos) < bits.length
                && (bits[chunkPos(pos)] & bit(pos)) != 0;
        }

        @Override
		public void clear(int pos) {
            if (pos < 0 || chunkPos(pos) >= bits.length) {
                throw new IllegalArgumentException(
                    new StringBuilder("pos ").append(pos).append(" exceeds capacity ")
                        .append(bits.length << CHUNK_SHIFT).toString());
            }
            bits[chunkPos(pos)] &= ~bit(pos);
            hash = 0;
        }

        @Override
		public void clear() {
            for (int i = 0; i < bits.length; i++) {
                bits[i] = 0;
            }
            hash = 0;
        }

        @Override
		public int cardinality() {
            int n = 0;
            for (int i = 0; i < bits.length; i++) {
                n += Long.bitCount(bits[i]);
            }
            return n;
        }

        private void or(long bits0) {
            this.bits[0] |= bits0;
            hash = 0;
        }

        private void or(long bits0, long bits1) {
            this.bits[0] |= bits0;
            this.bits[1] |= bits1;
            hash = 0;
        }

        private void or(long[] bits) {
            // every caller picks the larger side as the receiver first (a Big
            // always has >= 2 chunks - the Factory only builds it for
            // size >= 128). The clamp keeps a future caller's mistake from
            // becoming an AIOOBE; the assert keeps it from silently DROPPING
            // bits, which for or() would be worse than the crash
            assert bits.length <= this.bits.length : "or() argument wider than receiver";
            int length = Math.min(bits.length, this.bits.length);
            for (int i = 0; i < length; i++) {
                this.bits[i] |= bits[i];
            }
            hash = 0;
        }




        private void and(long[] bits) {
            // same shape as or()'s guard: the Big/Big paths pick the SMALLER
            // side as the receiver, so the argument is never shorter - the
            // min-clamp plus the tail-zeroing keeps a future caller's
            // mistake correct (missing high chunks count as zero for and)
            int length = Math.min(bits.length, this.bits.length);
            for (int i = 0; i < length; i++) {
                this.bits[i] &= bits[i];
            }
            for (int i = bits.length; i < this.bits.length; i++) {
                this.bits[i] = 0;
            }
            hash = 0;
        }

        @Override
		public BitKey or(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                final BitKey.Big result = (BitKey.Big) copy();
                result.or(other.bits);
                return result;

            } else if (bitKey instanceof BitKey.Mid128 other) {
                final BitKey.Big result = (BitKey.Big) copy();
                result.or(other.bits0, other.bits1);
                return result;

            } else if (bitKey instanceof BitKey.Big other) {
                if (other.size() > size()) {
                    final BitKey.Big result = (BitKey.Big) other.copy();
                    result.or(bits);
                    return result;
                } else {
                    final BitKey.Big result = (BitKey.Big) copy();
                    result.or(other.bits);
                    return result;
                }
            }

            throw createException(bitKey);
        }


        @Override
		public BitKey and(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small) {
                final BitKey.Small result = (BitKey.Small) bitKey.copy();
                result.and(bits[0]);
                return result;

            } else if (bitKey instanceof BitKey.Mid128) {
                final BitKey.Mid128 result = (BitKey.Mid128) bitKey.copy();
                result.and(bits[0], bits[1]);
                return result;

            } else if (bitKey instanceof BitKey.Big other) {
                if (other.size() < size()) {
                    final BitKey.Big result = (BitKey.Big) other.copy();
                    result.and(bits);
                    return result;
                } else {
                    final BitKey.Big result = (BitKey.Big) copy();
                    result.and(other.bits);
                    return result;
                }
            }

            throw createException(bitKey);
        }

        @Override
		public BitKey andNot(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                final BitKey.Big result = (BitKey.Big) copy();
                result.andNot(other.bits);
                return result;

            } else if (bitKey instanceof BitKey.Mid128 other) {
                final BitKey.Big result = (BitKey.Big) copy();
                result.andNot(other.bits0, other.bits1);
                return result;

            } else if (bitKey instanceof BitKey.Big other) {
                final BitKey.Big result = (BitKey.Big) copy();
                result.andNot(other.bits);
                return result;
            }

            throw createException(bitKey);
        }

        private void andNot(long[] bits) {
            // bits beyond either capacity are zero; clamping keeps a longer
            // operand from running past this array (and() clamps the same way)
            final int length = Math.min(bits.length, this.bits.length);
            for (int i = 0; i < length; i++) {
                this.bits[i] &= ~bits[i];
            }
            hash = 0;
        }

        private void andNot(long bits0, long bits1) {
            this.bits[0] &= ~bits0;
            this.bits[1] &= ~bits1;
            hash = 0;
        }

        private void andNot(long bits) {
            this.bits[0] &= ~bits;
            hash = 0;
        }

        @Override
		public boolean isSuperSetOf(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                return ((this.bits[0] | other.bits) == this.bits[0]);

            } else if (bitKey instanceof BitKey.Mid128 other) {
                return ((this.bits[0] | other.bits0) == this.bits[0])
                    && ((this.bits[1] | other.bits1) == this.bits[1]);

            } else if (bitKey instanceof BitKey.Big other) {
                int len = Math.min(bits.length, other.bits.length);
                for (int i = 0; i < len; i++) {
                    if ((this.bits[i] | other.bits[i]) != this.bits[i]) {
                        return false;
                    }
                }
                if (other.bits.length > this.bits.length) {
                    for (int i = len; i < other.bits.length; i++) {
                        if (other.bits[i] != 0) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        }

        @Override
		public boolean intersects(BitKey bitKey) {
            if (bitKey instanceof BitKey.Small other) {
                return (this.bits[0] & other.bits) != 0;

            } else if (bitKey instanceof BitKey.Mid128 other) {
                return (this.bits[0] & other.bits0) != 0
                    || (this.bits[1] & other.bits1) != 0;

            } else if (bitKey instanceof BitKey.Big other) {
                int len = Math.min(bits.length, other.bits.length);
                for (int i = 0; i < len; i++) {
                    if ((this.bits[i] & other.bits[i]) != 0) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }

        @Override
		public long[] toLongArray() {
            return java.util.Arrays.copyOf(bits, effectiveSize());
        }


        @Override
		public int nextSetBit(int fromIndex) {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException(
                    "fromIndex < 0: " + fromIndex);
            }

            int u = chunkPos(fromIndex);
            if (u >= bits.length) {
                return -1;
            }
            long word = bits[u] & (-1L << fromIndex);

            while (true) {
                if (word != 0) {
                    return (u * 64) + Long.numberOfTrailingZeros(word);
                }
                if (++u == bits.length) {
                    return -1;
                }
                word = bits[u];
            }
        }

        @Override
		public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof BitKey.Small other) {
                if (this.bits[0] != other.bits) {
                    return false;
                } else {
                    for (int i = 1; i < this.bits.length; i++) {
                        if (this.bits[i] != 0) {
                            return false;
                        }
                    }
                    return true;
                }

            } else if (o instanceof BitKey.Mid128 other) {
                if (this.bits[0] != other.bits0) {
                    return false;
                } else if (this.bits[1] != other.bits1) {
                    return false;
                } else {
                    for (int i = 2; i < this.bits.length; i++) {
                        if (this.bits[i] != 0) {
                            return false;
                        }
                    }
                    return true;
                }

            } else if (o instanceof BitKey.Big other) {
                int len = Math.min(bits.length, other.bits.length);
                for (int i = 0; i < len; i++) {
                    if (this.bits[i] != other.bits[i]) {
                        return false;
                    }
                }
                if (this.bits.length > other.bits.length) {
                    for (int i = len; i < this.bits.length; i++) {
                        if (this.bits[i] != 0) {
                            return false;
                        }
                    }
                } else if (other.bits.length > this.bits.length) {
                    for (int i = len; i < other.bits.length; i++) {
                        if (other.bits[i] != 0) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        }

        @Override
		public int hashCode() {
            // It is important that leading 0s, and bits.length do not affect
            // the hash code. For instance, we want {1} to be equal to
            // {1, 0, 0}. This algorithm in fact ignores all 0s.
            //
            // It is also important that the hash code is the same as produced
            // by Small and Mid128. Cached: wide keys are hot map keys, and a
            // published key is not mutated (see the interface contract).
            int cached = hash;
            if (cached != 0) {
                return cached;
            }
            long h = 1234;
            for (int i = bits.length; --i >= 0;) {
                h ^= bits[i] * (i + 1);
            }
            cached = (int)((h >> 32) ^ h);
            hash = cached;
            return cached;
        }

        @Override
		public String toString() {
            StringBuilder buf = new StringBuilder(64);
            buf.append("0x");
            int start = bits.length * 64 - 1;
            for (int i = start; i >= 0; i--) {
                buf.append((get(i)) ? '1' : '0');
            }
            return buf.toString();
        }

        @Override
		public BitKey copy() {
            return new Big(this);
        }

        @Override
		public BitKey emptyCopy() {
            final Big result = new Big(this);
            result.clear();
            return result;
        }

        @Override
		public boolean isEmpty() {
            for (long bit : bits) {
                if (bit != 0) {
                    return false;
                }
            }
            return true;
        }

        @Override
		public int compareTo(BitKey bitKey) {
            if (bitKey instanceof Big big) {
                return compareUnsignedArrays(this.bits, big.bits);
            } else if (bitKey instanceof Mid128 other) {
                return -other.compareToBig(this);
            } else {
                Small other = (Small) bitKey;
                return -other.compareToBig(this);
            }
        }
    }
}
