package com.chroniccare.controllers.Client;

import com.chroniccare.entities.Commande;
import com.chroniccare.entities.Produit;
import com.chroniccare.entities.User;
import com.chroniccare.services.CartService;
import com.chroniccare.services.CommandeService;
import com.chroniccare.utils.FxNavigator;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class CommandeRapideController {

    @FXML private Label nomLabel;
    @FXML private Label categorieLabel;
    @FXML private Label prixLabel;
    @FXML private Label stockLabel;
    @FXML private Spinner<Integer> qtySpinner;
    @FXML private ComboBox<String> paiementCombo;
    @FXML private Label totalLabel;

    private final CommandeService commandeService = new CommandeService();
    private Produit produit;

    @FXML
    public void initialize() {
        paiementCombo.setItems(FXCollections.observableArrayList("card", "stripe", "cash"));
        paiementCombo.setValue("cash");
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        qtySpinner.valueProperty().addListener((obs, oldValue, newValue) -> updateTotal());
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
        if (produit == null) {
            return;
        }
        nomLabel.setText(produit.getNom());
        categorieLabel.setText(produit.getCategorie() == null ? "" : produit.getCategorie());
        prixLabel.setText(String.valueOf(produit.getPrix()));
        stockLabel.setText(String.valueOf(produit.getStock()));

        int max = Math.max(1, produit.getStock());
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, 1));
        updateTotal();
    }

    @FXML
    public void confirmCommande() {
        if (produit == null) {
            showError("Produit manquant");
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getId() <= 0) {
            showError("Utilisateur non connecté");
            return;
        }

        int qty = qtySpinner.getValue();
        if (qty <= 0 || qty > produit.getStock()) {
            showError("Quantité invalide");
            return;
        }

        String paiement = paiementCombo.getValue();
        if (paiement == null || paiement.isBlank()) {
            showError("Paiement obligatoire");
            return;
        }

        double total = produit.getPrix() * qty;

        try {
            Map<Integer, CartService.CartItem> items = new LinkedHashMap<>();
            items.put(produit.getId(), new CartService.CartItem(produit, qty));
            Commande commande = commandeService.createFromCart(user.getId(), total, paiement, items);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("ChronicCare");
            alert.setHeaderText(null);
            alert.setContentText("Commande créée : " + commande.getNumeroCommande());
            alert.showAndWait();

            FxNavigator.go(nomLabel, "/com/chroniccare/Client/CommandesDashboard.fxml");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void goBack() {
        FxNavigator.go(nomLabel, "/com/chroniccare/Client/ProduitsDashboard.fxml");
    }

    private void updateTotal() {
        if (produit == null) {
            totalLabel.setText("Total: 0.00");
            return;
        }
        int qty = qtySpinner.getValue();
        double total = produit.getPrix() * qty;
        totalLabel.setText("Total: " + total);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ChronicCare");
        alert.setHeaderText("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }
}

