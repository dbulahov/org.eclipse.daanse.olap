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
package org.eclipse.daanse.olap.xmla.bridge.execute;

import static java.util.Optional.empty;
import static org.eclipse.daanse.xmla.api.XmlaConstants.CLIENT_FAULT_FC;
import static org.eclipse.daanse.xmla.api.XmlaConstants.HSB_DRILL_THROUGH_SQL_CODE;
import static org.eclipse.daanse.xmla.api.XmlaConstants.HSB_DRILL_THROUGH_SQL_FAULT_FS;
import static org.eclipse.daanse.xmla.api.XmlaConstants.SERVER_FAULT_FC;
import static org.eclipse.daanse.xmla.api.XmlaConstants.USM_DOM_PARSE_FAULT_FS;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.eclipse.daanse.lcid.api.LcidService;
import org.eclipse.daanse.mdx.model.api.select.Allocation;
import org.eclipse.daanse.olap.api.CacheControl;
import org.eclipse.daanse.olap.api.Command;
import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.NameSegment;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.query.component.CalculatedFormula;
import org.eclipse.daanse.olap.api.query.component.DmvQuery;
import org.eclipse.daanse.olap.api.query.component.DrillThrough;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.Refresh;
import org.eclipse.daanse.olap.api.query.component.SqlQuery;
import org.eclipse.daanse.olap.api.query.component.TransactionCommand;
import org.eclipse.daanse.olap.api.query.component.UnresolvedFunCall;
import org.eclipse.daanse.olap.api.query.component.Update;
import org.eclipse.daanse.olap.api.query.component.UpdateClause;
import org.eclipse.daanse.olap.api.result.AllocationPolicy;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.result.CellSetAxis;
import org.eclipse.daanse.olap.api.result.Scenario;
import org.eclipse.daanse.olap.xmla.bridge.ActionService;
import org.eclipse.daanse.olap.xmla.bridge.ContextGroupXmlaServiceConfig;
import org.eclipse.daanse.olap.xmla.bridge.ContextListSupplyer;
import org.eclipse.daanse.olap.xmla.bridge.RoleUtils;
import org.eclipse.daanse.olap.xmla.bridge.discover.DBSchemaDiscoverService;
import org.eclipse.daanse.olap.xmla.bridge.discover.MDSchemaDiscoverService;
import org.eclipse.daanse.olap.xmla.bridge.discover.OtherDiscoverService;
import org.eclipse.daanse.olap.xmla.bridge.discover.Utils;
import org.eclipse.daanse.xmla.api.RequestMetaData;
import org.eclipse.daanse.xmla.api.UserRolePrincipal;
import org.eclipse.daanse.xmla.api.XmlaException;
import org.eclipse.daanse.xmla.api.common.properties.Content;
import org.eclipse.daanse.xmla.api.common.properties.Format;
import org.eclipse.daanse.xmla.api.common.properties.OperationNames;
import org.eclipse.daanse.xmla.api.discover.Properties;
import org.eclipse.daanse.xmla.api.discover.dbschema.catalogs.DbSchemaCatalogsResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.columns.DbSchemaColumnsResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.providertypes.DbSchemaProviderTypesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.schemata.DbSchemaSchemataRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.sourcetables.DbSchemaSourceTablesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.tables.DbSchemaTablesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.tablesinfo.DbSchemaTablesInfoRequest;
import org.eclipse.daanse.xmla.api.discover.discover.datasources.DiscoverDataSourcesRequest;
import org.eclipse.daanse.xmla.api.discover.discover.enumerators.DiscoverEnumeratorsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.keywords.DiscoverKeywordsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.literals.DiscoverLiteralsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.properties.DiscoverPropertiesRequest;
import org.eclipse.daanse.xmla.api.discover.discover.schemarowsets.DiscoverSchemaRowsetsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.xmlmetadata.DiscoverXmlMetaDataRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.actions.MdSchemaActionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.cubes.MdSchemaCubesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.demensions.MdSchemaDimensionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.functions.MdSchemaFunctionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.hierarchies.MdSchemaHierarchiesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.kpis.MdSchemaKpisRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.levels.MdSchemaLevelsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroups.MdSchemaMeasureGroupsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measures.MdSchemaMeasuresRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.members.MdSchemaMembersRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.properties.MdSchemaPropertiesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.sets.MdSchemaSetsRequest;
import org.eclipse.daanse.xmla.api.execute.ExecuteParameter;
import org.eclipse.daanse.xmla.api.execute.ExecuteService;
import org.eclipse.daanse.xmla.api.execute.alter.AlterRequest;
import org.eclipse.daanse.xmla.api.execute.alter.AlterResponse;
import org.eclipse.daanse.xmla.api.execute.cancel.CancelRequest;
import org.eclipse.daanse.xmla.api.execute.cancel.CancelResponse;
import org.eclipse.daanse.xmla.api.execute.clearcache.ClearCacheRequest;
import org.eclipse.daanse.xmla.api.execute.clearcache.ClearCacheResponse;
import org.eclipse.daanse.xmla.api.execute.statement.StatementRequest;
import org.eclipse.daanse.xmla.api.execute.statement.StatementResponse;
import org.eclipse.daanse.xmla.api.mddataset.RowSetRow;
import org.eclipse.daanse.xmla.api.mddataset.RowSetRowItem;
import org.eclipse.daanse.xmla.model.record.discover.PropertiesR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.providertypes.DbSchemaProviderTypesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.providertypes.DbSchemaProviderTypesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.schemata.DbSchemaSchemataRequestR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.schemata.DbSchemaSchemataRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.sourcetables.DbSchemaSourceTablesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.sourcetables.DbSchemaSourceTablesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.tablesinfo.DbSchemaTablesInfoRequestR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.tablesinfo.DbSchemaTablesInfoRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.datasources.DiscoverDataSourcesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.datasources.DiscoverDataSourcesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.enumerators.DiscoverEnumeratorsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.enumerators.DiscoverEnumeratorsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.keywords.DiscoverKeywordsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.keywords.DiscoverKeywordsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.literals.DiscoverLiteralsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.literals.DiscoverLiteralsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.properties.DiscoverPropertiesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.properties.DiscoverPropertiesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.schemarowsets.DiscoverSchemaRowsetsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.schemarowsets.DiscoverSchemaRowsetsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.xmlmetadata.DiscoverXmlMetaDataRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.xmlmetadata.DiscoverXmlMetaDataRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.actions.MdSchemaActionsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.actions.MdSchemaActionsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.cubes.MdSchemaCubesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.cubes.MdSchemaCubesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.demensions.MdSchemaDimensionsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.demensions.MdSchemaDimensionsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.functions.MdSchemaFunctionsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.functions.MdSchemaFunctionsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.hierarchies.MdSchemaHierarchiesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.hierarchies.MdSchemaHierarchiesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.kpis.MdSchemaKpisRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.kpis.MdSchemaKpisRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.levels.MdSchemaLevelsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.levels.MdSchemaLevelsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measuregroups.MdSchemaMeasureGroupsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measuregroups.MdSchemaMeasureGroupsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measures.MdSchemaMeasuresRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measures.MdSchemaMeasuresRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.members.MdSchemaMembersRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.members.MdSchemaMembersRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.properties.MdSchemaPropertiesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.properties.MdSchemaPropertiesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.sets.MdSchemaSetsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.sets.MdSchemaSetsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.execute.alter.AlterResponseR;
import org.eclipse.daanse.xmla.model.record.execute.cancel.CancelResponseR;
import org.eclipse.daanse.xmla.model.record.execute.clearcache.ClearCacheResponseR;
import org.eclipse.daanse.xmla.model.record.execute.statement.StatementResponseR;
import org.eclipse.daanse.xmla.model.record.mddataset.RowSetR;
import org.eclipse.daanse.xmla.model.record.mddataset.RowSetRowR;
import org.eclipse.daanse.xmla.model.record.xmla_empty.EmptyresultR;

