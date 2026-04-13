package com.chroniccare.controllers.Admin;

import com.chroniccare.entities.Commande;
import com.chroniccare.services.CommandeService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;

public class EditCommandeController {

    @FXML private TextField numeroField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField totalField;
    @FXML private TextField utilisateurIdField;
    @FXML private ComboBox<String> paiementCombo;
    @FXML private Label errorLabel;

    private final CommandeService service = new CommandeService();
    private Commande currentCommande;

    @FXML
    public void initialize() {
        statutCombo.setItems(FXCollections.observableArrayList("en_attente", "validee", "annulee", "livree"));
        paiementCombo.setItems(FXCollections.observableArrayList("card", "stripe", "cash"));
    }

    public void setCommande(Commande commande) {
        this.currentCommande = commande;
        if (commande == null) {
            return;
        }

        numeroField.setText(commande.getNumeroCommande());
        statutCombo.setValue(commande.getStatut());
        totalField.setText(String.valueOf(commande.getTotal()));
        utilisateurIdField.setText(String.valueOf(commande.getUtilisateurId()));
        paiementCombo.setValue(commande.getMethodePaiement());
    }

    @FXML
    public void handleSave() {
        if (!valider()) return;
        try {
            Commande commande = (currentCommande == null) ? new Commande() : currentCommande;
            commande.setNumeroCommande(numeroField.getText().trim());
            commande.setStatut(statutCombo.getValue().trim());
            commande.setTotal(parseDouble(totalField.getText(), "Total invalide"));
            commande.setUtilisateurId(parseInt(utilisateurIdField.getText(), "Utilisateur invalide"));
            commande.setMethodePaiement(paiementCombo.getValue().trim());
            commande.setCreatedAt(commande.getCreatedAt() == null ? LocalDateTime.now() : commande.getCreatedAt());

            if (currentCommande == null) {
                service.save(commande);
            } else {
                service.update(commande);
            }

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succes");
            success.setHeaderText(null);
            success.setContentText("Commande enregistree avec succes !");
            success.showAndWait();

            goToList();
        } catch (Exception e) {
            errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private boolean valider() {
        StringBuilder errors = new StringBuilder();

        if (numeroField.getText() == null || numeroField.getText().trim().isEmpty()) {
            errors.append("• Numero commande obligatoire\n");
        }

        if (statutCombo.getValue() == null || statutCombo.getValue().trim().isEmpty()) {
            errors.append("• Statut obligatoire\n");
        }

        if (paiementCombo.getValue() == null || paiementCombo.getValue().trim().isEmpty()) {
            errors.append("• Paiement obligatoire\n");
        }

        try {
            double total = parseDouble(totalField.getText(), "Total invalide");
            if (total <= 0) {
                errors.append("• Total : doit etre superieur a 0\n");
            }
        } catch (Exception e) {
            errors.append("• Total invalide\n");
        }

        try {
            int userId = parseInt(utilisateurIdField.getText(), "Utilisateur invalide");
            if (userId <= 0) {
                errors.append("• Utilisateur : ID invalide\n");
            }
        } catch (Exception e) {
            errors.append("• Utilisateur invalide\n");
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreurs de saisie");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }

    @FXML
    public void goToList() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/Admin/CommandesDashboard.fxml"));
            numeroField.getScene().setRoot(root);
        } catch (Exception e) {
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private int parseInt(String value, String message) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(message);
        }
    }

    private double parseDouble(String value, String message) {
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (Exception e) {
            throw new IllegalArgumentException(message);
        }
    }
}
