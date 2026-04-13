package controllers.Client;

import com.chroniccare.entities.Produit;
import com.chroniccare.services.CartService;
import com.chroniccare.services.ProduitsService;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProduitsDashboardController {

    @FXML private TableView<Produit> productsTable;
    @FXML private TableColumn<Produit, Integer> idCol;
    @FXML private TableColumn<Produit, String> nomCol;
    @FXML private TableColumn<Produit, String> categorieCol;
    @FXML private TableColumn<Produit, Double> prixCol;
    @FXML private TableColumn<Produit, Integer> stockCol;
    @FXML private TableColumn<Produit, Void> actionsCol;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categorieFilter;
    @FXML private Label pageLabel;
    @FXML private Label countLabel;

    private final ProduitsService service = new ProduitsService();
    private final ObservableList<Produit> allProduits = FXCollections.observableArrayList();
    private final ObservableList<Produit> filteredProduits = FXCollections.observableArrayList();

    private int currentPage = 1;
    private static final int PAGE_SIZE = 8;

    @FXML
    public void initialize() {
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        categorieCol.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        prixCol.setCellValueFactory(new PropertyValueFactory<>("prix"));
        // Afficher le stock disponible (stock total - stock réservé dans le panier)
        stockCol.setCellValueFactory(cell -> {
            Produit p = cell.getValue();
            int available = CartService.getInstance().getAvailableStock(p);
            return new javafx.beans.property.SimpleIntegerProperty(available).asObject();
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnCommander = new Button("Commander");
            private final Button btnAddToCart = new Button("Ajouter au panier");
            private final HBox box = new HBox(5, btnCommander, btnAddToCart);
            {
                btnCommander.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnAddToCart.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

                // Commander = commande rapide (1 produit) -> Checkout
                btnCommander.setOnAction(e -> {
                    Produit produit = getRowProduitSafe();
                    if (produit == null) {
                        return;
                    }

                    int available = CartService.getInstance().getAvailableStock(produit);
                    if (available <= 0) {
                        Alert warn = new Alert(Alert.AlertType.WARNING);
                        warn.setTitle("Panier");
                        warn.setHeaderText(null);
                        warn.setContentText("Stock insuffisant pour ce produit.");
                        warn.showAndWait();
                        return;
                    }

                    Integer qty = askQuantity(produit.getNom(), available);
                    if (qty == null) {
                        return; // annulé
                    }

                    // Commande rapide: on remplace le panier par ce produit puis on va au checkout
                    CartService.getInstance().clear();
                    CartService.getInstance().add(produit, qty);
                    productsTable.refresh();
                    navigate("/com/chroniccare/Client/Checkout.fxml");
                });

                // Ajouter au panier = ajoute puis ouvre le panier
                btnAddToCart.setOnAction(e -> {
                    Produit produit = getRowProduitSafe();
                    if (produit == null) {
                        return;
                    }

                    int available = CartService.getInstance().getAvailableStock(produit);
                    if (available <= 0) {
                        Alert warn = new Alert(Alert.AlertType.WARNING);
                        warn.setTitle("Panier");
                        warn.setHeaderText(null);
                        warn.setContentText("Stock insuffisant pour ce produit.");
                        warn.showAndWait();
                        return;
                    }

                    Integer qty = askQuantity(produit.getNom(), available);
                    if (qty == null) {
                        return;
                    }

                    CartService.getInstance().add(produit, qty);
                    productsTable.refresh();
                    goToPanier();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }

            private Produit getRowProduitSafe() {
                if (isEmpty()) {
                    return null;
                }
                int idx = getIndex();
                TableView<Produit> tv = getTableView();
                if (tv == null || tv.getItems() == null || idx < 0 || idx >= tv.getItems().size()) {
                    return null;
                }
                return tv.getItems().get(idx);
            }
        });

        loadProduits();
    }

    private Integer askQuantity(String nomProduit, int max) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Ajouter au panier");
        dialog.setHeaderText(null);
        dialog.setContentText("Quantité pour '" + (nomProduit == null ? "Produit" : nomProduit) + "' (max: " + max + ") :");

        return dialog.showAndWait()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException ex) {
                        return -1;
                    }
                })
                .filter(q -> q > 0)
                .map(q -> Math.min(q, max))
                .orElseGet(() -> {
                    Alert warn = new Alert(Alert.AlertType.WARNING);
                    warn.setTitle("Ajouter au panier");
                    warn.setHeaderText(null);
                    warn.setContentText("Quantité invalide.");
                    warn.showAndWait();
                    return null;
                });
    }

    private void loadProduits() {
        try {
            List<Produit> produits = service.findAll();
            allProduits.setAll(produits.stream().filter(Produit::isActive).collect(Collectors.toList()));
            filteredProduits.setAll(allProduits);
            currentPage = 1;
            fillCategories();
            updateTable();
        } catch (Exception e) {
            showError("Chargement des produits echoue", e.getMessage());
        }
    }

    private void fillCategories() {
        Set<String> categories = allProduits.stream()
                .map(Produit::getCategorie)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.toSet());

        ObservableList<String> values = FXCollections.observableArrayList();
        values.add("Toutes");
        values.addAll(categories.stream().sorted().collect(Collectors.toList()));
        categorieFilter.setItems(values);
        categorieFilter.setValue("Toutes");
    }

    private void updateTable() {
        int totalPages = (int) Math.ceil((double) filteredProduits.size() / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filteredProduits.size());

        if (fromIndex > filteredProduits.size()) {
            currentPage = 1;
            fromIndex = 0;
            toIndex = Math.min(PAGE_SIZE, filteredProduits.size());
        }

        ObservableList<Produit> pageData = FXCollections.observableArrayList(filteredProduits.subList(fromIndex, toIndex));
        productsTable.setItems(pageData);
        pageLabel.setText("Page " + currentPage + " / " + totalPages);
        countLabel.setText("Total : " + filteredProduits.size() + " produit(s)");
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String category = categorieFilter.getValue();

        List<Produit> result = allProduits.stream()
                .filter(p -> {
                    boolean matchKeyword = keyword.isEmpty()
                            || (p.getNom() != null && p.getNom().toLowerCase().contains(keyword))
                            || (p.getCategorie() != null && p.getCategorie().toLowerCase().contains(keyword));

                    boolean matchCategory = category == null
                            || category.equals("Toutes")
                            || (p.getCategorie() != null && p.getCategorie().equalsIgnoreCase(category));

                    return matchKeyword && matchCategory;
                })
                .collect(Collectors.toList());

        filteredProduits.setAll(result);
        currentPage = 1;
        updateTable();
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        categorieFilter.setValue("Toutes");
        filteredProduits.setAll(allProduits);
        currentPage = 1;
        updateTable();
    }

    @FXML
    public void nextPage() {
        int totalPages = (int) Math.ceil((double) filteredProduits.size() / PAGE_SIZE);
        if (currentPage < totalPages) {
            currentPage++;
            updateTable();
        }
    }

    @FXML
    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updateTable();
        }
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
    public void goToPanier() {
        navigate("/com/chroniccare/Client/Panier.fxml");
    }

    @FXML
    public void goToLogin() {
        SessionManager.getInstance().logout();
        navigate("/com/chroniccare/login.fxml");
    }

    private void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            productsTable.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation echouee", e.getMessage());
        }
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(header);
        alert.setContentText(message == null || message.isBlank() ? "Une erreur inconnue s'est produite." : message);
        alert.showAndWait();
    }

    @SuppressWarnings("unused")
    private void showError(String header, Throwable t) {
        StringWriter sw = new StringWriter();
        if (t != null) {
            t.printStackTrace(new PrintWriter(sw));
        }
        showError(header, sw.toString());
    }
}
