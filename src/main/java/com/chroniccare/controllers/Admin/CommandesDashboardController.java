package com.chroniccare.controllers.Admin;

import com.chroniccare.entities.Commande;
import com.chroniccare.services.AnnulationCommandeService;
import com.chroniccare.services.CommandeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.stream.Collectors;

public class CommandesDashboardController {

    @FXML private TableView<Commande> ordersTable;
    @FXML private TableColumn<Commande, String> statutCol;
    @FXML private TableColumn<Commande, Double> totalCol;
    @FXML private TableColumn<Commande, String> paiementCol;
    @FXML private TableColumn<Commande, Void> actionsCol;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilter;
    @FXML private Label pageLabel;
    @FXML private Label countLabel;

    private final CommandeService service = new CommandeService();
    private final AnnulationCommandeService annulationService = new AnnulationCommandeService();
    private final ObservableList<Commande> allCommandes = FXCollections.observableArrayList();
    private final ObservableList<Commande> filteredCommandes = FXCollections.observableArrayList();

    private int currentPage = 1;
    private static final int PAGE_SIZE = 8;

    @FXML
    public void initialize() {
        statutFilter.setItems(FXCollections.observableArrayList("Tous", "en_attente", "validee", "annulee", "livree"));
        statutFilter.setValue("Tous");

        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        paiementCol.setCellValueFactory(new PropertyValueFactory<>("methodePaiement"));

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final Button btnAnnuler = new Button("Annuler");
            private final HBox box = new HBox(5, btnEdit, btnAnnuler, btnDelete);
            {
                btnEdit.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnAnnuler.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

                btnEdit.setOnAction(e -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    goToEdit(commande);
                });

                btnAnnuler.setOnAction(e -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    handleAnnulation(commande);
                });

                btnDelete.setOnAction(e -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    handleDelete(commande);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        loadCommandes();
    }

    private void loadCommandes() {
        try {
            List<Commande> commandes = service.findAll();
            allCommandes.setAll(commandes);
            filteredCommandes.setAll(commandes);
            currentPage = 1;
            updateTable();
        } catch (Exception e) {
            showError("Chargement des commandes echoue", e.getMessage());
        }
    }

    private void updateTable() {
        int totalPages = (int) Math.ceil((double) filteredCommandes.size() / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filteredCommandes.size());

        if (fromIndex > filteredCommandes.size()) {
            currentPage = 1;
            fromIndex = 0;
            toIndex = Math.min(PAGE_SIZE, filteredCommandes.size());
        }

        ObservableList<Commande> pageData = FXCollections.observableArrayList(
                filteredCommandes.subList(fromIndex, toIndex)
        );

        ordersTable.setItems(pageData);
        pageLabel.setText("Page " + currentPage + " / " + totalPages);
        countLabel.setText("Total : " + filteredCommandes.size() + " commande(s)");
    }

    @FXML
    public void handleSearch() {

        String statut = statutFilter.getValue();

        List<Commande> result = allCommandes.stream()
                .filter(c -> {
                    boolean matchStatut = statut == null
                            || statut.equals("Tous")
                            || (c.getStatut() != null && c.getStatut().equalsIgnoreCase(statut));

                    return matchStatut;
                })
                .collect(Collectors.toList());

        filteredCommandes.setAll(result);
        currentPage = 1;
        updateTable();
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        statutFilter.setValue("Tous");
        filteredCommandes.setAll(allCommandes);
        currentPage = 1;
        updateTable();
    }

    @FXML
    public void nextPage() {
        int totalPages = (int) Math.ceil((double) filteredCommandes.size() / PAGE_SIZE);
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

    private void handleDelete(Commande commande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous supprimer la commande '" + commande.getNumeroCommande() + "' ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.delete(commande.getId());
                    loadCommandes();
                } catch (Exception e) {
                    showError("Suppression commande echouee", e.getMessage());
                }
            }
        });
    }

    private void handleAnnulation(Commande commande) {
        // Vérifier si la commande peut être annulée
        if (commande.getStatut().equals("annulee") || commande.getStatut().equals("livree")) {
            showError("Annulation impossible", "Cette commande a déjà le statut: " + commande.getStatut());
            return;
        }

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Annuler Commande");
        dialog.setHeaderText("Annuler la commande n°" + commande.getNumeroCommande());
        dialog.setContentText("Êtes-vous certain de vouloir annuler cette commande?\nLe stock sera restitué automatiquement.");
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    annulationService.annulerCommande(commande.getId(), "Annulée par l'administrateur", 1);
                    loadCommandes();
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Succès");
                    success.setHeaderText(null);
                    success.setContentText("Commande annulée avec succès et stock restitué.");
                    success.showAndWait();
                } catch (Exception e) {
                    showError("Erreur annulation", e.getMessage());
                }
            }
        });
    }

    private void goToEdit(Commande commande) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chroniccare/Admin/edit-commande.fxml"));
            Parent root = loader.load();
            EditCommandeController controller = loader.getController();
            controller.setCommande(commande);
            ordersTable.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation echouee", e.getMessage());
        }
    }

    @FXML
    public void goToAdd() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/Admin/edit-commande.fxml"));
            ordersTable.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation echouee", e.getMessage());
        }
    }

    @FXML
    public void goToHome() {
        navigate("/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToProduits() {
        navigate("/com/chroniccare/Admin/ProduitsDashboard.fxml");
    }

    @FXML
    public void goToLogin() {
        navigate("/com/chroniccare/login.fxml");
    }

    private void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            ordersTable.getScene().setRoot(root);
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
