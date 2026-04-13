package controllers.Client;

import com.chroniccare.services.CartService;
import com.chroniccare.services.CartService.CartItem;
import com.chroniccare.utils.SessionManager;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;

public class PanierController {

    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> colNom;
    @FXML private TableColumn<CartItem, Integer> colQty;
    @FXML private TableColumn<CartItem, Double> colPrix;
    @FXML private TableColumn<CartItem, Double> colTotal;
    @FXML private Label totalLabel;

    private final CartService cart = CartService.getInstance();
    private final ObservableList<CartItem> model = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getProduit().getNom()));
        colPrix.setCellValueFactory(c -> new ReadOnlyDoubleWrapper(c.getValue().getProduit().getPrix()).asObject());
        colTotal.setCellValueFactory(c -> new ReadOnlyDoubleWrapper(c.getValue().getLineTotal()).asObject());
        colQty.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().getQuantity()).asObject());

        cartTable.setEditable(true);
        colQty.setEditable(true);
        colQty.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQty.setOnEditCommit(evt -> {
            CartItem item = evt.getRowValue();
            if (item == null || item.getProduit() == null) {
                return;
            }
            int newQty = evt.getNewValue() == null ? 0 : evt.getNewValue();

            // clamp vs stock total du produit
            int max = Math.max(0, item.getProduit().getStock());
            if (newQty > max) {
                newQty = max;
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.setTitle("Panier");
                warn.setHeaderText(null);
                warn.setContentText("Quantité ajustée au stock disponible (max: " + max + ").");
                warn.showAndWait();
            }

            cart.setQuantity(item.getProduit().getId(), newQty);
            reloadFromService();
        });

        reloadFromService();
    }

    private void reloadFromService() {
        model.setAll(cart.getItems().values());
        cartTable.setItems(model);
        cartTable.refresh();
        updateTotal();
    }

    private void updateTotal() {
        if (totalLabel != null) {
            totalLabel.setText(String.format("Total: %.2f", cart.getTotalPrice()));
        }
    }

    @FXML
    public void removeSelected() {
        CartItem selected = cartTable == null ? null : cartTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getProduit() == null) {
            return;
        }
        cart.remove(selected.getProduit().getId());
        reloadFromService();
    }

    @FXML
    public void clearCart() {
        cart.clear();
        reloadFromService();
    }

    @FXML
    public void checkoutInfo() {
        // Aller à l'écran de commande / checkout
        navigate("/com/chroniccare/Client/Checkout.fxml");
    }

    @FXML
    public void goToHome() {
        navigate("/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToCommandes() {
        navigate("/com/chroniccare/Client/CommandesDashboard.fxml");
    }

    @FXML
    public void goToCatalogue() {
        navigate("/com/chroniccare/Client/ProduitsDashboard.fxml");
    }

    @FXML
    public void goToLogin() {
        SessionManager.getInstance().logout();
        navigate("/com/chroniccare/login.fxml");
    }

    private void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            cartTable.getScene().setRoot(root);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ChronicCare");
            alert.setHeaderText("Navigation échouée");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private static final class IntegerStringConverter extends StringConverter<Integer> {
        @Override
        public String toString(Integer object) {
            return object == null ? "" : object.toString();
        }

        @Override
        public Integer fromString(String string) {
            if (string == null) {
                return 0;
            }
            String s = string.trim();
            if (s.isEmpty()) {
                return 0;
            }
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}

