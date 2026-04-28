package com.chroniccare.controllers.Client;

import com.chroniccare.entities.Commande;
import com.chroniccare.entities.Livraison;
import com.chroniccare.services.CommandeService;
import com.chroniccare.services.LivraisonService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface d'historique client - voir ses commandes et livraisons
 */
public class ClientHistoriqueController {

    @FXML private Label historyTitle;
    @FXML private Label utilisateurLabel;

    // Onglets
    @FXML private TableView<Commande> commandesTable;
    @FXML private TableColumn<Commande, Integer> colCommandeId;
    @FXML private TableColumn<Commande, String> colNumeroCommande;
    @FXML private TableColumn<Commande, String> colCommandeStatut;
    @FXML private TableColumn<Commande, Double> colTotal;
    @FXML private TableColumn<Commande, LocalDateTime> colDateCommande;

    @FXML private TableView<Livraison> livraisonsTable;
    @FXML private TableColumn<Livraison, Integer> colLivraisonId;
    @FXML private TableColumn<Livraison, Integer> colCommandeIdLiv;
    @FXML private TableColumn<Livraison, String> colLivraisonStatut;
    @FXML private TableColumn<Livraison, String> colAdresse;
    @FXML private TableColumn<Livraison, String> colVille;
    @FXML private TableColumn<Livraison, LocalDateTime> colDatePrevue;

    @FXML private Label commandesCountLabel;
    @FXML private Label livraisonsCountLabel;
    @FXML private Label totalCALabel;

    private final CommandeService commandeService = new CommandeService();
    private final LivraisonService livraisonService = new LivraisonService();

    // TODO: À récupérer depuis la session utilisateur
    private static final int UTILISATEUR_ID = 2;

    private final ObservableList<Commande> commandesList = FXCollections.observableArrayList();
    private final ObservableList<Livraison> livraisonsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupCommandesTable();
        setupLivraisonsTable();
        loadData();
    }

    private void setupCommandesTable() {
        colCommandeId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNumeroCommande.setCellValueFactory(new PropertyValueFactory<>("numeroCommande"));
        colCommandeStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colDateCommande.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        commandesTable.setItems(commandesList);
    }

    private void setupLivraisonsTable() {
        colLivraisonId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCommandeIdLiv.setCellValueFactory(new PropertyValueFactory<>("commandeId"));
        colLivraisonStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville"));
        colDatePrevue.setCellValueFactory(new PropertyValueFactory<>("datePrevue"));
        livraisonsTable.setItems(livraisonsList);
    }

    private void loadData() {
        try {
            utilisateurLabel.setText("ID Client: " + UTILISATEUR_ID);

            // Charger les commandes
            List<Commande> commandes = commandeService.findAll();
            commandesList.setAll(commandes.stream()
                    .filter(c -> c.getUtilisateurId() == UTILISATEUR_ID)
                    .toList());

            // Charger les livraisons
            List<Livraison> livraisons = livraisonService.findByUtilisateurId(UTILISATEUR_ID);
            livraisonsList.setAll(livraisons);

            // Statistiques
            commandesCountLabel.setText(String.valueOf(commandesList.size()));
            livraisonsCountLabel.setText(String.valueOf(livraisonsList.size()));
            double totalCA = commandesList.stream()
                    .mapToDouble(Commande::getTotal)
                    .sum();
            totalCALabel.setText(String.format("%.2f €", totalCA));

        } catch (Exception e) {
            showError("Erreur de chargement", e.getMessage());
        }
    }

    @FXML
    public void goToAccueil() {
        navigate("/com/chroniccare/Client/Home.fxml");
    }

    @FXML
    public void goToLogin() {
        navigate("/com/chroniccare/login.fxml");
    }

    private void navigate(String fxml) {
        try {
            java.net.URL resource = getClass().getResource(fxml);
            if (resource == null) {
                showError("Fichier non trouvé", "Le fichier " + fxml + " n'existe pas");
                return;
            }
            Parent root = FXMLLoader.load(resource);
            historyTitle.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Navigation échouée", e.getMessage());
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


