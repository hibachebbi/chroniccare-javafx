package com.chroniccare.controllers.Client;

import com.chroniccare.models.Commande;
import com.chroniccare.models.User;
import com.chroniccare.services.AnnulationCommandeService;
import com.chroniccare.services.CommandeService;
import com.chroniccare.services.OrderReminderEmailService;
import com.chroniccare.utils.FxNavigator;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
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

    @FXML
    private Node root;

    @FXML
    private TableView<Commande> ordersTable;
    @FXML
    private TableColumn<Commande, String> statutCol;
    @FXML
    private TableColumn<Commande, Double> totalCol;
    @FXML
    private TableColumn<Commande, String> paiementCol;
    @FXML
    private TableColumn<Commande, Void> actionsCol;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statutFilter;
    @FXML
    private Label pageLabel;
    @FXML
    private Label countLabel;

    private final CommandeService service = new CommandeService();
    private final AnnulationCommandeService annulationService = new AnnulationCommandeService();
    private final OrderReminderEmailService orderReminderEmailService = new OrderReminderEmailService();
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
            private final Button btnCheckout = new Button("Payer");
            private final Button btnAnnuler = new Button("🟠 Annuler");
            private final Button btnRappel = new Button("Rappel email");
            private final HBox box = new HBox(5, btnCheckout, btnAnnuler, btnRappel);
            {
            box.getStyleClass().add("order-action-box");
            btnCheckout.getStyleClass().add("table-action-primary");
            btnAnnuler.getStyleClass().add("table-action-warning");
            btnRappel.getStyleClass().add("table-action-info");

                btnCheckout.setOnAction(e -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    goToCheckout(commande);
                });

                btnAnnuler.setOnAction(e -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    handleAnnulation(commande);
                });

                btnRappel.setOnAction(e -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    handleEmailReminder(commande);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Commande commande = getTableView().getItems().get(getIndex());
                    if (commande == null || commande.getStatut() == null) {
                        setGraphic(null);
                        return;
                    }

                    String statut = commande.getStatut().trim().toLowerCase();

                    // Afficher "Payer" seulement si en_attente et non payée
                    boolean showCheckout = "en_attente".equals(statut);
                    btnCheckout.setVisible(showCheckout);
                    btnCheckout.setManaged(showCheckout);

                    // Afficher "Annuler" seulement si en_attente ou validee
                    // Exclure explicitement annulee et livree
                    boolean canCancel = "en_attente".equals(statut) || "validee".equals(statut);
                    btnAnnuler.setVisible(canCancel);
                    btnAnnuler.setManaged(canCancel);

                    // Afficher le rappel tant que la commande n'est pas terminee
                    boolean canRemind = !"annulee".equals(statut) && !"livree".equals(statut);
                    btnRappel.setVisible(canRemind);
                    btnRappel.setManaged(canRemind);

                    // Désactiver le bouton pour les commandes non annulables
                    if (!canCancel) {
                        btnAnnuler.setDisable(true);
                        btnAnnuler.setOpacity(0.5);
                    } else {
                        btnAnnuler.setDisable(false);
                        btnAnnuler.setOpacity(1.0);
                    }

                    setGraphic(box);
                }
            }
        });

        loadCommandes();
    }

    private void handleEmailReminder(Commande commande) {
        if (commande == null) {
            showError("Erreur", "Commande non valide pour envoi d'email");
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            showError("Erreur", "Utilisateur non connecte");
            return;
        }

        OrderReminderEmailService.ReminderResult result = orderReminderEmailService.sendOrderReminder(user, commande);
        Alert alert = new Alert(result.isSent() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(result.isSent() ? "Rappel envoye" : "Rappel non envoye");
        alert.setContentText(result.getMessage());
        alert.showAndWait();
    }

    private void goToCheckout(Commande commande) {
        if (commande == null)
            return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chroniccare/Client/Checkout.fxml"));
            Parent root = loader.load();
            CheckoutController controller = loader.getController();
            controller.setCommande(commande);
            anchor().getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation echouee", e.getMessage());
        }
    }

    /**
     * Gère l'annulation d'une commande par le client
     * Vérifications:
     * - La commande existe
     * - Le statut est annulable (en_attente ou validee)
     * - Confirmation utilisateur
     */
    private void handleAnnulation(Commande commande) {
        if (commande == null) {
            showError("Erreur", "Commande non valide");
            return;
        }

        // Récupération et normalisation du statut
        String statut = commande.getStatut();
        if (statut == null || statut.trim().isEmpty()) {
            showError("Erreur", "Statut de commande invalide");
            return;
        }

        statut = statut.trim().toLowerCase();

        // Vérification métier stricte
        if (!statut.equals("en_attente") && !statut.equals("validee")) {
            String message;
            if (statut.equals("annulee")) {
                message = "Cette commande est déjà annulée.\nVous ne pouvez pas l'annuler à nouveau.";
            } else if (statut.equals("livree")) {
                message = "Impossible d'annuler une commande déjà livrée.";
            } else {
                message = "Cette commande ne peut pas être annulée (statut: " + statut + ").";
            }
            showError("Annulation impossible", message);
            return;
        }

        // Dialogue de confirmation avec détails
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler la commande");
        confirm.setHeaderText("Confirmez-vous l'annulation?");
        confirm.setContentText("Commande n°" + commande.getNumeroCommande() +
                "\nMontant: " + String.format("%.2f", commande.getTotal()) + "€\n" +
                "Statut actuel: " + statut + "\n\n" +
                "Après annulation:\n" +
                "- Le statut passera à 'annulée'\n" +
                "- Vous serez remboursé automatiquement\n" +
                "- Le stock sera restitué");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    User currentUser = SessionManager.getInstance().getCurrentUser();
                    if (currentUser == null) {
                        showError("Erreur", "Vous devez être connecté pour annuler une commande");
                        return;
                    }

                    // Appel au service d'annulation
                    annulationService.annulerCommande(
                            commande.getId(),
                            "Annulée par le client",
                            currentUser.getId());

                    // Recharger les commandes depuis la BD
                    loadCommandes();

                    // Message de succès
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Succès");
                    success.setHeaderText("Commande annulée");
                    success.setContentText(
                            "La commande n°" + commande.getNumeroCommande() + " a été annulée avec succès.\n\n" +
                                    "Remboursement: " + String.format("%.2f", commande.getTotal()) + "€\n" +
                                    "Délai: 3-5 jours ouvrables");
                    success.showAndWait();

                } catch (IllegalArgumentException e) {
                    // Erreur métier (statut, etc.)
                    showError("Erreur d'annulation", e.getMessage());
                } catch (Exception e) {
                    // Erreur technique
                    showError("Erreur système", "Une erreur est survenue lors de l'annulation.\n" +
                            "Détail: " + (e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
                    e.printStackTrace();
                }
            }
        });
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
        if (totalPages == 0)
            totalPages = 1;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filteredCommandes.size());

        if (fromIndex > filteredCommandes.size()) {
            currentPage = 1;
            fromIndex = 0;
            toIndex = Math.min(PAGE_SIZE, filteredCommandes.size());
        }

        ObservableList<Commande> pageData = FXCollections
                .observableArrayList(filteredCommandes.subList(fromIndex, toIndex));
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
        FxNavigator.go(anchor(), "/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToProduits() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/ProduitsDashboard.fxml");
    }

    @FXML
    public void goToPanier() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/Panier.fxml");
    }

    @FXML
    public void goToLivraisons() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/LivraisonsDashboard.fxml");
    }

    @FXML
    public void goToLogin() {
        SessionManager.getInstance().logout();
        FxNavigator.go(anchor(), "/com/chroniccare/login.fxml");
    }

    private Node anchor() {
        if (root != null)
            return root;
        // fallback (au cas où root n'est pas injecté)
        return ordersTable;
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
