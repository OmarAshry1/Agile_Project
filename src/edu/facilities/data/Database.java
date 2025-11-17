package edu.facilities.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    // 🔧 TODO: change USER and PASSWORD to match your SQL Server login
    // DB name is "agile" from your script: USE agile;
    private static final String URL =
            "jdbc:sqlserver://localhost:1433;databaseName=agile;encrypt=false";
    private static final String USER = "sa";          // or your own SQL Server user
    private static final String PASSWORD = "password"; // change this

    static {
        try {
            // SQL Server JDBC driver
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQL Server JDBC driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}