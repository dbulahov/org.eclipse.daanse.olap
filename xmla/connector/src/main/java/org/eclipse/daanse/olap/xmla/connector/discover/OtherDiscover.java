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
package org.eclipse.daanse.olap.xmla.connector.discover;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.xmla.connector.discover.csdl.CsdlDocuments;
import org.eclipse.daanse.olap.xmla.connector.discover.csdl.CsdlEmitterImpl;
import org.eclipse.daanse.olap.xmla.connector.discover.csdl.CsdlRequest;
import org.eclipse.daanse.olap.xmla.connector.discover.csdl.CsdlVersion;
import org.eclipse.daanse.olap.xmla.connector.discover.csdl.LocalePolicy;
import org.eclipse.daanse.olap.xmla.connector.ContextListSupplyer;
import org.eclipse.daanse.xmla.model.engine.Database;
import org.eclipse.daanse.xmla.model.engine.EditionEnum;
import org.eclipse.daanse.xmla.model.engine.EngineFactory;
import org.eclipse.daanse.xmla.model.engine.Server;
import org.eclipse.daanse.xmla.model.io.EcoreXmlWriter;
import org.eclipse.daanse.xmla.model.io.HardenedXml;
import org.eclipse.daanse.xmla.model.io.PropertyCatalog;
import org.eclipse.daanse.xmla.model.io.XmlaNamespaces;
import org.eclipse.daanse.xmla.api.RestrictionValues;
import org.eclipse.daanse.xmla.model.rowset.server.DiscoverCsdlMetadataRow;
import org.eclipse.daanse.xmla.model.rowset.core.DiscoverDatasourcesRow;
import org.eclipse.daanse.xmla.model.rowset.core.DiscoverEnumeratorsRow;
import org.eclipse.daanse.xmla.model.rowset.core.DiscoverKeywordsRow;
import org.eclipse.daanse.xmla.model.rowset.core.DiscoverLiteralsRow;
import org.eclipse.daanse.xmla.model.rowset.core.DiscoverPropertiesRow;
import org.eclipse.daanse.xmla.model.rowset.server.DiscoverXmlMetadataRow;
import org.eclipse.daanse.xmla.model.rowset.core.RowsetCoreFactory;
import org.eclipse.daanse.xmla.model.rowset.server.RowsetServerFactory;
import org.eclipse.daanse.xmla.api.XmlaRequest;
import org.eclipse.daanse.xmla.model.io.RowsetCatalog;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.ExtendedMetaData;

/**
 * The DISCOVER_* rowsets: the server describing itself.
 * <p>
 * Ported from the bridge's {@code OtherDiscoverService}, with one deliberate
 * departure: the rows are built as model EObjects rather than api records —
 * there is no converter behind this any more, so what is built here is what
 * goes on the wire.
 * <p>
 * {@code DISCOVER_SCHEMA_ROWSETS} is deliberately not here. It is answered out
 * of the Ecore model by {@code DiscoverSchemaRowsetsProvider}, which is where
 * the reason is written down.
 */
public class OtherDiscover {

    private static final String DAANSE = "Daanse";
    private static final String DBLITERAL = "DBLITERAL_";
    // Two rowset factories, and the split is why: the six rowsets XMLA 1.1 defines
    // itself live in rowset.core, the server's own in rowset.server. This is the
    // only
    // rowset class here that spans both. EngineFactory, used further down, is a
    // third
    // factory but for the engine model rather than for rowsets.
    private static final RowsetCoreFactory CORE = RowsetCoreFactory.eINSTANCE;
    private static final RowsetServerFactory SERVER = RowsetServerFactory.eINSTANCE;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final ContextListSupplyer contexts;

    public OtherDiscover(ContextListSupplyer contexts) {
        this.contexts = contexts;
    }

    public List<EObject> dataSources(RestrictionValues restrictions) {
        Optional<String> wanted = restrictions.catalogProperty();
        if (wanted.isEmpty()) {
            wanted = restrictions.value("DataSourceName");
        }

        List<EObject> result = new ArrayList<>();
        if (wanted.isEmpty()) {
            for (Context<?> context : contexts.getContexts()) {
                result.add(dataSource(context));
            }
        } else {
            Optional<Context<?>> context = contexts.getContext(wanted.get());
            if (context.isPresent()) {
                result.add(dataSource(context.get()));
            }
        }
        return result;
    }

