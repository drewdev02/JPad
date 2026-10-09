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

    public static java.util.List<java.util.Map<String, Object>> query(String sql) {
        String dbPath = System.getProperty("user.home") + "/.jpad/databases/sample.db";
        return query("jdbc:sqlite:" + dbPath, sql);
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
