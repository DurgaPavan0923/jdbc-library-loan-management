package com.library.benchmark;

import com.library.connection.ConnectionManager;

import java.sql.*;
import java.util.*;
import java.nio.file.*;
import java.time.LocalDateTime;

/**
 * Benchmarking framework for comparing JDBC access patterns.
 *
 * Test Suites:
 * 1. Insert Strategy
 * 2. Query Strategy
 * 3. Statement Comparison
 * 4. Transaction Granularity
 *
 * Features:
 * - Warm-up runs
 * - Mean & Standard Deviation
 * - Throughput calculation
 * - CSV report generation
 * - Timestamped reports
 */
public class PerformanceEvaluator {

    private static final int RUNS = 5;
    private static final int SMALL_BATCH = 1000;
    private static final int LARGE_BATCH = 10000;
    private static final int TX_OPS = 100;

    private static final String BENCH_TABLE = "BenchmarkInserts";

    private final ConnectionManager cm;
    private final List<BenchmarkResult> results = new ArrayList<>();

    public PerformanceEvaluator(ConnectionManager cm) {
        this.cm = cm;
    }

    // =========================================================
    // ENTRY POINT
    // =========================================================

    public void runAll() {

        System.out.println("\n" + "=".repeat(70));
        System.out.println(" PERFORMANCE EVALUATION FRAMEWORK ");
        System.out.println("=".repeat(70));

        try {

            setupBenchmarkTable();

            benchmarkInsertStrategies();

            benchmarkQueryStrategies();

            benchmarkStatementTypes();

            benchmarkTransactionGranularity();

            printReport();

        } catch (SQLException e) {

            System.err.println("[PerformanceEvaluator] Fatal Error");

            e.printStackTrace();

        } finally {

            teardownBenchmarkTable();
        }
    }

    // =========================================================
    // SUITE 1 - INSERT STRATEGIES
    // =========================================================

    private void benchmarkInsertStrategies() throws SQLException {

        System.out.println("\n── Insert Strategy Benchmark ──");

        for (int count : new int[]{SMALL_BATCH, LARGE_BATCH}) {

            double[] individualTimes =
                    runBenchmark("Individual Inserts " + count, () -> {

                        individualInserts(count);

                        clearBenchmarkData();
                    });

            results.add(new BenchmarkResult(
                    "Insert - Individual",
                    count + " rows",
                    mean(individualTimes),
                    stdDev(individualTimes),
                    throughput(count, mean(individualTimes)),
                    "One executeUpdate() per row"
            ));

            double[] batchTimes =
                    runBenchmark("Batch Inserts " + count, () -> {

                        batchInserts(count);

                        clearBenchmarkData();
                    });

            results.add(new BenchmarkResult(
                    "Insert - Batch",
                    count + " rows",
                    mean(batchTimes),
                    stdDev(batchTimes),
                    throughput(count, mean(batchTimes)),
                    "addBatch()/executeBatch()"
            ));

            System.out.printf(
                    " %,d rows | Individual: %.2f ms | Batch: %.2f ms | Speedup: %.2fx%n",
                    count,
                    mean(individualTimes),
                    mean(batchTimes),
                    mean(individualTimes) / mean(batchTimes)
            );
        }
    }

