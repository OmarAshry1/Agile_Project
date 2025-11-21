package edu.facilities.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection utility for SQL Server
 * Uses SQL Authentication (username/password) - no DLL required
 */
public class DatabaseConnection {

    // Get connection parameters from environment variables with fallback defaults
    private static final String HOST = getEnvOrDefault("MSSQL_HOST", "localhost");
    private static final String PORT = getEnvOrDefault("MSSQL_PORT", "1433");
    private static final String DATABASE = getEnvOrDefault("MSSQL_DB", "agile");
    private static final String USERNAME = getEnvOrDefault("MSSQL_USER", "sa");
    private static final String PASSWORD = getEnvOrDefault("MSSQL_PASSWORD", "Password123");
    
    // Build connection URL - using SQL Authentication (not Windows Auth)
    // encrypt=false to avoid SSL certificate issues
    private static final String DB_URL = String.format(
        "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=false;trustServerCertificate=true;loginTimeout=30;",
        HOST, PORT, DATABASE
    );

    private static Connection connection = null;

    /**
     * Get a database connection using SQL Authentication
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load driver explicitly (JDBC 4+ auto-loads, but explicit is safer)
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                
                // Connect with username and password (SQL Authentication)
                connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
                
                System.out.println("✓ Connected to SQL Server: " + HOST + ":" + PORT + "/" + DATABASE);
            } catch (ClassNotFoundException e) {
                throw new SQLException(
                    "SQL Server JDBC Driver not found. Ensure mssql-jdbc is in classpath.", e);
            } catch (SQLException e) {
                System.err.println("Connection URL: " + DB_URL);
                System.err.println("Host: " + HOST + ", Port: " + PORT + ", Database: " + DATABASE);
                throw new SQLException(
                    "Failed to connect to SQL Server. Check: 1) Server is running, 2) Port is open, 3) Credentials are correct. Error: " + e.getMessage(), e);
            }
        }
        return connection;
    }

    /**
     * Close the database connection
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
                System.out.println("✓ Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    /**
     * Test database connection
     * @return true if connection successful
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            boolean isValid = conn != null && !conn.isClosed();
            if (isValid) {
                System.out.println("✓ Database connection test: SUCCESS");
            }
            return isValid;
        } catch (SQLException e) {
            System.err.println("✗ Database connection test: FAILED");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get environment variable or return default value
     */
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }

    /**
     * Get the connection URL (for debugging)
     */
    public static String getConnectionUrl() {
        return DB_URL;
    }
}