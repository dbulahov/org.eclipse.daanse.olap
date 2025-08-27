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
package org.eclipse.daanse.olap.xmla.bridge;

import static org.eclipse.daanse.olap.xmla.bridge.DrillThroughUtils.getCoordinateElements;
import static org.eclipse.daanse.olap.xmla.bridge.DrillThroughUtils.getDrillThroughQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.daanse.olap.api.action.Action;
import org.eclipse.daanse.olap.api.action.ReportAction;
import org.eclipse.daanse.olap.api.action.UrlAction;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.DrillThroughAction;
import org.eclipse.daanse.xmla.api.RequestMetaData;
import org.eclipse.daanse.xmla.api.UserRolePrincipal;
import org.eclipse.daanse.xmla.api.common.enums.ActionTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.CoordinateTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.CubeSourceEnum;
import org.eclipse.daanse.xmla.api.common.enums.InvocationEnum;
import org.eclipse.daanse.xmla.api.discover.mdschema.actions.MdSchemaActionsResponseRow;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.actions.MdSchemaActionsResponseRowR;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;

@Component(service = ActionService.class, scope = ServiceScope.SINGLETON, name = "actionService", immediate = true)
public class ActionServiceImpl implements ActionService {

    public static final String REF_NAME_URL_ACTIONS = "urlAction";
    public static final String REF_NAME_REPORT_ACTIONS = "reportAction";
    public static final String REF_NAME_DRILL_THROUGH_ACTIONS = "drillThroughAction";

    private List<Action> xmlaActions = new ArrayList<>();

