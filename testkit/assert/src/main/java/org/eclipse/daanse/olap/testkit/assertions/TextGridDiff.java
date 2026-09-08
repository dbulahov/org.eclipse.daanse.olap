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
package org.eclipse.daanse.olap.testkit.assertions;

import java.util.ArrayList;
import java.util.List;

/**
 * Side-by-side diff of two multi-line texts.
 *
 * <p>Byte-identical to {@code org.eclipse.daanse.rolap.testkit.assertions.GridDiff},
 * which is package-private and therefore unreachable from other bundles. Once this class
 * ships, that one should delegate here so there is one implementation.
 *
 * <p>Layout: line numbers on both sides, a {@code |} marker for equal lines and
 * {@code ≠} for differing ones, columns capped at 80 characters with an ellipsis, and
 * runs of more than {@code 2*CONTEXT+1} equal lines collapsed to a placeholder.
 */
public final class TextGridDiff {

    private static final int COLUMN_WIDTH = 80;
    private static final int CONTEXT = 2;
    /** Above this many DP cells the LCS alignment is skipped in favour of index pairing. */
    private static final long MAX_ALIGN_CELLS = 4_000_000L;

    private TextGridDiff() {
    }

    /** Returns the rendered diff, or the empty string when both texts are equal. */
    public static String render(String expected, String actual) {
        if (expected.equals(actual)) {
            return "";
        }
        String[] left = expected.split("\r\n|\r|\n", -1);
        String[] right = actual.split("\r\n|\r|\n", -1);

        List<Row> rows = (long) left.length * right.length <= MAX_ALIGN_CELLS
                ? alignedRows(left, right)
                : zippedRows(left, right);
        rows = collapseEqualRuns(rows);

        int width = 1;
        for (Row row : rows) {
            if (!row.collapse()) {
                width = Math.max(width, Math.max(lengthOf(row.left()), lengthOf(row.right())));
            }
        }
        width = Math.min(COLUMN_WIDTH, width);

        int leftNoWidth = Integer.toString(left.length).length();
        int rightNoWidth = Integer.toString(right.length).length();
        String format = "%" + leftNoWidth + "s %-" + width + "s %s %-" + width + "s %" + rightNoWidth + "s%n";

        StringBuilder buf = new StringBuilder();
        for (Row row : rows) {
            if (row.collapse()) {
                buf.append("  ... (").append(row.collapsedCount()).append(" matching lines omitted) ...")
                   .append(System.lineSeparator());
                continue;
            }
            buf.append(String.format(format,
                    row.leftNo() == 0 ? "" : Integer.toString(row.leftNo()),
                    truncate(row.left(), width),
                    row.equal() ? "|" : "≠",
                    truncate(row.right(), width),
                    row.rightNo() == 0 ? "" : Integer.toString(row.rightNo())));
        }
        return buf.toString();
    }

    // ---- alignment ---------------------------------------------------------

    /** Longest-common-subsequence alignment; delete/insert runs are paired into change rows. */
    private static List<Row> alignedRows(String[] left, String[] right) {
        int n = left.length;
        int m = right.length;
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                lcs[i][j] = left[i].equals(right[j])
                        ? lcs[i + 1][j + 1] + 1
                        : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        List<Row> rows = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            if (left[i].equals(right[j])) {
                rows.add(Row.of(i + 1, left[i], j + 1, right[j], true));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                rows.add(Row.of(i + 1, left[i], 0, null, false));
                i++;
            } else {
                rows.add(Row.of(0, null, j + 1, right[j], false));
                j++;
            }
        }
        while (i < n) {
            rows.add(Row.of(i + 1, left[i], 0, null, false));
            i++;
        }
        while (j < m) {
            rows.add(Row.of(0, null, j + 1, right[j], false));
            j++;
        }
        return mergeChangeRuns(rows);
    }

    /** Pairs a delete run with the insert run that follows it, so changes appear side by side. */
    private static List<Row> mergeChangeRuns(List<Row> rows) {
        List<Row> merged = new ArrayList<>(rows.size());
        int k = 0;
        while (k < rows.size()) {
            Row row = rows.get(k);
            if (row.equal()) {
                merged.add(row);
                k++;
                continue;
            }
            List<Row> deletes = new ArrayList<>();
            List<Row> inserts = new ArrayList<>();
            while (k < rows.size() && !rows.get(k).equal() && rows.get(k).rightNo() == 0) {
                deletes.add(rows.get(k++));
            }
            while (k < rows.size() && !rows.get(k).equal() && rows.get(k).leftNo() == 0) {
                inserts.add(rows.get(k++));
            }
            int pairs = Math.max(deletes.size(), inserts.size());
            for (int p = 0; p < pairs; p++) {
                Row d = p < deletes.size() ? deletes.get(p) : null;
                Row ins = p < inserts.size() ? inserts.get(p) : null;
                merged.add(Row.of(d == null ? 0 : d.leftNo(), d == null ? null : d.left(),
                        ins == null ? 0 : ins.rightNo(), ins == null ? null : ins.right(), false));
            }
        }
        return merged;
    }

    /** Fallback for very large inputs: pair purely by index. */
    private static List<Row> zippedRows(String[] left, String[] right) {
        List<Row> rows = new ArrayList<>();
        int max = Math.max(left.length, right.length);
        for (int k = 0; k < max; k++) {
            String l = k < left.length ? left[k] : null;
            String r = k < right.length ? right[k] : null;
            rows.add(Row.of(l == null ? 0 : k + 1, l, r == null ? 0 : k + 1, r,
                    l != null && l.equals(r)));
        }
        return rows;
    }

    private static List<Row> collapseEqualRuns(List<Row> rows) {
        List<Row> out = new ArrayList<>(rows.size());
        int k = 0;
        while (k < rows.size()) {
            if (!rows.get(k).equal()) {
                out.add(rows.get(k++));
                continue;
            }
            int start = k;
            while (k < rows.size() && rows.get(k).equal()) {
                k++;
            }
            int runLength = k - start;
            if (runLength > 2 * CONTEXT + 1) {
                for (int c = 0; c < CONTEXT; c++) {
                    out.add(rows.get(start + c));
                }
                out.add(Row.collapse(runLength - 2 * CONTEXT));
                for (int c = CONTEXT; c > 0; c--) {
                    out.add(rows.get(k - c));
                }
            } else {
                out.addAll(rows.subList(start, k));
            }
        }
        return out;
    }

    private static int lengthOf(String s) {
        return s == null ? 0 : s.length();
    }

    private static String truncate(String s, int width) {
        if (s == null) {
            return "";
        }
        if (s.length() <= width) {
            return s;
        }
        return width <= 1 ? s.substring(0, width) : s.substring(0, width - 1) + "…";
    }

    private record Row(boolean collapse, int collapsedCount, int leftNo, String left,
            int rightNo, String right, boolean equal) {

        static Row of(int leftNo, String left, int rightNo, String right, boolean equal) {
            return new Row(false, 0, leftNo, left, rightNo, right, equal);
        }

        static Row collapse(int count) {
            return new Row(true, count, 0, null, 0, null, true);
        }
    }
}