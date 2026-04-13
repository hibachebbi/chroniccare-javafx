package com.chroniccare.controllers.Client;

import com.chroniccare.entities.Produit;
import com.chroniccare.services.ProduitsService;
import com.chroniccare.services.CartService;
import com.chroniccare.utils.FxNavigator;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextInputDialog;

import java.util.List;
import java.util.stream.Collectors;

public class ProduitsDashboardController {

    // Ce node sert d'ancrage pour récupérer la Scene et naviguer.
    @FXML private Node root;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categorieFilter;
    @FXML private TableView<Produit> productsTable;
    @FXML private TableColumn<Produit, String> nomCol;
    @FXML private TableColumn<Produit, String> categorieCol;
    @FXML private TableColumn<Produit, Double> prixCol;
    @FXML private TableColumn<Produit, Integer> stockCol;
    @FXML private TableColumn<Produit, Void> actionsCol;
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
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnCommander = new Button("Commander");
            private final Button btnPanier = new Button("Ajouter au panier");
            private final HBox box = new HBox(6, btnCommander, btnPanier);
            {
                btnCommander.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnPanier.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

                btnCommander.setOnAction(e -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    goToCommandeRapide(produit);
                });

                btnPanier.setOnAction(e -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    addToCart(produit);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        categorieFilter.setItems(FXCollections.observableArrayList("Toutes"));
        categorieFilter.setValue("Toutes");

        loadProduits();
    }

    private void loadProduits() {
        try {
            List<Produit> produits = service.findAll();
            // Côté client: on affiche seulement les produits actifs
            produits = produits.stream().filter(Produit::isActive).collect(Collectors.toList());

            allProduits.setAll(produits);
            filteredProduits.setAll(produits);
            initCategorieFilter(produits);
            currentPage = 1;
            updateTable();
        } catch (Exception e) {
            // évite d'interrompre la navigation si la base est indisponible
            countLabel.setText("Erreur de chargement des produits");
        }
    }

    private void initCategorieFilter(List<Produit> produits) {
        List<String> categories = produits.stream()
                .map(Produit::getCategorie)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        ObservableList<String> items = FXCollections.observableArrayList("Toutes");
        items.addAll(categories);
        categorieFilter.setItems(items);
        if (categorieFilter.getValue() == null) {
            categorieFilter.setValue("Toutes");
        }
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
        String categorie = categorieFilter.getValue();

        List<Produit> result = allProduits.stream()
                .filter(p -> {
                    boolean matchKeyword = keyword.isEmpty()
                            || (p.getNom() != null && p.getNom().toLowerCase().contains(keyword))
                            || (p.getCategorie() != null && p.getCategorie().toLowerCase().contains(keyword));

                    boolean matchCategorie = categorie == null
                            || categorie.equals("Toutes")
                            || (p.getCategorie() != null && p.getCategorie().equalsIgnoreCase(categorie));

                    return matchKeyword && matchCategorie;
                })
                .collect(Collectors.toList());

        filteredProduits.setAll(result);
        currentPage = 1;
        updateTable();
    }

    @FXML
    public void handleReset(ActionEvent actionEvent) {
        searchField.clear();
        categorieFilter.setValue("Toutes");
        filteredProduits.setAll(allProduits);
        currentPage = 1;
        updateTable();
    }

    @FXML
    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updateTable();
        }
    }

    @FXML
    public void nextPage() {
        int totalPages = (int) Math.ceil((double) filteredProduits.size() / PAGE_SIZE);
        if (currentPage < totalPages) {
            currentPage++;
            updateTable();
        }
    }

    private void addToCart(Produit produit) {
        if (produit == null) return;
        if (produit.getStock() <= 0) {
            showInfo("Stock indisponible", "Ce produit est en rupture de stock.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Quantite");
        dialog.setHeaderText("Choisissez la quantite");
        dialog.setContentText("Quantite (max " + produit.getStock() + "):");

        dialog.showAndWait().ifPresent(value -> {
            try {
                int qty = Integer.parseInt(value.trim());
                if (qty <= 0) {
                    showInfo("Quantite invalide", "La quantite doit etre superieure a 0.");
                    return;
                }
                if (qty > produit.getStock()) {
                    showInfo("Stock insuffisant", "Stock disponible: " + produit.getStock());
                    return;
                }

                CartService.getInstance().add(produit, qty);
                showInfo("Ajout panier", "Produit ajoute au panier.");
                FxNavigator.go(anchor(), "/com/chroniccare/Client/Panier.fxml");
            } catch (NumberFormatException e) {
                showInfo("Quantite invalide", "Veuillez saisir un nombre valide.");
            }
        });
    }

    private void goToCommandeRapide(Produit produit) {
        if (produit == null) return;
        if (produit.getStock() <= 0) {
            showInfo("Stock indisponible", "Ce produit est en rupture de stock.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chroniccare/Client/CommandeRapide.fxml"));
            Parent root = loader.load();
            CommandeRapideController controller = loader.getController();
            controller.setProduit(produit);
            anchor().getScene().setRoot(root);
        } catch (Exception e) {
            showInfo("Navigation échouée", e.getMessage());
        }
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(header);
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
    public void goToLogin() {
        SessionManager.getInstance().logout();
        FxNavigator.go(anchor(), "/com/chroniccare/login.fxml");
    }

    private Node anchor() {
        // root peut être null si non déclaré dans le FXML; dans ce cas on tente de s'ancrer
        // sur n'importe quel node accessible via le graph (à compléter quand le FXML sera ajusté).
        if (root != null) return root;
        throw new IllegalStateException("Ancre de navigation manquante: ajoutez fx:id=\"root\" sur le noeud racine du FXML");
    }
}

