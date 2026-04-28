package com.chroniccare.controllers.Client;

import com.chroniccare.entities.Produit;
import com.chroniccare.entities.User;
import com.chroniccare.services.CartService;
import com.chroniccare.services.WishlistService;
import com.chroniccare.utils.FxNavigator;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;

public class ProduitDetailController {

    @FXML
    private Node root;
    @FXML
    private Label nomLabel;
    @FXML
    private Label categorieLabel;
    @FXML
    private Label prixLabel;
    @FXML
    private Label stockLabel;
    @FXML
    private Label statutLabel;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label messageLabel;
    @FXML
    private Button wishlistBtn;
    @FXML
    private Spinner<Integer> qtySpinner;

    private final WishlistService wishlistService = new WishlistService();
    private Produit produit;

    @FXML
    public void initialize() {
        if (qtySpinner != null) {
            qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        }
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
        if (produit == null) {
            return;
        }

        nomLabel.setText(produit.getNom() == null ? "Produit" : produit.getNom());
        categorieLabel
                .setText(produit.getCategorie() == null || produit.getCategorie().isBlank() ? "Catégorie non renseignée"
                        : produit.getCategorie());
        prixLabel.setText(String.format("%.2f €", produit.getPrix()));
        stockLabel.setText(String.valueOf(produit.getStock()));
        statutLabel.setText(produit.getStock() <= 0 ? "Rupture" : "Disponible");
        statutLabel.setStyle(produit.getStock() <= 0
                ? "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #b91c1c;"
                : "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #166534;");
        descriptionArea.setText(produit.getDescription() == null || produit.getDescription().isBlank()
                ? "Aucune description disponible."
                : produit.getDescription());

        int max = Math.max(1, produit.getStock());
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, 1));
        qtySpinner.setDisable(produit.getStock() <= 0);
        updateWishlistButton();
        updateMessage("Consultez les détails puis choisissez votre action.");
    }

    @FXML
    public void toggleWishlist() {
        if (produit == null) {
            showError("Aucun produit sélectionné");
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            showError("Vous devez être connecté pour gérer la wishlist");
            return;
        }

        try {
            boolean inWishlist = wishlistService.isInWishlist(user.getId(), produit.getId());
            if (inWishlist) {
                wishlistService.removeFromWishlist(user.getId(), produit.getId());
                updateMessage("Produit retiré de votre wishlist.");
            } else {
                wishlistService.addToWishlist(user.getId(), produit.getId());
                updateMessage("Produit ajouté à votre wishlist.");
            }
            updateWishlistButton();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void commanderMaintenant() {
        if (produit == null) {
            showError("Aucun produit sélectionné");
            return;
        }

        if (produit.getStock() <= 0) {
            showError("Ce produit est en rupture de stock");
            return;
        }

        try {
            var loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/chroniccare/Client/CommandeRapide.fxml"));
            javafx.scene.Parent rootNode = loader.load();
            CommandeRapideController controller = loader.getController();
            controller.setProduit(produit);
            root.getScene().setRoot(rootNode);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void ajouterAuPanier() {
        if (produit == null) {
            showError("Aucun produit sélectionné");
            return;
        }

        if (produit.getStock() <= 0) {
            showError("Ce produit est en rupture de stock");
            return;
        }

        int quantity = qtySpinner != null && qtySpinner.getValue() != null ? qtySpinner.getValue() : 1;
        quantity = Math.max(1, Math.min(quantity, produit.getStock()));

        try {
            CartService.getInstance().add(produit, quantity);
            updateMessage("Produit ajouté au panier.");
            showInfo("Ajout panier", "Produit ajouté au panier.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void goBack() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/ProduitsDashboard.fxml");
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
    public void goToWishlist() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/Wishlist.fxml");
    }

    @FXML
    public void goToLogin() {
        SessionManager.getInstance().logout();
        FxNavigator.go(anchor(), "/com/chroniccare/login.fxml");
    }

    private void updateWishlistButton() {
        if (wishlistBtn == null || produit == null) {
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            wishlistBtn.setText("❤️ Ajouter au wishlist");
            wishlistBtn.setStyle(
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 12 18; -fx-cursor: hand; -fx-background-radius: 8;");
            return;
        }

        try {
            boolean inWishlist = wishlistService.isInWishlist(user.getId(), produit.getId());
            wishlistBtn.setText(inWishlist ? "💔 Retirer du wishlist" : "❤️ Ajouter au wishlist");
            wishlistBtn.setStyle(inWishlist
                    ? "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 12 18; -fx-cursor: hand; -fx-background-radius: 8;"
                    : "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 12 18; -fx-cursor: hand; -fx-background-radius: 8;");
        } catch (Exception e) {
            wishlistBtn.setText("❤️ Ajouter au wishlist");
        }
    }

    private void updateMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ChronicCare");
        alert.setHeaderText("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Node anchor() {
        if (root != null) {
            return root;
        }
        throw new IllegalStateException(
                "Ancre de navigation manquante: ajoutez fx:id=\"root\" sur le noeud racine du FXML");
    }
}