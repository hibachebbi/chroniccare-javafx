package com.chroniccare.controllers.Client;

import com.chroniccare.services.CartService;
import com.chroniccare.utils.FxNavigator;
import com.chroniccare.utils.SessionManager;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;

public class PanierController {

    @FXML private Node root;
    @FXML private TableView<CartService.CartItem> cartTable;
    @FXML private TableColumn<CartService.CartItem, String> colNom;
    @FXML private TableColumn<CartService.CartItem, Integer> colQty;
    @FXML private TableColumn<CartService.CartItem, Double> colPrix;
    @FXML private TableColumn<CartService.CartItem, Double> colTotal;
    @FXML private Label totalLabel;

    private final ObservableList<CartService.CartItem> cartItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getProduit().getNom()));
        colQty.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().getQuantity()).asObject());
        colPrix.setCellValueFactory(data -> new ReadOnlyDoubleWrapper(data.getValue().getProduit().getPrix()).asObject());
        colTotal.setCellValueFactory(data -> new ReadOnlyDoubleWrapper(data.getValue().getLineTotal()).asObject());

        cartTable.setItems(cartItems);

        cartTable.setRowFactory(tv -> {
            TableRow<CartService.CartItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editQuantity(row.getItem());
                }
            });
            return row;
        });

        refreshCart();
    }

    @FXML
    public void removeSelected() {
        CartService.CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Panier", "Veuillez sélectionner un produit.");
            return;
        }
        CartService.getInstance().remove(selected.getProduit().getId());
        refreshCart();
    }

    @FXML
    public void clearCart() {
        CartService.getInstance().clear();
        refreshCart();
    }

    @FXML
    public void checkoutInfo() {
        if (cartItems.isEmpty()) {
            showInfo("Panier", "Votre panier est vide.");
            return;
        }
        FxNavigator.go(anchor(), "/com/chroniccare/Client/Checkout.fxml");
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

    private void refreshCart() {
        cartItems.setAll(CartService.getInstance().getItems().values());
        totalLabel.setText("Total: " + CartService.getInstance().getTotalPrice());
    }

    private void editQuantity(CartService.CartItem item) {
        if (item == null) return;
        int maxStock = item.getProduit().getStock();
        TextInputDialog dialog = new TextInputDialog(String.valueOf(item.getQuantity()));
        dialog.setTitle("Modifier la quantite");
        dialog.setHeaderText(item.getProduit().getNom());
        dialog.setContentText("Quantite (max " + maxStock + "):");

        dialog.showAndWait().ifPresent(value -> {
            try {
                int qty = Integer.parseInt(value.trim());
                if (qty <= 0) {
                    showInfo("Quantite invalide", "La quantite doit etre superieure a 0.");
                    return;
                }
                if (qty > maxStock) {
                    showInfo("Stock insuffisant", "Stock disponible: " + maxStock);
                    return;
                }
                CartService.getInstance().setQuantity(item.getProduit().getId(), qty);
                refreshCart();
            } catch (NumberFormatException e) {
                showInfo("Quantite invalide", "Veuillez saisir un nombre valide.");
            }
        });
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
