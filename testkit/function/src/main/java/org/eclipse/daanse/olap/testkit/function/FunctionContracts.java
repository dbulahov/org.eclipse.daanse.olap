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

import java.util.List;

import org.eclipse.daanse.olap.testkit.function.contracts.AbsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AddCalculatedMembersContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AllMembersContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AscendantsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.BottomCountContract;
import org.eclipse.daanse.olap.testkit.function.contracts.BottomPercentContract;
import org.eclipse.daanse.olap.testkit.function.contracts.BottomSumContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ChildrenContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CurrentMemberContract;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.HeadContract;
import org.eclipse.daanse.olap.testkit.function.contracts.MinusContract;
import org.eclipse.daanse.olap.testkit.function.contracts.RangeContract;
import org.eclipse.daanse.olap.testkit.function.contracts.StarContract;
import org.eclipse.daanse.olap.testkit.function.contracts.SumContract;

/**
 * The enumeration of all contracts. Hand-maintained, exactly like
 * {@code StandardFunctions.standard()} — and for the same reason: subclasses cannot be
 * enumerated without classpath scanning, and there is no scanner in the build.
 */
public final class FunctionContracts {

    private FunctionContracts() {
    }

    public static List<FunctionContract> all() {
        return List.of(
                AbsContract.CONTRACT,
                AddCalculatedMembersContract.CONTRACT,
                AllMembersContract.CONTRACT,
                AsContract.CONTRACT,
                AscendantsContract.CONTRACT,
                BottomCountContract.CONTRACT,
                BottomPercentContract.CONTRACT,
                BottomSumContract.CONTRACT,
                ChildrenContract.CONTRACT,
                CurrentMemberContract.CONTRACT,
                HeadContract.CONTRACT,
                MinusContract.CONTRACT,
                RangeContract.CONTRACT,
                StarContract.CONTRACT,
                SumContract.CONTRACT
                // ... one line per function
        );
    }
}