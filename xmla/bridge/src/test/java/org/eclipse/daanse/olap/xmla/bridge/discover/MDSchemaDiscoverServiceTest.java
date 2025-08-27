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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.ContextGroup;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.KPI;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.LevelType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.result.Property;
import org.eclipse.daanse.olap.xmla.bridge.ActionService;
import org.eclipse.daanse.olap.xmla.bridge.ContextsSupplyerImpl;
import org.eclipse.daanse.xmla.api.RequestMetaData;
import org.eclipse.daanse.xmla.api.common.enums.DimensionCardinalityEnum;
import org.eclipse.daanse.xmla.api.common.enums.DimensionUniqueSettingEnum;
import org.eclipse.daanse.xmla.api.common.enums.HierarchyOriginEnum;
import org.eclipse.daanse.xmla.api.common.enums.LevelDbTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.LevelTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.MeasureAggregatorEnum;
import org.eclipse.daanse.xmla.api.common.enums.MemberTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.OriginEnum;
import org.eclipse.daanse.xmla.api.common.enums.PropertyContentTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.PropertyTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.ScopeEnum;
import org.eclipse.daanse.xmla.api.common.enums.SetEvaluationContextEnum;
import org.eclipse.daanse.xmla.api.common.enums.StructureEnum;
import org.eclipse.daanse.xmla.api.discover.Properties;
import org.eclipse.daanse.xmla.api.discover.mdschema.actions.MdSchemaActionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.actions.MdSchemaActionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.actions.MdSchemaActionsRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.cubes.MdSchemaCubesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.cubes.MdSchemaCubesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.cubes.MdSchemaCubesRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.demensions.MdSchemaDimensionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.demensions.MdSchemaDimensionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.demensions.MdSchemaDimensionsRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.functions.MdSchemaFunctionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.functions.MdSchemaFunctionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.functions.MdSchemaFunctionsRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.hierarchies.MdSchemaHierarchiesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.hierarchies.MdSchemaHierarchiesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.hierarchies.MdSchemaHierarchiesRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.kpis.MdSchemaKpisRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.kpis.MdSchemaKpisResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.kpis.MdSchemaKpisRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.levels.MdSchemaLevelsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.levels.MdSchemaLevelsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.levels.MdSchemaLevelsRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroups.MdSchemaMeasureGroupsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroups.MdSchemaMeasureGroupsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroups.MdSchemaMeasureGroupsRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.measures.MdSchemaMeasuresRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measures.MdSchemaMeasuresResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measures.MdSchemaMeasuresRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.members.MdSchemaMembersRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.members.MdSchemaMembersResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.members.MdSchemaMembersRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.properties.MdSchemaPropertiesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.properties.MdSchemaPropertiesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.properties.MdSchemaPropertiesRestrictions;
import org.eclipse.daanse.xmla.api.discover.mdschema.sets.MdSchemaSetsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.sets.MdSchemaSetsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.sets.MdSchemaSetsRestrictions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class MDSchemaDiscoverServiceTest {

    @Mock
    private Context context1;

    @Mock
    private Catalog catalog1;

    @Mock
    private Catalog catalog2;

    @Mock
    private KPI kpi1;

    @Mock
    private KPI kpi2;

    @Mock
    private Cube cube1;

    @Mock
    private Cube cube2;

    @Mock
    private Dimension dimension1;

    @Mock
    private Dimension dimension2;

    @Mock
    private Hierarchy hierarchy1;

    @Mock
    private Hierarchy hierarchy2;

    @Mock
    private Level level1;

    @Mock
    private Level level2;

    @Mock
    private Member measure1;

    @Mock
    private Member measure2;

    @Mock
    private ContextGroup contextGroup;

    @Mock
    private RequestMetaData requestMetaData;

    private MDSchemaDiscoverService service;

    @Mock
    private ContextsSupplyerImpl cls;

    @Mock
    private ActionService actionService;

    @BeforeEach
    void setup() {
        /*
         * empty list, but override with: when(cls.get()).thenReturn(List.of(context1,context2));`
         */

        service = new MDSchemaDiscoverService(cls, actionService);
    }

    @Test
    void mdSchemaActions() {
        MdSchemaActionsRequest request = mock(MdSchemaActionsRequest.class);
        MdSchemaActionsRestrictions restrictions = mock(MdSchemaActionsRestrictions.class);
        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));
        List<MdSchemaActionsResponseRow> rows = service.mdSchemaActions(request, requestMetaData);
        assertThat(rows).isNotNull();
    }

    @Test
    void mdSchemaCubes() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));

        MdSchemaCubesRequest request = mock(MdSchemaCubesRequest.class);
        MdSchemaCubesRestrictions restrictions = mock(MdSchemaCubesRestrictions.class);
        Properties properties = mock(Properties.class);
        when(properties.catalog()).thenReturn(Optional.empty());
        when(request.restrictions()).thenReturn(restrictions);
        when(request.properties()).thenReturn(properties);
        when(restrictions.catalogName()).thenReturn("foo");

        when(catalog2.getName()).thenReturn("schema2Name");

        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");
        when(cube2.getDescription()).thenReturn("cube2description");
        when(cube1.isVisible()).thenReturn(true);
        when(cube2.isVisible()).thenReturn(true);

        when(catalog2.getCubes()).thenAnswer(setupDummyListAnswer(cube1, cube2));

        List<MdSchemaCubesResponseRow> rows = service.mdSchemaCubes(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(2);

        MdSchemaCubesResponseRow row = rows.get(0);
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains("schema2Name");
        assertThat(row.schemaName()).isEmpty();
        assertThat(row.description()).contains("schema2Name Schema - cube1Name Cube");

        row = rows.get(1);
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains("schema2Name");
        assertThat(row.schemaName()).isEmpty();
        assertThat(row.description()).contains("cube2description");
    }

    @Test
    void mdSchemaDimensions() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));

        MdSchemaDimensionsRequest request = mock(MdSchemaDimensionsRequest.class);
        MdSchemaDimensionsRestrictions restrictions = mock(MdSchemaDimensionsRestrictions.class);
        Properties properties = mock(Properties.class);

        when(properties.deep()).thenReturn(Optional.of(true));

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));
        when(request.properties()).thenReturn(properties);

        when(catalog2.getName()).thenReturn("schema2Name");

        when(hierarchy1.getUniqueName()).thenReturn("hierarchy1UniqueName");
        when(hierarchy2.getUniqueName()).thenReturn("hierarchy2UniqueName");
        when(hierarchy1.getName()).thenReturn("hierarchy1Name");
        when(hierarchy2.getName()).thenReturn("hierarchy2Name");
        when(hierarchy1.getLevels()).thenAnswer(setupDummyListAnswer(level1));
        when(hierarchy2.getLevels()).thenAnswer(setupDummyListAnswer(level1));
        when(level1.getLevelType()).thenReturn(LevelType.REGULAR);

        when(dimension1.getName()).thenReturn("dimension1Name");
        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");
        when(dimension1.getCaption()).thenReturn("dimension1Caption");
        when(dimension1.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));

        when(dimension2.getName()).thenReturn("dimension2Name");
        when(dimension2.getUniqueName()).thenReturn("dimension2UniqueName");
        when(dimension2.getCaption()).thenReturn("dimension2Caption");
        when(dimension2.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));

        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");
        when(cube1.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));
        when(cube2.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));

        Member member = mock(Member.class);
        when(cube1.getLevelMembers(any(), eq(true))).thenAnswer(setupDummyListAnswer(member));

        when(catalog2.getCubes()).thenAnswer(setupDummyListAnswer(cube1, cube2));

        List<MdSchemaDimensionsResponseRow> rows = service.mdSchemaDimensions(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(4);
        checkMdSchemaDimensionsResponseRow(rows.get(0), "schema2Name", "schema2Name", "cube1Name", "dimension1Name",
                "dimension1UniqueName", "dimension1Caption", 0, 1, "hierarchy1UniqueName",
                "cube1Name Cube - dimension1Name Dimension");

        checkMdSchemaDimensionsResponseRow(rows.get(1), "schema2Name", "schema2Name", "cube2Name", "dimension1Name",
                "dimension1UniqueName", "dimension1Caption", 0, 1, "hierarchy1UniqueName",
                "cube2Name Cube - dimension1Name Dimension");

        checkMdSchemaDimensionsResponseRow(rows.get(2), "schema2Name", "schema2Name", "cube1Name", "dimension2Name",
                "dimension2UniqueName", "dimension2Caption", 1, 1, "hierarchy1UniqueName",
                "cube1Name Cube - dimension2Name Dimension");

        checkMdSchemaDimensionsResponseRow(rows.get(3), "schema2Name", "schema2Name", "cube2Name", "dimension2Name",
                "dimension2UniqueName", "dimension2Caption", 1, 1, "hierarchy1UniqueName",
                "cube2Name Cube - dimension2Name Dimension");

    }

    @Test
    void mdSchemaFunctions() {
        when(cls.getContexts()).thenReturn(List.of(context1));

        MdSchemaFunctionsRequest request = mock(MdSchemaFunctionsRequest.class);
        MdSchemaFunctionsRestrictions restrictions = mock(MdSchemaFunctionsRestrictions.class);
        FunctionService functionService = mock(FunctionService.class);
        OperationAtom functionAtom1 = new FunctionOperationAtom("functionAtom1Name");
        OperationAtom functionAtom2 = new MethodOperationAtom("functionAtom2Name");
        FunctionMetaData functionMetaData1 = mock(FunctionMetaData.class);
        FunctionMetaData functionMetaData2 = mock(FunctionMetaData.class);
        when(functionMetaData1.parameterDataTypes())
                .thenAnswer(setupDummyArrayAnswer(DataType.INTEGER, DataType.MEMBER));
        when(functionMetaData1.returnCategory()).thenReturn(DataType.INTEGER);
        when(functionMetaData1.description()).thenReturn("functionMetaData1Description");
        when(functionMetaData2.parameterDataTypes()).thenAnswer(setupDummyArrayAnswer(DataType.CUBE));
        when(functionMetaData2.returnCategory()).thenReturn(DataType.MEMBER);
        when(functionMetaData2.description()).thenReturn("functionMetaData2Description");

        when(functionMetaData1.operationAtom()).thenReturn(functionAtom1);
        when(functionMetaData2.operationAtom()).thenReturn(functionAtom2);

        when(functionService.getFunctionMetaDatas())
                .thenAnswer(setupDummyListAnswer(functionMetaData1, functionMetaData2));

        when(request.restrictions()).thenReturn(restrictions);
        when(context1.getFunctionService()).thenReturn(functionService);
        List<MdSchemaFunctionsResponseRow> rows = service.mdSchemaFunctions(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(2);
        checkMdSchemaFunctionsResponseRow(rows.get(0), "functionAtom1Name", "functionMetaData1Description",
                "Integer, Member", 2, OriginEnum.MSOLAP, "functionAtom1Name");
        checkMdSchemaFunctionsResponseRow(rows.get(1), "functionAtom2Name", "functionMetaData2Description", "Cube", 12,
                OriginEnum.MSOLAP, "functionAtom2Name");

    }

    @Test
    void mdSchemaHierarchies() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));

        MdSchemaHierarchiesRequest request = mock(MdSchemaHierarchiesRequest.class);
        MdSchemaHierarchiesRestrictions restrictions = mock(MdSchemaHierarchiesRestrictions.class);
        Properties properties = mock(Properties.class);

        when(properties.deep()).thenReturn(Optional.of(true));

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("schema2Name"));
        when(request.properties()).thenReturn(properties);

        when(catalog2.getName()).thenReturn("schema2Name");

        when(hierarchy1.getUniqueName()).thenReturn("hierarchy1UniqueName");
        when(hierarchy2.getUniqueName()).thenReturn("hierarchy2UniqueName");
        when(hierarchy1.getName()).thenReturn("hierarchy1Name");
        when(hierarchy2.getName()).thenReturn("hierarchy2Name");
        when(hierarchy1.getLevels()).thenAnswer(setupDummyListAnswer(level1));
        when(hierarchy2.getLevels()).thenAnswer(setupDummyListAnswer(level1));
        when(level1.getLevelType()).thenReturn(LevelType.REGULAR);

        when(dimension1.getName()).thenReturn("dimension1Name");
        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");
        when(dimension1.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));

        when(dimension2.getName()).thenReturn("dimension2Name");
        when(dimension2.getUniqueName()).thenReturn("dimension2UniqueName");
        when(dimension2.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));

        Member member = mock(Member.class);
        when(cube1.getLevelMembers(any(), eq(true))).thenAnswer(setupDummyListAnswer(member));
        when(cube1.getLevelCardinality(any(), eq(true), eq(true))).thenReturn(1);
        when(cube2.getLevelMembers(any(), eq(true))).thenAnswer(setupDummyListAnswer(member));
        when(cube2.getLevelCardinality(any(), eq(true), eq(true))).thenReturn(1);

        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");
        when(cube1.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));
        when(cube2.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));

        when(catalog2.getCubes()).thenAnswer(setupDummyListAnswer(cube1, cube2));

        when(catalog2.getName()).thenReturn("schema2Name");

        List<MdSchemaHierarchiesResponseRow> rows = service.mdSchemaHierarchies(request, requestMetaData);
        verify(catalog2, times(3)).getName();
        assertThat(rows).isNotNull().hasSize(8);

        checkMdSchemaHierarchiesResponseRow(rows.get(0), "schema2Name", "schema2Name", "cube1Name",
                "dimension1UniqueName", "cube1Name Cube - dimension1Name.hierarchy1Name Hierarchy", "hierarchy1Name", 0,
                "hierarchy1UniqueName");
        checkMdSchemaHierarchiesResponseRow(rows.get(1), "schema2Name", "schema2Name", "cube1Name",
                "dimension2UniqueName", "cube1Name Cube - dimension2Name.hierarchy1Name Hierarchy", "hierarchy1Name", 2,
                "hierarchy1UniqueName");
        checkMdSchemaHierarchiesResponseRow(rows.get(2), "schema2Name", "schema2Name", "cube2Name",
                "dimension1UniqueName", "cube2Name Cube - dimension1Name.hierarchy1Name Hierarchy", "hierarchy1Name", 0,
                "hierarchy1UniqueName");
        checkMdSchemaHierarchiesResponseRow(rows.get(3), "schema2Name", "schema2Name", "cube2Name",
                "dimension2UniqueName", "cube2Name Cube - dimension2Name.hierarchy1Name Hierarchy", "hierarchy1Name", 2,
                "hierarchy1UniqueName");

        checkMdSchemaHierarchiesResponseRow(rows.get(4), "schema2Name", "schema2Name", "cube1Name",
                "dimension1UniqueName", "cube1Name Cube - dimension1Name.hierarchy2Name Hierarchy", "hierarchy2Name", 1,
                "hierarchy2UniqueName");
        checkMdSchemaHierarchiesResponseRow(rows.get(5), "schema2Name", "schema2Name", "cube1Name",
                "dimension2UniqueName", "cube1Name Cube - dimension2Name.hierarchy2Name Hierarchy", "hierarchy2Name", 3,
                "hierarchy2UniqueName");
        checkMdSchemaHierarchiesResponseRow(rows.get(6), "schema2Name", "schema2Name", "cube2Name",
                "dimension1UniqueName", "cube2Name Cube - dimension1Name.hierarchy2Name Hierarchy", "hierarchy2Name", 1,
                "hierarchy2UniqueName");
        checkMdSchemaHierarchiesResponseRow(rows.get(7), "schema2Name", "schema2Name", "cube2Name",
                "dimension2UniqueName", "cube2Name Cube - dimension2Name.hierarchy2Name Hierarchy", "hierarchy2Name", 3,
                "hierarchy2UniqueName");
    }

    @Test
    void mdSchemaKpis() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));

        MdSchemaKpisRequest request = mock(MdSchemaKpisRequest.class);
        MdSchemaKpisRestrictions restrictions = mock(MdSchemaKpisRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));

        when(catalog2.getName()).thenReturn("schema2Name");

        when(catalog2.getCubes()).thenAnswer(setupDummyListAnswer(cube1, cube2));

        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");

        when(cube1.getKPIs()).thenAnswer(setupDummyListAnswer(kpi1, kpi2));
        when(cube2.getKPIs()).thenAnswer(setupDummyListAnswer(kpi1, kpi2));

        when(kpi1.getName()).thenReturn("kpi1Name");
        // when(mappingKpi1.getCaption()).thenReturn("kpi1Caption");
        when(kpi1.getDescription()).thenReturn("kpi1Description");
        when(kpi1.getDisplayFolder()).thenReturn("kpi1DisplayFolder");

        when(kpi1.getValue()).thenReturn("kpi1Value");
        when(kpi1.getGoal()).thenReturn("kpi1Goal");
        when(kpi1.getStatus()).thenReturn("kpi1Status");
        when(kpi1.getTrend()).thenReturn("kpi1Trend");
        when(kpi1.getWeight()).thenReturn("kpi1Weight");
        when(kpi1.getTrendGraphic()).thenReturn("kpi1TrendGraphic");
        when(kpi1.getStatusGraphic()).thenReturn("kpi1StatusGraphic");
        when(kpi1.getCurrentTimeMember()).thenReturn("kpi1CurrentTimeMember");

        when(kpi2.getName()).thenReturn("kpi2Name");

        // when(mappingKpi2.getCaption()).thenReturn("kpi2caption");
        when(kpi2.getDescription()).thenReturn("kpi2Description");
        when(kpi2.getDisplayFolder()).thenReturn("kpi2DisplayFolder");

        when(kpi2.getValue()).thenReturn("kpi2Value");
        when(kpi2.getGoal()).thenReturn("kpi2Goal");
        when(kpi2.getStatus()).thenReturn("kpi2Status");
        when(kpi2.getTrend()).thenReturn("kpi2Trend");
        when(kpi2.getWeight()).thenReturn("kpi2Weight");
        when(kpi2.getTrendGraphic()).thenReturn("kpi2TrendGraphic");
        when(kpi2.getStatusGraphic()).thenReturn("kpi2StatusGraphic");
        when(kpi2.getCurrentTimeMember()).thenReturn("kpi2CurrentTimeMember");
        when(kpi2.getParentKpi()).thenReturn(kpi1);

        when(catalog2.getName()).thenReturn("foo");

        List<MdSchemaKpisResponseRow> rows = service.mdSchemaKpis(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(4);
        checkMdSchemaKpisResponseRow(rows.get(1), "foo", "cube1Name", "cube1Name", "kpi2Name", "kpi2Name",
                "kpi2Description", "kpi2DisplayFolder", "kpi2Value", "kpi2Goal", "kpi2Status", "kpi2Trend",
                "kpi2StatusGraphic", "kpi2TrendGraphic", "kpi2Weight", "kpi2CurrentTimeMember", "kpi1Name",
                ScopeEnum.GLOBAL);
    }

    @Test
    void mdSchemaLevels() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));

        MdSchemaLevelsRequest request = mock(MdSchemaLevelsRequest.class);
        MdSchemaLevelsRestrictions restrictions = mock(MdSchemaLevelsRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));

        when(catalog2.getName()).thenReturn("schema2Name");

        when(level1.getName()).thenReturn("level1Name");
        when(level2.getName()).thenReturn("level2Name");
        when(level1.getUniqueName()).thenReturn("level1UniqueName");
        when(level2.getUniqueName()).thenReturn("level2UniqueName");
        when(level1.isAll()).thenReturn(true);
        when(level2.isAll()).thenReturn(true);
        when(level1.getCaption()).thenReturn("level1Caption");
        when(level2.getCaption()).thenReturn("level2Caption");
        when(level2.getDescription()).thenReturn("level2Description");

        when(hierarchy1.getName()).thenReturn("hierarchy1Name");
        when(hierarchy2.getName()).thenReturn("hierarchy2Name");
        when(hierarchy1.getUniqueName()).thenReturn("hierarchy1UniqueName");
        when(hierarchy2.getUniqueName()).thenReturn("hierarchy2UniqueName");

        when(hierarchy1.getLevels()).thenAnswer(setupDummyListAnswer(level1, level2));
        when(hierarchy2.getLevels()).thenAnswer(setupDummyListAnswer(level1, level2));

        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");
        when(dimension1.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));

        when(dimension2.getUniqueName()).thenReturn("dimension2UniqueName");
        when(dimension2.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));

        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");
        when(cube1.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));
        when(cube2.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));
        when(cube1.getLevelCardinality(any(), eq(true), eq(true))).thenReturn(1);
        when(cube2.getLevelCardinality(any(), eq(true), eq(true))).thenReturn(1);

        when(catalog2.getCubes()).thenAnswer(setupDummyListAnswer(cube1, cube2));

        List<MdSchemaLevelsResponseRow> rows = service.mdSchemaLevels(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(16);
        checkMdSchemaLevelsResponseRow(rows.get(0), "schema2Name", "cube1Name", "dimension1UniqueName",
                "hierarchy1UniqueName", "level1Name", "level1UniqueName", "level1Caption", 0, 1, LevelTypeEnum.ALL,
                "cube1Name Cube - dimension1UniqueName.hierarchy1Name Hierarchy - level1Name Level");
        checkMdSchemaLevelsResponseRow(rows.get(1), "schema2Name", "cube1Name", "dimension1UniqueName",
                "hierarchy1UniqueName", "level2Name", "level2UniqueName", "level2Caption", 0, 1, LevelTypeEnum.ALL,
                "level2Description");
        checkMdSchemaLevelsResponseRow(rows.get(15), "schema2Name", "cube2Name", "dimension2UniqueName",
                "hierarchy2UniqueName", "level2Name", "level2UniqueName", "level2Caption", 0, 1, LevelTypeEnum.ALL,
                "level2Description");

    }

    @Test
    void mdSchemaMeasureGroupDimensions() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));

        MdSchemaMeasureGroupDimensionsRequest request = mock(MdSchemaMeasureGroupDimensionsRequest.class);
        MdSchemaMeasureGroupDimensionsRestrictions restrictions = mock(
                MdSchemaMeasureGroupDimensionsRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));

        // when(catalog1.getName()).thenReturn("schema1Name");

        when(catalog2.getName()).thenReturn("schema2Name");

        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");
        when(dimension2.getUniqueName()).thenReturn("dimension2UniqueName");

        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");
        when(cube1.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));
        when(cube2.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));

        when(catalog2.getCubes()).thenAnswer(setupDummyListAnswer(cube1, cube2));

        List<MdSchemaMeasureGroupDimensionsResponseRow> rows = service.mdSchemaMeasureGroupDimensions(request,
                requestMetaData);
        assertThat(rows).isNotNull().hasSize(4);
        checkMdSchemaMeasureGroupDimensionsResponseRow(rows.get(0), "schema2Name", "cube1Name", "ONE",
                "dimension1UniqueName", DimensionCardinalityEnum.MANY, false, false);
        checkMdSchemaMeasureGroupDimensionsResponseRow(rows.get(3), "schema2Name", "cube2Name", "ONE",
                "dimension2UniqueName", DimensionCardinalityEnum.MANY, false, false);
    }

    @Test
    void mdSchemaMeasureGroups() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));

        MdSchemaMeasureGroupsRequest request = mock(MdSchemaMeasureGroupsRequest.class);
        MdSchemaMeasureGroupsRestrictions restrictions = mock(MdSchemaMeasureGroupsRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));

        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");

        when(catalog2.getName()).thenReturn("foo");

        when(catalog2.getCubes()).thenAnswer(setupDummyListAnswer(cube1, cube2));

        List<MdSchemaMeasureGroupsResponseRow> rows = service.mdSchemaMeasureGroups(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(2);
        checkMdSchemaMeasureGroupsResponseRow(rows.get(0), "foo", "cube1Name", "", false);
        checkMdSchemaMeasureGroupsResponseRow(rows.get(1), "foo", "cube2Name", "", false);
    }

    @Test
    void mdSchemaMeasures() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));

        MdSchemaMeasuresRequest request = mock(MdSchemaMeasuresRequest.class);
        MdSchemaMeasuresRestrictions restrictions = mock(MdSchemaMeasuresRestrictions.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));

        when(catalog2.getName()).thenReturn("schema2Name");

        when(dimension1.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));

        when(dimension2.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));

        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");
        when(measure1.getName()).thenReturn("measure1Name");
        when(measure2.getName()).thenReturn("measure2Name");
        when(measure1.getUniqueName()).thenReturn("measure1UniqueName");
        when(measure2.getUniqueName()).thenReturn("measure2UniqueName");
        when(measure1.getCaption()).thenReturn("measure1Caption");
        when(measure2.getCaption()).thenReturn("measure2Caption");
        when(measure1.getPropertyValue(Property.StandardMemberProperty.$visible.getName())).thenReturn(Boolean.TRUE);
        when(measure2.getPropertyValue(Property.StandardMemberProperty.$visible.getName())).thenReturn(Boolean.TRUE);
        when(measure1.getPropertyValue(Property.StandardCellProperty.FORMAT_STRING.getName()))
                .thenReturn("formatString1");
        when(measure2.getPropertyValue(Property.StandardCellProperty.FORMAT_STRING.getName()))
                .thenReturn("formatString2");

        when(cube1.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));
        when(cube2.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));
        when(cube1.getMeasures()).thenAnswer(setupDummyListAnswer(measure1, measure2));
        when(cube2.getMeasures()).thenAnswer(setupDummyListAnswer(measure1, measure2));

        when(catalog2.getCubes()).thenAnswer(setupDummyListAnswer(cube1, cube2));

        List<MdSchemaMeasuresResponseRow> rows = service.mdSchemaMeasures(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(4);
        checkMdSchemaMeasuresResponseRow(rows.get(0), "schema2Name", "schema2Name", "cube1Name", "measure1Name",
                "measure1UniqueName", "measure1Caption", MeasureAggregatorEnum.MDMEASURE_AGGR_UNKNOWN,
                LevelDbTypeEnum.DBTYPE_WSTR, "cube1Name Cube - measure1Name Member", true, "", "", "formatString1");
        checkMdSchemaMeasuresResponseRow(rows.get(1), "schema2Name", "schema2Name", "cube1Name", "measure2Name",
                "measure2UniqueName", "measure2Caption", MeasureAggregatorEnum.MDMEASURE_AGGR_UNKNOWN,
                LevelDbTypeEnum.DBTYPE_WSTR, "cube1Name Cube - measure2Name Member", true, "", "", "formatString2");

        checkMdSchemaMeasuresResponseRow(rows.get(2), "schema2Name", "schema2Name", "cube2Name", "measure1Name",
                "measure1UniqueName", "measure1Caption", MeasureAggregatorEnum.MDMEASURE_AGGR_UNKNOWN,
                LevelDbTypeEnum.DBTYPE_WSTR, "cube2Name Cube - measure1Name Member", true, "", "", "formatString1");
        checkMdSchemaMeasuresResponseRow(rows.get(3), "schema2Name", "schema2Name", "cube2Name", "measure2Name",
                "measure2UniqueName", "measure2Caption", MeasureAggregatorEnum.MDMEASURE_AGGR_UNKNOWN,
                LevelDbTypeEnum.DBTYPE_WSTR, "cube2Name Cube - measure2Name Member", true, "", "", "formatString2");
    }

    @Test
    void mdSchemaMembers() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));

        MdSchemaMembersRequest request = mock(MdSchemaMembersRequest.class);
        MdSchemaMembersRestrictions restrictions = mock(MdSchemaMembersRestrictions.class);
        Properties properties = mock(Properties.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(request.properties()).thenReturn(properties);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));
        when(restrictions.memberUniqueName()).thenReturn(Optional.of("memberUniqueName"));

        when(catalog2.getName()).thenReturn("schema2Name");

        when(level1.getUniqueName()).thenReturn("level1UniqueName");

        when(hierarchy1.getUniqueName()).thenReturn("hierarchy1UniqueName");

        when(hierarchy1.getDimension()).thenReturn(dimension1);

        when(hierarchy1.getLevels()).thenAnswer(setupDummyListAnswer(level1, level2));
        when(hierarchy2.getLevels()).thenAnswer(setupDummyListAnswer(level1, level2));

        when(dimension1.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));
        when(dimension2.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));
        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");

        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");

        when(cube1.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));
        when(cube2.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));
        Member member = mock(Member.class);
        when(member.getUniqueName()).thenReturn("memberUniqueName");
        when(member.getName()).thenReturn("measure1Name");
        when(member.getDescription()).thenReturn("measure1Description");
        when(member.getCaption()).thenReturn("measure1Caption");

        when(member.getLevel()).thenReturn(level1);
        when(cube1.getLevelMembers(any(), eq(true))).thenAnswer(setupDummyListAnswer(member));
        when(cube2.getLevelMembers(any(), eq(true))).thenAnswer(setupDummyListAnswer(member));

        when(level1.getHierarchy()).thenReturn(hierarchy1);

        when(catalog2.getCubes()).thenAnswer(setupDummyListAnswer(cube1, cube2));

        List<MdSchemaMembersResponseRow> rows = service.mdSchemaMembers(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(16);
        checkMdSchemaMembersResponseRow(rows.get(0), "schema2Name", Optional.empty(), "cube1Name",
                "dimension1UniqueName", "hierarchy1UniqueName", "level1UniqueName", 0, 0, "measure1Name",
                "memberUniqueName", MemberTypeEnum.REGULAR_MEMBER, "measure1Caption", 100, 0, Optional.empty(), 0,
                "measure1Description");
        checkMdSchemaMembersResponseRow(rows.get(15), "schema2Name", Optional.empty(), "cube2Name",
                "dimension1UniqueName", "hierarchy1UniqueName", "level1UniqueName", 0, 0, "measure1Name",
                "memberUniqueName", MemberTypeEnum.REGULAR_MEMBER, "measure1Caption", 100, 0, Optional.empty(), 0,
                "measure1Description");

    }

    @Test
    void mdSchemaProperties() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));

        MdSchemaPropertiesRequest request = mock(MdSchemaPropertiesRequest.class);
        MdSchemaPropertiesRestrictions restrictions = mock(MdSchemaPropertiesRestrictions.class);
        org.eclipse.daanse.olap.common.AbstractProperty property1 = mock(
                org.eclipse.daanse.olap.common.AbstractProperty.class);
        org.eclipse.daanse.olap.common.AbstractProperty property2 = mock(
                org.eclipse.daanse.olap.common.AbstractProperty.class);

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));

        when(property1.getName()).thenReturn("property1Name");
        when(property2.getName()).thenReturn("property2Name");
        when(property1.getCaption()).thenReturn("property1Caption");
        when(property2.getCaption()).thenReturn("property2Caption");

        when(catalog2.getName()).thenReturn("schema2Name");

        when(level1.getUniqueName()).thenReturn("level1UniqueName");
        when(level1.getHierarchy()).thenReturn(hierarchy1);
        when(level1.getProperties()).thenAnswer(setupDummyArrayAnswer(property1, property2));
        when(level1.getName()).thenReturn("level1Name");

        when(level2.getUniqueName()).thenReturn("level2UniqueName");
        when(level2.getHierarchy()).thenReturn(hierarchy2);
        when(level2.getProperties()).thenAnswer(setupDummyArrayAnswer(property1, property2));
        when(level2.getName()).thenReturn("level2Name");

        when(hierarchy1.getName()).thenReturn("hierarchy1Name");
        when(hierarchy1.getUniqueName()).thenReturn("hierarchy1UniqueName");
        when(hierarchy1.getDimension()).thenReturn(dimension1);
        when(hierarchy1.getLevels()).thenAnswer(setupDummyListAnswer(level1, level2));

        when(hierarchy2.getName()).thenReturn("hierarchy2Name");
        when(hierarchy2.getUniqueName()).thenReturn("hierarchy2UniqueName");
        when(hierarchy2.getDimension()).thenReturn(dimension2);
        when(hierarchy2.getLevels()).thenAnswer(setupDummyListAnswer(level1, level2));

        when(dimension1.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));
        when(dimension2.getHierarchies()).thenAnswer(setupDummyListAnswer(hierarchy1, hierarchy2));
        when(dimension1.getName()).thenReturn("dimension1Name");
        when(dimension2.getName()).thenReturn("dimension2Name");
        when(dimension1.getUniqueName()).thenReturn("dimension1UniqueName");
        when(dimension2.getUniqueName()).thenReturn("dimension2UniqueName");

        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");

        when(cube1.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));
        when(cube2.getDimensions()).thenAnswer(setupDummyListAnswer(dimension1, dimension2));

        when(catalog2.getCubes()).thenAnswer(setupDummyListAnswer(cube1, cube2));

        List<MdSchemaPropertiesResponseRow> rows = service.mdSchemaProperties(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(32);
        checkMdSchemaPropertiesResponseRow(rows.get(0), "schema2Name", "cube1Name", "dimension1UniqueName",
                "hierarchy1UniqueName", "level1UniqueName", PropertyTypeEnum.PROPERTY_MEMBER, "property1Name",
                "property1Caption", LevelDbTypeEnum.DBTYPE_WSTR,
                "cube1Name Cube - dimension1Name.hierarchy1Name Hierarchy - level1Name Level - property1Name Property",
                PropertyContentTypeEnum.REGULAR);
        checkMdSchemaPropertiesResponseRow(rows.get(31), "schema2Name", "cube2Name", "dimension2UniqueName",
                "hierarchy2UniqueName", "level2UniqueName", PropertyTypeEnum.PROPERTY_MEMBER, "property2Name",
                "property2Caption", LevelDbTypeEnum.DBTYPE_WSTR,
                "cube2Name Cube - dimension2Name.hierarchy2Name Hierarchy - level2Name Level - property2Name Property",
                PropertyContentTypeEnum.REGULAR);

    }

    @Test
    void mdSchemaSets() {
        when(cls.tryGetFirstByName(any(), any())).thenReturn(Optional.of(catalog2));

        MdSchemaSetsRequest request = mock(MdSchemaSetsRequest.class);
        MdSchemaSetsRestrictions restrictions = mock(MdSchemaSetsRestrictions.class);
        NamedSet namedSet1 = mock(NamedSet.class);
        NamedSet namedSet2 = mock(NamedSet.class);
        when(namedSet1.getName()).thenReturn("set1Name");
        when(namedSet2.getName()).thenReturn("set2Name");
        when(namedSet1.getDescription()).thenReturn("set1Description");
        when(namedSet2.getDescription()).thenReturn("set2Description");
        when(namedSet1.getCaption()).thenReturn("set1Caption");
        when(namedSet2.getCaption()).thenReturn("set2Caption");

        when(request.restrictions()).thenReturn(restrictions);
        when(restrictions.catalogName()).thenReturn(Optional.of("foo"));

        when(catalog2.getName()).thenReturn("schema2Name");

        when(cube1.getName()).thenReturn("cube1Name");
        when(cube2.getName()).thenReturn("cube2Name");

        when(cube1.getNamedSets()).thenAnswer(setupDummyArrayAnswer(namedSet1, namedSet2));
        when(cube2.getNamedSets()).thenAnswer(setupDummyArrayAnswer(namedSet1, namedSet2));

        when(catalog2.getCubes()).thenAnswer(setupDummyListAnswer(cube1, cube2));

        List<MdSchemaSetsResponseRow> rows = service.mdSchemaSets(request, requestMetaData);
        assertThat(rows).isNotNull().hasSize(4);
        checkMdSchemaSetsResponseRow(rows.get(0), "schema2Name", "cube1Name", "set1Name", ScopeEnum.GLOBAL,
                "set1Description", Optional.empty(), "", "set1Caption", Optional.empty(),
                SetEvaluationContextEnum.STATIC);
        checkMdSchemaSetsResponseRow(rows.get(3), "schema2Name", "cube2Name", "set2Name", ScopeEnum.GLOBAL,
                "set2Description", Optional.empty(), "", "set2Caption", Optional.empty(),
                SetEvaluationContextEnum.STATIC);
    }

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

    private void checkMdSchemaDimensionsResponseRow(MdSchemaDimensionsResponseRow row, String catalogName,
            String schemaName, String cubeName, String dimensionName, String dimensionUniqueName,
            String dimensionCaption, int dimensionOrdinal, int dimensionCardinality, String defaultHierarchy,
            String description) {
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains(catalogName);
        assertThat(row.schemaName()).contains(schemaName);
        assertThat(row.cubeName()).contains(cubeName);
        assertThat(row.dimensionName()).contains(dimensionName);
        assertThat(row.dimensionUniqueName()).contains(dimensionUniqueName);
        assertThat(row.dimensionCaption()).contains(dimensionCaption);
        assertThat(row.dimensionOptional()).contains(dimensionOrdinal);
        assertThat(row.dimensionType()).isEmpty();
        assertThat(row.dimensionCardinality()).contains(dimensionCardinality);
        assertThat(row.defaultHierarchy()).contains(defaultHierarchy);
        assertThat(row.description()).contains(description);
    }

    private void checkMdSchemaFunctionsResponseRow(MdSchemaFunctionsResponseRow row, String functionalName,
            String description, String parameterList, int returnType, OriginEnum origin, String caption) {
        assertThat(row).isNotNull();
        assertThat(row.functionalName()).contains(functionalName);
        assertThat(row.description()).contains(description);
        assertThat(row.parameterList()).contains(parameterList);
        assertThat(row.returnType()).contains(returnType);
        assertThat(row.origin()).contains(origin);
        assertThat(row.caption()).contains(caption);
    }

    private void checkMdSchemaHierarchiesResponseRow(MdSchemaHierarchiesResponseRow row, String catalogName,
            String schemaName, String cubeName, String dimensionUniqueName, String description, String hierarchyName,
            int hierarchyOrdinal, String hierarchyUniqueName) {
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains(catalogName);
        assertThat(row.schemaName()).contains(schemaName);
        assertThat(row.cubeName()).contains(cubeName);
        assertThat(row.dimensionUniqueName()).contains(dimensionUniqueName);
        assertThat(row.description()).contains(description);
        assertThat(row.dimensionIsShared()).contains(true);
        assertThat(row.dimensionIsVisible()).contains(false);
        assertThat(row.dimensionUniqueSettings()).contains(DimensionUniqueSettingEnum.NONE);
        assertThat(row.hierarchyCardinality()).contains(1);
        assertThat(row.hierarchyIsVisible()).contains(false);
        assertThat(row.hierarchyName()).contains(hierarchyName);
        assertThat(row.hierarchyOrdinal()).contains(hierarchyOrdinal);
        assertThat(row.hierarchyOrigin()).contains(HierarchyOriginEnum.USER_DEFINED);
        assertThat(row.hierarchyUniqueName()).contains(hierarchyUniqueName);
        assertThat(row.isReadWrite()).contains(false);
        assertThat(row.isVirtual()).contains(false);
        assertThat(row.structure()).contains(StructureEnum.HIERARCHY_FULLY_BALANCED);
    }

    private void checkMdSchemaKpisResponseRow(MdSchemaKpisResponseRow row, String catalogName, String cubeName,
            String measureGroupName, String kpiName, String kpiCaption, String kpiDescription, String kpiDisplayFolder,
            String kpiValue, String kpiGoal, String kpiStatus, String kpiTrend, String kpiStatusGraphic,
            String kpiTrendGraphic, String kpiWight, String kpiCurrentTimeMember, String kpiParentKpiName,
            ScopeEnum scope) {
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains(catalogName);
        assertThat(row.schemaName()).isEmpty();
        assertThat(row.cubeName()).contains(cubeName);
        assertThat(row.measureGroupName()).contains(measureGroupName);
        assertThat(row.kpiName()).contains(kpiName);
        assertThat(row.kpiCaption()).contains(kpiCaption);
        assertThat(row.kpiDescription()).contains(kpiDescription);
        assertThat(row.kpiDisplayFolder()).contains(kpiDisplayFolder);
        assertThat(row.kpiValue()).contains(kpiValue);
        assertThat(row.kpiGoal()).contains(kpiGoal);
        assertThat(row.kpiStatus()).contains(kpiStatus);
        assertThat(row.kpiTrend()).contains(kpiTrend);
        assertThat(row.kpiStatusGraphic()).contains(kpiStatusGraphic);
        assertThat(row.kpiTrendGraphic()).contains(kpiTrendGraphic);
        assertThat(row.kpiWight()).contains(kpiWight);
        assertThat(row.kpiCurrentTimeMember()).contains(kpiCurrentTimeMember);
        assertThat(row.kpiParentKpiName()).contains(kpiParentKpiName);
        assertThat(row.scope()).contains(scope);
    }

    private void checkMdSchemaLevelsResponseRow(MdSchemaLevelsResponseRow row, String catalogName, String cubeName,
            String dimensionUniqueName, String hierarchyUniqueName, String levelName, String levelUniqueName,
            String levelCaption, Integer levelNumber, Integer levelCardinality, LevelTypeEnum levelType,
            String description) {
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains(catalogName);
        assertThat(row.schemaName()).contains(catalogName);
        assertThat(row.cubeName()).contains(cubeName);
        assertThat(row.dimensionUniqueName()).contains(dimensionUniqueName);
        assertThat(row.hierarchyUniqueName()).contains(hierarchyUniqueName);
        assertThat(row.levelName()).contains(levelName);
        assertThat(row.levelUniqueName()).contains(levelUniqueName);
        assertThat(row.levelCaption()).contains(levelCaption);
        assertThat(row.levelNumber()).contains(levelNumber);
        assertThat(row.levelCardinality()).contains(levelCardinality);
        assertThat(row.levelType()).contains(levelType);
        assertThat(row.description()).contains(description);
    }

    private void checkMdSchemaMeasureGroupDimensionsResponseRow(MdSchemaMeasureGroupDimensionsResponseRow row,
            String catalogName, String cubeName, String measureGroupCardinality, String dimensionUniqueName,
            DimensionCardinalityEnum dimensionCardinality, Boolean dimensionIsVisible,
            Boolean dimensionIsFactDimension) {
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains(catalogName);
        assertThat(row.schemaName()).isEmpty();
        assertThat(row.cubeName()).contains(cubeName);
        assertThat(row.measureGroupName()).contains(cubeName);
        assertThat(row.measureGroupCardinality()).contains(measureGroupCardinality);
        assertThat(row.dimensionUniqueName()).contains(dimensionUniqueName);
        assertThat(row.dimensionCardinality()).contains(dimensionCardinality);
        assertThat(row.dimensionIsVisible()).contains(dimensionIsVisible);
        assertThat(row.dimensionIsFactDimension()).contains(dimensionIsFactDimension);
    }

    private void checkMdSchemaMeasureGroupsResponseRow(MdSchemaMeasureGroupsResponseRow row, String catalogName,
            String cubeName, String description, Boolean isWriteEnabled) {
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains(catalogName);
        assertThat(row.schemaName()).isEmpty();
        assertThat(row.cubeName()).contains(cubeName);
        assertThat(row.measureGroupName()).contains(cubeName);
        assertThat(row.description()).contains(description);
        assertThat(row.isWriteEnabled()).contains(isWriteEnabled);
        assertThat(row.measureGroupCaption()).contains(cubeName);

    }

    private void checkMdSchemaMeasuresResponseRow(MdSchemaMeasuresResponseRow row, String catalogName,
            String schemaName, String cubeName, String measureName, String measureUniqueName, String measureCaption,
            MeasureAggregatorEnum mdmeasureAggregator, LevelDbTypeEnum dataType, String description,
            boolean measureIsVisible, String measureLevelsList, String measureDisplayFolder,
            String defaultFormatString) {
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains(catalogName);
        assertThat(row.schemaName()).contains(schemaName);
        assertThat(row.cubeName()).contains(cubeName);
        assertThat(row.measureName()).contains(measureName);
        assertThat(row.measureUniqueName()).contains(measureUniqueName);
        assertThat(row.measureCaption()).contains(measureCaption);
        assertThat(row.measureAggregator()).contains(mdmeasureAggregator);
        assertThat(row.dataType()).contains(dataType);
        assertThat(row.description()).contains(description);
        assertThat(row.measureIsVisible()).contains(measureIsVisible);
        assertThat(row.levelsList()).contains(measureLevelsList);
        assertThat(row.measureDisplayFolder()).contains(measureDisplayFolder);
        assertThat(row.defaultFormatString()).contains(defaultFormatString);
    }

    private void checkMdSchemaMembersResponseRow(MdSchemaMembersResponseRow row, String catalogName,
            Optional<String> schemaName, String cubeName, String dimensionUniqueName, String hierarchyUniqueName,
            String levelUniqueName, int levelNumber, int memberOrdinal, String memberName, String memberUniqueName,
            MemberTypeEnum memberType, String memberCaption, int childrenCardinality, int parentLevel,
            Optional<String> oParentUniqueName, int parentCount, String description) {
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains(catalogName);
        assertThat(row.schemaName()).isEqualTo(schemaName);
        assertThat(row.cubeName()).contains(cubeName);
        assertThat(row.dimensionUniqueName()).contains(dimensionUniqueName);
        assertThat(row.hierarchyUniqueName()).contains(hierarchyUniqueName);
        assertThat(row.levelUniqueName()).contains(levelUniqueName);
        assertThat(row.levelNumber()).contains(levelNumber);
        assertThat(row.memberOrdinal()).contains(memberOrdinal);
        assertThat(row.memberName()).contains(memberName);
        assertThat(row.memberUniqueName()).contains(memberUniqueName);
        assertThat(row.memberType()).contains(memberType);
        assertThat(row.memberCaption()).contains(memberCaption);
        assertThat(row.childrenCardinality()).contains(childrenCardinality);
        assertThat(row.parentLevel()).contains(parentLevel);
        assertThat(row.parentUniqueName()).isEqualTo(oParentUniqueName);
        assertThat(row.parentCount()).contains(parentCount);
        assertThat(row.description()).contains(description);
    }

    private void checkMdSchemaPropertiesResponseRow(MdSchemaPropertiesResponseRow row, String catalogName,
            String cubeName, String dimensionUniqueName, String hierarchyUniqueName, String levelUniqueName,
            PropertyTypeEnum propertyType, String propertyName, String propertyCaption, LevelDbTypeEnum dbType,
            String description, PropertyContentTypeEnum propertyContentType) {
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains(catalogName);
        assertThat(row.schemaName()).isEmpty();
        assertThat(row.cubeName()).contains(cubeName);
        assertThat(row.dimensionUniqueName()).contains(dimensionUniqueName);
        assertThat(row.hierarchyUniqueName()).contains(hierarchyUniqueName);
        assertThat(row.levelUniqueName()).contains(levelUniqueName);
        assertThat(row.propertyType()).contains(propertyType);
        assertThat(row.propertyName()).contains(propertyName);
        assertThat(row.propertyCaption()).contains(propertyCaption);
        assertThat(row.dataType()).contains(dbType);
        assertThat(row.description()).contains(description);
        assertThat(row.propertyContentType()).contains(propertyContentType);
    }

    private void checkMdSchemaSetsResponseRow(MdSchemaSetsResponseRow row, String catalogName, String cubeName,
            String setName, ScopeEnum scope, String description, Optional<String> oExpression, String dimension,
            String setCaption, Optional<String> oSetDisplayFolder, SetEvaluationContextEnum setEvaluationContext) {
        assertThat(row).isNotNull();
        assertThat(row.catalogName()).contains(catalogName);
        assertThat(row.schemaName()).isEmpty();
        assertThat(row.cubeName()).contains(cubeName);
        assertThat(row.setName()).contains(setName);
        assertThat(row.scope()).contains(scope);
        assertThat(row.description()).contains(description);
        assertThat(row.expression()).isEqualTo(oExpression);
        assertThat(row.dimension()).contains(dimension);
        assertThat(row.setCaption()).contains(setCaption);
        assertThat(row.setDisplayFolder()).isEqualTo(oSetDisplayFolder);
        assertThat(row.setEvaluationContext()).contains(setEvaluationContext);
    }

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