public class OlapExecuteService implements ExecuteService {

    private static final String MDX_CUBE_0_NOT_FOUND = "MDX cube ''{0}'' not found";
    public static final String SESSION_ID = "sessionId";
    public static final String CODE3238658121 = "3238658121";
    private ContextListSupplyer contextsListSupplyer;
    private ContextGroupXmlaServiceConfig config;
    private final DBSchemaDiscoverService dbSchemaService;
    private final MDSchemaDiscoverService mdSchemaService;
    private final OtherDiscoverService otherDiscoverService;
    private final LcidService lcidService;

    public OlapExecuteService(ContextListSupplyer contextsListSupplyer, ActionService actionService,
            LcidService lcidService, ContextGroupXmlaServiceConfig config) {
        this.contextsListSupplyer = contextsListSupplyer;
        this.lcidService = lcidService;
        this.config = config;
        dbSchemaService = new DBSchemaDiscoverService(contextsListSupplyer);
        mdSchemaService = new MDSchemaDiscoverService(contextsListSupplyer, actionService);
        otherDiscoverService = new OtherDiscoverService(contextsListSupplyer, config);
    }

    @Override
    public AlterResponse alter(AlterRequest statementRequest, RequestMetaData metaData, UserRolePrincipal userRolePrincipal) {
        // TODO we use schema provider. need discus how change schema
        return new AlterResponseR(new EmptyresultR(null, null));
    }

    @Override
    public CancelResponse cancel(CancelRequest cancel, RequestMetaData metaData, UserRolePrincipal userRolePrincipal) {
        List<Context<?>> contexts = contextsListSupplyer.getContexts();
        // TODO: Context should have cencel with session
        List<String> roles = RoleUtils.getRoles(contextsListSupplyer, r -> userRolePrincipal.hasRole(r));
        for (Context<?> context : contexts) {
            try {
                final Connection connection = context.getConnection(new ConnectionProps(roles));
                /*
                 * final mondrian.rolap.RolapConnection rolapConnection1 =
                 * ((mondrian.olap4j.MondrianOlap4jConnection) connection).getMondrianConnection(); for(XmlaRequest
                 * xmlaRequest: currentRequests){
                 * if(xmlaRequest.getSessionId().equals(rolapConnection1.getConnectInfo().get( SESSION_ID))){
                 * ((mondrian.xmla.impl.DefaultXmlaRequest)xmlaRequest).setProperty(CANCELED, "true"); } }
                 */
                Context<Connection> cont = (Context<Connection>) connection.getContext();
                for (Statement statement : cont.getStatements(connection)) {
                    statement.cancel();
                }
                /*
                 * for(XmlaRequest xmlaRequest: currentRequests){ if(xmlaRequest.getSessionId().equals(sessionId)){
                 * ((mondrian.xmla.impl.DefaultXmlaRequest)xmlaRequest).setProperty(CANCELED, "true"); } }
                 */
            } catch (java.sql.SQLException oe) {
                throw new XmlaException(CLIENT_FAULT_FC, CODE3238658121, USM_DOM_PARSE_FAULT_FS, oe);
            }
        }
        return new CancelResponseR(new EmptyresultR(null, null));
    }

