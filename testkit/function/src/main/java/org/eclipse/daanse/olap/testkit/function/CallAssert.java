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

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.function.ArgumentBinding;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolutionResult;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.function.ResolutionExplanation;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.testkit.assertions.AssertionMessages;
import org.eclipse.daanse.olap.testkit.function.stub.ResolutionOnlyValidator;

/** Assertions about how one concrete call resolves. */
public final class CallAssert {

    private static final String NL = System.lineSeparator();

    private final FunctionService functionService;
    private final OperationAtom atom;
    private final Expression[] args;
    private final boolean scalarContext;

    CallAssert(FunctionService functionService, OperationAtom atom, Expression[] args, boolean scalarContext) {
        this.functionService = functionService;
        this.atom = atom;
        this.args = args;
        this.scalarContext = scalarContext;
    }

    // ---- context -----------------------------------------------------------

    /** A bare {@code *} may resolve to CrossJoin. The default. */
    public CallAssert inSetContext() {
        return new CallAssert(functionService, atom, args, false);
    }

    /** A bare {@code *} must resolve to multiplication. */
    public CallAssert inScalarContext() {
        return new CallAssert(functionService, atom, args, true);
    }


    public CallAssert resolvesUniquely() {
        ResolutionExplanation explanation = explanation();
        int matches = explanation.bestMatches().size();
        if (matches == 1) {
            return this;
        }
        if (matches == 0) {
            throw AssertionMessages.mismatch("call resolution", "Call", callSignature(),
                    "exactly one match", "0 matches",
                    ResolutionExplanationPrinter.render(explanation));
        }
        throw AssertionMessages.mismatch("call resolution", "Call", callSignature(),
                "exactly one match",
                matches + " matches at equal cost " + explanation.bestMatches().get(0).cost(),
                ResolutionExplanationPrinter.render(explanation) + NL + """
                ValidatorImpl.getDef would fail here with "More than one function matches signature".
                Two overloads at equal conversion cost means the caller cannot disambiguate.
                Either the overloads must differ in cost, or one of them must not be registered.
                """);
    }

    public CallAssert resolvesTo(Class<? extends FunctionDefinition> expected) {
        resolvesUniquely();
        FunctionDefinition actual = winner().definition();
        if (!expected.isInstance(actual)) {
            throw AssertionMessages.mismatch("resolved definition", "Call", callSignature(),
                    expected.getName(), actual.getClass().getName(),
                    ResolutionExplanationPrinter.render(explanation()));
        }
        return this;
    }

    public CallAssert resolvesToOverload(String declaredSignature) {
        resolvesUniquely();
        String actual = SignatureText.ofDeclaration(winner().matchedMetaData());
        if (!declaredSignature.equals(actual)) {
            throw AssertionMessages.mismatch("matched overload", "Call", callSignature(),
                    declaredSignature, actual,
                    ResolutionExplanationPrinter.render(explanation()));
        }
        return this;
    }

    public CallAssert resolvesVia(Class<? extends FunctionResolver> expected) {
        resolvesUniquely();
        boolean found = explanation().candidates().stream()
                .filter(c -> c.result().isPresent())
                .anyMatch(c -> expected.isInstance(c.resolver()));
        if (!found) {
            throw AssertionMessages.mismatch("matching resolver", "Call", callSignature(),
                    expected.getName(),
                    explanation().candidates().stream()
                            .filter(c -> c.result().isPresent())
                            .map(c -> c.resolver().getClass().getName())
                            .reduce((a, b) -> a + NL + b).orElse("(none)"),
                    ResolutionExplanationPrinter.render(explanation()));
        }
        return this;
    }

    public CallAssert isRejected() {
        ResolutionExplanation explanation = explanation();
        if (!explanation.bestMatches().isEmpty()) {
            FunctionResolutionResult result = explanation.bestMatches().get(0);
            throw AssertionMessages.mismatch("call rejection", "Call", callSignature(),
                    "no match", SignatureText.ofDeclaration(result.matchedMetaData()),
                    ResolutionExplanationPrinter.render(explanation) + NL
                            + conversionNarrative(result));
        }
        return this;
    }

    /**
     * The call must be rejected <em>and</em> the validator's message must contain
     * {@code messageSubstring}. This is the only assertion that calls {@code getDef};
     * it is safe because {@code getDef} throws before applying any conversion when
     * {@code bestMatches} is empty.
     */
    public CallAssert isRejectedBecause(String messageSubstring) {
        isRejected();
        try {
            validator().getDef(args, atom);
        } catch (OlapRuntimeException e) {
            String message = String.valueOf(e.getMessage());
            if (!message.contains(messageSubstring)) {
                throw AssertionMessages.mismatch("rejection message", "Call", callSignature(),
                        messageSubstring, message, null);
            }
            return this;
        }
        throw AssertionMessages.failed(
                "call was expected to be rejected with a message containing '" + messageSubstring + '\''
                        + NL + "Call:" + NL + callSignature() + NL + NL
                        + "Validator.getDef returned a definition instead of throwing.",
                messageSubstring, "<no exception>");
    }

    public CallAssert isAmbiguous() {
        int matches = explanation().bestMatches().size();
        if (matches < 2) {
            throw AssertionMessages.mismatch("ambiguity", "Call", callSignature(),
                    "more than one best match", matches + " best match(es)",
                    ResolutionExplanationPrinter.render(explanation()));
        }
        return this;
    }

    /** For {@code NonFunctionResolver} entries that exist only for the catalogue. */
    public CallAssert neverResolves() {
        return isRejected();
    }


