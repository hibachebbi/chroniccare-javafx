package com.chroniccare.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class MyDatabase {
    private static final String DEFAULT_JDBC_URL =
            "jdbc:mysql://localhost:3306/chroniccare?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_JDBC_USER = "root";
    private static final String DEFAULT_JDBC_PASSWORD = "";
    private static final Path DB_PROPERTIES_PATH = Path.of("db.properties");

    private static MyDatabase instance;
    private Connection connection;
    private final String jdbcUrl;
    private final String jdbcUser;
    private final String jdbcPassword;

    private MyDatabase() {
        Properties properties = loadDatabaseProperties();
        jdbcUrl = readSetting(properties, "db.url", "DB_URL", DEFAULT_JDBC_URL);
        jdbcUser = readSetting(properties, "db.user", "DB_USER", DEFAULT_JDBC_USER);
        jdbcPassword = readSetting(properties, "db.password", "DB_PASSWORD", DEFAULT_JDBC_PASSWORD);
        openConnection();
    }

    public static synchronized MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                openConnection();
            }
        } catch (SQLException e) {
            openConnection();
        }
        return connection;
    }

    private synchronized void openConnection() {
        try {
            connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
            System.out.println("Connexion reussie !");
        } catch (SQLException e) {
            connection = null;
            System.err.println("Erreur connexion : " + e.getMessage());
            System.err.println("Base visee : " + jdbcUrl);
            System.err.println("Utilisateur : " + jdbcUser);
            System.err.println("Astuce : demarrez MySQL/WAMP et verifiez db.properties ou les variables DB_URL / DB_USER / DB_PASSWORD.");
        }
    }

    private Properties loadDatabaseProperties() {
        Properties properties = new Properties();
        if (!Files.exists(DB_PROPERTIES_PATH)) {
            return properties;
        }

        try (var reader = Files.newBufferedReader(DB_PROPERTIES_PATH)) {
            properties.load(reader);
        } catch (Exception e) {
            System.err.println("Impossible de lire db.properties : " + e.getMessage());
        }
        return properties;
    }

    private String readSetting(Properties properties, String key, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return defaultValue;
    }
}