    private static EObject dataSource(Context<?> context) {
        DiscoverDatasourcesRow row = CORE.createDiscoverDatasourcesRow();
        row.setDataSourceName("DataSource of " + context.getName());
        context.getDescription().ifPresent(row::setDataSourceDescription);
        // The instance name, as [MS-SSAS] asks: "Contains the information, such as the
        // instance name, that is required to connect to the data source." Over HTTP
        // this rowset is the second round trip of every connection and a client refuses
        // the connection over it, so the value has to be one - the empty string is not.
        // The client keeps it as the connection's DataSourceInfo property and sends it
        // back with every later request.
        row.setDataSourceInfo(SupportedProperties.serverName());
        row.setProviderName(DAANSE);
        row.getProviderType().add("MDP");
        row.setAuthenticationMode("Unauthenticated");
        return row;
    }

    public List<EObject> enumerators() {
        List<EObject> result = new ArrayList<>();
        // The vocabularies are in the model, not in a Java enum here: they are XMLA's
        // own and the same for every provider. The catalogue finds them by the
        // annotation they carry, so adding one is a change to the model alone.
        //
        // Both names come from the model's own wire spelling rather than from its Java
        // one: the enumeration through ExtendedMetaData, the literal through
        // getLiteral,
        // which is where Ecore keeps the text that goes out. Reading element.getName()
        // instead would be a second source for the same thing.
        //
        // The value is the model's too, and it matters that it is: XMLA 1.1 assigns
        // numbers to none of these vocabularies except TreeOp, and has the provider
        // publish through this very rowset whatever it uses. This is that publication.
        for (EEnum enumeration : RowsetCatalog.enumerators()) {
            for (EEnumLiteral element : enumeration.getELiterals()) {
                DiscoverEnumeratorsRow row = CORE.createDiscoverEnumeratorsRow();
                row.setEnumName(ExtendedMetaData.INSTANCE.getName(enumeration));
                row.setEnumType("string");
                row.setElementName(element.getLiteral());
                row.setElementValue(String.valueOf(element.getValue()));
                result.add(row);
            }
        }
        return result;
    }

    public List<EObject> keywords() {
        List<EObject> result = new ArrayList<>();
        List<Context<?>> all = contexts.getContexts();
        if (all != null && !all.isEmpty()) {
            for (String keyword : all.getFirst().getKeywordList()) {
                DiscoverKeywordsRow row = CORE.createDiscoverKeywordsRow();
                row.setKeyword(keyword);
                result.add(row);
            }
        }
        return result;
    }

    public List<EObject> literals() {
        List<EObject> result = new ArrayList<>();
        for (OleDbLiteral literal : OleDbLiteral.values()) {
            DiscoverLiteralsRow row = CORE.createDiscoverLiteralsRow();
            row.setLiteralName(DBLITERAL + literal.name());
            row.setLiteralValue(literal.literalValue());
            if (literal.literalInvalidChars() != null) {
                row.setLiteralInvalidChars(literal.literalInvalidChars());
            }
            if (literal.literalInvalidStartingChars() != null) {
                row.setLiteralInvalidStartingChars(literal.literalInvalidStartingChars());
            }
            row.setLiteralMaxLength(literal.literalMaxLength());
            row.setLiteralNameEnumValue(literal.literalNameEnumValue());
            result.add(row);
        }
        return result;
    }

    public List<EObject> properties(RestrictionValues restrictions) {
        List<String> wanted = restrictions.values("PropertyName");
        Optional<String> catalogProperty = restrictions.catalogProperty();

        List<EObject> result = new ArrayList<>();
        for (String name : SupportedProperties.NAMES) {
            if (!wanted.isEmpty() && !wanted.contains(name)) {
                continue;
            }
            // orElseThrow is the drift alarm: a name this server answers has to be a
            // property the model states facts for.
            PropertyCatalog.Property property = PropertyCatalog.byName(name).orElseThrow();
            DiscoverPropertiesRow row = CORE.createDiscoverPropertiesRow();
            row.setPropertyName(property.name());
            if (!property.description().isEmpty()) {
                row.setPropertyDescription(property.description());
            }
            row.setPropertyType(property.type());
            row.setPropertyAccessType(property.access());
            row.setIsRequired(Boolean.FALSE);
            row.setValue(propertyValue(property, catalogProperty));
            result.add(row);
        }
        return result;
    }

    private String propertyValue(PropertyCatalog.Property property, Optional<String> catalogProperty) {
        if ("Catalog".equals(property.name())) {
            // The current catalog: the one the client named where this server has it,
            // the first one otherwise. A server with no catalog at all answers with no
            // value rather than throwing - the property exists, it is just unset.
            if (catalogProperty.isPresent()) {
                Optional<Context<?>> context = contexts.getContext(catalogProperty.get());
                if (context.isPresent()) {
                    return context.get().getName();
                }
            }
            List<Context<?>> all = contexts.getContexts();
            return all == null || all.isEmpty() ? null : all.get(0).getName();
        }
        String serverTruth = SupportedProperties.VALUES.get(property.name());
        if (serverTruth != null) {
            return serverTruth;
        }
        return property.defaultValue().orElse(null);
    }

