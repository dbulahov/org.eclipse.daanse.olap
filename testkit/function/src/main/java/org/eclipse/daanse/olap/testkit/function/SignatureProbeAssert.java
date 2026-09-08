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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.function.core.resolver.FunctionMetaDataMatcher;
import org.eclipse.daanse.olap.testkit.assertions.AssertionMessages;
import org.eclipse.daanse.olap.testkit.function.stub.ResolutionOnlyValidator;
import org.eclipse.daanse.olap.testkit.function.stub.TypedExpressionStub;

/**
 * Compares what a function <em>declares</em> against what it <em>accepts</em>.
 *
 * <p>For every argument tuple in a probe space, the resolver must accept the call exactly
 * when {@link FunctionMetaDataMatcher} accepts it against the declared overloads. A call
 * accepted only by the matcher means MDSCHEMA_FUNCTIONS advertises a signature that does
 * not work; a call accepted only by the resolver means clients cannot discover it.
 */
public final class SignatureProbeAssert {

    /** The 14 categories a {@link TypedExpressionStub} can represent. */
    private static final DataType[] DEFAULT_PROBES = {
            DataType.SET, DataType.TUPLE, DataType.MEMBER, DataType.LEVEL, DataType.HIERARCHY,
            DataType.DIMENSION, DataType.NUMERIC, DataType.INTEGER, DataType.STRING,
            DataType.LOGICAL, DataType.DATE_TIME, DataType.SYMBOL, DataType.VALUE, DataType.NULL };

    private final FunctionService functionService;
    private final OperationAtom atom;
    private DataType[] probes = DEFAULT_PROBES;
    private int minArity = 0;
    private int maxArity = 3;

    SignatureProbeAssert(FunctionService functionService, OperationAtom atom) {
        this.functionService = functionService;
        this.atom = atom;
    }

    public SignatureProbeAssert overCategories(DataType... probeCategories) {
        this.probes = probeCategories;
        return this;
    }

    public SignatureProbeAssert overArities(int minInclusive, int maxInclusive) {
        this.minArity = minInclusive;
        this.maxArity = maxInclusive;
        return this;
    }

    public SignatureProbeAssert isConsistent() {
        return isConsistentExceptFor();
    }

    /** {@code exemptCallSignatures} are calls the resolver may accept beyond its declaration. */
    public SignatureProbeAssert isConsistentExceptFor(String... exemptCallSignatures) {
        Set<String> exempt = Set.of(exemptCallSignatures);
        Set<String> declaredAccepts = new LinkedHashSet<>();
        Set<String> resolverAccepts = new LinkedHashSet<>();
        List<FunctionMetaData> declared = declaredMetaData();

        forEachProbe(args -> {
            String signature = SignatureText.ofCall(atom, args);
            if (declared.stream().anyMatch(m -> FunctionMetaDataMatcher
                    .match(m, args, ResolutionOnlyValidator.inSetContext(functionService)).isPresent())) {
                declaredAccepts.add(signature);
            }
            for (FunctionResolver resolver : functionService.getResolvers(atom)) {
                if (resolver.resolve(args, ResolutionOnlyValidator.inSetContext(functionService)).isPresent()) {
                    resolverAccepts.add(signature);
                    break;
                }
            }
        });

        resolverAccepts.removeAll(exempt);
        declaredAccepts.removeAll(exempt);
        String expected = SignatureText.lines(List.copyOf(declaredAccepts));
        String actual = SignatureText.lines(List.copyOf(resolverAccepts));
        if (!expected.equals(actual)) {
            throw AssertionMessages.mismatch("accepted calls vs. declared signature",
                    "Function", SignatureText.ofAtom(atom), expected, actual,
                    """
                    left  = calls accepted by FunctionMetaDataMatcher over the %d declared overload(s)
                    right = calls accepted by the registered resolver(s)

                    A call on the left only  -> getFunctionMetaDatas()/MDSCHEMA_FUNCTIONS advertises a
                                                signature that does not work.
                    A call on the right only -> the resolver accepts a call it never documented;
                                                clients cannot discover it.
                    """.formatted(declared.size()));
        }
        return this;
    }

    /** No {@code resolve} call over the probe space may throw. Promise 6, wholesale. */
    public SignatureProbeAssert neverThrows() {
        forEachProbe(args -> new CallAssert(functionService, atom, args, false).resolutionDoesNotThrow());
        return this;
    }

    // ---- internals ---------------------------------------------------------

    private void forEachProbe(java.util.function.Consumer<Expression[]> action) {
        for (int arity = minArity; arity <= maxArity; arity++) {
            forEachTuple(new DataType[arity], 0, action);
        }
    }

    private void forEachTuple(DataType[] tuple, int position, java.util.function.Consumer<Expression[]> action) {
        if (position == tuple.length) {
            action.accept(TypedExpressionStub.ofCategories(tuple.clone()));
            return;
        }
        for (DataType probe : probes) {
            tuple[position] = probe;
            forEachTuple(tuple, position + 1, action);
        }
    }

    private List<FunctionMetaData> declaredMetaData() {
        List<FunctionMetaData> all = new ArrayList<>();
        for (FunctionResolver resolver : functionService.getResolvers(atom)) {
            all.addAll(resolver.getRepresentativeFunctionMetaDatas());
        }
        return all;
    }
}