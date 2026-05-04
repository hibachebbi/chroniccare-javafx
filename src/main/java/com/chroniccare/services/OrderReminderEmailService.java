package com.chroniccare.services;

import com.chroniccare.models.Commande;
import com.chroniccare.models.User;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Objects;
import java.util.Properties;

public class OrderReminderEmailService {

    public static class ReminderResult {
        private final boolean sent;
        private final String message;

        public ReminderResult(boolean sent, String message) {
            this.sent = sent;
            this.message = message;
        }

        public boolean isSent() {
            return sent;
        }

        public String getMessage() {
            return message;
        }
    }

    public ReminderResult sendOrderReminder(User user, Commande commande) {
        Objects.requireNonNull(user, "user obligatoire");
        Objects.requireNonNull(commande, "commande obligatoire");

        String recipientEmail = user.getEmail() == null ? null : user.getEmail().trim().toLowerCase();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return new ReminderResult(false, "Email utilisateur introuvable");
        }

        SmtpConfig config = SmtpConfig.fromEnvironment();
        if (!config.isValid()) {
            return new ReminderResult(false,
                    "SMTP non configure. Definis SMTP_HOST, SMTP_PORT, SMTP_USER et SMTP_PASS. SMTP_FROM est optionnel.");
        }

        try {
            Session session = Session.getInstance(config.toProperties(), new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.user, config.password);
                }
            });

            Message message = new MimeMessage(session);
            // Sender is fixed by SMTP account/config. Recipient changes per logged-in user.
            message.setFrom(new InternetAddress(config.from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Rappel commande " + safe(commande.getNumeroCommande()));
            message.setText(buildBody(user, commande));

            Transport.send(message);
            return new ReminderResult(true, "Rappel envoye a " + recipientEmail);
        } catch (Exception e) {
            String raw = e.getMessage() == null ? "Erreur SMTP inconnue" : e.getMessage();
            String normalized = raw.toLowerCase();

            if (normalized.contains("application-specific password required") || normalized.contains("534-5.7.9")) {
                return new ReminderResult(false,
                        "Gmail refuse l'authentification: il faut un mot de passe d'application (16 caracteres), pas le mot de passe normal. "
                                + "Active la verification en 2 etapes sur le compte emetteur puis genere un App Password Google.");
            }

            if (normalized.contains("authentication failed") || normalized.contains("535")) {
                return new ReminderResult(false,
                        "Authentification SMTP invalide. Verifie SMTP_USER et SMTP_PASS (App Password si Gmail).");
            }

            return new ReminderResult(false, "Envoi email echoue: " + raw);
        }
    }

    private String buildBody(User user, Commande commande) {
        String prenom = safe(user.getPrenom());
        String numero = safe(commande.getNumeroCommande());
        String statut = safe(commande.getStatut());

        return "Bonjour " + prenom + ",\n\n"
                + "Ceci est un rappel concernant votre commande " + numero + ".\n"
                + "Statut actuel: " + statut + "\n"
                + "Montant: " + String.format("%.2f", commande.getTotal()) + " TND\n\n"
                + "Merci de votre confiance.\n"
                + "Equipe ChronicCare";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static class SmtpConfig {
        private final String host;
        private final String port;
        private final String user;
        private final String password;
        private final String from;

        private SmtpConfig(String host, String port, String user, String password, String from) {
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
            this.from = from;
        }

        static SmtpConfig fromEnvironment() {
            String smtpUser = env("SMTP_USER");
            String smtpFrom = env("SMTP_FROM", smtpUser);
            return new SmtpConfig(
                    env("SMTP_HOST"),
                    env("SMTP_PORT", "587"),
                    smtpUser,
                    env("SMTP_PASS"),
                    smtpFrom);
        }

        boolean isValid() {
            return notBlank(host) && notBlank(port) && notBlank(user) && notBlank(password) && notBlank(from);
        }

        Properties toProperties() {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.ssl.trust", host);
            return props;
        }

        private static String env(String key) {
            return System.getenv(key);
        }

        private static String env(String key, String defaultValue) {
            String value = env(key);
            return value == null || value.isBlank() ? defaultValue : value;
        }

        private static boolean notBlank(String value) {
            return value != null && !value.isBlank();
        }
    }
}
