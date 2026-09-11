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
package org.eclipse.daanse.olap.xmla.connector.embedded;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.db.DatabaseColumn;
import org.eclipse.daanse.olap.api.element.db.DatabaseTable;
import org.eclipse.daanse.olap.xmla.connector.ContextListSupplyer;
import org.eclipse.daanse.xmla.api.RowsetProvider;
import org.eclipse.daanse.xmla.api.RowsetScope;
import org.eclipse.daanse.xmla.api.XmlaRequest;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaCatalogsRow;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaColumnsRow;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaProviderTypesRow;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaSchemataRow;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaTablesInfoRow;
import org.eclipse.daanse.xmla.model.rowset.relational.DbschemaTablesRow;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * The bridge's {@code DbSchemaDiscoverServiceTest}, ported onto the connector:
 * the same OLAP mocks, the requests as model {@code Discover} objects, the
 * asserts on the typed EMF rows.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DbSchemaDiscoverTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context<?> context;
    @Mock
    private Catalog catalog;
    @Mock
    private Cube cube1;
    @Mock
    private Cube cube2;
    @Mock
    private Member measure;
    @Mock
    private Dimension dimension1;
    @Mock
    private Dimension dimension2;
    @Mock
    private org.eclipse.daanse.olap.api.element.db.DatabaseSchema dbSchema1;
    @Mock
    private org.eclipse.daanse.olap.api.element.db.DatabaseSchema dbSchema2;
    @Mock
    private Hierarchy hierarchy1;
    @Mock
    private Hierarchy hierarchy2;
    @Mock
    private Level level1;
    @Mock
    private Level level2;
    @Mock
    private ContextListSupplyer contexts;

    @Mock
    private Connection connection;
    @Mock
    private CatalogReader reader;
    @Mock
    private DatabaseTable factTable;
    @Mock
    private DatabaseTable secretTable;
    @Mock
    private DatabaseColumn keyColumn;
    
    private final XmlaRequest anonymous = XmlaRequest.anonymous();

    @BeforeEach
    void setup() {
    }

    /**
     * One rowset through its provider - the whiteboard's dispatch, minus OSGi: the
     * provider for this request type, called with the scope the connector would
     * have built.
     */
    private List<EObject> discover(String requestType, Map<String, String> restrictions) {
        RowsetProvider<ContextListSupplyer> provider = Providers.of(requestType);
        return provider.rows(
                RowsetScope.of(Requests.discover(requestType, restrictions), anonymous, contexts, Providers.served()));
    }

    /** A caller who holds roles, the way CatalogScopeTest builds one. */
    private static XmlaRequest named(String user, Set<String> roles) {
        Principal principal = () -> user;
        return new XmlaRequest(principal, roles, Map.of(), "http://localhost/xmla", "127.0.0.1");
    }

    /** The element knows two tables in schema main; the caller's reader admits one. */
    private void aCatalogWhoseReaderHidesTheSecretTable() {
        when(contexts.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));
        when(contexts.get(any())).thenAnswer(i -> List.of(catalog));
        when(catalog.getName()).thenReturn("FoodMart");
        when(dbSchema1.getName()).thenReturn("main");
        when(factTable.getName()).thenReturn("Fact");
        when(secretTable.getName()).thenReturn("Salary");
        when(keyColumn.getName()).thenReturn("KEY");
        when(factTable.getDbColumns()).thenAnswer(i -> List.of(keyColumn));
        when(secretTable.getDbColumns()).thenAnswer(i -> List.of(keyColumn));
        when(dbSchema1.getDbTables()).thenAnswer(i -> List.of(factTable, secretTable));
        when(catalog.getDatabaseSchemas()).thenAnswer(i -> List.of(dbSchema1));

        // what the role sees: the same schema, one table
        when(dbSchema2.getName()).thenReturn("main");
        when(dbSchema2.getDbTables()).thenAnswer(i -> List.of(factTable));
        when(reader.getDatabaseSchemas()).thenAnswer(i -> List.of(dbSchema2));
        when(connection.getCatalogReader()).thenReturn(reader);
        when(contexts.getConnection(any(), eq("FoodMart"))).thenReturn(connection);
    }

    private List<EObject> discover(String requestType, Map<String, String> restrictions, XmlaRequest caller) {
        RowsetProvider<ContextListSupplyer> provider = Providers.of(requestType);
        return provider.rows(
                RowsetScope.of(Requests.discover(requestType, restrictions), caller, contexts, Providers.served()));
    }
    
    @Test
    void dbSchemaCatalogs() {
        when(contexts.getContexts()).thenAnswer(invocation -> List.of(context));
        when(context.getName()).thenReturn("foo");
        when(context.getDescription()).thenReturn(Optional.of("schema2Description"));
        when(context.getAccessRoles()).thenReturn(List.of("role1", "role2"));

        List<EObject> rows = discover("DBSCHEMA_CATALOGS", Map.of("CATALOG_NAME", "foo"));
        assertThat(rows).hasSize(1);
        DbschemaCatalogsRow row = (DbschemaCatalogsRow) rows.get(0);
        assertThat(row.getCatalogName()).isEqualTo("foo");
        assertThat(row.getDescription()).isEqualTo("schema2Description");
        assertThat(row.getRoles()).isEqualTo("role1,role2");
    }

    /**
     * The two numbers of DBSCHEMA_CATALOGS are always written, never left out.
     * <p>
     * A client listing databases asks for COMPATIBILITY_LEVEL and reads it into a
     * value type. A column omitted from the row arrives there as null and throws
     * before one catalog is shown; declaring it in the inline schema does not help,
     * the row has to carry a value.
     */
    @Test
    void dbSchemaCatalogsAlwaysCarriesItsNumbers() {
        when(contexts.getContexts()).thenAnswer(invocation -> List.of(context));
        when(context.getName()).thenReturn("foo");
        when(context.getDescription()).thenReturn(Optional.empty());
        when(context.getAccessRoles()).thenReturn(List.of());

        DbschemaCatalogsRow row = (DbschemaCatalogsRow) discover("DBSCHEMA_CATALOGS", Map.of("CATALOG_NAME", "foo"))
                .get(0);

        assertThat(row.getCompatibilityLevel()).as("null here stops a client from listing any database")
                .isEqualTo(1100);
        assertThat(row.getType()).as("no bit set means a multidimensional database").isEqualTo(0);
        assertThat(row.getDateModified()).isNotNull();
    }

    @Test
    void dbSchemaColumns() {
        when(contexts.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));
        when(catalog.getName()).thenReturn("schema2Name");
        when(hierarchy1.getName()).thenReturn("hierarchy1Name");
        when(hierarchy2.getName()).thenReturn("hierarchy2Name");
        when(dimension1.getHierarchies()).thenAnswer(invocation -> List.of(hierarchy1, hierarchy2));
        when(measure.getName()).thenReturn("measureName");
        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");
        when(cube1.getDimensions()).thenAnswer(invocation -> List.of(dimension1));
        when(cube2.getDimensions()).thenAnswer(invocation -> List.of(dimension1));
        when(cube1.getMeasures()).thenAnswer(invocation -> List.of(measure));
        when(cube2.getMeasures()).thenAnswer(invocation -> List.of(measure));
        when(catalog.getCubes()).thenAnswer(invocation -> List.of(cube1, cube2));
        when(contexts.getConnection(any(), any())).thenReturn(connection);
        when(connection.getCatalogReader()).thenReturn(reader);
        when(reader.getDatabaseSchemas()).thenAnswer(invocation -> List.of(dbSchema1));

        List<EObject> rows = discover("DBSCHEMA_COLUMNS", Map.of("TABLE_CATALOG", "foo"));
        assertThat(rows).hasSize(10);
        DbschemaColumnsRow first = (DbschemaColumnsRow) rows.get(0);
        assertThat(first.getTableCatalog()).isEqualTo("schema2Name");
        assertThat(first.getTableSchema()).isNull();
        assertThat(rows).extracting(row -> ((DbschemaColumnsRow) row).getColumnName()).contains(
                "hierarchy1Name:(All)!NAME", "hierarchy1Name:(All)!UNIQUE_NAME", "hierarchy2Name:(All)!NAME",
                "hierarchy2Name:(All)!UNIQUE_NAME", "Measures:measureName");
    }

    @Test
    void dbSchemaProviderTypes() {
        List<EObject> rows = discover("DBSCHEMA_PROVIDER_TYPES", Map.of());
        assertThat(rows).hasSize(6);
        assertThat(rows).extracting(row -> ((DbschemaProviderTypesRow) row).getTypeName()).containsExactly("INTEGER",
                "DOUBLE", "CURRENCY", "BOOLEAN", "LARGE_INTEGER", "STRING");
        DbschemaProviderTypesRow integer = (DbschemaProviderTypesRow) rows.get(0);
        assertThat(integer.getDataType()).isEqualTo(3);
        assertThat(integer.getColumnSize()).isEqualTo(8L);
        assertThat(integer.getIsNullable()).isTrue();
        assertThat(integer.getUnsignedAttribute()).isFalse();
        assertThat(integer.getBestMatch()).isTrue();
        DbschemaProviderTypesRow string = (DbschemaProviderTypesRow) rows.get(5);
        assertThat(string.getDataType()).isEqualTo(130);
        assertThat(string.getColumnSize()).isEqualTo(255L);
        assertThat(string.getLiteralPrefix()).isEqualTo("\"");
        assertThat(string.getLiteralSuffix()).isEqualTo("\"");
        assertThat(string.getCaseSensitive()).isFalse();
        assertThat(string.getUnsignedAttribute()).isNull();
    }

    @Test
    void dbSchemaProviderTypesFiltered() {
        List<EObject> rows = discover("DBSCHEMA_PROVIDER_TYPES", Map.of("DATA_TYPE", "5"));
        assertThat(rows).hasSize(1);
        assertThat(((DbschemaProviderTypesRow) rows.get(0)).getTypeName()).isEqualTo("DOUBLE");
    }

    @Test
    void dbSchemaSchemata() {
        when(contexts.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));
        when(catalog.getName()).thenReturn("schema2Name");
        when(dbSchema1.getName()).thenReturn("dbSchema1Name");
        when(dbSchema2.getName()).thenReturn("dbSchema2Name");
        when(catalog.getDatabaseSchemas()).thenAnswer(invocation -> List.of(dbSchema1, dbSchema2));
        when(contexts.getConnection(any(), any())).thenReturn(connection);
        when(connection.getCatalogReader()).thenReturn(reader);
        when(reader.getDatabaseSchemas()).thenAnswer(invocation -> List.of(dbSchema1, dbSchema2));

        List<EObject> rows = discover("DBSCHEMA_SCHEMATA", Map.of("CATALOG_NAME", "foo"));
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> ((DbschemaSchemataRow) row).getSchemaName()).containsExactly("dbSchema1Name",
                "dbSchema2Name");
        assertThat(((DbschemaSchemataRow) rows.get(0)).getCatalogName()).isEqualTo("schema2Name");
    }

    @Test
    void dbSchemaTables() {
        when(contexts.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));
        when(catalog.getName()).thenReturn("schema2Name");
        when(catalog.getCubes()).thenAnswer(invocation -> List.of(cube1, cube2));
        when(level1.getName()).thenReturn("level1Name");
        when(level1.getDescription()).thenReturn("level1Description");
        when(level2.getName()).thenReturn("level2Name");
        when(hierarchy1.getLevels()).thenAnswer(invocation -> List.of(level1, level2));
        when(hierarchy1.getName()).thenReturn("hierarchy1Name");
        when(dimension1.getName()).thenReturn("dim1Name");
        when(dimension1.getHierarchies()).thenAnswer(invocation -> List.of(hierarchy1, hierarchy2));
        when(dimension2.getName()).thenReturn("dim2Name");
        when(dimension2.getHierarchies()).thenAnswer(invocation -> List.of(hierarchy1, hierarchy2));
        when(cube1.getName()).thenReturn("cube1Name");
        when(cube1.getDimensions()).thenAnswer(invocation -> List.of(dimension1, dimension2));
        when(cube2.getName()).thenReturn("cube2Name");
        when(cube2.getDimensions()).thenAnswer(invocation -> List.of(dimension1, dimension2));
        when(contexts.getConnection(any(), any())).thenReturn(connection);
        when(connection.getCatalogReader()).thenReturn(reader);
        when(reader.getDatabaseSchemas()).thenAnswer(invocation -> List.of(dbSchema1, dbSchema2));

        List<EObject> rows = discover("DBSCHEMA_TABLES", Map.of("TABLE_CATALOG", "foo"));
        // Two cubes, each one TABLE row plus two level rows per dimension (hierarchy2
        // declares no levels and is skipped), exactly as the bridge answered.
        assertThat(rows).hasSize(10);
        assertThat(rows).extracting(row -> ((DbschemaTablesRow) row).getTableType()).contains("TABLE", "SYSTEM TABLE");
        // The level tables carry the dimension-qualified hierarchy name, following the
        // dimension.hierarchy convention.
        assertThat(rows).extracting(row -> ((DbschemaTablesRow) row).getTableName()).contains("cube1Name",
                "cube1Name:dim1Name.hierarchy1Name:level1Name", "cube1Name:dim2Name.hierarchy1Name:level2Name",
                "cube2Name");
    }

    @Test
    void dbSchemaTablesInfo() {
        when(contexts.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));
        when(catalog.getName()).thenReturn("schema2Name");
        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");
        when(catalog.getCubes()).thenAnswer(invocation -> List.of(cube1, cube2));

        List<EObject> rows = discover("DBSCHEMA_TABLES_INFO", Map.of("TABLE_CATALOG", "foo"));
        assertThat(rows).hasSize(2);
        DbschemaTablesInfoRow row = (DbschemaTablesInfoRow) rows.get(0);
        assertThat(row.getTableName()).isEqualTo("cube1Name");
        assertThat(row.getTableType()).isEqualTo("TABLE");
        assertThat(row.getCardinality()).isEqualTo(BigInteger.valueOf(1_000_000L));
        assertThat(row.getDescription()).isEqualTo("schema2Name - cube1Name Cube");
    }

    @Test
    void dbSchemaSourceTablesAnswersEmpty() {
        when(contexts.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));
        assertThat(discover("DBSCHEMA_SOURCE_TABLES", Map.of("TABLE_CATALOG", "foo"))).isEmpty();
    }

    /** One cube with one level — the smallest shape that has both kinds of row. */
    private void oneCubeOneLevel() {
        when(contexts.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));
        when(catalog.getName()).thenReturn("cat");
        when(catalog.getCubes()).thenAnswer(invocation -> List.of(cube1));
        when(cube1.getName()).thenReturn("cube1Name");
        when(cube1.getDimensions()).thenAnswer(invocation -> List.of(dimension1));
        when(dimension1.getName()).thenReturn("dim1Name");
        when(dimension1.getHierarchies()).thenAnswer(invocation -> List.of(hierarchy1));
        when(hierarchy1.getName()).thenReturn("hierarchy1Name");
        when(hierarchy1.getLevels()).thenAnswer(invocation -> List.of(level1));
        when(level1.getName()).thenReturn("level1Name");
    }

    /**
     * The column that says what a row really is. Without it the two kinds cannot be
     * told apart, because [MS-SSAS] gives a measure group the same
     * {@code TABLE_TYPE} as a table.
     */
    @Test
    void cubeRowsCarryTheirOlapType() {
        oneCubeOneLevel();
        when(contexts.getConnection(any(), any())).thenReturn(connection);
        when(connection.getCatalogReader()).thenReturn(reader);
        when(reader.getDatabaseSchemas()).thenAnswer(invocation -> List.of(dbSchema1, dbSchema2));

        List<EObject> rows = discover("DBSCHEMA_TABLES", Map.of("TABLE_CATALOG", "cat"));

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> ((DbschemaTablesRow) row).getTableOlapType())
                .containsExactlyInAnyOrder("MEASURE_GROUP", "CUBE_DIMENSION");
    }

    @Test
    void theOlapTypeRestrictionSelectsOneKind() {
        oneCubeOneLevel();

        List<EObject> rows = discover("DBSCHEMA_TABLES",
                Map.of("TABLE_CATALOG", "cat", "TABLE_OLAP_TYPE", "MEASURE_GROUP"));

        assertThat(rows).hasSize(1);
        assertThat(((DbschemaTablesRow) rows.get(0)).getTableName()).isEqualTo("cube1Name");
    }

    @Test
    void theNameRestrictionIsHonoured() {
        oneCubeOneLevel();
        when(contexts.getConnection(any(), any())).thenReturn(connection);
        when(connection.getCatalogReader()).thenReturn(reader);
        when(reader.getDatabaseSchemas()).thenAnswer(invocation -> List.of(dbSchema1, dbSchema2));

        assertThat(discover("DBSCHEMA_TABLES", Map.of("TABLE_CATALOG", "cat", "TABLE_NAME", "cube1Name"))).hasSize(1);
        assertThat(discover("DBSCHEMA_TABLES", Map.of("TABLE_CATALOG", "cat", "TABLE_NAME", "nothing"))).isEmpty();
    }

    /**
     * The point of the rowset for a relational catalog: tables exist without a
     * cube, and a client has to be able to find them.
     */
    @Test
    void aCatalogWithoutCubesStillAnswersItsTables() {
        org.eclipse.daanse.olap.api.element.db.DatabaseTable table = org.mockito.Mockito
                .mock(org.eclipse.daanse.olap.api.element.db.DatabaseTable.class);
        when(table.getName()).thenReturn("SALES_FACT");
        when(dbSchema1.getName()).thenReturn("public");
        when(dbSchema1.getDbTables()).thenAnswer(invocation -> List.of(table));
        when(contexts.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog));
        when(catalog.getName()).thenReturn("cat");
        when(catalog.getCubes()).thenAnswer(invocation -> List.of());
        when(catalog.getDatabaseSchemas()).thenAnswer(invocation -> List.of(dbSchema1));
        when(contexts.getConnection(any(), any())).thenReturn(connection);
        when(connection.getCatalogReader()).thenReturn(reader);
        when(reader.getDatabaseSchemas()).thenAnswer(invocation -> List.of(dbSchema1));
        List<EObject> rows = discover("DBSCHEMA_TABLES", Map.of("TABLE_CATALOG", "cat"));

        assertThat(rows).hasSize(1);
        DbschemaTablesRow row = (DbschemaTablesRow) rows.get(0);
        assertThat(row.getTableName()).isEqualTo("SALES_FACT");
        assertThat(row.getTableSchema()).isEqualTo("public");
        // TABLE, not SCHEMA: [MS-SSAS] gives SCHEMA to a schema rowset, a $SYSTEM
        // table, and not to a table a client can query.
        assertThat(row.getTableType()).isEqualTo("TABLE");
        // The absence is the signal: this row is a table, not a cube object.
        assertThat(row.getTableOlapType()).isNull();
    }
    
    
    @Test
    void dbSchemaTablesListsOnlyWhatTheCallersReaderAdmits() {
        aCatalogWhoseReaderHidesTheSecretTable();
        XmlaRequest caller = named("tester", Set.of("roleCustom"));

        List<EObject> rows = discover("DBSCHEMA_TABLES", Map.of("TABLE_CATALOG", "FoodMart"), caller);

        assertThat(rows).extracting(row -> ((DbschemaTablesRow) row).getTableName()).containsExactly("Fact");
        verify(contexts).getConnection(caller, "FoodMart");
    }

    @Test
    void dbSchemaColumnsListsOnlyTheColumnsOfAdmittedTables() {
        aCatalogWhoseReaderHidesTheSecretTable();

        List<EObject> rows = discover("DBSCHEMA_COLUMNS", Map.of("TABLE_CATALOG", "FoodMart"),
                named("tester", Set.of("roleCustom")));

        assertThat(rows).extracting(row -> ((DbschemaColumnsRow) row).getTableName()).containsOnly("Fact");
    }

    @Test
    void dbSchemaSchemataAnswersFromTheReaderToo() {
        aCatalogWhoseReaderHidesTheSecretTable();
        when(reader.getDatabaseSchemas()).thenAnswer(i -> List.of()); // the role sees no schema at all

        List<EObject> rows = discover("DBSCHEMA_SCHEMATA", Map.of("CATALOG_NAME", "FoodMart"),
                named("tester", Set.of("roleNone")));

        assertThat(rows).extracting(row -> ((DbschemaSchemataRow) row).getSchemaName()).doesNotContain("main");
    }
