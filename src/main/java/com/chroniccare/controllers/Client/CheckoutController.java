package com.chroniccare.controllers.Client;

import com.chroniccare.entities.Commande;
import com.chroniccare.entities.Livraison;
import com.chroniccare.entities.User;
import com.chroniccare.services.CartService;
import com.chroniccare.services.CommandeService;
import com.chroniccare.services.LivraisonService;
import com.chroniccare.utils.FxNavigator;
import com.chroniccare.utils.SessionManager;
import java.time.LocalDateTime;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CheckoutController {

    @FXML private Node root;
    @FXML private TextArea adresseArea;
    @FXML private ComboBox<String> paiementCombo;
    @FXML private Label totalLabel;
    @FXML private Label infoLabel;
    @FXML private VBox deliverySection;
    @FXML private ComboBox<String> livraisonModeCombo;
    @FXML private TextField villeField;
    @FXML private TextField codePostalField;
    @FXML private Button confirmButton;

    private Commande currentCommande;
    private boolean livraisonStep = false;

    private final CommandeService commandeService = new CommandeService();
    private final LivraisonService livraisonService = new LivraisonService();

    @FXML
    public void initialize() {
        paiementCombo.setItems(FXCollections.observableArrayList("card", "stripe", "cash"));
        paiementCombo.setValue("cash");

        livraisonModeCombo.setItems(FXCollections.observableArrayList("standard", "express", "point_relais"));
        livraisonModeCombo.setValue("standard");

        updateView();
    }

    public void setCommande(Commande commande) {
        this.currentCommande = commande;
        updateView();
    }

    private void updateView() {
        if (totalLabel == null || paiementCombo == null) return;
        if (currentCommande == null) {
            double total = CartService.getInstance().getTotalPrice();
            totalLabel.setText("Total: " + total);
            if (infoLabel != null) {
                infoLabel.setText(total > 0 ? "Commande panier" : "Panier vide");
            }
            return;
        }
        totalLabel.setText("Total: " + currentCommande.getTotal());
        if (currentCommande.getMethodePaiement() != null && !currentCommande.getMethodePaiement().isBlank()) {
            paiementCombo.setValue(currentCommande.getMethodePaiement());
        }
        if (infoLabel != null && currentCommande.getNumeroCommande() != null) {
            infoLabel.setText("Commande: " + currentCommande.getNumeroCommande());
        }
    }

    @FXML
    public void annuler() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/Panier.fxml");
    }

    @FXML
    public void confirmerCommande() {
        if (!livraisonStep) {
            if (adresseArea.getText() == null || adresseArea.getText().trim().isEmpty()) {
                showError("Adresse obligatoire");
                return;
            }
            livraisonStep = true;
            if (deliverySection != null) {
                deliverySection.setManaged(true);
                deliverySection.setVisible(true);
            }
            if (infoLabel != null) {
                infoLabel.setText("Choisissez le mode de livraison et completez les informations.");
            }
            if (confirmButton != null) {
                confirmButton.setText("Valider livraison");
            }
            return;
        }

        // Etape 2: valider et enregistrer la livraison + (si besoin) la commande panier
        try {
            String adresse = adresseArea.getText() == null ? "" : adresseArea.getText().trim();
            String ville = villeField.getText() == null ? "" : villeField.getText().trim();
            String codePostal = codePostalField.getText() == null ? "" : codePostalField.getText().trim();
            String modeLivraison = livraisonModeCombo.getValue();

            if (adresse.isEmpty() || ville.isEmpty() || codePostal.isEmpty()) {
                showError("Veuillez completer adresse, ville et code postal");
                return;
            }
            if (modeLivraison == null || modeLivraison.isBlank()) {
                showError("Mode de livraison obligatoire");
                return;
            }

            // 1) Créer la commande si on vient du panier
            if (currentCommande == null) {
                User user = SessionManager.getInstance().getCurrentUser();
                if (user == null || user.getId() <= 0) {
                    showError("Utilisateur non connecté");
                    return;
                }
                if (CartService.getInstance().getItems().isEmpty()) {
                    showError("Panier vide");
                    return;
                }
                String paiement = paiementCombo.getValue();
                if (paiement == null || paiement.isBlank()) {
                    showError("Methode de paiement obligatoire");
                    return;
                }

                double total = CartService.getInstance().getTotalPrice();
                currentCommande = commandeService.createFromCart(user.getId(), total, paiement, CartService.getInstance().getItems());
            }

            // 2) Enregistrer la livraison
            Livraison livraison = new Livraison();
            livraison.setCommandeId(currentCommande.getId());
            livraison.setStatut("en_preparation");
            livraison.setAdresse(adresse);
            livraison.setVille(ville);
            livraison.setCodePostal(codePostal);
            livraison.setTrackingCode(null);
            livraison.setDatePrevue(LocalDateTime.now().plusDays(modeLivraison.equals("express") ? 1 : 3));
            livraison.setCreatedAt(LocalDateTime.now());
            livraison.setUpdatedAt(LocalDateTime.now());

            livraisonService.save(livraison);

            // 3) Nettoyer panier si commande issue du panier
            CartService.getInstance().clear();

            showError("Livraison enregistrée avec succes");
            FxNavigator.go(anchor(), "/com/chroniccare/Client/CommandesDashboard.fxml");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void goToHome() {
        FxNavigator.go(anchor(), "/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToCommandes() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/CommandesDashboard.fxml");
    }

    @FXML
    public void goToPanier() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/Panier.fxml");
    }

    @FXML
    public void goToCatalogue() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/ProduitsDashboard.fxml");
    }

    @FXML
    public void goToLogin() {
        SessionManager.getInstance().logout();
        FxNavigator.go(anchor(), "/com/chroniccare/login.fxml");
    }

    private Node anchor() {
        if (root != null) return root;
        throw new IllegalStateException("Ancre de navigation manquante: ajoutez fx:id=\"root\" sur le noeud racine du FXML");
    }
}
