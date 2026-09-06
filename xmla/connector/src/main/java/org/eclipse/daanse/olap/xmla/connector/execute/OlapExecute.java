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
package org.eclipse.daanse.olap.xmla.connector.execute;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.daanse.lcid.api.LcidService;
import org.eclipse.daanse.mdx.model.api.select.Allocation;
import org.eclipse.daanse.olap.api.Command;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.cache.CacheControl;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Measure;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.CalculatedFormula;
import org.eclipse.daanse.olap.api.query.component.DmvQuery;
import org.eclipse.daanse.olap.api.query.component.DrillThrough;
import org.eclipse.daanse.olap.api.query.component.SqlQuery;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.Refresh;
import org.eclipse.daanse.olap.api.query.component.TransactionCommand;
import org.eclipse.daanse.olap.api.query.component.Update;
import org.eclipse.daanse.olap.api.query.component.UpdateClause;
import org.eclipse.daanse.olap.api.result.AllocationPolicy;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.result.CellSetAxis;
import org.eclipse.daanse.olap.api.result.Scenario;
import org.eclipse.daanse.olap.xmla.connector.session.SessionScenarios;
import org.eclipse.daanse.xmla.api.XmlaCommandFailedException;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.olap.core.AbstractBasicContext;
import org.eclipse.daanse.olap.query.component.QueryPrintWriter;
import org.eclipse.daanse.olap.xmla.connector.ContextListSupplyer;
import org.eclipse.daanse.xmla.model.io.RowsetCatalog;
import org.eclipse.daanse.xmla.model.xmla.Batch;
import org.eclipse.daanse.xmla.model.xmla.Cancel;
import org.eclipse.daanse.xmla.model.xmla.ClearCache;
import org.eclipse.daanse.xmla.model.xmla.Execute;
import org.eclipse.daanse.xmla.model.io.RowsetResults;
import org.eclipse.daanse.xmla.model.xmla.PropertyList;
import org.eclipse.daanse.xmla.model.xmla.Discover;
import org.eclipse.daanse.xmla.model.xmla.Parameter;
import org.eclipse.daanse.xmla.model.xmla.RequestTypeEnum;
import org.eclipse.daanse.xmla.model.xmla.Statement;
import org.eclipse.daanse.xmla.model.xmla.XmlaFactory;
import org.eclipse.daanse.xmla.api.XmlaRequest;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute: an MDX statement against a connection, answered as a model
 * {@code MdDataset}.
 * <p>
 * The MDX query path - parse, scenario, run, {@link CellSetToMdDataset} - takes
 * the caller's roles and locale into the connection, applies a writeback
 * session's scenario to the fact before running, and omits the default slicer
 * info under {@code Content=Data} the way Analysis Services does. A client that
 * names no catalog gets the only one where there is only one.
 * <p>
 * Beside it: the writeback family ({@code BEGIN/COMMIT/ROLLBACK},
 * {@code UPDATE CUBE}, {@code REFRESH}, calculated formulas), the commands
 * {@code Alter}, {@code Cancel} and {@code ClearCache}, and the kinds answered
 * with a rowset rather than a dataset - DMV, drill-through, SQL,
 * {@code Format=Tabular}.
 */
public class OlapExecute {

    private static final Logger LOGGER = LoggerFactory.getLogger(OlapExecute.class);

    /** What a client is told failed, in the place MSOLAP names its own engine. */
    private static final String SOURCE = "Eclipse Daanse OLAP";

    /**
     * How a DMV query reaches the discover implementations without knowing them.
     */
    public interface Discoverer {
        List<EObject> discover(String requestType, Discover request, XmlaRequest context);
    }

    private final ContextListSupplyer contexts;
    private final SessionScenarios scenarios;
    private final LcidService lcidService;
    private final Discoverer discoverer;

    public OlapExecute(ContextListSupplyer contexts, SessionScenarios scenarios, LcidService lcidService,
            Discoverer discoverer) {
        this.contexts = contexts;
        this.scenarios = scenarios;
        this.lcidService = lcidService;
        this.discoverer = discoverer;
    }

