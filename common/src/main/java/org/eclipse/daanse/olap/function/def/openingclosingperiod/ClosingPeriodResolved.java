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
package org.eclipse.daanse.olap.function.def.openingclosingperiod;

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
public class ClosingPeriodResolved extends AbstractFunctionDefinitionMultiResolver {

    private static OperationAtom atom = new FunctionOperationAtom("ClosingPeriod");
    private static String DESCRIPTION = "Returns the last descendant of a member at a level.";
    // {"fm", "fml", "fmlm", "fmm"}

    private static FunctionMetaData functionMetaDataWithoutParam = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, new FunctionParameterR[] {}).interfaceName(FunctionInterface.DATETIME);
    private static FunctionMetaData functionMetaDataWithLevel = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, new FunctionParameterR[] { FunctionParameterR.param(DataType.LEVEL) }).interfaceName(FunctionInterface.DATETIME);
    private static FunctionMetaData functionMetaDataWithLevelMember = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, new FunctionParameterR[] { FunctionParameterR.param(DataType.LEVEL),
                    FunctionParameterR.param(DataType.MEMBER) }).interfaceName(FunctionInterface.DATETIME);
    private static FunctionMetaData functionMetaDataWithMember = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, new FunctionParameterR[] { FunctionParameterR.param(DataType.MEMBER) }).interfaceName(FunctionInterface.DATETIME);

    public ClosingPeriodResolved() {
        // AbstractFunctionDefinitionMultiResolver.resolve() returns the *first* declaration in
        // this list that FunctionMetaDataMatcher.match accepts — it does not compare
        // conversion cost across the whole list the way resolution across separate resolvers
        // does. A Member argument converts to Level at cost 1 (see TypeUtil.canConvert), so
        // with the (Level) overload listed before the (Member) one, a genuine
        // ClosingPeriod(<Member>) call was being captured by the (Level) overload (arity 1,
        // treating the sole argument as a Level and substituting the default Time hierarchy's
        // current member) instead of the (Member) one it actually matches exactly. A Level
        // argument never converts to Member (TypeUtil.convertFromLevel has no MEMBER case), so
        // this reordering does not affect the (Level) overload's own resolution.
        super(List.of(new OpeningClosingPeriodFunDef(functionMetaDataWithoutParam, false),
                new OpeningClosingPeriodFunDef(functionMetaDataWithMember, false),
                new OpeningClosingPeriodFunDef(functionMetaDataWithLevel, false),
                new OpeningClosingPeriodFunDef(functionMetaDataWithLevelMember, false)));
    }
}
