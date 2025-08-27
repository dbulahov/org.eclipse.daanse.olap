/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.xmla.bridge.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.xmla.bridge.ContextListSupplyer;
import org.eclipse.daanse.olap.xmla.bridge.ContextsSupplyerImpl;
import org.eclipse.daanse.xmla.api.UserRolePrincipal;
import org.eclipse.daanse.xmla.api.session.SessionService;
import org.eclipse.daanse.xmla.api.xmla.BeginSession;
import org.eclipse.daanse.xmla.api.xmla.EndSession;
import org.eclipse.daanse.xmla.api.xmla.Session;
import org.eclipse.daanse.xmla.model.record.xmla.SessionR;

public class SessionServiceImpl implements SessionService {
// ToDo independen sessionservice that tracks  session by ServerInstance and User and Role
    // Component singleton imidiate
    // not in bridge daanse.server
    private Set<String> store = new HashSet<>();

    private ContextListSupplyer contextsListSupplyer;

    public SessionServiceImpl(ContextListSupplyer contextsListSupplyer) {        
        this.contextsListSupplyer = contextsListSupplyer;
    }
    
    @Override
    public Optional<Session> beginSession(BeginSession beginSession, UserRolePrincipal userRolePrincipal) {

        String sessionStr = UUID.randomUUID().toString();
        Function<String, Boolean> isUserInRoleFunction = r -> userRolePrincipal.hasRole(r);
        for (Context<?> context : contextsListSupplyer.getContexts()) {
            List<String> roles = context.getAccessRoles().stream().filter(r -> isUserInRoleFunction.apply(r)).toList();
            Connection con = context.getConnection(new ConnectionProps(roles));
            ((ContextsSupplyerImpl) contextsListSupplyer).getSessionCache()
            .computeIfAbsent(sessionStr, k -> new HashMap<String, Connection>()).put(context.getName(), con);
        }
        store.add(sessionStr);
        return Optional.of(new SessionR(sessionStr, null));
    }

    @Override
    public boolean checkSession(Session session, UserRolePrincipal userPrincipal) {
        return store.contains(session.sessionId());
    }

    @Override
    public void endSession(EndSession endSession, UserRolePrincipal userPrincipal) {
        ((ContextsSupplyerImpl) contextsListSupplyer).getSessionCache().remove(endSession.sessionId());
        store.remove(endSession.sessionId());
    }

}
