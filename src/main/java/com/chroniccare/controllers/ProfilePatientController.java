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

public class ProfilePatientController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private Label statusBadge;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<String> genreCombo;
    @FXML private TextField medicalField;
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
        if (statusBadge != null) {
            String s = currentUser.getApprovalStatus();
            statusBadge.setText("approved".equals(s) ? "✓ Approuvé" : "pending".equals(s) ? "⏳ En attente" : "✗ Rejeté");
            statusBadge.setStyle("-fx-background-color: " +
                    ("approved".equals(s) ? "#22c55e" : "pending".equals(s) ? "#f97316" : "#ef4444") +
                    "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 11; -fx-font-weight: bold;");
        }
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        telephoneField.setText(currentUser.getTelephone() != null ? currentUser.getTelephone() : "");
        genreCombo.setValue(currentUser.getGenre());
        medicalField.setText(currentUser.getMedicalCondition() != null ? currentUser.getMedicalCondition() : "");
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
            currentUser.setMedicalCondition(medicalField.getText().trim());
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
        String p = (user.getPrenom() != null && !user.getPrenom().isEmpty())
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String n = (user.getNom() != null && !user.getNom().isEmpty())
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return p + n;
    }
}