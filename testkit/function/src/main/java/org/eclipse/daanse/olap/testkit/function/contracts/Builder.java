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
package org.eclipse.daanse.olap.testkit.function.contracts;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.CallCase;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.DependencyCase;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.EdgeCase;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.ResultStyleCase;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.ValueCase;

public final class Builder {

    private final String name;
    private Class<? extends OperationAtom> atomClass = FunctionOperationAtom.class;
    private final List<String> declaredSignatures = new ArrayList<>();
    private Optional<DataType> returnCategory = Optional.empty();
    private OptionalInt minArity = OptionalInt.empty();
    private OptionalInt maxArity = OptionalInt.empty();
    private final List<String> reservedWords = new ArrayList<>();
    private final List<CallCase> calls = new ArrayList<>();
    private final List<EdgeCase> edgeCases = new ArrayList<>();
    private final List<ValueCase> values = new ArrayList<>();
    private final List<DependencyCase> dependencies = new ArrayList<>();
    private final List<ResultStyleCase> resultStyles = new ArrayList<>();
    private final Map<Promise, String> waivers = new EnumMap<>(Promise.class);
    private boolean autoEdgeCases;

    Builder(String name) {
        this.name = name;
    }

    // ---- promise 1 and 2 ---------------------------------------------------

    public Builder atom(Class<? extends OperationAtom> value) { this.atomClass = value; return this; }
    public Builder signatures(String... declared) { declaredSignatures.addAll(List.of(declared)); return this; }
    public Builder returns(DataType category) { returnCategory = Optional.of(category); return this; }
    public Builder arity(int min, int max) { minArity = OptionalInt.of(min); maxArity = OptionalInt.of(max); return this; }
    public Builder reservedWords(String... words) { reservedWords.addAll(List.of(words)); return this; }

    // ---- promise 3 and 4 ---------------------------------------------------

    public Builder resolvesTo(Class<? extends FunctionDefinition> definition, DataType... args) {
        calls.add(new CallCase(List.of(args), false, Optional.of(definition), Optional.empty(),
                Optional.empty(), OptionalInt.empty(), false, Optional.empty()));
        return this;
    }

    public Builder resolvesToInScalarContext(Class<? extends FunctionDefinition> definition, DataType... args) {
        calls.add(new CallCase(List.of(args), true, Optional.of(definition), Optional.empty(),
                Optional.empty(), OptionalInt.empty(), false, Optional.empty()));
        return this;
    }

    public Builder resolvesToOverload(String declaredSignature, DataType... args) {
        calls.add(new CallCase(List.of(args), false, Optional.empty(), Optional.of(declaredSignature),
                Optional.empty(), OptionalInt.empty(), false, Optional.empty()));
        return this;
    }

    public Builder resolvesWithCost(int cost, Class<? extends FunctionDefinition> definition, DataType... args) {
        calls.add(new CallCase(List.of(args), false, Optional.of(definition), Optional.empty(),
                Optional.empty(), OptionalInt.of(cost), false, Optional.empty()));
        return this;
    }

    public Builder rejects(DataType... args) {
        calls.add(new CallCase(List.of(args), false, Optional.empty(), Optional.empty(),
                Optional.empty(), OptionalInt.empty(), true, Optional.empty()));
        return this;
    }

    public Builder rejectsWith(String messageSubstring, DataType... args) {
        calls.add(new CallCase(List.of(args), false, Optional.empty(), Optional.empty(),
                Optional.empty(), OptionalInt.empty(), true, Optional.of(messageSubstring)));
        return this;
    }

    /** For {@code NonFunctionResolver} entries that exist only for MDSCHEMA_FUNCTIONS. */
    public Builder neverResolves() {
        return waive(Promise.RESOLUTION, "NonFunctionResolver: catalogue entry only, resolves via another atom");
    }

    // ---- promise 6 ---------------------------------------------------------

    /** Derive edge cases from the declared parameters. Recommended for every contract. */
    public Builder autoEdgeCases() { this.autoEdgeCases = true; return this; }

    public Builder edgeCase(String label, DataType... args) {
        edgeCases.add(new EdgeCase(label, List.of(args), Optional.empty()));
        return this;
    }

    public Builder edgeCaseMdx(String label, String mdx) {
        edgeCases.add(new EdgeCase(label, List.of(), Optional.of(mdx)));
        return this;
    }

