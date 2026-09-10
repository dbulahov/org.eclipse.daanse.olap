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
import org.eclipse.daanse.olap.function.def.properties.PropertiesFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX method {@code <Member>.Properties(<String>)}, which returns the value
 * of a named member property. Unlike every other atom in this suite, {@code PropertiesResolver}
 * is a hand-written {@code FunctionResolver} rather than a {@code
 * ParametersCheckingFunctionDefinitionResolver}/{@code AbstractFunctionDefinitionMultiResolver}
 * wrapper — but its {@code resolve()} is still a pure predicate: it checks arity and per-argument
 * {@code Validator.canConvert} exactly like {@code FunctionMetaDataMatcher.match} does, then
 * calls {@code deducePropertyCategory(args[0], args[1])} to pick the returned {@code
 * PropertiesFunDef}'s declared return category. That method never throws for generic
 * stub-based probing: it returns {@code DataType.VALUE} immediately whenever the property-name
 * argument is not a string {@code Literal}, or whenever the member argument's type has no
 * resolvable hierarchy — both true for every stub this test kit can construct — so RESOLUTION/
 * SIGNATURE/EDGE need no waiver (the same "resolve() is safe by construction" shape as the KPI
 * accessor family), and the same {@code MEMBER}/{@code STRING} cost ladders {@link
 * FirstChildContract} (receiver) and {@link LenContract} (string argument) already establish
 * apply unchanged.
 *
 * <p>Unlike a real {@code "[Gender].[F].Properties(\"MEMBER_NAME\")"} call — where the parser
 * supplies a literal property name and a member expression with a resolvable hierarchy, so
 * {@code deducePropertyCategory} looks up the real property and returns its concrete category
 * ({@code STRING} for {@code MEMBER_NAME}) — the declared return category asserted below ({@code
 * VALUE}) reflects only the generic stub-probing outcome; the two are unrelated promises (Stage A
 * declared category vs. Stage B's real compiled/evaluated type).
 *
 * <p>{@code PropertiesCalc.evaluateInternal} calls {@code member.getPropertyValue(name,
 * matchCase)}, falling back to {@code Util.isValidProperty} to throw a diagnosed {@code
 * DaanseEvaluationException} for a name that is not a recognized property of the member's level
 * — an unrecognized property name is exercised as an edge case, not a value assertion, for
 * exactly that reason. {@code MEMBER_NAME} is a {@code StandardProperty} every member answers
 * (the same standard-property family {@code MEMBER_CAPTION} belongs to, which {@link
 * CaptionContract}/{@link NameContract} decline to assert a value for since the underlying
 * {@code OlapElement.getCaption()}/{@code getName()} accessors have no default implementation in
 * this repository), so RESULT is asserted here instead of waived.
 */
public final class PropertiesContract {

    private PropertiesContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Properties")
            .atom(MethodOperationAtom.class)
            .signatures("<Value> <Member>.Properties(<String>)")
            .returns(VALUE)
            .arity(2, 2)

            .resolvesTo(PropertiesFunDef.class, MEMBER, STRING)
            .resolvesWithCost(1, PropertiesFunDef.class, HIERARCHY, STRING)   // Hierarchy -> Member
            .resolvesWithCost(2, PropertiesFunDef.class, DIMENSION, STRING)   // Dimension -> Member
            .rejects(LEVEL, STRING)     // Level does not convert to Member
            .rejects(SET, STRING)
            .rejects(TUPLE, STRING)     // Tuple does not convert to Member
            .rejects(NUMERIC, STRING)

            .resolvesWithCost(2, PropertiesFunDef.class, MEMBER, VALUE)      // Value -> String, cost 2
            .resolvesWithCost(4, PropertiesFunDef.class, MEMBER, MEMBER)     // Member -> String, cost 4
            .resolvesWithCost(4, PropertiesFunDef.class, MEMBER, TUPLE)      // Tuple -> String, cost 4
            .rejects(MEMBER, NUMERIC)   // Numeric does not convert to String
            .rejects(MEMBER, SET)       // Set converts to nothing

            .rejects()                          // arity 0
            .rejects(MEMBER)                     // arity 1
            .rejects(MEMBER, STRING, STRING)     // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("known standard property",       "[Gender].[F].Properties(\"MEMBER_NAME\")")
            .edgeCaseMdx("hierarchy/dimension reference",  "[Gender].Properties(\"MEMBER_NAME\")")
            .edgeCaseMdx("property name NULL",             "[Gender].[F].Properties(NULL)")
            // Documented: Util.isValidProperty declines it and PropertiesCalc.evaluateInternal
            // throws a diagnosed DaanseEvaluationException — an allowed edge-case outcome (see
            // the class Javadoc), not a crash.
            .edgeCaseMdx("unknown property name (documented, diagnosed exception)",
                    "[Gender].[F].Properties(\"NoSuchProperty\")")

            .value("[Gender].[F].Properties(\"MEMBER_NAME\")", "F")

            .scalarDependsOn("[Gender].[F].Properties(\"MEMBER_NAME\")")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")

            .build();
}
