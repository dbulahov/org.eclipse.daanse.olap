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

import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaCubesResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaDimensionsResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaHierarchiesResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaKpisResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaLevelsResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaMeasureGroupDimensionsResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaMeasureGroupsResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaMeasuresResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaMembersResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaPropertiesResponseRow;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaPropertiesResponseRowCell;
import static org.eclipse.daanse.olap.xmla.bridge.discover.Utils.getMdSchemaSetsResponseRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.xmla.bridge.ActionService;
import org.eclipse.daanse.olap.xmla.bridge.ContextListSupplyer;
import org.eclipse.daanse.olap.xmla.bridge.RoleUtils;
import org.eclipse.daanse.xmla.api.RequestMetaData;
import org.eclipse.daanse.xmla.api.UserRolePrincipal;
import org.eclipse.daanse.xmla.api.common.enums.ActionTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.CoordinateTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.CubeSourceEnum;
import org.eclipse.daanse.xmla.api.common.enums.InterfaceNameEnum;
import org.eclipse.daanse.xmla.api.common.enums.InvocationEnum;
import org.eclipse.daanse.xmla.api.common.enums.MemberTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.OriginEnum;
import org.eclipse.daanse.xmla.api.common.enums.PropertyOriginEnum;
import org.eclipse.daanse.xmla.api.common.enums.PropertyTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.ScopeEnum;
import org.eclipse.daanse.xmla.api.common.enums.TreeOpEnum;
import org.eclipse.daanse.xmla.api.common.enums.VisibilityEnum;
import org.eclipse.daanse.xmla.api.discover.mdschema.actions.MdSchemaActionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.actions.MdSchemaActionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.cubes.MdSchemaCubesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.cubes.MdSchemaCubesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.demensions.MdSchemaDimensionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.demensions.MdSchemaDimensionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.functions.MdSchemaFunctionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.functions.MdSchemaFunctionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.hierarchies.MdSchemaHierarchiesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.hierarchies.MdSchemaHierarchiesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.kpis.MdSchemaKpisRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.kpis.MdSchemaKpisResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.levels.MdSchemaLevelsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.levels.MdSchemaLevelsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroups.MdSchemaMeasureGroupsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroups.MdSchemaMeasureGroupsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measures.MdSchemaMeasuresRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measures.MdSchemaMeasuresResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.members.MdSchemaMembersRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.members.MdSchemaMembersResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.properties.MdSchemaPropertiesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.properties.MdSchemaPropertiesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.sets.MdSchemaSetsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.sets.MdSchemaSetsResponseRow;

public class MDSchemaDiscoverService {

    private ContextListSupplyer contextsListSupplyer;
    private ActionService actionService;

    public MDSchemaDiscoverService(ContextListSupplyer contextsListSupplyer, ActionService actionService) {
        this.contextsListSupplyer = contextsListSupplyer;
        this.actionService = actionService;
    }

    public List<MdSchemaActionsResponseRow> mdSchemaActions(MdSchemaActionsRequest request, RequestMetaData metaData) {
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> schemaName = request.restrictions().schemaName();
        String cubeName = request.restrictions().cubeName();

        Optional<String> actionName = request.restrictions().actionName();
        Optional<ActionTypeEnum> actionType = request.restrictions().actionType();
        Optional<String> coordinate = request.restrictions().coordinate();
        CoordinateTypeEnum coordinateType = request.restrictions().coordinateType();
        InvocationEnum invocation = request.restrictions().invocation();
        Optional<CubeSourceEnum> cubeSource = request.restrictions().cubeSource();

        List<MdSchemaActionsResponseRow> result = new ArrayList<>();
        String catalogName = null;

        if (oCatalogName.isPresent()) {
            catalogName = oCatalogName.get();
        } else {
            oCatalogName = request.properties().catalog();
            if (oCatalogName.isPresent()) {
                catalogName = oCatalogName.get();
            }
        }
        if (catalogName != null) {
            Optional<Catalog> oCatalog = contextsListSupplyer.tryGetFirstByName(catalogName, metaData.sessionId());
            if (oCatalog.isPresent()) {
                Catalog catalog = oCatalog.get();
                result.addAll(actionService.getResponses(List.of(catalog), schemaName, cubeName, actionName, actionType,
                        coordinate, coordinateType, invocation, cubeSource, metaData));
            }
        } else {
            result.addAll(actionService.getResponses(contextsListSupplyer.get(metaData.sessionId()), schemaName,
                    cubeName, actionName, actionType, coordinate, coordinateType, invocation, cubeSource, metaData));
        }
        return result;
    }

