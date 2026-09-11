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
package org.eclipse.daanse.olap.xmla.connector.common;

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.db.DatabaseSchema;
import org.eclipse.daanse.olap.xmla.connector.ContextListSupplyer;
import org.eclipse.daanse.xmla.api.XmlaRequest;

/**
 * The two rules every discover implementation needs, in one place.
 * <p>
 * Both decide what a client is shown, and a rule kept in two copies is two
 * chances to change one and not the other.
 */
public final class DiscoverScope {

    private DiscoverScope() {
        // static access only
    }

    /**
     * The catalogs a request reaches: the named one when the server has it and the
     * caller may see it, all of the caller's otherwise.
     * <p>
     * A named catalog the server does not have yields nothing rather than
     * everything — a restriction that matches no object must not widen the answer.
     */
    public static List<Catalog> catalogs(ContextListSupplyer contexts, Optional<String> catalogName, XmlaRequest caller) {
        if (catalogName.isPresent()) {
            Optional<Catalog> catalog = contexts.tryGetFirstByName(catalogName.get(), caller);
            return catalog.map(List::of).orElseGet(List::of);
        }
        return contexts.get(caller);
    }

    /**
     * Whether this caller may open that context at all.
     * <p>
     * Roles are intersected with the ones the catalog itself declares, and a caller
     * left with none is refused by the rolap layer. Listing such a catalog offers
     * something the next request cannot deliver, so a rowset that names catalogs
     * asks this first.
     * <p>
     * A catalog that declares no roles is open to everyone, and so is every catalog
     * to a caller nobody named: on an endpoint that authenticates no one there is
     * nothing to intersect, and filtering would hide the whole server. The check
     * bites where it should - a caller who <em>is</em> named but holds none of the
     * roles the catalog declares.
     */
    public static boolean mayOpen(Context<?> context, XmlaRequest caller) {
        List<String> declared = context.getAccessRoles();
        if (declared == null || declared.isEmpty() || caller == null || caller.isAnonymous()) {
            return true;
        }
        return declared.stream().anyMatch(caller::hasRole);
    }

    /**
     * The bridge's naming rule: a hierarchy named like its dimension stands alone,
     * any other is qualified with the dimension.
     * <p>
     * Clients have seen these names for years and match on them, so the rule is
     * kept as the bridge had it rather than tidied.
     */
    public static String hierarchyName(String hierarchyName, String dimensionName) {
        if (!hierarchyName.equals(dimensionName)) {
            return dimensionName + "." + hierarchyName;
        }
        return hierarchyName;
    }
    
    /**
     * The database schemas this caller may see in that catalog: the connection's
     * reader answers, the same list the SQL guard is built over, so a table a client
     * is shown is a table it can query. The element's list would show everyone
     * everything.
     * <p>
     * Session-cached or freshly opened, the connection is the one
     * {@link ContextListSupplyer#getConnection} hands out; the catalog reached this
     * caller through the same call, so the name resolves.
     */
    public static List<? extends DatabaseSchema> databaseSchemas(ContextListSupplyer contexts, Catalog catalog,
            XmlaRequest caller) {
        Connection connection = contexts.getConnection(caller, catalog.getName());
        List<? extends DatabaseSchema> schemas = connection.getCatalogReader().getDatabaseSchemas();
        return schemas == null ? List.of() : schemas;
    }
}
