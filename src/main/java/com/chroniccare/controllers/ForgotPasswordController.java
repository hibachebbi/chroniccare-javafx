package com.chroniccare.controllers;

import com.chroniccare.services.EmailService;
import com.chroniccare.services.PasswordResetService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private TextField tokenField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label infoLabel;
    @FXML private Label errorLabel;

    private final PasswordResetService passwordResetService = new PasswordResetService();
    private final EmailService emailService = new EmailService();

    @FXML
    public void handleSendResetLink() {
        clearMessages();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isBlank()) {
            errorLabel.setText("Veuillez saisir votre email.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            errorLabel.setText("Email invalide.");
            return;
        }

        if (!emailService.isConfigured()) {
            String missing = emailService.getMissingConfigKeys();
            String suffix = (missing == null || missing.isBlank()) ? "" : (" Manquant : " + missing + ".");
            String baseDir = System.getProperty("user.dir");
            errorLabel.setText("Email non configuré." + suffix + " Configurez les variables d'environnement SMTP_* ou créez `smtp2.properties` (ou `smtp.properties`) dans : " + baseDir);
            return;
        }

        try {
            PasswordResetService.ResetToken resetToken = passwordResetService.createResetToken(email);

            if (resetToken != null) {
                String resetUrl = buildResetUrl(resetToken.getToken());
                emailService.sendPasswordResetEmail(
                        resetToken.getEmail(),
                        resetUrl,
                        resetToken.getToken(),
                        resetToken.getExpiresAt()
                );
            }

            infoLabel.setText("Si un compte existe, un email de récupération a été envoyé.");
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur lors de l'envoi de l'email.");
        }
    }

    @FXML
    public void handleResetPassword() {
        clearMessages();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String token = normalizeResetToken(tokenField.getText());
        String newPassword = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (email.isBlank()) {
            errorLabel.setText("Veuillez saisir votre email.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            errorLabel.setText("Email invalide.");
            return;
        }

        if (token.isBlank()) {
            errorLabel.setText("Veuillez coller le code reçu par email.");
            return;
        }

        if (!token.matches("\\d{6}")) {
            errorLabel.setText("Le code doit contenir exactement 6 chiffres.");
            return;
        }

        if (newPassword == null || newPassword.length() < 8) {
            errorLabel.setText("Mot de passe : minimum 8 caractères.");
            return;
        }

        if (!newPassword.equals(confirm)) {
            errorLabel.setText("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            boolean ok = passwordResetService.resetPassword(email, token, newPassword);
            if (!ok) {
                errorLabel.setText("Code invalide, expiré, ou non associé à cet email.");
                return;
            }

            infoLabel.setText("Mot de passe modifié. Vous pouvez vous connecter.");
            newPasswordField.clear();
            confirmPasswordField.clear();
            tokenField.clear();
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur lors de la réinitialisation.");
        }
    }

    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            emailField.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur de navigation.");
        }
    }

    private String buildResetUrl(String token) {
        String base = System.getenv().getOrDefault("CHRONICCARE_RESET_URL_BASE", "chroniccare://reset-password");
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);

        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "token=" + encodedToken;
    }

    private void clearMessages() {
        if (infoLabel != null) infoLabel.setText("");
        if (errorLabel != null) errorLabel.setText("");
    }

    private String normalizeResetToken(String rawValue) {
        if (rawValue == null) {
            return "";
        }

        String trimmed = rawValue.trim();
        if (trimmed.isBlank()) {
            return "";
        }

        String tokenFromUrl = extractTokenQueryParam(trimmed);
        if (!tokenFromUrl.isBlank()) {
            trimmed = tokenFromUrl;
        }

        return trimmed.replaceAll("\\s+", "");
    }

    private String extractTokenQueryParam(String value) {
        int tokenIndex = value.indexOf("token=");
        if (tokenIndex < 0) {
            return "";
        }

        String tokenPart = value.substring(tokenIndex + "token=".length());
        int ampIndex = tokenPart.indexOf('&');
        if (ampIndex >= 0) {
            tokenPart = tokenPart.substring(0, ampIndex);
        }

        return tokenPart.trim();
    }
}
