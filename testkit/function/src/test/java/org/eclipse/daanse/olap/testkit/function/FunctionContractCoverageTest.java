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

import static org.eclipse.daanse.olap.testkit.function.FunctionAssertions.assertThatFunctions;

import org.eclipse.daanse.olap.function.services.standard.StandardFunctions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FunctionContractCoverageTest {

    @Test
    @Disabled("disabled ubtil testkit implementation finished")
    void everyRegisteredFunctionHasAContract() {
        assertThatFunctions(StandardFunctions.standard())
                .coversAllContracts(FunctionContracts.all(), KnownGaps.ALLOWED);
    }

    @Test
    void registryIsSelfConsistent() {
        assertThatFunctions(StandardFunctions.standard())
                .hasNoDuplicateResolverClasses()     // finds F-20 (Cosh twice)
                .hasNoCollidingSignatures()          // finds F-30 (two Val)
                .hasNoBlankDescriptions();
    }
}