package com.chroniccare.modules.suivi.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public class SmtpMailService {
    private static final Path SMTP_PROPERTIES_PATH = Path.of("smtp.properties");
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public void sendMail(String to, String subject, String body) {
        sendMail(to, subject, body, null, null);
    }

    public void sendMail(String to, String subject, String body, String senderEmail, String senderName) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Destinataire email obligatoire.");
        }

        BrevoConfig config = BrevoConfig.fromEnvironment();
        String effectiveSenderEmail = isBlank(senderEmail) ? config.fromAddress() : senderEmail.trim();
        String effectiveSenderName = isBlank(senderName) ? config.fromName() : senderName.trim();
        HttpRequest request = HttpRequest.newBuilder(config.apiUrl())
                .timeout(Duration.ofSeconds(25))
                .header("accept", "application/json")
                .header("api-key", config.apiKey())
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        buildPayload(effectiveSenderEmail, effectiveSenderName, to, subject, body),
                        StandardCharsets.UTF_8
                ))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(buildFriendlyErrorMessage(response.statusCode(), response.body()));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Envoi email impossible: connexion a l'API Brevo echouee.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envoi email interrompu.", ex);
        }
    }

    private String buildPayload(String senderEmail, String senderName, String to, String subject, String body) {
        return "{"
                + "\"sender\":{"
                + "\"name\":\"" + escapeJson(senderName) + "\","
                + "\"email\":\"" + escapeJson(senderEmail) + "\""
                + "},"
                + "\"to\":[{\"email\":\"" + escapeJson(to.trim()) + "\"}],"
                + "\"replyTo\":{\"email\":\"" + escapeJson(senderEmail) + "\",\"name\":\"" + escapeJson(senderName) + "\"},"
                + "\"subject\":\"" + escapeJson(subject == null ? "" : subject) + "\","
                + "\"textContent\":\"" + escapeJson(body == null ? "" : body) + "\""
                + "}";
    }

    private String buildFriendlyErrorMessage(int statusCode, String responseBody) {
        String normalized = responseBody == null ? "" : responseBody.toLowerCase();
        if (statusCode == 401 || statusCode == 403) {
            return "Cle API Brevo invalide ou refusee. Verifiez CC_MAIL_API_KEY dans smtp.properties.";
        }
        if (statusCode == 400 && normalized.contains("sender")) {
            return "Expediteur refuse par Brevo. Validez l'adresse email du coach/nutritionniste dans Brevo, ou utilisez une adresse deja validee.";
        }
        if (statusCode == 400 && normalized.contains("recipient")) {
            return "Email patient invalide.";
        }
        return "Envoi email impossible via Brevo (code " + statusCode + ").";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record BrevoConfig(
            URI apiUrl,
            String apiKey,
            String fromAddress,
            String fromName
    ) {
        private static BrevoConfig fromEnvironment() {
            Properties fileProperties = loadPropertiesFile();

            String apiUrl = value("CC_MAIL_API_URL", fileProperties, "https://api.brevo.com/v3/smtp/email");
            String apiKey = required("CC_MAIL_API_KEY", fileProperties);
            String fromAddress = value("CC_MAIL_FROM", fileProperties, "revtechtuning.contact@gmail.com");
            String fromName = value("CC_MAIL_FROM_NAME", fileProperties, "Chronicare");
            return new BrevoConfig(URI.create(apiUrl), apiKey, fromAddress, fromName);
        }

        private static String required(String name, Properties fileProperties) {
            String value = value(name, fileProperties, "");
            if (value == null || value.isBlank() || value.contains("YOUR_API")) {
                throw new IllegalStateException("Configuration email manquante: ajoutez votre cle API Brevo dans CC_MAIL_API_KEY.");
            }
            return value.trim();
        }

        private static String value(String name, Properties fileProperties, String defaultValue) {
            String envValue = System.getenv(name);
            if (envValue != null && !envValue.isBlank()) {
                return envValue.trim();
            }
            String fileValue = fileProperties.getProperty(name);
            if (fileValue != null && !fileValue.isBlank()) {
                return fileValue.trim();
            }
            return defaultValue;
        }

        private static Properties loadPropertiesFile() {
            Properties properties = new Properties();
            if (!Files.exists(SMTP_PROPERTIES_PATH)) {
                return properties;
            }

            try (InputStream inputStream = Files.newInputStream(SMTP_PROPERTIES_PATH)) {
                properties.load(inputStream);
                return properties;
            } catch (IOException ex) {
                throw new IllegalStateException("Impossible de lire smtp.properties: " + ex.getMessage(), ex);
            }
        }
    }
}