    public List<MdSchemaCubesResponseRow> mdSchemaCubes(MdSchemaCubesRequest request, RequestMetaData metaData) {
        String catalogName = request.restrictions().catalogName();
        Optional<String> cubeName = request.restrictions().cubeName();
        Optional<String> schemaName = request.restrictions().schemaName();
        Optional<String> baseCubeName = request.restrictions().baseCubeName();
        Optional<CubeSourceEnum> cubeSource = request.restrictions().cubeSource();
        List<MdSchemaCubesResponseRow> result = new ArrayList<>();

        // ??????????????????
        Optional<String> oCatalogName = request.properties().catalog();
        if (oCatalogName.isPresent()) {
            catalogName = oCatalogName.get();
        }

        if (catalogName != null) {
            Optional<Catalog> oCatalog = contextsListSupplyer.tryGetFirstByName(catalogName, metaData.sessionId());
            if (oCatalog.isPresent()) {
                Catalog catalog = oCatalog.get();
                result.addAll(getMdSchemaCubesResponseRow(catalog, schemaName, cubeName, baseCubeName, cubeSource,
                        metaData));
            }
        } else { // TODO: open Connection here and delegate to row.

            result.addAll(
                    contextsListSupplyer
                            .get(metaData.sessionId()).stream().map(c -> getMdSchemaCubesResponseRow(c, schemaName,
                                    cubeName, baseCubeName, cubeSource, metaData))
                            .flatMap(Collection::stream).toList());
        }
        return result;
    }

