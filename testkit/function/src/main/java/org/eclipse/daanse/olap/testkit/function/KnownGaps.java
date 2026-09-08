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

import java.util.Set;

/**
 * Functions that have no contract yet. The set may only shrink: the coverage meta test
 * fails both when a registered function is missing from it and when an entry here already
 * has a contract.
 */
public final class KnownGaps {

    /** Key format is {@code "Name (AtomClassSimpleName)"} — see {@code FunctionContract.key()}. */
    public static final Set<String> ALLOWED = Set.of(
            "NativizeSet (FunctionOperationAtom)",   // 1982 LOC, needs the native SQL path
            "Parameter (FunctionOperationAtom)",     // needs Query.createOrLookupParam
            "ParamRef (FunctionOperationAtom)"       // ditto
            // ... shrinking from 241 to 0
    );

    private KnownGaps() {
    }
}