/*
    @Test
    void dbSchemaPrimaryKeysSkipsTablesTheCallerMayNotSee() {
        aCatalogWhoseReaderHidesTheSecretTable();
        org.eclipse.daanse.olap.api.element.db.DatabaseKey key = mock(org.eclipse.daanse.olap.api.element.db.DatabaseKey.class);
        when(key.getName()).thenReturn("PK_Salary");
        when(key.getColumns()).thenAnswer(i -> List.of(keyColumn));
        when(secretTable.getPrimaryKey()).thenReturn(Optional.of(key));
        when(factTable.getPrimaryKey()).thenReturn(Optional.empty());

        List<EObject> rows = discover("DBSCHEMA_PRIMARY_KEYS", Map.of("TABLE_CATALOG", "FoodMart"),
                named("tester", Set.of("roleCustom")));

        assertThat(rows).isEmpty();
    }
*/
    @Test
    void theElementStillAnswersEverythingWhenTheReaderIsUnfiltered() {
        aCatalogWhoseReaderHidesTheSecretTable();
        when(reader.getDatabaseSchemas()).thenAnswer(i -> List.of(dbSchema1)); // an ALL role: the element's own list

        List<EObject> rows = discover("DBSCHEMA_TABLES", Map.of("TABLE_CATALOG", "FoodMart"), anonymous);

        assertThat(rows).extracting(row -> ((DbschemaTablesRow) row).getTableName()).containsExactly("Fact", "Salary");
    }
}
