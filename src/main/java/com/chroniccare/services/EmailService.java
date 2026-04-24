package com.chroniccare.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.util.List;
import java.util.Properties;

public class EmailService {
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final Properties DOT_ENV = loadDotEnv();
    private static final String FROM_EMAIL = resolveConfig("GMAIL_EMAIL");
    private static final String FROM_PASSWORD = resolveConfig("GMAIL_PASSWORD");

    private static Session getMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });
    }

    public static void sendRegistrationEmailWithQRCode(
            String recipientEmail,
            String recipientName,
            String eventTitle,
            String eventDate,
            String eventLocation,
            String qrCodePath) throws Exception {

        if (FROM_EMAIL == null || FROM_EMAIL.isBlank() || FROM_PASSWORD == null || FROM_PASSWORD.isBlank()) {
            throw new IllegalStateException(
                    "Variables GMAIL_EMAIL/GMAIL_PASSWORD introuvables. "
                            + "Definis-les dans l'environnement systeme ou dans un fichier .env a la racine du projet."
            );
        }

        Session session = getMailSession();
        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(FROM_EMAIL, "ChronicCare"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("Confirmation d'inscription - " + eventTitle);

        MimeMultipart multipart = new MimeMultipart();

        // Corps du mail
        MimeBodyPart textPart = new MimeBodyPart();
        String htmlContent = buildEmailHTML(recipientName, eventTitle, eventDate, eventLocation);
        textPart.setContent(htmlContent, "text/html; charset=utf-8");
        multipart.addBodyPart(textPart);

        // QR Code en pièce jointe
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

    private static String buildEmailHTML(String name, String eventTitle, String eventDate, String eventLocation) {
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

    private static String resolveConfig(String key) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String dotEnvValue = DOT_ENV.getProperty(key);
        if (dotEnvValue != null && !dotEnvValue.isBlank()) {
            return dotEnvValue.trim();
        }

        return null;
    }

    private static Properties loadDotEnv() {
        Properties properties = new Properties();
        Path dotEnvPath = findDotEnvPath();

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

                String key = trimmedLine.substring(0, separatorIndex).trim();
                String value = trimmedLine.substring(separatorIndex + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                properties.setProperty(key, value);
            }
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Lecture du fichier .env impossible: " + e.getMessage(), e);
        }
    }

    private static Path findDotEnvPath() {
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
