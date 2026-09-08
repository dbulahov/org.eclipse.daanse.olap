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
import java.util.List;
import java.util.Locale;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionOrigin;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.testkit.assertions.AssertionMessages;
import org.eclipse.daanse.olap.testkit.function.stub.TypedExpressionStub;

/** Assertions about a function's registration and declared signature. */
public final class FunctionAssert {

    private static final String NL = System.lineSeparator();

    private final FunctionService functionService;
    private final OperationAtom atom;

    FunctionAssert(FunctionService functionService, OperationAtom atom) {
        this.functionService = functionService;
        this.atom = atom;
    }

    public FunctionAssert isRegistered() {
        List<FunctionResolver> resolvers = functionService.getResolvers(atom);
        if (resolvers.isEmpty()) {
            throw AssertionMessages.failed(
                    "function is not registered" + NL + subjectLabel() + ':' + NL + subject() + NL + NL
                            + FunctionRegistryIndex.notFoundDiagnosis(functionService, atom),
                    subject(), "<not registered>");
        }
        return this;
    }

    public FunctionAssert hasResolverCount(int expected) {
        int actual = functionService.getResolvers(atom).size();
        if (actual != expected) {
            throw AssertionMessages.mismatch("resolver count", subjectLabel(), subject(),
                    Integer.toString(expected), Integer.toString(actual),
                    """
                    Resolvers registered under this atom:
                    %s
                    Two resolvers of the same class mean the same overload is offered twice; the
                    validator then sees two best matches at equal cost and fails the call with
                    "More than one function matches signature".
                    """.formatted(resolverList()));
        }
        return this;
    }

    public FunctionAssert hasResolverOfType(Class<? extends FunctionResolver> expected) {
        boolean found = functionService.getResolvers(atom).stream().anyMatch(expected::isInstance);
        if (!found) {
            throw AssertionMessages.mismatch("resolver type", subjectLabel(), subject(),
                    expected.getName(), resolverList(), null);
        }
        return this;
    }

    /**
     * The OSGi/hand-registry cross check: a function must be reachable from both the
     * {@code @Component}-driven service and {@code StandardFunctions.standard()}.
     */
    public FunctionAssert isAlsoRegisteredIn(FunctionService other) {
        boolean here = !functionService.getResolvers(atom).isEmpty();
        boolean there = !other.getResolvers(atom).isEmpty();
        if (here != there) {
            throw AssertionMessages.mismatch("registration across services", subjectLabel(), subject(),
                    describePresence(here, functionService), describePresence(there, other),
                    """
                    A function registered via @Component(service = FunctionResolver.class) but missing
                    from StandardFunctions.standard() works under OSGi and silently disappears in every
                    embedded runtime — and in every test, because the test path uses the hand registry.
                    The reverse (in StandardFunctions but no @Component) is worse: the test is green and
                    the product does not have the function.

                    Fix by adding `svc.addResolver(new %sResolver());` to StandardFunctions.standard(),
                    or `@Component(service = FunctionResolver.class)` to the resolver class.
                    """.formatted(atom.name()));
        }
        return this;
    }

    public FunctionAssert declaresReservedWords(String... words) {
        List<String> expected = List.of(words);
        List<String> actual = new ArrayList<>();
        for (FunctionResolver resolver : functionService.getResolvers(atom)) {
            actual.addAll(resolver.getReservedWords());
        }
        if (!sorted(expected).equals(sorted(actual))) {
            throw AssertionMessages.mismatch("reserved words", subjectLabel(), subject(),
                    SignatureText.lines(expected), SignatureText.lines(actual),
                    reservedWordCrossCheck(expected));
        }
        return this;
    }

    public FunctionAssert hasSignatures(String... declaredSignatures) {
        String expected = SignatureText.lines(List.of(declaredSignatures));
        String actual = SignatureText.ofDeclarations(declaredMetaData());
        if (!expected.equals(actual)) {
            throw AssertionMessages.mismatch("declared signatures", subjectLabel(), subject(),
                    expected, actual,
                    """
                    Declarations come from FunctionResolver.getRepresentativeFunctionMetaDatas().
                    They are what MDSCHEMA_FUNCTIONS advertises, so a wrong declaration misleads
                    Excel and Power BI even when the function itself works.
                    """);
        }
        return this;
    }

    public FunctionAssert hasSignature(String declaredSignature) {
        return hasSignatures(declaredSignature);
    }

    public FunctionAssert hasReturnCategory(DataType expected) {
        for (FunctionMetaData metaData : declaredMetaData()) {
            if (metaData.returnCategory() != expected) {
                throw AssertionMessages.mismatch("return category", subjectLabel(), subject(),
                        expected.name(), metaData.returnCategory().name(),
                        "Overload: " + SignatureText.ofDeclaration(metaData));
            }
        }
        return this;
    }

    public FunctionAssert hasArity(int expectedMin, int expectedMax) {
        int actualMin = declaredMetaData().stream().mapToInt(FunctionMetaData::minArity).min().orElse(-1);
        int actualMax = declaredMetaData().stream().mapToInt(FunctionMetaData::maxArity).max().orElse(-1);
        if (actualMin != expectedMin || actualMax != expectedMax) {
            throw AssertionMessages.mismatch("arity", subjectLabel(), subject(),
                    "min " + expectedMin + ", max " + arityText(expectedMax),
                    "min " + actualMin + ", max " + arityText(actualMax),
                    """
                    Arity comes from FunctionMetaData.minArity()/maxArity() over %d declared overload(s):
                    %s
                    minArity counts non-optional, non-skippable parameters (a repeat group once);
                    maxArity is Integer.MAX_VALUE as soon as one parameter is repeatable.
                    A parameter that should be optional but lacks .asOptional() raises minArity.
                    """.formatted(declaredMetaData().size(), declarationTable()));
        }
        return this;
    }

