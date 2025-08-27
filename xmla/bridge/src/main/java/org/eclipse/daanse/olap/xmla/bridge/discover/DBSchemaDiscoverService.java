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

import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getDbSchemaColumnsResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getDbSchemaSchemataResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getDbSchemaSourceTablesResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getDbSchemaTablesInfoResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getRoles;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.isDataTypeCond;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.xmla.bridge.ContextListSupplyer;
import org.eclipse.daanse.xmla.api.RequestMetaData;
import org.eclipse.daanse.xmla.api.XmlaConstants;
import org.eclipse.daanse.xmla.api.common.enums.ColumnOlapTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.LevelDbTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.TableTypeEnum;
import org.eclipse.daanse.xmla.api.discover.dbschema.catalogs.DbSchemaCatalogsRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.catalogs.DbSchemaCatalogsResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.columns.DbSchemaColumnsRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.columns.DbSchemaColumnsResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.providertypes.DbSchemaProviderTypesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.providertypes.DbSchemaProviderTypesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.schemata.DbSchemaSchemataRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.schemata.DbSchemaSchemataResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.sourcetables.DbSchemaSourceTablesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.sourcetables.DbSchemaSourceTablesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.tables.DbSchemaTablesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.tables.DbSchemaTablesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.tablesinfo.DbSchemaTablesInfoRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.tablesinfo.DbSchemaTablesInfoResponseRow;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.catalogs.DbSchemaCatalogsResponseRowR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.providertypes.DbSchemaProviderTypesResponseRowR;

public class DBSchemaDiscoverService {

    private ContextListSupplyer contextsListSupplyer;

    public DBSchemaDiscoverService(ContextListSupplyer contextsListSupplyer) {
        this.contextsListSupplyer = contextsListSupplyer;
    }

    public List<DbSchemaCatalogsResponseRow> dbSchemaCatalogs(DbSchemaCatalogsRequest request, RequestMetaData metaData) {

        Optional<String> oCatalogName = request.restrictions().catalogName();
        if (oCatalogName.isPresent()) {
            Optional<Context> oContext = oCatalogName.flatMap(name -> contextsListSupplyer.getContext(name));
            if (oContext.isPresent()) {
                Context context = oContext.get();
                return List.of(dbSchemaCatalogsRow(context));
            }
        } else {
            return contextsListSupplyer.getContexts().stream().map(this::dbSchemaCatalogsRow).toList();
        }
        return List.of();
    }

    public DbSchemaCatalogsResponseRow dbSchemaCatalogsRow(Context catalog) {
        return new DbSchemaCatalogsResponseRowR(Optional.ofNullable(catalog.getName()), catalog.getDescription(),
                getRoles(catalog.getAccessRoles()), Optional.of(LocalDateTime.now()), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    public List<DbSchemaColumnsResponseRow> dbSchemaColumns(DbSchemaColumnsRequest request, RequestMetaData metaData) {
        Optional<String> oCatalog = request.restrictions().tableCatalog();
        Optional<String> oTableSchema = request.restrictions().tableSchema();
        Optional<String> oTableName = request.restrictions().tableName();
        Optional<String> oColumnName = request.restrictions().columnName();
        Optional<ColumnOlapTypeEnum> oColumnOlapType = request.restrictions().columnOlapType();
        List<DbSchemaColumnsResponseRow> result = new ArrayList<>();
        if (oCatalog.isPresent()) {
            Optional<Catalog> oContext = oCatalog
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oContext.isPresent()) {
                Catalog catalog = oContext.get();
                result.addAll(
                        getDbSchemaColumnsResponseRow(catalog, oTableSchema, oTableName, oColumnName, oColumnOlapType));
            }
        } else {
            result.addAll(contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getDbSchemaColumnsResponseRow(c, oTableSchema, oTableName, oColumnName, oColumnOlapType))
                    .flatMap(Collection::stream).toList());
        }
        return result;
    }

