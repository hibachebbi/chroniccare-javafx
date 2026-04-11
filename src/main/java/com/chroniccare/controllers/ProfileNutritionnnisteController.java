package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ProfileNutritionnnisteController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private Label statusLabel;
    @FXML private Label completionLabel;
    @FXML private Label approvalLabel;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<String> genreCombo;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private final UserService userService = new UserService();
    private User currentUser;

    @FXML
    public void initialize() {
        genreCombo.setItems(FXCollections.observableArrayList("Homme", "Femme"));
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String initials = getInitials(currentUser);
        sidebarAvatar.setText(initials);
        sidebarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        topbarAvatar.setText(initials);
        topbarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        topbarDate.setText(LocalDate.now().format(fmt));

        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        telephoneField.setText(currentUser.getTelephone() != null ? currentUser.getTelephone() : "");
        genreCombo.setValue(currentUser.getGenre());

        String statut = currentUser.getApprovalStatus();
        approvalLabel.setText(getStatusLabel(statut));
        approvalLabel.setStyle(getStatusStyle(statut));
        statusLabel.setText(getStatusSimpleLabel(statut));
        statusLabel.setStyle("-fx-text-fill: " + getStatusColor(statut) + "; -fx-font-weight: bold;");

        clearMessages();
        updateCompletionLabel();
        nomField.textProperty().addListener((obs, oldValue, newValue) -> updateCompletionLabel());
        prenomField.textProperty().addListener((obs, oldValue, newValue) -> updateCompletionLabel());
        emailField.textProperty().addListener((obs, oldValue, newValue) -> updateCompletionLabel());
        telephoneField.textProperty().addListener((obs, oldValue, newValue) -> updateCompletionLabel());
        genreCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateCompletionLabel());
    }

    @FXML
    public void handleSave() {
        if (!valider()) return;
        try {
            currentUser.setNom(nomField.getText().trim());
            currentUser.setPrenom(prenomField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setTelephone(telephoneField.getText().trim());
            currentUser.setGenre(genreCombo.getValue());
            userService.update(currentUser);
            SessionManager.getInstance().setCurrentUser(currentUser);
            showSuccess("Profil mis à jour avec succès !");
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void goToHome() {
        navigate("/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToRdv() {
        navigate("/com/chroniccare/home.fxml");
    }

    @FXML
    public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    private boolean valider() {
        StringBuilder errors = new StringBuilder();
        if (nomField.getText().trim().isEmpty()) errors.append("• Nom obligatoire\n");
        if (prenomField.getText().trim().isEmpty()) errors.append("• Prénom obligatoire\n");
        if (!emailField.getText().matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) errors.append("• Email invalide\n");
        if (!telephoneField.getText().isEmpty() && !telephoneField.getText().matches("\\d{8}")) {
            errors.append("• Téléphone : exactement 8 chiffres\n");
        }
        if (genreCombo.getValue() == null) errors.append("• Genre obligatoire\n");

        if (errors.length() > 0) {
            showError(errors.toString().trim());
            return false;
        }

        clearMessages();
        return true;
    }

    private void navigate(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            nomField.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    private void updateCompletionLabel() {
        int completed = 0;
        if (nomField.getText() != null && !nomField.getText().trim().isEmpty()) completed++;
        if (prenomField.getText() != null && !prenomField.getText().trim().isEmpty()) completed++;
        if (emailField.getText() != null && !emailField.getText().trim().isEmpty()) completed++;
        if (telephoneField.getText() != null && !telephoneField.getText().trim().isEmpty()) completed++;
        if (genreCombo.getValue() != null && !genreCombo.getValue().trim().isEmpty()) completed++;
        completionLabel.setText(completed + "/5");
    }

    private void clearMessages() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setText("");
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        successLabel.setText("");
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private String getInitials(User user) {
        String p = (user.getPrenom() != null && !user.getPrenom().isEmpty())
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String n = (user.getNom() != null && !user.getNom().isEmpty())
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return p + n;
    }

    private String getStatusLabel(String s) {
        if ("approved".equals(s)) return "Approuvé";
        if ("pending".equals(s)) return "En attente";
        return "Rejeté";
    }

    private String getStatusSimpleLabel(String s) {
        if ("approved".equals(s)) return "Validé";
        if ("pending".equals(s)) return "En attente";
        return "Rejeté";
    }

    private String getStatusStyle(String s) {
        if ("approved".equals(s)) return "-fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-font-size: 14;";
        if ("pending".equals(s)) return "-fx-text-fill: #d97706; -fx-font-weight: bold; -fx-font-size: 14;";
        return "-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 14;";
    }

    private String getStatusColor(String s) {
        if ("approved".equals(s)) return "#16a34a";
        if ("pending".equals(s)) return "#d97706";
        return "#dc2626";
    }
}