    public FunctionAssert hasNonBlankDescription() {
        for (FunctionMetaData metaData : declaredMetaData()) {
            if (metaData.description() == null || metaData.description().isBlank()) {
                throw AssertionMessages.failed(
                        "function has a blank description" + NL + subjectLabel() + ':' + NL + subject() + NL + NL
                                + "The description is the DESCRIPTION column of MDSCHEMA_FUNCTIONS and "
                                + "shows up in Excel and Power BI.",
                        "a non-blank description", String.valueOf(metaData.description()));
            }
        }
        return this;
    }

    public FunctionAssert hasOrigin(FunctionOrigin expected) {
        for (FunctionMetaData metaData : declaredMetaData()) {
            if (metaData.origin() != expected) {
                throw AssertionMessages.mismatch("origin", subjectLabel(), subject(),
                        expected.name(), metaData.origin().name(),
                        "ORIGIN distinguishes built-in MSOLAP functions from UDF libraries and is a "
                                + "tiebreaker candidate when two libraries declare the same name.");
            }
        }
        return this;
    }

    /**
     * Which argument positions force a scalar expression.
     *
     * <p>This is a disjunction over <em>all</em> resolvers of the atom: as soon as one of
     * them accepts a set at position k, no scalar is required. A newly registered resolver
     * can therefore change how an existing expression parses.
     */
    public FunctionAssert requiresScalarExpressionOn(int... argumentPositions) {
        int width = 1 + java.util.Arrays.stream(argumentPositions).max().orElse(0);
        List<String> expected = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        for (int k = 0; k < width; k++) {
            int position = k;
            boolean want = java.util.Arrays.stream(argumentPositions).anyMatch(p -> p == position);
            boolean got = functionService.getResolvers(atom).stream()
                    .allMatch(r -> r.requiresScalarExpressionOnArgument(position));
            expected.add("arg#" + k + ' ' + want);
            actual.add("arg#" + k + ' ' + got);
        }
        String e = String.join(NL, expected);
        String a = String.join(NL, actual);
        if (!e.equals(a)) {
            throw AssertionMessages.mismatch("requiresScalarExpressionOnArgument", subjectLabel(), subject(),
                    e, a,
                    """
                    FunctionResolver.requiresScalarExpressionOnArgument(k) decides whether the validator
                    may resolve a bare "*" at position k to CrossJoin instead of multiplication.
                    Returning false where a set is not actually accepted makes "a * b" resolve to
                    CrossJoin in a scalar context.
                    """);
        }
        return this;
    }

    // ================= transitions =========================================

    public CallAssert calledWith(Expression... args) {
        return new CallAssert(functionService, atom, args, false);
    }

    public CallAssert calledWith(DataType... argumentCategories) {
        return calledWith(TypedExpressionStub.ofCategories(argumentCategories));
    }

    public CallAssert calledWithNothing() {
        return calledWith(new Expression[0]);
    }

    public SignatureProbeAssert probeSignature() {
        return new SignatureProbeAssert(functionService, atom);
    }

    // ================= internals ===========================================

    /** The subject description, for use in custom assertions. */
    public String describe() {
        return subject();
    }

    List<FunctionMetaData> declaredMetaData() {
        List<FunctionMetaData> all = new ArrayList<>();
        for (FunctionResolver resolver : functionService.getResolvers(atom)) {
            all.addAll(resolver.getRepresentativeFunctionMetaDatas());
        }
        return all;
    }

    private String subjectLabel() {
        return "Function";
    }

    private String subject() {
        return SignatureText.ofAtom(atom);
    }

    private String resolverList() {
        return functionService.getResolvers(atom).stream()
                .map(r -> "  " + r.getClass().getName())
                .reduce((a, b) -> a + NL + b).orElse("  (none)");
    }

    private String declarationTable() {
        StringBuilder buf = new StringBuilder();
        for (FunctionMetaData metaData : declaredMetaData()) {
            buf.append("  ").append(SignatureText.ofDeclaration(metaData))
               .append("   min ").append(metaData.minArity())
               .append("  max ").append(arityText(metaData.maxArity())).append(NL);
        }
        return buf.toString();
    }

    private String reservedWordCrossCheck(List<String> expected) {
        StringBuilder buf = new StringBuilder();
        for (String word : expected) {
            if (!functionService.isReservedWord(word)) {
                buf.append("FunctionService.isReservedWord(\"").append(word).append("\") = false.").append(NL);
            }
        }
        if (buf.isEmpty()) {
            return null;
        }
        buf.append("""
                A word listed in a FunctionParameter's reservedWords() but not in
                FunctionResolver.getReservedWords() never reaches FunctionServiceImpl.reInitialize()
                and stays parseable as an identifier.""");
        return buf.toString();
    }

    private static String describePresence(boolean present, FunctionService service) {
        return present
                ? "present (" + service.getClass().getSimpleName() + ")"
                : "absent (" + service.getClass().getSimpleName() + ")";
    }

    private static String arityText(int arity) {
        return arity == Integer.MAX_VALUE ? "unbounded" : Integer.toString(arity);
    }

    private static List<String> sorted(List<String> values) {
        return values.stream().map(v -> v.toUpperCase(Locale.ROOT)).sorted().toList();
    }
}