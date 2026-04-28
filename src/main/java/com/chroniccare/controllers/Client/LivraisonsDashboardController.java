package com.chroniccare.controllers.Client;

import com.chroniccare.entities.Livraison;
import com.chroniccare.entities.User;
import com.chroniccare.services.LivraisonService;
import com.chroniccare.services.OpenStreetMapService;
import com.chroniccare.utils.FxNavigator;
import com.chroniccare.utils.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class LivraisonsDashboardController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private Node root;

    @FXML
    private Label deliveriesCountLabel;
    @FXML
    private Label preparingDeliveriesLabel;
    @FXML
    private Label deliveredDeliveriesLabel;
    @FXML
    private Label lastRefreshLabel;

    @FXML
    private TableView<Livraison> deliveriesTable;
    @FXML
    private TableColumn<Livraison, Integer> idCol;
    @FXML
    private TableColumn<Livraison, Integer> commandeIdCol;
    @FXML
    private TableColumn<Livraison, String> statutCol;
    @FXML
    private TableColumn<Livraison, String> adresseCol;
    @FXML
    private TableColumn<Livraison, String> villeCol;
    @FXML
    private TableColumn<Livraison, String> trackingCol;
    @FXML
    private TableColumn<Livraison, LocalDateTime> datePrevueCol;
    @FXML
    private TableColumn<Livraison, Void> actionCol;

    private final ObservableList<Livraison> livraisons = FXCollections.observableArrayList();
    private final LivraisonService livraisonService = new LivraisonService();
    private final OpenStreetMapService osmService = new OpenStreetMapService();

    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        commandeIdCol.setCellValueFactory(new PropertyValueFactory<>("commandeId"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        adresseCol.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        villeCol.setCellValueFactory(new PropertyValueFactory<>("ville"));
        trackingCol.setCellValueFactory(new PropertyValueFactory<>("trackingCode"));
        datePrevueCol.setCellValueFactory(new PropertyValueFactory<>("datePrevue"));
        datePrevueCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "-" : item.format(DATE_FORMATTER));
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button mapButton = new Button("Voir carte");

            {
                mapButton.setStyle(
                        "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
                mapButton.setOnAction(e -> {
                    Livraison livraison = getTableView().getItems().get(getIndex());
                    openMap(livraison);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : mapButton);
            }
        });

        deliveriesTable.setItems(livraisons);
        refreshData();
        startRealtimeRefresh();
    }

    @FXML
    public void manualRefresh() {
        refreshData();
    }

    @FXML
    public void goToCommandes() {
        stopRealtimeRefresh();
        FxNavigator.go(anchor(), "/com/chroniccare/Client/CommandesDashboard.fxml");
    }

    private void refreshData() {
        try {
            User user = SessionManager.getInstance().getCurrentUser();
            if (user == null || user.getId() <= 0) {
                showError("Session invalide", "Connectez-vous pour voir vos livraisons.");
                return;
            }

            List<Livraison> result = livraisonService.findByUtilisateurId(user.getId());
            livraisons.setAll(result);

            long preparing = result.stream().filter(l -> "en_preparation".equalsIgnoreCase(l.getStatut())).count();
            long delivered = result.stream().filter(l -> "livree".equalsIgnoreCase(l.getStatut())).count();

            deliveriesCountLabel.setText(String.valueOf(result.size()));
            preparingDeliveriesLabel.setText(String.valueOf(preparing));
            deliveredDeliveriesLabel.setText(String.valueOf(delivered));

            if (lastRefreshLabel != null) {
                lastRefreshLabel.setText("Derniere mise a jour: " + LocalDateTime.now().format(DATE_FORMATTER));
            }
        } catch (Exception e) {
            showError("Chargement des livraisons echoue", e.getMessage());
        }
    }

    private void startRealtimeRefresh() {
        stopRealtimeRefresh();
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(8), event -> refreshData()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void stopRealtimeRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
    }

    private void openMap(Livraison livraison) {
        if (livraison == null) {
            return;
        }
        try {
            String url = osmService.buildMapUrl(livraison.getAdresse(), livraison.getVille(),
                    livraison.getCodePostal());
            showMapInApp(url, livraison);
        } catch (Exception e) {
            showError("Ouverture OpenStreetMap impossible", e.getMessage());
        }
    }

    private void showMapInApp(String url, Livraison livraison) {
        WebView webView = new WebView();
        webView.getEngine().load(url);

        Label title = new Label("Carte de livraison - Commande #" + livraison.getCommandeId());
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle(
                "-fx-background-color: #334155; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, title, spacer, closeBtn);
        topBar.setStyle("-fx-padding: 10; -fx-alignment: center-left; -fx-background-color: #f8fafc;");

        BorderPane rootPane = new BorderPane();
        rootPane.setTop(topBar);
        rootPane.setCenter(webView);

        Stage stage = new Stage();
        stage.setTitle("ChronicCare - Carte");
        stage.initModality(Modality.NONE);
        if (anchor() != null && anchor().getScene() != null && anchor().getScene().getWindow() != null) {
            stage.initOwner(anchor().getScene().getWindow());
        }
        stage.setScene(new javafx.scene.Scene(rootPane, 1100, 700));

        closeBtn.setOnAction(event -> stage.close());
        stage.show();
    }

    private Node anchor() {
        if (root != null) {
            return root;
        }
        return deliveriesTable;
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
