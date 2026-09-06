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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.xmla.connector.execute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.xmla.connector.ContextListSupplyer;
import org.eclipse.daanse.olap.api.result.Scenario;
import org.eclipse.daanse.olap.xmla.connector.session.SessionScenarios;
import org.eclipse.daanse.xmla.api.XmlaCommandFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A multi-cube commit that fails halfway is retried by the client (the
 * failure is reported in band, the session survives). The rows of every
 * cube that DID persist must leave the scenario immediately - before the
 * per-cube clearing, a retry re-inserted the first cube's rows on top of
 * the already permanent ones.
 */
class WritebackCommitRetryTest {

    private final Map<Cube, List<Map<String, Map.Entry<DataTypeJdbc, Object>>>> pending =
        new LinkedHashMap<>();
    private Scenario scenario;
    private OlapExecute execute;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void wire() {
        scenario = mock(Scenario.class);
        when(scenario.pendingCubes()).thenAnswer(inv -> pending.keySet());
        when(scenario.pendingRows(any())).thenAnswer(inv -> pending.get(inv.getArgument(0, Cube.class)));
        doAnswer(inv -> {
            pending.remove(inv.getArgument(0, Cube.class));
            return null;
        }).when(scenario).clearPendingRows(any());
        doAnswer(inv -> {
            pending.clear();
            return null;
        }).when(scenario).clear();

        SessionScenarios scenarios = new SessionScenarios();
        scenarios.begin("a-session", scenario);
        execute = new OlapExecute(mock(ContextListSupplyer.class), scenarios, null, null);
    }

    private Cube cube(String name) {
        Cube cube = mock(Cube.class);
        when(cube.getName()).thenReturn(name);
        when(cube.isWriteEnabled()).thenReturn(true);
        pending.put(cube, List.of());
        return cube;
    }

    @Test
    void aRetryAfterAPartialFailureDoesNotInsertTwice() {
        Cube written = cube("Written");
        Cube failing = cube("Failing");
        doThrow(new RuntimeException("store down"))
            .when(failing).commit(anyList(), anyString());

        assertThatThrownBy(() -> execute.commit(scenario, "a-session", "user"))
            .isInstanceOf(XmlaCommandFailedException.class);
        // the persisted cube's rows are gone, the failed cube's remain
        assertThat(pending.keySet()).containsExactly(failing);
        verify(scenario, never()).clear();

        // the client retries; the failing store recovered
        doAnswer(inv -> null).when(failing).commit(anyList(), anyString());
        execute.commit(scenario, "a-session", "user");

        verify(written, times(1)).commit(anyList(), anyString());
        verify(failing, times(2)).commit(anyList(), anyString());
        verify(scenario, times(1)).clear();
    }
}
