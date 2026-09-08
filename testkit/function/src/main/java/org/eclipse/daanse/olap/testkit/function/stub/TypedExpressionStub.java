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

import java.io.PrintWriter;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.DateTimeType;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.api.type.DimensionType;
import org.eclipse.daanse.olap.api.type.EmptyType;
import org.eclipse.daanse.olap.api.type.HierarchyType;
import org.eclipse.daanse.olap.api.type.LevelType;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.NullType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.ScalarType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.SymbolType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.query.component.expression.AbstractExpression;
import org.eclipse.daanse.olap.util.type.TypeUtil;

/**
 * An {@link Expression} that carries a {@link Type} and nothing else.
 *
 * <p>Function resolution only ever asks an argument for its type and category, so a stub
 * is enough to drive {@code ValidatorImpl.explainDef} and every
 * {@code FunctionResolver.resolve} that follows the declarative path. A stub has no
 * value and cannot be compiled; see {@code CalcAssertions} for the evaluating stage.
 */
public final class TypedExpressionStub extends AbstractExpression {

    private final Type type;
    private final String label;

    private TypedExpressionStub(Type type, String label) {
        this.type = java.util.Objects.requireNonNull(type, "type");
        this.label = label;
    }

    // ---- generic factories -------------------------------------------------

    public static TypedExpressionStub ofType(Type type) {
        return ofType(type, "<" + TypeUtil.typeToCategory(type).getPrettyName() + ">");
    }

    public static TypedExpressionStub ofType(Type type, String label) {
        return new TypedExpressionStub(type, label);
    }

    /** The canonical stub for a category, matching {@code TypeUtil.castType}. */
    public static TypedExpressionStub ofCategory(DataType category) {
        return ofType(canonicalTypeOf(category));
    }

    public static Expression[] ofCategories(DataType... categories) {
        Expression[] args = new Expression[categories.length];
        for (int i = 0; i < categories.length; i++) {
            args[i] = ofCategory(categories[i]);
        }
        return args;
    }

    // ---- named conveniences ------------------------------------------------

    public static TypedExpressionStub set()                    { return ofType(new SetType(MemberType.Unknown)); }
    public static TypedExpressionStub setOf(Hierarchy h)       { return ofType(new SetType(MemberType.forHierarchy(h))); }
    public static TypedExpressionStub setOfTuples(int arity)   { return ofType(new SetType(tupleTypeOf(arity))); }
    public static TypedExpressionStub member()                 { return ofType(MemberType.Unknown); }
    public static TypedExpressionStub memberOf(Hierarchy h)    { return ofType(MemberType.forHierarchy(h)); }
    public static TypedExpressionStub tuple(int arity)         { return ofType(tupleTypeOf(arity)); }
    public static TypedExpressionStub numeric()                { return ofType(NumericType.INSTANCE); }
    public static TypedExpressionStub integer()                { return ofType(new DecimalType(Integer.MAX_VALUE, 0)); }
    public static TypedExpressionStub string()                 { return ofType(StringType.INSTANCE); }
    public static TypedExpressionStub logical()                { return ofType(BooleanType.INSTANCE); }
    public static TypedExpressionStub dateTime()               { return ofType(DateTimeType.INSTANCE); }
    public static TypedExpressionStub level()                  { return ofType(LevelType.Unknown); }
    public static TypedExpressionStub hierarchy()              { return ofType(HierarchyType.Unknown); }
    public static TypedExpressionStub dimension()              { return ofType(DimensionType.Unknown); }
    public static TypedExpressionStub symbol()                 { return ofType(SymbolType.INSTANCE); }
    public static TypedExpressionStub value()                  { return ofType(ScalarType.INSTANCE); }
    public static TypedExpressionStub nullExpression()         { return ofType(NullType.INSTANCE); }
    public static TypedExpressionStub empty()                  { return ofType(EmptyType.INSTANCE); }

    // ---- Expression --------------------------------------------------------

    @Override
    public DataType getCategory() {
        return TypeUtil.typeToCategory(type);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Expression cloneExp() {
        return this;   // immutable
    }

    @Override
    public Expression accept(Validator validator) {
        return this;   // already "validated": the type is given, not derived
    }

    @Override
    public void unparse(PrintWriter pw) {
        pw.print(label);
    }

    @Override
    public Calc<?> accept(ExpressionCompiler compiler) {
        throw notCompilable();
    }

    @Override
    public Object accept(QueryComponentVisitor visitor) {
        throw notCompilable();
    }

    @Override
    public String toString() {
        return label;
    }

    // ---- internals ---------------------------------------------------------

    private static TupleType tupleTypeOf(int arity) {
        Type[] elements = new Type[arity];
        java.util.Arrays.fill(elements, MemberType.Unknown);
        return new TupleType(elements);
    }

    private static Type canonicalTypeOf(DataType category) {
        return switch (category) {
        case SET       -> new SetType(MemberType.Unknown);
        case TUPLE     -> tupleTypeOf(2);
        case MEMBER    -> MemberType.Unknown;
        case LEVEL     -> LevelType.Unknown;
        case HIERARCHY -> HierarchyType.Unknown;
        case DIMENSION -> DimensionType.Unknown;
        case NUMERIC   -> NumericType.INSTANCE;
        case INTEGER   -> new DecimalType(Integer.MAX_VALUE, 0);
        case STRING    -> StringType.INSTANCE;
        case LOGICAL   -> BooleanType.INSTANCE;
        case DATE_TIME -> DateTimeType.INSTANCE;
        case SYMBOL    -> SymbolType.INSTANCE;
        case VALUE     -> ScalarType.INSTANCE;
        case NULL      -> NullType.INSTANCE;
        case EMPTY     -> EmptyType.INSTANCE;
        case CUBE, ARRAY, UNKNOWN -> throw notRepresentable(category);
        };
    }

    private static IllegalArgumentException notRepresentable(DataType category) {
        return new IllegalArgumentException("""
                TypedExpressionStub cannot represent DataType.%s

                Representable categories are:
                  SET, TUPLE, MEMBER, LEVEL, HIERARCHY, DIMENSION, NUMERIC, INTEGER,
                  STRING, LOGICAL, DATE_TIME, SYMBOL, VALUE, NULL, EMPTY

                A %s argument needs real cube metadata. Move the assertion to the
                evaluating stage (CalcAssertions), or record a waiver:
                    .waive(Promise.RESOLUTION, "needs a real %s argument")
                """.formatted(category, category, category));
    }

    private UnsupportedOperationException notCompilable() {
        return new UnsupportedOperationException("""
                TypedExpressionStub %s cannot be compiled.

                Stubs carry a Type for resolution only; they have no value and no Calc.
                Use CalcAssertions.assertThatSetExpr(connection, cube, mdx) for evaluation,
                ResultStyle and dependsOn assertions.
                """.formatted(label));
    }
}