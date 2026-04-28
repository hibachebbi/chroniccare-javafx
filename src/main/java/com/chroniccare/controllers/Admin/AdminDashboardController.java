package com.chroniccare.controllers.Admin;

import com.chroniccare.services.CommandeService;
import com.chroniccare.services.CommandeWorkflowService;
import com.chroniccare.services.LivraisonWorkflowService;
import com.chroniccare.services.ProduitsService;
import com.chroniccare.services.StockAlertService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.io.IOException;

/**
 * Dashboard Administrateur - Vue d'ensemble globale
 */
public class AdminDashboardController {

    @FXML private Label caLabel;
    @FXML private Label commandesTotalLabel;
    @FXML private Label commandesTodayLabel;
    @FXML private Label commandesPendingLabel;
    @FXML private Label livraisonsLabel;
    @FXML private Label livraisonsRetardLabel;
    @FXML private Label produitsLabel;
    @FXML private Label produitsStockFaibleLabel;
    @FXML private Label produitsRuptureLabel;

    @FXML private ProgressBar caProgress;
    @FXML private VBox alertsBox;

    private final CommandeService commandeService = new CommandeService();
    private final CommandeWorkflowService commandeWorkflow = new CommandeWorkflowService();
    private final ProduitsService produitsService = new ProduitsService();
    private final StockAlertService stockAlertService = new StockAlertService();
    private final LivraisonWorkflowService livraisonWorkflow = new LivraisonWorkflowService();

    @FXML
    public void initialize() {
        refreshDashboard();
    }

    @FXML
    public void refreshDashboard() {
        try {
            //  Chiffre d'affaires
            double totalCA = commandeService.sumTotal();
            double completedCA = commandeWorkflow.sumCompletedCommandes();
            caLabel.setText(String.format("%.2f € / %.2f €", completedCA, totalCA));
            caProgress.setProgress(totalCA > 0 ? completedCA / totalCA : 0);

            //  Commandes
            int totalCommandes = commandeService.countAll();
            int commandesToday = commandeWorkflow.countTodayCommandes();
            int commandesPending = commandeWorkflow.countPendingCommandes();
            commandesTotalLabel.setText(String.valueOf(totalCommandes));
            commandesTodayLabel.setText(String.valueOf(commandesToday));
            commandesPendingLabel.setText(String.valueOf(commandesPending));

            //  Livraisons
            int totalLivraisons = livraisonWorkflow.countAll();
            int livraisonsRetard = livraisonWorkflow.countLivraisonsEnRetard();
            livraisonsLabel.setText(String.valueOf(totalLivraisons));
            livraisonsRetardLabel.setText(String.valueOf(livraisonsRetard));

            //  Produits
            int totalProduits = produitsService.countAll();
            int stockFaible = stockAlertService.countStockFaible();
            int rupture = stockAlertService.countRupture();
            produitsLabel.setText(String.valueOf(totalProduits));
            produitsStockFaibleLabel.setText(String.valueOf(stockFaible));
            produitsRuptureLabel.setText(String.valueOf(rupture));

            //  Alertes
            updateAlerts(commandesPending, livraisonsRetard, stockFaible, rupture);

        } catch (Exception e) {
            showError("Erreur de chargement", e.getMessage());
        }
    }

    private void updateAlerts(int pendingCommandes, int lateDeliveries, int lowStock, int rupture) {
        alertsBox.getChildren().clear();

        if (pendingCommandes > 0) {
            addAlert("⚠️ " + pendingCommandes + " commandes en attente de traitement", "#f59e0b");
        }
        if (lateDeliveries > 0) {
            addAlert(" " + lateDeliveries + " livraisons en retard", "#ef4444");
        }
        if (lowStock > 0) {
            addAlert(" " + lowStock + " produits en stock faible", "#f59e0b");
        }
        if (rupture > 0) {
            addAlert(" " + rupture + " produits en rupture", "#ef4444");
        }
        if (pendingCommandes == 0 && lateDeliveries == 0 && lowStock == 0 && rupture == 0) {
            addAlert("✅ Tout est normal !", "#10b981");
        }
    }

    private void addAlert(String message, String color) {
        Label alert = new Label(message);
        alert.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13; -fx-font-weight: bold; -fx-padding: 10;");
        alertsBox.getChildren().add(alert);
    }

    @FXML
    public void goToCommandes() {
        navigate("/com/chroniccare/Admin/CommandesDashboard.fxml");
    }

    @FXML
    public void goToProduits() {
        navigate("/com/chroniccare/Admin/ProduitsDashboard.fxml");
    }

    @FXML
    public void goToLivraisons() {
        navigate("/com/chroniccare/Admin/LivraisonsDashboard.fxml");
    }

    @FXML
    public void goToLogin() {
        navigate("/com/chroniccare/login.fxml");
    }

    private void navigate(String fxml) {
        try {
            URL resource = getClass().getResource(fxml);
            if (resource == null) {
                showError("Fichier non trouvé", "Le fichier " + fxml + " n'existe pas");
                return;
            }
            Parent root = FXMLLoader.load(resource);
            alertsBox.getScene().setRoot(root);
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