    /**
     * DISCOVER_XML_METADATA: the requested object's DDL definition, built from the
     * engine model and serialized into its own namespace - the shape every live
     * server answers with. No restriction means the Server; {@code DatabaseID}
     * selects that Database as the root instead; every {@code ObjectExpansion}
     * except ObjectProperties attaches the Databases children.
     */
    public List<EObject> xmlMetaData(RestrictionValues restrictions, XmlaRequest caller) {
        // Only a restriction picks the object. [MS-SSAS] lists this rowset's additional
        // restrictions by name - DatabaseID, CubeID, DimensionID and the rest - and the
        // connection's Catalog is not among them: it says which database the session is
        // on, not which object is asked for. Reading it here would answer every request
        // a connected client sends with a Database as the root, and never the Server
        // element AMO reads the version and compatibility level from.
        Optional<String> databaseId = restrictions.value("DatabaseID");
        String expansion = restrictions.value("ObjectExpansion").orElse("ExpandObject");
        // Only ObjectProperties keeps the contained objects out. [MS-SSAS] on the four
        // values: ReferenceOnly "returns only the name/ID/timestamp/state ... for the
        // requested objects and all descendant major objects recursively";
        // ObjectProperties "expands the requested object with no references to
        // contained objects"; ExpandObject is ObjectProperties plus the name, ID and
        // timestamp of contained major objects; ExpandFull expands everything.
        boolean withDatabases = !"ObjectProperties".equalsIgnoreCase(expansion);
        XMLGregorianCalendar date = timestampNow();

        List<EObject> result = new ArrayList<>();
        if (databaseId.isPresent()) {
            Optional<Catalog> catalog = contexts.tryGetFirstByName(databaseId.get(), caller);
            if (catalog.isPresent()) {
                result.add(metadataRow(engineDocument(databaseOf(catalog.get(), date), "Database")));
            }
            return result;
        }
        List<Catalog> catalogs = contexts.get(caller);
        if (catalogs == null || catalogs.isEmpty()) {
            return result;
        }
        Server server = EngineFactory.eINSTANCE.createServer();
        // The server's own name, not the first catalog's - this is the Server object.
        // It is the same value DISCOVER_PROPERTIES reports as ServerName, so a client
        // that compares the two finds them agreeing.
        String serverName = SupportedProperties.serverName();
        server.setName(serverName);
        server.setId(serverName);
        server.setCreatedTimestamp(date);
        server.setLastSchemaUpdate(date);
        // Exactly the seven elements a recorded Analysis Services writes here, and in
        // its order: Name, ID, CreatedTimestamp, LastSchemaUpdate, Version, Edition,
        // EditionID. An element more is not free - a reader takes this sequence in
        // order and an unexpected one arrives in the wrong place.
        server.setVersion("13.0.4001.0");
        server.setEdition(EditionEnum.ENTERPRISE64);
        server.setEditionID(1804890536L);
        if (withDatabases) {
            for (Catalog catalog : catalogs) {
                server.getDatabases().add(databaseOf(catalog, date));
            }
        }
        result.add(metadataRow(engineDocument(server, "Server")));
        return result;
    }

    /**
     * The compatibility level of a multidimensional database, stated rather than
     * left out.
     * <p>
     * AMO reads it from the Database element of DISCOVER_XML_METADATA and, finding
     * none, falls back to 1050 - the 2008 R2 level, below what a client needs for a
     * live connection to a multidimensional model. 1100 is SQL Server 2012, which
     * this engine is; 1200 and above mean tabular metadata, which it has not.
     */
    private static final java.math.BigInteger MULTIDIMENSIONAL_COMPATIBILITY_LEVEL = java.math.BigInteger
            .valueOf(1100);

    private static Database databaseOf(Catalog catalog, XMLGregorianCalendar date) {
        Database database = EngineFactory.eINSTANCE.createDatabase();
        database.setName(catalog.getName());
        database.setId(catalog.getName());
        database.setCreatedTimestamp(date);
        database.setLastSchemaUpdate(date);
        database.setCompatibilityLevel(MULTIDIMENSIONAL_COMPATIBILITY_LEVEL);
        return database;
    }

    private static EObject metadataRow(String document) {
        DiscoverXmlMetadataRow row = SERVER.createDiscoverXmlMetadataRow();
        row.setMetaData(document);
        return row;
    }

