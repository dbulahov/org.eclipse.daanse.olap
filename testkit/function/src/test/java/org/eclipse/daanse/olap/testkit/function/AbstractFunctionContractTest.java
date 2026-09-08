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

import static org.eclipse.daanse.olap.testkit.function.FunctionAssertions.assertThatFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.function.services.standard.StandardFunctions;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.CallCase;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.DependencyCase;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.Promise;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.ResultStyleCase;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract.ValueCase;
import org.eclipse.daanse.olap.testkit.function.eval.CalcAssertions;
import org.eclipse.daanse.olap.testkit.function.eval.MdxValues;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.opentest4j.AssertionFailedError;

/**
 * Runs one {@link FunctionContract}. Subclass it once per function; the eight promises
 * come for free.
 *
 * <p>Stage A runs everywhere. Stage B is skipped unless {@link #connection()} is
 * overridden — in the olap repo it is not, in the rolap repo a four-line subclass supplies
 * a real cube and the same contract runs completely.
 */
public abstract class AbstractFunctionContractTest {

    /** The contract of the function under test. */
    protected abstract FunctionContract contract();

    /** The registry stage A runs against. */
    protected FunctionService functionService() {
        return StandardFunctions.standard();
    }

    /** A second registry for the cross check; empty skips that test. */
    protected Optional<FunctionService> osgiFunctionService() {
        return Optional.empty();
    }

    /** A connection for stage B; empty skips promises 5, 7 and 8. */
    protected Optional<Connection> connection() {
        return Optional.empty();
    }

    protected String cubeName() {
        return "Sales";
    }

    // ================= promise 1 ===========================================

    @Test
    void isRegistered() {
        skipIfWaived(Promise.REGISTRATION);
        assertThatFunction(functionService(), contract().name(), contract().atomClass()).isRegistered();
    }

    @Test
    void isRegisteredInBothServices() {
        skipIfWaived(Promise.REGISTRATION);
        FunctionService osgi = osgiFunctionService().orElse(null);
        Assumptions.assumeTrue(osgi != null, "no OSGi FunctionService supplied");
        assertThatFunction(osgi, contract().name(), contract().atomClass())
                .isAlsoRegisteredIn(functionService());
    }

    // ================= promise 2 ===========================================

    @Test
    void hasDeclaredSignature() {
        skipIfWaived(Promise.SIGNATURE);
        FunctionAssert assertion = assertThatFunction(functionService(), contract().name(),
                contract().atomClass()).isRegistered();
        if (!contract().declaredSignatures().isEmpty()) {
            assertion.hasSignatures(contract().declaredSignatures().toArray(String[]::new));
        }
        contract().returnCategory().ifPresent(assertion::hasReturnCategory);
        if (contract().minArity().isPresent() && contract().maxArity().isPresent()) {
            assertion.hasArity(contract().minArity().getAsInt(), contract().maxArity().getAsInt());
        }
        if (!contract().reservedWords().isEmpty()) {
            assertion.declaresReservedWords(contract().reservedWords().toArray(String[]::new));
        }
        assertion.hasNonBlankDescription();
    }

    @Test
    void declaredSignatureMatchesAcceptedCalls() {
        skipIfWaived(Promise.SIGNATURE);
        assertThatFunction(functionService(), contract().name(), contract().atomClass())
                .probeSignature()
                .overArities(0, 2)
                .isConsistent();
    }

    // ================= promises 3 and 4 ====================================

    @TestFactory
    Stream<DynamicTest> resolvesDeclaredCalls() {
        if (contract().isWaived(Promise.RESOLUTION)) {
            return Stream.of(waivedTest(Promise.RESOLUTION));
        }
        return contract().calls().stream().map(callCase -> DynamicTest.dynamicTest(
                contract().name() + " » resolution » " + describe(callCase),
                () -> runCall(callCase)));
    }

    // ================= promise 6 ===========================================

