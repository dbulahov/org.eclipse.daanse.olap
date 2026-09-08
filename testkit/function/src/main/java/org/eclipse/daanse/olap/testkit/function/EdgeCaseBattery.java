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
import java.util.Optional;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionParameter;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.testkit.function.stub.TypedExpressionStub;

/** Generates edge cases from a function's declared parameters. */
public final class EdgeCaseBattery {

    /** One probe: a label, stage-A arguments, and optionally a stage-B MDX fragment. */
    public record Probe(String label, Expression[] args, Optional<String> mdxFragment) {

        public static Probe of(String label, Expression... args) {
            return new Probe(label, args, Optional.empty());
        }

        public static Probe mdx(String label, String mdxFragment) {
            return new Probe(label, new Expression[0], Optional.of(mdxFragment));
        }
    }

    private EdgeCaseBattery() {
    }

    /** Probes derived from one declared overload. */
    public static List<Probe> forMetaData(FunctionMetaData metaData) {
        List<Probe> probes = new ArrayList<>();
        FunctionParameter[] parameters = metaData.parameters();
        DataType[] canonical = new DataType[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            canonical[i] = parameters[i].dataType();
        }

        for (int position = 0; position < parameters.length; position++) {
            for (DataType edge : edgeCategoriesFor(parameters[position])) {
                DataType[] tuple = canonical.clone();
                tuple[position] = edge;
                probes.add(Probe.of(
                        "arg#" + position + " = <" + edge.getPrettyName() + ">",
                        TypedExpressionStub.ofCategories(tuple)));
            }
        }
        probes.addAll(arityViolations(metaData));
        return probes;
    }

    /** Probes over all overloads of a function, de-duplicated by call signature. */
    public static List<Probe> forFunction(FunctionService functionService, OperationAtom atom) {
        Set<String> seen = new LinkedHashSet<>();
        List<Probe> probes = new ArrayList<>();
        for (FunctionResolver resolver : functionService.getResolvers(atom)) {
            for (FunctionMetaData metaData : resolver.getRepresentativeFunctionMetaDatas()) {
                for (Probe probe : forMetaData(metaData)) {
                    if (seen.add(SignatureText.ofCall(atom, probe.args()))) {
                        probes.add(probe);
                    }
                }
            }
        }
        return probes;
    }

    /** One argument too few and one too many. */
    public static List<Probe> arityViolations(FunctionMetaData metaData) {
        List<Probe> probes = new ArrayList<>();
        int min = metaData.minArity();
        int max = metaData.maxArity();
        DataType filler = metaData.parameters().length > 0
                ? metaData.parameters()[0].dataType()
                : DataType.NUMERIC;

        if (min > 0) {
            probes.add(Probe.of("arity " + (min - 1) + " (one below minArity)",
                    TypedExpressionStub.ofCategories(repeat(filler, min - 1))));
        }
        if (max != Integer.MAX_VALUE) {
            probes.add(Probe.of("arity " + (max + 1) + " (one above maxArity)",
                    TypedExpressionStub.ofCategories(repeat(filler, max + 1))));
        }
        return probes;
    }

    // ---- internals ---------------------------------------------------------

    /**
     * Categories worth substituting at a parameter of this type. NULL is always included:
     * NULL converts to every scalar at zero cost, so it reaches almost every function and
     * is the trigger of the whole F-36 family.
     */
    private static List<DataType> edgeCategoriesFor(FunctionParameter parameter) {
        List<DataType> edges = new ArrayList<>();
        edges.add(DataType.NULL);
        switch (parameter.dataType()) {
        case SET -> {
            edges.add(DataType.MEMBER);
            edges.add(DataType.TUPLE);
        }
        case INTEGER, NUMERIC -> {
            edges.add(DataType.STRING);
            edges.add(DataType.SET);
        }
        case STRING -> {
            edges.add(DataType.NUMERIC);
            edges.add(DataType.SET);
        }
        case MEMBER -> {
            edges.add(DataType.SET);
            edges.add(DataType.LEVEL);
        }
        case SYMBOL -> edges.add(DataType.STRING);
        default -> edges.add(DataType.SET);
        }
        return edges;
    }

    private static DataType[] repeat(DataType category, int count) {
        DataType[] values = new DataType[Math.max(0, count)];
        java.util.Arrays.fill(values, category);
        return values;
    }
}