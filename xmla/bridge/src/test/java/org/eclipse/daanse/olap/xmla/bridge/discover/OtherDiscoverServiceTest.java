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

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.ContextGroup;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.xmla.bridge.ContextGroupXmlaServiceConfig;
import org.eclipse.daanse.olap.xmla.bridge.ContextsSupplyerImpl;
import org.eclipse.daanse.xmla.api.RequestMetaData;
import org.eclipse.daanse.xmla.api.discover.Properties;
import org.eclipse.daanse.xmla.api.discover.discover.datasources.DiscoverDataSourcesRequest;
import org.eclipse.daanse.xmla.api.discover.discover.datasources.DiscoverDataSourcesResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.enumerators.DiscoverEnumeratorsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.enumerators.DiscoverEnumeratorsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.keywords.DiscoverKeywordsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.keywords.DiscoverKeywordsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.literals.DiscoverLiteralsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.literals.DiscoverLiteralsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.properties.DiscoverPropertiesRequest;
import org.eclipse.daanse.xmla.api.discover.discover.properties.DiscoverPropertiesResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.properties.DiscoverPropertiesRestrictions;
import org.eclipse.daanse.xmla.api.discover.discover.schemarowsets.DiscoverSchemaRowsetsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.schemarowsets.DiscoverSchemaRowsetsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.schemarowsets.DiscoverSchemaRowsetsRestrictions;
import org.eclipse.daanse.xmla.api.discover.discover.xmlmetadata.DiscoverXmlMetaDataRequest;
import org.eclipse.daanse.xmla.api.discover.discover.xmlmetadata.DiscoverXmlMetaDataResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.xmlmetadata.DiscoverXmlMetaDataRestrictions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class OtherDiscoverServiceTest {

    @Mock
    private Catalog catalog1;

    @Mock
    private Catalog catalog2;

    @Mock
    private Context context1;

    @Mock
    private Context context2;

    @Mock
    private ContextGroup contextGroup;

    @Mock
    private RequestMetaData requestMetaData;

    private OtherDiscoverService service;

    @Mock
    private ContextsSupplyerImpl cls;

    private ContextGroupXmlaServiceConfig config;

    @BeforeEach
    void setup() {
        config = mock(ContextGroupXmlaServiceConfig.class);

        service = new OtherDiscoverService(cls, config);
    }

    @Test
    void dataSources() throws SQLException {
        when(cls.get(any())).thenReturn(List.of(catalog1, catalog2));

        DiscoverDataSourcesRequest request = mock(DiscoverDataSourcesRequest.class);

        when(catalog1.getName()).thenReturn("bar");
        when(catalog1.getDescription()).thenReturn("barDescription");
        when(catalog2.getName()).thenReturn("foo");
        when(catalog2.getDescription()).thenReturn("fooDescription");
        // when(context2.getDataSource()).thenReturn(dataSource);

        List<DiscoverDataSourcesResponseRow> rows = service.dataSources(request, requestMetaData);
        verify(catalog1, times(1)).getName();
        verify(catalog2, times(1)).getName();
        assertThat(rows).isNotNull().hasSize(2);
        DiscoverDataSourcesResponseRow row = rows.get(0);
        assertThat(row).isNotNull();
        assertThat(row.dataSourceName()).isEqualTo("DataSource of bar");
        assertThat(row.dataSourceDescription()).contains("barDescription");
        assertThat(row.url()).isEmpty();
        assertThat(row.dataSourceInfo()).isEmpty();
        assertThat(row.providerName()).isNull();
        assertThat(row.providerType()).isEmpty();
        assertThat(row.authenticationMode()).isEmpty();
        row = rows.get(1);
        assertThat(row).isNotNull();
        assertThat(row.dataSourceName()).isEqualTo("DataSource of foo");
        assertThat(row.dataSourceDescription()).contains("fooDescription");
        assertThat(row.url()).isEmpty();
        assertThat(row.dataSourceInfo()).isEmpty();
        assertThat(row.providerName()).isNull();
        assertThat(row.providerType()).isEmpty();
        assertThat(row.authenticationMode()).isEmpty();

    }

    @Test
    void discoverEnumerators() {
        DiscoverEnumeratorsRequest request = mock(DiscoverEnumeratorsRequest.class);

        List<DiscoverEnumeratorsResponseRow> rows = service.discoverEnumerators(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(15).extracting(DiscoverEnumeratorsResponseRow::enumName)
                .containsAnyOf("Access", "AuthenticationMode", "ProviderType", "TreeOp");
    }

    @Test
    void discoverKeywords() {
        DiscoverKeywordsRequest request = mock(DiscoverKeywordsRequest.class);
        when(cls.getContexts()).thenReturn(List.of(context1, context2));
        when(context1.getKeywordList()).thenReturn(Context.KEYWORD_LIST);
        List<DiscoverKeywordsResponseRow> rows = service.discoverKeywords(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(256).extracting(DiscoverKeywordsResponseRow::keyword)
                .containsExactlyInAnyOrder("$AdjustedProbability", "$Distance", "$Probability", "$ProbabilityStDev",
                        "$ProbabilityStdDeV", "$ProbabilityVariance", "$StDev", "$StdDeV", "$Support", "$Variance",
                        "AddCalculatedMembers", "Action", "After", "Aggregate", "All", "Alter", "Ancestor", "And",
                        "Append", "As", "ASC", "Axis", "Automatic", "Back_Color", "BASC", "BDESC", "Before",
                        "Before_And_After", "Before_And_Self", "Before_Self_After", "BottomCount", "BottomPercent",
                        "BottomSum", "Break", "Boolean", "Cache", "Calculated", "Call", "Case", "Catalog_Name", "Cell",
                        "Cell_Ordinal", "Cells", "Chapters", "Children", "Children_Cardinality", "ClosingPeriod",
                        "Cluster", "ClusterDistance", "ClusterProbability", "Clusters", "CoalesceEmpty",
                        "Column_Values", "Columns", "Content", "Contingent", "Continuous", "Correlation", "Cousin",
                        "Covariance", "CovarianceN", "Create", "CreatePropertySet", "CrossJoin", "Cube", "Cube_Name",
                        "CurrentMember", "CurrentCube", "Custom", "Cyclical", "DefaultMember", "Default_Member", "DESC",
                        "Descendents", "Description", "Dimension", "Dimension_Unique_Name", "Dimensions", "Discrete",
                        "Discretized", "DrillDownLevel", "DrillDownLevelBottom", "DrillDownLevelTop", "DrillDownMember",
                        "DrillDownMemberBottom", "DrillDownMemberTop", "DrillTrough", "DrillUpLevel", "DrillUpMember",
                        "Drop", "Else", "Empty", "End", "Equal_Areas", "Exclude_Null", "ExcludeEmpty", "Exclusive",
                        "Expression", "Filter", "FirstChild", "FirstRowset", "FirstSibling", "Flattened", "Font_Flags",
                        "Font_Name", "Font_size", "Fore_Color", "Format_String", "Formatted_Value", "Formula", "From",
                        "Generate", "Global", "Head", "Hierarchize", "Hierarchy", "Hierary_Unique_name", "IIF",
                        "IsEmpty", "Include_Null", "Include_Statistics", "Inclusive", "Input_Only", "IsDescendant",
                        "Item", "Lag", "LastChild", "LastPeriods", "LastSibling", "Lead", "Level", "Level_Number",
                        "Level_Unique_Name", "Levels", "LinRegIntercept", "LinRegR2", "LinRegPoint", "LinRegSlope",
                        "LinRegVariance", "Long", "MaxRows", "Median", "Member", "Member_Caption", "Member_Guid",
                        "Member_Name", "Member_Ordinal", "Member_Type", "Member_Unique_Name", "Members",
                        "Microsoft_Clustering", "Microsoft_Decision_Trees", "Mining", "Model", "Model_Existence_Only",
                        "Models", "Move", "MTD", "Name", "Nest", "NextMember", "Non", "NonEmpty", "Normal", "Not",
                        "Ntext", "Nvarchar", "OLAP", "On", "OpeningPeriod", "OpenQuery", "Or", "Ordered", "Ordinal",
                        "Pages", "ParallelPeriod", "Parent", "Parent_Level", "Parent_Unique_Name", "PeriodsToDate",
                        "PMML", "Predict", "Predict_Only", "PredictAdjustedProbability", "PredictHistogram",
                        "Prediction", "PredictionScore", "PredictProbability", "PredictProbabilityStDev",
                        "PredictProbabilityVariance", "PredictStDev", "PredictSupport", "PredictVariance", "PrevMember",
                        "Probability", "Probability_StDev", "Probability_StdDev", "Probability_Variance", "Properties",
                        "Property", "QTD", "RangeMax", "RangeMid", "RangeMin", "Rank", "Recursive", "Refresh",
                        "Related", "Rename", "Rollup", "Rows", "Schema_Name", "Sections", "Select", "Self",
                        "Self_And_After", "Sequence_Time", "Server", "Session", "Set", "SetToArray", "SetToStr",
                        "Shape", "Skip", "Solve_Order", "Sort", "StdDev", "Stdev", "StripCalculatedMembers", "StrToSet",
                        "StrToTuple", "SubSet", "Support", "Tail", "Text", "Thresholds", "ToggleDrillState", "TopCount",
                        "TopPercent", "TopSum", "TupleToStr", "Under", "Uniform", "UniqueName", "Use", "Value", "Var",
                        "Variance", "VarP", "VarianceP", "VisualTotals", "When", "Where", "With", "WTD", "Xor");
    }

    @Test
    void discoverLiterals() {
        DiscoverLiteralsRequest request = mock(DiscoverLiteralsRequest.class);
        List<DiscoverLiteralsResponseRow> rows = service.discoverLiterals(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(17);
    }

    @Test
    void discoverProperties() {
        when(cls.getContexts()).thenReturn(List.of(context1, context2));
        DiscoverPropertiesRequest request = mock(DiscoverPropertiesRequest.class);
        DiscoverPropertiesRestrictions restrictions = mock(DiscoverPropertiesRestrictions.class);
        Properties properties = mock(Properties.class);
        when(properties.catalog()).thenReturn(Optional.empty());
        when(request.restrictions()).thenReturn(restrictions);
        when(request.properties()).thenReturn(properties);

        List<DiscoverPropertiesResponseRow> rows = service.discoverProperties(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(31).extracting(DiscoverPropertiesResponseRow::propertyName)
                .containsExactlyInAnyOrder("AxisFormat", "BeginRange", "Catalog", "Content", "Cube", "DataSourceInfo",
                        "Deep", "EmitInvisibleMembers", "EndRange", "Format", "LocaleIdentifier", "MDXSupport",
                        "Password", "ProviderName", "ProviderVersion", "ResponseMimeType", "StateSupport", "Timeout",
                        "UserName", "VisualMode", "TableFields", "AdvancedFlag", "SafetyOptions",
                        "MdxMissingMemberMode", "DbpropMsmdMDXCompatibility", "MdpropMdxSubqueries", "ClientProcessID",
                        "SspropInitAppName", "DbpropMsmdSubqueries", "DbpropMsmdActivityID", "DBMSVersion");
    }

    @Test
    void discoverSchemaRowsets() {
        DiscoverSchemaRowsetsRequest request = mock(DiscoverSchemaRowsetsRequest.class);
        DiscoverSchemaRowsetsRestrictions restrictions = mock(DiscoverSchemaRowsetsRestrictions.class);
        when(request.restrictions()).thenReturn(restrictions);

        List<DiscoverSchemaRowsetsResponseRow> rows = service.discoverSchemaRowsets(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(28).extracting(DiscoverSchemaRowsetsResponseRow::schemaName)
                .containsExactlyInAnyOrder("MDSCHEMA_FUNCTIONS", "MDSCHEMA_DIMENSIONS", "MDSCHEMA_CUBES",
                        "MDSCHEMA_ACTIONS", "DBSCHEMA_TABLES", "DISCOVER_LITERALS", "DISCOVER_KEYWORDS",
                        "DISCOVER_ENUMERATORS", "DISCOVER_SCHEMA_ROWSETS", "DISCOVER_PROPERTIES", "DBSCHEMA_CATALOGS",
                        "DISCOVER_DATASOURCES", "DISCOVER_XML_METADATA", "DISCOVER_CSDL_METADATA", "DBSCHEMA_COLUMNS",
                        "DBSCHEMA_PROVIDER_TYPES", "DBSCHEMA_SCHEMATA", "DBSCHEMA_SOURCE_TABLES",
                        "DBSCHEMA_TABLES_INFO", "MDSCHEMA_HIERARCHIES", "MDSCHEMA_LEVELS",
                        "MDSCHEMA_MEASUREGROUP_DIMENSIONS", "MDSCHEMA_MEASURES", "MDSCHEMA_MEMBERS",
                        "MDSCHEMA_PROPERTIES", "MDSCHEMA_SETS", "MDSCHEMA_KPIS", "MDSCHEMA_MEASUREGROUPS");
    }

    @Test
    void discoverSchemaRowsetsWithRestriction() {
        DiscoverSchemaRowsetsRequest request = mock(DiscoverSchemaRowsetsRequest.class);
        DiscoverSchemaRowsetsRestrictions restrictions = mock(DiscoverSchemaRowsetsRestrictions.class);
        when(restrictions.schemaName()).thenReturn(Optional.of("DISCOVER_SCHEMA_ROWSETS"));
        when(request.restrictions()).thenReturn(restrictions);

        List<DiscoverSchemaRowsetsResponseRow> rows = service.discoverSchemaRowsets(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(1);
        assertThat(rows.get(0)).isNotNull();
        assertThat(rows.get(0).schemaName()).isEqualTo("DISCOVER_SCHEMA_ROWSETS");
    }

    @Test
    void xmlMetaData() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog1));
        DiscoverXmlMetaDataRequest request = mock(DiscoverXmlMetaDataRequest.class);
        DiscoverXmlMetaDataRestrictions restrictions = mock(DiscoverXmlMetaDataRestrictions.class);
        when(restrictions.databaseId()).thenReturn(Optional.of("foo"));
        when(request.restrictions()).thenReturn(restrictions);
        List<DiscoverXmlMetaDataResponseRow> rows = service.xmlMetaData(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(1);
        assertThat(rows.get(0)).isNotNull();
        assertThat(rows.get(0).metaData()).isNotEmpty();
        assertThat(rows.get(0).metaData()).contains("<Server>");
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
}
