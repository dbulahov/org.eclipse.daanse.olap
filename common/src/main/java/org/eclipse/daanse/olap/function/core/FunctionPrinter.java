/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2002-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
 * 
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial adapt parts of Syntax.class
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.core;

import java.io.PrintWriter;

import org.eclipse.daanse.mdx.model.api.expression.operation.AmpersandQuotedPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.CaseOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.CastOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.EmptyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InternalOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PostfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PrefixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.QuotedPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.FunctionCall;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.query.base.Expressions;

public class FunctionPrinter {

	public static void unparse(OperationAtom operationAtom, Expression[] args, PrintWriter pw) {

		switch (operationAtom) {
		case AmpersandQuotedPropertyOperationAtom _UNNAMED:

			args[0].unparse(pw);
			pw.print(".[&");
			pw.print(operationAtom.name());
			pw.print("]");
			return;

		case BracesOperationAtom _UNNAMED:
			Expressions.unparseExpressions(pw, args, "{", ", ", "}");
			return;

		case CastOperationAtom _UNNAMED:
			pw.print("CAST(");
			args[0].unparse(pw);
			pw.print(" AS ");
			args[1].unparse(pw);
			pw.print(")");
			return;

		case CaseOperationAtom _UNNAMED:
			if (operationAtom.name().equals("_CaseTest")) {
				pw.print("CASE");
				int j = 0;
				int clauseCount = (args.length - j) / 2;
				for (int i = 0; i < clauseCount; i++) {
					pw.print(" WHEN ");
					args[j++].unparse(pw);
					pw.print(" THEN ");
					args[j++].unparse(pw);
				}
				if (j < args.length) {
					pw.print(" ELSE ");
					args[j++].unparse(pw);
				}
				Util.assertTrue(j == args.length);
				pw.print(" END");
			} else {
				Util.assertTrue(operationAtom.name().equals("_CaseMatch"));

				pw.print("CASE ");
				int j = 0;
				args[j++].unparse(pw);
				int clauseCount = (args.length - j) / 2;
				for (int i = 0; i < clauseCount; i++) {
					pw.print(" WHEN ");
					args[j++].unparse(pw);
					pw.print(" THEN ");
					args[j++].unparse(pw);
				}
				if (j < args.length) {
					pw.print(" ELSE ");
					args[j++].unparse(pw);
				}
				Util.assertTrue(j == args.length);
				pw.print(" END");
			}
			return;

		case EmptyOperationAtom _UNNAMED:
			return;
		case FunctionOperationAtom _UNNAMED:

			Expressions.unparseExpressions(pw, args, new StringBuilder(operationAtom.name()).append("(").toString(),
					", ", ")");

			return;
		case InfixOperationAtom _UNNAMED:
			if (needParentheses(args)) {
				Expressions.unparseExpressions(pw, args, "(",
						new StringBuilder(" ").append(operationAtom.name()).append(" ").toString(), ")");
			} else {
				Expressions.unparseExpressions(pw, args, "",
						new StringBuilder(" ").append(operationAtom.name()).append(" ").toString(), "");
			}
			return;
		case InternalOperationAtom _UNNAMED:
			return;
		case MethodOperationAtom _UNNAMED:
			Util.assertTrue(args.length >= 1);
			args[0].unparse(pw); // 'this'
			pw.print(".");
			pw.print(operationAtom.name());
			pw.print("(");
			for (int i = 1; i < args.length; i++) {
				if (i > 1) {
					pw.print(", ");
				}
				args[i].unparse(pw);
			}
			pw.print(")");

			return;

		case ParenthesesOperationAtom _UNNAMED:

			Expressions.unparseExpressions(pw, args, "(", ", ", ")");

			return;
		case PlainPropertyOperationAtom _UNNAMED:

			args[0].unparse(pw);
			pw.print(".");
			pw.print(operationAtom.name());
			return;
		case PostfixOperationAtom _UNNAMED:

			if (needParentheses(args)) {
				Expressions.unparseExpressions(pw, args, "(", null,
						new StringBuilder(" ").append(operationAtom.name()).append(")").toString());
			} else {
				Expressions.unparseExpressions(pw, args, "", null, " " + operationAtom.name());
			}
			return;
		case PrefixOperationAtom _UNNAMED:

			if (needParentheses(args)) {
				Expressions.unparseExpressions(pw, args,
						new StringBuilder("(").append(operationAtom.name()).append(" ").toString(), null, ")");
			} else {
				Expressions.unparseExpressions(pw, args, new StringBuilder(operationAtom.name()).append(" ").toString(),
						null, "");
			}
			return;
		case QuotedPropertyOperationAtom _UNNAMED:
			args[0].unparse(pw);
			pw.print(".&");
			pw.print(operationAtom.name());
			return;

		}
	}

