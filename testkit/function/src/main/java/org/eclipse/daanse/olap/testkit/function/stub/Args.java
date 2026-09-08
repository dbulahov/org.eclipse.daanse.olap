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
package org.eclipse.daanse.olap.testkit.function.stub;

import java.math.BigDecimal;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.query.component.DimensionExpressionImpl;
import org.eclipse.daanse.olap.query.component.HierarchyExpressionImpl;
import org.eclipse.daanse.olap.query.component.NullLiteralImpl;
import org.eclipse.daanse.olap.query.component.NumericLiteralImpl;
import org.eclipse.daanse.olap.query.component.StringLiteralImpl;
import org.eclipse.daanse.olap.query.component.SymbolLiteralImpl;

/** Argument lists for resolution assertions. */
public final class Args {

    private Args() {
    }

    public static Expression[] of(Expression... args) {
        return args;
    }

    public static Expression[] of(DataType... categories) {
        return TypedExpressionStub.ofCategories(categories);
    }

    public static Expression[] none() {
        return new Expression[0];
    }

    // ---- real literals, for resolvers that test `instanceof Literal` --------

    public static Expression numericLiteral(int value) {
        return NumericLiteralImpl.create(BigDecimal.valueOf(value));
    }

    public static Expression numericLiteral(BigDecimal value) {
        return NumericLiteralImpl.create(value);
    }

    public static Expression stringLiteral(String value) {
        return StringLiteralImpl.create(value);
    }

    public static Expression symbolLiteral(String value) {
        return SymbolLiteralImpl.create(value);
    }

    public static Expression nullLiteral() {
        return NullLiteralImpl.nullValue;
    }

    // ---- real element expressions, for resolvers that test the node type ----

    public static Expression hierarchyExpression(Hierarchy hierarchy) {
        return new HierarchyExpressionImpl(hierarchy);
    }

    public static Expression dimensionExpression(Dimension dimension) {
        return new DimensionExpressionImpl(dimension);
    }
}