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
package org.eclipse.daanse.olap.function.def.periodstodate;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionInterface;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class PeriodsToDateResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("PeriodsToDate");
    private static String DESCRIPTION = "Returns a set of periods (members) from a specified level starting with the first period and ending with a specified member.";
    // Must be LEVEL/MEMBER, matching what compileCall actually compiles them as
    // (compiler.compileLevel(arg 0), compiler.compileMember(arg 1)) and the "fxlm" (Level,
    // Member) signature the comment below documents. This used to be declared SET/NUMERIC —
    // a plain LEVEL and MEMBER each still satisfy that by implicit conversion (Level -> Set
    // cost 1, Member -> Numeric cost 3) so ordinary calls kept resolving, but it also let a
    // genuine SET or NUMERIC argument (e.g. "PeriodsToDate([Time].[Month], 5)") match at cost
    // 0 and then crash in compileCall instead of being cleanly rejected at resolution.
    private static FunctionParameterR[] lm = { FunctionParameterR.param(DataType.LEVEL, "Level").asOptional(),
            FunctionParameterR.param(DataType.MEMBER, "Member").asOptional() };
    // {"fx", "fxl", "fxlm"}

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, lm).interfaceName(FunctionInterface.DATETIME);

    public PeriodsToDateResolver() {
        super(List.of(new PeriodsToDateFunDef(functionMetaData)));
    }
}