    /**
     * Runs one command. Returns the result, or {@code null} for a command that has
     * none.
     */
    public EObject execute(Execute request, XmlaRequest context) {
        if (request.getCommand() instanceof Statement statement) {
            return statement(statement, request, context);
        }
        if (request.getCommand() instanceof Cancel) {
            return cancel(context);
        }
        if (request.getCommand() instanceof Batch batch) {
            return batch(batch, request, context);
        }
        if (request.getCommand() instanceof org.eclipse.daanse.xmla.model.xmla.Alter
                || request.getCommand() instanceof ClearCache) {
            // The bridge answered both with an empty result: Alter because the schema comes
            // from a provider it cannot change, ClearCache because old mondrian never had
            // it.
            return null;
        }
        throw new UnsupportedOperationException(
                "the command " + ExtendedMetaData.INSTANCE.getName(request.getCommand().eClass())
                        + " is not run by this connector yet");
    }

    /**
     * Cancels every running statement the caller's roles can reach, as the bridge
     * did.
     */
    private EObject cancel(XmlaRequest context) {
        for (Context<?> olapContext : contexts.getContexts()) {
            try {
                Connection connection = olapContext.getConnection(new ConnectionProps(rolesOf(context, olapContext)));
                try {
                    @SuppressWarnings("unchecked")
                    Context<Connection> typed = (Context<Connection>) connection.getContext();
                    for (org.eclipse.daanse.olap.api.execution.Statement statement : typed.getStatements(connection)) {
                        statement.cancel();
                    }
                } finally {
                    connection.close();
                }
            } catch (java.sql.SQLException e) {
                throw new RuntimeException("Cancel failed against " + olapContext.getName(), e);
            }
        }
        return null;
    }

    private EObject statement(Statement statement, Execute request, XmlaRequest context) {
        String mdx = statement.getStatement();
        if (mdx == null || mdx.isBlank()) {
            LOGGER.warn("Empty statement received");
            return null;
        }

        PropertyList properties = request.getProperties() == null ? null : request.getProperties().getPropertyList();

        Optional<String> catalogName = Optional.empty();
        if (properties != null && properties.getCatalog() != null && !properties.getCatalog().isEmpty()) {
            catalogName = Optional.of(properties.getCatalog());
        }
        // Some clients send an empty catalog when there is only one.
        List<Context<?>> available = contexts.getContexts() == null ? List.of() : contexts.getContexts();
        if (catalogName.isEmpty() && available.size() == 1) {
            catalogName = Optional.of(available.get(0).getName());
        }
        Optional<Context<?>> named = catalogName.flatMap(contexts::getContext);
        if (named.isEmpty() && catalogName.isPresent()) {
            LOGGER.warn("no context found for catalog {}", catalogName.get());
            return null;
        }

        // Parsing needs a connection, but a DMV does not need *this* one: it asks about
        // metadata rather than about a cube, and its rows come from the discover
        // implementations, which already span every catalog the caller may see when no
        // Catalog property names one. So where none is named, borrow the first
        // reachable catalog to parse with, and insist on a real one only for the
        // statement kinds that genuinely need it - refusing here would answer a client
        // that names no catalog with the empty root, which it reads as "not a rowset".
        Optional<Context<?>> olapContext = named.isPresent() ? named : available.stream().findFirst();
        if (olapContext.isEmpty()) {
            LOGGER.warn("this server has no catalog to run a statement against; nothing is run");
            return null;
        }

        Connection connection = olapContext.get()
                .getConnection(new ConnectionProps(rolesOf(context, olapContext.get()), locale(properties)));
        // close in EVERY arm, not just drill-through: the context registry
        // holds every connection strongly, so a leaked one pinned its
        // session cache overlay (and the overlay's local store of
        // uncommitted writeback segments) for the process lifetime.
        // close() is idempotent and reaps the overlay itself
        // (removeSegmentCacheManager) - so the transaction-boundary reap
        // no longer depends on matching the connection object either.
        try {
            QueryComponent queryComponent = connection.parseStatement(mdx);

            String sessionId = context.sessionId();
            if (queryComponent instanceof DmvQuery dmvQuery) {
                return dmv(dmvQuery, request, context);
            }
            if (named.isEmpty()) {
                LOGGER.warn("no catalog named and more than one available; nothing is run");
                return null;
            }
            if (queryComponent instanceof Query query) {
                return runQuery(query, properties, sessionId);
            }
            if (queryComponent instanceof DrillThrough drillThrough) {
                return drillThrough(drillThrough, properties, sessionId);
            }
            if (queryComponent instanceof SqlQuery sqlQuery) {
                return sql(sqlQuery, properties);
            }
            if (queryComponent instanceof TransactionCommand transaction) {
                return transaction(connection, transaction, sessionId, context.userName());
            }
            if (queryComponent instanceof Update update) {
                return update(connection, update, sessionId);
            }
            if (queryComponent instanceof Refresh refresh) {
                return refresh(connection, refresh);
            }
            if (queryComponent instanceof CalculatedFormula calculatedFormula) {
                return calculatedFormula(connection, calculatedFormula);
            }
            throw new UnsupportedOperationException("the statement kind " + queryComponent.getClass().getSimpleName()
                    + " is not run by this connector yet");
        } finally {
            connection.close();
        }
    }

