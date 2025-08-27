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

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Catalog;

public interface ContextListSupplyer {

    List<Context<?>> getContexts();

    List<Catalog> get(Optional<String> sessionId);

    Optional<Catalog> tryGetFirstByName(String catalogName, Optional<String> sessionId);

    Optional<Context<?>> getContext(String name);
    
    Connection getConnection(Optional<String> sessionId, String catalogName);
}
