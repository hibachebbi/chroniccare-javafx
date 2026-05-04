package com.chroniccare.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class GeminiConfig {
    private static final String CONFIG_PATH = "config/gemini.properties";

    private GeminiConfig() {
    }

    public static String getApiKey() {
        String environmentKey = System.getenv("GEMINI_API_KEY");
        if (isPresent(environmentKey)) {
            return environmentKey.trim();
        }

        String systemPropertyKey = System.getProperty("chroniccare.gemini.apiKey");
        if (isPresent(systemPropertyKey)) {
            return systemPropertyKey.trim();
        }

        Properties properties = loadLocalProperties();
        String fileKey = properties.getProperty("gemini.apiKey");
        return isPresent(fileKey) ? fileKey.trim() : null;
    }

    public static String getModel() {
        String systemPropertyModel = System.getProperty("chroniccare.gemini.model");
        if (isPresent(systemPropertyModel)) {
            return systemPropertyModel.trim();
        }

        Properties properties = loadLocalProperties();
        String fileModel = properties.getProperty("gemini.model");
        if (isPresent(fileModel)) {
            return fileModel.trim();
        }

        return "gemini-2.5-flash";
    }

    private static Properties loadLocalProperties() {
        Path path = Path.of(CONFIG_PATH);
        Properties properties = new Properties();
        if (!Files.exists(path)) {
            return properties;
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        } catch (IOException ignored) {
            return new Properties();
        }
        return properties;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
