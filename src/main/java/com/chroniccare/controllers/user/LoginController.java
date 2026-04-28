package com.chroniccare.controllers.user;

import com.chroniccare.entities.User;
import com.chroniccare.services.CartService;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    private static final String STATIC_ADMIN_EMAIL = "admin@chronic.com";
    private static final String STATIC_ADMIN_PASSWORD = "admin123";

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserService userService = new UserService();

    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim().toLowerCase();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            errorLabel.setText("Email invalide.");
            return;
        }

        try {
            // Login statique admin (bypass DB)
            if (STATIC_ADMIN_EMAIL.equalsIgnoreCase(email)
                    && STATIC_ADMIN_PASSWORD.equals(password)) {
                User admin = new User();
                admin.setNom("Admin");
                admin.setPrenom("Super");
                admin.setEmail(STATIC_ADMIN_EMAIL);
                admin.setRoles("[\"ROLE_ADMIN\"]");
                admin.setActive(true);
                admin.setId(1); // ID arbitraire pour admin statique

                SessionManager.getInstance().setCurrentUser(admin);
                // Charger le panier persistant
                CartService.getInstance().chargerPanierUtilisateur(admin.getId());
                
                Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/home.fxml"));
                emailField.getScene().setRoot(root);
                return;
            }

            User user = userService.checkLogin(email, password);

            if (user == null) {
                errorLabel.setText("Email ou mot de passe incorrect.");
                return;
            }

            if (!user.isActive()) {
                errorLabel.setText("Ce compte est désactivé.");
                return;
            }

            SessionManager.getInstance().setCurrentUser(user);
            // Charger le panier persistant de l'utilisateur
            CartService.getInstance().chargerPanierUtilisateur(user.getId());

            // Tous les utilisateurs (admin inclus) passent par la page d'accueil commune
            String fxmlPath = "/com/chroniccare/home.fxml";

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            emailField.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur lors de la connexion.");
        }
    }

    @FXML
    public void goToRegister() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/chroniccare/register.fxml"));
            emailField.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur de navigation.");
        }
    }
}