    public List<MdSchemaDimensionsResponseRow> mdSchemaDimensions(MdSchemaDimensionsRequest request,
            RequestMetaData metaData) {
        List<MdSchemaDimensionsResponseRow> result = new ArrayList<>();
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        Optional<String> oCubeName = request.restrictions().cubeName();
        Optional<String> oDimensionName = request.restrictions().dimensionName();
        Optional<String> oDimensionUniqueName = request.restrictions().dimensionUniqueName();
        Optional<CubeSourceEnum> cubeSource = request.restrictions().cubeSource();
        Optional<VisibilityEnum> oDimensionVisibility = request.restrictions().dimensionVisibility();
        Optional<Boolean> deep = request.properties().deep();
        // ??????????????????
        if (oCatalogName.isEmpty()) {
            oCatalogName = request.properties().catalog();
        }
        if (oCatalogName.isPresent()) {
            Optional<Catalog> oCatalog = oCatalogName
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oCatalog.isPresent()) {
                Catalog catalog = oCatalog.get();
                result.addAll(getMdSchemaDimensionsResponseRow(catalog, oSchemaName, oCubeName, oDimensionName,
                        oDimensionUniqueName, cubeSource, oDimensionVisibility, deep, metaData));
            }
        } else {
            result.addAll(contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getMdSchemaDimensionsResponseRow(c, oSchemaName, oCubeName, oDimensionName,
                            oDimensionUniqueName, cubeSource, oDimensionVisibility, deep, metaData))
                    .flatMap(Collection::stream).toList());
        }
        return result;
    }

    public List<MdSchemaFunctionsResponseRow> mdSchemaFunctions(MdSchemaFunctionsRequest request,
            RequestMetaData metaData) {
        Optional<String> oLibraryName = request.restrictions().libraryName();
        Optional<InterfaceNameEnum> oInterfaceName = request.restrictions().interfaceName();
        Optional<OriginEnum> oOrigin = request.restrictions().origin();
        return contextsListSupplyer.getContexts().stream().map(c -> Utils.getMdSchemaFunctionsResponseRow(c,
                oLibraryName, oInterfaceName, oOrigin, metaData)).flatMap(Collection::stream).toList();

    }

    public List<MdSchemaHierarchiesResponseRow> mdSchemaHierarchies(MdSchemaHierarchiesRequest request,
            RequestMetaData metaData) {
        List<MdSchemaHierarchiesResponseRow> result = new ArrayList<>();
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        Optional<String> oCubeName = request.restrictions().cubeName();
        Optional<CubeSourceEnum> oCubeSource = request.restrictions().cubeSource();
        Optional<String> oDimensionUniqueName = request.restrictions().dimensionUniqueName();
        Optional<String> oHierarchyName = request.restrictions().hierarchyName();
        Optional<String> oHierarchyUniqueName = request.restrictions().hierarchyUniqueName();
        Optional<VisibilityEnum> oHierarchyVisibility = request.restrictions().hierarchyVisibility();
        Optional<Integer> oHierarchyOrigin = request.restrictions().hierarchyOrigin();
        Optional<Boolean> deep = request.properties().deep();

        // ??????????????????
        if (oCatalogName.isEmpty()) {

            Optional<String> oCatalog = request.properties().catalog();
            if (oCatalog.isPresent()) {
                oCatalogName = oCatalog;
            }
        }

        if (oCatalogName.isPresent()) {
            Optional<Catalog> oCatalog = oCatalogName
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oCatalog.isPresent()) {
                Catalog catalog = oCatalog.get();
                result.addAll(getMdSchemaHierarchiesResponseRow(catalog, oSchemaName, oCubeName, oCubeSource,
                        oDimensionUniqueName, oHierarchyName, oHierarchyUniqueName, oHierarchyVisibility,
                        oHierarchyOrigin, deep, metaData));
            }
        } else {
            result.addAll(contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getMdSchemaHierarchiesResponseRow(c, oSchemaName, oCubeName, oCubeSource,
                            oDimensionUniqueName, oHierarchyName, oHierarchyUniqueName, oHierarchyVisibility,
                            oHierarchyOrigin, deep, metaData))
                    .flatMap(Collection::stream).toList());
        }
        return result;
    }

    public List<MdSchemaKpisResponseRow> mdSchemaKpis(MdSchemaKpisRequest request, RequestMetaData metaData) {
        List<MdSchemaKpisResponseRow> result = new ArrayList<>();
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        Optional<String> oCubeName = request.restrictions().cubeName();
        Optional<String> oKpiName = request.restrictions().kpiName();
        Optional<CubeSourceEnum> cubeSource = request.restrictions().cubeSource();
        // ??????????????????
        if (oCatalogName.isEmpty()) {
            oCatalogName = request.properties().catalog();
        }
        if (oCatalogName.isPresent()) {
            Optional<Catalog> oCatalog = oCatalogName
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oCatalog.isPresent()) {
                Catalog catalog = oCatalog.get();
                result.addAll(getMdSchemaKpisResponseRow(catalog, oSchemaName, oCubeName, oKpiName));
            }
        } else {
            result.addAll(contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getMdSchemaKpisResponseRow(c, oSchemaName, oCubeName, oKpiName))
                    .flatMap(Collection::stream).toList());
        }
        return result;
    }

    public List<MdSchemaLevelsResponseRow> mdSchemaLevels(MdSchemaLevelsRequest request, RequestMetaData metaData) {
        List<MdSchemaLevelsResponseRow> result = new ArrayList<>();
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        Optional<String> oCubeName = request.restrictions().cubeName();
        Optional<String> oDimensionUniqueName = request.restrictions().dimensionUniqueName();
        Optional<String> oHierarchyUniqueName = request.restrictions().hierarchyUniqueName();
        Optional<String> oLevelName = request.restrictions().levelName();
        Optional<String> oLevelUniqueName = request.restrictions().levelUniqueName();
        Optional<VisibilityEnum> oLevelVisibility = request.restrictions().levelVisibility();

        // ??????????????????
        if (oCatalogName.isEmpty()) {

            Optional<String> oCatalog = request.properties().catalog();
            if (oCatalog.isPresent()) {
                oCatalogName = oCatalog;
            }
        }

        if (oCatalogName.isPresent()) {
            Optional<Catalog> oCatalog = oCatalogName
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oCatalog.isPresent()) {
                Catalog catalog = oCatalog.get();
                result.addAll(getMdSchemaLevelsResponseRow(catalog, oSchemaName, oCubeName, oDimensionUniqueName,
                        oHierarchyUniqueName, oLevelName, oLevelUniqueName, oLevelVisibility, metaData));
            }
        } else {
            result.addAll(contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getMdSchemaLevelsResponseRow(c, oSchemaName, oCubeName, oDimensionUniqueName,
                            oHierarchyUniqueName, oLevelName, oLevelUniqueName, oLevelVisibility, metaData))
                    .flatMap(Collection::stream).toList());
        }
        return result;
    }

    public List<MdSchemaMeasureGroupDimensionsResponseRow> mdSchemaMeasureGroupDimensions(
            MdSchemaMeasureGroupDimensionsRequest request, RequestMetaData metaData) {
        List<MdSchemaMeasureGroupDimensionsResponseRow> result = new ArrayList<>();
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        Optional<String> oCubeName = request.restrictions().cubeName();
        Optional<String> oMeasureGroupName = request.restrictions().measureGroupName();
        Optional<String> oDimensionUniqueName = request.restrictions().dimensionUniqueName();
        Optional<VisibilityEnum> oDimensionVisibility = request.restrictions().dimensionVisibility();
        // ??????????????????
        if (oCatalogName.isEmpty()) {
            oCatalogName = request.properties().catalog();
        }
        if (oCatalogName.isPresent()) {
            Optional<Catalog> oCatalog = oCatalogName
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oCatalog.isPresent()) {
                Catalog catalog = oCatalog.get();
                result.addAll(getMdSchemaMeasureGroupDimensionsResponseRow(catalog, oSchemaName, oCubeName,
                        oMeasureGroupName, oDimensionUniqueName, oDimensionVisibility, metaData));
            }
        } else {
            result.addAll(contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getMdSchemaMeasureGroupDimensionsResponseRow(c, oSchemaName, oCubeName, oMeasureGroupName,
                            oDimensionUniqueName, oDimensionVisibility, metaData))
                    .flatMap(Collection::stream).toList());
        }
        return result;
    }

    public List<MdSchemaMeasureGroupsResponseRow> mdSchemaMeasureGroups(MdSchemaMeasureGroupsRequest request,
            RequestMetaData metaData) {
        List<MdSchemaMeasureGroupsResponseRow> result = new ArrayList<>();
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        Optional<String> oCubeName = request.restrictions().cubeName();
        Optional<String> oMeasureGroupName = request.restrictions().measureGroupName();
        // ??????????????????
        if (oCatalogName.isEmpty()) {
            oCatalogName = request.properties().catalog();
        }
        if (oCatalogName.isPresent()) {
            Optional<Catalog> oCatalog = oCatalogName
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oCatalog.isPresent()) {
                Catalog catalog = oCatalog.get();
                result.addAll(getMdSchemaMeasureGroupsResponseRow(catalog, oSchemaName, oCubeName, oMeasureGroupName));
            }
        } else {
            result.addAll(contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getMdSchemaMeasureGroupsResponseRow(c, oSchemaName, oCubeName, oMeasureGroupName))
                    .flatMap(Collection::stream).toList());
        }

        return result;
    }

    public List<MdSchemaMeasuresResponseRow> mdSchemaMeasures(MdSchemaMeasuresRequest request, RequestMetaData metaData) {
        List<MdSchemaMeasuresResponseRow> result = new ArrayList<>();
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        Optional<String> oCubeName = request.restrictions().cubeName();
        Optional<String> oMeasureName = request.restrictions().measureName();
        Optional<String> oMeasureUniqueName = request.restrictions().measureUniqueName();
        Optional<String> oMeasureGroupName = request.restrictions().measureGroupName();
        Optional<VisibilityEnum> oMeasureVisibility = request.restrictions().measureVisibility();
        boolean shouldEmitInvisibleMembers = oMeasureVisibility.isPresent()
                && VisibilityEnum.NOT_VISIBLE.equals(oMeasureVisibility.get());

        // ??????????????????
        if (oCatalogName.isEmpty()) {

            Optional<String> oCatalog = request.properties().catalog();
            if (oCatalog.isPresent()) {
                oCatalogName = oCatalog;
            }
        }

        if (oCatalogName.isPresent()) {
            Optional<Catalog> oContext = oCatalogName
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oContext.isPresent()) {
                Catalog catalog = oContext.get();
                result.addAll(getMdSchemaMeasuresResponseRow(catalog, oSchemaName, oCubeName, oMeasureName,
                        oMeasureUniqueName, oMeasureGroupName, shouldEmitInvisibleMembers));
            }
        } else {
            result.addAll(contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getMdSchemaMeasuresResponseRow(c, oSchemaName, oCubeName, oMeasureName,
                            oMeasureUniqueName, oMeasureGroupName, shouldEmitInvisibleMembers))
                    .flatMap(Collection::stream).toList());
        }

        return result;

    }

    public List<MdSchemaMembersResponseRow> mdSchemaMembers(MdSchemaMembersRequest request, RequestMetaData metaData) {
        List<MdSchemaMembersResponseRow> result = new ArrayList<>();
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        Optional<String> oCubeName = request.restrictions().cubeName();
        Optional<String> oDimensionUniqueName = request.restrictions().dimensionUniqueName();
        Optional<String> oHierarchyUniqueName = request.restrictions().hierarchyUniqueName();
        Optional<String> oLevelUniqueName = request.restrictions().levelUniqueName();
        Optional<Integer> oLevelNumber = request.restrictions().levelNumber();
        Optional<String> oMemberName = request.restrictions().memberName();
        Optional<String> oMemberUniqueName = request.restrictions().memberUniqueName();
        Optional<MemberTypeEnum> oMemberType = request.restrictions().memberType();
        Optional<String> oMemberCaption = request.restrictions().memberCaption();
        Optional<CubeSourceEnum> cubeSource = request.restrictions().cubeSource();
        Optional<TreeOpEnum> treeOp = request.restrictions().treeOp();
        Optional<Boolean> emitInvisibleMembers = request.properties().emitInvisibleMembers();
        // ??????????????????
        if (oCatalogName.isEmpty()) {
            oCatalogName = request.properties().catalog();
        }

        // TODO: open Connection here and delegate to row.
        if (oCatalogName.isPresent()) {
            Optional<Catalog> oCatalog = oCatalogName
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oCatalog.isPresent()) {
                Catalog catalog = oCatalog.get();
                result.addAll(getMdSchemaMembersResponseRow(catalog, oSchemaName, oCubeName, oDimensionUniqueName,
                        oHierarchyUniqueName, oLevelUniqueName, oLevelNumber, oMemberName, oMemberUniqueName,
                        oMemberType, oMemberCaption, cubeSource, treeOp, emitInvisibleMembers));
            }
        } else {
            result.addAll(contextsListSupplyer.get(metaData.sessionId()).stream()
                    .map(c -> getMdSchemaMembersResponseRow(c, oSchemaName, oCubeName, oDimensionUniqueName,
                            oHierarchyUniqueName, oLevelUniqueName, oLevelNumber, oMemberName, oMemberUniqueName,
                            oMemberType, oMemberCaption, cubeSource, treeOp, emitInvisibleMembers))
                    .flatMap(Collection::stream).toList());
        }

        return result;
    }

    public List<MdSchemaPropertiesResponseRow> mdSchemaProperties(MdSchemaPropertiesRequest request,
            RequestMetaData metaData) {
        List<MdSchemaPropertiesResponseRow> result = new ArrayList<>();
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        Optional<String> oCubeName = request.restrictions().cubeName();
        Optional<String> oDimensionUniqueName = request.restrictions().dimensionUniqueName();
        Optional<String> oHierarchyUniqueName = request.restrictions().hierarchyUniqueName();
        Optional<String> oLevelUniqueName = request.restrictions().levelUniqueName();
        Optional<String> oMemberUniqueName = request.restrictions().memberUniqueName();
        Optional<PropertyTypeEnum> oPropertyType = request.restrictions().propertyType();
        Optional<String> oPropertyName = request.restrictions().propertyName();
        Optional<PropertyOriginEnum> oPropertyOrigin = request.restrictions().propertyOrigin();
        Optional<CubeSourceEnum> oCubeSource = request.restrictions().cubeSource();
        Optional<VisibilityEnum> oPropertyVisibility = request.restrictions().propertyVisibility();
        PropertyTypeEnum propertyType = oPropertyType.orElse(PropertyTypeEnum.PROPERTY_MEMBER);

        // ??????????????????
        if (oCatalogName.isEmpty()) {

            Optional<String> oCatalog = request.properties().catalog();
            if (oCatalog.isPresent()) {
                oCatalogName = oCatalog;
            }
        }

        switch (propertyType) {
        case PROPERTY_MEMBER:
            if (oCatalogName.isPresent()) {
                Optional<Catalog> oCatalog = oCatalogName
                        .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
                if (oCatalog.isPresent()) {
                    Catalog catalog = oCatalog.get();
                    result.addAll(getMdSchemaPropertiesResponseRow(catalog, oSchemaName, oCubeName,
                            oDimensionUniqueName, oHierarchyUniqueName, oLevelUniqueName, oMemberUniqueName,
                            oPropertyName, oPropertyOrigin, oCubeSource, oPropertyVisibility));
                }
            } else {
                // TODO: open Connection here and delegate to row.
                result.addAll(contextsListSupplyer.get(metaData.sessionId()).stream()
                        .map(c -> getMdSchemaPropertiesResponseRow(c, oSchemaName, oCubeName, oDimensionUniqueName,
                                oHierarchyUniqueName, oLevelUniqueName, oMemberUniqueName, oPropertyName,
                                oPropertyOrigin, oCubeSource, oPropertyVisibility))
                        .flatMap(Collection::stream).toList());
            }
            break;
        case PROPERTY_CELL:
            result.addAll(getMdSchemaPropertiesResponseRowCell());
            break;
        case INTERNAL_PROPERTY, BLOB_PROPERTY:
        default:
        }

        return result;
    }

    public List<MdSchemaSetsResponseRow> mdSchemaSets(MdSchemaSetsRequest request, RequestMetaData metaData) {
        List<MdSchemaSetsResponseRow> result = new ArrayList<>();
        Optional<String> oCatalogName = request.restrictions().catalogName();
        Optional<String> oSchemaName = request.restrictions().schemaName();
        Optional<String> oCubeName = request.restrictions().cubeName();
        Optional<String> oSetName = request.restrictions().setName();
        Optional<ScopeEnum> oScope = request.restrictions().scope();
        Optional<CubeSourceEnum> cubeSource = request.restrictions().cubeSource();
        Optional<String> oHierarchyUniqueName = request.restrictions().hierarchyUniqueName();
        // ??????????????????
        if (oCatalogName.isEmpty()) {
            oCatalogName = request.properties().catalog();
        }
        // TODO: open Connection here and delegate to row.

        if (oCatalogName.isPresent()) {
            Optional<Catalog> oCatalog = oCatalogName
                    .flatMap(name -> contextsListSupplyer.tryGetFirstByName(name, metaData.sessionId()));
            if (oCatalog.isPresent()) {
                Catalog catalog = oCatalog.get();
                result.addAll(getMdSchemaSetsResponseRow(catalog, oSchemaName, oCubeName, oSetName, oScope, cubeSource,
                        oHierarchyUniqueName, metaData));
            }
        } else {
            // TODO: open Connection here and delegate to row.
            result.addAll(contextsListSupplyer
                    .get(metaData.sessionId()).stream().map(c -> getMdSchemaSetsResponseRow(c, oSchemaName, oCubeName,
                            oSetName, oScope, cubeSource, oHierarchyUniqueName, metaData))
                    .flatMap(Collection::stream).toList());
        }
        return result;
    }

}
