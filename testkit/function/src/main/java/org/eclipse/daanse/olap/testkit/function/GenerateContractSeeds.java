/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.testkit.function;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.function.services.standard.StandardFunctions;

/**
 * Not a test: a one-shot generator. Run it manually, review the output, commit it.
 * Re-run it only when a new function needs a seed.
 */
class GenerateContractSeeds {

    public static void main(String[] args) throws Exception {
        Path target = Path.of("testkit/function/src/main/java/org/eclipse/daanse/olap/"
                + "testkit/function/contracts");
        FunctionService service = StandardFunctions.standard();

        for (FunctionResolver resolver : service.getResolvers()) {
            for (FunctionMetaData metaData : resolver.getRepresentativeFunctionMetaDatas()) {
                String name = metaData.operationAtom().name();
                String className = javaIdentifierOf(name) + "Contract";
                Path file = target.resolve(className + ".java");
                if (Files.exists(file)) {
                    continue;   // never overwrite hand-edited contracts
                }
                Files.writeString(file, renderSeed(className, metaData, resolver));
            }
        }
    }

	private static CharSequence renderSeed(String className, FunctionMetaData metaData, FunctionResolver resolver) {
		// TODO Auto-generated method stub
		return null;
	}

	private static String javaIdentifierOf(String name) {
		// TODO Auto-generated method stub
		return null;
	}
}