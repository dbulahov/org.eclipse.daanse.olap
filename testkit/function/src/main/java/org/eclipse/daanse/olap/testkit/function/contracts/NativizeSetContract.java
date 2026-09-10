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

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.nativizeset.NativizeSetFunDef;

/**
 * The contract of the MDX function {@code NativizeSet(<Set>)} — a query-optimization hint,
 * not a set-transforming function: it asks the engine to try evaluating its argument via a
 * native SQL rewrite, but is defined to return the same members {@code <Set>} alone would.
 *
 * <p>{@code NativizeSetResolver} is an {@code AbstractFunctionDefinitionMultiResolver} with a
 * single declared overload — {@code resolve()} delegates to the generic {@code
 * FunctionMetaDataMatcher.match}, and the resolver's {@code createFunDef} override only ever
 * constructs a fresh {@code NativizeSetFunDef} or returns {@code null} for a metadata that
 * cannot occur (there is exactly one), so there is no path that could throw or diverge from
 * the declared signature.
 *
 * <p>{@code NativizeSetFunDef.createCall} is overridden to walk the resolved call with a
 * {@code FindLevelsVisitor} that records the levels/dimensions involved — this runs when a
 * matched call is turned into a {@code ResolvedFunCall} AST node, a step {@code
 * CallAssert}/{@code EdgeCaseBattery} never reaches (they only ever invoke {@code resolve()}),
 * so it is inert in this test kit regardless of the (cube-free) stub category probed.
 *
 * <p>{@code compileCall} takes the same cube-touching path for every argument shape: for the
 * single-hierarchy sets this test kit's fixture produces ({@code arity == 1}), it always falls
 * back to wrapping the argument's own compiled calc unchanged in a {@code NonNativeListCalc}/
 * {@code NonNativeIterCalc} — a transparent delegate whose {@code evaluateInternal} just calls
 * {@code parent().evaluate(evaluator)} — so {@code NativizeSet([Gender].Members)} evaluates
 * identically to {@code [Gender].Members} alone.
 */
public final class NativizeSetContract {

    private NativizeSetContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("NativizeSet")
            .signatures("<Set> NativizeSet(<Set>)")
            .returns(SET)
            .arity(1, 1)

            .resolvesTo(NativizeSetFunDef.class, SET)
            .resolvesWithCost(2, NativizeSetFunDef.class, MEMBER)   // Member -> Set
            .resolvesWithCost(2, NativizeSetFunDef.class, TUPLE)    // Tuple -> Set
            .resolvesWithCost(1, NativizeSetFunDef.class, LEVEL)    // Level -> Set
            .rejects(DIMENSION)   // Dimension converts to Member/Hierarchy/Level, never Set
            .rejects(HIERARCHY)   // Hierarchy converts to Member/Dimension/Tuple, never Set
            .rejects(NUMERIC)
            .rejects()             // arity 0
            .rejects(SET, SET)     // arity 2

            .autoEdgeCases()
            .edgeCaseMdx("empty set",              "NativizeSet({})")
            .edgeCaseMdx("simple set",              "NativizeSet([Gender].Members)")
            .edgeCaseMdx("member operand (lenient)", "NativizeSet([Gender].[F])")
            .edgeCaseMdx("crossjoined set",          "NativizeSet([Gender].Members * [Measures].[Unit Sales])")

            .value("Count(NativizeSet([Gender].Members))", "3")
            .value("SetToStr(NativizeSet({[Gender].[F]}))", "{[Gender].[Gender].[F]}")

            .dependsOn("NativizeSet([Gender].Members)")

            .resultStyle("NativizeSet([Gender].Members)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("NativizeSet([Gender].Members)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("NativizeSet([Gender].Members)")

            .build();
}
