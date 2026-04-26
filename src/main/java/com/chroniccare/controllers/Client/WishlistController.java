package com.chroniccare.controllers.Client;

import com.chroniccare.entities.Produit;
import com.chroniccare.entities.Wishlist;
import com.chroniccare.services.ProduitsService;
import com.chroniccare.services.WishlistService;
import com.chroniccare.utils.FxNavigator;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * Contrôleur pour la page Wishlist (Liste de Souhaits)
 * Affiche tous les produits dans la wishlist de l'utilisateur
 */
public class WishlistController {

    @FXML private Node root;
    @FXML private TableView<WishlistItem> wishlistTable;
    @FXML private TableColumn<WishlistItem, String> nomCol;
    @FXML private TableColumn<WishlistItem, Double> prixCol;
    @FXML private TableColumn<WishlistItem, Integer> stockCol;
    @FXML private TableColumn<WishlistItem, Void> actionsCol;
    @FXML private Label countLabel;

    private final WishlistService wishlistService = new WishlistService();
    private final ProduitsService produitsService = new ProduitsService();
    private final ObservableList<WishlistItem> wishlistItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nomProduit"));
        prixCol.setCellValueFactory(new PropertyValueFactory<>("prix"));
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));

        // Colonne Actions (Retirer + Ajouter au panier)
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnAjouter = new Button("🛒 Ajouter au panier");
            private final Button btnRetirer = new Button("❌ Retirer");
            private final HBox box = new HBox(5, btnAjouter, btnRetirer);

            {
                btnAjouter.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnRetirer.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

                btnAjouter.setOnAction(e -> {
                    WishlistItem item = getTableView().getItems().get(getIndex());
                    ajouterAuPanier(item);
                });

                btnRetirer.setOnAction(e -> {
                    WishlistItem item = getTableView().getItems().get(getIndex());
                    retirerDuWishlist(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        loadWishlist();
    }

    private void loadWishlist() {
        try {
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            List<Wishlist> wishlist = wishlistService.findByUtilisateurId(userId);

            wishlistItems.clear();
            for (Wishlist w : wishlist) {
                Produit p = produitsService.findById(w.getProduitId());
                if (p != null) {
                    wishlistItems.add(new WishlistItem(w.getId(), p));
                }
            }

            wishlistTable.setItems(wishlistItems);
            updateCountLabel();

        } catch (Exception e) {
            showError("Chargement échoué", e.getMessage());
            e.printStackTrace();
        }
    }

    private void retirerDuWishlist(WishlistItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Retirer de la wishlist");
        confirm.setHeaderText("Êtes-vous sûr?");
        confirm.setContentText("Retirer \"" + item.getNomProduit() + "\" de votre wishlist?");

        confirm.showAndWait().ifPresent(response -> {
            if (response.getButtonData().isDefaultButton()) {
                try {
                    wishlistService.removeFromWishlist(
                        SessionManager.getInstance().getCurrentUser().getId(),
                        item.getProduitId()
                    );
                    loadWishlist();
                    showInfo("Succès", "Produit retiré de votre wishlist");
                } catch (Exception e) {
                    showError("Erreur", e.getMessage());
                }
            }
        });
    }

    private void ajouterAuPanier(WishlistItem item) {
        // TODO: Intégrer avec CartService
        showInfo("À venir", "Ajouter au panier depuis wishlist (comming soon)");
    }

    private void updateCountLabel() {
        int count = wishlistItems.size();
        countLabel.setText("Total: " + count + " produit(s) dans votre wishlist");
    }

    @FXML
    public void goToHome() {
        FxNavigator.go(anchor(), "/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToProduits() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/ProduitsDashboard.fxml");
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
    public void goToLogin() {
        SessionManager.getInstance().logout();
        FxNavigator.go(anchor(), "/com/chroniccare/login.fxml");
    }

    private Node anchor() {
        return root != null ? root : wishlistTable;
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Classe interne pour afficher les items de la wishlist
     */
    public static class WishlistItem {
        private final int wishlistId;
        private final int produitId;
        private final String nomProduit;
        private final double prix;
        private final int stock;

        public WishlistItem(int wishlistId, Produit produit) {
            this.wishlistId = wishlistId;
            this.produitId = produit.getId();
            this.nomProduit = produit.getNom();
            this.prix = produit.getPrix();
            this.stock = produit.getStock();
        }

        public int getWishlistId() { return wishlistId; }
        public int getProduitId() { return produitId; }
        public String getNomProduit() { return nomProduit; }
        public Double getPrix() { return prix; }
        public Integer getStock() { return stock; }
    }
}

