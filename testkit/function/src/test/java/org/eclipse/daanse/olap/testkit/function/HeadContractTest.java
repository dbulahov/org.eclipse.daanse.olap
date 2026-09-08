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

import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.testkit.function.FunctionAssertions.assertThatFunction;

import org.eclipse.daanse.olap.function.def.headtail.HeadResolver;
import org.eclipse.daanse.olap.function.def.headtail.HeadTailFunDef;
import org.eclipse.daanse.olap.function.def.headtail.TailResolver;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.HeadContract;
import org.junit.jupiter.api.Test;

class HeadContractTest extends AbstractFunctionContractTest {

    @Override
    protected FunctionContract contract() {
        return HeadContract.CONTRACT;
    }

    /** Head and Tail share one FunDef but not one atom. */
    @Test
    void headAndTailShareOneDefinitionButNotOneAtom() {
        assertThatFunction(functionService(), "Head").calledWith(SET, NUMERIC)
                .resolvesTo(HeadTailFunDef.class)
                .resolvesVia(HeadResolver.class);
        assertThatFunction(functionService(), "Tail").calledWith(SET, NUMERIC)
                .resolvesTo(HeadTailFunDef.class)
                .resolvesVia(TailResolver.class);
    }
}