package com.chroniccare.controllers.Admin;

import com.chroniccare.entities.Produit;
import com.chroniccare.services.ProduitsService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;

public class EditProduitController {

    @FXML private TextField nomField;
    @FXML private TextField categorieField;
    @FXML private TextField prixField;
    @FXML private TextField stockField;
    @FXML private TextArea descriptionField;
    @FXML private CheckBox activeCheckBox;
    @FXML private Label errorLabel;

    private final ProduitsService service = new ProduitsService();
    private Produit currentProduit;

    public void setProduit(Produit produit) {
        this.currentProduit = produit;
        if (produit == null) {
            return;
        }

        nomField.setText(produit.getNom());
        categorieField.setText(produit.getCategorie());
        prixField.setText(String.valueOf(produit.getPrix()));
        stockField.setText(String.valueOf(produit.getStock()));
        descriptionField.setText(produit.getDescription() == null ? "" : produit.getDescription());
        activeCheckBox.setSelected(produit.isActive());
    }

    @FXML
    public void handleSave() {
        if (!valider()) return;

        try {
            Produit produit = (currentProduit == null) ? new Produit() : currentProduit;
            produit.setNom(nomField.getText().trim());
            produit.setCategorie(categorieField.getText().trim());
            produit.setPrix(parseDouble(prixField.getText(), "Prix invalide"));
            produit.setStock(parseInt(stockField.getText(), "Stock invalide"));
            produit.setDescription(descriptionField.getText() == null ? "" : descriptionField.getText().trim());
            produit.setActive(activeCheckBox.isSelected());
            produit.setCreatedAt(produit.getCreatedAt() == null ? LocalDateTime.now() : produit.getCreatedAt());

            if (currentProduit == null) {
                service.save(produit);
            } else {
                service.update(produit);
            }

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succes");
            success.setHeaderText(null);
            success.setContentText("Produit enregistre avec succes !");
            success.showAndWait();

            goToList();
        } catch (Exception e) {
            errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private boolean valider() {
        StringBuilder errors = new StringBuilder();

        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
            errors.append("• Nom obligatoire\n");
        }

        if (categorieField.getText() == null || categorieField.getText().trim().isEmpty()) {
            errors.append("• Categorie obligatoire\n");
        }

        try {
            double prix = parseDouble(prixField.getText(), "Prix invalide");
            if (prix <= 0) {
                errors.append("• Prix : doit etre superieur a 0\n");
            }
        } catch (Exception e) {
            errors.append("• Prix invalide\n");
        }

        try {
            int stock = parseInt(stockField.getText(), "Stock invalide");
            if (stock < 0) {
                errors.append("• Stock : doit etre positif\n");
            }
        } catch (Exception e) {
            errors.append("• Stock invalide\n");
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
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/Admin/ProduitsDashboard.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) {
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
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
