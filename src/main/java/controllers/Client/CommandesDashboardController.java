package controllers.Client;

import com.chroniccare.entities.Commande;
import com.chroniccare.entities.User;
import com.chroniccare.services.CommandeService;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    @FXML private TableColumn<Commande, String> numeroCol;
    @FXML private TableColumn<Commande, String> statutCol;
    @FXML private TableColumn<Commande, Double> totalCol;
    @FXML private TableColumn<Commande, String> paiementCol;
    @FXML private TableColumn<Commande, Void> actionsCol;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilter;
    @FXML private Label pageLabel;
    @FXML private Label countLabel;

    private final CommandeService service = new CommandeService();
    private final ObservableList<Commande> allCommandes = FXCollections.observableArrayList();
    private final ObservableList<Commande> filteredCommandes = FXCollections.observableArrayList();

    private int currentPage = 1;
    private static final int PAGE_SIZE = 8;

    @FXML
    public void initialize() {
        statutFilter.setItems(FXCollections.observableArrayList("Tous", "en_attente", "validee", "annulee", "livree"));
        statutFilter.setValue("Tous");

        numeroCol.setCellValueFactory(new PropertyValueFactory<>("numeroCommande"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        paiementCol.setCellValueFactory(new PropertyValueFactory<>("methodePaiement"));

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnCheckout = new Button("Payer");
            private final HBox box = new HBox(5, btnCheckout);
            {
                btnCheckout.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnCheckout.setOnAction(e -> navigate("/com/chroniccare/Client/Checkout.fxml"));
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
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.getId() > 0) {
                commandes = commandes.stream()
                        .filter(c -> c.getUtilisateurId() == currentUser.getId())
                        .collect(Collectors.toList());
            }

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

        ObservableList<Commande> pageData = FXCollections.observableArrayList(filteredCommandes.subList(fromIndex, toIndex));
        ordersTable.setItems(pageData);
        pageLabel.setText("Page " + currentPage + " / " + totalPages);
        countLabel.setText("Total : " + filteredCommandes.size() + " commande(s)");
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String statut = statutFilter.getValue();

        List<Commande> result = allCommandes.stream()
                .filter(c -> {
                    boolean matchKeyword = keyword.isEmpty()
                            || (c.getNumeroCommande() != null && c.getNumeroCommande().toLowerCase().contains(keyword));

                    boolean matchStatut = statut == null
                            || statut.equals("Tous")
                            || (c.getStatut() != null && c.getStatut().equalsIgnoreCase(statut));

                    return matchKeyword && matchStatut;
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

    @FXML
    public void goToHome() {
        navigate("/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToProduits() {
        navigate("/com/chroniccare/Client/ProduitsDashboard.fxml");
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

