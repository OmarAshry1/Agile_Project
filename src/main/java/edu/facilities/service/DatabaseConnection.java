package edu.facilities.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection utility for Supabase (PostgreSQL) with connection pooling
 * Uses HikariCP for efficient connection management
 * Configuration priority: database.local.properties > environment variables > defaults
 */
public class DatabaseConnection {

    private static Properties config = loadConfiguration();
    
    // Get connection parameters from config file, then environment variables, then defaults
    private static final String HOST = getConfigValue("supabase.host", "SUPABASE_HOST", "db.your-project.supabase.co");
    private static final String PORT = getConfigValue("supabase.port", "SUPABASE_PORT", "6543");
    private static final String DATABASE = getConfigValue("supabase.database", "SUPABASE_DB", "postgres");
    private static final String USERNAME = getConfigValue("supabase.user", "SUPABASE_USER", "postgres");
    private static final String PASSWORD = getConfigValue("supabase.password", "SUPABASE_PASSWORD", "");
    
    private static final boolean USE_POOLER = Boolean.parseBoolean(
        getConfigValue("supabase.use.pooler", "SUPABASE_USE_POOLER", "true"));
    
    // Pool configuration
    private static final int MIN_POOL_SIZE = Integer.parseInt(
        getConfigValue("db.min.pool.size", "DB_MIN_POOL_SIZE", "2"));
    private static final int MAX_POOL_SIZE = Integer.parseInt(
        getConfigValue("db.max.pool.size", "DB_MAX_POOL_SIZE", "10"));
    private static final long CONNECTION_TIMEOUT = Long.parseLong(
        getConfigValue("db.connection.timeout", "DB_CONNECTION_TIMEOUT", "30000"));
    private static final long IDLE_TIMEOUT = Long.parseLong(
        getConfigValue("db.idle.timeout", "DB_IDLE_TIMEOUT", "600000"));
    private static final long MAX_LIFETIME = Long.parseLong(
        getConfigValue("db.max.lifetime", "DB_MAX_LIFETIME", "1800000"));
    private static final long LEAK_DETECTION_THRESHOLD = Long.parseLong(
        getConfigValue("db.leak.detection", "DB_LEAK_DETECTION", "60000"));

    private static HikariDataSource dataSource = null;

    /**
     * Load configuration from properties file
     * Priority: database.local.properties > database.properties
     */
    private static Properties loadConfiguration() {
        Properties props = new Properties();
        
        try {
            // Try to load local config first (for team members' personal configs)
            InputStream localConfig = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("database.local.properties");
            if (localConfig != null) {
                props.load(localConfig);
                localConfig.close();
                System.out.println("✓ Loaded database.local.properties");
                return props;
            }
            
            // Fall back to default config file
            InputStream defaultConfig = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("database.properties");
            if (defaultConfig != null) {
                props.load(defaultConfig);
                defaultConfig.close();
                System.out.println("✓ Loaded database.properties");
            }
        } catch (Exception e) {
            System.out.println("⚠ No database.properties file found, using environment variables/defaults");
        }
        
        return props;
    }

    /**
     * Get config value with priority: properties file > environment variable > default
     */
    private static String getConfigValue(String propKey, String envKey, String defaultValue) {
        // First try properties file
        String value = config.getProperty(propKey);
        if (value != null && !value.trim().isEmpty() && !value.equals("YOUR_PASSWORD_HERE")) {
            return value.trim();
        }
        
        // Then try environment variable
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        
        // Finally use default
        return defaultValue;
    }

    /**
     * Initialize the connection pool
     */
    private static synchronized void initializePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            return; // Already initialized
        }

        try {
            HikariConfig hikariConfig = new HikariConfig();
            
            // Build connection URL
            String jdbcUrl;
            if (USE_POOLER) {
                // Supabase connection pooler (recommended for production)
                // Port 6543 uses transaction mode pooling
                jdbcUrl = String.format(
                    "jdbc:postgresql://%s:%s/%s?sslmode=require",
                    HOST, PORT, DATABASE
                );
                hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
                hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            } else {
                // Direct connection (port 5432)
                jdbcUrl = String.format(
                    "jdbc:postgresql://%s:%s/%s?sslmode=require",
                    HOST, PORT, DATABASE
                );
            }
            
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(USERNAME);
            hikariConfig.setPassword(PASSWORD);
            hikariConfig.setDriverClassName("org.postgresql.Driver");
            
            // Connection pool settings
            hikariConfig.setMinimumIdle(MIN_POOL_SIZE);
            hikariConfig.setMaximumPoolSize(MAX_POOL_SIZE);
            hikariConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
            hikariConfig.setIdleTimeout(IDLE_TIMEOUT);
            hikariConfig.setMaxLifetime(MAX_LIFETIME);
            hikariConfig.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);
            
            // Connection validation
            hikariConfig.setConnectionTestQuery("SELECT 1");
            hikariConfig.setValidationTimeout(5000); // 5 seconds
            
            // Connection properties
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
            hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
            hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
            hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
            hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
            
            // Pool name for monitoring
            hikariConfig.setPoolName("SupabasePool");
            
            // Create the data source
            dataSource = new HikariDataSource(hikariConfig);
            
            System.out.println("✓ Connection pool initialized:");
            System.out.println("  - Host: " + HOST + ":" + PORT);
            System.out.println("  - Database: " + DATABASE);
            System.out.println("  - Mode: " + (USE_POOLER ? "Connection Pooler" : "Direct Connection"));
            System.out.println("  - Pool Size: " + MIN_POOL_SIZE + "-" + MAX_POOL_SIZE);
            
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize connection pool: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database connection pool initialization failed", e);
        }
    }

    /**
     * Get a database connection from the pool
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initializePool();
        }
        
        try {
            Connection connection = dataSource.getConnection();
            if (connection == null || connection.isClosed()) {
                throw new SQLException("Failed to get connection from pool");
            }
            return connection;
        } catch (SQLException e) {
            System.err.println("✗ Failed to get connection from pool: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Close the connection pool (call this on application shutdown)
     */
    public static void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
            System.out.println("✓ Connection pool closed");
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
                conn.close(); // Return connection to pool
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
     * Get pool statistics (useful for monitoring)
     */
    public static String getPoolStats() {
        if (dataSource == null || dataSource.isClosed()) {
            return "Connection pool not initialized";
        }
        
        return String.format(
            "Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    /**
     * Get the connection URL (for debugging)
     */
    public static String getConnectionUrl() {
        if (dataSource == null) {
            return "Connection pool not initialized";
        }
        return dataSource.getJdbcUrl();
    }

    /**
     * Shutdown hook to properly close the connection pool on application exit
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down connection pool...");
            closeConnection();
        }));
    }
}
