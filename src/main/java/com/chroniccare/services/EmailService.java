package com.chroniccare.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailService {

    public boolean isConfigured() {
        String host = getSetting("SMTP_HOST");
        String username = getSetting("SMTP_USERNAME");
        String password = getSetting("SMTP_PASSWORD");
        String from = getSetting("SMTP_FROM");

        if (from == null || from.isBlank()) {
            from = username;
        }

        boolean hasHost = host != null && !host.isBlank();
        boolean hasFrom = from != null && !from.isBlank();
        boolean hasAuth = username != null && !username.isBlank()
                && password != null && !password.isBlank();

        return hasHost && hasFrom && hasAuth;
    }

    public String getMissingConfigKeys() {
        String host = getSetting("SMTP_HOST");
        String username = getSetting("SMTP_USERNAME");
        String password = getSetting("SMTP_PASSWORD");
        String from = getSetting("SMTP_FROM");

        StringBuilder missing = new StringBuilder();

        if (isBlank(host)) appendMissing(missing, "SMTP_HOST");
        if (isBlank(username)) appendMissing(missing, "SMTP_USERNAME");
        if (isBlank(password)) appendMissing(missing, "SMTP_PASSWORD");

        String effectiveFrom = from;
        if (isBlank(effectiveFrom)) {
            effectiveFrom = username;
        }
        if (isBlank(effectiveFrom)) {
            appendMissing(missing, "SMTP_FROM");
        }

        return missing.toString();
    }

    public void sendPasswordResetEmail(String toEmail, String resetUrl, String token, java.sql.Timestamp expiresAt)
            throws MessagingException {
        String subject = "ChronicCare - Réinitialisation du mot de passe";

        String expiresText = expiresAt == null
                ? "bientôt"
                : expiresAt.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        String body = """
                Bonjour,

                Vous avez demandé la réinitialisation de votre mot de passe ChronicCare.

                Lien de réinitialisation :
                %s

                Si le lien ne fonctionne pas, vous pouvez utiliser ce code dans l'application :
                %s

                Ce lien/code expire : %s.

                Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.

                -- ChronicCare
                """.formatted(resetUrl, token, expiresText);

        sendText(toEmail, subject, body);
    }

    public void sendText(String toEmail, String subject, String body) throws MessagingException {
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Email destinataire manquant.");
        }

        String host = getSetting("SMTP_HOST");
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("SMTP_HOST n'est pas configuré.");
        }

        String port = getSetting("SMTP_PORT");
        if (port == null || port.isBlank()) {
            port = "587";
        }

        String username = getSetting("SMTP_USERNAME");
        String password = getSetting("SMTP_PASSWORD");

        String from = getSetting("SMTP_FROM");
        if (from == null || from.isBlank()) {
            from = username == null ? "" : username.trim();
        }

        boolean startTls = Boolean.parseBoolean(getSettingOrDefault("SMTP_STARTTLS", "true"));
        boolean ssl = Boolean.parseBoolean(getSettingOrDefault("SMTP_SSL", "false"));

        if (from.isBlank()) {
            throw new IllegalStateException("SMTP_FROM (ou SMTP_USERNAME) n'est pas configuré.");
        }

        boolean authEnabled = username != null && !username.isBlank() && password != null && !password.isBlank();
        if (!authEnabled) {
            throw new IllegalStateException("SMTP_USERNAME/SMTP_PASSWORD ne sont pas configurés.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", Boolean.toString(authEnabled));
        props.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
        props.put("mail.smtp.ssl.enable", Boolean.toString(ssl));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Session session = Session.getInstance(props, authEnabled ? new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        } : null);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        message.setSubject(subject, "UTF-8");
        message.setText(body, "UTF-8");

        Transport.send(message);
    }

    private volatile boolean fileConfigLoaded = false;
    private volatile Properties fileConfig = null;

    private String getSettingOrDefault(String key, String defaultValue) {
        String value = getSetting(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private String getSetting(String key) {
        String envOrProp = readEnvOrProperty(key);
        if (envOrProp != null && !envOrProp.isBlank()) {
            return envOrProp.trim();
        }

        Properties props = getFileConfig();
        if (props == null) {
            return null;
        }

        String fileValue = props.getProperty(key);
        if (fileValue == null || fileValue.isBlank()) {
            return null;
        }

        return fileValue.trim();
    }

    private Properties getFileConfig() {
        if (fileConfigLoaded) return fileConfig;
        synchronized (this) {
            if (fileConfigLoaded) return fileConfig;
            fileConfigLoaded = true;

            Path path = resolveConfigPath();
            if (path == null || !Files.exists(path)) {
                fileConfig = null;
                return null;
            }

            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
                fileConfig = props;
                return props;
            } catch (IOException e) {
                fileConfig = null;
                return null;
            }
        }
    }

    private Path resolveConfigPath() {
        String explicit = readEnvOrProperty("SMTP_CONFIG_PATH");
        if (explicit != null && !explicit.isBlank()) {
            try {
                return Paths.get(explicit.trim());
            } catch (Exception ignored) {
                return null;
            }
        }

        return Paths.get(System.getProperty("user.dir"), "smtp.properties");
    }

    private static String readEnvOrProperty(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return System.getProperty(key);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void appendMissing(StringBuilder sb, String key) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(key);
    }
}
