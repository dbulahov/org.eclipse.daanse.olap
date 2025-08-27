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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.ContextGroup;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.DatabaseSchema;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.xmla.bridge.ContextsSupplyerImpl;
import org.eclipse.daanse.xmla.api.RequestMetaData;
import org.eclipse.daanse.xmla.api.common.enums.LevelDbTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.TableTypeEnum;
import org.eclipse.daanse.xmla.api.discover.dbschema.catalogs.DbSchemaCatalogsRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.catalogs.DbSchemaCatalogsResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.catalogs.DbSchemaCatalogsRestrictions;
import org.eclipse.daanse.xmla.api.discover.dbschema.columns.DbSchemaColumnsRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.columns.DbSchemaColumnsResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.columns.DbSchemaColumnsRestrictions;
import org.eclipse.daanse.xmla.api.discover.dbschema.providertypes.DbSchemaProviderTypesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.providertypes.DbSchemaProviderTypesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.providertypes.DbSchemaProviderTypesRestrictions;
import org.eclipse.daanse.xmla.api.discover.dbschema.schemata.DbSchemaSchemataRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.schemata.DbSchemaSchemataResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.schemata.DbSchemaSchemataRestrictions;
import org.eclipse.daanse.xmla.api.discover.dbschema.sourcetables.DbSchemaSourceTablesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.sourcetables.DbSchemaSourceTablesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.sourcetables.DbSchemaSourceTablesRestrictions;
import org.eclipse.daanse.xmla.api.discover.dbschema.tables.DbSchemaTablesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.tables.DbSchemaTablesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.tables.DbSchemaTablesRestrictions;
import org.eclipse.daanse.xmla.api.discover.dbschema.tablesinfo.DbSchemaTablesInfoRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.tablesinfo.DbSchemaTablesInfoResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.tablesinfo.DbSchemaTablesInfoRestrictions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class DbSchemaDiscoverServiceTest {

    @Mock
    private Context context;

    @Mock
    private Catalog catalog;

    @Mock
    private Cube cub1;

    @Mock
    private Cube cub2;

    @Mock
    private Member member;

    @Mock
    private Dimension dim1;

    @Mock
    private Dimension dim2;

    @Mock
    private DatabaseSchema dbSchema1;

    @Mock
    private DatabaseSchema dbSchema2;

    @Mock
    private Hierarchy hierar1;

    @Mock
    private Hierarchy hierar2;

    @Mock
    private DataSource dataSource;

    @Mock
    private ContextGroup contextGroup;

    @Mock
    private RequestMetaData requestMetaData;

    private DBSchemaDiscoverService service;

    @Mock
    private ContextsSupplyerImpl cls;

    @BeforeEach
    void setup() {
        /*
         * empty list, but override with: when(cls.get()).thenReturn(List.of(context1,context2));`
         */
        service = new DBSchemaDiscoverService(cls);
    }

    @Test
    void dbSchemaCatalogs() {
        when(cls.getContext(any())).thenReturn(Optional.of(context));

        DbSchemaCatalogsRequest request = mock(DbSchemaCatalogsRequest.class);
        DbSchemaCatalogsRestrictions restrictions = mock(DbSchemaCatalogsRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));

        when(context.getName()).thenReturn("foo");
        when(context.getDescription()).thenReturn(Optional.of("schema2Description"));

        when(context.getAccessRoles()).thenAnswer(setupDummyListAnswer("role1", "role2"));

        List<DbSchemaCatalogsResponseRow> rows = service.dbSchemaCatalogs(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(1);
        DbSchemaCatalogsResponseRow row = rows.get(0);
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains("foo");
        assertThat(row.description()).contains("schema2Description");
        assertThat(row.roles()).contains("role1,role2");
    }

    @Test
    void dbSchemaColumns() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));
        DbSchemaColumnsRequest request = mock(DbSchemaColumnsRequest.class);
        DbSchemaColumnsRestrictions restrictions = mock(DbSchemaColumnsRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.tableCatalog()).thenReturn(Optional.of("foo"));

        when(catalog.getName()).thenReturn("schema2Name");

        when(hierar1.getName()).thenReturn("hierarchy1Name");
        when(hierar2.getName()).thenReturn("hierarchy2Name");

        when(dim1.getHierarchies()).thenAnswer(setupDummyListAnswer(hierar1, hierar2));

        when(member.getName()).thenReturn("measureName");

        when(cub1.getName()).thenReturn("cube1Name");

        when(cub1.getDimensions()).thenAnswer(setupDummyListAnswer(dim1));
        when(cub2.getDimensions()).thenAnswer(setupDummyListAnswer(dim1));

        when(cub2.getName()).thenReturn("cube2Name");

        when(cub1.getMeasures()).thenAnswer(setupDummyListAnswer(member));
        when(cub2.getMeasures()).thenAnswer(setupDummyListAnswer(member));

        when(catalog.getCubes()).thenAnswer(setupDummyListAnswer(cub1, cub2));

        List<DbSchemaColumnsResponseRow> rows = service.dbSchemaColumns(request, requestMetaData);
        verify(catalog, times(2)).getName();
        assertThat(rows).isNotNull().hasSize(10);
        DbSchemaColumnsResponseRow row = rows.get(0);
        assertThat(row).isNotNull();
        assertThat(row.tableCatalog()).contains("schema2Name");
        assertThat(row.tableSchema()).isEmpty();

        assertThat(rows).extracting(DbSchemaColumnsResponseRow::columnName)
                .contains(Optional.of("hierarchy1Name:(All)!NAME"))
                .contains(Optional.of("hierarchy1Name:(All)!UNIQUE_NAME"))
                .contains(Optional.of("hierarchy2Name:(All)!NAME"))
                .contains(Optional.of("hierarchy2Name:(All)!UNIQUE_NAME"))
                .contains(Optional.of("Measures:measureName"));
    }

    @Test
    void dbSchemaProviderTypes() {

        DbSchemaProviderTypesRequest request = mock(DbSchemaProviderTypesRequest.class);
        DbSchemaProviderTypesRestrictions restrictions = mock(DbSchemaProviderTypesRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.dataType()).thenReturn(Optional.empty());

        List<DbSchemaProviderTypesResponseRow> rows = service.dbSchemaProviderTypes(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(6);
        DbSchemaProviderTypesResponseRow row = rows.get(0);
        assertThat(row).isNotNull();
        assertThat(row.typeName()).contains("INTEGER");
        assertThat(row.dataType()).contains(LevelDbTypeEnum.DBTYPE_I4);
        assertThat(row.columnSize()).contains(8);
        assertThat(row.isNullable()).contains(true);
        assertThat(row.unsignedAttribute()).contains(false);
        assertThat(row.fixedPrecScale()).contains(false);
        assertThat(row.autoUniqueValue()).contains(false);
        assertThat(row.isLong()).contains(false);
        assertThat(row.bestMatch()).contains(true);

        row = rows.get(1);
        assertThat(row).isNotNull();
        assertThat(row.typeName()).contains("DOUBLE");
        assertThat(row.dataType()).contains(LevelDbTypeEnum.DBTYPE_R8);
        assertThat(row.columnSize()).contains(16);
        assertThat(row.isNullable()).contains(true);
        assertThat(row.unsignedAttribute()).contains(false);
        assertThat(row.fixedPrecScale()).contains(false);
        assertThat(row.autoUniqueValue()).contains(false);
        assertThat(row.isLong()).contains(false);
        assertThat(row.bestMatch()).contains(true);

        row = rows.get(2);
        assertThat(row).isNotNull();
        assertThat(row.typeName()).contains("CURRENCY");
        assertThat(row.dataType()).contains(LevelDbTypeEnum.DBTYPE_CY);
        assertThat(row.columnSize()).contains(8);
        assertThat(row.isNullable()).contains(true);
        assertThat(row.unsignedAttribute()).contains(false);
        assertThat(row.fixedPrecScale()).contains(false);
        assertThat(row.autoUniqueValue()).contains(false);
        assertThat(row.isLong()).contains(false);
        assertThat(row.bestMatch()).contains(true);

        row = rows.get(3);
        assertThat(row).isNotNull();
        assertThat(row.typeName()).contains("BOOLEAN");
        assertThat(row.dataType()).contains(LevelDbTypeEnum.DBTYPE_BOOL);
        assertThat(row.columnSize()).contains(1);
        assertThat(row.isNullable()).contains(true);
        assertThat(row.unsignedAttribute()).contains(false);
        assertThat(row.fixedPrecScale()).contains(false);
        assertThat(row.autoUniqueValue()).contains(false);
        assertThat(row.isLong()).contains(false);
        assertThat(row.bestMatch()).contains(true);

        row = rows.get(4);
        assertThat(row).isNotNull();
        assertThat(row.typeName()).contains("LARGE_INTEGER");
        assertThat(row.dataType()).contains(LevelDbTypeEnum.DBTYPE_I8);
        assertThat(row.columnSize()).contains(16);
        assertThat(row.isNullable()).contains(true);
        assertThat(row.unsignedAttribute()).contains(false);
        assertThat(row.fixedPrecScale()).contains(false);
        assertThat(row.autoUniqueValue()).contains(false);
        assertThat(row.isLong()).contains(false);
        assertThat(row.bestMatch()).contains(true);

        row = rows.get(5);
        assertThat(row).isNotNull();
        assertThat(row.typeName()).contains("STRING");
        assertThat(row.dataType()).contains(LevelDbTypeEnum.DBTYPE_WSTR);
        assertThat(row.columnSize()).contains(255);
        assertThat(row.literalPrefix()).contains("\"");
        assertThat(row.literalSuffix()).contains("\"");
        assertThat(row.isNullable()).contains(true);
        assertThat(row.unsignedAttribute()).isEmpty();
        assertThat(row.fixedPrecScale()).contains(false);
        assertThat(row.autoUniqueValue()).contains(false);
        assertThat(row.isLong()).contains(false);
        assertThat(row.bestMatch()).contains(true);
    }

    @Test
    void dbSchemaSchemata() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));

        DbSchemaSchemataRequest request = mock(DbSchemaSchemataRequest.class);
        DbSchemaSchemataRestrictions restrictions = mock(DbSchemaSchemataRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn("foo");

        when(dbSchema1.getName()).thenReturn("dbSchema1Name");
        when(dbSchema2.getName()).thenReturn("dbSchema2Name");

        when(catalog.getName()).thenReturn("schema2Name");
        when(catalog.getDatabaseSchemas()).thenAnswer(setupDummyListAnswer(dbSchema1, dbSchema2));

        List<DbSchemaSchemataResponseRow> rows = service.dbSchemaSchemata(request, requestMetaData);

        assertThat(rows).isNotNull().hasSize(2);
        DbSchemaSchemataResponseRow row = rows.get(0);
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains("schema2Name");
        assertThat(row.schemaName()).contains("dbSchema1Name");

        row = rows.get(1);
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains("schema2Name");
        assertThat(row.schemaName()).contains("dbSchema2Name");

        assertThat(rows).extracting(DbSchemaSchemataResponseRow::schemaName).contains("dbSchema1Name")
                .contains("dbSchema2Name");
    }

    @Test
    void dbSchemaSourceTables() throws SQLException {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));

        DbSchemaSourceTablesRequest request = mock(DbSchemaSourceTablesRequest.class);
        DbSchemaSourceTablesRestrictions restrictions = mock(DbSchemaSourceTablesRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));
        when(restrictions.tableType()).thenReturn(TableTypeEnum.TABLE);

        List<DbSchemaSourceTablesResponseRow> rows = service.dbSchemaSourceTables(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(0);
        // TODO
        // assertThat(rows).isNotNull().hasSize(2);
        // DbSchemaSourceTablesResponseRow row = rows.get(0);
        // assertThat(row).isNotNull();
        // assertThat(row.catalogName()).contains("tableCatalog");
        // assertThat(row.schemaName()).contains("tableSchema");
        // assertThat(row.tableName()).isEqualTo("tableName");
        // assertThat(row.tableType()).isEqualTo(TableTypeEnum.TABLE);
    }

    @Test
    void dbSchemaTables() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));
        DbSchemaTablesRequest request = mock(DbSchemaTablesRequest.class);
        DbSchemaTablesRestrictions restrictions = mock(DbSchemaTablesRestrictions.class);
        Level level1 = mock(Level.class);
        Level level2 = mock(Level.class);
        when(level1.getName()).thenReturn("level1Name");
        when(level1.getDescription()).thenReturn("level1Description");
        when(level2.getName()).thenReturn("level2Name");

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.tableCatalog()).thenReturn(Optional.of("foo"));

        when(catalog.getName()).thenReturn("schema2Name");
        when(catalog.getCubes()).thenAnswer(setupDummyListAnswer(cub1, cub2));

        when(hierar1.getLevels()).thenAnswer(setupDummyListAnswer(level1, level2));
        when(hierar1.getName()).thenReturn("hierarchy1Name");

        when(dim1.getName()).thenReturn("dim1Name");
        when(dim1.getHierarchies()).thenAnswer(setupDummyListAnswer(hierar1, hierar2));
        when(dim2.getName()).thenReturn("dim2Name");
        when(dim2.getHierarchies()).thenAnswer(setupDummyListAnswer(hierar1, hierar2));

        when(cub1.getName()).thenReturn("cube1Name");
        when(cub1.getDimensions()).thenAnswer(setupDummyListAnswer(dim1, dim2));

        when(cub2.getName()).thenReturn("cube2Name");
        when(cub2.getDimensions()).thenAnswer(setupDummyListAnswer(dim1, dim2));

        List<DbSchemaTablesResponseRow> rows = service.dbSchemaTables(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(10);
        checkDbSchemaTablesResponseRow(rows.get(0), "schema2Name", "cube1Name", "TABLE",
                "schema2Name - cube1Name Cube");
        checkDbSchemaTablesResponseRow(rows.get(1), "schema2Name", "cube1Name:dim1Name.hierarchy1Name:level1Name",
                "SYSTEM TABLE", "level1Description");
        checkDbSchemaTablesResponseRow(rows.get(2), "schema2Name", "cube1Name:dim1Name.hierarchy1Name:level2Name",
                "SYSTEM TABLE", "schema2Name - cube1Name Cube - dim1Name.hierarchy1Name Hierarchy - level2Name Level");
        checkDbSchemaTablesResponseRow(rows.get(3), "schema2Name", "cube1Name:dim2Name.hierarchy1Name:level1Name",
                "SYSTEM TABLE", "level1Description");
        checkDbSchemaTablesResponseRow(rows.get(4), "schema2Name", "cube1Name:dim2Name.hierarchy1Name:level2Name",
                "SYSTEM TABLE", "schema2Name - cube1Name Cube - dim2Name.hierarchy1Name Hierarchy - level2Name Level");
    }

    @Test
    void dbSchemaTablesWithDimensionUsage() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));

        DbSchemaTablesRequest request = mock(DbSchemaTablesRequest.class);
        DbSchemaTablesRestrictions restrictions = mock(DbSchemaTablesRestrictions.class);

        Level level1 = mock(Level.class);
        Level level2 = mock(Level.class);
        when(level1.getName()).thenReturn("level1Name");
        when(level1.getDescription()).thenReturn("level1Description");
        when(level2.getName()).thenReturn("level2Name");

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.tableCatalog()).thenReturn(Optional.of("foo"));

        when(catalog.getName()).thenReturn("schema2Name");
        when(catalog.getCubes()).thenAnswer(setupDummyListAnswer(cub1, cub2));

        when(hierar1.getLevels()).thenAnswer(setupDummyListAnswer(level1, level2));
        when(hierar1.getName()).thenReturn("hierarchy1Name");

        when(dim1.getHierarchies()).thenAnswer(setupDummyListAnswer(hierar1, hierar2));
        when(dim1.getName()).thenReturn("dimension1Name");
        when(dim2.getName()).thenReturn("dimension2Name");
        when(dim2.getHierarchies()).thenAnswer(setupDummyListAnswer(hierar1));

        when(cub1.getName()).thenReturn("cube1Name");
        when(cub1.getDimensions()).thenAnswer(setupDummyListAnswer(dim1, dim2));

        when(cub2.getName()).thenReturn("cube2Name");
        when(cub2.getDimensions()).thenAnswer(setupDummyListAnswer(dim1, dim2));

        List<DbSchemaTablesResponseRow> rows = service.dbSchemaTables(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(10);
        checkDbSchemaTablesResponseRow(rows.get(0), "schema2Name", "cube1Name", "TABLE",
                "schema2Name - cube1Name Cube");
        checkDbSchemaTablesResponseRow(rows.get(1), "schema2Name", "cube1Name:dimension1Name.hierarchy1Name:level1Name",
                "SYSTEM TABLE", "level1Description");
        checkDbSchemaTablesResponseRow(rows.get(2), "schema2Name", "cube1Name:dimension1Name.hierarchy1Name:level2Name",
                "SYSTEM TABLE",
                "schema2Name - cube1Name Cube - dimension1Name.hierarchy1Name Hierarchy - level2Name Level");
        checkDbSchemaTablesResponseRow(rows.get(3), "schema2Name", "cube1Name:dimension2Name.hierarchy1Name:level1Name",
                "SYSTEM TABLE", "level1Description");
        checkDbSchemaTablesResponseRow(rows.get(4), "schema2Name", "cube1Name:dimension2Name.hierarchy1Name:level2Name",
                "SYSTEM TABLE",
                "schema2Name - cube1Name Cube - dimension2Name.hierarchy1Name Hierarchy - level2Name Level");
    }

    @Test
    void dbSchemaTablesWithRestriction() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));

        DbSchemaTablesRequest request = mock(DbSchemaTablesRequest.class);
        DbSchemaTablesRestrictions restrictions = mock(DbSchemaTablesRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.tableCatalog()).thenReturn(Optional.of("foo"));
        when(restrictions.tableType()).thenReturn(Optional.of("TABLE"));

        when(catalog.getName()).thenReturn("schema2Name");
        when(catalog.getCubes()).thenAnswer(setupDummyListAnswer(cub1, cub2));

        when(cub1.getName()).thenReturn("cube1Name");

        when(cub2.getName()).thenReturn("cube2Name");

        List<DbSchemaTablesResponseRow> rows = service.dbSchemaTables(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(2);
        checkDbSchemaTablesResponseRow(rows.get(0), "schema2Name", "cube1Name", "TABLE",
                "schema2Name - cube1Name Cube");
        checkDbSchemaTablesResponseRow(rows.get(1), "schema2Name", "cube2Name", "TABLE",
                "schema2Name - cube2Name Cube");
    }

    @Test
    void dbSchemaTablesInfo() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));

        DbSchemaTablesInfoRequest request = mock(DbSchemaTablesInfoRequest.class);
        DbSchemaTablesInfoRestrictions restrictions = mock(DbSchemaTablesInfoRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));

        when(catalog.getName()).thenReturn("schema2Name");
        when(catalog.getCubes()).thenAnswer(setupDummyListAnswer(cub1, cub2));

        when(cub1.getName()).thenReturn("cube1Name");

        when(cub2.getName()).thenReturn("cube2Name");
        List<DbSchemaTablesInfoResponseRow> rows = service.dbSchemaTablesInfo(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(2);
        checkDbSchemaTablesInfoResponseRow(rows.get(0), "schema2Name", "cube1Name", false, TableTypeEnum.TABLE.name(),
                1000000l, "schema2Name - cube1Name Cube");
        checkDbSchemaTablesInfoResponseRow(rows.get(1), "schema2Name", "cube2Name", false, TableTypeEnum.TABLE.name(),
                1000000l, "schema2Name - cube2Name Cube");
    }

    private void checkDbSchemaTablesInfoResponseRow(DbSchemaTablesInfoResponseRow row, String catalogName,
            String tableName, boolean bookmarks, String tableType, long cardinality, String description) {
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains(catalogName);
        assertThat(row.schemaName()).isEmpty();
        assertThat(row.tableName()).contains(tableName);
        assertThat(row.bookmarks()).contains(bookmarks);
        assertThat(row.tableType()).contains(tableType);
        assertThat(row.cardinality()).contains(cardinality);
        assertThat(row.description()).contains(description);
    }

    private void checkDbSchemaTablesResponseRow(DbSchemaTablesResponseRow row, String tableCatalog, String tableName,
            String tableType, String description) {
        assertThat(row).isNotNull();
        assertThat(row.tableCatalog()).contains(tableCatalog);
        assertThat(row.tableSchema()).isEmpty();
        assertThat(row.tableName()).contains(tableName);
        assertThat(row.tableType()).contains(tableType);
        assertThat(row.tableGuid()).isEmpty();
        assertThat(row.description()).contains(description);
        assertThat(row.tablePropId()).isEmpty();
        assertThat(row.dateCreated()).isEmpty();
        assertThat(row.dateModified()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static <N> Answer<List<N>> setupDummyListAnswer(N... values) {
        final List<N> someList = new ArrayList<>(Arrays.asList(values));

        Answer<List<N>> answer = new Answer<>() {
            @Override
            public List<N> answer(InvocationOnMock invocation) throws Throwable {
                return someList;
            }
        };
        return answer;
    }

    @SuppressWarnings("unchecked")
    private static <N> Answer<N[]> setupDummyArrayAnswer(N... values) {

        Answer<N[]> answer = new Answer<>() {
            @Override
            public N[] answer(InvocationOnMock invocation) throws Throwable {
                return values;
            }
        };
        return answer;
    }

}