    @Override
    public ClearCacheResponse clearCache(ClearCacheRequest clearCacheRequest, RequestMetaData metaData,
            UserRolePrincipal userRolePrincipal) {
        // TODO clear cache was not implemented in old mondrian
        return new ClearCacheResponseR(new EmptyresultR(null, null));
    }

    @Override
    public StatementResponse statement(StatementRequest statementRequest, RequestMetaData metaData,
            UserRolePrincipal userRolePrincipal) {

        Optional<String> oCatalog = statementRequest.properties().catalog();
        if (oCatalog.isPresent()) {
            String catalogName = oCatalog.get();
            Optional<Context<?>> oContext = contextsListSupplyer.getContexts().stream()
                    .filter(ctx -> catalogName.equals(ctx.getName())).findAny();
            Context context = oContext.get();
            String statement = statementRequest.command().statement();
            if (statement != null && statement.length() > 0) {
                Locale locale = getLocale(statementRequest.properties());
                Connection connection = context.getConnection(new ConnectionProps(RoleUtils.getRoles(contextsListSupplyer, r -> userRolePrincipal.hasRole(r)), locale));
                QueryComponent queryComponent = connection.parseStatement(statement);

                if (queryComponent instanceof DrillThrough drillThrough) {
                    return executeDrillThroughQuery(statementRequest, drillThrough);
                } else if (queryComponent instanceof CalculatedFormula calculatedFormula) {
                    return executeCalculatedFormula(connection, calculatedFormula);
                } else if (queryComponent instanceof DmvQuery dmvQuery) {
                    return executeDmvQuery(connection, dmvQuery, metaData, statementRequest); // toto:
                                                                                                             // remove
                                                                                                             // userRolePrincipal,
                                                                                                             // metaData,
                } else if (queryComponent instanceof Refresh refresh) {
                    return executeRefresh(connection, refresh);
                } else if (queryComponent instanceof Update update) {
                    return executeUpdate(connection, statementRequest, update);
                } else if (queryComponent instanceof TransactionCommand transactionCommand) {
                    return executeTransactionCommand(connection, statementRequest, transactionCommand,
                            userRolePrincipal.userName());
                } else if (queryComponent instanceof Query query) {
                    return executeQuery(statementRequest, query);
                } else if (queryComponent instanceof SqlQuery sqlQuery) {
                    return executeSqlQuery(sqlQuery);
                }
            }
        } else {
            String statement = statementRequest.command().statement();
            if (statement != null && statement.length() > 0 && contextsListSupplyer.getContexts() != null
                    && !contextsListSupplyer.getContexts().isEmpty()) {
                Connection connection = contextsListSupplyer.getContexts().get(0).getConnection(new ConnectionProps(RoleUtils.getRoles(contextsListSupplyer, r -> userRolePrincipal.hasRole(r))));
                QueryComponent queryComponent = connection.parseStatement(statement);
                if (queryComponent instanceof DmvQuery dmvQuery
                        && dmvQuery.getTableName().equals(OperationNames.DBSCHEMA_CATALOGS)) {
                    List<DbSchemaCatalogsResponseRow> dbSchemaCatalogsResponseRowSetList = new ArrayList<DbSchemaCatalogsResponseRow>();
                    for (Context c : contextsListSupplyer.getContexts()) {
                        dbSchemaCatalogsResponseRowSetList.add(dbSchemaService.dbSchemaCatalogsRow(c));
                    }
                    RowSetR rowSet = DiscoveryResponseConvertor
                            .dbSchemaCatalogsResponseRowToRowSet(dbSchemaCatalogsResponseRowSetList);
                    return new StatementResponseR(null, filterRowSetByColumns(rowSet, dmvQuery.getColumns(),
                            dmvQuery.getWhereExpression(), statementRequest.parameters()));
                }
            }
        }

        return new StatementResponseR(null, null);
    }

    private Locale getLocale(Properties properties) {
        if (lcidService != null) { 
            Optional<Locale> oLocale = lcidService.lcidToLocale(properties != null ? properties.localeIdentifier() : Optional.empty());
            return oLocale.orElse(Locale.getDefault());
        }
        return Locale.getDefault();
    }

    private StatementResponse executeSqlQuery(SqlQuery sqlQuery) {
        try {
            ResultSet execute = sqlQuery.execute();
            return Convertor.toStatementResponseRowSet(execute, -1);
        } catch (java.sql.SQLException oe) {
            throw new RuntimeException(oe);
        }
    }

    private StatementResponse executeQuery(StatementRequest statementRequest, Query query) {
        ScenarioSession session = ScenarioSession.getWithoutCheck(statementRequest.sessionId());
        // RelationalQueryMapping fact = null;
        Cube cube = query.getCube();
        try {
            Scenario scenario;
            if (session != null) {
                scenario = session.getScenario();
                if (scenario != null) {
                    query.getConnection().setScenario(scenario);
                } else {
                    scenario = query.getConnection().createScenario();
                    query.getConnection().setScenario(scenario);
                }
            } else {
                session = ScenarioSession.create(statementRequest.sessionId());
                scenario = query.getConnection().createScenario();
                query.getConnection().setScenario(scenario);

            }

            // scenario.setWriteBackTable(cube.getWritebackTable());
            cube.modifyFact(scenario.getSessionValues());
            Statement statement = query.getConnection().createStatement();
            String mdx = statementRequest.command().statement();
            if ((mdx != null) && (mdx.length() != 0)) {

                // TODO: not double execute , we have QueryComponent query
                CellSet cellSet = statement.executeQuery(query);
//          CellSet cellSet = statement.executeQuery(statementRequest.command().statement());

                Optional<Content> content = statementRequest.properties().content();
                boolean omitDefaultSlicerInfo = false;
                if (!content.isPresent() || !Content.DATA_INCLUDE_DEFAULT_SLICER.equals(content.get())) {
                    omitDefaultSlicerInfo = true;
                }
                boolean json = false; // TODO? I think we don't need that at all
                Optional<Format> format = statementRequest.properties().format();
                if (!format.isPresent() || Format.NATIVE.equals(format.get())
                        || Format.MULTIDIMENSIONAL.equals(format.get())) {
                    return Convertor.toStatementResponseMddataset(cellSet, omitDefaultSlicerInfo, json);
                }
                return Convertor.toStatementResponseRowSet(cellSet);
            }
            return null;
        } finally {
            cube.restoreFact();
        }
    }

