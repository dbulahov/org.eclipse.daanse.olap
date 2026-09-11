/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.xmla.connector.relational;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.xmla.api.XmlaRequest;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.db.DatabaseSchema;
import org.eclipse.daanse.olap.api.element.db.DatabaseTable;
import org.eclipse.daanse.olap.xmla.connector.ContextListSupplyer;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaCatalogsRow;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaColumnsRow;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaProviderTypesRow;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaTablesInfoRow;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaSchemataRow;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaTablesRow;
import org.eclipse.daanse.xmla.model.rowset.relational.RowsetRelationalFactory;
import org.eclipse.daanse.xmla.api.RestrictionValues;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.daanse.olap.xmla.connector.common.DiscoverScope;
import org.eclipse.daanse.olap.xmla.connector.common.OleDbType;

/**
 * The DBSCHEMA_* rowsets: the OLE DB relational surface.
 * <p>
 * Ported from the bridge's {@code DBSchemaDiscoverService} and the DBSCHEMA
 * half of its {@code Utils}. The classic rowsets — catalogs, schemata, tables —
 * read the OLAP metadata the way the bridge did: cubes as TABLEs, levels as
 * SYSTEM TABLEs named {@code cube:hierarchy:level}, database tables as SCHEMA
 * entries. The CWM relational rowsets (constraints, keys, indexes, procedures,
 * views) wait on {@code Catalog.getRelationalSchemas()} landing in the api.
 * Until then they are modelled but unserved, and an unserved rowset answers
 * empty rather than faulting.
 */
public class DbSchemaDiscover {

    private static final RowsetRelationalFactory FACTORY = RowsetRelationalFactory.eINSTANCE;

    private static final String TABLE = "TABLE";
    private static final String SYSTEM_TABLE = "SYSTEM TABLE";
    private static final String SCHEMA = "SCHEMA";

    /**
     * The two {@code TABLE_OLAP_TYPE} values that say a row describes a cube object
     * rather than a table. [MS-SSAS] names a third, {@code SCHEMA}, for a schema
     * rowset; the catalog's own tables carry none of them.
     */
    private static final String MEASURE_GROUP = "MEASURE_GROUP";
    private static final String CUBE_DIMENSION = "CUBE_DIMENSION";

    private final ContextListSupplyer contexts;

    public DbSchemaDiscover(ContextListSupplyer contexts) {
        this.contexts = contexts;
    }

    // --- DBSCHEMA_COLUMNS, ported from Utils.getDbSchemaColumnsResponseRow ---

