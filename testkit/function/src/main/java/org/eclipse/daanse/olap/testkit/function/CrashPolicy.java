/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.testkit.function;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;

/** Decides whether an exception thrown by an edge case is diagnosed or accidental. */
public final class CrashPolicy {

    private static final List<Class<? extends Throwable>> ALWAYS_FORBIDDEN = List.of(
            NullPointerException.class,
            ClassCastException.class,
            IndexOutOfBoundsException.class,
            NumberFormatException.class,
            ArithmeticException.class,
            UnsupportedOperationException.class,
            java.util.ConcurrentModificationException.class,
            StackOverflowError.class,
            OutOfMemoryError.class);

    private final List<Class<? extends Throwable>> allowed = new ArrayList<>();
    private final List<Class<? extends Throwable>> forbidden = new ArrayList<>(ALWAYS_FORBIDDEN);

    private CrashPolicy() {
    }

    public static CrashPolicy standard() {
        return new CrashPolicy();
    }

    @SafeVarargs
    public final CrashPolicy allowingAdditionally(Class<? extends Throwable>... additional) {
        allowed.addAll(List.of(additional));
        return this;
    }

    @SafeVarargs
    public final CrashPolicy forbiddingAdditionally(Class<? extends Throwable>... additional) {
        forbidden.addAll(List.of(additional));
        return this;
    }

    /**
     * Diagnosed = an {@link OlapRuntimeException} with a non-blank message, or a
     * cancellation/limit exception, or an explicitly allowed type. A blank message is not
     * diagnosed: it tells the user nothing.
     */
    public boolean isDiagnosed(Throwable thrown) {
        if (allowed.stream().anyMatch(c -> c.isInstance(thrown))) {
            return true;
        }
        if (forbidden.stream().anyMatch(c -> c.isInstance(thrown))) {
            return false;
        }
        if (thrown instanceof OlapRuntimeException) {
            return thrown.getMessage() != null && !thrown.getMessage().isBlank();
        }
        return false;
    }

    /** Why it is not diagnosed — goes straight into the failure message. */
    public String explain(Throwable thrown) {
        if (forbidden.stream().anyMatch(c -> c.isInstance(thrown))) {
            return """
                    %s is a JDK runtime exception that escaped by accident. An illegal input must be
                    rejected deliberately, with a message that names the argument and its value:
                        throw new InvalidArgumentException("Head: count must not be negative, was " + n);
                    """.formatted(thrown.getClass().getName());
        }
        if (thrown instanceof OlapRuntimeException) {
            return """
                    %s carries a blank message. An OlapRuntimeException without text is as unhelpful
                    as a NullPointerException: the user sees a failure and learns nothing.
                    """.formatted(thrown.getClass().getName());
        }
        return """
                %s is neither an OlapRuntimeException nor a cancellation/limit exception.
                Only deliberate, diagnosed failures are permitted here.
                """.formatted(thrown.getClass().getName());
    }
}