package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.GoogleOAuthService;
import com.chroniccare.services.SecurityService;
import com.chroniccare.services.UserService;
import com.chroniccare.services.PatientMedicalRecordService;
import com.chroniccare.utils.PasswordUtils;
import com.chroniccare.utils.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserService userService = new UserService();
    private final SecurityService securityService = new SecurityService();
    private final PatientMedicalRecordService patientMedicalRecordService = new PatientMedicalRecordService();

    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            securityService.logInvalidEmailFormat(email);
            errorLabel.setText("Email invalide.");
            return;
        }

        try {
            User existingUser = userService.findByEmail(email);

            if (existingUser != null && securityService.isAccountLocked(existingUser)) {
                securityService.logBlockedLogin(existingUser, email);
                errorLabel.setText(securityService.getRemainingLockMessage(existingUser));
                return;
            }

            User user = userService.checkLogin(email, password);

            if (user == null) {
                securityService.handleFailedLogin(existingUser, email);
                errorLabel.setText("Email ou mot de passe incorrect.");
                return;
            }

            if (!user.isActive()) {
                errorLabel.setText("Ce compte est désactivé.");
                return;
            }

            securityService.handleSuccessfulLogin(user);

            User refreshedUser = userService.findByEmail(email);
            SessionManager.getInstance().setCurrentUser(refreshedUser != null ? refreshedUser : user);

            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/home.fxml"));
            emailField.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur lors de la connexion.");
        }
    }

    @FXML
    public void goToRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/register.fxml"));
            emailField.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur de navigation.");
        }
    }

    @FXML
    public void goToForgotPassword() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/forgot-password.fxml"));
            emailField.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur de navigation.");
        }
    }

    @FXML
    public void handleGoogleLogin() {
        errorLabel.setText("Continuer avec Google...");

        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                GoogleOAuthService googleOAuthService = new GoogleOAuthService();

                if (!googleOAuthService.isConfigured()) {
                    String baseDir = System.getProperty("user.dir");
                    String missing = googleOAuthService.getMissingConfigKeys();
                    String suffix = (missing == null || missing.isBlank()) ? "" : (" Manquant : " + missing + ".");
                    throw new IllegalStateException(
                            "Google non configuré." + suffix + " Ajoutez GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET (env) ou créez `google-oauth.properties` dans : " + baseDir
                    );
                }

                GoogleOAuthService.GoogleUserInfo googleUser = googleOAuthService.authenticateInteractive();
                if (googleUser == null || googleUser.getEmail() == null || googleUser.getEmail().isBlank()) {
                    throw new IllegalStateException("Impossible de récupérer l'email Google.");
                }

                String email = googleUser.getEmail().trim();

                User existingUser = userService.findByEmail(email);
                if (existingUser != null) {
                    if (securityService.isAccountLocked(existingUser)) {
                        throw new IllegalStateException(securityService.getRemainingLockMessage(existingUser));
                    }

                    if (!existingUser.isActive()) {
                        throw new IllegalStateException("Ce compte est désactivé.");
                    }

                    try {
                        securityService.handleSuccessfulLogin(existingUser);
                    } catch (Exception ignored) {
                    }

                    User refreshed = userService.findByEmail(email);
                    return refreshed != null ? refreshed : existingUser;
                }

                String selectedRole = promptRoleFromUser();
                if (selectedRole == null || selectedRole.isBlank()) {
                    throw new IllegalStateException("Connexion annulée.");
                }

                String medicalCondition = null;
                if ("ROLE_PATIENT".equals(selectedRole)) {
                    medicalCondition = promptMedicalConditionFromUser();
                    if (medicalCondition == null || medicalCondition.isBlank()) {
                        throw new IllegalStateException("Connexion annulée.");
                    }
                }

                String genre = promptGenreFromUser();
                if (genre == null || genre.isBlank()) {
                    throw new IllegalStateException("Connexion annulée.");
                }

                String telephone = promptTelephoneFromUser();
                if (telephone == null || telephone.isBlank()) {
                    throw new IllegalStateException("Connexion annulée.");
                }

                String firstName = firstNonBlank(googleUser.getGivenName(), extractFirstName(googleUser.getName()), "Utilisateur");
                String lastName = firstNonBlank(googleUser.getFamilyName(), extractLastName(googleUser.getName()), "");

                User newUser = new User();
                newUser.setPrenom(firstName);
                newUser.setNom(lastName);
                newUser.setEmail(email);
                newUser.setPassword(PasswordUtils.hash(generateRandomPassword()));
                newUser.setRoles("[\"" + selectedRole + "\"]");
                newUser.setTelephone(telephone);
                newUser.setGenre(genre);
                newUser.setApprovalStatus("pending");
                newUser.setMedicalCondition(medicalCondition);
                newUser.setActive(true);

                userService.insert(newUser);

                User saved = userService.findByEmail(email);
                if (saved != null && saved.getRoles() != null && saved.getRoles().contains("ROLE_PATIENT")) {
                    try {
                        patientMedicalRecordService.ensureRecordExists(saved.getId(), medicalCondition);
                    } catch (Exception ignored) {
                    }
                }

                return saved != null ? saved : newUser;
            }
        };

        task.setOnSucceeded(evt -> {
            try {
                User user = task.getValue();
                if (user == null) {
                    errorLabel.setText("Erreur lors de la connexion Google.");
                    return;
                }

                SessionManager.getInstance().setCurrentUser(user);

                Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/home.fxml"));
                emailField.getScene().setRoot(root);
            } catch (Exception e) {
                e.printStackTrace();
                errorLabel.setText("Erreur lors de la connexion Google.");
            }
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erreur lors de la connexion Google.";
            errorLabel.setText(msg);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static String generateRandomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return "";
    }

    private static String extractFirstName(String fullName) {
        if (fullName == null) return null;
        String s = fullName.trim().replaceAll("\\s+", " ");
        if (s.isBlank()) return null;
        int idx = s.indexOf(' ');
        return idx > 0 ? s.substring(0, idx) : s;
    }

    private static String extractLastName(String fullName) {
        if (fullName == null) return null;
        String s = fullName.trim().replaceAll("\\s+", " ");
        if (s.isBlank()) return null;
        int idx = s.lastIndexOf(' ');
        return idx > 0 ? s.substring(idx + 1) : "";
    }

    private static String roleLabelFromRoles(String roles) {
        if (roles == null) return "Utilisateur";
        if (roles.contains("ROLE_ADMIN")) return "Administrateur";
        if (roles.contains("ROLE_NUTRITIONNISTE")) return "Nutritionniste";
        if (roles.contains("ROLE_COACH")) return "Coach";
        if (roles.contains("ROLE_PATIENT")) return "Patient";
        return "Utilisateur";
    }

    private static String promptRoleFromUser() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Continuer avec Google");
            alert.setHeaderText("Choisissez votre rôle");
            alert.setContentText("Ce rôle sera associé à votre compte ChronicCare.");

            ButtonType patientBtn = new ButtonType("Patient");
            ButtonType coachBtn = new ButtonType("Coach");
            ButtonType nutriBtn = new ButtonType("Nutritionniste");
            ButtonType cancelBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(patientBtn, coachBtn, nutriBtn, cancelBtn);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty()) {
                future.complete(null);
                return;
            }

            ButtonType chosen = result.get();
            if (chosen.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                future.complete(null);
                return;
            }

            if (chosen == patientBtn) future.complete("ROLE_PATIENT");
            else if (chosen == coachBtn) future.complete("ROLE_COACH");
            else if (chosen == nutriBtn) future.complete("ROLE_NUTRITIONNISTE");
            else future.complete(null);
        });

        return future.get(300, TimeUnit.SECONDS);
    }

    private static String promptMedicalConditionFromUser() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            while (true) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Profil patient");
                dialog.setHeaderText("Maladie principale");
                dialog.setContentText("Ex: diabète, hypertension, asthme...");

                Optional<String> result = dialog.showAndWait();
                if (result.isEmpty()) {
                    future.complete(null);
                    return;
                }

                String value = result.get() == null ? "" : result.get().trim();
                if (!value.isBlank()) {
                    future.complete(value);
                    return;
                }

                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Erreur");
                error.setHeaderText(null);
                error.setContentText("La maladie principale est obligatoire pour un patient.");
                error.showAndWait();
            }
        });

        return future.get(300, TimeUnit.SECONDS);
    }

    private static String promptGenreFromUser() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Profil");
            alert.setHeaderText("Genre");
            alert.setContentText("Choisissez votre genre.");

            ButtonType hommeBtn = new ButtonType("Homme");
            ButtonType femmeBtn = new ButtonType("Femme");
            ButtonType cancelBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(hommeBtn, femmeBtn, cancelBtn);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty()) {
                future.complete(null);
                return;
            }

            ButtonType chosen = result.get();
            if (chosen.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                future.complete(null);
                return;
            }

            if (chosen == hommeBtn) future.complete("Homme");
            else if (chosen == femmeBtn) future.complete("Femme");
            else future.complete(null);
        });

        return future.get(300, TimeUnit.SECONDS);
    }

    private static String promptTelephoneFromUser() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            while (true) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Profil");
                dialog.setHeaderText("Téléphone");
                dialog.setContentText("Numéro (8 chiffres) :");

                Optional<String> result = dialog.showAndWait();
                if (result.isEmpty()) {
                    future.complete(null);
                    return;
                }

                String value = result.get() == null ? "" : result.get().trim();
                if (value.matches("\\d{8}")) {
                    future.complete(value);
                    return;
                }

                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Erreur");
                error.setHeaderText(null);
                error.setContentText("Téléphone invalide. Il faut exactement 8 chiffres.");
                error.showAndWait();
            }
        });

        return future.get(300, TimeUnit.SECONDS);
    }
}
