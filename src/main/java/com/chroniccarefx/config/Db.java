package com.chroniccarefx.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    private static final String URL = System.getenv().getOrDefault(
            "CC_DB_URL",
            "jdbc:mysql://localhost:3306/chroniccare?useSSL=false&serverTimezone=UTC"
    );
    private static final String USER = System.getenv().getOrDefault("CC_DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("CC_DB_PASSWORD", "");

    private Db() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