    @TestFactory
    Stream<DynamicTest> survivesEdgeCases() {
        if (contract().isWaived(Promise.EDGE)) {
            return Stream.of(waivedTest(Promise.EDGE));
        }
        List<DynamicTest> tests = new ArrayList<>();

        for (EdgeCaseBattery.Probe probe : EdgeCaseBattery.forFunction(
                functionService(), contract().atom())) {
            tests.add(DynamicTest.dynamicTest(
                    contract().name() + " » edge » " + probe.label(),
                    () -> assertThatFunction(functionService(), contract().name(), contract().atomClass())
                            .calledWith(probe.args())
                            .resolutionDoesNotThrow()));
        }

        Connection connection = connection().orElse(null);
        for (FunctionContract.EdgeCase edgeCase : contract().edgeCases()) {
            edgeCase.mdx().ifPresent(mdx -> tests.add(DynamicTest.dynamicTest(
                    contract().name() + " » edge » " + edgeCase.label(),
                    () -> {
                        Assumptions.assumeTrue(connection != null, "stage B: no Connection supplied");
                        assertNoUndiagnosedCrash(connection, mdx, edgeCase.label());
                    })));
        }
        return tests.stream();
    }

    // ================= promise 5 ===========================================

    @TestFactory
    Stream<DynamicTest> returnsExpectedValues() {
        if (contract().isWaived(Promise.RESULT)) {
            return Stream.of(waivedTest(Promise.RESULT));
        }
        Connection connection = connection().orElse(null);
        return contract().values().stream().map(valueCase -> DynamicTest.dynamicTest(
                contract().name() + " » Ergebnis » " + valueCase.mdx(),
                () -> {
                    Assumptions.assumeTrue(connection != null, "stage B: no Connection supplied");
                    runValue(connection, valueCase);
                }));
    }

    // ================= promise 7 ===========================================

    @TestFactory
    Stream<DynamicTest> dependsOnExpectedHierarchies() {
        if (contract().isWaived(Promise.DEPENDENCIES)) {
            return Stream.of(waivedTest(Promise.DEPENDENCIES));
        }
        Connection connection = connection().orElse(null);
        return contract().dependencies().stream().map(dependencyCase -> DynamicTest.dynamicTest(
                contract().name() + " » Abhängigkeiten » " + dependencyCase.mdx(),
                () -> {
                    Assumptions.assumeTrue(connection != null, "stage B: no Connection supplied");
                    runDependency(connection, dependencyCase);
                }));
    }

    // ================= promise 8 ===========================================

    @TestFactory
    Stream<DynamicTest> honoursResultStyle() {
        if (contract().isWaived(Promise.RESULT_SHAPE)) {
            return Stream.of(waivedTest(Promise.RESULT_SHAPE));
        }
        Connection connection = connection().orElse(null);
        return contract().resultStyles().stream().map(styleCase -> DynamicTest.dynamicTest(
                contract().name() + " » Ergebnisform » " + styleCase.mdx(),
                () -> {
                    Assumptions.assumeTrue(connection != null, "stage B: no Connection supplied");
                    runResultStyle(connection, styleCase);
                }));
    }

    // ================= meta ================================================

    @Test
    void waiversAreJustified() {
        contract().waivers().forEach((promise, reason) -> {
            if (reason == null || reason.isBlank()) {
                throw new AssertionFailedError("waiver for " + promise + " has no reason",
                        "a reason", String.valueOf(reason));
            }
        });
    }

    // ---- internals ---------------------------------------------------------

    private void runCall(CallCase callCase) {
        DataType[] args = callCase.argumentCategories().toArray(DataType[]::new);
        CallAssert call = assertThatFunction(functionService(), contract().name(), contract().atomClass())
                .calledWith(args);
        if (callCase.scalarContext()) {
            call = call.inScalarContext();
        }
        if (callCase.expectRejection()) {
            if (callCase.expectedRejectionMessage().isPresent()) {
                call.isRejectedBecause(callCase.expectedRejectionMessage().get());
            } else {
                call.isRejected();
            }
            return;
        }
        call.resolvesUniquely();
        callCase.expectedDefinition().ifPresent(call::resolvesTo);
        callCase.expectedOverload().ifPresent(call::resolvesToOverload);
        callCase.expectedReturnCategory().ifPresent(call::resolvesToReturnCategory);
        if (callCase.expectedConversionCost().isPresent()) {
            call.resolvesWithConversionCost(callCase.expectedConversionCost().getAsInt());
        }
    }

