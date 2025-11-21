package edu.facilities.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Legacy database connection class
 * NOTE: This class is deprecated. Use DatabaseConnection instead.
 * Kept for backward compatibility but delegates to DatabaseConnection.
 */
@Deprecated
public class Database {

    // Get connection parameters from environment variables with fallback defaults
    private static final String HOST = getEnvOrDefault("MSSQL_HOST", "DESKTOP-U4EMGMM");
    private static final String PORT = getEnvOrDefault("MSSQL_PORT", "1433");
    private static final String DATABASE = getEnvOrDefault("MSSQL_DB", "agile");
    private static final String USERNAME = getEnvOrDefault("MSSQL_USER", "sa");
    private static final String PASSWORD = getEnvOrDefault("MSSQL_PASSWORD", "Password123");
    
    // Build connection URL - using SQL Authentication (NOT Windows Auth)
    private static final String URL = String.format(
        "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;loginTimeout=30;",
        HOST, PORT, DATABASE
    );

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQL Server JDBC driver not found", e);
        }
    }

    /**
     * Get database connection using SQL Authentication
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        // Use SQL Authentication with username and password
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
    
    /**
     * Get environment variable or return default value
     */
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }
}