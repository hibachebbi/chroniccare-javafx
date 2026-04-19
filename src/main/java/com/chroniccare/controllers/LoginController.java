package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.SecurityService;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserService userService = new UserService();
    private final SecurityService securityService = new SecurityService();

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
}