	private static boolean needParentheses(Expression[] args) {
		return !(args.length == 1 && args[0] instanceof FunctionCall fc
				&& fc.getOperationAtom() instanceof ParenthesesOperationAtom);
	}

	public static String getSignature(FunctionMetaData functionMetaData) {

		DataType[] parameterDataTypes = functionMetaData.parameterDataTypes();
		DataType returnCategory = functionMetaData.returnCategory();

		return getSignature(functionMetaData.operationAtom(), returnCategory, parameterDataTypes);

	}

	public static String getSignature(OperationAtom operationAtom, DataType returnType, DataType[] parameterDataTypes) {
		String name = operationAtom.name();

		switch (operationAtom) {
		case PlainPropertyOperationAtom a: {
			return new StringBuilder(getTypeDescription(parameterDataTypes, 0)).append(".").append(name).toString();
		}

		case MethodOperationAtom a: {

			return new StringBuilder(returnType == DataType.UNKNOWN ? ""
					: new StringBuilder(getTypeDescription(returnType)).append(" ").toString())
					.append(getTypeDescription(parameterDataTypes, 0)).append(".").append(name).append("(")
					.append(getTypeDescriptionCommaList(parameterDataTypes, 1)).append(")").toString();
		}
		case InfixOperationAtom a: {
			return new StringBuilder(getTypeDescription(parameterDataTypes, 0)).append(" ").append(name).append(" ")
					.append(getTypeDescription(parameterDataTypes, 1)).toString();

		}

		case PrefixOperationAtom a: {
			return new StringBuilder(name).append(" ").append(getTypeDescription(parameterDataTypes, 0)).toString();

		}

		case PostfixOperationAtom a: {
			return new StringBuilder(getTypeDescription(parameterDataTypes, 0)).append(" ").append(name).toString();

		}

		case BracesOperationAtom a: {
			return new StringBuilder("{").append(getTypeDescriptionCommaList(parameterDataTypes, 0)).append("}")
					.toString();

		}

		case ParenthesesOperationAtom a: {
			return new StringBuilder("(").append(getTypeDescriptionCommaList(parameterDataTypes, 0)).append(")")
					.toString();

		}

		case CaseOperationAtom a: {
			String s = getTypeDescription(parameterDataTypes, 0);
			if (parameterDataTypes.length > 0 && parameterDataTypes[0] == DataType.LOGICAL) {
				return new StringBuilder("CASE WHEN ").append(s).append(" THEN <Expression> ... END").toString();
			} else {
				return new StringBuilder("CASE ").append(s).append(" WHEN ").append(s)
						.append(" THEN <Expression> ... END").toString();
			}
		}

		case CastOperationAtom a: {
			return "CAST(<Expression> AS <Type>)";

		}

		case EmptyOperationAtom a: {
			return "";

		}
		default:

			return new StringBuilder((returnType == DataType.UNKNOWN ? ""
					: new StringBuilder(getTypeDescription(returnType)).append(" ").toString())).append(name)
					.append("(").append(getTypeDescriptionCommaList(parameterDataTypes, 0)).append(")").toString();
		}
	}

	private static String getTypeDescription(DataType type) {
		return new StringBuilder("<").append(type.getPrettyName()).append(">").toString();
	}

	/**
	 * Renders the type at {@code index}, or {@code "<?>"} when {@code types} is shorter than
	 * the atom's own shape expects. getSignature is also used to render diagnostic text for
	 * probed calls whose argument count does not match the atom (for example a signature
	 * probe trying a property atom with zero arguments): it must describe such a call rather
	 * than throw.
	 */
	private static String getTypeDescription(DataType[] types, int index) {
		return index < types.length ? getTypeDescription(types[index]) : "<?>";
	}

	private static String getTypeDescriptionCommaList(DataType[] types, int start) {
		int initialSize = (types.length - start) * 16;
		StringBuilder sb = new StringBuilder(initialSize > 0 ? initialSize : 16);
		for (int i = start; i < types.length; i++) {
			if (i > start) {
				sb.append(", ");
			}
			sb.append("<").append(types[i].getPrettyName()).append(">");
		}
		return sb.toString();
	}

}
