package com.chroniccare.controllers.user;

import com.chroniccare.entities.User;
import com.chroniccare.services.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<String> genreCombo;
    @FXML private ComboBox<String> rolesCombo;
    @FXML private Label errorLabel;

    private UserService userService = new UserService();

    @FXML
    public void initialize() {
        genreCombo.setItems(FXCollections.observableArrayList("Homme", "Femme"));
        rolesCombo.setItems(FXCollections.observableArrayList(
                "ROLE_PATIENT", "ROLE_COACH", "ROLE_NUTRITIONNISTE"
        ));
    }

    @FXML
    public void handleRegister() {
        // Validation
        StringBuilder errors = new StringBuilder();

        if (nomField.getText().trim().isEmpty())
            errors.append("• Nom obligatoire\n");

        if (prenomField.getText().trim().isEmpty())
            errors.append("• Prénom obligatoire\n");

        if (!emailField.getText().matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$"))
            errors.append("• Email invalide\n");

        if (passwordField.getText().length() < 8)
            errors.append("• Mot de passe : minimum 8 caractères\n");

        if (!telephoneField.getText().matches("\\d{8}"))
            errors.append("• Téléphone : exactement 8 chiffres\n");

        if (genreCombo.getValue() == null)
            errors.append("• Genre obligatoire\n");

        if (rolesCombo.getValue() == null)
            errors.append("• Rôle obligatoire\n");

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreurs de saisie");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(errors.toString());
            alert.show();
            return;
        }

        // Création de l'utilisateur
        try {
            User u = new User(
                    nomField.getText().trim(),
                    prenomField.getText().trim(),
                    emailField.getText().trim().toLowerCase(),
                    passwordField.getText(),
                    "[\"" + rolesCombo.getValue() + "\"]",
                    telephoneField.getText().trim(),
                    genreCombo.getValue()
            );

            userService.insert(u);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succès");
            success.setContentText("Compte créé avec succès !");
            success.show();

            // Retour au login
            goToLogin();

        } catch (Exception e) {
            errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/chroniccare/login.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) {
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }
}