    private StatementResponse executeTransactionCommand(Connection connection, StatementRequest statementRequest,
            TransactionCommand transactionCommand, String userId) {
        String sessionId = statementRequest.sessionId();
        if (transactionCommand.getCommand() == Command.BEGIN) {
            ScenarioSession session = ScenarioSession.create(sessionId);
            Scenario scenario = connection.createScenario();
            session.setScenario(scenario);
        } else if (transactionCommand.getCommand() == Command.ROLLBACK) {
            ScenarioSession session = ScenarioSession.get(sessionId);
            session.setScenario(null);
        } else if (transactionCommand.getCommand() == Command.COMMIT) {
            ScenarioSession session = ScenarioSession.get(sessionId);
            Scenario scenario = session.getScenario();
            List<Cube> cubes = connection.getCatalog().getCubes();
            if (cubes != null) {
                for (Cube cube : cubes) {
                    cube.commit(scenario.getSessionValues(), userId);
                }
            }
            // writeBackService.commit(scenario, connection, userId);
            scenario.clear();
        }
        return new StatementResponseR(null, null);
    }

    private StatementResponse executeUpdate(Connection connection, StatementRequest statementRequest, Update update) {
        ScenarioSession session = ScenarioSession.get(statementRequest.sessionId());
        if (session != null) {
            Scenario scenario = session.getScenario();
            connection.setScenario(scenario);
            for (UpdateClause updateClause : update.getUpdateClauses()) {
                if (updateClause instanceof UpdateClause updateClauseImpl) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new org.eclipse.daanse.olap.query.component.QueryPrintWriter(sw);
                    updateClause.getTupleExp().unparse(pw);
                    String tupleString = sw.toString();

                    Statement pstmt = connection.createStatement();
                    CellSet cellSet = pstmt.executeQuery(new StringBuilder("SELECT ").append(tupleString)
                            .append(" ON 0 FROM ").append(update.getCubeName())
                            // .append(" CELL PROPERTIES CELL_ORDINAL")
                            .toString());
                    CellSetAxis axis = cellSet.getAxes().get(0);
                    if (axis.getPositionCount() == 0) {
                        // Empty tuple exception
                    }
                    if (axis.getPositionCount() == 1) {
                        // More than one tuple exception
                    }
                    // Cell writeBackCell = cellSet.getCell(Arrays.asList(0));

                    sw = new StringWriter();
                    pw = new org.eclipse.daanse.olap.query.component.QueryPrintWriter(sw);
                    updateClause.getValueExp().unparse(pw);
                    String valueString = sw.toString();

                    pstmt = connection.createStatement();
                    cellSet = pstmt.executeQuery(new StringBuilder("WITH MEMBER [Measures].[m1] AS ")
                            .append(valueString).append(" SELECT [Measures].[m1] ON 0 FROM ")
                            .append(update.getCubeName()).append(" CELL PROPERTIES VALUE").toString());
                    Cell cell = cellSet.getCell(Arrays.asList(0));
                    AllocationPolicy allocationPolicy = convertAllocation(updateClauseImpl.getAllocation());
                    String cubeName = update.getCubeName();
                    Cube cube = connection.getCatalog().lookupCube(cubeName)
                            .orElseThrow(() -> createCubeNotFoundException(cubeName));
                    List<Map<String, Map.Entry<DataTypeJdbc, Object>>> values = cube.getAllocationValues(tupleString,
                            cell.getValue(), allocationPolicy);
                    scenario.getSessionValues().addAll(values);
                    connection.getCacheControl(null).flushSchemaCache();
                }
            }
        }
        return new StatementResponseR(null, null);
    }

    private AllocationPolicy convertAllocation(Allocation allocation) {
        switch (allocation) {
        case NO_ALLOCATION:
            return AllocationPolicy.EQUAL_ALLOCATION;
        case USE_EQUAL_ALLOCATION:
            return AllocationPolicy.EQUAL_ALLOCATION;
        case USE_EQUAL_INCREMENT:
            return AllocationPolicy.EQUAL_INCREMENT;
        case USE_WEIGHTED_ALLOCATION:
            return AllocationPolicy.WEIGHTED_ALLOCATION;
        case USE_WEIGHTED_INCREMENT:
            return AllocationPolicy.WEIGHTED_INCREMENT;
        default:
            return AllocationPolicy.EQUAL_ALLOCATION;
        }
    }

    private StatementResponse executeRefresh(Connection connection, Refresh refresh) {
        Catalog catalog = connection.getCatalog();
        String cubeName = refresh.getCubeName();
        Cube cube = catalog.lookupCube(cubeName).orElseThrow(() -> createCubeNotFoundException(cubeName));
        flushCache(cube, connection);
        return new StatementResponseR(null, null);
    }

    private void flushCache(Cube cube, Connection connection) {
        final CacheControl cacheControl = connection.getCacheControl(null);
        cacheControl.flush(cacheControl.createMeasuresRegion(cube));
        // TODO

    }

    private StatementResponse executeDmvQuery(Connection connection, DmvQuery dmvQuery,
            RequestMetaData metaData, StatementRequest statementRequest) {
        Catalog catalog = connection.getCatalog();
        String tableName = dmvQuery.getTableName().toUpperCase();
        List<String> columns = dmvQuery.getColumns();

        RowSetR rowSet = null;
        switch (tableName) {
        case OperationNames.DBSCHEMA_COLUMNS:
            List<DbSchemaColumnsResponseRow> rows = Utils.getDbSchemaColumnsResponseRow(catalog, empty(), empty(),
                    empty(), empty());
            rowSet = DiscoveryResponseConvertor.dbSchemaColumnsResponseRowToRowSet(rows);
            break;
        case OperationNames.DBSCHEMA_TABLES:
            List<DbSchemaTablesResponseRow> dbSchemaTablesResponseRows = Utils.getDbSchemaTablesResponseRow(catalog,
                    empty(), empty(), empty());
            rowSet = DiscoveryResponseConvertor.dbSchemaTablesResponseRowToRowSet(dbSchemaTablesResponseRows);
            break;
        case OperationNames.DBSCHEMA_CATALOGS:

            rowSet = DiscoveryResponseConvertor.dbSchemaCatalogsResponseRowToRowSet(
                    List.of(dbSchemaService.dbSchemaCatalogsRow(connection.getContext())));
            break;
        case OperationNames.DBSCHEMA_PROVIDER_TYPES:
            DbSchemaProviderTypesRestrictionsR dbSchemaProviderTypesRestrictions = new DbSchemaProviderTypesRestrictionsR(
                    empty(), empty());
            DbSchemaProviderTypesRequest dbSchemaProviderTypesRequest = new DbSchemaProviderTypesRequestR(
                    (PropertiesR) statementRequest.properties(), dbSchemaProviderTypesRestrictions);
            rowSet = DiscoveryResponseConvertor.dbSchemaProviderTypesResponseRowToRowSet(
                    dbSchemaService.dbSchemaProviderTypes(dbSchemaProviderTypesRequest, metaData));
            break;
        case OperationNames.DBSCHEMA_SCHEMATA:
            DbSchemaSchemataRestrictionsR dbSchemaSchemataRestrictions = new DbSchemaSchemataRestrictionsR(null, null,
                    null);
            DbSchemaSchemataRequest dbSchemaSchemataRequest = new DbSchemaSchemataRequestR(
                    (PropertiesR) statementRequest.properties(), dbSchemaSchemataRestrictions);
            rowSet = DiscoveryResponseConvertor.dbSchemaSchemataResponseRowToRowSet(
                    dbSchemaService.dbSchemaSchemata(dbSchemaSchemataRequest, metaData));
            break;
        case OperationNames.DBSCHEMA_SOURCE_TABLES:
            DbSchemaSourceTablesRestrictionsR dbSchemaSourceTablesRestrictions = new DbSchemaSourceTablesRestrictionsR(
                    empty(), empty(), null, null);
            DbSchemaSourceTablesRequest dbSchemaSourceTablesRequest = new DbSchemaSourceTablesRequestR(
                    (PropertiesR) statementRequest.properties(), dbSchemaSourceTablesRestrictions);
            rowSet = DiscoveryResponseConvertor.dbSchemaSourceTablesResponseRowToRowSet(
                    dbSchemaService.dbSchemaSourceTables(dbSchemaSourceTablesRequest, metaData));
            break;
        case OperationNames.DBSCHEMA_TABLES_INFO:
            DbSchemaTablesInfoRestrictionsR dbSchemaTablesInfoRestrictions = new DbSchemaTablesInfoRestrictionsR(
                    empty(), empty(), null, null);
            DbSchemaTablesInfoRequest dbSchemaTablesInfoRequest = new DbSchemaTablesInfoRequestR(
                    (PropertiesR) statementRequest.properties(), dbSchemaTablesInfoRestrictions);
            rowSet = DiscoveryResponseConvertor.dbSchemaTablesInfoResponseRowToRowSet(
                    dbSchemaService.dbSchemaTablesInfo(dbSchemaTablesInfoRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_FUNCTIONS:
            MdSchemaFunctionsRestrictionsR mdSchemaFunctionsRestrictions = new MdSchemaFunctionsRestrictionsR(empty(),
                    empty(), empty(), empty());
            MdSchemaFunctionsRequest mdSchemaFunctionsRequest = new MdSchemaFunctionsRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaFunctionsRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaFunctionsResponseRowToRowSet(
                    mdSchemaService.mdSchemaFunctions(mdSchemaFunctionsRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_DIMENSIONS:
            MdSchemaDimensionsRestrictionsR mdSchemaDimensionsRestrictions = new MdSchemaDimensionsRestrictionsR(
                    empty(), empty(), empty(), empty(), empty(), empty(), empty());
            MdSchemaDimensionsRequest mdSchemaDimensionsRequest = new MdSchemaDimensionsRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaDimensionsRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaDimensionsResponseRowToRowSet(
                    mdSchemaService.mdSchemaDimensions(mdSchemaDimensionsRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_CUBES:
            MdSchemaCubesRestrictionsR mdSchemaCubesRestrictions = new MdSchemaCubesRestrictionsR(null, empty(),
                    empty(), empty(), empty(), empty());
            MdSchemaCubesRequest mdSchemaCubesRequest = new MdSchemaCubesRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaCubesRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaCubesResponseRowToRowSet(
                    mdSchemaService.mdSchemaCubes(mdSchemaCubesRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_ACTIONS:
            MdSchemaActionsRestrictionsR mdSchemaActionsRestrictions = new MdSchemaActionsRestrictionsR(empty(),
                    empty(), null, empty(), empty(), empty(), null, null, empty());
            MdSchemaActionsRequest mdSchemaActionsRequest = new MdSchemaActionsRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaActionsRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaActionsResponseRowToRowSet(
                    mdSchemaService.mdSchemaActions(mdSchemaActionsRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_HIERARCHIES:
            MdSchemaHierarchiesRestrictionsR mdSchemaHierarchiesRestrictions = new MdSchemaHierarchiesRestrictionsR(
                    empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty());
            MdSchemaHierarchiesRequest mdSchemaHierarchiesRequest = new MdSchemaHierarchiesRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaHierarchiesRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaHierarchiesResponseRowToRowSet(
                    mdSchemaService.mdSchemaHierarchies(mdSchemaHierarchiesRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_LEVELS:
            MdSchemaLevelsRestrictionsR mdSchemaLevelsRestrictions = new MdSchemaLevelsRestrictionsR(empty(), empty(),
                    empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty());
            MdSchemaLevelsRequest mdSchemaLevelsRequest = new MdSchemaLevelsRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaLevelsRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaLevelsResponseRowToRowSet(
                    mdSchemaService.mdSchemaLevels(mdSchemaLevelsRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_MEASUREGROUP_DIMENSIONS:
            MdSchemaMeasureGroupDimensionsRestrictionsR mdSchemaMeasureGroupDimensionsRestrictions = new MdSchemaMeasureGroupDimensionsRestrictionsR(
                    empty(), empty(), empty(), empty(), empty(), empty());
            MdSchemaMeasureGroupDimensionsRequest mdSchemaMeasureGroupDimensionsRequest = new MdSchemaMeasureGroupDimensionsRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaMeasureGroupDimensionsRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaMeasureGroupDimensionsResponseRowToRowSet(mdSchemaService
                    .mdSchemaMeasureGroupDimensions(mdSchemaMeasureGroupDimensionsRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_MEASURES:
            MdSchemaMeasuresRestrictionsR mdSchemaMeasuresRestrictions = new MdSchemaMeasuresRestrictionsR(empty(),
                    empty(), empty(), empty(), empty(), empty(), empty(), empty());
            MdSchemaMeasuresRequest mdSchemaMeasuresRequest = new MdSchemaMeasuresRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaMeasuresRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaMeasuresResponseRowToRowSet(
                    mdSchemaService.mdSchemaMeasures(mdSchemaMeasuresRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_MEMBERS:
            MdSchemaMembersRestrictionsR mdSchemaMembersRestrictions = new MdSchemaMembersRestrictionsR(empty(),
                    empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(),
                    empty());
            MdSchemaMembersRequest mdSchemaMembersRequest = new MdSchemaMembersRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaMembersRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaMembersResponseRowToRowSet(
                    mdSchemaService.mdSchemaMembers(mdSchemaMembersRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_PROPERTIES:
            MdSchemaPropertiesRestrictionsR mdSchemaPropertiesRestrictions = new MdSchemaPropertiesRestrictionsR(
                    empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(),
                    empty(), empty());
            MdSchemaPropertiesRequest mdSchemaPropertiesRequest = new MdSchemaPropertiesRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaPropertiesRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaPropertiesResponseRowToRowSet(
                    mdSchemaService.mdSchemaProperties(mdSchemaPropertiesRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_SETS:
            MdSchemaSetsRestrictionsR mdSchemaSetsRestrictions = new MdSchemaSetsRestrictionsR(empty(), empty(),
                    empty(), empty(), empty(), empty(), empty(), empty());
            MdSchemaSetsRequest mdSchemaSetsRequest = new MdSchemaSetsRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaSetsRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaSetsResponseRowToRowSet(
                    mdSchemaService.mdSchemaSets(mdSchemaSetsRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_KPIS:
            MdSchemaKpisRestrictionsR mdSchemaKpisRestrictions = new MdSchemaKpisRestrictionsR(empty(), empty(),
                    empty(), empty(), empty());
            MdSchemaKpisRequest mdSchemaKpisRequest = new MdSchemaKpisRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaKpisRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaKpisResponseRowToRowSet(
                    mdSchemaService.mdSchemaKpis(mdSchemaKpisRequest, metaData));
            break;
        case OperationNames.MDSCHEMA_MEASUREGROUPS:
            MdSchemaMeasureGroupsRestrictionsR mdSchemaMeasureGroupsRestrictions = new MdSchemaMeasureGroupsRestrictionsR(
                    empty(), empty(), empty(), empty());
            MdSchemaMeasureGroupsRequest mdSchemaMeasureGroupsRequest = new MdSchemaMeasureGroupsRequestR(
                    (PropertiesR) statementRequest.properties(), mdSchemaMeasureGroupsRestrictions);
            rowSet = DiscoveryResponseConvertor.mdSchemaMeasureGroupsResponseRowToRowSet(
                    mdSchemaService.mdSchemaMeasureGroups(mdSchemaMeasureGroupsRequest, metaData));
            break;
        case OperationNames.DISCOVER_LITERALS:
            DiscoverLiteralsRestrictionsR discoverLiteralsRestrictions = new DiscoverLiteralsRestrictionsR(empty());
            DiscoverLiteralsRequest discoverLiteralsRequest = new DiscoverLiteralsRequestR(
                    (PropertiesR) statementRequest.properties(), discoverLiteralsRestrictions);
            rowSet = DiscoveryResponseConvertor.discoverLiteralsResponseRowToRowSet(
                    otherDiscoverService.discoverLiterals(discoverLiteralsRequest, metaData));
            break;
        case OperationNames.DISCOVER_KEYWORDS:
            DiscoverKeywordsRestrictionsR discoverKeywordsRestrictions = new DiscoverKeywordsRestrictionsR(empty());
            DiscoverKeywordsRequest discoverKeywordsRequest = new DiscoverKeywordsRequestR(
                    (PropertiesR) statementRequest.properties(), discoverKeywordsRestrictions);
            rowSet = DiscoveryResponseConvertor.discoverKeywordsResponseRowToRowSet(
                    otherDiscoverService.discoverKeywords(discoverKeywordsRequest, metaData));
            break;
        case OperationNames.DISCOVER_ENUMERATORS:
            DiscoverEnumeratorsRestrictionsR discoverEnumeratorsRestrictions = new DiscoverEnumeratorsRestrictionsR(
                    empty());
            DiscoverEnumeratorsRequest discoverEnumeratorsRequest = new DiscoverEnumeratorsRequestR(
                    (PropertiesR) statementRequest.properties(), discoverEnumeratorsRestrictions);
            rowSet = DiscoveryResponseConvertor.discoverEnumeratorsResponseRowToRowSet(
                    otherDiscoverService.discoverEnumerators(discoverEnumeratorsRequest, metaData));
            break;
        case OperationNames.DISCOVER_SCHEMA_ROWSETS:
            DiscoverSchemaRowsetsRestrictionsR discoverSchemaRowsetsRestrictions = new DiscoverSchemaRowsetsRestrictionsR(
                    empty());
            DiscoverSchemaRowsetsRequest discoverSchemaRowsetsRequest = new DiscoverSchemaRowsetsRequestR(
                    (PropertiesR) statementRequest.properties(), discoverSchemaRowsetsRestrictions);
            rowSet = DiscoveryResponseConvertor.discoverSchemaRowsetsResponseRowToRowSet(
                    otherDiscoverService.discoverSchemaRowsets(discoverSchemaRowsetsRequest, metaData));
            break;
        case OperationNames.DISCOVER_PROPERTIES:
            DiscoverPropertiesRestrictionsR discoverPropertiesRestrictions = new DiscoverPropertiesRestrictionsR(
                    List.of());
            DiscoverPropertiesRequest discoverPropertiesRequest = new DiscoverPropertiesRequestR(
                    (PropertiesR) statementRequest.properties(), discoverPropertiesRestrictions);
            rowSet = DiscoveryResponseConvertor.discoverPropertiesResponseRowToRowSet(
                    otherDiscoverService.discoverProperties(discoverPropertiesRequest, metaData));
            break;
        case OperationNames.DISCOVER_DATASOURCES:
            DiscoverDataSourcesRestrictionsR discoverDataSourcesRestrictions = new DiscoverDataSourcesRestrictionsR(
                    null, empty(), empty(), empty(), null, empty(), empty());
            DiscoverDataSourcesRequest discoverDataSourcesRequest = new DiscoverDataSourcesRequestR(
                    (PropertiesR) statementRequest.properties(), discoverDataSourcesRestrictions);
            rowSet = DiscoveryResponseConvertor.discoverDataSourcesResponseRowToRowSet(
                    otherDiscoverService.dataSources(discoverDataSourcesRequest, metaData));
            break;
        case OperationNames.DISCOVER_XML_METADATA:
            DiscoverXmlMetaDataRestrictionsR discoverXmlMetaDataRestrictions = new DiscoverXmlMetaDataRestrictionsR(
                    empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(),
                    empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(),
                    empty());
            DiscoverXmlMetaDataRequest discoverXmlMetaDataRequest = new DiscoverXmlMetaDataRequestR(
                    (PropertiesR) statementRequest.properties(), discoverXmlMetaDataRestrictions);
            rowSet = DiscoveryResponseConvertor.discoverXmlMetaDataResponseRowToRowSet(
                    otherDiscoverService.xmlMetaData(discoverXmlMetaDataRequest, metaData));
            break;

        }

        return new StatementResponseR(null,
                filterRowSetByColumns(rowSet, columns, dmvQuery.getWhereExpression(), statementRequest.parameters()));
    }

    private boolean isCompatible(RowSetRow row, Expression exp, List<ExecuteParameter> parameters) {
        if (exp == null) {
            return true;
        }

        if (exp instanceof UnresolvedFunCall unresolvedFunCall) {
            final String functionName = unresolvedFunCall.getOperationAtom().name();

            Object o1, o2;
            boolean result;

            switch (functionName) {
            case "AND":
                return isCompatible(row, unresolvedFunCall.getArgs()[0], parameters)
                        && isCompatible(row, unresolvedFunCall.getArgs()[1], parameters);
            case "OR":
                return isCompatible(row, unresolvedFunCall.getArgs()[0], parameters)
                        || isCompatible(row, unresolvedFunCall.getArgs()[1], parameters);
            case "=":
                o1 = getValue(row, unresolvedFunCall.getArgs()[0], parameters);
                o2 = getValue(row, unresolvedFunCall.getArgs()[1], parameters);

                result = (o1 == null && o2 == null) || (o1 != null && o1.equals(o2));

                return result;
            case "<>":
                o1 = getValue(row, unresolvedFunCall.getArgs()[0], parameters);
                o2 = getValue(row, unresolvedFunCall.getArgs()[1], parameters);

                result = !((o1 == null && o2 == null) || (o1 != null && o1.equals(o2)));

                return result;
            default:
                return true;
            }
        } else if (exp instanceof Id) {
            Object value = getValue(row, exp, parameters);
            return value != null && value.equals("true");
        }

        return true;
    }

    private Object getValue(RowSetRow row, Expression exp, List<ExecuteParameter> parameters) {
        if (exp instanceof Id id) {
            Segment s = id.getElement(0);
            if (s instanceof NameSegment ns) {
                String columnName = ns.getName();
                if (columnName.startsWith("@")) {
                    columnName = columnName.substring(1);
                    Object value = null;
                    final String cn = columnName;
                    Optional<ExecuteParameter> oExecuteParameter = parameters.stream().filter(p -> cn.equals(p.name()))
                            .findFirst();
                    if (oExecuteParameter.isPresent()) {
                        value = oExecuteParameter.get().value();
                    }
                    return value;
                } else {
                    Object value = null;
                    final String cn = columnName;
                    Optional<RowSetRowItem> oRowSetRowItem = row.rowSetRowItem().stream()
                            .filter(ri -> cn.equals(ri.tagName())).findFirst();
                    if (oRowSetRowItem.isPresent() && oRowSetRowItem.get().value() != null) {
                        value = oRowSetRowItem.get().value();
                    }
                    return value;
                }
            }
        } else if (exp instanceof Literal l) {
            Object value = l.getValue();
            if (value != null) {
                value = value.toString();
            }
            return value;
        }
        return null;
    }

    private RowSetR filterRowSetByColumns(RowSetR rowSet, List<String> columns, Expression where,
            List<ExecuteParameter> parameters) {
        if (columns != null && !columns.isEmpty() && rowSet.rowSetRows() != null && !rowSet.rowSetRows().isEmpty()) {
            List<RowSetRow> rowSetRows = new ArrayList<RowSetRow>();
            for (RowSetRow rr : rowSet.rowSetRows()) {
                if (isCompatible(rr, where, parameters)) {
                    if (columns != null && !columns.isEmpty() && rr.rowSetRowItem() != null) {
                        rowSetRows.add(new RowSetRowR(
                                rr.rowSetRowItem().stream().filter(i -> columns.contains(i.tagName())).toList()));
                    } else {
                        rowSetRows.add(rr);
                    }
                }
            }
            return new RowSetR(rowSetRows);
        }
        return rowSet;
    }

    private StatementResponse executeCalculatedFormula(Connection connection, CalculatedFormula calculatedFormula) {
        Formula formula = calculatedFormula.getFormula();
        final Catalog schema = connection.getCatalog();
        String cubeName = calculatedFormula.getCubeName();
        final Cube cube = schema.lookupCube(cubeName).orElseThrow(() -> createCubeNotFoundException(cubeName));
        if (formula.isMember()) {
            cube.createCalculatedMember(formula);
        } else {
            cube.createNamedSet(formula);
        }
        return new StatementResponseR(null, null);
    }

    private StatementResponse executeDrillThroughQuery(StatementRequest statementRequest, DrillThrough drillThrough) {
        Optional<String> tabFields = statementRequest.properties().tableFields();
//        Optional<Boolean> advanced = statementRequest.properties().advancedFlag();
        Connection connection = drillThrough.getQuery().getConnection();
        final boolean enableRowCount = connection.getContext().getConfigValue(ConfigConstants.ENABLE_TOTAL_COUNT,
                ConfigConstants.ENABLE_TOTAL_COUNT_DEFAULT_VALUE, Boolean.class);
        final int[] rowCountSlot = enableRowCount ? new int[] { 0 } : null;
        Statement statement;
        ResultSet resultSet = null;
        Cube c = null;
        ScenarioSession session = ScenarioSession.getWithoutCheck(statementRequest.sessionId());
        if (drillThrough.getQuery() != null) {
            c = drillThrough.getQuery().getCube();
        }
        try {
            if (c != null) {
                Scenario scenario;
                if (session != null) {
                    scenario = session.getScenario();
                    connection.setScenario(scenario);
                } else {
                    session = ScenarioSession.create(statementRequest.sessionId());
                    scenario = drillThrough.getQuery().getConnection().createScenario();
                    drillThrough.getQuery().getConnection().setScenario(scenario);

                }
                if (connection.getScenario() != null) {
                    c.modifyFact(scenario.getSessionValues());
                }
            }
            statement = connection.createStatement();
            resultSet = statement.executeQuery(drillThrough, tabFields, rowCountSlot);
            int rowCount = enableRowCount ? rowCountSlot[0] : -1;
            return Convertor.toStatementResponseRowSet(resultSet, rowCount);
        } catch (Exception e) {
            e.printStackTrace();
            // NOTE: One important error is "cannot drill through on the cell"
            throw new XmlaException(SERVER_FAULT_FC, HSB_DRILL_THROUGH_SQL_CODE, HSB_DRILL_THROUGH_SQL_FAULT_FS, e);
        } finally {
            if (c != null) {
                c.restoreFact();
            }
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    private static OlapRuntimeException createCubeNotFoundException(String cubeName) {
        return new OlapRuntimeException(MessageFormat.format(MDX_CUBE_0_NOT_FOUND, cubeName));
    }

}
