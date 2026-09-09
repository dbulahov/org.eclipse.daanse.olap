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
package org.eclipse.daanse.olap.function.def.set.extract;

import static org.eclipse.daanse.olap.function.core.FunctionParameterR.param;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionInterface;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolutionResult;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.FunctionResolutionResultR;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;

public class ExtractResolver extends NoExpressionRequiredFunctionResolver {
    @Override
    public Optional<FunctionResolutionResult> resolve(Expression[] args, Validator validator) {
        List<Conversion> conversions = new ArrayList<>();
        if (args.length < 2) {
            return Optional.empty();
        }
        if (!validator.canConvert(0, args[0], DataType.SET, conversions)) {
            return Optional.empty();
        }
        for (int i = 1; i < args.length; ++i) {
            if (!validator.canConvert(0, args[i], DataType.HIERARCHY, conversions)) {
                return Optional.empty();
            }
        }

        // Whether the hierarchies named by args[1..] actually belong to the LHS set (and are
        // each named at most once) is a semantic check, not a shape/type check: it needs the
        // hierarchy identities named in args[0], and — for
        // Extract(X.Members * Y.Members * Z.Members, Z, X) — the ordinal of each one within
        // the LHS's dimensionality (X=0, Y=1, Z=2, so the extracted ordinals are {2, 0}).
        // resolve() must be a pure predicate over argument shape and never throw, so that
        // check is deliberately left to ExtractFunDef.getResultType/compileCall, which run
        // once resolution has already matched this overload and can report a precise,
        // diagnosed error instead of a generic "no function matches signature".
        FunctionParameterR[] parameterTypes = new FunctionParameterR[args.length];
        parameterTypes[0] = FunctionParameterR.param(DataType.SET);
        Arrays.fill(parameterTypes, 1, parameterTypes.length, FunctionParameterR.param(DataType.HIERARCHY));

        return Optional.of(FunctionResolutionResultR.of(new ExtractFunDef(parameterTypes), conversions));
    }

    @Override
    public OperationAtom getFunctionAtom() {
        return ExtractFunDef.functionAtom;
    }

    private static final List<FunctionMetaData> REPRESENTATIVE_METADATAS = List.<FunctionMetaData>of(
        FunctionMetaDataR.of(ExtractFunDef.functionAtom,
            "Returns a set of tuples from extracted hierarchy elements. The opposite of Crossjoin.", DataType.SET,
            param(DataType.SET),
            param(DataType.HIERARCHY, "Hierarchy").repeatable(1)).interfaceName(FunctionInterface.FILTER));

    @Override
    public List<FunctionMetaData> getRepresentativeFunctionMetaDatas() {
        return REPRESENTATIVE_METADATAS;
    }

}
