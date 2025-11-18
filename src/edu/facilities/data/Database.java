package edu.facilities.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    // de data el database 3ndy el y5o4 y8yrha
    // DB name is "agile" from your script: USE agile;
    // DB name is "agile"
    private static final String URL =
            "jdbc:sqlserver://DESKTOP-L634FC4;"
                    + "instanceName=ahmed;"
                    + "databaseName=agile;"
                    + "encrypt=false;"
                    + "trustServerCertificate=true;";

    private static final String USER = "agile_user";
    private static final String PASSWORD = "agile123";

    static {
        try {
            // SQL Server JDBC driver
            Class.forName("jdbc:sqlserver://DESKTOP-L634FC4;"
                    + "instanceName=ahmed;"
                    + "databaseName=agile;"
                    + "encrypt=false;"
                    + "trustServerCertificate=true;");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQL Server JDBC driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}