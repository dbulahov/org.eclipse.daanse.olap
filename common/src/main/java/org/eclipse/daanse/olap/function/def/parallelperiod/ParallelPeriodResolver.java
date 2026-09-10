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
package org.eclipse.daanse.olap.function.def.parallelperiod;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionInterface;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class ParallelPeriodResolver extends AbstractFunctionDefinitionMultiResolver {

    private static OperationAtom atom = new FunctionOperationAtom("ParallelPeriod");
    private static String DESCRIPTION = "Returns a member from a prior period in the same relative position as a specified member.";
    // {"fm", "fml", "fmln", "fmlnm"}

    private static FunctionMetaData functionMetaDataWithoutParam = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, new FunctionParameterR[] {}).interfaceName(FunctionInterface.DATETIME);
    private static FunctionMetaData functionMetaDataWithLevel = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, new FunctionParameterR[] { FunctionParameterR.param(DataType.LEVEL) }).interfaceName(FunctionInterface.DATETIME);
    private static FunctionMetaData functionMetaDataWithLevelNumeric = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, new FunctionParameterR[] { FunctionParameterR.param(DataType.LEVEL),
                    FunctionParameterR.param(DataType.NUMERIC) }).interfaceName(FunctionInterface.DATETIME);
    private static FunctionMetaData functionMetaDataWithLevelNumericMember = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, new FunctionParameterR[] { FunctionParameterR.param(DataType.LEVEL),
                    FunctionParameterR.param(DataType.NUMERIC),
                    FunctionParameterR.param(DataType.MEMBER) }).interfaceName(FunctionInterface.DATETIME);

    public ParallelPeriodResolver() {
        // See ClosingPeriodResolved/OpeningPeriodResolved: a single FunctionMetaData with
        // trailing optional parameters lets FunctionMetaDataMatcher.match's positional, greedy
        // walk skip a parameter it cannot bind and fall through to the next one instead of
        // rejecting the call outright. With LEVEL, NUMERIC and MEMBER all optional here, a bare
        // one-arg NUMERIC or TUPLE call (neither converts to LEVEL) fell through into the
        // NUMERIC slot, and a bare one-arg HIERARCHY call (converts to neither LEVEL nor
        // NUMERIC) fell through all the way into the MEMBER slot — both silently accepted
        // instead of being rejected, only to fail later in ParallelPeriodFunDef.compileCall,
        // which unconditionally treats a one-arg call as (Level). Fixed the same way: split
        // into four separate declared overloads (0-arg, (Level), (Level, Numeric), (Level,
        // Numeric, Member)) — each with only required parameters, so a category that cannot
        // bind the parameter at its position fails the whole overload immediately instead of
        // being skipped and retried against a later, unrelated parameter. The four overloads
        // never overlap in arity, so list order does not matter for resolution the way it did
        // for ClosingPeriod's two same-arity overloads.
        super(List.of(new ParallelPeriodFunDef(functionMetaDataWithoutParam),
                new ParallelPeriodFunDef(functionMetaDataWithLevel),
                new ParallelPeriodFunDef(functionMetaDataWithLevelNumeric),
                new ParallelPeriodFunDef(functionMetaDataWithLevelNumericMember)));
    }
}
