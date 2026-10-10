package org.andrew.jpad.runtime;

public final class JPad {

    @FunctionalInterface
    public interface DumpConsumer {
        void accept(Object obj, String title);
    }

    private static volatile DumpConsumer currentConsumer = null;

    public static void setDumpConsumer(DumpConsumer consumer) {
        currentConsumer = consumer;
    }

    public static <T> T dump(T obj) {
        return dump(obj, null);
    }

    public static <T> T dump(T obj, String title) {
        DumpConsumer consumer = currentConsumer;
        if (consumer != null) {
            consumer.accept(obj, title);
        } else {
            System.out.println(obj);
        }
        return obj;
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, Object> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T cache(String key, java.util.function.Supplier<T> supplier) {
        String effectiveKey = (key != null && !key.trim().isEmpty()) ? key.trim() : autoCacheKey();
        return (T) CACHE.computeIfAbsent(effectiveKey, k -> supplier.get());
    }

    public static <T> T cache(java.util.function.Supplier<T> supplier) {
        return cache(autoCacheKey(), supplier);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static boolean clearCache(String key) {
        if (key == null) return false;
        return CACHE.remove(key.trim()) != null;
    }

    public static boolean isCached(String key) {
        if (key == null) return false;
        return CACHE.containsKey(key.trim());
    }

    public static int cacheSize() {
        return CACHE.size();
    }

    private static String autoCacheKey() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        for (StackTraceElement elem : st) {
            String cls = elem.getClassName();
            if (!cls.equals(JPad.class.getName()) && !cls.startsWith("java.lang.Thread")) {
                return cls + "." + elem.getMethodName() + ":" + elem.getLineNumber();
            }
        }
        return "jpad_cached_default";
    }

    public static final class BenchmarkResult implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final int warmupIterations;
        private final int iterations;
        private final double totalTimeMs;
        private final double avgTimeMs;
        private final double minTimeMs;
        private final double maxTimeMs;
        private final double medianTimeMs;
        private final double p95TimeMs;
        private final double p99TimeMs;
        private final double throughputOpsPerSec;

        public BenchmarkResult(
            String name,
            int warmupIterations,
            int iterations,
            double totalTimeMs,
            double avgTimeMs,
            double minTimeMs,
            double maxTimeMs,
            double medianTimeMs,
            double p95TimeMs,
            double p99TimeMs,
            double throughputOpsPerSec
        ) {
            this.name = name;
            this.warmupIterations = warmupIterations;
            this.iterations = iterations;
            this.totalTimeMs = totalTimeMs;
            this.avgTimeMs = avgTimeMs;
            this.minTimeMs = minTimeMs;
            this.maxTimeMs = maxTimeMs;
            this.medianTimeMs = medianTimeMs;
            this.p95TimeMs = p95TimeMs;
            this.p99TimeMs = p99TimeMs;
            this.throughputOpsPerSec = throughputOpsPerSec;
        }

        public String getName() { return name; }
        public int getWarmupIterations() { return warmupIterations; }
        public int getIterations() { return iterations; }
        public double getTotalTimeMs() { return totalTimeMs; }
        public double getAvgTimeMs() { return avgTimeMs; }
        public double getMinTimeMs() { return minTimeMs; }
        public double getMaxTimeMs() { return maxTimeMs; }
        public double getMedianTimeMs() { return medianTimeMs; }
        public double getP95TimeMs() { return p95TimeMs; }
        public double getP99TimeMs() { return p99TimeMs; }
        public double getThroughputOpsPerSec() { return throughputOpsPerSec; }

        @Override
        public String toString() {
            return String.format(
                "Benchmark '%s' [%d ops]: avg=%.4f ms, median=%.4f ms, min=%.4f ms, max=%.4f ms, p95=%.4f ms, throughput=%.1f ops/sec",
                name, iterations, avgTimeMs, medianTimeMs, minTimeMs, maxTimeMs, p95TimeMs, throughputOpsPerSec
            );
        }
    }