    public List<DbSchemaProviderTypesResponseRow> dbSchemaProviderTypes(DbSchemaProviderTypesRequest request,
            RequestMetaData metaData) {
        List<DbSchemaProviderTypesResponseRow> result = new ArrayList<>();
        Optional<LevelDbTypeEnum> oLevelDbType = request.restrictions().dataType();

        if (isDataTypeCond(XmlaConstants.DBType.I4, oLevelDbType)) {
            result.add(new DbSchemaProviderTypesResponseRowR(Optional.of(XmlaConstants.DBType.I4.userName),
                    Optional.of(LevelDbTypeEnum.DBTYPE_I4), Optional.of(8), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.of(true), Optional.empty(), Optional.empty(), Optional.of(false),
                    Optional.of(false), Optional.of(false), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(false), Optional.of(true),
                    Optional.empty()));
        }

        // R8
        if (isDataTypeCond(XmlaConstants.DBType.R8, oLevelDbType)) {

            result.add(new DbSchemaProviderTypesResponseRowR(Optional.of(XmlaConstants.DBType.R8.userName),
                    Optional.of(LevelDbTypeEnum.DBTYPE_R8), Optional.of(16), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.of(true), Optional.empty(), Optional.empty(), Optional.of(false),
                    Optional.of(false), Optional.of(false), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(false), Optional.of(true),
                    Optional.empty()));
        }

        // CY
        if (isDataTypeCond(XmlaConstants.DBType.CY, oLevelDbType)) {
            result.add(new DbSchemaProviderTypesResponseRowR(Optional.of(XmlaConstants.DBType.CY.userName),
                    Optional.of(LevelDbTypeEnum.DBTYPE_CY), Optional.of(8), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.of(true), Optional.empty(), Optional.empty(), Optional.of(false),
                    Optional.of(false), Optional.of(false), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(false), Optional.of(true),
                    Optional.empty()));
        }

        // BOOL
        if (isDataTypeCond(XmlaConstants.DBType.BOOL, oLevelDbType)) {
            result.add(new DbSchemaProviderTypesResponseRowR(Optional.of(XmlaConstants.DBType.BOOL.userName),
                    Optional.of(LevelDbTypeEnum.DBTYPE_BOOL), Optional.of(1), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.of(true), Optional.empty(), Optional.empty(), Optional.of(false),
                    Optional.of(false), Optional.of(false), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(false), Optional.of(true),
                    Optional.empty()));
        }
        // I8
        if (isDataTypeCond(XmlaConstants.DBType.I8, oLevelDbType)) {
            result.add(new DbSchemaProviderTypesResponseRowR(Optional.of(XmlaConstants.DBType.I8.userName),
                    Optional.of(LevelDbTypeEnum.DBTYPE_I8), Optional.of(16), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.of(true), Optional.empty(), Optional.empty(), Optional.of(false),
                    Optional.of(false), Optional.of(false), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(false), Optional.of(true),
                    Optional.empty()));
        }

        // WSTR
        if (isDataTypeCond(XmlaConstants.DBType.WSTR, oLevelDbType)) {

            result.add(new DbSchemaProviderTypesResponseRowR(Optional.of(XmlaConstants.DBType.WSTR.userName),
                    Optional.of(LevelDbTypeEnum.DBTYPE_WSTR), Optional.of(255), Optional.of("\""), Optional.of("\""),
                    Optional.empty(), Optional.of(true), Optional.of(false), Optional.empty(), Optional.empty(),
                    Optional.of(false), Optional.of(false), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(false), Optional.of(true),
                    Optional.empty()));
        }

        return result;
    }

    public List<DbSchemaSchemataResponseRow> dbSchemaSchemata(DbSchemaSchemataRequest request, RequestMetaData metaData) {
        String catalogName = request.restrictions().catalogName();
        String schemaName = request.restrictions().schemaName();
        String schemaOwner = request.restrictions().schemaOwner();
        List<DbSchemaSchemataResponseRow> result = new ArrayList<>();
        if (catalogName != null) {
            Optional<Catalog> oCatalog = contextsListSupplyer.tryGetFirstByName(catalogName, metaData.sessionId());
            if (oCatalog.isPresent()) {
                result.addAll(getDbSchemaSchemataResponseRow(oCatalog.get(), schemaName, schemaOwner));
            }
        } else {
            result.addAll(contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getDbSchemaSchemataResponseRow(c, schemaName, schemaOwner)).flatMap(Collection::stream)
                    .toList());
        }
        return result;
    }

    public List<DbSchemaSourceTablesResponseRow> dbSchemaSourceTables(DbSchemaSourceTablesRequest request,
            RequestMetaData metaData) {
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        String tableName = request.restrictions().tableName();
        TableTypeEnum tableType = request.restrictions().tableType();

        if (oCatalogName.isPresent()) {
            Optional<Catalog> oCatalog = oCatalogName
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oCatalog.isPresent()) {
                return getDbSchemaSourceTablesResponseRow(oCatalog.get(), List.of(tableType.getValue()));
            }
        } else {
            return contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getDbSchemaSourceTablesResponseRow(c, List.of(tableType.getValue())))
                    .flatMap(Collection::stream).toList();
        }
        return List.of();
    }

    public List<DbSchemaTablesResponseRow> dbSchemaTables(DbSchemaTablesRequest request, RequestMetaData metaData) {
        Optional<String> oTableCatalog = request.restrictions().tableCatalog();
        Optional<String> oTableSchema = request.restrictions().tableSchema();
        Optional<String> oTableName = request.restrictions().tableName();
        Optional<String> oTableType = request.restrictions().tableType();

        if (oTableCatalog.isEmpty()) {
            oTableCatalog = request.properties().catalog();
        }

        if (oTableCatalog.isPresent()) {
            Optional<Catalog> oCatalog = oTableCatalog
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oCatalog.isPresent()) {
                return Utils.getDbSchemaTablesResponseRow(oCatalog.get(), oTableSchema, oTableName, oTableType);
            }
        } else {
            return contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> Utils.getDbSchemaTablesResponseRow(c, oTableSchema, oTableName, oTableType))
                    .flatMap(Collection::stream).toList();
        }
        return List.of();
    }

    public List<DbSchemaTablesInfoResponseRow> dbSchemaTablesInfo(DbSchemaTablesInfoRequest request,
            RequestMetaData metaData) {
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        String tableName = request.restrictions().tableName();
        TableTypeEnum tableType = request.restrictions().tableType();
        if (oCatalogName.isPresent()) {
            Optional<Catalog> oCatalog = oCatalogName
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oCatalog.isPresent()) {
                return getDbSchemaTablesInfoResponseRow(oCatalog.get(), oSchemaName, tableName, tableType);
            }
        } else {
            return contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getDbSchemaTablesInfoResponseRow(c, oSchemaName, tableName, tableType))
                    .flatMap(Collection::stream).toList();
        }
        return List.of();
    }

}
