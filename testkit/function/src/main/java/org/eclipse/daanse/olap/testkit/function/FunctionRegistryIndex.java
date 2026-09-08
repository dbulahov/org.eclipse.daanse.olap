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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;

/** Looks a name up against everything a {@link FunctionService} has registered. */
public class FunctionRegistryIndex {

	private static final String NL = System.lineSeparator();

	private FunctionRegistryIndex() {
	}

	/** Every distinct atom, of any kind, registered under {@code name}. */
	public static List<OperationAtom> atomsNamed(FunctionService functionService, String name) {
		return distinctAtoms(functionService).stream().filter(atom -> atom.name().equals(name)).toList();
	}

	/**
	 * Why {@code atom} could not be found, for use in an assertion failure message.
	 *
	 * <p>Checks, in order: the name registered under a different kind of atom (a common
	 * confusion between {@code Members} the property and {@code Members} the method), a
	 * case-insensitive match (MDX function names are case-sensitive in this registry), and
	 * otherwise the closest registered names by edit distance.
	 */
	public static String notFoundDiagnosis(FunctionService functionService, OperationAtom atom) {
		String name = atom.name();
		List<OperationAtom> allAtoms = distinctAtoms(functionService);

		List<OperationAtom> sameNameOtherKind = allAtoms.stream()
				.filter(a -> a.name().equals(name) && !a.getClass().equals(atom.getClass())).toList();
		if (!sameNameOtherKind.isEmpty()) {
			return "\"" + name + "\" is registered, but under a different kind of atom:" + NL
					+ sameNameOtherKind.stream().map(a -> "  " + SignatureText.ofAtom(a))
							.collect(Collectors.joining(NL))
					+ NL
					+ "Use assertThatFunction(functionService, \"" + name + "\", "
					+ sameNameOtherKind.get(0).getClass().getSimpleName()
					+ ".class), or pass the OperationAtom directly.";
		}

		List<OperationAtom> caseInsensitiveMatches = allAtoms.stream().filter(a -> a.name().equalsIgnoreCase(name))
				.toList();
		if (!caseInsensitiveMatches.isEmpty()) {
			return "No exact match for \"" + name + "\"; did you mean:" + NL
					+ caseInsensitiveMatches.stream().map(a -> "  " + SignatureText.ofAtom(a))
							.collect(Collectors.joining(NL))
					+ NL + "MDX function names are case-sensitive in this registry.";
		}

		List<String> suggestions = allAtoms.stream().map(OperationAtom::name).filter(n -> !n.isBlank()).distinct()
				.sorted(Comparator.comparingInt((String n) -> editDistance(n, name)).thenComparing(n -> n)).limit(5)
				.toList();
		String message = "No function named \"" + name + "\" is registered (" + allAtoms.size()
				+ " distinct atom(s) known to this service).";
		if (!suggestions.isEmpty()) {
			message += NL + "Closest registered names:" + NL
					+ suggestions.stream().map(s -> "  " + s).collect(Collectors.joining(NL));
		}
		return message;
	}

	/**
	 * Builds an atom of {@code atomClass} named {@code name} for a lookup, when the
	 * registry itself has nothing under that name to borrow the atom instance from.
	 */
	public static OperationAtom syntheticAtom(String name, Class<? extends OperationAtom> atomClass) {
		try {
			Constructor<? extends OperationAtom> nameConstructor = atomClass.getDeclaredConstructor(String.class);
			return nameConstructor.newInstance(name);
		} catch (NoSuchMethodException noStringConstructor) {
			try {
				Constructor<? extends OperationAtom> noArgConstructor = atomClass.getDeclaredConstructor();
				return noArgConstructor.newInstance();
			} catch (ReflectiveOperationException e) {
				throw new IllegalArgumentException(
						"Cannot construct a synthetic " + atomClass.getSimpleName() + " for \"" + name + "\"", e);
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException(
					"Cannot construct a synthetic " + atomClass.getSimpleName() + " for \"" + name + "\"", e);
		}
	}

	private static List<OperationAtom> distinctAtoms(FunctionService functionService) {
		Set<OperationAtom> atoms = new LinkedHashSet<>();
		for (FunctionResolver resolver : functionService.getResolvers()) {
			atoms.add(resolver.getFunctionAtom());
		}
		return new ArrayList<>(atoms);
	}

	/** Levenshtein distance, for ranking name suggestions. */
	private static int editDistance(String a, String b) {
		int[][] d = new int[a.length() + 1][b.length() + 1];
		for (int i = 0; i <= a.length(); i++) {
			d[i][0] = i;
		}
		for (int j = 0; j <= b.length(); j++) {
			d[0][j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			for (int j = 1; j <= b.length(); j++) {
				int cost = Character.toLowerCase(a.charAt(i - 1)) == Character.toLowerCase(b.charAt(j - 1)) ? 0 : 1;
				d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
			}
		}
		return d[a.length()][b.length()];
	}
}