    public static BenchmarkResult benchmark(String name, int iterations, Runnable task) {
        int targetIterations = Math.max(1, iterations);
        int warmup = Math.max(2, Math.min(10, targetIterations / 5));

        // Warmup phase for JIT optimizations
        for (int i = 0; i < warmup; i++) {
            task.run();
        }

        double[] timesNs = new double[targetIterations];
        long startTotal = System.nanoTime();
        for (int i = 0; i < targetIterations; i++) {
            long t0 = System.nanoTime();
            task.run();
            long t1 = System.nanoTime();
            timesNs[i] = (double) (t1 - t0);
        }
        long endTotal = System.nanoTime();

        java.util.Arrays.sort(timesNs);
        double totalTimeMs = (endTotal - startTotal) / 1_000_000.0;
        double sumNs = 0.0;
        for (double d : timesNs) {
            sumNs += d;
        }
        double avgTimeMs = (sumNs / targetIterations) / 1_000_000.0;
        double minTimeMs = timesNs[0] / 1_000_000.0;
        double maxTimeMs = timesNs[targetIterations - 1] / 1_000_000.0;
        double medianTimeMs = timesNs[targetIterations / 2] / 1_000_000.0;
        double p95TimeMs = timesNs[(int) Math.min(targetIterations - 1, targetIterations * 0.95)] / 1_000_000.0;
        double p99TimeMs = timesNs[(int) Math.min(targetIterations - 1, targetIterations * 0.99)] / 1_000_000.0;

        double elapsedSeconds = (endTotal - startTotal) / 1_000_000_000.0;
        double throughput = elapsedSeconds > 0 ? (targetIterations / elapsedSeconds) : 0.0;

        BenchmarkResult result = new BenchmarkResult(
            name, warmup, targetIterations, totalTimeMs, avgTimeMs, minTimeMs, maxTimeMs, medianTimeMs, p95TimeMs, p99TimeMs, throughput
        );

        dump(result, "Benchmark: " + name);
        return result;
    }

    public static BenchmarkResult benchmark(String name, Runnable task) {
        return benchmark(name, 50, task);
    }

    public static BenchmarkResult benchmark(Runnable task) {
        return benchmark("Anonymous Benchmark", 50, task);
    }

    public static <T> T benchmark(String name, int iterations, java.util.function.Supplier<T> task) {
        benchmark(name, iterations, () -> { task.get(); });
        return task.get();
    }

    public static <T> T benchmark(String name, java.util.function.Supplier<T> task) {
        return benchmark(name, 50, task);
    }

    public static java.util.List<java.util.Map<String, Object>> query(String sql) {
        String dbPath = System.getProperty("user.home") + "/.jpad/databases/sample.db";
        return query("jdbc:sqlite:" + dbPath, sql);
    }

    public static java.util.List<java.util.Map<String, Object>> queryAndDump(String sql) {
        java.util.List<java.util.Map<String, Object>> rows = query(sql);
        dump(rows, "Query: " + (sql.length() > 30 ? sql.substring(0, 30) + "..." : sql));
        return rows;
    }

    public static java.util.List<java.util.Map<String, Object>> queryAndDump(String jdbcUrl, String sql) {
        java.util.List<java.util.Map<String, Object>> rows = query(jdbcUrl, sql);
        dump(rows, "Query: " + (sql.length() > 30 ? sql.substring(0, 30) + "..." : sql));
        return rows;
    }

    public static java.util.List<String> tables() {
        String dbPath = System.getProperty("user.home") + "/.jpad/databases/sample.db";
        return tables("jdbc:sqlite:" + dbPath);
    }

    public static java.util.List<String> tables(String jdbcUrl) {
        java.util.List<String> tables = new java.util.ArrayList<>();
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(jdbcUrl);
             java.sql.ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch tables: " + e.getMessage(), e);
        }
        return tables;
    }

    public static java.util.List<java.util.Map<String, Object>> query(String jdbcUrl, String sql) {
        java.util.List<java.util.Map<String, Object>> results = new java.util.ArrayList<>();
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(jdbcUrl);
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            String[] cols = new String[colCount];
            for (int i = 1; i <= colCount; i++) {
                cols[i - 1] = meta.getColumnLabel(i);
            }
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(cols[i - 1], rs.getObject(i));
                }
                results.add(row);
            }
        } catch (Exception e) {
            throw new RuntimeException("SQL Query failed: " + e.getMessage(), e);
        }
        return results;
    }
}
