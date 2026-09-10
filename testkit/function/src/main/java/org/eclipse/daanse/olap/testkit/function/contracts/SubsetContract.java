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

import static org.eclipse.daanse.olap.api.DataType.LEVEL;
import static org.eclipse.daanse.olap.api.DataType.MEMBER;
import static org.eclipse.daanse.olap.api.DataType.NUMERIC;
import static org.eclipse.daanse.olap.api.DataType.SET;
import static org.eclipse.daanse.olap.api.DataType.STRING;

import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.function.def.subset.SubsetFunDef;

/**
 * The contract of the MDX function {@code Subset(<Set>, <Start>[, <Count>])}. {@code
 * SubsetResolver} is an {@code AbstractFunctionDefinitionMultiResolver} over one declared
 * overload with an optional {@code Count} — {@code resolve()} delegates to the generic {@code
 * FunctionMetaDataMatcher.match}, so there is no hand-written resolver code that could diverge
 * from the declared signature or throw instead of returning empty.
 *
 * <p>{@code SubsetCalc.evaluateInternal} used to unbox {@code start} and {@code count}
 * unconditionally — {@code NullPointerException} for {@code Subset(set, NULL)} or {@code
 * Subset(set, 0, NULL)}, the same null-unboxing bug class fixed repeatedly elsewhere this
 * session (e.g. {@code HeadCalc}, {@code TailCalc}, {@code SetItemCalc}). Fixed: a {@code
 * NULL} {@code Start} or {@code Count} now produces the same empty result the function
 * already returns for an out-of-range {@code Start}, instead of crashing.
 *
 * <p>Integer overflow in {@code end = start + count} for extreme {@code Start}/{@code Count}
 * combinations is a separate, still-unfixed latent issue (silently wraps to an unexpected —
 * but not crashing — empty result) — the same class of gap {@code BottomCountContract}
 * documents for its own negative-count overflow, left as a documented edge case rather than a
 * promise waiver.
 */
public final class SubsetContract {

    private SubsetContract() {
    }

    public static final FunctionContract CONTRACT = FunctionContract.of("Subset")
            .signatures("<Set> Subset(<Set>, <Numeric Expression>, <Numeric Expression>)")
            .returns(SET)
            .arity(2, 3)

            .resolvesTo(SubsetFunDef.class, SET, NUMERIC)
            .resolvesTo(SubsetFunDef.class, SET, NUMERIC, NUMERIC)
            .resolvesWithCost(2, SubsetFunDef.class, MEMBER, NUMERIC)   // Member -> Set
            .resolvesWithCost(1, SubsetFunDef.class, LEVEL, NUMERIC)    // Level -> Set
            .rejects(SET, STRING)              // String does not convert to Numeric
            .rejects(SET)                       // arity 1
            .rejects()                          // arity 0
            .rejects(SET, NUMERIC, NUMERIC, NUMERIC)   // arity 4

            .autoEdgeCases()
            .edgeCaseMdx("start 0, no count",     "Subset([Gender].Members, 0)")
            .edgeCaseMdx("start beyond end",       "Subset([Gender].Members, 100)")
            .edgeCaseMdx("start negative",         "Subset([Gender].Members, -1)")
            .edgeCaseMdx("start MIN_VALUE",        "Subset([Gender].Members, -2147483648)")
            .edgeCaseMdx("start NULL",             "Subset([Gender].Members, NULL)")
            .edgeCaseMdx("count beyond end",       "Subset([Gender].Members, 0, 100)")
            .edgeCaseMdx("count NULL",             "Subset([Gender].Members, 0, NULL)")
            // Documented, currently-inert: end = start + count overflows silently (see the
            // class Javadoc) instead of crashing, but is not asserted precisely here.
            .edgeCaseMdx("count MAX_VALUE (documented overflow gap)",
                    "Subset([Gender].Members, 1, 2147483647)")

            .value("Count(Subset([Gender].Members, 0))", "3")
            .value("Count(Subset([Gender].Members, 1))", "2")
            .value("Subset([Gender].Members, 1).Item(0).Name", "F")
            .value("Count(Subset([Gender].Members, 0, 1))", "1")
            .value("Subset([Gender].Members, 0, 1).Item(0).Name", "All Gender")
            .value("Count(Subset([Gender].Members, 5))", "0")
            .value("Count(Subset([Gender].Members, NULL))", "0")

            .dependsOn("Subset([Gender].Members, 0, 1)")

            .resultStyle("Subset([Gender].Members, 0, 1)", ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST)
            .resultStyle("Subset([Gender].Members, 0, 1)", ResultStyle.ITERABLE, ResultStyle.ITERABLE)
            .independentMutableList("Subset([Gender].Members, 0, 1)")

            .build();
}
