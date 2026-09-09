/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 */
package org.eclipse.daanse.olap.testkit.function.contracts;

import static org.eclipse.daanse.olap.api.DataType.DIMENSION;
import static org.eclipse.daanse.olap.api.DataType.HIERARCHY;
import static org.eclipse.daanse.olap.api.DataType.INTEGER;
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.function.def.member.namedsetcurrentordinal.NamedSetCurrentOrdinalFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code CurrentOrdinal}: {@code <NamedSet>.CurrentOrdinal},
 * the zero-based ordinal of the current iteration through a named set. The same shape as
 * {@link CurrentContract} — see that contract's Javadoc for the full reasoning — except there
 * is no disabled sibling resolver here; {@code NamedSetCurrentOrdinalFunDef} is the only
 * implementation, resolved by a plain {@code ParametersCheckingFunctionDefinitionResolver}
 * over a single {@code SET} parameter, so {@code resolve()} is a pure predicate.
 *
 * <p>{@code NamedSetCurrentOrdinalFunDef.createCall} requires {@code args[0] instanceof
 * NamedSetExpression} — a real AST node type only the parser produces for a genuine {@code
 * WITH SET x AS ...} reference, throwing {@code NotANamedSetException} otherwise. That check
 * lives in {@code createCall}, never {@code resolve()}, so RESOLUTION/SIGNATURE/EDGE need no
 * waiver; only RESULT/DEPENDENCIES/RESULT_SHAPE do, since verifying them would need a real
 * named set this module cannot construct.
 */
public final class CurrentOrdinalContract {

    private CurrentOrdinalContract() {
    }

    private static final String NEEDS_A_REAL_NAMED_SET =
            "NamedSetCurrentOrdinalFunDef.createCall requires args[0] to be a real "
                    + "NamedSetExpression (a WITH SET x AS ... reference) — a precondition only the "
                    + "parser can satisfy, and 'CurrentOrdinal' is only meaningful during the iteration "
                    + "a real named set provides. This test kit has no such fixture, and this module "
                    + "never reaches Stage B regardless (no test overrides connection()).";

    public static final FunctionContract CONTRACT = FunctionContract.of("CurrentOrdinal")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Set>.CurrentOrdinal")
            .returns(INTEGER)
            .arity(1, 1)

            .resolvesTo(NamedSetCurrentOrdinalFunDef.class, SET)
            .resolvesWithCost(2, NamedSetCurrentOrdinalFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, NamedSetCurrentOrdinalFunDef.class, TUPLE)    // Tuple -> Set
            .resolvesWithCost(1, NamedSetCurrentOrdinalFunDef.class, LEVEL)    // Level -> Set
            .rejects(DIMENSION)   // Dimension converts to Member/Hierarchy/Level, never Set
            .rejects(HIERARCHY)   // Hierarchy converts to Member/Dimension/Tuple, never Set
            .rejects(NUMERIC)
            .rejects()             // arity 0
            .rejects(SET, SET)     // arity 2

            .autoEdgeCases()
            // Documented, currently-inert (see the class Javadoc): NotANamedSetException is
            // thrown from createCall, never resolve(), so this never actually runs here.
            .edgeCaseMdx("named set reference (documented gap)",
                    "WITH SET Genders AS [Gender].Members SELECT Genders.CurrentOrdinal ON 0 FROM [Sales]")

            .waive(Promise.RESULT, NEEDS_A_REAL_NAMED_SET)
            .waive(Promise.DEPENDENCIES, NEEDS_A_REAL_NAMED_SET)
            .waive(Promise.RESULT_SHAPE, NEEDS_A_REAL_NAMED_SET)

            .build();
}