    private void runValue(Connection connection, ValueCase valueCase) {
        String actual = MdxValues.formattedValueOf(connection, cubeName(), valueCase.mdx());
        if (!valueCase.expectedFormattedValue().equals(actual)) {
            throw org.eclipse.daanse.olap.testkit.assertions.AssertionMessages.mismatch(
                    "formatted value", "MDX", valueCase.mdx(),
                    valueCase.expectedFormattedValue(), actual, null);
        }
    }

    private void runDependency(Connection connection, DependencyCase dependencyCase) {
        var assertion = dependencyCase.scalar()
                ? CalcAssertions.assertThatScalarExpr(connection, cubeName(), dependencyCase.mdx())
                : CalcAssertions.assertThatSetExpr(connection, cubeName(), dependencyCase.mdx());
        if (!dependencyCase.forbiddenHierarchyUniqueNames().isEmpty()) {
            assertion.doesNotDependOn(
                    dependencyCase.forbiddenHierarchyUniqueNames().toArray(String[]::new));
        }
        if (!dependencyCase.hierarchyUniqueNames().isEmpty()
                || dependencyCase.forbiddenHierarchyUniqueNames().isEmpty()) {
            assertion.dependsOnExactly(dependencyCase.hierarchyUniqueNames().toArray(String[]::new));
        }
    }

    private void runResultStyle(Connection connection, ResultStyleCase styleCase) {
        var assertion = CalcAssertions.assertThatSetExpr(connection, cubeName(), styleCase.mdx(),
                styleCase.requested());
        assertion.hasResultStyle(styleCase.expected());
        if (styleCase.requireIndependentMutableList()) {
            assertion.producesIndependentMutableList();
        }
    }

    private void assertNoUndiagnosedCrash(Connection connection, String mdx, String label) {
        CrashPolicy policy = CrashPolicy.standard();
        Optional<Throwable> thrown = MdxValues.errorOf(connection, cubeName(), mdx);
        if (thrown.isPresent() && !policy.isDiagnosed(thrown.get())) {
            throw org.eclipse.daanse.olap.testkit.assertions.AssertionMessages.failed(
                    "edge case crashed" + System.lineSeparator()
                            + "Call:" + System.lineSeparator() + mdx + "   [" + label + ']'
                            + System.lineSeparator() + System.lineSeparator()
                            + """
                            An edge case must either produce a value (MDX NULL and the empty cell count
                            as values) or fail with a diagnosed exception. It failed with an
                            undiagnosed one.

                            allowed:   OlapRuntimeException with a non-blank message, or a
                                       cancellation/limit exception
                            forbidden: JDK runtime exceptions that escaped by accident

                            """ + policy.explain(thrown.get()),
                    "a value or a diagnosed exception", thrown.get().getClass().getName(), thrown.get());
        }
    }

    private void skipIfWaived(Promise promise) {
        if (contract().isWaived(promise)) {
            Assumptions.abort("waived: " + contract().waiverReason(promise));
        }
    }

    private DynamicTest waivedTest(Promise promise) {
        return DynamicTest.dynamicTest(
                contract().name() + " » " + promise + " » waived",
                () -> Assumptions.abort("waived: " + contract().waiverReason(promise)));
    }

    private static String describe(CallCase callCase) {
        String args = callCase.argumentCategories().stream()
                .map(c -> "<" + c.getPrettyName() + ">")
                .reduce((a, b) -> a + ", " + b).orElse("");
        String suffix = callCase.expectRejection() ? " rejected" : " resolves";
        return "(" + args + ")" + suffix + (callCase.scalarContext() ? " [scalar context]" : "");
    }
}