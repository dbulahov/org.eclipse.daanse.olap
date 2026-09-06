/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.olap.api.result;

import org.eclipse.daanse.olap.api.element.Member;

public interface WritebackCell {

    double getNewValue();

    double getCurrentValue();

    AllocationPolicy getAllocationPolicy();

    double getAtomicCellCount();

    /**
     * Returns the amount by which the cell value has increased with this override.
     *
     * @return Amount by which value has increased
     */
    double getOffset();

    CellRelation getRelationTo(Member[] members);

    /**
     * Decribes the relationship between two cells.
     */
    enum CellRelation {
        ABOVE, EQUAL, BELOW, NONE
    }

}