    private void individualInserts(int count) throws SQLException {

        String sql =
                "INSERT INTO " + BENCH_TABLE +
                        " (DataKey, DataValue) VALUES (?, ?)";

        try (Connection conn = cm.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement ps =
                         conn.prepareStatement(sql)) {

                for (int i = 0; i < count; i++) {

                    ps.setString(1, "key_" + i);

                    ps.setString(2, "value_" + i);

                    ps.executeUpdate();
                }

                conn.commit();

            } catch (SQLException e) {

                conn.rollback();

                throw e;
            }
        }
    }

    private void batchInserts(int count) throws SQLException {

        String sql =
                "INSERT INTO " + BENCH_TABLE +
                        " (DataKey, DataValue) VALUES (?, ?)";

        try (Connection conn = cm.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement ps =
                         conn.prepareStatement(sql)) {

                for (int i = 0; i < count; i++) {

                    ps.setString(1, "key_" + i);

                    ps.setString(2, "value_" + i);

                    ps.addBatch();

                    if ((i + 1) % 500 == 0) {
                        ps.executeBatch();
                    }
                }

                ps.executeBatch();

                conn.commit();

            } catch (SQLException e) {

                conn.rollback();

                throw e;
            }
        }
    }

    // =========================================================
    // SUITE 2 - QUERY STRATEGIES
    // =========================================================

    private void benchmarkQueryStrategies() throws SQLException {

        System.out.println("\n── Query Strategy Benchmark ──");

        seedLoansForQuery(500);

        double[] scanTimes =
                runBenchmark("Full Table Scan", () -> {

                    try (Connection conn = cm.getConnection();
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(
                                 "SELECT * FROM Loans")) {

                        while (rs.next()) {
                        }
                    }
                });

        results.add(new BenchmarkResult(
                "Query - Full Scan",
                "500 rows",
                mean(scanTimes),
                stdDev(scanTimes),
                throughput(500, mean(scanTimes)),
                "Sequential table scan"
        ));

        double[] indexedTimes =
                runBenchmark("Indexed Query", () -> {

                    try (Connection conn = cm.getConnection();
                         PreparedStatement ps =
                                 conn.prepareStatement(
                                         "SELECT * FROM Loans WHERE MemberID=?")) {

                        ps.setInt(1, 1);

                        try (ResultSet rs = ps.executeQuery()) {

                            while (rs.next()) {
                            }
                        }
                    }
                });

        results.add(new BenchmarkResult(
                "Query - Indexed",
                "MemberID",
                mean(indexedTimes),
                stdDev(indexedTimes),
                throughput(1, mean(indexedTimes)),
                "Indexed lookup"
        ));

        cleanupSeedLoans();
    }

    // =========================================================
    // SUITE 3 - STATEMENT COMPARISON
    // =========================================================

    private void benchmarkStatementTypes() throws SQLException {

        System.out.println("\n── Statement Benchmark ──");

        final int OPS = 200;

        double[] stmtTimes =
                runBenchmark("Statement", () -> {

                    try (Connection conn = cm.getConnection()) {

                        conn.setAutoCommit(false);

                        try (Statement stmt =
                                     conn.createStatement()) {

                            for (int i = 0; i < OPS; i++) {

                                stmt.executeUpdate(
                                        "INSERT INTO "
                                                + BENCH_TABLE +
                                                " (DataKey, DataValue) VALUES "
                                                + "('stmt_" + i + "', 'val_" + i + "')"
                                );
                            }

                            conn.commit();

                        } catch (SQLException e) {

                            conn.rollback();

                            throw e;
                        }
                    }

                    clearBenchmarkData();
                });

        results.add(new BenchmarkResult(
                "Statement",
                OPS + " ops",
                mean(stmtTimes),
                stdDev(stmtTimes),
                throughput(OPS, mean(stmtTimes)),
                "String concatenation"
        ));

        double[] psTimes =
                runBenchmark("PreparedStatement", () -> {

                    try (Connection conn = cm.getConnection()) {

                        conn.setAutoCommit(false);

                        try (PreparedStatement ps =
                                     conn.prepareStatement(
                                             "INSERT INTO "
                                                     + BENCH_TABLE +
                                                     " (DataKey, DataValue) VALUES (?, ?)")) {

                            for (int i = 0; i < OPS; i++) {

                                ps.setString(1, "ps_" + i);

                                ps.setString(2, "val_" + i);

                                ps.executeUpdate();
                            }

                            conn.commit();

                        } catch (SQLException e) {

                            conn.rollback();

                            throw e;
                        }
                    }

                    clearBenchmarkData();
                });

        results.add(new BenchmarkResult(
                "PreparedStatement",
                OPS + " ops",
                mean(psTimes),
                stdDev(psTimes),
                throughput(OPS, mean(psTimes)),
                "Precompiled execution plan"
        ));
    }

    // =========================================================
    // SUITE 4 - TRANSACTION GRANULARITY
    // =========================================================

    private void benchmarkTransactionGranularity() throws SQLException {

        System.out.println("\n── Transaction Granularity Benchmark ──");

        double[] perOpTimes =
                runBenchmark("Per-operation Commit", () -> {

                    try (Connection conn = cm.getConnection()) {

                        conn.setAutoCommit(true);

                        try (PreparedStatement ps =
                                     conn.prepareStatement(
                                             "INSERT INTO "
                                                     + BENCH_TABLE +
                                                     " (DataKey, DataValue) VALUES (?, ?)")) {

                            for (int i = 0; i < TX_OPS; i++) {

                                ps.setString(1, "tx_" + i);

                                ps.setString(2, "val_" + i);

                                ps.executeUpdate();
                            }
                        }
                    }

                    clearBenchmarkData();
                });

        results.add(new BenchmarkResult(
                "Per-operation Commit",
                TX_OPS + " ops",
                mean(perOpTimes),
                stdDev(perOpTimes),
                throughput(TX_OPS, mean(perOpTimes)),
                "Each operation commits individually"
        ));

        double[] batchedTimes =
                runBenchmark("Batched Commit", () -> {

                    try (Connection conn = cm.getConnection()) {

                        conn.setAutoCommit(false);

                        try (PreparedStatement ps =
                                     conn.prepareStatement(
                                             "INSERT INTO "
                                                     + BENCH_TABLE +
                                                     " (DataKey, DataValue) VALUES (?, ?)")) {

                            for (int i = 0; i < TX_OPS; i++) {

                                ps.setString(1, "btx_" + i);

                                ps.setString(2, "val_" + i);

                                ps.executeUpdate();
                            }

                            conn.commit();

                        } catch (SQLException e) {

                            conn.rollback();

                            throw e;
                        }
                    }

                    clearBenchmarkData();
                });

        results.add(new BenchmarkResult(
                "Batched Commit",
                TX_OPS + " ops",
                mean(batchedTimes),
                stdDev(batchedTimes),
                throughput(TX_OPS, mean(batchedTimes)),
                "Single transaction commit"
        ));
    }

    // =========================================================
    // BENCHMARK RUNNER
    // =========================================================

    @FunctionalInterface
    interface BenchmarkTask {
        void run() throws Exception;
    }

    private double[] runBenchmark(
            String label,
            BenchmarkTask task) {

        double[] times = new double[RUNS - 1];

        System.out.printf(" Running %-40s ", label);

        for (int run = 0; run < RUNS; run++) {

            System.gc();

            long start = System.nanoTime();

            try {

                task.run();

            } catch (Exception e) {

                System.err.println(
                        "\n[Benchmark Error] " + e.getMessage());
            }

            long elapsed = System.nanoTime() - start;

            if (run > 0) {

                times[run - 1] =
                        elapsed / 1_000_000.0;
            }
        }

        System.out.printf(" Mean: %.2f ms%n", mean(times));

        return times;
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    private double mean(double[] arr) {

        double sum = 0;

        for (double v : arr) {
            sum += v;
        }

        return arr.length == 0 ? 0 : sum / arr.length;
    }

    private double stdDev(double[] arr) {

        double m = mean(arr);

        double variance = 0;

        for (double v : arr) {

            variance += (v - m) * (v - m);
        }

        return arr.length < 2
                ? 0
                : Math.sqrt(variance / (arr.length - 1));
    }

    private double throughput(int ops, double ms) {

        return ms <= 0
                ? 0
                : (ops / (ms / 1000.0));
    }

    // =========================================================
    // REPORT PRINTING
    // =========================================================

    private void printReport() {

        System.out.println("\n" + "=".repeat(120));

        System.out.println(" PERFORMANCE REPORT ");

        System.out.println("=".repeat(120));

        System.out.printf(
                "%-35s %-20s %-12s %-12s %-15s %s%n",
                "Test",
                "Scale",
                "Avg(ms)",
                "StdDev",
                "Throughput",
                "Observation"
        );

        System.out.println("-".repeat(120));

        for (BenchmarkResult r : results) {

            System.out.printf(
                    "%-35s %-20s %-12.2f %-12.2f %-15.0f %s%n",
                    r.test(),
                    r.scale(),
                    r.avgMs(),
                    r.stdDevMs(),
                    r.throughput(),
                    r.observation()
            );
        }

        System.out.println("=".repeat(120));

        writeCsv();
    }

    // =========================================================
    // CSV REPORT GENERATION
    // =========================================================

    private void writeCsv() {

        try {

            Path reportsDir = Path.of("reports");

            if (!Files.exists(reportsDir)) {

                Files.createDirectories(reportsDir);
            }

            String timestamp =
                    LocalDateTime.now()
                            .toString()
                            .replace(":", "-");

            Path csvPath =
                    reportsDir.resolve(
                            "performance_report_" +
                                    timestamp +
                                    ".csv"
                    );

            StringBuilder sb = new StringBuilder();

            sb.append(
                    "Test,Scale,Avg_ms,StdDev_ms,Throughput_ops_sec,Observation\n");

            for (BenchmarkResult r : results) {

                sb.append(String.format(
                        "\"%s\",\"%s\",%.2f,%.2f,%.0f,\"%s\"%n",
                        r.test(),
                        r.scale(),
                        r.avgMs(),
                        r.stdDevMs(),
                        r.throughput(),
                        r.observation()
                ));
            }

            Files.writeString(csvPath, sb.toString());

            System.out.println("\n"
                    + "=".repeat(70));

            System.out.println(
                    "[PerformanceEvaluator] CSV Report Generated");

            System.out.println(
                    "Location : " + csvPath.toAbsolutePath());

            System.out.println(
                    "Total Benchmark Suites : "
                            + results.size());

            System.out.println(
                    "=".repeat(70));

        } catch (Exception e) {

            System.err.println(
                    "[PerformanceEvaluator] Failed to write CSV");

            e.printStackTrace();
        }
    }

    // =========================================================
    // TABLE MANAGEMENT
    // =========================================================

    private void setupBenchmarkTable() throws SQLException {

        teardownBenchmarkTable();

        try (Connection conn = cm.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                    "CREATE TABLE " + BENCH_TABLE + " ("
                            + "ID INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
                            + "DataKey VARCHAR(100),"
                            + "DataValue VARCHAR(200))"
            );

            System.out.println(
                    "[PerformanceEvaluator] Benchmark table created.");
        }
    }

    private void teardownBenchmarkTable() {

        try (Connection conn = cm.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE " + BENCH_TABLE);

        } catch (SQLException ignored) {
        }
    }

    private void clearBenchmarkData() throws SQLException {

        try (Connection conn = cm.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DELETE FROM " + BENCH_TABLE);
        }
    }

    // =========================================================
    // LOAN DATA FOR QUERY TESTS
    // =========================================================

    private void seedLoansForQuery(int count)
            throws SQLException {

        String sql =
                "INSERT INTO Loans "
                        + "(BookID, MemberID, LoanDate, ReturnDate) "
                        + "VALUES (?, ?, CURRENT_DATE, CURRENT_DATE)";

        try (Connection conn = cm.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement ps =
                         conn.prepareStatement(sql)) {

                for (int i = 0; i < count; i++) {

                    ps.setInt(1, (i % 10) + 1);

                    ps.setInt(2, (i % 5) + 1);

                    ps.addBatch();
                }

                ps.executeBatch();

                conn.commit();

            } catch (SQLException e) {

                conn.rollback();
            }
        }
    }

    private void cleanupSeedLoans() {

        try (Connection conn = cm.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DELETE FROM Loans");

        } catch (SQLException ignored) {
        }
    }

    // =========================================================
    // RESULT RECORD
    // =========================================================

    private record BenchmarkResult(
            String test,
            String scale,
            double avgMs,
            double stdDevMs,
            double throughput,
            String observation
    ) {
    }
}
