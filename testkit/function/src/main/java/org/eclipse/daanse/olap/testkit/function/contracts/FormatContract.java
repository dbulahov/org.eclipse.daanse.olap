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

import static org.eclipse.daanse.olap.api.DataType.DATE_TIME;
import static org.eclipse.daanse.olap.api.DataType.INTEGER;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;

import org.eclipse.daanse.olap.function.def.format.FormatFunDef;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;

/**
 * The contract of the MDX function {@code Format}. Three overloads share the atom, all
 * {@code (Value, Format)} pairs distinguished only by the Value category — Member, Numeric or
 * DateTime — and all returning String. {@code org.eclipse.daanse.olap.util.format.Format}
 * (the VB-{@code Format()}-compatible mask engine) is explicitly null-safe: its constructor
 * does {@code if (formatString == null) formatString = ""}, and an empty mask falls back to a
 * locale-default {@code JavaFormat} — so a literal NULL Format argument behaves exactly like
 * an empty one, not a crash.
 *
 * <p>{@code FormatFunDef.compileCall} takes two different paths depending on whether the
 * Format argument is a literal: a constant mask compiles once into a {@code
 * FormatLiteralCalc} whose only child is the Value calc, while a variable (computed) mask
 * compiles into a {@code FormatCalc} with a second child for the mask itself — so a
 * non-literal Format expression's own hierarchy dependencies are reported too, not just the
 * Value's.
 *
 * <p>Neither {@code FormatLiteralCalc} nor {@code FormatCalc} overrides {@code dependsOn}; both
 * use the generic "depends on hierarchy if any child calc does" walk. A literal member Value
 * argument like {@code [Measures].[Unit Sales]} is coerced to a scalar via an implicit {@code
 * MemberValueCalc}-style wrapper, which — the same "depends on everything except the hierarchy
 * it fixes" shape {@link ValueContract}/{@link MinusContract} document — therefore does
 * <em>not</em> depend on {@code Measures} itself, but does depend on every other hierarchy in
 * the cube. A dynamic reference like {@code [Gender].CurrentMember.Name} has no such fixed
 * hierarchy and genuinely does depend on {@code Gender}, asserted below unchanged.
 */
public final class FormatContract {

    private FormatContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Format")
            .signatures(
                    "<String> Format(<Member>, <String>)",
                    "<String> Format(<Numeric Expression>, <String>)",
                    "<String> Format(<DateTime>, <String>)")
            .returns(STRING)
            .arity(2, 2)

            .resolvesTo(FormatFunDef.class, MEMBER, STRING)
            .resolvesTo(FormatFunDef.class, NUMERIC, STRING)
            .resolvesTo(FormatFunDef.class, DATE_TIME, STRING)
            .resolvesTo(FormatFunDef.class, INTEGER, STRING)   // Integer -> Numeric, free
            .rejects(SET, STRING)          // Set converts to none of Member/Numeric/DateTime
            .rejects(NUMERIC, NUMERIC)      // Numeric does not convert to String
            .rejects()                      // arity 0
            .rejects(NUMERIC, STRING, STRING)   // arity 3

            .autoEdgeCases()
            .edgeCaseMdx("empty format string",                  "Format(1234, \"\")")
            .edgeCaseMdx("whitespace-only format string",        "Format(1234, \"   \")")
            .edgeCaseMdx("format string with brackets and ampersand", "Format(1234, \"[a]b&c\")")
            .edgeCaseMdx("null format string",                   "Format(1234, NULL)")
            .edgeCaseMdx("member value",                         "Format([Measures].[Unit Sales], \"#,##0\")")
            .edgeCaseMdx("date value",                           "Format(Now(), \"yyyy\")")
            .edgeCaseMdx("variable (non-literal) format string", "Format(1234, [Gender].CurrentMember.Name)")

            // "#,##0" and "0.00" are the two most standard VB-Format masks — thousands
            // grouping and fixed decimal padding — documented by name in Format's own class
            // Javadoc example.
            .value("Format(1234, \"#,##0\")", "1,234")
            .value("Format(3, \"0.00\")", "3.00")
            // Reuses the fact established by SumContract: Unit Sales summed over [Gender] is
            // 266,773; a Member value formats its current cell value, same as a plain scalar
            // reference to it would.
            .value("Format([Measures].[Unit Sales], \"#,##0\")", "266,773")

            .scalarDoesNotDependOn("Format([Measures].[Unit Sales], \"#,##0\")", "[Measures]")
            .scalarDependsOn("Format(1234, [Gender].CurrentMember.Name)", "[Gender].[Gender]")

            .waive(Promise.RESULT_SHAPE,
                    "scalar function; ResultStyle is VALUE by construction")
            .build();
}
