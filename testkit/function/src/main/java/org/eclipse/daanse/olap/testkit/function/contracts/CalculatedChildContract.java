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
import static org.eclipse.daanse.olap.api.DataType.STRING;
import static org.eclipse.daanse.olap.api.DataType.TUPLE;
import static org.eclipse.daanse.olap.api.DataType.VALUE;

import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.olap.function.def.nonstandard.CalculatedChildFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the non-standard MDX method {@code <Member>.CalculatedChild(<String>)}, a
 * Mondrian/Daanse extension (not part of the MDX specification) that looks up an
 * <em>existing</em> calculated member named {@code <String>} among the given member's children
 * — it does not create one. {@code CalculatedChildFunDef} is resolved by a plain {@code
 * ParametersCheckingFunctionDefinitionResolver} over two required parameters ({@code MEMBER},
 * {@code STRING}) — {@code resolve()} delegates entirely to the generic {@code
 * FunctionMetaDataMatcher.match}, so there is no hand-written resolver code that could diverge
 * from the declared signature or throw instead of returning empty. The {@code MEMBER}/{@code
 * STRING} cost ladders match {@link PropertiesContract} (same two parameter categories, same
 * order).
 *
 * <p>{@code CalculatedChildCalc.getCalculatedChild} falls back to {@code
 * parent.getHierarchy().getNullMember()} whenever the parent has no child level at all (a leaf
 * member) or no calculated member at that level matches both the given name and parent — the
 * name-matching case needs a real {@code WITH MEMBER} clause in the query, which the shared
 * {@code MdxValues}/{@code CalcAssertions} single-expression helpers cannot embed (their
 * generated wrapper already owns the query's one {@code WITH MEMBER [Measures].[Foo]} clause);
 * it is instead exercised directly against a real {@code Connection} in a supplementary
 * {@code @Test}, the same shape {@code SumContractTest.bindsTheHierarchiesOfItsSetArgument}
 * already uses for a promise beyond the standard eight.
 *
 * <p>Since {@code CalculatedChild} returns a {@code Member} of the same hierarchy as its
 * receiver, a cell-context caller implicitly wraps the call in a {@code MemberValueCalc} to
 * read its value — the same "depends on everything except the hierarchy the wrapped expression
 * fixes" shape {@link ValueContract}/{@link ValidMeasureContract} document for their own
 * {@code Member}-fixing arguments. {@code
 * [Gender].[Gender].[F].CalculatedChild("NoSuchCalcMember")} therefore depends on every
 * hierarchy in the cube <em>except</em> {@code Gender}, the receiver's own hierarchy — asserted
 * below with {@code scalarDoesNotDependOn} only (a positive {@code scalarDependsOn} would need
 * every other hierarchy in the fixture cube listed to satisfy {@code dependsOnExactly}, brittle
 * and not the promise that matters — see {@code Builder.scalarDoesNotDependOn}'s own Javadoc:
 * "the interesting direction for context-setting functions").
 */
public final class CalculatedChildContract {

    private CalculatedChildContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("CalculatedChild")
            .atom(MethodOperationAtom.class)
            .signatures("<Member> <Member>.CalculatedChild(<String>)")
            .returns(MEMBER)
            .arity(2, 2)

            .resolvesTo(CalculatedChildFunDef.class, MEMBER, STRING)
            .resolvesWithCost(1, CalculatedChildFunDef.class, HIERARCHY, STRING)   // Hierarchy -> Member
            .resolvesWithCost(2, CalculatedChildFunDef.class, DIMENSION, STRING)   // Dimension -> Member
            .rejects(LEVEL, STRING)     // Level does not convert to Member
            .rejects(SET, STRING)
            .rejects(TUPLE, STRING)     // Tuple does not convert to Member
            .rejects(NUMERIC, STRING)

            .resolvesWithCost(2, CalculatedChildFunDef.class, MEMBER, VALUE)      // Value -> String
            .resolvesWithCost(4, CalculatedChildFunDef.class, MEMBER, MEMBER)     // Member -> String
            .resolvesWithCost(4, CalculatedChildFunDef.class, MEMBER, TUPLE)      // Tuple -> String
            .rejects(MEMBER, NUMERIC)   // Numeric does not convert to String
            .rejects(MEMBER, SET)       // Set converts to nothing

            .rejects()                          // arity 0
            .rejects(MEMBER)                     // arity 1
            .rejects(MEMBER, VALUE, VALUE)       // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("leaf member (no child level, null result)",
                    "[Gender].[Gender].[F].CalculatedChild(\"NoSuchCalcMember\")")
            .edgeCaseMdx("name NULL", "[Gender].[Gender].[F].CalculatedChild(NULL)")

            // A leaf member has no child level at all, so CalculatedChild falls back to the
            // null-member sentinel regardless of the name given — no real calculated member
            // needs to exist for this case (see the class Javadoc for the one that does).
            .value("([Gender].[Gender].[F].CalculatedChild(\"NoSuchCalcMember\") "
                    + "IS [Gender].[Gender].[F].Parent.Parent)", "true")

            .scalarDoesNotDependOn("[Gender].[Gender].[F].CalculatedChild(\"NoSuchCalcMember\")", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "returns a Member, not a set; the Set ResultStyle promise does not apply")

            .build();
}
