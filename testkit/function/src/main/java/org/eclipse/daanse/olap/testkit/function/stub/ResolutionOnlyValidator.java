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

import java.util.Map;

import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.common.ValidatorImpl;
import org.eclipse.daanse.olap.function.services.standard.StandardFunctions;

/**
 * A {@link org.eclipse.daanse.olap.api.query.Validator} that can resolve function calls
 * and nothing else: no query, no cube, no catalog reader.
 *
 * <p>{@code ValidatorImpl} implements seven of the nine {@code Validator} methods and
 * leaves {@code getQuery()} and {@code getCatalogReader()} open, plus its own abstract
 * {@code defineParameter}. All three are unreachable from
 * {@code ValidatorImpl.explainDef}, which is what this class exists to drive.
 */
public final class ResolutionOnlyValidator extends ValidatorImpl {

    private final boolean requiresExpression;

    private ResolutionOnlyValidator(FunctionService functionService, boolean requiresExpression) {
        super(functionService, Map.of());
        this.requiresExpression = requiresExpression;
    }

    /** Set context: a bare {@code *} may resolve to CrossJoin. The normal case. */
    public static ResolutionOnlyValidator inSetContext(FunctionService functionService) {
        return new ResolutionOnlyValidator(functionService, false);
    }

    /** Scalar context: a bare {@code *} must resolve to multiplication. */
    public static ResolutionOnlyValidator inScalarContext(FunctionService functionService) {
        return new ResolutionOnlyValidator(functionService, true);
    }

    /** Convenience: set context over {@code StandardFunctions.standard()}. */
    public static ResolutionOnlyValidator standard() {
        return inSetContext(StandardFunctions.standard());
    }

    @Override
    public boolean requiresExpression() {
        return requiresExpression;
    }

    @Override
    public Query getQuery() {
        throw noCube("getQuery()");
    }

    @Override
    public CatalogReader getCatalogReader() {
        throw noCube("getCatalogReader()");
    }

    @Override
    protected void defineParameter(Parameter parameter) {
        throw noCube("defineParameter(" + parameter.getName() + ")");
    }

    private static UnsupportedOperationException noCube(String member) {
        return new UnsupportedOperationException("""
                ResolutionOnlyValidator.%s was called, but this validator has no Query,
                no Cube and no CatalogReader.

                It exists to drive FunctionResolver.resolve(...) and ValidatorImpl.explainDef(...)
                with TypedExpressionStub arguments. The resolver under test needs cube metadata,
                so it cannot be covered by the cube-free stage.

                Either move this assertion to the evaluating stage (CalcAssertions),
                or record it as a waiver:
                    .waive(Promise.RESOLUTION, "needs Query.getCube().getTimeHierarchy()")
                """.formatted(member));
    }
}