package com.chroniccare.controllers.Admin;

import com.chroniccare.entities.Produit;
import com.chroniccare.services.ProduitsService;
import com.chroniccare.services.StockAlertService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProduitsDashboardController {

    @FXML private TableView<Produit> productsTable;
    @FXML private TableColumn<Produit, String> nomCol;
    @FXML private TableColumn<Produit, String> categorieCol;
    @FXML private TableColumn<Produit, Double> prixCol;
    @FXML private TableColumn<Produit, Integer> stockCol;
    @FXML private TableColumn<Produit, String> statutCol;
    @FXML private TableColumn<Produit, Boolean> activeCol;
    @FXML private TableColumn<Produit, Void> actionsCol;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categorieFilter;
    @FXML private Label pageLabel;
    @FXML private Label countLabel;

    private final ProduitsService service = new ProduitsService();
    private final StockAlertService stockAlertService = new StockAlertService();
    private final ObservableList<Produit> allProduits = FXCollections.observableArrayList();
    private final ObservableList<Produit> filteredProduits = FXCollections.observableArrayList();

    private int currentPage = 1;
    private static final int PAGE_SIZE = 8;

    @FXML
    public void initialize() {
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        categorieCol.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        prixCol.setCellValueFactory(new PropertyValueFactory<>("prix"));
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));

        statutCol.setCellValueFactory(cellData -> {
            Produit produit = cellData.getValue();
            String statut;
            if (produit.getStock() <= 0) {
                statut = "🔴 Rupture";
            } else if (produit.getStock() <= StockAlertService.STOCK_FAIBLE_SEUIL) {
                statut = "📉 Faible";
            } else {
                statut = "✅ OK";
            }
            return new javafx.beans.property.SimpleStringProperty(statut);
        });
        statutCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                if (item.contains("Rupture")) {
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 12;");
                } else if (item.contains("Faible")) {
                    setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 12;");
                } else {
                    setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 12;");
                }
            }
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox box = new HBox(5, btnEdit, btnDelete);
            {
                btnEdit.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

                btnEdit.setOnAction(e -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    goToEdit(produit);
                });

                btnDelete.setOnAction(e -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    handleDelete(produit);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                Produit produit = getTableView().getItems().get(getIndex());
                boolean rupture = produit != null && produit.getStock() <= 0;
                btnDelete.setDisable(!rupture);
                btnDelete.setText(rupture ? "Supprimer" : "Supprimer (stock > 0)");
                setGraphic(box);
            }
        });

        loadProduits();
    }

    private void loadProduits() {
        try {
            List<Produit> produits = service.findAll();
            allProduits.setAll(produits);
            filteredProduits.setAll(produits);
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

        ObservableList<Produit> pageData = FXCollections.observableArrayList(
                filteredProduits.subList(fromIndex, toIndex)
        );

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

    private void handleDelete(Produit produit) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous supprimer le produit '" + produit.getNom() + "' ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.delete(produit.getId());
                    loadProduits();
                } catch (Exception e) {
                    showError("Suppression produit echouee", e.getMessage());
                }
            }
        });
    }

    private void goToEdit(Produit produit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chroniccare/Admin/edit-produit.fxml"));
            Parent root = loader.load();
            EditProduitController controller = loader.getController();
            controller.setProduit(produit);
            productsTable.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation echouee", e.getMessage());
        }
    }

    @FXML
    public void goToAdd() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/Admin/edit-produit.fxml"));
            productsTable.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation echouee", e.getMessage());
        }
    }

    @FXML
    public void goToHome() {
        navigate("/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToCommandes() {
        navigate("/com/chroniccare/Admin/CommandesDashboard.fxml");
    }

    @FXML
    public void goToLogin() {
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
        alert.setContentText(message);
        alert.showAndWait();
    }
}