    private EObject runQuery(Query query, PropertyList properties, String sessionId) {
        Cube cube = query.getCube();
        // A writeback session's pending values take part in every query of that
        // session. Without one, the query gets a scenario of its own that nothing
        // outlives - which is what the previous code did too, except that it also
        // registered an entry it never put the scenario into.
        Scenario scenario = scenarios.of(sessionId);
        if (scenario == null) {
            scenario = query.getConnection().createScenario();
        }
        query.getConnection().setScenario(scenario);
        // This cube's own pending values take part in the query; another cube's
        // would describe columns it does not have. The bracket holds the cube for
        // the whole query, so a second session cannot rewrite the fact underneath
        // this one.
        return cube.withPendingRows(scenario.pendingRows(cube), () -> {
            org.eclipse.daanse.olap.api.execution.Statement statement = query.getConnection().createStatement();
            CellSet cellSet = statement.executeQuery(query);

            // Content=Data (the default) omits the default slicer info, as SSAS does;
            // DATA_INCLUDE_DEFAULT_SLICER asks for it back.
            boolean omitDefaultSlicerInfo = true;
            if (properties != null && properties.getContent() != null
                    && "DataIncludeDefaultSlicer".equalsIgnoreCase(properties.getContent())) {
                omitDefaultSlicerInfo = false;
            }
            // Format=Native/Multidimensional answers a dataset; Tabular flattens to a
            // rowset.
            if (properties != null && properties.getFormat() != null
                    && "Tabular".equalsIgnoreCase(properties.getFormat())) {
                return applyContent(TabularResults.fromCellSet(cellSet, schemaIncluded(properties)), properties);
            }
            return CellSetToMdDataset.toMdDataset(cellSet, omitDefaultSlicerInfo);
        });
    }

    // --- the writeback family, ported from the bridge clause by clause ---

    private EObject transaction(Connection connection, TransactionCommand transaction, String sessionId,
            String userId) {
        LOGGER.info("Writeback[xmla] TransactionCommand {} sessionId='{}' userId='{}'", transaction.getCommand(),
                sessionId, userId);
        if (transaction.getCommand() == Command.BEGIN) {
            scenarios.begin(sessionId, connection.createScenario());
        } else if (transaction.getCommand() == Command.ROLLBACK) {
            scenarios.clear(sessionId);
            releaseSessionCacheOverlay(connection);
        } else if (transaction.getCommand() == Command.COMMIT) {
            try {
                commit(scenarios.require(sessionId), sessionId, userId);
            } finally {
                // even a failed commit drops the overlay: it holds
                // PRE-commit derived segments, and the next read rebuilds
                // one from whatever is still pending
                releaseSessionCacheOverlay(connection);
            }
        }
        return null;
    }

