package com.chroniccare.controllers.Admin;

import com.chroniccare.entities.Commande;
import com.chroniccare.services.CommandeService;
import com.chroniccare.services.LivraisonService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.time.LocalDateTime;
import java.util.Set;

public class EditCommandeController {

    @FXML private ComboBox<String> statutCombo;
    @FXML private ComboBox<String> paiementCombo;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private final CommandeService service = new CommandeService();
    private final LivraisonService livraisonService = new LivraisonService();

    private Commande currentCommande;
    private String oldStatut;
    private String oldPaiement;

    @FXML
    public void initialize() {
        statutCombo.setItems(FXCollections.observableArrayList("en_attente", "validee", "annulee", "livree"));
        paiementCombo.setItems(FXCollections.observableArrayList("card", "stripe", "cash"));

        // Par défaut: écran non alimenté => désactiver jusqu'à setCommande(...)
        setFormDisabled(true);
    }

    public void setCommande(Commande commande) {
        this.currentCommande = commande;
        if (commande == null) {
            showAlert(Alert.AlertType.WARNING, "ChronicCare", null, "Aucune commande sélectionnée. Retournez à la liste et choisissez une commande à modifier.");
            setFormDisabled(true);
            return;
        }

        this.oldStatut = safeTrim(commande.getStatut());
        this.oldPaiement = safeTrim(commande.getMethodePaiement());

        statutCombo.setValue(oldStatut);
        paiementCombo.setValue(oldPaiement);
        setFormDisabled(false);
    }

    @FXML
    public void handleSave() {
        if (!valider()) return;

        if (saveButton != null) saveButton.setDisable(true);
        try {
            if (currentCommande == null) {
                showAlert(Alert.AlertType.WARNING, "ChronicCare", null, "Aucune commande sélectionnée à modifier.");
                return;
            }

            // Règle: en modification admin, on ne touche pas au numéro/total/utilisateur.
            // On autorise uniquement la mise à jour du statut + mode de paiement.
            String newStatut = safeTrim(statutCombo.getValue());
            String newPaiement = safeTrim(paiementCombo.getValue());

            if (equalsIgnoreCase(oldStatut, newStatut) && equalsIgnoreCase(oldPaiement, newPaiement)) {
                showAlert(Alert.AlertType.INFORMATION, "ChronicCare", null, "Aucune modification à enregistrer.");
                return;
            }

            if (!isTransitionAllowed(oldStatut, newStatut)) {
                showAlert(Alert.AlertType.ERROR, "ChronicCare", "Transition refusée",
                        "Transition de statut non autorisée : '" + oldStatut + "' → '" + newStatut + "'.");
                return;
            }

            currentCommande.setStatut(newStatut);
            currentCommande.setMethodePaiement(newPaiement);
            currentCommande.setCreatedAt(currentCommande.getCreatedAt() == null ? LocalDateTime.now() : currentCommande.getCreatedAt());

            // Update SQL minimaliste (évite d'écraser numéro/total/utilisateur_id)
            service.updateAdminStatusAndPayment(currentCommande.getId(), newStatut, newPaiement);

            // Synchroniser la livraison avec le statut de commande
            if ("validee".equalsIgnoreCase(newStatut)) {
                livraisonService.updateStatutByCommandeId(currentCommande.getId(), "en_transit");
            } else if ("livree".equalsIgnoreCase(newStatut)) {
                livraisonService.updateStatutByCommandeId(currentCommande.getId(), "livree");
            } else if ("annulee".equalsIgnoreCase(newStatut)) {
                livraisonService.updateStatutByCommandeId(currentCommande.getId(), "annulee");
            }

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succes");
            success.setHeaderText(null);
            success.setContentText("Commande enregistree avec succes !");
            success.showAndWait();

            goToList();
        } catch (Exception e) {
            errorLabel.setText("Erreur : " + e.getMessage());
        } finally {
            if (saveButton != null) saveButton.setDisable(false);
        }
    }

    private boolean valider() {
        StringBuilder errors = new StringBuilder();

        if (statutCombo.getValue() == null || statutCombo.getValue().trim().isEmpty()) {
            errors.append("• Statut obligatoire\n");
        }

        if (paiementCombo.getValue() == null || paiementCombo.getValue().trim().isEmpty()) {
            errors.append("• Paiement obligatoire\n");
        }

        if (!errors.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreurs de saisie");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }

    private void setFormDisabled(boolean disabled) {
        if (statutCombo != null) statutCombo.setDisable(disabled);
        if (paiementCombo != null) paiementCombo.setDisable(disabled);
        if (saveButton != null) saveButton.setDisable(disabled);
    }

    private boolean isTransitionAllowed(String from, String to) {
        String f = safeTrim(from);
        String t = safeTrim(to);
        if (f == null || t == null) return true;

        // Règles simples (workflow minimal)
        if (equalsIgnoreCase(f, t)) return true;

        // Statuts terminaux
        if (equalsIgnoreCase(f, "livree")) return false;
        if (equalsIgnoreCase(f, "annulee")) return false;

        // Autorisations
        if (equalsIgnoreCase(f, "en_attente")) {
            return Set.of("validee", "annulee").contains(t.toLowerCase());
        }
        if (equalsIgnoreCase(f, "validee")) {
            return Set.of("livree", "annulee").contains(t.toLowerCase());
        }

        // Par défaut, permissif
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    @FXML
    public void goToList() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/Admin/CommandesDashboard.fxml"));
            statutCombo.getScene().setRoot(root);
        } catch (Exception e) {
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }
}
