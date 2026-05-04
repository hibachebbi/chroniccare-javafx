package com.chroniccare.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

        Path baseDir = Paths.get(System.getProperty("user.dir"));
        Path smtp2Path = baseDir.resolve("smtp2.properties");
        if (Files.exists(smtp2Path)) {
            return smtp2Path;
        }

        return baseDir.resolve("smtp.properties");
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

    /* ----- Inscription événement + QR (chroniccare-javafx, GMAIL_EMAIL / GMAIL_PASSWORD ou .env) ----- */

    private static final String QR_SMTP_HOST = "smtp.gmail.com";
    private static final String QR_SMTP_PORT = "587";
    private static final Properties QR_DOT_ENV = loadQrDotEnv();
    private static final String QR_FROM_EMAIL = resolveQrConfig("GMAIL_EMAIL");
    private static final String QR_FROM_PASSWORD = resolveQrConfig("GMAIL_PASSWORD");

    public static void sendRegistrationEmailWithQRCode(
            String recipientEmail,
            String recipientName,
            String eventTitle,
            String eventDate,
            String eventLocation,
            String qrCodePath) throws Exception {

        if (QR_FROM_EMAIL == null || QR_FROM_EMAIL.isBlank()
                || QR_FROM_PASSWORD == null || QR_FROM_PASSWORD.isBlank()) {
            throw new IllegalStateException(
                    "Variables GMAIL_EMAIL/GMAIL_PASSWORD introuvables. "
                            + "Définis-les dans l'environnement ou dans un fichier .env à la racine du projet."
            );
        }

        Session session = Session.getInstance(buildQrSmtpProps(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(QR_FROM_EMAIL, QR_FROM_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(QR_FROM_EMAIL, "ChronicCare"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("Confirmation d'inscription - " + eventTitle);

        MimeMultipart multipart = new MimeMultipart();

        MimeBodyPart textPart = new MimeBodyPart();
        String htmlContent = buildQrRegistrationEmailHtml(recipientName, eventTitle, eventDate, eventLocation);
        textPart.setContent(htmlContent, "text/html; charset=utf-8");
        multipart.addBodyPart(textPart);

        if (qrCodePath != null && !qrCodePath.isBlank()) {
            File qrFile = new File(qrCodePath);
            if (qrFile.exists()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(qrFile);
                attachmentPart.setHeader("Content-ID", "<qrcode>");
                multipart.addBodyPart(attachmentPart);
            }
        }

        message.setContent(multipart);
        Transport.send(message);
    }

    private static Properties buildQrSmtpProps() {
        Properties props = new Properties();
        props.put("mail.smtp.host", QR_SMTP_HOST);
        props.put("mail.smtp.port", QR_SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        return props;
    }

    private static String buildQrRegistrationEmailHtml(String name, String eventTitle, String eventDate, String eventLocation) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
                        .container { max-width: 600px; margin: 20px auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        h1 { color: #2c3e50; margin-bottom: 20px; }
                        .event-info { background: #ecf0f1; padding: 15px; border-radius: 5px; margin: 20px 0; }
                        .event-info p { margin: 8px 0; }
                        .qr-section { text-align: center; margin: 30px 0; }
                        .qr-section img { max-width: 250px; border: 2px solid #3498db; padding: 10px; }
                        .footer { color: #7f8c8d; font-size: 12px; margin-top: 30px; text-align: center; border-top: 1px solid #ecf0f1; padding-top: 15px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Inscription confirmée ✓</h1>
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Votre inscription à l'événement a été confirmée. Retrouvez tous les détails ci-dessous :</p>
                        <div class="event-info">
                            <p><strong>Événement :</strong> %s</p>
                            <p><strong>Date :</strong> %s</p>
                            <p><strong>Lieu :</strong> %s</p>
                        </div>
                        <p>Voici votre code QR d'accès à l'événement :</p>
                        <div class="qr-section">
                            <img src="cid:qrcode" alt="QR Code" />
                        </div>
                        <p>Présentez ce code QR le jour de l'événement pour valider votre présence.</p>
                        <div class="footer">
                            <p>ChronicCare - Gestion des événements de santé</p>
                            <p>Ne répondez pas à ce mail automatique</p>
                        </div>
                    </div>
                </body>
                </html>
                """, name, eventTitle, eventDate, eventLocation);
    }

    private static String resolveQrConfig(String key) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String dotEnvValue = QR_DOT_ENV.getProperty(key);
        if (dotEnvValue != null && !dotEnvValue.isBlank()) {
            return dotEnvValue.trim();
        }
        return null;
    }

    private static Properties loadQrDotEnv() {
        Properties properties = new Properties();
        Path dotEnvPath = findQrDotEnvPath();
        if (dotEnvPath == null) {
            return properties;
        }
        try {
            List<String> lines = Files.readAllLines(dotEnvPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    continue;
                }
                int separatorIndex = trimmedLine.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }
                String k = trimmedLine.substring(0, separatorIndex).trim();
                String v = trimmedLine.substring(separatorIndex + 1).trim();
                if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length() - 1);
                }
                properties.setProperty(k, v);
            }
        } catch (IOException ignored) {
        }
        return properties;
    }

    private static Path findQrDotEnvPath() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(".env");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }
}
