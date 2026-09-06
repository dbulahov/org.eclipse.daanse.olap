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
import java.util.stream.Collectors;

/**
 * Typed, dialect-free compound predicate of a segment. Part of the wire form
 * and of the {@link SegmentHeader#getUniqueID() unique id}: equal logical
 * predicates carry equal values and produce the same {@link #canonical()}
 * text on every node and every database dialect. Producers canonicalize
 * (sorted list values, sorted And/Or children) before construction.
 *
 * The engine reconstructs its runtime predicates from the structural forms;
 * {@link Opaque} carries only canonical identity text for predicate shapes
 * without a structural mapping.
 */
public sealed interface SegmentPredicate extends Serializable
        permits SegmentPredicate.Value, SegmentPredicate.Values, SegmentPredicate.Range,
        SegmentPredicate.And, SegmentPredicate.Or, SegmentPredicate.Not, SegmentPredicate.Literal,
        SegmentPredicate.Opaque {

    /** Deterministic text form, for display and {@link Opaque} identity. */
    String canonical();

    /**
     * Structural digestion: emits a type tag and every field as separate
     * tokens (values as class name + string). The unique-id digest feeds
     * each token through a length-safe update - unlike a flat
     * {@link #canonical()} string, no separator inside an expression or
     * value can make two different predicates digest identically.
     */
    default void digest(java.util.function.Consumer<String> sink) {
        switch (this) {
        case Value v -> {
            sink.accept("V");
            sink.accept(v.columnExpression());
            digestValue(sink, v.value());
        }
        case Values vs -> {
            sink.accept("VS");
            sink.accept(vs.columnExpression());
            sink.accept(String.valueOf(vs.values().size()));
            for (Comparable value : vs.values()) {
                digestValue(sink, value);
            }
        }
        case Range r -> {
            sink.accept("R");
            sink.accept(r.columnExpression());
            digestValue(sink, r.lower());
            sink.accept(String.valueOf(r.lowerInclusive()));
            digestValue(sink, r.upper());
            sink.accept(String.valueOf(r.upperInclusive()));
        }
        case And and -> {
            sink.accept("AND");
            sink.accept(String.valueOf(and.children().size()));
            for (SegmentPredicate child : and.children()) {
                child.digest(sink);
            }
        }
        case Or or -> {
            sink.accept("OR");
            sink.accept(String.valueOf(or.children().size()));
            for (SegmentPredicate child : or.children()) {
                child.digest(sink);
            }
        }
        case Not not -> {
            sink.accept("NOT");
            not.child().digest(sink);
        }
        case Literal l -> {
            sink.accept("L");
            sink.accept(String.valueOf(l.value()));
        }
        case Opaque o -> {
            sink.accept("O");
            sink.accept(o.canonicalForm());
        }
        }
    }

    private static void digestValue(java.util.function.Consumer<String> sink, Comparable value) {
        if (value == null) {
            sink.accept("0");
        } else {
            sink.accept(value.getClass().getName());
            sink.accept(String.valueOf(value));
        }
    }

    /** column = value; a null value means IS NULL. */
    record Value(String columnExpression, Comparable value) implements SegmentPredicate {
        public Value {
            if (columnExpression == null) {
                throw new IllegalArgumentException("columnExpression");
            }
        }

        @Override
        public String canonical() {
            return columnExpression + "=" + value;
        }
    }

    /** column in {values}; values are canonically sorted, may contain null. */
    record Values(String columnExpression, List<Comparable> values) implements SegmentPredicate {
        public Values {
            if (columnExpression == null) {
                throw new IllegalArgumentException("columnExpression");
            }
            // Stream.toList: immutable and null-tolerant (IS NULL keys)
            values = values.stream().toList();
        }

        @Override
        public String canonical() {
            return columnExpression + " in ("
                    + values.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
        }
    }

    /** lower/upper bound on one column; a null bound is open. */
    record Range(String columnExpression, Comparable lower, boolean lowerInclusive, Comparable upper,
            boolean upperInclusive) implements SegmentPredicate {
        public Range {
            if (columnExpression == null) {
                throw new IllegalArgumentException("columnExpression");
            }
        }

        @Override
        public String canonical() {
            return columnExpression + (lower == null ? " (" : (lowerInclusive ? " [" : " (") + lower) + ";"
                    + (upper == null ? ")" : upper + (upperInclusive ? "]" : ")"));
        }
    }

    record And(List<SegmentPredicate> children) implements SegmentPredicate {
        public And {
            children = List.copyOf(children);
        }

        @Override
        public String canonical() {
            return children.stream().map(SegmentPredicate::canonical)
                    .collect(Collectors.joining(" and ", "(", ")"));
        }
    }

    record Or(List<SegmentPredicate> children) implements SegmentPredicate {
        public Or {
            children = List.copyOf(children);
        }

        @Override
        public String canonical() {
            return children.stream().map(SegmentPredicate::canonical)
                    .collect(Collectors.joining(" or ", "(", ")"));
        }
    }

    record Not(SegmentPredicate child) implements SegmentPredicate {
        @Override
        public String canonical() {
            return "not (" + child.canonical() + ")";
        }
    }

    /** Constant truth value. */
    record Literal(boolean value) implements SegmentPredicate {
        @Override
        public String canonical() {
            return String.valueOf(value);
        }
    }

    /**
     * Identity-only carrier for predicate shapes without a structural
     * mapping; holds their canonical dialect-free rendering.
     */
    record Opaque(String canonicalForm) implements SegmentPredicate {
        public Opaque {
            if (canonicalForm == null) {
                throw new IllegalArgumentException("canonicalForm");
            }
        }

        @Override
        public String canonical() {
            return canonicalForm;
        }
    }
}
