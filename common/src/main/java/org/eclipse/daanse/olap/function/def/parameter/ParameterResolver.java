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
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractMetaDataMultiResolver;
import org.osgi.service.component.annotations.Component;

/**
 * Resolves calls to the Parameter MDX function.
 */
@Component(service = FunctionResolver.class)
public class ParameterResolver extends AbstractMetaDataMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Parameter");
    private static final List<String> RESERVED_WORDS = List.of("NUMERIC", "STRING");
    private static String DESCRIPTION = "Returns default value of parameter.";

    private static FunctionParameterR[] SySS = { FunctionParameterR.param(DataType.STRING, "Name"),
            new FunctionParameterR(DataType.SYMBOL, "Type", Optional.of(RESERVED_WORDS)), FunctionParameterR.param(DataType.STRING, "DefaultValue"),
            FunctionParameterR.param(DataType.STRING, "Description") };
    private static FunctionParameterR[] SyS = { FunctionParameterR.param(DataType.STRING, "Name"),
            new FunctionParameterR(DataType.SYMBOL, "Type", Optional.of(RESERVED_WORDS)), FunctionParameterR.param(DataType.STRING, "DefaultValue") };

    private static FunctionParameterR[] SynS = { FunctionParameterR.param(DataType.STRING, "Name"),
            new FunctionParameterR(DataType.SYMBOL, "Type", Optional.of(RESERVED_WORDS)), FunctionParameterR.param(DataType.NUMERIC, "DefaultValue"),
            FunctionParameterR.param(DataType.STRING, "Description") };
    private static FunctionParameterR[] Syn = { FunctionParameterR.param(DataType.STRING, "Name"),
            new FunctionParameterR(DataType.SYMBOL, "Type", Optional.of(RESERVED_WORDS)), FunctionParameterR.param(DataType.NUMERIC, "DefaultValue") };

    private static FunctionParameterR[] ShmS = { FunctionParameterR.param(DataType.STRING, "Name"),
            FunctionParameterR.param(DataType.HIERARCHY), FunctionParameterR.param(DataType.MEMBER, "DefaultValue"),
            FunctionParameterR.param(DataType.STRING, "Description") };
    private static FunctionParameterR[] Shm = { FunctionParameterR.param(DataType.STRING, "Name"),
            FunctionParameterR.param(DataType.HIERARCHY), FunctionParameterR.param(DataType.MEMBER, "DefaultValue") };

    private static FunctionParameterR[] ShxS = { FunctionParameterR.param(DataType.STRING, "Name"),
            FunctionParameterR.param(DataType.HIERARCHY), FunctionParameterR.param(DataType.SET, "DefaultValue"),
            FunctionParameterR.param(DataType.STRING, "Description") };
    private static FunctionParameterR[] Shx = { FunctionParameterR.param(DataType.STRING, "Name"),
            FunctionParameterR.param(DataType.HIERARCHY), FunctionParameterR.param(DataType.SET, "DefaultValue") };

    // {"fSSySS", "fSSyS", "fnSynS", "fnSyn", "fmShmS", "fmShm","fxShxS", "fxShx"}

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, SySS);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, SyS);

    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.NUMERIC, SynS);
    private static FunctionMetaData functionMetaData4 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.NUMERIC, Syn);

    private static FunctionMetaData functionMetaData5 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, ShmS);
    private static FunctionMetaData functionMetaData6 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, Shm);

    private static FunctionMetaData functionMetaData7 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, ShxS);
    private static FunctionMetaData functionMetaData8 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, Shx);



    @Override
    public List<String> getReservedWords() {
        return RESERVED_WORDS;
    }

    public ParameterResolver() {

        super(List.of(functionMetaData1, functionMetaData2, functionMetaData3, functionMetaData4, functionMetaData5,
                functionMetaData6, functionMetaData7, functionMetaData8));
    }

    @Override
    protected FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData,
            FunctionMetaData fmdTarget) {
        // ParameterFunDef.getParameterName throws when args[0] is not a string literal — a
        // precondition only the parser guarantees for real "Parameter('x', ...)" MDX. This
        // method is reached from resolve() (see AbstractMetaDataMultiResolver.resolve()),
        // which must stay a pure predicate (see CallAssert.resolutionDoesNotThrow, and
        // ParamRefContract's identical finding): a non-literal Name here is a genuine "no
        // overload matches" case, not something to throw past.
        if (!(args[0] instanceof Literal<?> nameLiteral) || args[0].getCategory() != DataType.STRING) {
            return null;
        }
        String parameterName = (String) nameLiteral.getValue();
        Expression typeArg = args[1];
        DataType category;
        Type type = typeArg.getType();
        switch (typeArg.getCategory()) {
        case DIMENSION:
        case HIERARCHY:
        case LEVEL:
            Dimension dimension = type.getDimension();
            if (!ParameterFunDef.isConstant(typeArg)) {
                throw FunUtil.newEvalException(functionMetaData,
                        new StringBuilder("Invalid parameter '").append(parameterName)
                                .append("'. Type must be a NUMERIC, STRING, or a dimension, ")
                                .append("hierarchy or level").toString());
            }
            if (dimension == null) {
                throw FunUtil.newEvalException(functionMetaData, new StringBuilder("Invalid dimension for parameter '")
                        .append(parameterName).append("'").toString());
            }
            type = new MemberType(type.getDimension(), type.getHierarchy(), type.getLevel(), null);
            category = DataType.MEMBER;
            break;

        case SYMBOL:
            // Same "resolve() must be a pure predicate" reasoning as the Name guard above:
            // this used to cast typeArg to Literal unconditionally, throwing
            // ClassCastException for any SYMBOL-typed argument the parser did not itself
            // produce as a literal (e.g. this test kit's stub-based probing).
            if (!(typeArg instanceof Literal<?> typeLiteral)) {
                return null;
            }
            String s = (String) typeLiteral.getValue();
            if (s.equalsIgnoreCase("NUMERIC")) {
                category = DataType.NUMERIC;
                type = NumericType.INSTANCE;
                break;
            } else if (s.equalsIgnoreCase("STRING")) {
                category = DataType.STRING;
                type = StringType.INSTANCE;
                break;
            }
            // fall through and throw error
        default:
            // Error is internal because the function call has already been
            // type-checked.
            throw FunUtil.newEvalException(functionMetaData, new StringBuilder("Invalid type for parameter '")
                    .append(parameterName).append("'; expecting NUMERIC, STRING or a hierarchy").toString());
        }

        // Default value
        Expression exp = args[2];
        //Validator validator = Util.createSimpleValidator(BuiltinFunTable.instance());
        //final List<Conversion> conversionList = new ArrayList<>();
        //String typeName = category.getName().toUpperCase();
        //if (!validator.canConvert(2, exp, category, conversionList)) {
        //    throw FunUtil.newEvalException(functionMetaData, new StringBuilder("Default value of parameter '")
        //            .append(parameterName).append("' is inconsistent with its type, ").append(typeName).toString());
        //}
        if (exp.getCategory() == DataType.SET && category == DataType.MEMBER) {
            // Default value is a set; take this an indication that
            // the type is 'set of <member type>'.
            type = new SetType(type);
        }
        if (category == DataType.MEMBER) {
            Type expType = exp.getType();
            if (expType instanceof SetType setType) {
                expType = setType.getElementType();
            }
            if (ParameterResolver.distinctFrom(type.getDimension(), expType.getDimension())
                    || ParameterResolver.distinctFrom(type.getHierarchy(), expType.getHierarchy())
                    || ParameterResolver.distinctFrom(type.getLevel(), expType.getLevel())) {
                throw FunUtil.newEvalException(functionMetaData,
                        new StringBuilder("Default value of parameter '").append(parameterName)
                                .append("' is not consistent with the parameter type '").append(type).toString());
            }
        }

        String parameterDescription = null;
        if (args.length > 3) {
            if (args[3] instanceof Literal && args[3].getCategory() == DataType.STRING) {
                parameterDescription = (String) ((Literal<?>) args[3]).getValue();
            } else {
                throw FunUtil.newEvalException(functionMetaData, new StringBuilder("Description of parameter '")
                        .append(parameterName).append("' must be a string constant").toString());
            }
        }

        return new ParameterFunDef(functionMetaData, parameterName, type, category, exp, parameterDescription);
    }

    private static <T> boolean distinctFrom(T t1, T t2) {
        return t1 != null && t2 != null && !t1.equals(t2);
    }

}
