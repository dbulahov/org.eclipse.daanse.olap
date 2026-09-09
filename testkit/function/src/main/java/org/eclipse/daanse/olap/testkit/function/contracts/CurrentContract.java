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
import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.function.def.hierarchy.member.NamedSetCurrentFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX property {@code Current}: {@code <NamedSet>.Current}, the current
 * member or tuple during an iteration through a named set.
 *
 * <p>Two classes declare a resolver for {@code PlainPropertyOperationAtom("Current")} in this
 * codebase, but only one is active: {@code dimension.current.CurrentResolver} has its {@code
 * @Component} annotation commented out and is never registered in {@code
 * StandardFunctions.standard()} — its {@code CurrentFunDef.compileCall} throws {@code
 * UnsupportedOperationException} unconditionally, an unfinished stub. The real, registered
 * implementation is {@code hierarchy.member.NamedSetCurrentFunDef}, resolved by a plain {@code
 * ParametersCheckingFunctionDefinitionResolver} over a single {@code SET} parameter — {@code
 * resolve()} delegates entirely to the generic {@code FunctionMetaDataMatcher.match}, so there
 * is no hand-written resolver code that could diverge from the declared signature or throw
 * instead of returning empty.
 *
 * <p>{@code NamedSetCurrentFunDef.createCall} requires {@code args[0] instanceof
 * NamedSetExpression} — a real AST node type only the parser produces for a genuine {@code
 * WITH SET x AS ...} reference, throwing {@code NotANamedSetException} otherwise (the same
 * precondition {@link AsContract} documents for {@code AsAliasResolver}). Unlike {@code
 * AsAliasResolver}, though, that check lives in {@code createCall} — reached only when a
 * matched call is turned into a {@code ResolvedFunCall} AST node, a step {@code CallAssert}
 * never performs (it only ever calls {@code resolve()}) — so RESOLUTION/SIGNATURE/EDGE need no
 * waiver here; only RESULT/DEPENDENCIES/RESULT_SHAPE do, since verifying them would need a
 * real named set this module cannot construct.
 */
public final class CurrentContract {

    private CurrentContract() {
    }

    private static final String NEEDS_A_REAL_NAMED_SET =
            "NamedSetCurrentFunDef.createCall requires args[0] to be a real NamedSetExpression "
                    + "(a WITH SET x AS ... reference) — a precondition only the parser can satisfy, and "
                    + "'Current' is only meaningful during the iteration a real named set provides. This "
                    + "test kit has no such fixture, and this module never reaches Stage B regardless (no "
                    + "test overrides connection()).";

    public static final FunctionContract CONTRACT = FunctionContract.of("Current")
            .atom(PlainPropertyOperationAtom.class)
            .signatures("<Set>.Current")
            .returns(TUPLE)
            .arity(1, 1)

            .resolvesTo(NamedSetCurrentFunDef.class, SET)
            .resolvesWithCost(2, NamedSetCurrentFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, NamedSetCurrentFunDef.class, TUPLE)    // Tuple -> Set
            .resolvesWithCost(1, NamedSetCurrentFunDef.class, LEVEL)    // Level -> Set
            .rejects(DIMENSION)   // Dimension converts to Member/Hierarchy/Level, never Set
            .rejects(HIERARCHY)   // Hierarchy converts to Member/Dimension/Tuple, never Set
            .rejects(NUMERIC)
            .rejects()             // arity 0
            .rejects(SET, SET)     // arity 2

            .autoEdgeCases()
            // Documented, currently-inert (see the class Javadoc): NotANamedSetException is
            // thrown from createCall, never resolve(), so this never actually runs here.
            .edgeCaseMdx("named set reference (documented gap)",
                    "WITH SET Genders AS [Gender].Members SELECT Genders.Current ON 0 FROM [Sales]")

            .waive(Promise.RESULT, NEEDS_A_REAL_NAMED_SET)
            .waive(Promise.DEPENDENCIES, NEEDS_A_REAL_NAMED_SET)
            .waive(Promise.RESULT_SHAPE, NEEDS_A_REAL_NAMED_SET)

            .build();
}
