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
package org.eclipse.daanse.olap.function.def.parameter;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractMetaDataMultiResolver;
import org.osgi.service.component.annotations.Component;

/**
 * Resolves calls to the ParamRef MDX function.
 */
@Component(service = FunctionResolver.class)
public class ParamRefResolver  extends AbstractMetaDataMultiResolver {

    private static FunctionOperationAtom atom = new FunctionOperationAtom("ParamRef");
    private static String DESCRIPTION = "Returns the current value of this parameter. If it is null, returns the default value.";
    
    private static FunctionParameterR[] S = { FunctionParameterR.param(DataType.STRING, "Name") };
    //"fvS"
    
    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.VALUE, S);

    public ParamRefResolver() {
        super(List.of(functionMetaData));
    }


    @Override
    protected FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData,
            FunctionMetaData fmdTarget) {
        // ParameterFunDef.getParameterName throws when args[0] is not a string literal — a
        // precondition only the parser guarantees for real "ParamRef('x')" MDX. This method
        // is reached from resolve() (see AbstractMetaDataMultiResolver.resolve()), which must
        // stay a pure predicate (see CallAssert.resolutionDoesNotThrow): a non-literal Name
        // here is a genuine "no overload matches" case, not something to throw past. Real
        // validation still gets a proper diagnosed error later, from QueryImpl.
        if (!(args[0] instanceof Literal<?> literal) || args[0].getCategory() != DataType.STRING) {
            return null;
        }
        String parameterName = (String) literal.getValue();
        return new ParameterFunDef(
            functionMetaData, parameterName, null, DataType.UNKNOWN, null,
            null);
    }

}
