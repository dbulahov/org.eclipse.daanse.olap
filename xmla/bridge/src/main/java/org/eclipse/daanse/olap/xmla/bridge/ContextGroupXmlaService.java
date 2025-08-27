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

import java.util.Map;

import org.eclipse.daanse.lcid.api.LcidService;
import org.eclipse.daanse.olap.api.ContextGroup;
import org.eclipse.daanse.olap.xmla.bridge.discover.DelegatingDiscoverService;
import org.eclipse.daanse.olap.xmla.bridge.execute.OlapExecuteService;
import org.eclipse.daanse.olap.xmla.bridge.session.SessionServiceImpl;
import org.eclipse.daanse.xmla.api.XmlaService;
import org.eclipse.daanse.xmla.api.discover.DiscoverService;
import org.eclipse.daanse.xmla.api.execute.ExecuteService;
import org.eclipse.daanse.xmla.api.session.SessionService;
import org.osgi.namespace.unresolvable.UnresolvableNamespace;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.Designate;

@Component(service = XmlaService.class, configurationPid = ContextGroupXmlaService.PID)
@Designate(factory = true, ocd = ContextGroupXmlaServiceConfig.class)
public class ContextGroupXmlaService implements XmlaService {

    public static final String PID = "daanse.olap.xmla.bridge.ContextGroupXmlaService";
    public static final String REF_NAME_CONTEXT_GROUP = "contextGroup";

    public static final String REF_NAME_ACTION_SERVICE = "actionService";
    private ContextGroupXmlaServiceConfig config;

    @Reference(name = REF_NAME_ACTION_SERVICE)
    private ActionService actionService;
    private SessionService sessionService;
    
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private LcidService lcidService;
    
    public ContextGroupXmlaService() {

    }

    @Activate
    void activate(ContextGroupXmlaServiceConfig config, Map<String, Object> props) {
        ContextListSupplyer contextsListSupplyer = new ContextsSupplyerImpl(contextGroup);
        executeService = new OlapExecuteService(contextsListSupplyer, actionService, lcidService, config);
        discoverService = new DelegatingDiscoverService(contextsListSupplyer, actionService, config);
        sessionService = new SessionServiceImpl(contextsListSupplyer);
    }

    private ExecuteService executeService;
    private DiscoverService discoverService;
    private ContextGroup contextGroup;

    /*
     * target must be configured. no auto fetch of a ContextGroup
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY, name = REF_NAME_CONTEXT_GROUP, target = UnresolvableNamespace.UNRESOLVABLE_FILTER)
    void bindContextGroup(ContextGroup contextGroup) {
        this.contextGroup = contextGroup;
    }

    // @Reference(cardinality = ReferenceCardinality.MANDATORY, name = REF_NAME_ACTION_SERVICE, target =
    // UnresolvableNamespace.UNRESOLVABLE_FILTER)
    // void bindActionService(ActionService actionService) {
    // this.actionService = actionService;
    // }

    @Override
    public DiscoverService discover() {
        return discoverService;
    }

    @Override
    public ExecuteService execute() {
        return executeService;
    }

    @Override
    public SessionService session() {
        return sessionService;
    }

}
