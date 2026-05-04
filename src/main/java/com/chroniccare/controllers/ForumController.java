package com.chroniccare.controllers;
import com.chroniccare.models.User;
import com.chroniccare.models.Publication;
import com.chroniccare.services.CommentaireService;
import com.chroniccare.services.PublicationService;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ForumController implements Initializable {

    @FXML private TableView<Publication>            tablePublications;
    @FXML private TableColumn<Publication, String>  colTitre;
    @FXML private TableColumn<Publication, String>  colCategorie;
    @FXML private TableColumn<Publication, String>  colStatut;
    @FXML private TableColumn<Publication, Integer> colLikes;
    @FXML private TableColumn<Publication, Integer> colVues;
    @FXML private TableColumn<Publication, String>  colDate;
    @FXML private TableColumn<Publication, Void>    colActions;
    @FXML private ComboBox<String>                  filtreCategorie;
    @FXML private Label statPublications;
    @FXML private Label statCommentaires;
    @FXML private Label statLikes;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarRoleBadge;
    @FXML private Label sidebarAvatar;
    @FXML private Button btnAjouter;

    private final PublicationService  pubService  = new PublicationService();
    private final CommentaireService  comService  = new CommentaireService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerSidebar();
        configurerColonnes();
        configurerColonneActions();
        chargerPublications();
        chargerFiltres();
        appliquerPermissions();
    }

    // ── Sidebar ──────────────────────────────────────────────
    private void configurerSidebar() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            sidebarUserName.setText(user.getNom() + " " + user.getPrenom());
            sidebarAvatar.setText(String.valueOf(
                    user.getNom().charAt(0)).toUpperCase());
            String role = user.getRoles();
            if (role.contains("ROLE_ADMIN"))          sidebarRoleBadge.setText("Administrateur");
            else if (role.contains("ROLE_COACH"))     sidebarRoleBadge.setText("Coach");
            else if (role.contains("ROLE_NUTRITIONNISTE")) sidebarRoleBadge.setText("Nutritionniste");
            else if (role.contains("ROLE_PATIENT"))   sidebarRoleBadge.setText("Patient");
        }
    }

    // ── Colonnes tableau ──────────────────────────────────────
    private void configurerColonnes() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colLikes.setCellValueFactory(new PropertyValueFactory<>("nbLikes"));
        colVues.setCellValueFactory(new PropertyValueFactory<>("nbVues"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    private void configurerColonneActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnVoir = new Button("👁");
            final Button btnEdit = new Button("✏️");
            final Button btnSupp = new Button("🗑️");

            {
                btnVoir.setStyle("-fx-background-color:#dbeafe; -fx-background-radius:6px; -fx-cursor:hand;");
                btnEdit.setStyle("-fx-background-color:#fef3c7; -fx-background-radius:6px; -fx-cursor:hand;");
                btnSupp.setStyle("-fx-background-color:#fee2e2; -fx-background-radius:6px; -fx-cursor:hand;");

                btnVoir.setOnAction(e -> {
                    Publication p = getTableView().getItems().get(getIndex());
                    ouvrirDetail(p);
                });

                btnEdit.setOnAction(e -> {
                    Publication p = getTableView().getItems().get(getIndex());
                    ouvrirFormulaire(p);
                });

                btnSupp.setOnAction(e -> {
                    Publication p = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Supprimer \"" + p.getTitre() + "\" ?",
                            ButtonType.YES, ButtonType.NO);
                    alert.setHeaderText(null);
                    alert.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.YES) {
                            pubService.supprimer(p.getId());
                            chargerPublications();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                Publication p = getTableView().getItems().get(getIndex());
                var session = SessionManager.getInstance();
                User currentUser = session.getCurrentUser();

                HBox box = new HBox(6);
                box.getChildren().add(btnVoir); // tout le monde peut voir

                // Admin → peut tout modifier/supprimer
                if (session.isAdmin()) {
                    box.getChildren().addAll(btnEdit, btnSupp);
                }
                // Coach ou Nutritionniste → peut modifier/supprimer SES publications
                else if (session.isCoach() || session.isNutritionniste()) {
                    if (currentUser != null && p.getAuteurId() != null
                            && p.getAuteurId() == currentUser.getId()) {
                        box.getChildren().addAll(btnEdit, btnSupp);
                    }
                }
                // Patient → peut seulement voir

                setGraphic(box);
            }
        });
    }

    // ── Charger données ───────────────────────────────────────
    private void chargerPublications() {
        List<Publication> liste = pubService.getAll();
        tablePublications.setItems(FXCollections.observableArrayList(liste));

        statPublications.setText(String.valueOf(liste.size()));
        int totalLikes = liste.stream().mapToInt(Publication::getNbLikes).sum();
        statLikes.setText(String.valueOf(totalLikes));

        // Total commentaires
        int totalComm = liste.stream()
                .mapToInt(p -> comService.getByPublication(p.getId()).size())
                .sum();
        statCommentaires.setText(String.valueOf(totalComm));
    }

    private void chargerFiltres() {
        ObservableList<String> cats = FXCollections.observableArrayList("Toutes");
        pubService.getAll().stream()
                .map(Publication::getCategorie)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .forEach(cats::add);
        filtreCategorie.setItems(cats);
        filtreCategorie.setValue("Toutes");
    }

    // ── Actions ───────────────────────────────────────────────
    @FXML
    private void filtrer() {
        String cat = filtreCategorie.getValue();
        if (cat == null || cat.equals("Toutes")) {
            chargerPublications();
            return;
        }
        List<Publication> filtre = pubService.getAll().stream()
                .filter(p -> cat.equals(p.getCategorie()))
                .toList();
        tablePublications.setItems(FXCollections.observableArrayList(filtre));
    }

    @FXML
    private void ouvrirAjout() {
        ouvrirFormulaire(null);
    }

    private void ouvrirFormulaire(Publication publication) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/chroniccare/ajouter-publication.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(publication == null
                    ? "Nouvelle publication" : "Modifier publication");
            stage.setScene(new Scene(loader.load()));
            AjouterPublicationController ctrl = loader.getController();
            if (publication != null) ctrl.setPublication(publication);
            stage.showAndWait();
            chargerPublications();
            chargerFiltres();
        } catch (Exception e) {
            System.err.println("Erreur formulaire : " + e.getMessage());
        }
    }

    private void ouvrirDetail(Publication publication) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/chroniccare/detail-publication.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détail publication");
            stage.setScene(new Scene(loader.load()));
            DetailPublicationController ctrl = loader.getController();
            ctrl.setPublication(publication);
            stage.showAndWait();
            chargerPublications();
        } catch (Exception e) {
            System.err.println("Erreur détail : " + e.getMessage());
        }
    }
    private void appliquerPermissions() {
        var session = SessionManager.getInstance();

        if (session.isPatient()) {
            btnAjouter.setVisible(false);
            btnAjouter.setManaged(false);
        }
    }
}