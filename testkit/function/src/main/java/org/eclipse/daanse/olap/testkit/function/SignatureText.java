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

import java.util.Collection;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.function.core.FunctionPrinter;
import org.eclipse.daanse.olap.query.base.Expressions;

/** Renders operation atoms, calls and declarations exactly as the engine does. */
public final class SignatureText {

    private SignatureText() {
    }

    /** {@code Head(<Set>, <Numeric Expression>)} — how the call looks. */
    public static String ofCall(OperationAtom atom, Expression[] args) {
        return FunctionPrinter.getSignature(atom, DataType.UNKNOWN, Expressions.categoriesOf(args));
    }

    public static String ofCall(OperationAtom atom, DataType[] argumentCategories) {
        return FunctionPrinter.getSignature(atom, DataType.UNKNOWN, argumentCategories);
    }

    /** {@code <Set> Head(<Set>, <Numeric Expression>)} — how the overload is declared. */
    public static String ofDeclaration(FunctionMetaData functionMetaData) {
        return FunctionPrinter.getSignature(functionMetaData);
    }

    /** Declarations, sorted, one per line — ready for TextGridDiff. */
    public static String ofDeclarations(Collection<FunctionMetaData> functionMetaDatas) {
        return functionMetaDatas.stream()
                .map(SignatureText::ofDeclaration)
                .sorted()
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse("");
    }

    /** {@code Head (FunctionOperationAtom)} — the subject header of every message. */
    public static String ofAtom(OperationAtom atom) {
        return atom.name() + " (" + atom.getClass().getSimpleName() + ")";
    }

    public static String lines(List<String> values) {
        return values.stream().sorted().reduce((a, b) -> a + System.lineSeparator() + b).orElse("");
    }
}