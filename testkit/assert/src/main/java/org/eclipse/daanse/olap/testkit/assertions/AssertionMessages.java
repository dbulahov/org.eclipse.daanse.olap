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
package org.eclipse.daanse.olap.testkit.assertions;

import org.opentest4j.AssertionFailedError;

/**
 * The one failure shape used by every assertion in this test kit, mirroring
 * {@code MdxAssert.mismatch} in the rolap test kit.
 *
 * <p>{@code expected} and {@code actual} are always set so that IDEs open their own diff
 * view; the message additionally carries a rendered side-by-side diff for terminals and
 * CI logs, and an explanation of why the mismatch matters.
 */
public final class AssertionMessages {

    private static final String NL = System.lineSeparator();

    private AssertionMessages() {
    }

    public static AssertionFailedError mismatch(String what, String subjectLabel, String subject,
            String expected, String actual) {
        return mismatch(what, subjectLabel, subject, expected, actual, null);
    }

    public static AssertionFailedError mismatch(String what, String subjectLabel, String subject,
            String expected, String actual, String detail) {
        StringBuilder message = new StringBuilder()
                .append(what).append(" did not match").append(NL)
                .append(subjectLabel).append(':').append(NL)
                .append(subject).append(NL)
                .append(NL)
                .append(TextGridDiff.render(expected, actual));
        if (detail != null && !detail.isBlank()) {
            message.append(NL).append(detail.stripTrailing()).append(NL);
        }
        return new AssertionFailedError(message.toString(), expected, actual);
    }

    /** A one-line failure without a diff; expected/actual are still carried. */
    public static AssertionFailedError failed(String message, String expected, String actual) {
        return new AssertionFailedError(message, expected, actual);
    }

    public static AssertionFailedError failed(String message, String expected, String actual,
            Throwable cause) {
        return new AssertionFailedError(message, expected, actual, cause);
    }
}