    // ---- promise 5 ---------------------------------------------------------

    public Builder value(String mdx, String expectedFormattedValue) {
        values.add(new ValueCase(mdx, expectedFormattedValue));
        return this;
    }

    /** MDX NULL formats as the empty string. */
    public Builder valueIsNull(String mdx) { return value(mdx, ""); }

    // ---- promise 7 ---------------------------------------------------------

    public Builder dependsOn(String mdx, String... hierarchyUniqueNames) {
        dependencies.add(new DependencyCase(mdx, false, List.of(hierarchyUniqueNames), List.of()));
        return this;
    }

    public Builder scalarDependsOn(String mdx, String... hierarchyUniqueNames) {
        dependencies.add(new DependencyCase(mdx, true, List.of(hierarchyUniqueNames), List.of()));
        return this;
    }

    /** The interesting direction for context-setting functions. */
    public Builder scalarDoesNotDependOn(String mdx, String... hierarchyUniqueNames) {
        dependencies.add(new DependencyCase(mdx, true, List.of(), List.of(hierarchyUniqueNames)));
        return this;
    }

    // ---- promise 8 ---------------------------------------------------------

    public Builder resultStyle(String mdx, ResultStyle requested, ResultStyle expected) {
        resultStyles.add(new ResultStyleCase(mdx, requested, expected, false));
        return this;
    }

    public Builder independentMutableList(String mdx) {
        resultStyles.add(new ResultStyleCase(mdx, ResultStyle.MUTABLE_LIST, ResultStyle.MUTABLE_LIST, true));
        return this;
    }

    // ---- waivers -----------------------------------------------------------

    public Builder waive(Promise promise, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("a waiver needs a reason: " + name + " / " + promise);
        }
        waivers.put(promise, reason);
        return this;
    }

    public FunctionContract build() {
        List<EdgeCase> allEdgeCases = new ArrayList<>(edgeCases);
        if (autoEdgeCases) {
            allEdgeCases.add(new EdgeCase("<generated from declared parameters>", List.of(), Optional.empty()));
        }
        FunctionContract contract = new FunctionContract(name, atomClass, List.copyOf(declaredSignatures),
                returnCategory, minArity, maxArity, List.copyOf(reservedWords), List.copyOf(calls),
                List.copyOf(allEdgeCases), List.copyOf(values), List.copyOf(dependencies),
                List.copyOf(resultStyles), Map.copyOf(waivers));
        validate(contract);
        return contract;
    }

    /** A promise with neither cases nor a waiver would pass silently. Refuse to build it. */
    private static void validate(FunctionContract contract) {
        List<String> problems = new ArrayList<>();
        checkPromise(contract, Promise.SIGNATURE, !contract.declaredSignatures().isEmpty(), problems,
                ".signatures(...)");
        checkPromise(contract, Promise.RESOLUTION, !contract.calls().isEmpty(), problems,
                ".resolvesTo(...) / .rejects(...)");
        checkPromise(contract, Promise.EDGE, !contract.edgeCases().isEmpty(), problems,
                ".autoEdgeCases() / .edgeCaseMdx(...)");
        checkPromise(contract, Promise.RESULT, !contract.values().isEmpty(), problems,
                ".value(...)");
        checkPromise(contract, Promise.DEPENDENCIES, !contract.dependencies().isEmpty(), problems,
                ".dependsOn(...) / .scalarDependsOn(...)");
        checkPromise(contract, Promise.RESULT_SHAPE, !contract.resultStyles().isEmpty(), problems,
                ".resultStyle(...) / .independentMutableList(...)");
        if (!problems.isEmpty()) {
            throw new IllegalStateException("FunctionContract for '" + contract.name()
                    + "' is incomplete" + System.lineSeparator()
                    + String.join(System.lineSeparator(), problems) + System.lineSeparator()
                    + "A promise with no cases and no waiver would pass silently and give false confidence.");
        }
    }

    private static void checkPromise(FunctionContract contract, Promise promise, boolean hasCases,
            List<String> problems, String how) {
        if (!hasCases && !contract.isWaived(promise)) {
            problems.add("  Promise " + promise + " has neither cases nor a waiver."
                    + System.lineSeparator() + "    Add " + how
                    + " or .waive(Promise." + promise + ", \"<reason>\").");
        }
    }
}