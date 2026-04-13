package com.chroniccare.controllers.user;

import com.chroniccare.entities.User;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class EditUserController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<String> genreCombo;
    @FXML private ComboBox<String> rolesCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField medicalField;
    @FXML private Label errorLabel;

    private final UserService userService = new UserService();
    private User currentUser;

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut modifier un utilisateur.");
            alert.showAndWait();
            return;
        }

        genreCombo.setItems(FXCollections.observableArrayList("Homme", "Femme"));
        rolesCombo.setItems(FXCollections.observableArrayList(
                "ROLE_PATIENT", "ROLE_COACH", "ROLE_NUTRITIONNISTE", "ROLE_ADMIN"
        ));
        statusCombo.setItems(FXCollections.observableArrayList(
                "approved", "pending", "rejected"
        ));
    }

    public void setUser(User user) {
        this.currentUser = user;

        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        telephoneField.setText(user.getTelephone() != null ? user.getTelephone() : "");
        genreCombo.setValue(user.getGenre());

        if (user.getRoles() != null) {
            rolesCombo.setValue(user.getRoles()
                    .replace("[\"", "")
                    .replace("\"]", ""));
        }

        statusCombo.setValue(user.getApprovalStatus());
        medicalField.setText(user.getMedicalCondition() != null ? user.getMedicalCondition() : "");
    }

    private boolean valider() {
        StringBuilder errors = new StringBuilder();

        if (nomField.getText().trim().isEmpty())
            errors.append("• Nom obligatoire\n");

        if (prenomField.getText().trim().isEmpty())
            errors.append("• Prénom obligatoire\n");

        if (!emailField.getText().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
            errors.append("• Email invalide\n");

        if (!telephoneField.getText().isEmpty() &&
                !telephoneField.getText().matches("\\d{8}"))
            errors.append("• Téléphone : exactement 8 chiffres\n");

        if (genreCombo.getValue() == null)
            errors.append("• Genre obligatoire\n");

        if (rolesCombo.getValue() == null)
            errors.append("• Rôle obligatoire\n");

        if (statusCombo.getValue() == null)
            errors.append("• Statut obligatoire\n");

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreurs de saisie");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(errors.toString());
            alert.show();
            return false;
        }
        return true;
    }

    @FXML
    public void handleEdit() {
        if (currentUser == null) {
            errorLabel.setText("Aucun utilisateur sélectionné.");
            return;
        }

        if (!valider()) return;

        try {
            currentUser.setNom(nomField.getText().trim());
            currentUser.setPrenom(prenomField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setTelephone(telephoneField.getText().trim());
            currentUser.setGenre(genreCombo.getValue());
            currentUser.setRoles("[\"" + rolesCombo.getValue() + "\"]");
            currentUser.setApprovalStatus(statusCombo.getValue());
            currentUser.setMedicalCondition(medicalField.getText().trim());

            userService.update(currentUser);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succès");
            success.setHeaderText(null);
            success.setContentText("Utilisateur modifié avec succès !");
            success.showAndWait();

            goToList();

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void goToList() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/chroniccare/list-users.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }
}