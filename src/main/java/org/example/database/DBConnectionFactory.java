package org.example.database;

import java.sql.*;

public class DBConnectionFactory {
    public static Connection createConnection(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}