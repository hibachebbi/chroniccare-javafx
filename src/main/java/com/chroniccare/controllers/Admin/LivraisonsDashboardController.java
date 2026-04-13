package com.chroniccare.controllers.Admin;

import com.chroniccare.entities.Livraison;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import com.chroniccare.services.LivraisonService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class LivraisonsDashboardController {

    @FXML private Label deliveriesCountLabel;
    @FXML private Label preparingDeliveriesLabel;
    @FXML private Label deliveredDeliveriesLabel;
    @FXML private Label lateDeliveriesLabel;

    @FXML private TextField idField;
    @FXML private TextField commandeIdField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField adresseField;
    @FXML private TextField villeField;
    @FXML private TextField codePostalField;
    @FXML private DatePicker datePrevuePicker;
    @FXML private TextField trackingField;

    @FXML private TableView<Livraison> deliveriesTable;
    @FXML private TableColumn<Livraison, Integer> idCol;
    @FXML private TableColumn<Livraison, Integer> commandeIdCol;
    @FXML private TableColumn<Livraison, String> statutCol;
    @FXML private TableColumn<Livraison, String> adresseCol;
    @FXML private TableColumn<Livraison, String> villeCol;
    @FXML private TableColumn<Livraison, String> trackingCol;
    @FXML private TableColumn<Livraison, LocalDateTime> datePrevueCol;

    private final ObservableList<Livraison> livraisons = FXCollections.observableArrayList();
    private final LivraisonService service = new LivraisonService();

    @FXML
    public void initialize() {
        statutCombo.setItems(FXCollections.observableArrayList("en_preparation", "en_transit", "livree", "annulee"));

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        commandeIdCol.setCellValueFactory(new PropertyValueFactory<>("commandeId"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        adresseCol.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        villeCol.setCellValueFactory(new PropertyValueFactory<>("ville"));
        trackingCol.setCellValueFactory(new PropertyValueFactory<>("trackingCode"));
        datePrevueCol.setCellValueFactory(new PropertyValueFactory<>("datePrevue"));

        deliveriesTable.setItems(livraisons);
        deliveriesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                fillForm(newValue);
            }
        });
        refreshData();
    }

    @FXML
    public void saveLivraison() {
        try {
            Livraison livraison = readForm();
            service.save(livraison);
            refreshData();
            clearForm();
            info("Livraison ajoutee avec succes");
        } catch (Exception e) {
            error("Ajout livraison echoue", e.getMessage());
        }
    }

    @FXML
    public void updateLivraison() {
        try {
            Livraison livraison = readForm();
            if (livraison.getId() <= 0) {
                throw new IllegalArgumentException("Selectionnez une livraison a modifier");
            }
            service.update(livraison);
            refreshData();
            info("Livraison modifiee avec succes");
        } catch (Exception e) {
            error("Modification livraison echouee", e.getMessage());
        }
    }

    @FXML
    public void deleteLivraison() {
        try {
            int id = parseInt(idField.getText(), "ID livraison invalide");
            service.delete(id);
            refreshData();
            clearForm();
            info("Livraison supprimee avec succes");
        } catch (Exception e) {
            error("Suppression livraison echouee", e.getMessage());
        }
    }

    @FXML
    public void clearForm() {
        idField.clear();
        commandeIdField.clear();
        statutCombo.getSelectionModel().clearSelection();
        adresseField.clear();
        villeField.clear();
        codePostalField.clear();
        datePrevuePicker.setValue(null);
        trackingField.clear();
        deliveriesTable.getSelectionModel().clearSelection();
    }

    private void refreshData() {
        try {
            livraisons.setAll(service.findAll());
            deliveriesCountLabel.setText(String.valueOf(service.countAll()));
            preparingDeliveriesLabel.setText(String.valueOf(service.countByStatut("en_preparation")));
            deliveredDeliveriesLabel.setText(String.valueOf(service.countByStatut("livree")));
            lateDeliveriesLabel.setText(String.valueOf(service.countLate()));
        } catch (SQLException e) {
            error("Chargement des livraisons echoue", e.getMessage());
        }
    }

    private Livraison readForm() {
        Livraison livraison = new Livraison();
        if (!idField.getText().isBlank()) {
            livraison.setId(parseInt(idField.getText(), "ID livraison invalide"));
        }
        livraison.setCommandeId(parseInt(commandeIdField.getText(), "Commande invalide"));
        livraison.setStatut(requireText(statutCombo.getValue(), "Statut obligatoire"));
        livraison.setAdresse(requireText(adresseField.getText(), "Adresse obligatoire"));
        livraison.setVille(requireText(villeField.getText(), "Ville obligatoire"));
        livraison.setCodePostal(codePostalField.getText() == null ? null : codePostalField.getText().trim());
        livraison.setTrackingCode(trackingField.getText() == null ? null : trackingField.getText().trim());
        if (datePrevuePicker.getValue() != null) {
            livraison.setDatePrevue(datePrevuePicker.getValue().atStartOfDay());
        }
        livraison.setUpdatedAt(LocalDateTime.now());
        return livraison;
    }

    private void fillForm(Livraison livraison) {
        idField.setText(String.valueOf(livraison.getId()));
        commandeIdField.setText(String.valueOf(livraison.getCommandeId()));
        statutCombo.setValue(livraison.getStatut());
        adresseField.setText(livraison.getAdresse());
        villeField.setText(livraison.getVille());
        codePostalField.setText(livraison.getCodePostal());
        trackingField.setText(livraison.getTrackingCode());
        if (livraison.getDatePrevue() != null) {
            datePrevuePicker.setValue(livraison.getDatePrevue().toLocalDate());
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private int parseInt(String value, String message) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(message);
        }
    }

    private void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void error(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
