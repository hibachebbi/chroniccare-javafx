package com.chroniccare.controllers;

import com.chroniccare.entities.User;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

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
    @FXML private Label approvalLabel;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<String> genreCombo;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private UserService userService = new UserService();
    private User currentUser;

    @FXML
    public void initialize() {
        genreCombo.setItems(FXCollections.observableArrayList("Homme", "Femme"));
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String initials = getInitials(currentUser);
        if (sidebarAvatar != null) sidebarAvatar.setText(initials);
        if (sidebarUserName != null) sidebarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (topbarAvatar != null) topbarAvatar.setText(initials);
        if (topbarUserName != null) topbarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (topbarDate != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            topbarDate.setText(LocalDate.now().format(fmt));
        }
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        telephoneField.setText(currentUser.getTelephone() != null ? currentUser.getTelephone() : "");
        genreCombo.setValue(currentUser.getGenre());

        String statut = currentUser.getApprovalStatus();
        if (approvalLabel != null) { approvalLabel.setText(getStatusLabel(statut)); approvalLabel.setStyle(getStatusStyle(statut)); }
        if (statusLabel != null) {
            statusLabel.setText(getStatusLabel(statut));
            statusLabel.setStyle("-fx-background-color: " + getStatusColor(statut) +
                    "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 11; -fx-font-weight: bold;");
        }
    }

    private boolean valider() {
        StringBuilder errors = new StringBuilder();
        if (nomField.getText().trim().isEmpty()) errors.append("• Nom obligatoire\n");
        if (prenomField.getText().trim().isEmpty()) errors.append("• Prénom obligatoire\n");
        if (!emailField.getText().matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) errors.append("• Email invalide\n");
        if (!telephoneField.getText().isEmpty() && !telephoneField.getText().matches("\\d{8}"))
            errors.append("• Téléphone : exactement 8 chiffres\n");
        if (genreCombo.getValue() == null) errors.append("• Genre obligatoire\n");
        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreurs de saisie");
            alert.setHeaderText("Veuillez corriger les erreurs :");
            alert.setContentText(errors.toString());
            alert.show();
            return false;
        }
        return true;
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
            if (successLabel != null) successLabel.setText("✓ Profil mis à jour avec succès !");
            if (errorLabel != null) errorLabel.setText("");
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void goToHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/home.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private String getInitials(User user) {
        String p = (user.getPrenom() != null && !user.getPrenom().isEmpty()) ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String n = (user.getNom() != null && !user.getNom().isEmpty()) ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return p + n;
    }
    private String getStatusLabel(String s) {
        if ("approved".equals(s)) return "✓ Approuvé";
        if ("pending".equals(s)) return "⏳ En attente";
        return "✗ Rejeté";
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