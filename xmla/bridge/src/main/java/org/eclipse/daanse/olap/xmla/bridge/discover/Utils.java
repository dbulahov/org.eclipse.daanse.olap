/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.xmla.bridge.discover;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.daanse.mdx.model.api.expression.operation.EmptyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InternalOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.DatabaseColumn;
import org.eclipse.daanse.olap.api.element.DatabaseSchema;
import org.eclipse.daanse.olap.api.element.DatabaseTable;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.DimensionType;
import org.eclipse.daanse.olap.api.element.DrillThroughAction;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.KPI;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.StoredMeasure;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.result.Property;
import org.eclipse.daanse.olap.api.result.Property.TypeFlag;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.xmla.api.RequestMetaData;
import org.eclipse.daanse.xmla.api.UserRolePrincipal;
import org.eclipse.daanse.xmla.api.VarType;
import org.eclipse.daanse.xmla.api.XmlaConstants;
import org.eclipse.daanse.xmla.api.XmlaConstants.DBType;
import org.eclipse.daanse.xmla.api.common.enums.ColumnOlapTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.CubeSourceEnum;
import org.eclipse.daanse.xmla.api.common.enums.CubeTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.CustomRollupSettingEnum;
import org.eclipse.daanse.xmla.api.common.enums.DimensionCardinalityEnum;
import org.eclipse.daanse.xmla.api.common.enums.DimensionTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.DimensionUniqueSettingEnum;
import org.eclipse.daanse.xmla.api.common.enums.HierarchyOriginEnum;
import org.eclipse.daanse.xmla.api.common.enums.InterfaceNameEnum;
import org.eclipse.daanse.xmla.api.common.enums.LevelDbTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.LevelOriginEnum;
import org.eclipse.daanse.xmla.api.common.enums.LevelTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.LevelUniqueSettingsEnum;
import org.eclipse.daanse.xmla.api.common.enums.MeasureAggregatorEnum;
import org.eclipse.daanse.xmla.api.common.enums.MemberTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.OriginEnum;
import org.eclipse.daanse.xmla.api.common.enums.PropertyContentTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.PropertyOriginEnum;
import org.eclipse.daanse.xmla.api.common.enums.PropertyTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.ScopeEnum;
import org.eclipse.daanse.xmla.api.common.enums.SetEvaluationContextEnum;
import org.eclipse.daanse.xmla.api.common.enums.StructureEnum;
import org.eclipse.daanse.xmla.api.common.enums.TableTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.TreeOpEnum;
import org.eclipse.daanse.xmla.api.common.enums.VisibilityEnum;
import org.eclipse.daanse.xmla.api.discover.dbschema.columns.DbSchemaColumnsResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.sourcetables.DbSchemaSourceTablesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.tables.DbSchemaTablesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.tablesinfo.DbSchemaTablesInfoResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.cubes.MdSchemaCubesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.demensions.MdSchemaDimensionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.functions.MdSchemaFunctionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.hierarchies.MdSchemaHierarchiesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.kpis.MdSchemaKpisResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.levels.MdSchemaLevelsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroups.MdSchemaMeasureGroupsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measures.MdSchemaMeasuresResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.members.MdSchemaMembersResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.properties.MdSchemaPropertiesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.sets.MdSchemaSetsResponseRow;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.columns.DbSchemaColumnsResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.schemata.DbSchemaSchemataResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.tables.DbSchemaTablesResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.tablesinfo.DbSchemaTablesInfoResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.cubes.MdSchemaCubesResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.demensions.MdSchemaDimensionsResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.functions.MdSchemaFunctionsResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.hierarchies.MdSchemaHierarchiesResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.kpis.MdSchemaKpisResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.levels.MdSchemaLevelsResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measuregroups.MdSchemaMeasureGroupsResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measures.MdSchemaMeasuresResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.members.MdSchemaMembersResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.properties.MdSchemaPropertiesResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.sets.MdSchemaSetsResponseRowR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private static final String SCHEMA = "SCHEMA";
    private static final String SYSTEM_TABLE = "SYSTEM TABLE";
    private static final String TABLE = "TABLE";
    protected static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        // constructor
    }

    static Optional<String> getRoles(List<String> roles) {
        if (roles != null) {
            return Optional.of(roles.stream().collect(Collectors.joining(",")));
        }
        return Optional.empty();
    }

    public static List<DbSchemaColumnsResponseRow> getDbSchemaColumnsResponseRow(Catalog catalog,
            Optional<String> oTableSchema, Optional<String> oTableName, Optional<String> oColumnName,
            Optional<ColumnOlapTypeEnum> oColumnOlapType) {

        return catalog.getCubes().stream().sorted(Comparator.comparing(Cube::getName))
                .flatMap(c -> getDbSchemaColumnsResponseRow(catalog.getDatabaseSchemas(), catalog.getName(), null, c,
                        oTableName, oColumnName, oColumnOlapType).stream())
                .toList();
    }

    static List<DbSchemaColumnsResponseRow> getDbSchemaColumnsResponseRow(
            List<? extends DatabaseSchema> databaseSchemas, String catalogName, String schemaName, Cube cube,
            Optional<String> oTableName, Optional<String> oColumnName, Optional<ColumnOlapTypeEnum> oColumnOlapType) {
        Integer ordinalPosition = 1;
        List<DbSchemaColumnsResponseRow> result = new ArrayList<>();
        if (!oTableName.isPresent() || (oTableName.isPresent() && oTableName.get().equals(cube.getName()))) {
            final boolean emitInvisibleMembers = true; // TODO
            if (!oColumnOlapType.isPresent()
                    || (oColumnOlapType.isPresent() && oColumnOlapType.get().equals(ColumnOlapTypeEnum.ATTRIBUTE))) {
                for (Dimension dimension : cube.getDimensions()) {
                    populateDimensionForDbSchemaColumns(catalogName, schemaName, cube, dimension, ordinalPosition,
                            result);
                }
            }
            if (!oColumnOlapType.isPresent()
                    || (oColumnOlapType.isPresent() && oColumnOlapType.get().equals(ColumnOlapTypeEnum.MEASURE))) {
                populateMeasuresForDbSchemaColumns(schemaName, ordinalPosition, cube, emitInvisibleMembers, oTableName,
                        oColumnName, result);
            }
        }
        if (!oColumnOlapType.isPresent()
                || (oColumnOlapType.isPresent() && oColumnOlapType.get().equals(ColumnOlapTypeEnum.SCHEMA))) {
            populateDbSchemasForDbSchemaColumns(databaseSchemas, catalogName, ordinalPosition, oTableName, oColumnName,
                    result);
        }
        return result;
    }

    private static void populateMeasuresForDbSchemaColumns(String schemaName, Integer ordinalPosition, Cube cube,
            boolean emitInvisibleMembers, Optional<String> oTableName, Optional<String> oColumnName,
            List<DbSchemaColumnsResponseRow> result) {
        for (Member measure : cube.getMeasures()) {
            Boolean visible = true;
            Object o = measure.getPropertyValue("$visible");

            if (o != null) {
                visible = Boolean.valueOf(o.toString());
            }
            if (!emitInvisibleMembers && !visible) {
                continue;
            }

            String memberName = measure.getName();
            final String columnName = "Measures:" + memberName;
            if (oColumnName.isPresent() && !oColumnName.get().equals(columnName)) {
                continue;
            }
            String cubeName = cube.getName();
            result.add(new DbSchemaColumnsResponseRowR(Optional.of(cubeName), Optional.ofNullable(schemaName),
                    Optional.of(cubeName), Optional.of(columnName), Optional.empty(), Optional.empty(),
                    Optional.of(ordinalPosition++), Optional.of(false), Optional.empty(), Optional.empty(),
                    Optional.of(false), Optional.of(XmlaConstants.DBType.R8.xmlaOrdinal()), Optional.empty(),
                    Optional.of(0), Optional.of(0), Optional.of(16), Optional.of(255), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty()));
        }
    }

    private static void populateDbSchemasForDbSchemaColumns(List<? extends DatabaseSchema> databaseSchemas,
            String catalogName, Integer ordinalPosition, Optional<String> oTableName, Optional<String> oColumnName,
            List<DbSchemaColumnsResponseRow> result) {
        if (databaseSchemas != null) {
            for (DatabaseSchema databaseSchema : databaseSchemas) {
                if (databaseSchema.getDbTables() != null) {
                    for (DatabaseTable databaseTable : databaseSchema.getDbTables()) {
                        if (!oTableName.isPresent()
                                || (oTableName.isPresent() && oTableName.get().equals(databaseTable.getName()))) {
                            if (databaseTable.getDbColumns() != null) {
                                for (DatabaseColumn databaseColumn : databaseTable.getDbColumns()) {
                                    if (!oColumnName.isPresent() || (oColumnName.isPresent()
                                            && oColumnName.get().equals(databaseColumn.getName()))) {
                                        result.add(new DbSchemaColumnsResponseRowR(Optional.of(catalogName),
                                                Optional.ofNullable(databaseSchema.getName()),
                                                Optional.of(databaseTable.getName()),
                                                Optional.of(databaseColumn.getName()), Optional.empty(),
                                                Optional.empty(), Optional.of(ordinalPosition++), Optional.of(false),
                                                Optional.empty(), Optional.empty(), Optional.of(false),
                                                Optional.of(XmlaConstants.DBType.R8.xmlaOrdinal()), Optional.empty(),
                                                Optional.of(0), Optional.of(0), Optional.of(16), Optional.of(255),
                                                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                                                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                                                Optional.empty(), Optional.empty(), Optional.empty(),
                                                Optional.of(ColumnOlapTypeEnum.SCHEMA)));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void populateDimensionForDbSchemaColumns(String catalogName, String schemaName, Cube cube,
            Dimension dimension, Integer ordinalPosition, List<DbSchemaColumnsResponseRow> result) {
        for (Hierarchy hierarchy : dimension.getHierarchies()) {
            ordinalPosition = populateHierarchyForDbSchemaColumns(catalogName, schemaName, cube, hierarchy,
                    ordinalPosition, result);
        }

    }

    static int populateHierarchyForDbSchemaColumns(String catalogName, String schemaName, Cube cube,
            Hierarchy hierarchy, int ordinalPosition, List<DbSchemaColumnsResponseRow> result) {
        String cubeName = cube.getName();
        String hierarchyName = hierarchy.getName();
        result.add(new DbSchemaColumnsResponseRowR(Optional.of(catalogName), Optional.ofNullable(schemaName),
                Optional.of(cubeName), Optional.of(new StringBuilder(hierarchyName).append(":(All)!NAME").toString()),
                Optional.empty(), Optional.empty(), Optional.of(ordinalPosition++), Optional.of(false),
                Optional.empty(), Optional.empty(), Optional.of(false),
                Optional.of(XmlaConstants.DBType.WSTR.xmlaOrdinal()), Optional.empty(), Optional.of(0), Optional.of(0),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

        result.add(new DbSchemaColumnsResponseRowR(Optional.of(catalogName), Optional.ofNullable(schemaName),
                Optional.of(cubeName),
                Optional.of(new StringBuilder(hierarchyName).append(":(All)!UNIQUE_NAME").toString()), Optional.empty(),
                Optional.empty(), Optional.of(ordinalPosition++), Optional.of(false), Optional.empty(),
                Optional.empty(), Optional.of(false), Optional.of(XmlaConstants.DBType.WSTR.xmlaOrdinal()),
                Optional.empty(), Optional.of(0), Optional.of(0), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty()));
        return ordinalPosition;
    }

    static List<DbSchemaSchemataResponseRowR> getDbSchemaSchemataResponseRow(Catalog catalog, String schemaNameReq,
            String schemaOwnerRequ) {
        return catalog.getDatabaseSchemas().stream()
                .filter(dbs -> (schemaNameReq == null || dbs.getName().equals(schemaNameReq)))
                .map(dbs -> new DbSchemaSchemataResponseRowR(catalog.getName(), dbs.getName(), "")).toList();
    }

    static List<MdSchemaCubesResponseRow> getMdSchemaCubesResponseRow(Catalog catalog, Optional<String> schemaName,
            Optional<String> cubeName, Optional<String> baseCubeName, Optional<CubeSourceEnum> cubeSource,
            RequestMetaData metaData) {

        return getMdSchemaCubesResponseRow(catalog, cubeName, baseCubeName, cubeSource);
    }

    static List<MdSchemaFunctionsResponseRow> getMdSchemaFunctionsResponseRow(Context c, Optional<String> oLibraryName,
            Optional<InterfaceNameEnum> oInterfaceName, Optional<OriginEnum> oOrigin, RequestMetaData metaData) {
        List<MdSchemaFunctionsResponseRow> result = new ArrayList<>();
        List<FunctionMetaData> fmList = c.getFunctionService().getFunctionMetaDatas();
        StringBuilder buf = new StringBuilder(50);
        for (FunctionMetaData fm : fmList) {
            if (fm.operationAtom() instanceof EmptyOperationAtom//
                    || fm.operationAtom() instanceof InternalOperationAtom//
                    || fm.operationAtom() instanceof ParenthesesOperationAtom//
            ) {
                continue;
            }

            DataType[] paramCategories = fm.parameterDataTypes();
            DataType returnCategory = fm.returnCategory();

            // Convert Windows newlines in 'description' to UNIX format.
            String description = fm.description();
            if (description != null) {
                description = fm.description().replace("\r", "");
            }
            if ((paramCategories == null) || (paramCategories.length == 0)) {
                result.add(new MdSchemaFunctionsResponseRowR(Optional.ofNullable(fm.operationAtom().name()),
                        Optional.ofNullable(description), "(none)", Optional.of(1), Optional.of(OriginEnum.MSOLAP),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.ofNullable(fm.operationAtom().name()), Optional.empty(),
                        Optional.empty()));
            } else {

                buf.setLength(0);
                for (int j = 0; j < paramCategories.length; j++) {
                    DataType v = paramCategories[j];
                    if (j > 0) {
                        buf.append(", ");
                    }
                    buf.append(v.getPrittyName());
                }

                VarType varType = VarType.forCategory(returnCategory.getName());
                result.add(new MdSchemaFunctionsResponseRowR(Optional.ofNullable(fm.operationAtom().name()),
                        Optional.ofNullable(description), buf.toString(), Optional.of(varType.ordinal()),
                        Optional.of(OriginEnum.MSOLAP), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.ofNullable(fm.operationAtom().name()), Optional.empty(), Optional.empty()));
            }
        }
        return result;
    }

    private static List<MdSchemaCubesResponseRow> getMdSchemaCubesResponseRow(Catalog catalog,
            Optional<String> cubeName, Optional<String> baseCubeName, Optional<CubeSourceEnum> cubeSource) {
        List<Cube> cubes = catalog.getCubes();
        return Utils.getCubesWithFilter(cubes, cubeName).stream()
                .flatMap(cube -> getMdSchemaCubesResponseRow(catalog.getName(), cube).stream()).toList();
    }

    private static List<MdSchemaCubesResponseRow> getMdSchemaCubesResponseRow(String catalogName, Cube cube) {
        if (cube != null) {
            if (cube.isVisible()) {
                String desc = cube.getDescription();
                if (desc == null) {
                    desc = new StringBuilder(catalogName).append(" Schema - ").append(cube.getName()).append(" Cube")
                            .toString();
                }
                return List.of(new MdSchemaCubesResponseRowR(catalogName, Optional.ofNullable(null),
                        Optional.ofNullable(cube.getName()), Optional.of(CubeTypeEnum.CUBE), // TODO get cube type from
                                                                                             // olap
                        Optional.empty(), Optional.empty(), Optional.of(LocalDateTime.now()), // TODO get create date
                                                                                              // from olap
                        Optional.empty(), Optional.of(LocalDateTime.now()), Optional.empty(), Optional.ofNullable(desc),
                        Optional.of(true), Optional.of(false), Optional.of(false), Optional.of(false),
                        Optional.ofNullable(cube.getCaption() == null ? cube.getName() : cube.getCaption()),
                        Optional.ofNullable(cube.getName()), Optional.of(CubeSourceEnum.CUBE), Optional.empty()));
            }
        }
        return List.of();
    }

    private static List<? extends Cube> getCubeWithFilter(Cube[] cubes, Optional<String> cubeName) {
        return getCubesWithFilter(Stream.of(cubes).toList(), cubeName);
    }

    private static List<? extends Cube> getCubeWithFilter(List<? extends Cube> cubes, Optional<String> cubeName) {
        if (cubeName.isPresent()) {
            return getCubeWithFilter(cubes, cubeName.get());
        }
        return cubes;
    }

    private static List<? extends Cube> getCubeWithFilter(List<? extends Cube> cubes, String cubeName) {
        if (cubeName != null) {
            return cubes.stream().filter(c -> cubeName.equals(c.getName())).toList();
        }
        return cubes;
    }

    static boolean isDataTypeCond(XmlaConstants.DBType wstr, Optional<LevelDbTypeEnum> oLevelDbType) {
        if (oLevelDbType.isPresent()) {
            return wstr.xmlaOrdinal() == oLevelDbType.get().ordinal();
        }
        return true;
    }

    public static List<DbSchemaTablesResponseRow> getDbSchemaTablesResponseRow(Catalog catalog,
            Optional<String> oTableSchema, Optional<String> oTableName, Optional<String> oTableType) {
        List<? extends DatabaseSchema> dbSchemas = catalog.getDatabaseSchemas();
        String catalogName = catalog.getName();
        List<DbSchemaTablesResponseRow> result = new ArrayList<>();

        for (Cube cube : catalog.getCubes()) {

            String desc = cube.getDescription();
            if (desc == null) {
                desc = new StringBuilder(catalogName).append(" - ").append(cube.getName()).append(" Cube").toString();
            }
            if (isTableType(oTableType, TABLE)) {
                result.add(new DbSchemaTablesResponseRowR(Optional.of(catalogName), Optional.ofNullable(null),
                        Optional.of(cube.getName()), Optional.of(TABLE), Optional.empty(), Optional.of(desc),
                        Optional.empty(), Optional.empty(), Optional.empty()));
            }

            if (isTableType(oTableType, SYSTEM_TABLE)) {
                for (Dimension dimension : cube.getDimensions()) {
                    populateDimensionForDbSchemaTables(catalogName, null, cube, dimension, result);
                }
            }
        }
        if (isTableType(oTableType, SCHEMA)) {
            populateDbSchemasForDbSchemaTables(catalogName, null, dbSchemas, result);
        }
        return result;

    }

    private static void populateDbSchemasForDbSchemaTables(String catalogName, String schemaName,
            List<? extends DatabaseSchema> dbSchemas, List<DbSchemaTablesResponseRow> result) {
        if (dbSchemas != null) {
            for (DatabaseSchema dbSchema : dbSchemas) {
                List<? extends DatabaseTable> tables = dbSchema.getDbTables();
                if (tables != null) {
                    for (DatabaseTable table : tables) {
                        String tableName = table.getName();
                        result.add(new DbSchemaTablesResponseRowR(Optional.of(catalogName),
                                Optional.ofNullable(dbSchema.getName()), Optional.of(tableName), Optional.of(SCHEMA),
                                Optional.empty(), Optional.ofNullable(table.getDescription()), Optional.empty(),
                                Optional.empty(), Optional.empty()));
                    }
                }
            }
        }
    }

    private static void populateDimensionForDbSchemaTables(String catalogName, String schemaName, Cube cube,
            Dimension dimension, List<DbSchemaTablesResponseRow> result) {
        if (dimension != null) {
            for (Hierarchy hierarchy : dimension.getHierarchies()) {
                populateHierarchyForDbSchemaTables(catalogName, schemaName, cube, dimension.getName(), hierarchy,
                        result);
            }
        }
    }

    private static void populateHierarchyForDbSchemaTables(String catalogName, String schemaName, Cube cube,
            String dimensionName, Hierarchy hierarchy, List<DbSchemaTablesResponseRow> result) {
        if (hierarchy.getLevels() != null) {
            result.addAll(hierarchy.getLevels().stream().map(l -> {
                String cubeName = cube.getName();
                String hierarchyName = getHierarchyName(hierarchy.getName(), dimensionName);
                String levelName = l.getName();

                String tableName = cubeName + ':' + hierarchyName + ':' + levelName;

                String desc = l.getDescription();
                if (desc == null) {
                    desc = new StringBuilder(catalogName).append(" - ").append(cubeName).append(" Cube - ")
                            .append(hierarchyName).append(" Hierarchy - ").append(levelName).append(" Level")
                            .toString();
                }

                return new DbSchemaTablesResponseRowR(Optional.of(catalogName), Optional.ofNullable(schemaName),
                        Optional.of(tableName), Optional.of(SYSTEM_TABLE), Optional.empty(), Optional.of(desc),
                        Optional.empty(), Optional.empty(), Optional.empty());

            }).toList());
        }
    }

    private static String getHierarchyName(String hierarchyName, String dimensionName) {
        // TODO use Properties from context
        if (!hierarchyName.equals(dimensionName)) {
            hierarchyName = dimensionName + "." + hierarchyName;
        }
        return hierarchyName;
    }

    private static boolean isTableType(Optional<String> oTableType, String type) {
        if (oTableType.isPresent()) {
            return oTableType.get().equals(type);
        }
        return true;
    }

    static List<DbSchemaTablesInfoResponseRow> getDbSchemaTablesInfoResponseRow(Catalog catalog,
            Optional<String> oSchemaName, String tableName, TableTypeEnum tableType) {
        return getDbSchemaTablesInfoResponseRow(catalog, tableName, tableType);
    }

    private static List<DbSchemaTablesInfoResponseRow> getDbSchemaTablesInfoResponseRow(Catalog catalog,
            String tableName, TableTypeEnum tableType) {
        return catalog.getCubes().stream().sorted(Comparator.comparing(Cube::getName))
                .map(c -> getDbSchemaTablesInfoResponseRow(catalog.getName(), null, c, tableName, tableType)).toList();
    }

    private static DbSchemaTablesInfoResponseRow getDbSchemaTablesInfoResponseRow(String catalogName, String schemaName,
            Cube cube, String tableName, TableTypeEnum tableType) {
        String cubeName = cube.getName();
        String desc = cube.getDescription();
        if (desc == null) {
            desc = catalogName + " - " + cubeName + " Cube";
        }
        // TODO: SQL Server returns 1000000 for all tables
        long cardinality = 1000000l;
        return new DbSchemaTablesInfoResponseRowR(Optional.of(catalogName), Optional.ofNullable(schemaName), cubeName,
                TableTypeEnum.TABLE.name(), Optional.empty(), Optional.of(false), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(cardinality), Optional.of(desc),
                Optional.empty());
    }

    static List<MdSchemaLevelsResponseRow> getMdSchemaLevelsResponseRow(Catalog catalog, Optional<String> oSchemaName,
            Optional<String> oCubeName, Optional<String> oDimensionUniqueName, Optional<String> oHierarchyUniqueName,
            Optional<String> oLevelName, Optional<String> oLevelUniqueName, Optional<VisibilityEnum> oLevelVisibility,
            RequestMetaData metaData) {
        return getMdSchemaLevelsResponseRow(catalog.getName(), catalog, oCubeName, oDimensionUniqueName,
                oHierarchyUniqueName, oLevelName, oLevelUniqueName, oLevelVisibility);
    }

    static List<MdSchemaDimensionsResponseRow> getMdSchemaDimensionsResponseRow(Catalog catalog,
            Optional<String> oSchemaName, Optional<String> oCubeName, Optional<String> oDimensionName,
            Optional<String> oDimensionUniqueName, Optional<CubeSourceEnum> cubeSource,
            Optional<VisibilityEnum> oDimensionVisibility, Optional<Boolean> deep, RequestMetaData metaData) {
        return getMdSchemaDimensionsResponseRow(catalog.getName(), catalog, oCubeName, oDimensionName,
                oDimensionUniqueName, cubeSource, oDimensionVisibility, deep);

    }

    static List<MdSchemaMeasureGroupDimensionsResponseRow> getMdSchemaMeasureGroupDimensionsResponseRow(Catalog catalog,
            Optional<String> oSchemaName, Optional<String> oCubeName, Optional<String> oMeasureGroupName,
            Optional<String> oDimensionUniqueName, Optional<VisibilityEnum> oDimensionVisibility,
            RequestMetaData metaData) {
        return getMdSchemaMeasureGroupDimensionsResponseRow(catalog, oCubeName, oMeasureGroupName, oDimensionUniqueName,
                oDimensionVisibility);
    }

    static List<MdSchemaMembersResponseRow> getMdSchemaMembersResponseRow(Catalog catalog, Optional<String> oSchemaName,
            Optional<String> oCubeName, Optional<String> oDimensionUniqueName, Optional<String> oHierarchyUniqueName,
            Optional<String> oLevelUniqueName, Optional<Integer> oLevelNumber, Optional<String> oMemberName,
            Optional<String> oMemberUniqueName, Optional<MemberTypeEnum> oMemberType, Optional<String> oMemberCaption,
            Optional<CubeSourceEnum> oCubeSource, Optional<TreeOpEnum> oTreeOp,
            Optional<Boolean> emitInvisibleMembers) {

        return getMdSchemaMembersResponseRow(catalog, oSchemaName, oCubeName, oDimensionUniqueName,
                oHierarchyUniqueName, oLevelUniqueName, oLevelNumber, oMemberName, oMemberUniqueName, oMemberType,
                oCubeSource, oTreeOp, emitInvisibleMembers);

    }

    static List<MdSchemaKpisResponseRow> getMdSchemaKpisResponseRow(Catalog catalog, Optional<String> oSchemaName,
            Optional<String> oCubeName, Optional<String> oKpiName) {

        return getMdSchemaKpisResponseRow(catalog, oCubeName, oKpiName);
    }

    static List<MdSchemaSetsResponseRow> getMdSchemaSetsResponseRow(Catalog catalog, Optional<String> oSchemaName,
            Optional<String> oCubeName, Optional<String> oSetName, Optional<ScopeEnum> oScope,
            Optional<CubeSourceEnum> oCubeSource, Optional<String> oHierarchyUniqueName, RequestMetaData metaData) {

        return getMdSchemaSetsResponseRow(catalog, null, oCubeName, oSetName, oScope, oCubeSource,
                oHierarchyUniqueName);

    }

    private static List<MdSchemaSetsResponseRow> getMdSchemaSetsResponseRow(Catalog catalog, String schemaName,
            Optional<String> oCubeName, Optional<String> oSetName, Optional<ScopeEnum> oScope,
            Optional<CubeSourceEnum> oCubeSource, Optional<String> oHierarchyUniqueName) {
        List<Cube> cubes = catalog.getCubes() == null ? List.of() : catalog.getCubes();
        return getCubesWithFilter(cubes, oCubeName).stream()
                .map(cube -> getMdSchemaSetsResponseRowCube(catalog.getName(), schemaName, cube, oSetName, oScope,
                        oCubeSource, oHierarchyUniqueName))
                .flatMap(Collection::stream).toList();
    }

    private static List<MdSchemaSetsResponseRow> getMdSchemaSetsResponseRowCube(String catalogName, String schemaName,
            Cube cube, Optional<String> oSetName, Optional<ScopeEnum> oScope, Optional<CubeSourceEnum> oCubeSource,
            Optional<String> oHierarchyUniqueName) {
        List<NamedSet> sets = cube.getNamedSets() == null ? List.of() : Arrays.asList(cube.getNamedSets());
        return getNamedSetsWithFilter(sets, oSetName).stream().map(s -> getMdSchemaSetsResponseRowNamesSet(catalogName,
                schemaName, cube.getName(), s, oScope, oCubeSource, oHierarchyUniqueName)).toList();

    }

    private static MdSchemaSetsResponseRow getMdSchemaSetsResponseRowNamesSet(String catalogName, String schemaName,
            String cubeName, NamedSet set, Optional<ScopeEnum> oScope, Optional<CubeSourceEnum> oCubeSource,
            Optional<String> oHierarchyUniqueName) {

        String dimensions = set.getHierarchies().stream().map(it -> it.getUniqueName())
                .collect(Collectors.joining(","));

        return new MdSchemaSetsResponseRowR(Optional.ofNullable(catalogName), Optional.ofNullable(schemaName),
                Optional.ofNullable(cubeName), Optional.ofNullable(set.getName()), Optional.of(ScopeEnum.GLOBAL),
                Optional.ofNullable(set.getDescription()),
                Optional.ofNullable(set.getExp() != null ? set.getExp().toString() : null),
                Optional.ofNullable(dimensions), Optional.ofNullable(set.getCaption()),
                Optional.ofNullable(set.getDisplayFolder()), Optional.of(SetEvaluationContextEnum.STATIC));
    }

    private static List<NamedSet> getNamedSetsWithFilter(List<NamedSet> sets, Optional<String> oSetName) {
        if (oSetName.isPresent()) {
            return sets.stream().filter(s -> oSetName.get().equals(s.getName())).toList();
        }
        return sets;
    }

    private static List<MdSchemaKpisResponseRow> getMdSchemaKpisResponseRow(Catalog catalog, Optional<String> oCubeName,
            Optional<String> oKpiName) {
        List<MdSchemaKpisResponseRow> result = new ArrayList<>();
        result.addAll(getCubeWithFilter(catalog.getCubes(), oCubeName).stream()
                .map(c -> getMdSchemaKpisResponseRow(catalog.getName(), null, c, oKpiName)).flatMap(Collection::stream)
                .toList());
        return result;
    }

    private static List<MdSchemaKpisResponseRow> getMdSchemaKpisResponseRow(String catalogName, String schemaName,
            Cube cube, Optional<String> oKpiName) {
        return getMappingKpiWithFilter(cube.getKPIs(), oKpiName).stream()
                .map(kpi -> getMdSchemaKpisResponseRow(catalogName, schemaName, cube.getName(), kpi)).toList();

    }

    private static MdSchemaKpisResponseRow getMdSchemaKpisResponseRow(String catalogName, String schemaName,
            String cubeName, KPI kpi) {
        if (kpi != null) {
            return new MdSchemaKpisResponseRowR(Optional.ofNullable(catalogName), Optional.ofNullable(schemaName),
                    Optional.ofNullable(cubeName), Optional.ofNullable(cubeName), Optional.ofNullable(kpi.getName()),
                    Optional.ofNullable(kpi.getName()), Optional.ofNullable(kpi.getDescription()),
                    Optional.ofNullable(kpi.getDisplayFolder()), Optional.ofNullable(kpi.getValue()),
                    Optional.ofNullable(kpi.getGoal()), Optional.ofNullable(kpi.getStatus()),
                    Optional.ofNullable(kpi.getTrend()), Optional.ofNullable(kpi.getStatusGraphic()),
                    Optional.ofNullable(kpi.getTrendGraphic()), Optional.ofNullable(kpi.getWeight()),
                    Optional.ofNullable(kpi.getCurrentTimeMember()),
                    Optional.ofNullable(kpi.getParentKpi() != null ? kpi.getParentKpi().getName() : null),
                    Optional.empty(), Optional.of(ScopeEnum.GLOBAL));
        }
        return null;
    }

    private static List<? extends KPI> getMappingKpiWithFilter(List<? extends KPI> kpis, Optional<String> oKpiName) {
        if (oKpiName.isPresent()) {
            return kpis.stream().filter(k -> oKpiName.get().equals(k.getName())).toList();
        }
        return kpis;
    }

    private static List<DrillThroughAction> getMappingDrillThroughActionWithFilter(List<DrillThroughAction> actions,
            Optional<String> oActionName) {
        if (oActionName.isPresent()) {
            return actions.stream().filter(a -> oActionName.get().equals(a.getName())).toList();
        }
        return actions;
    }

    private static List<MdSchemaMembersResponseRow> getMdSchemaMembersResponseRow(Catalog catalog,
            Optional<String> oSchemaName, Optional<String> oCubeName, Optional<String> oDimensionUniqueName,
            Optional<String> oHierarchyUniqueName, Optional<String> oLevelUniqueName, Optional<Integer> oLevelNumber,
            Optional<String> oMemberName, Optional<String> oMemberUniqueName, Optional<MemberTypeEnum> oMemberType,
            Optional<CubeSourceEnum> oCubeSource, Optional<TreeOpEnum> oTreeOp,
            Optional<Boolean> emitInvisibleMembers) {
        List<Cube> cubes = catalog.getCubes();
        return getCubesWithFilter(cubes, oCubeName).stream()
                .map(c -> getMdSchemaMembersResponseRow(catalog.getName(), null, c, oDimensionUniqueName,
                        oHierarchyUniqueName, oLevelUniqueName, oLevelNumber, oMemberName, oMemberUniqueName,
                        oMemberType, oCubeSource, oTreeOp, emitInvisibleMembers))
                .flatMap(Collection::stream).toList();
    }

    private static List<MdSchemaMembersResponseRow> getMdSchemaMembersResponseRow(String catalogName, String schemaName,
            Cube cube, Optional<String> oDimensionUniqueName, Optional<String> oHierarchyUniqueName,
            Optional<String> oLevelUniqueName, Optional<Integer> oLevelNumber, Optional<String> oMemberName,
            Optional<String> oMemberUniqueName, Optional<MemberTypeEnum> oMemberType,
            Optional<CubeSourceEnum> oCubeSource, Optional<TreeOpEnum> oTreeOp,
            Optional<Boolean> emitInvisibleMembers) {
        if (!oMemberUniqueName.isPresent()) {
            // TODO
            return List.of();
        } else {
            if (oLevelUniqueName.isPresent()) {
                Optional<Level> l = lookupLevel(cube, oLevelUniqueName.get());
                if (l.isPresent()) {
                    List<Member> members = cube.getLevelMembers(l.get(), true);
                    return getMdSchemaMembersResponseRow(catalogName, schemaName, cube, members, oMemberUniqueName,
                            oMemberType, emitInvisibleMembers);
                }
            }
            List<? extends Dimension> dimensions = cube.getDimensions() == null ? List.of() : cube.getDimensions();
            return getDimensionsWithFilterByUniqueName(dimensions, oDimensionUniqueName).stream()
                    .map(d -> getMdSchemaMembersResponseRow(catalogName, schemaName, cube, d, oHierarchyUniqueName,
                            oLevelNumber, oMemberUniqueName, oMemberType, emitInvisibleMembers))
                    .flatMap(Collection::stream).toList();
        }
    }

    private static List<MdSchemaMembersResponseRow> getMdSchemaMembersResponseRow(String catalogName, String schemaName,
            Cube cube, Dimension dimension, Optional<String> oHierarchyUniqueName, Optional<Integer> oLevelNumber,
            Optional<String> oMemberUniqueName, Optional<MemberTypeEnum> oMemberType,
            Optional<Boolean> emitInvisibleMembers) {
        List<? extends Hierarchy> hierarchies = dimension.getHierarchies() == null ? List.of()
                : dimension.getHierarchies();
        return getHierarchiesWithFilterByUniqueName(hierarchies, oHierarchyUniqueName).stream()
                .map(h -> getMdSchemaMembersResponseRow(catalogName, schemaName, cube, h, oLevelNumber,
                        oMemberUniqueName, oMemberType, emitInvisibleMembers))
                .flatMap(Collection::stream).toList();
    }

    private static List<MdSchemaMembersResponseRow> getMdSchemaMembersResponseRow(String catalogName, String schemaName,
            Cube cube, Hierarchy hierarchy, Optional<Integer> oLevelNumber, Optional<String> oMemberUniqueName,
            Optional<MemberTypeEnum> oMemberType, Optional<Boolean> emitInvisibleMembers) {
        if (oLevelNumber.isPresent()) {
            int levelNumber = oLevelNumber.get();
            if (levelNumber == -1) {
                LOGGER.warn("LevelNumber invalid");
                return List.of();
            }
            List<? extends Level> levels = hierarchy.getLevels();
            if (levelNumber >= levels.size()) {
                LOGGER.warn("LevelNumber (" + levelNumber + ") is greater than number of levels (" + levels.size()
                        + ") for hierarchy \"" + hierarchy.getUniqueName() + "\"");
                return List.of();
            }

            Level level = levels.get(levelNumber);
            List<Member> members = cube.getLevelMembers(level, true);
            return getMdSchemaMembersResponseRow(catalogName, schemaName, cube, members, oMemberUniqueName, oMemberType,
                    emitInvisibleMembers);
        } else {
            // At this point we get ALL of the members associated with
            // the Hierarchy (rather than getting them one at a time).
            // The value returned is not used at this point but they are
            // now cached in the SchemaReader.
            List<? extends Level> levels = hierarchy.getLevels() == null ? List.of() : hierarchy.getLevels();
            return levels.stream()
                    .map(l -> getMdSchemaMembersResponseRow(catalogName, schemaName, cube,
                            cube.getLevelMembers(l, true), oMemberUniqueName, oMemberType, emitInvisibleMembers))
                    .flatMap(Collection::stream).toList();
        }

    }

    private static List<MdSchemaMembersResponseRow> getMdSchemaMembersResponseRow(String catalogName, String schemaName,
            Cube cube, List<Member> members, Optional<String> oMemberUniqueName, Optional<MemberTypeEnum> oMemberType,
            Optional<Boolean> emitInvisibleMembers) {
        return getMembersWithFilterByType(getMembersWithFilterByUniqueName(members, oMemberUniqueName), oMemberType)
                .stream()
                .map(m -> getMdSchemaMembersResponseRow(catalogName, schemaName, cube, m, emitInvisibleMembers))
                .flatMap(Collection::stream).toList();
    }

    private static List<Member> getMembersWithFilterByType(List<Member> members, Optional<MemberTypeEnum> oMemberType) {
        if (oMemberType.isPresent()) {
            return members.stream().filter(m -> oMemberType.get().equals(getMemberTypeEnum(m.getMemberType())))
                    .toList();
        }
        return members;
    }

    private static MemberTypeEnum getMemberTypeEnum(Member.MemberType memberType) {
        if (memberType != null) {
            switch (memberType) {
            case REGULAR:
                return MemberTypeEnum.REGULAR_MEMBER;
            case ALL:
                return MemberTypeEnum.ALL_MEMBER;
            case MEASURE:
                return MemberTypeEnum.MEASURE;
            case FORMULA:
                return MemberTypeEnum.FORMULA;
            case UNKNOWN:
                return MemberTypeEnum.UNKNOWN;
            default:
                return MemberTypeEnum.REGULAR_MEMBER;
            }
        }
        return MemberTypeEnum.REGULAR_MEMBER;
    }

    private static List<Member> getMembersWithFilterByUniqueName(List<Member> members,
            Optional<String> oMemberUniqueName) {
        if (oMemberUniqueName.isPresent()) {
            return members.stream().filter(m -> oMemberUniqueName.get().equals(m.getUniqueName())).toList();
        }
        return members;
    }

    private static List<MdSchemaMembersResponseRow> getMdSchemaMembersResponseRow(String catalogName, String schemaName,
            Cube cube, Member member, Optional<Boolean> emitInvisibleMembers) {

        // Check whether the member is visible, otherwise do not dump.
        Boolean visible = true;
        if (member.getPropertyValue("$visible") != null) {
            visible = (Boolean) member.getPropertyValue("$visible");
        }
        if (!visible && emitInvisibleMembers.isPresent() && !emitInvisibleMembers.get()) {
            return List.of();
        }

        final Level level = member.getLevel();
        final Hierarchy hierarchy = level.getHierarchy();
        final Dimension dimension = hierarchy.getDimension();

        int adjustedLevelDepth = level.getDepth();

        String parentUniqueName = null;
        if (adjustedLevelDepth != 0) {
            final Member parentMember = member.getParentMember();
            if (parentMember != null) {
                parentUniqueName = parentMember.getUniqueName();
            }
        }

        // TODO Depth absent in result
        // member.getDepth();

        MdSchemaMembersResponseRow r = new MdSchemaMembersResponseRowR(Optional.ofNullable(catalogName),
                Optional.ofNullable(schemaName), Optional.ofNullable(cube.getName()),
                Optional.ofNullable(dimension.getUniqueName()), Optional.ofNullable(hierarchy.getUniqueName()),
                Optional.ofNullable(level.getUniqueName()), Optional.ofNullable(adjustedLevelDepth),
                Optional.ofNullable(0), Optional.ofNullable(member.getName()),
                Optional.ofNullable(member.getUniqueName()),
                Optional.ofNullable(getMemberTypeEnum(member.getMemberType())), Optional.empty(),
                Optional.ofNullable(member.getCaption()), Optional.ofNullable(100),
                Optional.ofNullable(adjustedLevelDepth == 0 ? 0 : adjustedLevelDepth - 1),
                Optional.ofNullable(parentUniqueName), Optional.ofNullable(member.getParentMember() == null ? 0 : 1),
                Optional.ofNullable(member.getDescription()), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
        return List.of(r);
    }

    private static List<? extends Hierarchy> getHierarchiesWithFilterByUniqueName(List<? extends Hierarchy> hierarchies,
            Optional<String> oHierarchyUniqueName) {
        if (oHierarchyUniqueName.isPresent()) {
            return hierarchies.stream().filter(h -> oHierarchyUniqueName.get().equals(h.getUniqueName())).toList();
        }
        return hierarchies;
    }

    private static Optional<Level> lookupLevel(Cube cube, String levelUniqueName) {
        for (Dimension dimension : cube.getDimensions()) {
            for (Hierarchy hierarchy : dimension.getHierarchies()) {
                for (Level level : hierarchy.getLevels()) {
                    if (level.getUniqueName().equals(levelUniqueName)) {
                        return Optional.of(level);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static List<MdSchemaMeasureGroupDimensionsResponseRow> getMdSchemaMeasureGroupDimensionsResponseRow(
            Catalog catalog, Optional<String> oCubeName, Optional<String> oMeasureGroupName,
            Optional<String> oDimensionUniqueName, Optional<VisibilityEnum> oDimensionVisibility) {
        List<Cube> cubes = catalog.getCubes() == null ? List.of() : catalog.getCubes();
        return getCubesWithFilter(cubes, oCubeName).stream()
                .map(c -> getMdSchemaMeasureGroupDimensionsResponseRow(catalog.getName(), null, c, oDimensionUniqueName,
                        oDimensionVisibility))
                .flatMap(Collection::stream).toList();
    }

    private static List<MdSchemaMeasureGroupDimensionsResponseRow> getMdSchemaMeasureGroupDimensionsResponseRow(
            String catalogName, String schemaName, Cube cube, Optional<String> oDimensionUniqueName,
            Optional<VisibilityEnum> oDimensionVisibility) {
        List<? extends Dimension> dimensions = cube.getDimensions() == null ? List.of() : cube.getDimensions();
        return getDimensionsWithFilterByUniqueName(dimensions, oDimensionUniqueName).stream()
                .map(d -> getMdSchemaMeasureGroupDimensionsResponseRow(catalogName, schemaName, cube, d,
                        oDimensionVisibility))
                .toList();
    }

    private static MdSchemaMeasureGroupDimensionsResponseRow getMdSchemaMeasureGroupDimensionsResponseRow(
            String catalogName, String schemaName, Cube cube, Dimension dimension,
            Optional<VisibilityEnum> oDimensionVisibility) {

        return new MdSchemaMeasureGroupDimensionsResponseRowR(Optional.ofNullable(catalogName),
                Optional.ofNullable(schemaName), Optional.ofNullable(cube.getName()),
                Optional.ofNullable(cube.getName()), Optional.of("ONE"), Optional.ofNullable(dimension.getUniqueName()),
                Optional.of(DimensionCardinalityEnum.MANY), Optional.ofNullable(dimension.isVisible()),
                Optional.of(false), Optional.of(List.of()), // ?? this parameter absent in old implementation
                Optional.of(""));
    }

    private static List<MdSchemaDimensionsResponseRow> getMdSchemaDimensionsResponseRow(String catalogName,
            Catalog schema, Optional<String> oCubeName, Optional<String> oDimensionName,
            Optional<String> oDimensionUniqueName, Optional<CubeSourceEnum> cubeSource,
            Optional<VisibilityEnum> oDimensionVisibility, Optional<Boolean> deep) {
        List<Cube> cubes = schema.getCubes() == null ? List.of() : schema.getCubes();
        return getCubesWithFilter(cubes, oCubeName).stream()
                .map(c -> getMdSchemaDimensionsResponseRow(catalogName, schema.getName(), c, oDimensionName,
                        oDimensionUniqueName, cubeSource, oDimensionVisibility, deep))
                .flatMap(Collection::stream).sorted(Comparator.comparing(r -> r.dimensionUniqueName().orElse("")))
                .toList();
    }

    private static List<MdSchemaDimensionsResponseRow> getMdSchemaDimensionsResponseRow(String catalogName,
            String schemaName, Cube cube, Optional<String> oDimensionName, Optional<String> oDimensionUniqueName,
            Optional<CubeSourceEnum> cubeSource, Optional<VisibilityEnum> oDimensionVisibility,
            Optional<Boolean> deep) {
        List<? extends Dimension> dimensions = cube.getDimensions() == null ? List.of() : cube.getDimensions();
        return getDimensionsWithFilterByName(getDimensionsWithFilterByUniqueName(dimensions, oDimensionUniqueName),
                oDimensionName).stream()
                .map(d -> getMdSchemaDimensionsResponseRow(catalogName, schemaName, cube, d, cubeSource,
                        oDimensionVisibility, deep))
                .toList();
    }

    private static MdSchemaDimensionsResponseRow getMdSchemaDimensionsResponseRow(String catalogName, String schemaName,
            Cube cube, Dimension dimension, Optional<CubeSourceEnum> cubeSource,
            Optional<VisibilityEnum> oDimensionVisibility, Optional<Boolean> deep) {
        String cubeName = cube.getName();
        String desc = dimension.getDescription();
        if (desc == null) {
            desc = cube.getName() + " Cube - " + dimension.getName() + " Dimension";
        }
        List<? extends Dimension> dimensions = cube.getDimensions() != null ? cube.getDimensions() : List.of();
        // Is this the number of primaryKey members there are??
        // According to microsoft this is:
        // "The number of members in the key attribute."
        // There may be a better way of doing this but
        // this is what I came up with. Note that I need to
        // add '1' to the number inorder for it to match
        // match what microsoft SQL Server is producing.
        // The '1' might have to do with whether or not the
        // hierarchy has a 'all' member or not - don't know yet.
        // large data set total for Orders cube 0m42.923s

        String firstHierarchyUniqueName = null;
        Level lastLevel = null;
        if (dimension.getHierarchies() != null && dimension.getHierarchies().size() > 0) {
            Hierarchy firstHierarchy = dimension.getHierarchies().getFirst();
            firstHierarchyUniqueName = firstHierarchy.getUniqueName();
            if (firstHierarchy.getLevels() != null && firstHierarchy.getLevels().size() > 0) {
                lastLevel = firstHierarchy.getLevels().getLast();
            }
        }
        /*
         * if override config setting is set if approxRowCount has a value use it else do default
         */

        // Added by TWI to returned cached row numbers

        // int n = getExtra(connection).getLevelCardinality(lastLevel);
        int n = lastLevel == null ? 0 : cube.getLevelCardinality(lastLevel, true, true);
        int dimensionCardinality = n + 1;
        boolean isVirtual = false;
        // SQL Server always returns false
        boolean isReadWrite = false;

        // TODO: don't know what to do here
        // Are these the levels with uniqueMembers == true?
        // How are they mapped to specific column numbers?
        // but was 0
        DimensionUniqueSettingEnum dimensionUniqueSetting = DimensionUniqueSettingEnum.NONE;

        if (deep.isPresent() && deep.get()) {
            // TODO add MdSchemaHierarchiesResponse to response
            getMdSchemaHierarchiesResponseRow(cube, catalogName, schemaName, cubeName, dimension, 0, Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), deep);
        }

        return new MdSchemaDimensionsResponseRowR(Optional.ofNullable(catalogName), Optional.ofNullable(schemaName),
                Optional.ofNullable(cubeName), Optional.ofNullable(dimension.getName()),
                Optional.ofNullable(dimension.getUniqueName()), Optional.empty(),
                Optional.ofNullable(dimension.getCaption()), Optional.ofNullable(dimensions.indexOf(dimension)),
                Optional.ofNullable(getDimensionType(dimension.getDimensionType())), Optional.of(dimensionCardinality),
                Optional.ofNullable(firstHierarchyUniqueName), Optional.of(desc), Optional.of(isVirtual),
                Optional.of(isReadWrite), Optional.ofNullable(dimensionUniqueSetting), Optional.empty(),
                Optional.ofNullable(dimension.isVisible()));

    }

    private static DimensionTypeEnum getDimensionType(DimensionType dimensionType) {
        if (dimensionType != null) {
            switch (dimensionType) {
            case STANDARD_DIMENSION:
                return DimensionTypeEnum.OTHER;
            case MEASURES_DIMENSION:
                return DimensionTypeEnum.MEASURE;
            case TIME_DIMENSION:
                return DimensionTypeEnum.TIME;
            default:
                throw new RuntimeException("Wrong dimension type");
            }
        }
        return null;
    }

    private static List<MdSchemaLevelsResponseRow> getMdSchemaLevelsResponseRow(String catalogName, Catalog schema,
            Optional<String> oCubeName, Optional<String> oDimensionUniqueName, Optional<String> oHierarchyUniqueName,
            Optional<String> oLevelName, Optional<String> oLevelUniqueName, Optional<VisibilityEnum> oLevelVisibility) {
        List<Cube> cubes = schema.getCubes() == null ? List.of() : schema.getCubes();
        return getCubesWithFilter(cubes, oCubeName).stream().sorted(Comparator.comparing(Cube::getName))
                .map(cube -> getMdSchemaLevelsResponseRow(catalogName, schema.getName(), cube, oDimensionUniqueName,
                        oHierarchyUniqueName, oLevelName, oLevelUniqueName, oLevelVisibility))
                .flatMap(Collection::stream).toList();
    }

    private static List<Cube> getCubesWithFilter(List<Cube> cubes, Optional<String> oCubeName) {
        if (oCubeName.isPresent()) {
            return getCubesWithFilter(cubes, oCubeName.get());
        }
        return cubes;
    }

    private static List<Cube> getCubesWithFilter(List<Cube> cubes, String cubeName) {
        if (cubeName != null) {
            return cubes.stream().filter(c -> cubeName.equals(c.getName())).toList();
        }
        return cubes;
    }

    private static List<MdSchemaLevelsResponseRow> getMdSchemaLevelsResponseRow(String catalogName, String schemaName,
            Cube cube, Optional<String> oDimensionUniqueName, Optional<String> oHierarchyUniqueName,
            Optional<String> oLevelName, Optional<String> oLevelUniqueName, Optional<VisibilityEnum> oLevelVisibility) {
        List<? extends Dimension> dimensions = cube.getDimensions() == null ? List.of() : cube.getDimensions();
        return getDimensionsWithFilterByUniqueName(dimensions, oDimensionUniqueName).stream()
                .map(d -> getMdSchemaLevelsResponseRow(cube, catalogName, schemaName, cube.getName(), d,
                        oHierarchyUniqueName, oLevelName, oLevelUniqueName, oLevelVisibility))
                .flatMap(Collection::stream).toList();
    }

    private static List<? extends Dimension> getDimensionsWithFilterByUniqueName(List<? extends Dimension> dimensions,
            Optional<String> oDimensionUniqueName) {
        if (oDimensionUniqueName.isPresent()) {
            return dimensions.stream().filter(d -> oDimensionUniqueName.get().equals(d.getUniqueName())).toList();
        }
        return dimensions;
    }

    private static List<? extends Dimension> getDimensionsWithFilterByName(List<? extends Dimension> dimensions,
            Optional<String> oDimensionName) {
        if (oDimensionName.isPresent()) {
            return dimensions.stream().filter(d -> oDimensionName.get().equals(d.getName())).toList();
        }
        return dimensions;
    }

    private static List<MdSchemaLevelsResponseRow> getMdSchemaLevelsResponseRow(Cube cube, String catalogName,
            String schemaName, String cubeName, Dimension dimension, Optional<String> oHierarchyUniqueName,
            Optional<String> oLevelName, Optional<String> oLevelUniqueName, Optional<VisibilityEnum> oLevelVisibility) {
        List<? extends Hierarchy> hierarchies = dimension.getHierarchies() == null ? List.of()
                : dimension.getHierarchies();
        return getHierarchiesWithFilterByUniqueName(hierarchies, oHierarchyUniqueName).stream()
                .map(h -> getMdSchemaLevelsResponseRow(cube, catalogName, schemaName, cubeName,
                        dimension.getUniqueName(), h, oLevelName, oLevelUniqueName, oLevelVisibility))
                .flatMap(Collection::stream).toList();
    }

    private static List<? extends Hierarchy> getHierarchiesWithFilterByName(List<? extends Hierarchy> hierarchies,
            Optional<String> oHierarchyName) {
        if (oHierarchyName.isPresent()) {
            return hierarchies.stream().filter(h -> oHierarchyName.get().equals(h.getName())).toList();
        }
        return hierarchies;
    }

    private static List<MdSchemaLevelsResponseRow> getMdSchemaLevelsResponseRow(Cube cube, String catalogName,
            String schemaName, String cubeName, String dimensionUniqueName, Hierarchy h, Optional<String> oLevelName,
            Optional<String> oLevelUniqueName, Optional<VisibilityEnum> oLevelVisibility) {
        List<? extends Level> levels = h.getLevels() == null ? List.of() : h.getLevels();
        List<MdSchemaLevelsResponseRow> res = new ArrayList();
        List<? extends Level> ls = getLevelsWithFilterByUniqueName(getLevelsWithFilterByName(levels, oLevelName),
                oLevelUniqueName);
        for (Level level : ls) {
            String desc = level.getDescription();
            if (desc == null) {
                desc = cubeName + " Cube - " + getHierarchyName(h.getName(), dimensionUniqueName) + " Hierarchy - "
                        + level.getName() + " Level";
            }

            int uniqueSettings = 0;
            if (level.isAll()) {
                uniqueSettings |= 2;
            }
            if (level.isUnique()) {
                uniqueSettings |= 1;
            }
            // Get level cardinality
            // According to microsoft this is:
            // "The number of members in the level."
            // int levelCardinality = extra.getLevelCardinality(level); //TODO
            int levelCardinality = cube.getLevelCardinality(level, true, true);

            res.add((MdSchemaLevelsResponseRow) new MdSchemaLevelsResponseRowR(Optional.ofNullable(catalogName),
                    Optional.ofNullable(schemaName), Optional.ofNullable(cubeName),
                    Optional.ofNullable(dimensionUniqueName), Optional.ofNullable(h.getUniqueName()),
                    Optional.ofNullable(level.getName()), Optional.ofNullable(level.getUniqueName()), Optional.empty(),
                    Optional.ofNullable(level.getCaption()), Optional.ofNullable(level.getDepth()),
                    Optional.of(levelCardinality), Optional.of(getLevelType(level)), Optional.ofNullable(desc),
                    Optional.of(CustomRollupSettingEnum.NONE),
                    Optional.of(LevelUniqueSettingsEnum.fromValue(String.valueOf(uniqueSettings))),
                    Optional.ofNullable(level.isVisible()), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.of(LevelOriginEnum.NONE)));

            /*
             * if (level.isParentChild()) { String name = level.getName() + "_child"; String uniqueName =
             * Util.makeFqName(h, name); res.add((MdSchemaLevelsResponseRow) new MdSchemaLevelsResponseRowR(
             * Optional.ofNullable(catalogName), Optional.ofNullable(schemaName), Optional.ofNullable(cubeName),
             * Optional.ofNullable(dimensionUniqueName), Optional.ofNullable(h.getUniqueName()),
             * Optional.ofNullable(name), Optional.ofNullable(uniqueName), Optional.empty(),
             * Optional.ofNullable(name), Optional.ofNullable(level.getDepth()), Optional.of(levelCardinality),
             * Optional.of(getLevelType(level)), Optional.ofNullable(desc),
             * Optional.of(CustomRollupSettingEnum.NONE),
             * Optional.of(LevelUniqueSettingsEnum.fromValue(String.valueOf(uniqueSettings))),
             * Optional.ofNullable(level.isVisible()), Optional.empty(), Optional.empty(), Optional.empty(),
             * Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
             * Optional.of(LevelOriginEnum.NONE)));
             * 
             * }
             */
        }
        /*
         * res.add(new MdSchemaLevelsResponseRowR( Optional.ofNullable(catalogName),
         * Optional.ofNullable(schemaName), Optional.ofNullable(cubeName),
         * Optional.ofNullable(dimensionUniqueName), Optional.ofNullable(h.getUniqueName()),
         * Optional.ofNullable("1"), Optional.ofNullable("[Dimension].[Dimension].[1]"), Optional.empty(),
         * Optional.ofNullable("1"), Optional.ofNullable(0), Optional.of(17),
         * Optional.of(LevelTypeEnum.REGULAR), Optional.empty(), Optional.of(CustomRollupSettingEnum.NONE),
         * Optional.of(LevelUniqueSettingsEnum.KEY_COLUMNS), Optional.ofNullable(true), Optional.empty(),
         * Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
         * Optional.empty(), Optional.empty(), Optional.of(LevelOriginEnum.NONE)));
         * 
         * res.add(new MdSchemaLevelsResponseRowR( Optional.ofNullable(catalogName),
         * Optional.ofNullable(schemaName), Optional.ofNullable(cubeName),
         * Optional.ofNullable(dimensionUniqueName), Optional.ofNullable(h.getUniqueName()),
         * Optional.ofNullable("2"), Optional.ofNullable("[Dimension].[Dimension].[2]"), Optional.empty(),
         * Optional.ofNullable("2"), Optional.ofNullable(0), Optional.of(17),
         * Optional.of(LevelTypeEnum.REGULAR), Optional.empty(), Optional.of(CustomRollupSettingEnum.NONE),
         * Optional.of(LevelUniqueSettingsEnum.KEY_COLUMNS), Optional.ofNullable(true), Optional.empty(),
         * Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
         * Optional.empty(), Optional.empty(), Optional.of(LevelOriginEnum.NONE))); res.add(new
         * MdSchemaLevelsResponseRowR( Optional.ofNullable(catalogName), Optional.ofNullable(schemaName),
         * Optional.ofNullable(cubeName), Optional.ofNullable(dimensionUniqueName),
         * Optional.ofNullable(h.getUniqueName()), Optional.ofNullable("3"),
         * Optional.ofNullable("[Dimension].[Dimension].[3]"), Optional.empty(), Optional.ofNullable("3"),
         * Optional.ofNullable(0), Optional.of(17), Optional.of(LevelTypeEnum.REGULAR), Optional.empty(),
         * Optional.of(CustomRollupSettingEnum.NONE), Optional.of(LevelUniqueSettingsEnum.KEY_COLUMNS),
         * Optional.ofNullable(true), Optional.empty(), Optional.empty(), Optional.empty(),
         * Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
         * Optional.of(LevelOriginEnum.NONE)));
         */
        return res;
    }

    private static LevelTypeEnum getLevelType(Level level) {
        if (level.isAll()) {
            return LevelTypeEnum.ALL;
        }
        return switch (level.getLevelType()) {
        case REGULAR -> LevelTypeEnum.REGULAR;
        case TIME_YEARS -> LevelTypeEnum.TIME_YEARS;
        case TIME_HALF_YEARS -> LevelTypeEnum.TIME_HALF_YEARS;
        case TIME_QUARTERS -> LevelTypeEnum.TIME_QUARTERS;
        case TIME_MONTHS -> LevelTypeEnum.TIME; // TODO
        case TIME_WEEKS -> LevelTypeEnum.TIME_WEEKS;
        case TIME_DAYS -> LevelTypeEnum.TIME_DAYS;
        case TIME_HOURS -> LevelTypeEnum.TIME; // TODO
        case TIME_MINUTES -> LevelTypeEnum.TIME; // TODO
        case TIME_SECONDS -> LevelTypeEnum.TIME_SECONDS;
        case TIME_UNDEFINED -> LevelTypeEnum.TIME_UNDEFINED;
        case GEO_CONTINENT -> LevelTypeEnum.GEOGRAPHY_CONTINENT;
        case GEO_REGION -> LevelTypeEnum.GEOGRAPHY_REGION;
        case GEO_COUNTRY -> LevelTypeEnum.GEOGRAPHY_COUNTRY;
        case GEO_STATE_OR_PROVINCE -> LevelTypeEnum.GEOGRAPHY_STATE_OR_PROVINCE;
        case GEO_COUNTY -> LevelTypeEnum.GEOGRAPHY_COUNTY;
        case GEO_CITY -> LevelTypeEnum.GEOGRAPHY_CITY;
        case GEO_POSTALCODE -> LevelTypeEnum.POSTAL_CODE;
        case GEO_POINT -> LevelTypeEnum.GEOGRAPHY_POINT;
        case ORG_UNIT -> LevelTypeEnum.ORGANIZATION_UNIT;
        case BOM_RESOURCE -> LevelTypeEnum.BILL_OF_MATERIAL_RESOURCE;
        case QUANTITATIVE -> LevelTypeEnum.QUANTITATIVE;
        case ACCOUNT -> LevelTypeEnum.ACCOUNT;
        case CUSTOMER -> LevelTypeEnum.CUSTOMER;
        case CUSTOMER_GROUP -> LevelTypeEnum.CUSTOMER_GROUP;
        case CUSTOMER_HOUSEHOLD -> LevelTypeEnum.CUSTOMER_HOUSEHOLD;
        case PRODUCT -> LevelTypeEnum.PRODUCT;
        case PRODUCT_GROUP -> LevelTypeEnum.PRODUCT_GROUP;
        case SCENARIO -> LevelTypeEnum.SCENARIO;
        case UTILITY -> LevelTypeEnum.UTILITY;
        case PERSON -> LevelTypeEnum.PERSON;
        case COMPANY -> LevelTypeEnum.COMPANY;
        case CURRENCY_SOURCE -> LevelTypeEnum.CURRENCY_SOURCE;
        case CURRENCY_DESTINATION -> LevelTypeEnum.CURRENCY_DESTINATION;
        case CHANNEL -> LevelTypeEnum.CHANNEL;
        case REPRESENTATIVE -> LevelTypeEnum.REPRESENTATIVE;
        case PROMOTION -> LevelTypeEnum.PROMOTION;
        case NULL -> LevelTypeEnum.REGULAR;// Does not exist in Xmla
        };
    }

    private static List<? extends Level> getLevelsWithFilterByName(List<? extends Level> levels,
            Optional<String> oLevelName) {
        if (oLevelName.isPresent()) {
            return levels.stream().filter(l -> oLevelName.get().equals(l.getName())).toList();
        }
        return levels;
    }

    private static List<? extends Level> getLevelsWithFilterByUniqueName(List<? extends Level> levels,
            Optional<String> oLevelUniqueNameName) {
        if (oLevelUniqueNameName.isPresent()) {
            return levels.stream().filter(l -> oLevelUniqueNameName.get().equals(l.getUniqueName())).toList();
        }
        return levels;
    }

    static List<DbSchemaSourceTablesResponseRow> getDbSchemaSourceTablesResponseRow(Catalog catalog,
            List<String> tableTypeList) {
        List<DbSchemaSourceTablesResponseRow> result = new ArrayList<>();
        String[] tableTypeRestriction = null;
        if (tableTypeList != null) {
            tableTypeRestriction = tableTypeList.toArray(new String[0]);
        }

//TODO get database from catalog direct
//            while () {
//
//                final String tableCatalog = "";
//                final String tableSchema = "";
//                final String tableName = "";
//                final String tableType = "";
//
//                result.add(new DbSchemaSourceTablesResponseRowR(
//                    Optional.of(tableCatalog),
//                    Optional.of(tableSchema),
//                    tableName,
//                    TableTypeEnum.fromValue(tableType)
//                ));
//            }

        return result;
    }

    static List<MdSchemaHierarchiesResponseRow> getMdSchemaHierarchiesResponseRow(Catalog catalog,
            Optional<String> oSchemaName, Optional<String> oCubeName, Optional<CubeSourceEnum> oCubeSource,
            Optional<String> oDimensionUniqueName, Optional<String> oHierarchyName,
            Optional<String> oHierarchyUniqueName, Optional<VisibilityEnum> oHierarchyVisibility,
            Optional<Integer> oHierarchyOrigin, Optional<Boolean> deep, RequestMetaData requestMetaData) {
        return getMdSchemaHierarchiesResponseRow(catalog.getName(), catalog, oCubeName, oCubeSource,
                oDimensionUniqueName, oHierarchyName, oHierarchyUniqueName, oHierarchyVisibility, oHierarchyOrigin,
                deep);
    }

    private static List<MdSchemaHierarchiesResponseRow> getMdSchemaHierarchiesResponseRow(String catalogName, Catalog s,
            Optional<String> oCubeName, Optional<CubeSourceEnum> oCubeSource, Optional<String> oDimensionUniqueName,
            Optional<String> oHierarchyName, Optional<String> oHierarchyUniqueName,
            Optional<VisibilityEnum> oHierarchyVisibility, Optional<Integer> oHierarchyOrigin, Optional<Boolean> deep) {
        List<Cube> cubes = s.getCubes() == null ? List.of() : s.getCubes();
        return getCubesWithFilter(cubes, oCubeName).stream()
                .map(c -> getMdSchemaHierarchiesResponseRow(c, catalogName, s.getName(), c, oDimensionUniqueName,
                        oHierarchyName, oHierarchyUniqueName, oHierarchyVisibility, oHierarchyOrigin, deep))
                .flatMap(Collection::stream).sorted(Comparator.comparing(r -> r.hierarchyUniqueName().orElse("")))
                .toList();
    }

    private static List<MdSchemaHierarchiesResponseRow> getMdSchemaHierarchiesResponseRow(Cube cube, String catalogName,
            String schemaName, Cube c, Optional<String> oDimensionUniqueName, Optional<String> oHierarchyName,
            Optional<String> oHierarchyUniqueName, Optional<VisibilityEnum> oHierarchyVisibility,
            Optional<Integer> oHierarchyOrigin, Optional<Boolean> deep) {
        List<MdSchemaHierarchiesResponseRow> result = new ArrayList<>();
        List<? extends Dimension> dimensions = c.getDimensions() == null ? List.of() : c.getDimensions();
        int ordinal = 0;
        for (Dimension dimension : dimensions) {
            if (!oDimensionUniqueName.isPresent() || oDimensionUniqueName.get().equals(dimension.getUniqueName())) {
                result.addAll(getMdSchemaHierarchiesResponseRow(cube, catalogName, schemaName, c.getName(), dimension,
                        ordinal, oHierarchyName, oHierarchyUniqueName, oHierarchyVisibility, oHierarchyOrigin, deep));
            }
            ordinal += dimension.getHierarchies().size();
        }
        return result;
    }

    private static List<MdSchemaHierarchiesResponseRow> getMdSchemaHierarchiesResponseRow(Cube cube, String catalogName,
            String schemaName, String cubeName, Dimension dimension, int ordinal, Optional<String> oHierarchyName,
            Optional<String> oHierarchyUniqueName, Optional<VisibilityEnum> oHierarchyVisibility,
            Optional<Integer> oHierarchyOrigin, Optional<Boolean> deep) {
        List<? extends Hierarchy> hierarchies = dimension.getHierarchies() == null ? List.of()
                : dimension.getHierarchies();

        return getHierarchiesWithFilterByName(getHierarchiesWithFilterByUniqueName(hierarchies, oHierarchyName),
                oHierarchyName).stream()
                .map(h -> getMdSchemaHierarchiesResponseRow(cube, catalogName, schemaName, cubeName, dimension,
                        ordinal + hierarchies.indexOf(h), h, oHierarchyVisibility, oHierarchyOrigin, deep))
                .toList();

    }

    private static MdSchemaHierarchiesResponseRow getMdSchemaHierarchiesResponseRow(Cube cube, String catalogName,
            String schemaName, String cubeName, Dimension dimension, int ordinal, Hierarchy hierarchy,
            Optional<VisibilityEnum> oHierarchyVisibility, Optional<Integer> oHierarchyOrigin, Optional<Boolean> deep) {
        String desc = hierarchy.getDescription();
        if (desc == null) {
            desc = cubeName + " Cube - " + getHierarchyName(hierarchy.getName(), dimension.getName()) + " Hierarchy";
        }

        // mondrian.olap4j.MondrianOlap4jHierarchy mondrianOlap4jHierarchy =
        // (mondrian.olap4j.MondrianOlap4jHierarchy)hierarchy;

        // Bitmask
        // MD_ORIGIN_USER_DEFINED 0x00000001
        // MD_ORIGIN_ATTRIBUTE 0x00000002
        // MD_ORIGIN_KEY_ATTRIBUTE 0x00000004
        // MD_ORIGIN_INTERNAL 0x00000008
        int hierarchyOrigin;
        if (dimension.getUniqueName().equals(org.eclipse.daanse.olap.api.element.Dimension.MEASURES_UNIQUE_NAME)) {
            hierarchyOrigin = 6;
        } else {
            hierarchyOrigin = hierarchy.origin() != null ? Integer.parseInt(hierarchy.origin()) : 1;
        }

        // String displayFolder = mondrianOlap4jHierarchy.getDisplayFolder();
        String displayFolder = hierarchy.getDisplayFolder();
        if (displayFolder == null) {
            displayFolder = "";
        }

        // row.set(ParentChild.name, extra.isHierarchyParentChild(hierarchy));
        // TODO ParentChild
        if (deep.isPresent() && deep.get()) {
            // TODO add MdSchemaLevelsResponse to response
            getMdSchemaLevelsResponseRow(cube, catalogName, schemaName, cubeName, dimension.getUniqueName(), hierarchy,
                    Optional.empty(), Optional.empty(), Optional.empty());
        }

        final List<Member> levelMembers = cube.getLevelMembers(hierarchy.getLevels().getFirst(), true);
        return new MdSchemaHierarchiesResponseRowR(Optional.ofNullable(catalogName), Optional.ofNullable(schemaName),
                Optional.ofNullable(cubeName), Optional.ofNullable(dimension.getUniqueName()),
                Optional.ofNullable(hierarchy.getName()), Optional.ofNullable(hierarchy.getUniqueName()),
                Optional.empty(), Optional.ofNullable(hierarchy.getCaption()),
                Optional.ofNullable(getDimensionType(dimension.getDimensionType())),
                Optional.of(getHierarchyCardinality(cube, hierarchy)),
                Optional.ofNullable(
                        hierarchy.getDefaultMember() == null ? null : hierarchy.getDefaultMember().getUniqueName()),
                Optional.ofNullable((hierarchy.hasAll() && levelMembers != null && levelMembers.size() > 0)
                        ? levelMembers.get(0).getUniqueName()
                        : null),
                Optional.ofNullable(desc), Optional.of(StructureEnum.HIERARCHY_FULLY_BALANCED), Optional.of(false),
                Optional.of(false),
                // NOTE that SQL Server returns '0' not '1'.
                Optional.of(DimensionUniqueSettingEnum.NONE), Optional.empty(),
                Optional.ofNullable(dimension.isVisible()), Optional.of(ordinal), Optional.of(true),
                Optional.ofNullable(hierarchy.isVisible()),
                Optional.of(HierarchyOriginEnum.fromValue(String.valueOf(hierarchyOrigin))),
                Optional.ofNullable(displayFolder), Optional.empty(), Optional.empty(), Optional.empty());

    }

    private static int getHierarchyCardinality(Cube cube, Hierarchy hierarchy) {
        int cardinality = 0;
        if (hierarchy.getLevels() != null) {
            for (Level level : hierarchy.getLevels()) {
                cardinality += cube.getLevelCardinality(level, true, true);
            }
        }
        return cardinality;
    }

    static List<MdSchemaMeasureGroupsResponseRow> getMdSchemaMeasureGroupsResponseRow(Catalog catalog,
            Optional<String> oSchemaName, Optional<String> oCubeName, Optional<String> oMeasureGroupName) {
        return getMdSchemaMeasureGroupsResponseRow(catalog, oCubeName, oMeasureGroupName);

    }

    private static List<MdSchemaMeasureGroupsResponseRow> getMdSchemaMeasureGroupsResponseRow(Catalog catalog,
            Optional<String> oCubeName, Optional<String> oMeasureGroupName) {
        return getCubeWithFilter(catalog.getCubes(), oCubeName).stream()
                .map(c -> getMdSchemaMeasureGroupsResponseRow(catalog.getName(), null, c, oMeasureGroupName)).toList();
    }

    private static MdSchemaMeasureGroupsResponseRow getMdSchemaMeasureGroupsResponseRow(String catalogName,
            String schemaName, Cube cube, Optional<String> oMeasureGroupName) {
        return new MdSchemaMeasureGroupsResponseRowR(Optional.ofNullable(catalogName), Optional.ofNullable(schemaName),
                Optional.of(cube.getName()), Optional.of(cube.getName()), Optional.of(""), Optional.of(false),
                Optional.of(cube.getName()));
    }

    static List<MdSchemaPropertiesResponseRow> getMdSchemaPropertiesResponseRowCell() {
        List<MdSchemaPropertiesResponseRow> result = new ArrayList<>();
        for (Property.StandardCellProperty property : Property.StandardCellProperty.values()) {
            int type = toMask(property.getType());
            int dataType = property.getDatatype().xmlaOrdinal();

            result.add(new MdSchemaPropertiesResponseRowR(Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.ofNullable(PropertyTypeEnum.fromValue(String.valueOf(type))),
                    Optional.ofNullable(property.name()), Optional.ofNullable(property.getCaption()),
                    Optional.ofNullable(LevelDbTypeEnum.fromValue(String.valueOf(dataType))), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.of(CubeSourceEnum.DIMENSION), Optional.of(VisibilityEnum.VISIBLE)));
        }
        return result;
    }

    static int toMask(Set<TypeFlag> set) {
        int mask = 0;
        for (TypeFlag e : set) {
            mask |= e.xmlaOrdinal();
        }
        return mask;
    }

    static List<MdSchemaPropertiesResponseRow> getMdSchemaPropertiesResponseRow(Catalog catalog,
            Optional<String> oSchemaName, Optional<String> oCubeName, Optional<String> oDimensionUniqueName,
            Optional<String> oHierarchyUniqueName, Optional<String> oLevelUniqueName,
            Optional<String> oMemberUniqueName, Optional<String> oPropertyName,
            Optional<PropertyOriginEnum> oPropertyOrigin, Optional<CubeSourceEnum> oCubeSource,
            Optional<VisibilityEnum> oPropertyVisibility) {
        List<Cube> cubes = catalog.getCubes() == null ? List.of() : catalog.getCubes();
        return getCubesWithFilter(cubes, oCubeName).stream()
                .map(c -> getMdSchemaPropertiesResponseRow(catalog.getName(), null, c, oDimensionUniqueName,
                        oHierarchyUniqueName, oLevelUniqueName, oPropertyName, oPropertyOrigin, oCubeSource,
                        oPropertyVisibility))
                .flatMap(Collection::stream).toList();
    }

    private static List<MdSchemaPropertiesResponseRow> getMdSchemaPropertiesResponseRow(String catalogName,
            String schemaName, Cube cube, Optional<String> oDimensionUniqueName, Optional<String> oHierarchyUniqueName,
            Optional<String> oLevelUniqueName, Optional<String> oPropertyName,
            Optional<PropertyOriginEnum> oPropertyOrigin, Optional<CubeSourceEnum> oCubeSource,
            Optional<VisibilityEnum> oPropertyVisibility) {
        if (oLevelUniqueName.isPresent()) {
            // Note: If the LEVEL_UNIQUE_NAME has been specified, then
            // the dimension and hierarchy are specified implicitly.
            String levelUniqueName = oLevelUniqueName.get();
            if (levelUniqueName == null) {
                // The query specified two or more unique names
                // which means that nothing will match.
                return List.of();
            }
            Optional<Level> oLevel = lookupLevel(cube, levelUniqueName);
            if (!oLevel.isPresent()) {
                return List.of();
            }
            return getMdSchemaPropertiesResponseRow(catalogName, schemaName, cube, oLevel.get(), oPropertyName,
                    oPropertyOrigin, oCubeSource, oPropertyVisibility);
        } else {
            List<? extends Dimension> dimensions = cube.getDimensions() == null ? List.of() : cube.getDimensions();
            return getDimensionsWithFilterByUniqueName(dimensions, oDimensionUniqueName).stream().filter(d -> d != null)
                    .map(d -> getMdSchemaMeasuresResponseRow(catalogName, schemaName, cube, d, oHierarchyUniqueName,
                            oPropertyName, oPropertyOrigin, oCubeSource, oPropertyVisibility))
                    .flatMap(Collection::stream).toList();
        }

    }

    private static List<MdSchemaPropertiesResponseRow> getMdSchemaMeasuresResponseRow(String catalogName,
            String schemaName, Cube cube, Dimension dimension, Optional<String> oHierarchyUniqueName,
            Optional<String> oPropertyName, Optional<PropertyOriginEnum> oPropertyOrigin,
            Optional<CubeSourceEnum> oCubeSource, Optional<VisibilityEnum> oPropertyVisibility) {
        List<? extends Hierarchy> hierarchies = dimension.getHierarchies() == null ? List.of()
                : dimension.getHierarchies();
        return getHierarchiesWithFilterByUniqueName(hierarchies, oHierarchyUniqueName)
                .stream().filter(h -> h != null).map(h -> getMdSchemaPropertiesResponseRow(catalogName, schemaName,
                        cube, h, oPropertyName, oPropertyOrigin, oCubeSource, oPropertyVisibility))
                .flatMap(Collection::stream).toList();
    }

    private static List<MdSchemaPropertiesResponseRow> getMdSchemaPropertiesResponseRow(String catalogName,
            String schemaName, Cube cube, Hierarchy hierarchy, Optional<String> oPropertyName,
            Optional<PropertyOriginEnum> oPropertyOrigin, Optional<CubeSourceEnum> oCubeSource,
            Optional<VisibilityEnum> oPropertyVisibility) {
        List<? extends Level> levels = hierarchy.getLevels() == null ? List.of() : hierarchy.getLevels();
        return levels.stream().map(l -> getMdSchemaPropertiesResponseRow(catalogName, schemaName, cube, l,
                oPropertyName, oPropertyOrigin, oCubeSource, oPropertyVisibility)).flatMap(Collection::stream).toList();
    }

    private static List<MdSchemaPropertiesResponseRow> getMdSchemaPropertiesResponseRow(String catalogName,
            String schemaName, Cube cube, Level level, Optional<String> oPropertyName,
            Optional<PropertyOriginEnum> oPropertyOrigin, Optional<CubeSourceEnum> oCubeSource,
            Optional<VisibilityEnum> oPropertyVisibility) {
        List<org.eclipse.daanse.olap.api.element.Property> properties = level.getProperties() == null ? List.of()
                : Arrays.asList(level.getProperties());
        return getPropertiesWithFilterByUniqueName(properties, oPropertyName).stream().filter(p -> p != null)
                .map(p -> getMdSchemaPropertiesResponseRow(catalogName, schemaName, cube, level, p)).toList();
    }

    private static List<org.eclipse.daanse.olap.api.element.Property> getPropertiesWithFilterByUniqueName(
            List<org.eclipse.daanse.olap.api.element.Property> properties, Optional<String> oPropertyName) {
        if (oPropertyName.isPresent()) {
            return properties.stream().filter(p -> oPropertyName.get().equals(p.getName())).toList();
        }
        return properties;
    }

    private static MdSchemaPropertiesResponseRow getMdSchemaPropertiesResponseRow(String catalogName, String schemaName,
            Cube cube, Level level, org.eclipse.daanse.olap.api.element.Property property) {
        Hierarchy hierarchy = level.getHierarchy();
        Dimension dimension = hierarchy.getDimension();
        String propertyName = property.getName();
        LevelDbTypeEnum dbType = getDBTypeFromProperty(property);

        String desc = cube.getName() + " Cube - " + getHierarchyName(hierarchy.getName(), dimension.getName())
                + " Hierarchy - " + level.getName() + " Level - " + property.getName() + " Property";

        return new MdSchemaPropertiesResponseRowR(Optional.ofNullable(catalogName), Optional.ofNullable(schemaName),
                Optional.ofNullable(cube.getName()), Optional.ofNullable(dimension.getUniqueName()),
                Optional.ofNullable(hierarchy.getUniqueName()), Optional.ofNullable(level.getUniqueName()),
                // TODO: what is the correct value here? ""?
                Optional.empty(), Optional.of(PropertyTypeEnum.PROPERTY_MEMBER), Optional.ofNullable(propertyName),
                Optional.ofNullable(property.getCaption()), Optional.of(dbType), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.ofNullable(desc),
                Optional.of(PropertyContentTypeEnum.REGULAR), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(CubeSourceEnum.DIMENSION),
                Optional.of(VisibilityEnum.VISIBLE));

    }

    private static LevelDbTypeEnum getDBTypeFromProperty(org.eclipse.daanse.olap.api.element.Property prop) {
        if (prop.getType() != null) {
            switch (prop.getType()) {
            case TYPE_STRING:
                return LevelDbTypeEnum.DBTYPE_WSTR;
            case TYPE_INTEGER, TYPE_LONG, TYPE_NUMERIC:
                return LevelDbTypeEnum.DBTYPE_R8;
            case TYPE_BOOLEAN:
                return LevelDbTypeEnum.DBTYPE_BOOL;
            default:
                // TODO: what type is it really, its not a string
                return LevelDbTypeEnum.DBTYPE_WSTR;
            }
        }
        return LevelDbTypeEnum.DBTYPE_WSTR;
    }

    static List<MdSchemaMeasuresResponseRow> getMdSchemaMeasuresResponseRow(Catalog catalog,
            Optional<String> oSchemaName, Optional<String> oCubeName, Optional<String> oMeasureName,
            Optional<String> oMeasureUniqueName, Optional<String> oMeasureGroupName,
            boolean shouldEmitInvisibleMembers) {
        return getMdSchemaMeasuresResponseRow(catalog.getName(), catalog, oCubeName, oMeasureName, oMeasureUniqueName,
                oMeasureGroupName, shouldEmitInvisibleMembers);

    }

    private static List<MdSchemaMeasuresResponseRow> getMdSchemaMeasuresResponseRow(String catalogName, Catalog schema,
            Optional<String> oCubeName, Optional<String> oMeasureName, Optional<String> oMeasureUniqueName,
            Optional<String> oMeasureGroupName, boolean shouldEmitInvisibleMembers) {
        List<Cube> cubes = schema.getCubes() == null ? List.of() : schema.getCubes();
        return getCubesWithFilter(cubes, oCubeName).stream()
                .map(c -> getMdSchemaMeasuresResponseRow(catalogName, schema.getName(), c, oMeasureName,
                        oMeasureUniqueName, oMeasureGroupName, shouldEmitInvisibleMembers))
                .flatMap(Collection::stream).toList();
    }

    private static List<MdSchemaMeasuresResponseRow> getMdSchemaMeasuresResponseRow(String catalogName,
            String schemaName, Cube cube, Optional<String> oMeasureName, Optional<String> oMeasureUniqueName,
            Optional<String> oMeasureGroupName, boolean shouldEmitInvisibleMembers) {
        List<MdSchemaMeasuresResponseRow> result = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        int j = 0;
        for (Dimension dimension : cube.getDimensions()) {
            if (!DimensionType.MEASURES_DIMENSION.equals(dimension.getDimensionType())) {
                for (Hierarchy hierarchy : dimension.getHierarchies()) {
                    List<? extends Level> levels = hierarchy.getLevels();
                    if (levels != null && levels.size() > 0) {
                        Level lastLevel = levels.getLast();
                        if (j++ > 0) {
                            builder.append(',');
                        }
                        builder.append(lastLevel.getUniqueName());
                    }
                }
            }
        }
        String levelListStr = builder.toString();
        List<org.eclipse.daanse.olap.api.element.Member> measures = getMeasureWithFilterByUniqueName(
                getMeasureWithFilterByName(cube.getMeasures(), oMeasureName), oMeasureUniqueName);
        measures.stream().filter(m -> !m.isCalculated()).forEach(m -> populateMeasures(catalogName, schemaName,
                cube.getName(), levelListStr, shouldEmitInvisibleMembers, m, result));
        measures.stream().filter(m -> m.isCalculated()).forEach(m -> populateMeasures(catalogName, schemaName,
                cube.getName(), null, shouldEmitInvisibleMembers, m, result));
        return result;
    }

    private static void populateMeasures(String catalogName, String schemaName, String cubeName, String levelListStr,
            boolean shouldEmitInvisibleMembers, Member m, List<MdSchemaMeasuresResponseRow> result) {
        Boolean visible = (Boolean) m.getPropertyValue(Property.StandardMemberProperty.$visible.getName());
        if (visible == null) {
            visible = true;
        }
        if (visible || shouldEmitInvisibleMembers) {
            // TODO: currently this is always null
            String desc = m.getDescription();
            if (desc == null) {
                desc = cubeName + " Cube - " + m.getName() + " Member";
            }
            final String formatString = (String) m
                    .getPropertyValue(Property.StandardCellProperty.FORMAT_STRING.getName());
            final MeasureAggregatorEnum measureAggregator = getAggregator(m);

            // DATA_TYPE DBType best guess is string
            DBType dbType = XmlaConstants.DBType.WSTR;
            String datatype = (String) m.getPropertyValue(Property.StandardCellProperty.DATATYPE.getName());
            if (datatype != null) {
                if (datatype.equals("Integer")) {
                    dbType = XmlaConstants.DBType.I4;
                } else if (datatype.equals("Numeric")) {
                    dbType = XmlaConstants.DBType.R8;
                } else {
                    dbType = XmlaConstants.DBType.WSTR;
                }
            }
            String displayFolder = "";
            if (m.getPropertyValue(StandardProperty.DISPLAY_FOLDER.getName()) != null) {
                displayFolder = (String) m.getPropertyValue(StandardProperty.DISPLAY_FOLDER.getName());
            }
            result.add(new MdSchemaMeasuresResponseRowR(Optional.ofNullable(catalogName),
                    Optional.ofNullable(schemaName), Optional.ofNullable(cubeName), Optional.ofNullable(m.getName()),
                    Optional.ofNullable(m.getUniqueName()), Optional.ofNullable(m.getCaption()), Optional.empty(),
                    Optional.ofNullable(measureAggregator),
                    Optional.ofNullable(LevelDbTypeEnum.fromValue(String.valueOf(dbType.xmlaOrdinal()))),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(desc), Optional.empty(),
                    Optional.ofNullable(visible), Optional.ofNullable(levelListStr), Optional.empty(), Optional.empty(),
                    Optional.ofNullable(cubeName), Optional.ofNullable(displayFolder),
                    Optional.ofNullable(formatString), Optional.empty(), Optional.of(VisibilityEnum.VISIBLE)));
        }
    }

    private static MeasureAggregatorEnum getAggregator(Member m) {
        if (m instanceof StoredMeasure storedMeasure) {
            String aggregateFunction = storedMeasure.getAggregateFunction();
            if (aggregateFunction != null) {
                if (aggregateFunction.equalsIgnoreCase("Avg")) {
                    return MeasureAggregatorEnum.MDMEASURE_AGGR_AVG;
                }
                if (aggregateFunction.equalsIgnoreCase("Sum")) {
                    return MeasureAggregatorEnum.MDMEASURE_AGGR_SUM;
                }
                if (aggregateFunction.equalsIgnoreCase("Count")) {
                    return MeasureAggregatorEnum.MDMEASURE_AGGR_COUNT;
                }
                if (aggregateFunction.equalsIgnoreCase("DistinctCount")) {
                    return MeasureAggregatorEnum.MDMEASURE_AGGR_DST;
                }
                if (aggregateFunction.equalsIgnoreCase("Max")) {
                    return MeasureAggregatorEnum.MDMEASURE_AGGR_MAX;
                }
                if (aggregateFunction.equalsIgnoreCase("Min")) {
                    return MeasureAggregatorEnum.MDMEASURE_AGGR_MIN;
                }
            }
        }
        return MeasureAggregatorEnum.MDMEASURE_AGGR_UNKNOWN;
    }

    private static List<org.eclipse.daanse.olap.api.element.Member> getMeasureWithFilterByName(
            List<org.eclipse.daanse.olap.api.element.Member> measures, Optional<String> oMeasureName) {
        if (oMeasureName.isPresent()) {
            return measures.stream().filter(m -> oMeasureName.get().equals(m.getName())).toList();
        }
        return measures;
    }

    private static List<org.eclipse.daanse.olap.api.element.Member> getMeasureWithFilterByUniqueName(
            List<org.eclipse.daanse.olap.api.element.Member> measures, Optional<String> oMeasureUniqueName) {
        if (oMeasureUniqueName.isPresent()) {
            return measures.stream().filter(m -> oMeasureUniqueName.get().equals(m.getUniqueName())).toList();
        }
        return measures;
    }
}