    @Reference(name = REF_NAME_URL_ACTIONS, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindUrlAction(UrlAction action) {
        xmlaActions.add(action);
    }

    public void unbindUrlAction(UrlAction action) {
        xmlaActions.remove(action);
    }

    @Reference(name = REF_NAME_REPORT_ACTIONS, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindReportAction(ReportAction action) {
        xmlaActions.add(action);
    }

    public void unbindReportAction(ReportAction action) {
        xmlaActions.remove(action);
    }

    @Reference(name = REF_NAME_DRILL_THROUGH_ACTIONS, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindDrillThroughAction(org.eclipse.daanse.olap.api.action.DrillThroughAction action) {
        xmlaActions.add(action);
    }

    public void unbindDrillThroughAction(org.eclipse.daanse.olap.api.action.DrillThroughAction action) {
        xmlaActions.remove(action);
    }

    @Override
    public List<MdSchemaActionsResponseRow> getResponses(List<Catalog> catalogs, Optional<String> schemaName,
            String cubeName, Optional<String> actionName, Optional<ActionTypeEnum> actionType,
            Optional<String> coordinate, CoordinateTypeEnum coordinateType, InvocationEnum invocation,
            Optional<CubeSourceEnum> cubeSource, RequestMetaData metaData) {
        // TODO: one connection per context not each row
        List<MdSchemaActionsResponseRow> result = new ArrayList<>();
        result.addAll(catalogs.stream()
                .map(c -> getMdSchemaActionsResponseRow(c, schemaName, cubeName, actionName, actionType, coordinate,
                        coordinateType, invocation, cubeSource, metaData))
                .flatMap(Collection::stream).toList());

        if (CoordinateTypeEnum.CELL.equals(coordinateType)) {
            result.addAll(catalogs.stream().map(c -> getMdSchemaActionsResponseRow(c, schemaName, cubeName, actionName,
                    actionType, coordinate, xmlaActions)).flatMap(Collection::stream).toList());
        }
        return result;
    }

    private List<MdSchemaActionsResponseRow> getMdSchemaActionsResponseRow(Catalog catalog, Optional<String> schemaName,
            String cubeName, Optional<String> actionName, Optional<ActionTypeEnum> actionType,
            Optional<String> coordinate, List<Action> xmlaActions) {
        return getMdSchemaActionsResponseRow(schemaName, cubeName, actionName, actionType, coordinate,
                getXmlaActionWithFilterByOptional(xmlaActions, catalog.getName(), Action::catalogName));
    }

    private List<MdSchemaActionsResponseRow> getMdSchemaActionsResponseRow(Optional<String> schemaName, String cubeName,
            Optional<String> actionName, Optional<ActionTypeEnum> actionType, Optional<String> coordinate,
            List<Action> xmlaActions) {
        return getMdSchemaActionsResponseRow(cubeName, actionName, actionType, coordinate,
                getXmlaActionWithFilterBy(xmlaActions, schemaName, Action::schemaName));
    }

    private List<MdSchemaActionsResponseRow> getMdSchemaActionsResponseRow(String cubeName, Optional<String> actionName,
            Optional<ActionTypeEnum> actionType, Optional<String> coordinate, List<Action> xmlaActions) {
        return getMdSchemaActionsResponseRow(actionName, actionType, coordinate,
                getXmlaActionWithFilterBy(xmlaActions, cubeName, Action::cubeName));
    }

    private List<MdSchemaActionsResponseRow> getMdSchemaActionsResponseRow(Optional<String> actionName,
            Optional<ActionTypeEnum> actionType, Optional<String> coordinate, List<Action> xmlaActions) {
        return getMdSchemaActionsResponseRow(actionType, coordinate,
                getXmlaActionWithFilterBy(xmlaActions, actionName, Action::actionName));
    }

    private List<MdSchemaActionsResponseRow> getMdSchemaActionsResponseRow(Optional<ActionTypeEnum> actionType,
            Optional<String> coordinate, List<Action> xmlaActions) {
        return getMdSchemaActionsResponseRow(coordinate, getXmlaActionWithFilterByActionType(xmlaActions, actionType));
    }

    private List<MdSchemaActionsResponseRow> getMdSchemaActionsResponseRow(Optional<String> coordinate,
            List<Action> xmlaActions) {
        List<MdSchemaActionsResponseRow> result = new ArrayList<>();
        for (Action xmlaAcriton : xmlaActions) {
            result.add(new MdSchemaActionsResponseRowR(xmlaAcriton.catalogName(), xmlaAcriton.schemaName(),
                    xmlaAcriton.cubeName(), xmlaAcriton.actionName(), Optional.ofNullable(getActionType(xmlaAcriton)),
                    coordinate.orElse(null), getCoordinateType(xmlaAcriton.coordinateType()), xmlaAcriton.actionCaption(),
                    xmlaAcriton.description(),
                    Optional.ofNullable((String) xmlaAcriton.content(coordinate.orElse(null), xmlaAcriton.cubeName())),
                    Optional.empty(), Optional.ofNullable(InvocationEnum.NORMAL_OPERATION)));
        }
        return result;
    }

    private CoordinateTypeEnum getCoordinateType(org.eclipse.daanse.olap.api.action.CoordinateTypeEnum coordinateType) {
        if (coordinateType != null) {
            CoordinateTypeEnum.valueOf(coordinateType.name());
        }
        return null;
    }

    private ActionTypeEnum getActionType(Action xmlaAcriton) {
        if (xmlaAcriton instanceof DrillThroughAction) {
            return ActionTypeEnum.DRILL_THROUGH;
        }
        if (xmlaAcriton instanceof ReportAction) {
            return ActionTypeEnum.REPORT;
        }
        if (xmlaAcriton instanceof UrlAction) {
            return ActionTypeEnum.URL;
        }
        return null;
    }

    private List<MdSchemaActionsResponseRow> getMdSchemaActionsResponseRow(Catalog catalog,
            Optional<String> oSchemaName, String cubeName, Optional<String> oActionName,
            Optional<ActionTypeEnum> oActionType, Optional<String> oCoordinate, CoordinateTypeEnum coordinateType,
            InvocationEnum invocation, Optional<CubeSourceEnum> oCubeSource, RequestMetaData metaData) {
        if (catalog != null) {
            return getMdSchemaActionsResponseRow(catalog.getName(), catalog, cubeName, oActionName, oActionType,
                    oCoordinate, coordinateType, invocation, oCubeSource);
        }
        return List.of();
    }

    private List<MdSchemaActionsResponseRow> getMdSchemaActionsResponseRow(String catalogName, Catalog catalog,
            String cubeName, Optional<String> oActionName, Optional<ActionTypeEnum> oActionType,
            Optional<String> oCoordinate, CoordinateTypeEnum coordinateType, InvocationEnum invocation,
            Optional<CubeSourceEnum> oCubeSource) {
        List<MdSchemaActionsResponseRow> result = new ArrayList<>();
        List<Cube> cubes = catalog.getCubes() == null ? List.of() : catalog.getCubes();
        result.addAll(getCubesWithFilter(cubes, cubeName)
                .stream().map(c -> getMdSchemaActionsResponseRow(catalogName, catalog.getName(), c, oActionName,
                        oActionType, oCoordinate, coordinateType, invocation, oCubeSource))
                .flatMap(Collection::stream).toList());

        return result;
    }

    private List<MdSchemaActionsResponseRow> getMdSchemaActionsResponseRow(String catalogName, String schemaName,
            Cube cube, Optional<String> oActionName, Optional<ActionTypeEnum> oActionType, Optional<String> oCoordinate,
            CoordinateTypeEnum coordinateType, InvocationEnum invocation, Optional<CubeSourceEnum> oCubeSource) {
        List<MdSchemaActionsResponseRow> result = new ArrayList<>();
        if (cube.getDrillThroughActions() != null && coordinateType.equals(CoordinateTypeEnum.CELL)) {
            result.addAll(getMappingDrillThroughActionWithFilter(cube.getDrillThroughActions(), oActionName).stream()
                    .map(da -> getMdSchemaDrillThroughActionsResponseRow(catalogName, schemaName, cube, da,
                            oCoordinate))
                    .flatMap(Collection::stream).toList());

        }
        return result;
    }

    private static List<MdSchemaActionsResponseRow> getMdSchemaDrillThroughActionsResponseRow(String catalogName,
            String schemaName, Cube cube, DrillThroughAction da, Optional<String> oCoordinate) {
        List<MdSchemaActionsResponseRow> result = new ArrayList<>();
        if (oCoordinate != null && oCoordinate.isPresent()) {
            List<String> coordinateElements = getCoordinateElements(oCoordinate.get());
            // if (DrillThroughUtils.isDrillThroughElementsExist(da.getOlapElements(), coordinateElements,
            // cube)) {
            String query = getDrillThroughQuery(coordinateElements, da.getOlapElements(), cube);
            String coordinate = oCoordinate.get();

            result.add(new MdSchemaActionsResponseRowR(Optional.ofNullable(catalogName),
                    Optional.ofNullable(schemaName), cube.getName(), Optional.ofNullable(da.getName()),
                    Optional.of(ActionTypeEnum.DRILL_THROUGH), coordinate, CoordinateTypeEnum.CELL,
                    Optional.ofNullable(da.getCaption()), Optional.ofNullable(da.getDescription()), Optional.of(query),
                    Optional.empty(), Optional.ofNullable(InvocationEnum.NORMAL_OPERATION)));
            // }
        }
        return result;
    }

    private List<DrillThroughAction> getMappingDrillThroughActionWithFilter(List<DrillThroughAction> actions,
            Optional<String> oActionName) {
        if (oActionName.isPresent()) {
            return actions.stream().filter(a -> oActionName.get().equals(a.getName())).toList();
        }
        return actions;
    }

    private List<Cube> getCubesWithFilter(List<Cube> cubes, String cubeName) {
        if (cubeName != null) {
            return cubes.stream().filter(c -> cubeName.equals(c.getName())).toList();
        }
        return cubes;
    }

    private List<Action> getXmlaActionWithFilterBy(List<Action> actions, Optional<String> param,
            Function<Action, Optional<String>> f) {
        if (actions != null && !actions.isEmpty()) {
            if (param.isPresent()) {
                return actions.stream().filter(a -> !f.apply(a).isPresent() || param.get().equals(f.apply(a).get()))
                        .toList();
            } else {
                return actions;
            }
        }
        return List.of();
    }

    private List<Action> getXmlaActionWithFilterByActionType(List<Action> actions,
            Optional<ActionTypeEnum> param) {
        if (actions != null && !actions.isEmpty()) {
            if (param.isPresent()) {
                return actions.stream().filter(a -> param.get().equals(getActionType(a))).toList();
            } else {
                return actions;
            }
        }
        return List.of();
    }

    private List<Action> getXmlaActionWithFilterBy(List<Action> actions, String param,
            Function<Action, String> f) {
        if (actions != null && !actions.isEmpty()) {
            if (param != null) {
                return actions.stream().filter(a -> f.apply(a) == null || param.equals(f.apply(a))).toList();
            } else {
                return actions;
            }
        }
        return List.of();
    }

    private List<Action> getXmlaActionWithFilterByOptional(List<Action> actions, String param,
            Function<Action, Optional<String>> f) {
        if (actions != null && !actions.isEmpty()) {
            if (param != null) {
                return actions.stream().filter(a -> !f.apply(a).isPresent() || param.equals(f.apply(a).get())).toList();
            } else {
                return actions;
            }
        }
        return List.of();
    }

}
