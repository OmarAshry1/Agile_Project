package edu.facilities.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/** Supabase PostgreSQL connection pool. Credentials are read from env or ignored local config. */
public final class DatabaseConnection {
    private static HikariDataSource dataSource;
    private DatabaseConnection() { }

    private static String value(Properties p, String key, String env, String fallback) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank() || v.startsWith("YOUR_")) v = System.getenv(env);
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    private static synchronized void initialize() {
        if (dataSource != null && !dataSource.isClosed()) return;
        Properties p = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (in != null) p.load(in);
        } catch (Exception ignored) { }
        String host = value(p, "db.host", "SUPABASE_DB_HOST", "localhost");
        String port = value(p, "db.port", "SUPABASE_DB_PORT", "5432");
        String name = value(p, "db.name", "SUPABASE_DB_NAME", "postgres");
        String user = value(p, "db.user", "SUPABASE_DB_USER", "postgres");
        String password = value(p, "db.password", "SUPABASE_DB_PASSWORD", "");
        String ssl = value(p, "db.ssl", "SUPABASE_DB_SSL", "true");
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + name + "?sslmode=" + (Boolean.parseBoolean(ssl) ? "require" : "disable"));
        c.setUsername(user); c.setPassword(password); c.setMaximumPoolSize(5); c.setMinimumIdle(1);
        c.setConnectionTimeout(10000); c.setPoolName("UniversityManagementSupabasePool");
        dataSource = new HikariDataSource(c);
    }

    public static Connection getConnection() throws SQLException {
        initialize();
        return dataSource.getConnection();
    }

    public static boolean testConnection() {
        try (Connection c = getConnection()) { return c.isValid(3); }
        catch (SQLException e) { System.err.println("Supabase connection unavailable: " + e.getMessage()); return false; }
    }

    public static synchronized void close() { if (dataSource != null) dataSource.close(); }
}