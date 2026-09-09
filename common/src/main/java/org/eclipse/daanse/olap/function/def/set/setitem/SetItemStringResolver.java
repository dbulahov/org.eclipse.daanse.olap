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
package org.eclipse.daanse.olap.function.def.set.setitem;

import static org.eclipse.daanse.olap.function.core.FunctionParameterR.param;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolutionResult;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.resolver.FunctionResolutionResultR;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;
import org.eclipse.daanse.olap.query.base.Expressions;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class SetItemStringResolver extends NoExpressionRequiredFunctionResolver {
    @Override
    public Optional<FunctionResolutionResult> resolve(Expression[] args, Validator validator) {
        List<Conversion> conversions = new ArrayList<>();
        if (args.length < 1) {
            return Optional.empty();
        }
        final Expression setExp = args[0];
        // Mirror SetItemIntResolver's generic conversion behavior (a Level/Member/Tuple/
        // Dimension/Hierarchy arg can convert to Set/Tuple, see TypeUtil.canConvert) instead
        // of demanding a literal SetType: a Level -> Set conversion in particular is declared
        // but never materialized into an actual set literal by compile time (see
        // ExtractContract's identical finding), so a strict instanceof check here silently
        // rejected calls the declared signature advertises as accepted.
        if (!validator.canConvert(0, setExp, DataType.SET, conversions)) {
            return Optional.empty();
        }
        final Type type0 = setExp.getType();
        final int arity;
        if (type0 instanceof SetType setType) {
            arity = setType.getArity();
        } else if (type0 instanceof TupleType tupleType) {
            arity = tupleType.getArity();
        } else {
            arity = 1;
        }
        // All args must be strings.
        for (int i = 1; i < args.length; i++) {
            if (!validator.canConvert(i, args[i], DataType.STRING, conversions)) {
                return Optional.empty();
            }
        }
        if (args.length - 1 != arity) {
            // Not a shape/type mismatch resolve() can coerce past — a genuine "no overload
            // matches" case. resolve() must be a pure predicate (see
            // CallAssert.resolutionDoesNotThrow): throwing here would abort the whole query's
            // validation with a stack trace instead of a clean "no function matches signature"
            // message, even when a differently-shaped call would have matched fine.
            return Optional.empty();
        }
        final DataType category = arity == 1 ? DataType.MEMBER : DataType.TUPLE;

        FunctionMetaData functionMetaData = new FunctionMetaDataR(SetItemFunDef.functionAtom,
                "Returns a tuple from the set specified in <Set>. The tuple to be returned is specified by the member name (or names) in <String>.",
                category, Expressions.functionParameterOf(args));

        return Optional.of(FunctionResolutionResultR.of(new SetItemFunDef(functionMetaData), conversions));
    }

    @Override
    public OperationAtom getFunctionAtom() {
        return SetItemFunDef.functionAtom;
    }

    private static final List<FunctionMetaData> REPRESENTATIVE_METADATAS = List.<FunctionMetaData>of(
            FunctionMetaDataR.of(SetItemFunDef.functionAtom,
                    "Returns a tuple from the set specified in <Set>. The tuple to be returned is specified by the member name (or names) in <String>.",
                    DataType.TUPLE,
                    param(DataType.SET),
                    param(DataType.STRING, "Member_Name").repeatable(1)));

    @Override
    public List<FunctionMetaData> getRepresentativeFunctionMetaDatas() {
        return REPRESENTATIVE_METADATAS;
    }

}
