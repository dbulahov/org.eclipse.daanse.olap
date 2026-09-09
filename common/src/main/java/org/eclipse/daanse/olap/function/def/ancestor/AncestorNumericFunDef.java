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

package org.eclipse.daanse.olap.function.def.ancestor;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.function.FunctionInterface;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class AncestorNumericFunDef extends AbstractFunctionDefinition {

	static final FunctionMetaData fmdNum = new FunctionMetaDataR(AncestorResolver.operationAtom,
			"Returns the ancestor of a member at a specified level, defined by the distance.", DataType.MEMBER,
			new FunctionParameterR[] { FunctionParameterR.param(DataType.MEMBER), FunctionParameterR.param(DataType.NUMERIC) }).interfaceName(FunctionInterface.NAVIGATION);

	public AncestorNumericFunDef() {
		super(fmdNum);
	}

	@Override
	public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
		final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
		Expression expressionOfArg1 = call.getArg(1);
		final Type type1 = expressionOfArg1.getType();

		if (!(type1 instanceof NumericType)) {
			new OlapRuntimeException("unexpected type: " + type1 + " sould be " + NumericType.class);
		}

		final IntegerCalc distanceCalc = compiler.compileInteger(call.getArg(1));
		return new AncestorNumericCalc(call.getType(), memberCalc, distanceCalc);
	}
}