    public CallAssert resolvesToReturnCategory(DataType expected) {
        resolvesUniquely();
        DataType actual = winner().matchedMetaData().returnCategory();
        if (actual != expected) {
            throw AssertionMessages.mismatch("declared return category", "Call", callSignature(),
                    expected.name(), actual.name(),
                    """
                    This is the category declared on the matched overload. The type the call
                    actually produces comes from FunctionDefinition.createCall and needs a cube;
                    assert it with CalcAssert.hasType(...) in the evaluating stage.
                    """);
        }
        return this;
    }

    public CallAssert bindsArgumentsTo(DataType... parameterCategories) {
        resolvesUniquely();
        List<ArgumentBinding> bindings = winner().bindings();
        if (bindings.isEmpty()) {
            throw AssertionMessages.failed(
                    "no argument bindings available" + NL + "Call:" + NL + callSignature() + NL + NL
                            + """
                            FunctionResolutionResult.bindings() is empty. Hand-written resolvers that
                            build their result with FunctionResolutionResultR.of(definition, conversions)
                            instead of going through FunctionMetaDataMatcher carry no binding trace.
                            Assert resolvesToOverload(...) instead, or waive Promise.TYPE.
                            """,
                    "one binding per argument", "0 bindings");
        }
        String expected = renderExpectedBindings(parameterCategories);
        String actual = ResolutionExplanationPrinter.renderBindings(winner());
        if (!expected.equals(actual)) {
            throw AssertionMessages.mismatch("argument bindings", "Call", callSignature(),
                    expected, actual, null);
        }
        return this;
    }

    public CallAssert bindsArgumentToParameter(int argumentIndex, int parameterIndex) {
        resolvesUniquely();
        boolean ok = winner().bindings().stream()
                .anyMatch(b -> b.argumentIndex() == argumentIndex && b.parameterIndex() == parameterIndex);
        if (!ok) {
            throw AssertionMessages.mismatch("argument binding", "Call", callSignature(),
                    "arg#" + argumentIndex + " -> param#" + parameterIndex,
                    ResolutionExplanationPrinter.renderBindings(winner()), null);
        }
        return this;
    }

    public CallAssert resolvesWithoutConversion() {
        return resolvesWithConversionCost(0);
    }

    public CallAssert resolvesWithConversionCost(int expected) {
        resolvesUniquely();
        int actual = winner().cost();
        if (actual != expected) {
            throw AssertionMessages.mismatch("conversion cost", "Call", callSignature(),
                    "conversion cost " + expected, "conversion cost " + actual,
                    conversionNarrative(winner()));
        }
        return this;
    }

    /**
     * {@code FunctionResolver.resolve} is a pure predicate: for every input it must return
     * a match or {@code Optional.empty()}. There is no permitted exception — throwing
     * aborts validation with a stack trace instead of a diagnosed message.
     */
    public CallAssert resolutionDoesNotThrow() {
        for (FunctionResolver resolver : functionService.getResolvers(atom)) {
            try {
                resolver.resolve(args, validator());
            } catch (RuntimeException | StackOverflowError thrown) {
                throw AssertionMessages.failed(
                        "FunctionResolver.resolve threw instead of returning Optional.empty()" + NL
                                + "Call:" + NL + callSignature() + NL + NL
                                + """
                                resolve(...) is a pure predicate: it must return a match or
                                Optional.empty() for every input. Throwing makes the whole query fail
                                during validation with a stack trace instead of a diagnosed
                                "No function matches signature" message.

                                resolver: %s
                                """.formatted(resolver.getClass().getName()),
                        "Optional.empty() or a match", thrown.getClass().getName(), thrown);
            }
        }
        return this;
    }

    // ================= diagnostics =========================================

    /** The rendered candidate report — for custom assertions and for reading in a debugger. */
    public String explain() {
        return ResolutionExplanationPrinter.render(explanation());
    }

    public ResolutionExplanation explanation() {
        return validator().explainDef(args, atom);
    }

    public FunctionResolutionResult winner() {
        return explanation().winner().orElseThrow(
                () -> AssertionMessages.failed("call does not resolve uniquely" + NL + explain(),
                        "exactly one match", explanation().bestMatches().size() + " matches"));
    }

    // ---- internals ---------------------------------------------------------

    private ResolutionOnlyValidator validator() {
        return scalarContext
                ? ResolutionOnlyValidator.inScalarContext(functionService)
                : ResolutionOnlyValidator.inSetContext(functionService);
    }

    private String callSignature() {
        return SignatureText.ofCall(atom, args);
    }

    private String renderExpectedBindings(DataType[] parameterCategories) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < parameterCategories.length; i++) {
            buf.append("          arg#").append(i).append(' ')
               .append("<").append(args[i].getCategory().getPrettyName()).append(">")
               .append(" -> param#").append(i).append(' ')
               .append("<").append(parameterCategories[i].getPrettyName()).append(">").append(NL);
        }
        return buf.toString();
    }

    private String conversionNarrative(FunctionResolutionResult result) {
        if (result.conversions().isEmpty()) {
            return "No implicit conversion was needed.";
        }
        StringBuilder buf = new StringBuilder("Conversions applied:").append(NL);
        for (ArgumentBinding binding : result.bindings()) {
            if (binding.conversionCost() > 0) {
                buf.append("  arg#").append(binding.argumentIndex()).append(' ')
                   .append("<").append(binding.argumentCategory().getPrettyName()).append("> -> <")
                   .append(binding.parameterCategory().getPrettyName()).append(">  cost ")
                   .append(binding.conversionCost()).append(NL);
            }
        }
        buf.append("""
                Total cost %d. The conversion table lives in TypeUtil.canConvert; see
                docs/refactoring/funktionen-und-calc/04-typsystem-und-konvertierungskosten.md §2.
                """.formatted(result.cost()));
        return buf.toString();
    }
}