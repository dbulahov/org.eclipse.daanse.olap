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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.ContextGroup;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.api.element.Catalog;

public class ContextsSupplyerImpl implements ContextListSupplyer {

    private final ContextGroup contextsGroup;
    private Map<String, Map<String, Connection>> sessionCache = new HashMap<String, Map<String, Connection>>();

    // Accepts Null as Empty List
    public ContextsSupplyerImpl(ContextGroup contextsGroup) {

        this.contextsGroup = contextsGroup;
    }

    @Override
    public List<Catalog> get(Optional<String> sessionId) {
        return getContexts().stream().map(context -> getConnection(sessionId, context.getName())).map(Connection::getCatalog).toList();
    }

    @Override
    public Optional<Catalog> tryGetFirstByName(String catalogName, Optional<String> sessionId) {        
        return Optional.of(getConnection(sessionId, catalogName).getCatalog());
    }

    @Override
    public List<Context<?>> getContexts() {
        return contextsGroup.getValidContexts();
    }

    @Override
    public Optional<Context<?>> getContext(String name) {
        return getContexts().stream().filter(c -> c.getName().equals(name)).findFirst();

    }

    public Map<String, Map<String, Connection>> getSessionCache() {
        return this.sessionCache;

    }

    @Override
    public Connection getConnection(Optional<String> sessionId, String catalogName) {
        if (sessionId.isPresent() && sessionCache.containsKey(sessionId.get())) {
            Map<String, Connection> connectionMap =  sessionCache.get(sessionId.get());
            if (connectionMap.containsKey(catalogName)) {
                return connectionMap.get(catalogName);
            }
        }
        throw new RuntimeException("Connection is absent in cache for session " + sessionId + " for catalog " + catalogName);
    }

}
