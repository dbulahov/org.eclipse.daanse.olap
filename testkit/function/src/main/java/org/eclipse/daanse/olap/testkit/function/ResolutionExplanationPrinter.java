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

import org.eclipse.daanse.olap.api.function.ArgumentBinding;
import org.eclipse.daanse.olap.api.function.CandidateReport;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolutionResult;
import org.eclipse.daanse.olap.api.function.ResolutionExplanation;

/** Renders a {@link ResolutionExplanation} as a readable candidate report. */
public final class ResolutionExplanationPrinter {

    private static final String NL = System.lineSeparator();

    private ResolutionExplanationPrinter() {
    }

    public static String render(ResolutionExplanation explanation) {
        StringBuilder buf = new StringBuilder();
        buf.append("Resolution of ").append(explanation.signature()).append(NL);
        buf.append("  candidates (").append(explanation.candidates().size()).append(')').append(NL);

        int index = 1;
        for (CandidateReport candidate : explanation.candidates()) {
            String resolverName = candidate.resolver().getClass().getSimpleName();
            buf.append("    [").append(index++).append("] ").append(pad(resolverName, 28));
            if (candidate.result().isPresent()) {
                FunctionResolutionResult result = candidate.result().get();
                buf.append("matched, cost ").append(result.cost()).append(NL);
                buf.append("          overload   ")
                   .append(SignatureText.ofDeclaration(result.matchedMetaData())).append(NL);
                buf.append("          definition ")
                   .append(result.definition().getClass().getSimpleName()).append(NL);
                buf.append(renderBindings(result));
            } else {
                buf.append("rejected: ")
                   .append(candidate.rejectionReason().orElse("no reason given")).append(NL);
                for (FunctionMetaData declared : candidate.resolver().getRepresentativeFunctionMetaDatas()) {
                    buf.append("          declared   ")
                       .append(SignatureText.ofDeclaration(declared)).append(NL);
                }
            }
        }

        buf.append("  best matches (").append(explanation.bestMatches().size()).append(')').append(NL);
        for (FunctionResolutionResult best : explanation.bestMatches()) {
            buf.append("    ").append(SignatureText.ofDeclaration(best.matchedMetaData())).append(NL);
        }
        return buf.toString();
    }

    /**
     * One line per argument binding. Empty for hand-written resolvers that build their
     * result without {@code FunctionMetaDataMatcher} — the caller is told so explicitly
     * rather than being shown an empty block.
     */
    public static String renderBindings(FunctionResolutionResult result) {
        if (result.bindings().isEmpty()) {
            return "          (no argument bindings: this resolver builds its result "
                    + "without FunctionMetaDataMatcher)" + NL;
        }
        StringBuilder buf = new StringBuilder();
        for (ArgumentBinding binding : result.bindings()) {
            buf.append("          arg#").append(binding.argumentIndex()).append(' ')
               .append(pad("<" + binding.argumentCategory().getPrettyName() + ">", 24))
               .append("-> param#").append(binding.parameterIndex()).append(' ')
               .append(pad("<" + binding.parameterCategory().getPrettyName() + ">", 24))
               .append("cost ").append(binding.conversionCost()).append(NL);
        }
        return buf.toString();
    }

    private static String pad(String s, int width) {
        return s.length() >= width ? s + " " : s + " ".repeat(width - s.length());
    }
}