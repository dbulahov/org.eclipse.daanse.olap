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
package org.eclipse.daanse.olap.testkit.function.integration;

import static org.eclipse.daanse.olap.testkit.function.FunctionAssertions.assertThatFunctions;

import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.function.services.standard.StandardFunctions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.osgi.test.common.annotation.InjectService;

@Disabled("disabled ubtil testkit implementation finished")
class RegistryDriftTest {

    @Test
    void osgiAndHandRegistryAgree(@InjectService FunctionService osgi) {
        assertThatFunctions(osgi).matches(StandardFunctions.standard());   // finds F-01
    }
}