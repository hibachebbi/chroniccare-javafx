package com.chroniccare.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private static MyDatabase instance;
    private Connection connection;

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/chroniccare",
                    "root",
                    ""
            );
            System.out.println("Connexion réussie !");
        } catch (SQLException e) {
            System.err.println("Erreur connexion : " + e.getMessage());
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}