    /**
     * Drops the connection's private cache overlay at the transaction
     * boundary. The overlay isolates uncommitted writeback values from the
     * shared caches; reaping it here (instead of as a side effect of the
     * manager getter) means a concurrently running statement on the same
     * connection never loses its caches mid-query. Connection close reaps
     * as well.
     */
    private static void releaseSessionCacheOverlay(Connection connection) {
        if (connection.getContext() instanceof AbstractBasicContext<?> basicContext) {
            basicContext.getAggregationManager().removeSegmentCacheManager(connection);
        }
    }

    /**
     * Makes a session's pending values permanent, or says why it could not.
     * <p>
     * A commit that fails is reported in band rather than as a fault, the way a
     * Microsoft server reports a writeback it cannot honour: the request was
     * understood and answered, and the session is still there for a rollback or a
     * second attempt. That is also why the scenario is only cleared once every cube
     * has been written - a caller told "not committed" must still be holding what
     * did not commit.
     */
    void commit(Scenario scenario, String sessionId, String userId) {
        // Silence here was the worst answer available: without a writeback table
        // WritebackUtil.commit writes nothing and returns, and the client is told the
        // values are safe.
        List<String> notWritable = scenario.pendingCubes().stream().filter(cube -> !cube.isWriteEnabled())
                .map(Cube::getName).toList();
        if (!notWritable.isEmpty()) {
            LOGGER.warn("Writeback[commit] refused: no writeback table on {}", notWritable);
            throw new XmlaCommandFailedException(null,
                    "Cell writeback errors: the cube \"" + String.join("\", \"", notWritable)
                            + "\" has no writeback table, so there is nowhere to make these values permanent.",
                    SOURCE, null);
        }
        try {
            // Only the cubes this scenario produced rows for, and each only its own.
            // Handing every cube the whole list wrote the same values into every
            // writeback table in the catalog.
            for (Cube cube : List.copyOf(scenario.pendingCubes())) {
                cube.commit(scenario.pendingRows(cube), userId);
                // this cube's rows are permanent NOW: a retry after a later
                // cube's failure must not insert them a second time
                scenario.clearPendingRows(cube);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Writeback[commit] failed", e);
            throw new XmlaCommandFailedException(null, "Cell writeback errors: " + e.getMessage(), SOURCE, null, e);
        }
        scenario.clear();
        scenarios.clear(sessionId);
    }

    private EObject update(Connection connection, Update update, String sessionId) {
        int clauseCount = update.getUpdateClauses() == null ? 0 : update.getUpdateClauses().size();
        LOGGER.info("Writeback[xmla] UPDATE cube='{}' clauses={} sessionId='{}'", update.getCubeName(), clauseCount,
                sessionId);

        Scenario scenario = scenarios.require(sessionId);
        connection.setScenario(scenario);
        String cubeName = update.getCubeName();
        Cube cube = connection.getCatalog().lookupCube(cubeName)
                .orElseThrow(() -> new RuntimeException("cube " + cubeName + " not found"));
        // modifyFact because the data can already be in the writeback table. Only this
        // cube's own pending rows: another cube's rows describe columns this one does
        // not have, and would answer the caller with values never meant for it.
        // One bracket PER clause: the snapshot is taken eagerly, so a shared
        // bracket around the loop meant clause N read cell values that ignored
        // clause N-1's freshly pending rows - deltas against a stale base.
        for (UpdateClause clause : update.getUpdateClauses()) {
            cube.withPendingRows(scenario.pendingRows(cube), () -> {
                applyUpdateClause(connection, scenario, cube, update.getCubeName(), clause);
                return null;
            });
        }
        // once, after the loop, outside any bracket: flushing the catalog
        // pool per clause tore down the very catalog the loop's cube
        // belongs to while its bracket was still active
        connection.getCacheControl(null).flushSchemaCache();
        return null;
    }

    private void applyUpdateClause(Connection connection, Scenario scenario, Cube cube, String cubeName,
            UpdateClause clause) {
        String tuple = unparse(writer -> clause.getTupleExp().unparse(writer));
        CellSet tupleSet = connection.createStatement().executeQuery("SELECT " + tuple + " ON 0 FROM " + cubeName);
        CellSetAxis axis = tupleSet.getAxes().getFirst();

        String valueExpression = unparse(writer -> clause.getValueExp().unparse(writer));
        CellSet valueSet = connection.createStatement().executeQuery("WITH MEMBER [Measures].[m1] AS " + valueExpression
                + " SELECT [Measures].[m1] ON 0 FROM " + cubeName + " CELL PROPERTIES VALUE");
        Cell cell = valueSet.getCell(Arrays.asList(0));
        Object resolvedValue = cell.getValue();
        AllocationPolicy allocationPolicy = allocationPolicy(clause.getAllocation());

        // The measure goes first for setCellValue; a text-typed measure takes the text
        // short-path there, because the numeric allocation path drops text writebacks.
        List<Member> resolvedMembers = axis.getPositions().isEmpty() ? List.of()
                : axis.getPositions().get(0).getMembers();
        Member measureMember = null;
        List<Member> reordered = new ArrayList<>(resolvedMembers.size());
        for (Member member : resolvedMembers) {
            if (measureMember == null && member instanceof Measure) {
                measureMember = member;
            } else {
                reordered.add(member);
            }
        }
        if (measureMember != null) {
            reordered.add(0, measureMember);
        }
        boolean isText = measureMember != null
                && "String".equals(measureMember.getPropertyValue(StandardProperty.DATATYPE.getName()));

        LOGGER.info(
                "Writeback[xmla] UPDATE tuple='{}' valueExp='{}' resolvedValue={}"
                        + " allocation={} targetMeasure='{}' isTextMeasure={}",
                tuple, valueExpression, resolvedValue, allocationPolicy,
                measureMember == null ? "<unresolved>" : measureMember.getUniqueName(), isText);

        if (isText) {
            scenario.setCellValue(connection, reordered, resolvedValue, null, allocationPolicy, new Object[0]);
        } else {
            scenario.addPendingRows(cube,
                    cube.getAllocationValues(tuple, resolvedValue, allocationPolicy, connection.getRole()));
        }
    }

    private interface Unparsed {
        void unparse(PrintWriter writer);
    }

    private static String unparse(Unparsed expression) {
        StringWriter buffer = new StringWriter();
        expression.unparse(new QueryPrintWriter(buffer));
        return buffer.toString();
    }

    private static AllocationPolicy allocationPolicy(Allocation allocation) {
        if (allocation == Allocation.USE_EQUAL_INCREMENT) {
            return AllocationPolicy.EQUAL_INCREMENT;
        }
        if (allocation == Allocation.USE_WEIGHTED_ALLOCATION) {
            return AllocationPolicy.WEIGHTED_ALLOCATION;
        }
        if (allocation == Allocation.USE_WEIGHTED_INCREMENT) {
            return AllocationPolicy.WEIGHTED_INCREMENT;
        }
        return AllocationPolicy.EQUAL_ALLOCATION;
    }

    private EObject refresh(Connection connection, Refresh refresh) {
        String cubeName = refresh.getCubeName();
        Cube cube = connection.getCatalog().lookupCube(cubeName)
                .orElseThrow(() -> new RuntimeException("cube " + cubeName + " not found"));
        CacheControl cacheControl = connection.getCacheControl(null);
        cacheControl.flush(cacheControl.createMeasuresRegion(cube));
        return null;
    }

    private EObject calculatedFormula(Connection connection, CalculatedFormula calculatedFormula) {
        Formula formula = calculatedFormula.getFormula();
        String cubeName = calculatedFormula.getCubeName();
        Cube cube = connection.getCatalog().lookupCube(cubeName)
                .orElseThrow(() -> new RuntimeException("cube " + cubeName + " not found"));
        if (formula.isMember()) {
            cube.createCalculatedMember(formula);
        } else {
            cube.createNamedSet(formula);
        }
        return null;
    }

    // --- the rowset-answering kinds
    // -----------------------------------------------------

    /**
     * The Content property decides whether the inline schema travels with the rows.
     */
    static boolean schemaIncluded(PropertyList properties) {
        if (properties == null || properties.getContent() == null) {
            return true;
        }
        String content = properties.getContent();
        return !"Data".equalsIgnoreCase(content) && !"None".equalsIgnoreCase(content);
    }

    /**
     * And the other half of the same switch: whether the rows travel at all.
     * <p>
     * {@code Schema} means the shape without the data, and clients ask for it - for
     * a schema lookup and for {@code CommandBehavior.SchemaOnly}. Answering it with
     * every row is the expensive answer to a request for the cheap one.
     */
    static boolean dataIncluded(PropertyList properties) {
        if (properties == null || properties.getContent() == null) {
            return true;
        }
        String content = properties.getContent();
        return !"Schema".equalsIgnoreCase(content) && !"Metadata".equalsIgnoreCase(content)
                && !"None".equalsIgnoreCase(content);
    }

    /**
     * The single place the data half of Content is applied. Dropping the rows after
     * they were built wastes the work but keeps one rule in one place; the four
     * builders differ too much to each carry the flag.
     */
    private static org.eclipse.daanse.xmla.model.xmla.RowsetResult applyContent(
            org.eclipse.daanse.xmla.model.xmla.RowsetResult result, PropertyList properties) {
        if (!dataIncluded(properties)) {
            result.getRows().clear();
        }
        return result;
    }

    /**
     * A DMV query: SELECT against a {@code $SYSTEM} table. The table name is the
     * request type, the rows come from the same discover implementations a Discover
     * would reach, and the SELECT's column projection and WHERE are applied on top
     * - the bridge held a twenty-case copy of its discover wiring here, which one
     * dispatch replaces.
     */
    private EObject dmv(DmvQuery dmvQuery, Execute request, XmlaRequest context) {
        org.eclipse.daanse.dmv.model.api.DmvStatement statement = dmvQuery.statement();
        String tableName = statement.table().toUpperCase(Locale.ROOT);
        RequestTypeEnum requestType = RequestTypeEnum.getByName(tableName);
        java.util.Optional<org.eclipse.emf.ecore.EClass> rowClass = RowsetCatalog.forRequestType(tableName);
        if (requestType == null || rowClass.isEmpty()) {
            throw new UnsupportedOperationException("the DMV table " + statement.table() + " is not served");
        }

        Discover discover = XmlaFactory.eINSTANCE.createDiscover();
        discover.setRequestType(requestType);
        if (request.getProperties() != null) {
            discover.setProperties(org.eclipse.emf.ecore.util.EcoreUtil.copy(request.getProperties()));
        }
        // SYSTEMRESTRICTSCHEMA restrictions become the Discover's restriction list -
        // the
        // same wire shape a real Discover carries, so the transport's restriction rules
        // apply to a DMV exactly as they do to a Discover.
        if (!statement.restrictions().isEmpty()) {
            discover.setRestrictions(XmlaFactory.eINSTANCE.createRestrictions());
            for (org.eclipse.daanse.dmv.model.api.Restriction restriction : statement.restrictions()) {
                org.eclipse.daanse.xmla.model.xmla.RestrictionEntry entry = XmlaFactory.eINSTANCE
                        .createRestrictionEntry();
                entry.setName(restriction.name());
                entry.setValue(literalText(restriction.value()));
                discover.getRestrictions().getRestrictionList().add(entry);
            }
        }
        List<EObject> rows = discoverer.discover(tableName, discover, context);

        List<Parameter> parameters = request.getParameters() == null ? List.of()
                : request.getParameters().getParameter();
        PropertyList properties = request.getProperties() == null ? null : request.getProperties().getPropertyList();
        return applyContent(RowsetResults.fromRows(rowClass.get(), rows, statement, parameters, schemaIncluded(properties)),
                properties);
    }

    private static String literalText(org.eclipse.daanse.dmv.model.api.DmvLiteral literal) {
        if (literal instanceof org.eclipse.daanse.dmv.model.api.StringLiteral text) {
            return text.value();
        }
        return ((org.eclipse.daanse.dmv.model.api.NumericLiteral) literal).value().toPlainString();
    }

    private EObject drillThrough(DrillThrough drillThrough, PropertyList properties, String sessionId) {
        Connection connection = drillThrough.getQuery().getConnection();
        boolean enableRowCount = connection.getContext().getConfig().enableTotalCount();
        int[] rowCountSlot = enableRowCount ? new int[] { 0 } : null;
        Cube cube = drillThrough.getQuery().getCube();
        try {
            Scenario scenario = scenarios.of(sessionId);
            if (scenario == null) {
                scenario = connection.createScenario();
            }
            connection.setScenario(scenario);
            java.util.function.Supplier<EObject> run = () -> {
                java.sql.ResultSet resultSet = null;
                try {
                    // The model carries no TableFields property (the api had one); nothing is
                    // passed.
                    resultSet = connection.createStatement().executeQuery(drillThrough, java.util.Optional.empty(),
                            rowCountSlot);
                    int rowCount = enableRowCount ? rowCountSlot[0] : -1;
                    return applyContent(RowsetResults.fromResultSet(resultSet, rowCount, schemaIncluded(properties)),
                            properties);
                } catch (java.sql.SQLException e) {
                    throw new RuntimeException("Drill through SQL failed", e);
                } finally {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        } catch (java.sql.SQLException ignored) {
                            // closing is best effort, as it was in the bridge
                        }
                    }
                }
            };
            // Read inside the bracket: the rows are the ones this session's fact
            // describes, and the fact goes back the moment they have been read.
            return cube == null ? run.get() : cube.withPendingRows(scenario.pendingRows(cube), run);
        } finally {
            connection.close();
        }
    }

    /**
     * A raw SQL statement, answered as a rowset.
     * <p>
     * The result set is drained here and everything behind it closed.
     * {@code SqlQuery.execute()} hands back a live result set and cannot close the
     * connection without closing the rows it just returned, so the obligation lands
     * on this side - one leaked pooled connection per statement otherwise.
     */
    private EObject sql(SqlQuery sqlQuery, PropertyList properties) {
        try (java.sql.ResultSet rows = sqlQuery.execute()) {
            java.sql.Statement statement = rows.getStatement();
            try (java.sql.Connection connection = statement == null ? null : statement.getConnection()) {
                return applyContent(RowsetResults.fromResultSet(rows, -1, schemaIncluded(properties)), properties);
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A Batch: every command in order, one entry per command in the
     * {@code xmla-m:results} answer. A command with nothing to say contributes an
     * {@code Emptyresult}, because a list cannot carry null.
     */
    private EObject batch(Batch batch, Execute request, XmlaRequest context) {
        org.eclipse.daanse.xmla.model.multipleresults.Results results = org.eclipse.daanse.xmla.model.multipleresults.MultipleResultsFactory.eINSTANCE
                .createResults();
        List<org.eclipse.daanse.xmla.model.xmla.Command> commands = batch.getCommand();
        for (org.eclipse.daanse.xmla.model.xmla.Command command : commands) {
            Execute single = XmlaFactory.eINSTANCE.createExecute();
            single.setCommand(org.eclipse.emf.ecore.util.EcoreUtil.copy(command));
            if (request.getProperties() != null) {
                single.setProperties(org.eclipse.emf.ecore.util.EcoreUtil.copy(request.getProperties()));
            }
            EObject result = execute(single, context);
            results.getResults().add(result != null ? result
                    : org.eclipse.daanse.xmla.model.empty.EmptyFactory.eINSTANCE.createEmptyresult());
        }
        return results;
    }

    /**
     * The caller's roles, filtered against what <em>this</em> catalog defines.
     * <p>
     * Per catalog, not per server: a role name the catalog does not know is an
     * error to the rolap layer, so collecting the names every catalog defines makes
     * one catalog's roles break the connection to the next.
     */
    private static List<String> rolesOf(XmlaRequest request, Context<?> context) {
        List<String> roles = new ArrayList<>();
        for (String role : context.getAccessRoles()) {
            if (request.hasRole(role)) {
                roles.add(role);
            }
        }
        return roles;
    }

    private Locale locale(PropertyList properties) {
        if (lcidService != null) {
            Optional<Integer> lcid = properties == null ? Optional.empty()
                    : Optional.ofNullable(properties.getLocaleIdentifier());
            Optional<Locale> locale = lcidService.lcidToLocale(lcid);
            if (locale.isPresent()) {
                return locale.get();
            }
        }
        return Locale.getDefault();
    }
}