    /** The engine model's DDL, serialized into its own namespace. */
    private static String engineDocument(EObject object, String elementName) {
        try {
            StringWriter out = new StringWriter();
            XMLStreamWriter writer = HardenedXml.output().createXMLStreamWriter(out);
            new EcoreXmlWriter(XmlaNamespaces.ENGINE).write(writer, object, elementName);
            writer.flush();
            writer.close();
            return out.toString();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("the engine definition did not serialize", e);
        }
    }

    private static XMLGregorianCalendar timestampNow() {
        try {
            // UTC, for the same reason as everywhere else a timestamp leaves this server:
            // the client converts it to local time on arrival.
            return DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(LocalDateTime.now(java.time.ZoneOffset.UTC).format(FORMATTER));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The CSDL versions the emitters produce, by the {@code <integer>.<integer>}
     * text a client asks with. [MS-SSAS] 3.1.4.2.2.1.3.61.2 fixes only the shape of
     * the value, not a vocabulary, so what is listed here is what this server can
     * actually emit rather than what the specification enumerates.
     */
    private static final java.util.Map<String, CsdlVersion> CSDL_VERSIONS = java.util.Map.of("1.1", CsdlVersion.V1_1,
            "2.0", CsdlVersion.V2_0,
            // 2.5 is not in [MS-CSDLBI], which stops at 2.0, but it is what a client
            // asks for before establishing a live connection, and a recorded Analysis
            // Services answers it with a document. Served by the 2.0 emitter, the
            // closest this server has - an approximation, and where to look if a 2.5
            // document turns out to differ in a way that matters.
            "2.5", CsdlVersion.V2_0);

    private static final CsdlVersion CSDL_DEFAULT = CsdlVersion.V2_0;

    /**
     * DISCOVER_CSDL_METADATA: the catalog as an Edmx document, one row holding the
     * whole of it. The emitters were ported from the bridge's
     * {@code discover/csdl/dimensional} intact; an unnamed catalog falls back to
     * the first one, exactly like DISCOVER_XML_METADATA above.
     * <p>
     * The VERSION restriction is honoured: an unsupported version is refused the way
     * Analysis Services refuses it (error {@code 0xC114022F}), never silently
     * answered with a different one.
     */
    public List<EObject> csdlMetaData(RestrictionValues restrictions, XmlaRequest caller) {
        Optional<String> catalogName = restrictions.value("CATALOG_NAME");
        if (catalogName.isEmpty()) {
            catalogName = restrictions.catalogProperty();
        }
        Optional<String> perspectiveName = restrictions.value("PERSPECTIVE_NAME");
        CsdlVersion version = csdlVersion(restrictions.value("VERSION"));

        Catalog catalog = null;
        if (catalogName.isPresent()) {
            Optional<Catalog> found = contexts.tryGetFirstByName(catalogName.get(), caller);
            if (found.isPresent()) {
                catalog = found.get();
            }
        } else {
            List<Catalog> catalogs = contexts.get(caller);
            if (catalogs != null && !catalogs.isEmpty()) {
                catalog = catalogs.get(0);
            }
        }
        if (catalog == null) {
            return List.of();
        }

        CatalogReader reader = contexts.getConnection(caller, catalog.getName()).getCatalogReader();
        LocalePolicy localePolicy = new LocalePolicy.ServerDefault(java.util.Locale.getDefault());
        CsdlRequest csdlRequest = new CsdlRequest(version, perspectiveName, localePolicy);
        DiscoverCsdlMetadataRow row = SERVER.createDiscoverCsdlMetadataRow();
        row.setMetaData(CsdlDocuments.asString(new CsdlEmitterImpl().emit(reader, csdlRequest)));
        return List.of((EObject) row);
    }

    /**
     * @throws IllegalArgumentException if the value is malformed or names a version
     *                                  this server cannot emit — better a refusal
     *                                  the client can read than a document in a
     *                                  version it did not ask for
     */
    private static CsdlVersion csdlVersion(Optional<String> requested) {
        if (requested.isEmpty()) {
            return CSDL_DEFAULT;
        }
        String asked = requested.get().trim();
        if (!asked.matches("\\d+\\.\\d+")) {
            throw new IllegalArgumentException(
                    "the VERSION restriction must be of the format <integer>.<integer>, not '" + asked + "'");
        }
        CsdlVersion known = CSDL_VERSIONS.get(asked);
        if (known == null) {
            throw new IllegalArgumentException("CSDL version " + asked + " is not supported; this server emits "
                    + new java.util.TreeSet<>(CSDL_VERSIONS.keySet()));
        }
        return known;
    }

}
