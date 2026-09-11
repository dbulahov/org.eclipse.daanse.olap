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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.eclipse.daanse.olap.xmla.connector.ContextListSupplyer;
import org.eclipse.daanse.xmla.api.RowsetProvider;
import org.eclipse.daanse.xmla.api.RowsetScope;
import org.eclipse.daanse.xmla.api.XmlaRequest;
import org.eclipse.daanse.xmla.model.rowset.core.DiscoverDatasourcesRow;
import org.eclipse.daanse.xmla.model.rowset.core.DiscoverKeywordsRow;
import org.eclipse.daanse.xmla.model.rowset.core.DiscoverLiteralsRow;
import org.eclipse.daanse.xmla.model.rowset.core.DiscoverPropertiesRow;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * The bridge's {@code OtherDiscoverServiceTest}, ported onto the connector's
 * EMF rows.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtherDiscoverTest {

    @Mock
    private Context<?> context1;
    @Mock
    private Context<?> context2;
    @Mock
    private ContextListSupplyer contexts;

    @Mock
    private Catalog catalog;   // the existing test mocks context1, context2 and contexts only

    @Mock
    private Connection connection;

    @Mock
    private CatalogReader callerReader;

    @Mock
    private Cube cube;

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

    /** The same, with the connection's current database stated as a property. */
    private List<EObject> discover(String requestType, Map<String, String> restrictions, String catalog) {
        RowsetProvider<ContextListSupplyer> provider = Providers.of(requestType);
        return provider.rows(RowsetScope.of(Requests.discover(requestType, restrictions, catalog), anonymous, contexts,
                Providers.served()));
    }

    /** A caller who holds roles, the way CatalogScopeTest builds one. */
    private static XmlaRequest named(String user, Set<String> roles) {
        Principal principal = () -> user;
        return new XmlaRequest(principal, roles, Map.of(), "http://localhost/xmla", "127.0.0.1");
    }

    private List<EObject> discover(String requestType, Map<String, String> restrictions, XmlaRequest caller) {
        RowsetProvider<ContextListSupplyer> provider = Providers.of(requestType);
        return provider.rows(
                RowsetScope.of(Requests.discover(requestType, restrictions), caller, contexts, Providers.served()));
    }

    
    @Test
    void dataSources() {
        when(contexts.getContexts()).thenReturn(List.of(context1, context2));
        when(context1.getName()).thenReturn("foo");
        when(context1.getDescription()).thenReturn(Optional.of("fooDescription"));
        when(context2.getName()).thenReturn("bar");
        when(context2.getDescription()).thenReturn(Optional.empty());

        List<EObject> rows = discover("DISCOVER_DATASOURCES", Map.of());
        assertThat(rows).hasSize(2);
        DiscoverDatasourcesRow row = (DiscoverDatasourcesRow) rows.get(0);
        assertThat(row.getDataSourceName()).isEqualTo("DataSource of foo");
        assertThat(row.getDataSourceDescription()).isEqualTo("fooDescription");
        assertThat(row.getProviderName()).isEqualTo("Daanse");
        assertThat(row.getProviderType()).containsExactly("MDP");
        assertThat(row.getAuthenticationMode()).isEqualTo("Unauthenticated");
        // Carries a value, and empty is not one. Over HTTP this rowset is the second
        // round trip of every connection, and a client refuses the connection over it.
        assertThat(row.getDataSourceInfo()).isNotNull().isNotEmpty();
    }

    @Test
    void keywordsComeFromTheFirstContext() {
        when(contexts.getContexts()).thenReturn(List.of(context1, context2));
        when(context1.getKeywordList()).thenReturn(List.of("SELECT", "FROM"));

        List<EObject> rows = discover("DISCOVER_KEYWORDS", Map.of());
        assertThat(rows).extracting(row -> ((DiscoverKeywordsRow) row).getKeyword()).containsExactly("SELECT", "FROM");
    }

    @Test
    void literalsCarryTheDbliteralPrefix() {
        List<EObject> rows = discover("DISCOVER_LITERALS", Map.of());
        assertThat(rows).isNotEmpty();
        assertThat(rows)
                .allSatisfy(row -> assertThat(((DiscoverLiteralsRow) row).getLiteralName()).startsWith("DBLITERAL_"));
    }

    @Test
    void enumeratorsAreServed() {
        assertThat(discover("DISCOVER_ENUMERATORS", Map.of())).isNotEmpty();
    }

    @Test
    void propertiesAnswerTheCatalogWithTheCurrentOne() {
        when(contexts.getContexts()).thenReturn(List.of(context1));
        when(context1.getName()).thenReturn("foo");

        List<EObject> rows = discover("DISCOVER_PROPERTIES", Map.of("PropertyName", "Catalog"));
        assertThat(rows).hasSize(1);
        DiscoverPropertiesRow row = (DiscoverPropertiesRow) rows.get(0);
        assertThat(row.getPropertyName()).isEqualTo("Catalog");
        assertThat(row.getValue()).isEqualTo("foo");
    }

    /**
     * DISCOVER_XML_METADATA answers the engine model's DDL, serialized into the
     * engine namespace rather than the rowset one.
     */
    @Test
    void theServerDefinitionIsTheEngineModelsInItsNamespace() {
        org.eclipse.daanse.olap.api.element.Catalog catalog = org.mockito.Mockito
                .mock(org.eclipse.daanse.olap.api.element.Catalog.class);
        when(catalog.getName()).thenReturn("FoodMart");
        when(contexts.get(org.mockito.ArgumentMatchers.any(org.eclipse.daanse.xmla.api.XmlaRequest.class)))
                .thenReturn(List.of(catalog));

        List<EObject> rows = discover("DISCOVER_XML_METADATA", Map.of());
        assertThat(rows).hasSize(1);
        String document = (String) rows.get(0).eGet(rows.get(0).eClass().getEStructuralFeature("metaData"));
        assertThat(document).contains("xmlns=\"http://schemas.microsoft.com/analysisservices/2003/engine\"")
                .contains("<Name>FoodMart</Name>").contains("<Version>13.0.4001.0</Version>")
                .contains("<Edition>Enterprise64</Edition>").contains("<EditionID>1804890536</EditionID>")
                .contains("<Databases>")
                // AMO reads the compatibility level from here and, finding none, falls
                // back to 1050 - below what a live connection needs.
                .contains("<CompatibilityLevel>1100</CompatibilityLevel>");
    }

    /**
     * A DatabaseID restriction selects that Database as the root, the way SSMS
     * asks.
     */
    @Test
    void aDatabaseIdMakesTheDatabaseTheRoot() {
        org.eclipse.daanse.olap.api.element.Catalog catalog = org.mockito.Mockito
                .mock(org.eclipse.daanse.olap.api.element.Catalog.class);
        when(catalog.getName()).thenReturn("FoodMart");
        when(contexts.tryGetFirstByName(org.mockito.ArgumentMatchers.eq("FoodMart"),
                org.mockito.ArgumentMatchers.any(org.eclipse.daanse.xmla.api.XmlaRequest.class)))
                .thenReturn(java.util.Optional.of(catalog));

        List<EObject> rows = discover("DISCOVER_XML_METADATA", Map.of("DatabaseID", "FoodMart"));
        String document = (String) rows.get(0).eGet(rows.get(0).eClass().getEStructuralFeature("metaData"));
        assertThat(document).startsWith("<Database").doesNotContain("<Server");
    }

    /**
     * ReferenceOnly still carries the contained objects - only ObjectProperties
     * does not.
     * <p>
     * [MS-SSAS] on the four values: ReferenceOnly "returns only the
     * name/ID/timestamp/state ... for the requested objects <em>and all descendant
     * major objects recursively</em>"; ObjectProperties "expands the requested
     * object <em>with no references to contained objects</em>". ReferenceOnly is
     * the form a client asks with, so getting this the wrong way round leaves it
     * with a Server and no databases in it.
     */
    @Test
    void referenceOnlyStillCarriesTheDatabases() {
        org.eclipse.daanse.olap.api.element.Catalog catalog = org.mockito.Mockito
                .mock(org.eclipse.daanse.olap.api.element.Catalog.class);
        when(catalog.getName()).thenReturn("FoodMart");
        when(contexts.get(org.mockito.ArgumentMatchers.any(org.eclipse.daanse.xmla.api.XmlaRequest.class)))
                .thenReturn(List.of(catalog));

        String document = documentOf(discover("DISCOVER_XML_METADATA", Map.of("ObjectExpansion", "ReferenceOnly")));
        assertThat(document).contains("<Server").contains("<Databases>").contains("<Name>FoodMart</Name>");
    }

    /** ObjectProperties is the one expansion that keeps them out. */
    @Test
    void objectPropertiesCarriesNoDatabases() {
        org.eclipse.daanse.olap.api.element.Catalog catalog = org.mockito.Mockito
                .mock(org.eclipse.daanse.olap.api.element.Catalog.class);
        when(catalog.getName()).thenReturn("FoodMart");
        when(contexts.get(org.mockito.ArgumentMatchers.any(org.eclipse.daanse.xmla.api.XmlaRequest.class)))
                .thenReturn(List.of(catalog));

        String document = documentOf(discover("DISCOVER_XML_METADATA", Map.of("ObjectExpansion", "ObjectProperties")));
        assertThat(document).contains("<Server").doesNotContain("<Databases>");
    }

    /**
     * The connection's Catalog does not pick the object - only a restriction does.
     * <p>
     * [MS-SSAS] lists this rowset's additional restrictions by name - DatabaseID,
     * CubeID, DimensionID and the rest - and Catalog is not among them: it says
     * which database the session is on, not which object is being asked for. Every
     * request a connected client sends carries Catalog, so reading it here answers
     * all of them with a Database as the root and never the Server element AMO
     * reads the version and compatibility level from.
     */
    @Test
    void theCatalogPropertyDoesNotMakeTheDatabaseTheRoot() {
        org.eclipse.daanse.olap.api.element.Catalog catalog = org.mockito.Mockito
                .mock(org.eclipse.daanse.olap.api.element.Catalog.class);
        when(catalog.getName()).thenReturn("FoodMart");
        when(contexts.get(org.mockito.ArgumentMatchers.any(org.eclipse.daanse.xmla.api.XmlaRequest.class)))
                .thenReturn(List.of(catalog));

        String document = documentOf(
                discover("DISCOVER_XML_METADATA", Map.of("ObjectExpansion", "ReferenceOnly"), "FoodMart"));

        assertThat(document).as("the Server element is what a client comes here for").contains("<Server")
                .doesNotStartWith("<Database ").doesNotStartWith("<Database>");
    }

    private static String documentOf(List<EObject> rows) {
        assertThat(rows).hasSize(1);
        return (String) rows.get(0).eGet(rows.get(0).eClass().getEStructuralFeature("metaData"));
    }

    @Test
    void theSelfDescriptionNamesExactlyTheRegisteredRowsets() {
        List<EObject> rows = discover("DISCOVER_SCHEMA_ROWSETS", Map.of());

        // Generated from the whiteboard's registrations, not from the model's full
        // list:
        // what the server announces is what it can be asked.
        assertThat(rows).hasSize(Providers.served().size());
        assertThat(rows).extracting(row -> row.eGet(row.eClass().getEStructuralFeature("schemaName")))
                .containsExactlyInAnyOrderElementsOf(Providers.served());
    }

    @Test
    void theSelfDescriptionCarriesTheModelsRestrictionsAndMask() {
        List<EObject> rows = discover("DISCOVER_SCHEMA_ROWSETS", Map.of("SchemaName", "MDSCHEMA_MEMBERS"));

        assertThat(rows).hasSize(1);
        EObject row = rows.get(0);
        assertThat(row.eGet(row.eClass().getEStructuralFeature("restrictionsMask")))
                .isEqualTo(org.eclipse.daanse.xmla.model.io.RowsetCatalog
                        .restrictionsMaskOf(org.eclipse.daanse.xmla.model.io.RowsetCatalog
                                .forRequestType("MDSCHEMA_MEMBERS").orElseThrow()));
        @SuppressWarnings("unchecked")
        List<EObject> restrictions = (List<EObject>) row.eGet(row.eClass().getEStructuralFeature("restrictions"));
        assertThat(restrictions).isNotEmpty();
        assertThat(restrictions).extracting(one -> one.eGet(one.eClass().getEStructuralFeature("name")))
                .contains("CATALOG_NAME", "TREE_OP");
    }

    /**
     * The VERSION restriction was read by nobody and the answer was always 2.0 —
     * the one failure a version negotiation must not have, because the client
     * cannot tell it happened.
     */
    @Test
    void anUnsupportedCsdlVersionIsRefusedRatherThanSilentlySubstituted() {
        assertThatThrownBy(() -> discover("DISCOVER_CSDL_METADATA", Map.of("CATALOG_NAME", "foo", "VERSION", "9.9")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("9.9");
    }

    /** [MS-SSAS] 3.1.4.2.2.1.3.61.2: the value MUST be <integer>.<integer>. */
    @Test
    void aMalformedCsdlVersionIsRefused() {
        assertThatThrownBy(() -> discover("DISCOVER_CSDL_METADATA", Map.of("CATALOG_NAME", "foo", "VERSION", "zwei")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("<integer>.<integer>");
    }

    /**
     * Every registered rowset is advertised, and advertised completely. A client
     * reads this list to decide what it may ask for and with which restrictions; a
     * row without a mask or without its restrictions is worse than no row.
     */
    @Test
    void everyServedRowsetIsAdvertisedWithGuidAndMask() {
        List<EObject> rows = discover("DISCOVER_SCHEMA_ROWSETS", Map.of());

        assertThat(rows).hasSize(Providers.served().size());
        for (EObject row : rows) {
            String name = (String) row.eGet(row.eClass().getEStructuralFeature("schemaName"));
            assertThat(row.eGet(row.eClass().getEStructuralFeature("schemaGuid"))).as("%s guid", name).isNotNull();
            assertThat(row.eGet(row.eClass().getEStructuralFeature("restrictionsMask"))).as("%s mask", name)
                    .isNotNull();
        }
        assertThat(rows).extracting(row -> row.eGet(row.eClass().getEStructuralFeature("schemaName")))
                .contains("DBSCHEMA_PRIMARY_KEYS", "DBSCHEMA_FOREIGN_KEYS", "DBSCHEMA_INDEXES", "DBSCHEMA_VIEWS");
    }

    /**
     * Where a rowset comes from, said in the column the rowset has for saying it.
     * Only four of the relational rowsets are named by [MS-SSAS]; a client that
     * lists them should be able to see which is which without guessing from the
     * name.
     */
    @Test
    void theDescriptionNamesWhereARowsetComesFrom() {
        List<EObject> rows = discover("DISCOVER_SCHEMA_ROWSETS", Map.of("SchemaName", "DBSCHEMA_PRIMARY_KEYS"));

        assertThat(rows).hasSize(1);
        String description = (String) rows.get(0).eGet(rows.get(0).eClass().getEStructuralFeature("description"));
        assertThat(description).contains("OLE DB").contains("Appendix B");
    }

    @Test
    void csdlMetadataIsEmittedThroughTheCallersReader() {
        when(contexts.tryGetFirstByName(eq("FoodMart"), any())).thenReturn(Optional.of(catalog));
        when(catalog.getName()).thenReturn("FoodMart");
        when(contexts.getConnection(any(), eq("FoodMart"))).thenReturn(connection);
        when(connection.getCatalogReader()).thenReturn(callerReader);
        when(callerReader.getCatalog()).thenReturn(catalog);
        when(cube.getName()).thenReturn("Sales");
        when(callerReader.getCubes()).thenReturn(List.of(cube));
        when(callerReader.getCubeDimensions(cube)).thenAnswer(i -> List.of()); // no dimensions to walk

        XmlaRequest caller = named("tester", Set.of("roleNone"));
        List<EObject> rows = discover("DISCOVER_CSDL_METADATA",
                Map.of("CATALOG_NAME", "FoodMart", "PERSPECTIVE_NAME", "Sales"), caller);

        assertThat(rows).hasSize(1);
        verify(contexts).getConnection(caller, "FoodMart");
        verify(catalog, never()).getCatalogReaderWithDefaultRole();
    }
}
