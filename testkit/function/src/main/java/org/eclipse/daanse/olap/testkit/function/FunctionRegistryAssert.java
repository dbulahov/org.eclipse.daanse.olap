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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.testkit.assertions.AssertionMessages;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;

/** Registry-wide assertions, including the contract coverage meta test. */
public final class FunctionRegistryAssert {

    private final FunctionService functionService;

    FunctionRegistryAssert(FunctionService functionService) {
        this.functionService = functionService;
    }

    public FunctionRegistryAssert hasAtLeastResolvers(int expected) {
        int actual = functionService.getResolvers().size();
        if (actual < expected) {
            throw AssertionMessages.failed(
                    "registry holds fewer resolvers than expected",
                    ">= " + expected, Integer.toString(actual));
        }
        return this;
    }

    /** No resolver class registered twice — the {@code F-20} guard. */
    public FunctionRegistryAssert hasNoDuplicateResolverClasses() {
        List<String> names = functionService.getResolvers().stream()
                .map(r -> r.getClass().getName()).sorted().toList();
        List<String> duplicates = new ArrayList<>();
        for (int i = 1; i < names.size(); i++) {
            if (names.get(i).equals(names.get(i - 1)) && !duplicates.contains(names.get(i))) {
                duplicates.add(names.get(i));
            }
        }
        if (!duplicates.isEmpty()) {
            throw AssertionMessages.failed(
                    "resolver class registered more than once" + System.lineSeparator()
                            + SignatureText.lines(duplicates) + System.lineSeparator()
                            + """
                            Two instances of the same resolver class offer the same overload twice.
                            The validator then sees two best matches at equal cost and rejects every
                            call to that function with "More than one function matches signature".
                            """,
                    "no duplicates", duplicates.size() + " duplicate(s)");
        }
        return this;
    }

    /** No two resolvers declare the same signature — the {@code F-30} guard. */
    public FunctionRegistryAssert hasNoCollidingSignatures() {
        java.util.Map<String, List<String>> bySignature = new java.util.LinkedHashMap<>();
        for (FunctionResolver resolver : functionService.getResolvers()) {
            for (FunctionMetaData metaData : resolver.getRepresentativeFunctionMetaDatas()) {
                bySignature.computeIfAbsent(SignatureText.ofDeclaration(metaData), k -> new ArrayList<>())
                        .add(resolver.getClass().getName());
            }
        }
        List<String> collisions = bySignature.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(e -> e.getKey() + "   " + e.getValue())
                .sorted().toList();
        if (!collisions.isEmpty()) {
            throw AssertionMessages.failed(
                    "the same declared signature is offered by more than one resolver"
                            + System.lineSeparator() + SignatureText.lines(collisions),
                    "no colliding signatures", collisions.size() + " collision(s)");
        }
        return this;
    }

    public FunctionRegistryAssert hasNoBlankDescriptions() {
        List<String> blanks = functionService.getFunctionMetaDatas().stream()
                .filter(m -> m.description() == null || m.description().isBlank())
                .map(SignatureText::ofDeclaration).sorted().toList();
        if (!blanks.isEmpty()) {
            throw AssertionMessages.failed(
                    "functions without a description" + System.lineSeparator()
                            + SignatureText.lines(blanks),
                    "no blank descriptions", blanks.size() + " blank");
        }
        return this;
    }

    /** The OSGi registry against {@code StandardFunctions.standard()} — the {@code F-01} guard. */
    public FunctionRegistryAssert matches(FunctionService other) {
        String expected = SignatureText.lines(List.copyOf(atomKeys(functionService)));
        String actual = SignatureText.lines(List.copyOf(atomKeys(other)));
        if (!expected.equals(actual)) {
            Set<String> missingRight = new TreeSet<>(atomKeys(functionService));
            missingRight.removeAll(atomKeys(other));
            Set<String> missingLeft = new TreeSet<>(atomKeys(other));
            missingLeft.removeAll(atomKeys(functionService));
            throw AssertionMessages.mismatch("function registries", "Registry", describeBoth(other),
                    expected, actual,
                    """
                    left  = this registry, right = the other one, keyed by FunctionAtomCompareKey
                            (name upper-cased plus the atom class).

                    Missing on the right (%d): %s
                    Missing on the left  (%d): %s

                    A resolver annotated @Component(service = FunctionResolver.class) but absent from
                    StandardFunctions.standard() exists only under OSGi; every embedded runtime and
                    every test silently loses it. The reverse means a green test for a function the
                    product does not have.
                    """.formatted(missingRight.size(), missingRight, missingLeft.size(), missingLeft));
        }
        return this;
    }

    /**
     * Every registered function has a contract, and the allow list contains no stale
     * entries. Failing in both directions is what makes the list shrink monotonically.
     */
    public FunctionRegistryAssert coversAllContracts(Collection<FunctionContract> contracts,
            Set<String> knownGaps) {
        Set<String> registered = new TreeSet<>(atomKeys(functionService));
        Set<String> covered = new TreeSet<>();
        contracts.forEach(c -> covered.add(c.key()));

        Set<String> uncovered = new TreeSet<>(registered);
        uncovered.removeAll(covered);
        uncovered.removeAll(knownGaps);

        Set<String> stale = new TreeSet<>(knownGaps);
        stale.retainAll(covered);

        if (uncovered.isEmpty() && stale.isEmpty()) {
            return this;
        }
        Set<String> expectedSet = new TreeSet<>(registered);
        expectedSet.removeAll(knownGaps);
        throw AssertionMessages.mismatch("function contract coverage", "Registry",
                describe(functionService),
                SignatureText.lines(List.copyOf(expectedSet)),
                SignatureText.lines(List.copyOf(covered)),
                """
                left  = registered atoms that need a contract (%d registered, %d allowed to be missing)
                right = atoms covered by FunctionContracts.all() (%d)

                Uncovered and not in the allow list (%d):
                %s
                Either add a contract, or add the key to KnownGaps.ALLOWED with a reason.

                Stale allow-list entries (%d) — these now have a contract and must be removed:
                %s
                """.formatted(registered.size(), knownGaps.size(), covered.size(),
                        uncovered.size(), indent(uncovered), stale.size(), indent(stale)));
    }

    // ---- internals ---------------------------------------------------------

    private static Set<String> atomKeys(FunctionService service) {
        Set<String> keys = new LinkedHashSet<>();
        for (FunctionResolver resolver : service.getResolvers()) {
            keys.add(SignatureText.ofAtom(resolver.getFunctionAtom()));
        }
        return keys;
    }

    private static String describe(FunctionService service) {
        return service.getClass().getSimpleName() + " (" + service.getResolvers().size()
                + " resolvers, " + atomKeys(service).size() + " atoms)";
    }

    private String describeBoth(FunctionService other) {
        return "this  = " + describe(functionService) + System.lineSeparator()
                + "other = " + describe(other);
    }

    private static String indent(Collection<String> values) {
        return values.isEmpty() ? "  (none)"
                : values.stream().map(v -> "  " + v)
                        .reduce((a, b) -> a + System.lineSeparator() + b).orElse("  (none)");
    }
}