    public List<EObject> columns(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("TABLE_CATALOG").or(restrictions::catalogProperty);
        Optional<String> tableName = restrictions.value("TABLE_NAME");
        Optional<String> columnName = restrictions.value("COLUMN_NAME");
        Optional<String> olapType = restrictions.value("COLUMN_OLAP_TYPE");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            List<Cube> cubes = sortedByName(catalog.getCubes());
            for (Cube cube : cubes) {
                if (tableName.isEmpty() || tableName.get().equals(cube.getName())) {
                    if (olapType.isEmpty() || "ATTRIBUTE".equals(olapType.get())) {
                        for (Dimension dimension : cube.getDimensions()) {
                            hierarchyColumns(catalog.getName(), cube, dimension, result);
                        }
                    }
                    if (olapType.isEmpty() || "MEASURE".equals(olapType.get())) {
                        measureColumns(cube, columnName, result);
                    }
                }
            }
            // Outside the cube loop on purpose. The columns of the catalog's own tables
            // have nothing to do with any cube: inside, they were repeated once per cube
            // and a catalog holding tables but no cube produced none at all.
            if (olapType.isEmpty() || SCHEMA.equals(olapType.get())) {
                List<? extends DatabaseSchema> schemas = DiscoverScope.databaseSchemas(contexts, catalog, caller);
                schemaColumns(catalog.getName(), schemas, tableName, columnName, result);
            }
        }
        return result;
    }

    /**
     * Two columns per hierarchy, {@code <hierarchy>:(All)!NAME} and
     * {@code ...!UNIQUE_NAME}. The ordinal restarts at one for every dimension -
     * the bridge passed a boxed Integer around and the increments never made it
     * back to the caller; the numbering is part of what clients have seen, so it
     * stays.
     */
    private static void hierarchyColumns(String catalogName, Cube cube, Dimension dimension, List<EObject> result) {
        int ordinal = 1;
        for (Hierarchy hierarchy : dimension.getHierarchies()) {
            for (String suffix : new String[] { ":(All)!NAME", ":(All)!UNIQUE_NAME" }) {
                DbschemaColumnsRow row = FACTORY.createDbschemaColumnsRow();
                row.setTableCatalog(catalogName);
                row.setTableName(cube.getName());
                row.setColumnName(hierarchy.getName() + suffix);
                row.setOrdinalPosition((long) ordinal++);
                row.setColumnHasDefault(Boolean.FALSE);
                row.setIsNullable(Boolean.FALSE);
                row.setDataType(OleDbType.WSTR.dbTypeOrdinal());
                row.setCharacterMaximumLength(0L);
                row.setCharacterOctetLength(0L);
                result.add(row);
            }
        }
    }

    private static void measureColumns(Cube cube, Optional<String> columnName, List<EObject> result) {
        int ordinal = 1;
        for (Member measure : cube.getMeasures()) {
            String name = "Measures:" + measure.getName();
            if (columnName.isPresent() && !columnName.get().equals(name)) {
                continue;
            }
            DbschemaColumnsRow row = FACTORY.createDbschemaColumnsRow();
            row.setTableCatalog(cube.getName());
            row.setTableName(cube.getName());
            row.setColumnName(name);
            row.setOrdinalPosition((long) ordinal++);
            row.setColumnHasDefault(Boolean.FALSE);
            row.setIsNullable(Boolean.FALSE);
            row.setDataType(OleDbType.R8.dbTypeOrdinal());
            row.setCharacterMaximumLength(0L);
            row.setCharacterOctetLength(0L);
            row.setNumericPrecision(16);
            row.setNumericScale((short) 255);
            result.add(row);
        }
    }

    private static void schemaColumns(String catalogName, List<? extends DatabaseSchema> schemas,
            Optional<String> tableName, Optional<String> columnName, List<EObject> result) {
        int ordinal = 1;
        for (DatabaseSchema schema : schemas) {
            if (schema.getDbTables() == null) {
                continue;
            }
            for (DatabaseTable table : schema.getDbTables()) {
                if (tableName.isPresent() && !tableName.get().equals(table.getName())) {
                    continue;
                }
                if (table.getDbColumns() == null) {
                    continue;
                }
                for (org.eclipse.daanse.olap.api.element.db.DatabaseColumn column : table.getDbColumns()) {
                    if (columnName.isPresent() && !columnName.get().equals(column.getName())) {
                        continue;
                    }
                    DbschemaColumnsRow row = FACTORY.createDbschemaColumnsRow();
                    row.setTableCatalog(catalogName);
                    row.setTableSchema(schema.getName());
                    row.setTableName(table.getName());
                    row.setColumnName(column.getName());
                    row.setOrdinalPosition((long) ordinal++);
                    row.setColumnHasDefault(Boolean.FALSE);
                    row.setIsNullable(Boolean.FALSE);
                    row.setDataType(OleDbType.R8.dbTypeOrdinal());
                    row.setCharacterMaximumLength(0L);
                    row.setCharacterOctetLength(0L);
                    row.setNumericPrecision(16);
                    row.setNumericScale((short) 255);
                    row.setColumnOlapType(SCHEMA);
                    result.add(row);
                }
            }
        }
    }

    // --- DBSCHEMA_PROVIDER_TYPES: the six OLE DB types this server speaks ---

    public List<EObject> providerTypes(RestrictionValues restrictions) {
        Optional<String> dataType = restrictions.value("DATA_TYPE");
        List<EObject> result = new ArrayList<>();
        providerType(OleDbType.I4, 8L, dataType, result);
        providerType(OleDbType.R8, 16L, dataType, result);
        providerType(OleDbType.CY, 8L, dataType, result);
        providerType(OleDbType.BOOL, 1L, dataType, result);
        providerType(OleDbType.I8, 16L, dataType, result);
        providerType(OleDbType.WSTR, 255L, dataType, result);
        return result;
    }

    private static void providerType(OleDbType type, long columnSize, Optional<String> wanted, List<EObject> result) {
        if (wanted.isPresent() && Integer.parseInt(wanted.get()) != type.dbTypeOrdinal()) {
            return;
        }
        DbschemaProviderTypesRow row = FACTORY.createDbschemaProviderTypesRow();
        row.setTypeName(type.userName());
        row.setDataType(type.dbTypeOrdinal());
        row.setColumnSize(columnSize);
        row.setIsNullable(Boolean.TRUE);
        if (type == OleDbType.WSTR) {
            row.setLiteralPrefix("\"");
            row.setLiteralSuffix("\"");
            row.setCaseSensitive(Boolean.FALSE);
        } else {
            row.setUnsignedAttribute(Boolean.FALSE);
        }
        row.setFixedPrecScale(Boolean.FALSE);
        row.setAutoUniqueValue(Boolean.FALSE);
        row.setIsLong(Boolean.FALSE);
        row.setBestMatch(Boolean.TRUE);
        result.add(row);
    }

    // --- DBSCHEMA_TABLES_INFO: one row per cube, as the bridge answered it ---

    public List<EObject> tablesInfo(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("TABLE_CATALOG").or(restrictions::catalogProperty);
        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            List<Cube> cubes = sortedByName(catalog.getCubes());
            for (Cube cube : cubes) {
                String description = cube.getDescription();
                if (description == null) {
                    description = catalog.getName() + " - " + cube.getName() + " Cube";
                }
                DbschemaTablesInfoRow row = FACTORY.createDbschemaTablesInfoRow();
                row.setTableCatalog(catalog.getName());
                row.setTableName(cube.getName());
                row.setTableType(TABLE);
                row.setBookmarks(Boolean.FALSE);
                // SQL Server answers 1000000 for every table, and so did the bridge.
                row.setCardinality(BigInteger.valueOf(1_000_000L));
                row.setDescription(description);
                result.add(row);
            }
        }
        return result;
    }

    // --- DBSCHEMA_CATALOGS ---

    /**
     * The catalogs this caller may open, and only those.
     * <p>
     * A catalog listed here is one a client will go on to ask about, so offering
     * one it cannot open turns a rowset into a promise the next request breaks with
     * "user doesn't have any roles assigned".
     */
    public List<EObject> catalogs(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME");

        List<EObject> result = new ArrayList<>();
        List<Context<?>> available = contexts.getContexts() == null ? List.of() : contexts.getContexts();
        for (Context<?> context : available) {
            if (catalogName.isPresent() && !catalogName.get().equalsIgnoreCase(context.getName())) {
                continue;
            }
            if (DiscoverScope.mayOpen(context, caller)) {
                result.add(catalogRow(context));
            }
        }
        return result;
    }

    /**
     * The compatibility level every catalog reports, matching the one
     * DISCOVER_XML_METADATA states for the same database.
     * <p>
     * 1100 is SQL Server 2012, which is what this engine is; 1200 and above mean
     * tabular metadata, which it has not.
     */
    private static final int MULTIDIMENSIONAL_COMPATIBILITY_LEVEL = 1100;

    /** TYPE is a mask, and no bit set means a multidimensional database. */
    private static final int TYPE_MULTIDIMENSIONAL = 0;

    private static EObject catalogRow(Context<?> context) {
        DbschemaCatalogsRow row = FACTORY.createDbschemaCatalogsRow();
        row.setCatalogName(context.getName());
        context.getDescription().ifPresent(row::setDescription);
        List<String> roles = context.getAccessRoles();
        if (roles != null) {
            row.setRoles(String.join(",", roles));
        }
        // UTC: a client converts DATE_MODIFIED to local time itself, so a local time
        // written here would be shifted twice.
        row.setDateModified(LocalDateTime.now(java.time.ZoneOffset.UTC));
        // Both are numbers, and a number a client asks for must be there. A client
        // listing databases reads COMPATIBILITY_LEVEL into a value type, and a column
        // left out of the row arrives as null and throws before one catalog is shown.
        // Declaring it in the inline schema is not enough.
        row.setCompatibilityLevel(MULTIDIMENSIONAL_COMPATIBILITY_LEVEL);
        row.setType(TYPE_MULTIDIMENSIONAL);
        return row;
    }

    // --- DBSCHEMA_SCHEMATA ---

    public List<EObject> schemata(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME").or(restrictions::catalogProperty);
        Optional<String> schemaName = restrictions.value("SCHEMA_NAME");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
        	List<? extends DatabaseSchema> schemas = DiscoverScope.databaseSchemas(contexts, catalog, caller);
            if (schemas == null) {
                continue;
            }
            for (DatabaseSchema schema : schemas) {
                if (schemaName.isPresent() && !schemaName.get().equals(schema.getName())) {
                    continue;
                }
                DbschemaSchemataRow row = FACTORY.createDbschemaSchemataRow();
                row.setCatalogName(catalog.getName());
                row.setSchemaName(schema.getName());
                row.setSchemaOwner("");
                result.add(row);
            }
        }
        return result;
    }

    // --- DBSCHEMA_TABLES, ported from Utils.getDbSchemaTablesResponseRow ---

    public List<EObject> tables(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("TABLE_CATALOG").or(restrictions::catalogProperty);
        Optional<String> tableType = restrictions.value("TABLE_TYPE");
        Optional<String> tableSchema = restrictions.value("TABLE_SCHEMA");
        Optional<String> tableName = restrictions.value("TABLE_NAME");
        Optional<String> olapType = restrictions.value("TABLE_OLAP_TYPE");

        List<EObject> result = new ArrayList<>();
        for (Catalog catalog : DiscoverScope.catalogs(contexts, catalogName, caller)) {
            // Sorted, as DBSCHEMA_COLUMNS and DBSCHEMA_TABLES_INFO sort. This rowset did
            // not, so the same server answered the three in different orders and a client
            // reading them side by side saw a shuffle it had no reason to expect.
            for (Cube cube : sortedByName(catalog.getCubes())) {
                String description = cube.getDescription();
                if (description == null) {
                    description = catalog.getName() + " - " + cube.getName() + " Cube";
                }
                if (isTableType(tableType, TABLE) && matches(olapType, MEASURE_GROUP)
                        && matches(tableSchema, cube.getName()) && matches(tableName, cube.getName())) {
                    // TABLE_SCHEMA is the cube an object belongs to, and every live server
                    // fills it: Analysis Services on all 318 rows of a recorded answer, the
                    // Mondrian bridge with the cube name.
                    result.add(tableRow(catalog.getName(), cube.getName(), cube.getName(), TABLE, MEASURE_GROUP,
                            description));
                }
                if (isTableType(tableType, SYSTEM_TABLE) && matches(olapType, CUBE_DIMENSION)
                        && matches(tableSchema, cube.getName())) {
                    for (Dimension dimension : cube.getDimensions()) {
                        levelTables(catalog.getName(), cube, dimension, tableName, result);
                    }
                }
            }
            // Outside the cube loop: a catalog's own tables do not belong to any cube, and
            // a catalog with no cube at all still has them.
            if (isTableType(tableType, TABLE) && olapType.isEmpty()) {
                List<? extends DatabaseSchema> schemas = DiscoverScope.databaseSchemas(contexts, catalog, caller);
                databaseTables(catalog.getName(), schemas, tableSchema, tableName, result);
            }
        }
        return result;
    }

    /** Whether an optional restriction is absent or satisfied by this value. */
    private static boolean matches(Optional<String> restriction, String value) {
        return restriction.isEmpty() || restriction.get().equals(value);
    }

    /**
     * Every level of every hierarchy, as a {@code cube:hierarchy:level} system
     * table.
     */
    private static void levelTables(String catalogName, Cube cube, Dimension dimension, Optional<String> nameWanted,
            List<EObject> result) {
        if (dimension == null) {
            return;
        }
        for (Hierarchy hierarchy : dimension.getHierarchies()) {
            if (hierarchy.getLevels() == null) {
                continue;
            }
            for (Level level : hierarchy.getLevels()) {
                String hierarchyName = DiscoverScope.hierarchyName(hierarchy.getName(), dimension.getName());
                String tableName = cube.getName() + ':' + hierarchyName + ':' + level.getName();
                if (!matches(nameWanted, tableName)) {
                    continue;
                }
                String description = level.getDescription();
                if (description == null) {
                    description = catalogName + " - " + cube.getName() + " Cube - " + hierarchyName + " Hierarchy - "
                            + level.getName() + " Level";
                }
                result.add(tableRow(catalogName, cube.getName(), tableName, SYSTEM_TABLE, CUBE_DIMENSION, description));
            }
        }
    }

    /**
     * The catalog's own tables — the ones a client can actually query.
     * <p>
     * They are {@code TABLE}, like a measure group, because that is what OLE DB
     * calls a table; {@code SCHEMA} would be wrong, as [MS-SSAS] gives that value
     * to a schema rowset — a {@code $SYSTEM} table — and not to a user table.
     * {@code TABLE_OLAP_TYPE} is left unset, and that absence is the only thing
     * telling a client these rows are tables rather than cube objects.
     */
    private static void databaseTables(String catalogName, List<? extends DatabaseSchema> schemas, Optional<String> schemaWanted, Optional<String> nameWanted,
            List<EObject> result) {
        for (DatabaseSchema schema : schemas) {
            List<? extends DatabaseTable> tables = schema.getDbTables();
            if (tables == null || !matches(schemaWanted, schema.getName())) {
                continue;
            }
            for (DatabaseTable table : tables) {
                if (matches(nameWanted, table.getName())) {
                    result.add(tableRow(catalogName, schema.getName(), table.getName(), TABLE, null,
                            table.getDescription()));
                }
            }
        }
    }

    private static EObject tableRow(String catalogName, String schemaName, String tableName, String tableType,
            String olapType, String description) {
        DbschemaTablesRow row = FACTORY.createDbschemaTablesRow();
        row.setTableCatalog(catalogName);
        if (schemaName != null) {
            row.setTableSchema(schemaName);
        }
        row.setTableName(tableName);
        row.setTableType(tableType);
        if (olapType != null) {
            row.setTableOlapType(olapType);
        }
        if (description != null) {
            row.setDescription(description);
        }
        return row;
    }

    /** Cubes by name — the order every DBSCHEMA rowset answers in. */
    private static List<Cube> sortedByName(List<? extends Cube> cubes) {
        List<Cube> sorted = new ArrayList<>(cubes);
        sorted.sort((left, right) -> left.getName().compareTo(right.getName()));
        return sorted;
    }

    private static boolean isTableType(Optional<String> wanted, String type) {
        return wanted.isEmpty() || wanted.get().equals(type);
    }

}
