/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.core.resolver;

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolutionResult;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;

public class NonFunctionResolver implements FunctionResolver {

    private FunctionMetaData functionMetaData;

    public NonFunctionResolver(FunctionMetaData functionMetaData) {
        this.functionMetaData = functionMetaData;
    }


    @Override
    public OperationAtom getFunctionAtom() {
        return functionMetaData.operationAtom();
    }

    @Override
    public Optional<FunctionResolutionResult> resolve(Expression[] args, Validator validator) {
        return Optional.empty();
    }

    @Override
    public List<FunctionMetaData> getRepresentativeFunctionMetaDatas() {
        return List.of(functionMetaData);
    }

    @Override
    public boolean requiresScalarExpressionOnArgument(int k) {
        DataType[] parameterDataTypes = functionMetaData.parameterDataTypes();
        return (k >= parameterDataTypes.length) || (parameterDataTypes[k] != DataType.SET);
    }

}
