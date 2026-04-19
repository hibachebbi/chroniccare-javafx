package com.chroniccare.controllers;

import com.chroniccare.models.MedicalAuditEvent;
import com.chroniccare.models.User;
import com.chroniccare.services.MedicalAuditService;
import com.chroniccare.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MedicalAuditController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarRoleBadge;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;

    @FXML private TableView<MedicalAuditEvent> auditTable;
    @FXML private TableColumn<MedicalAuditEvent, Object> dateCol;
    @FXML private TableColumn<MedicalAuditEvent, String> actorCol;
    @FXML private TableColumn<MedicalAuditEvent, String> roleCol;
    @FXML private TableColumn<MedicalAuditEvent, String> actionCol;
    @FXML private TableColumn<MedicalAuditEvent, String> targetCol;
    @FXML private TableColumn<MedicalAuditEvent, String> detailsCol;

    @FXML private Label errorLabel;

    private final MedicalAuditService auditService = new MedicalAuditService();

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder à l'audit médical.");
            alert.show();
            Platform.runLater(this::goToHome);
            return;
        }

        String initials = getInitials(user);
        if (sidebarAvatar != null) sidebarAvatar.setText(initials);
        if (sidebarUserName != null) sidebarUserName.setText(user.getPrenom() + " " + user.getNom());
        if (sidebarRoleBadge != null) sidebarRoleBadge.setText("Administrateur");
        if (topbarAvatar != null) topbarAvatar.setText(initials);
        if (topbarUserName != null) topbarUserName.setText(user.getPrenom() + " " + user.getNom());
        if (topbarDate != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            topbarDate.setText(LocalDate.now().format(fmt));
        }

        if (auditTable != null) {
            auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        actorCol.setCellValueFactory(new PropertyValueFactory<>("actorName"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("actorRole"));
        actionCol.setCellValueFactory(new PropertyValueFactory<>("action"));
        targetCol.setCellValueFactory(new PropertyValueFactory<>("targetName"));
        detailsCol.setCellValueFactory(new PropertyValueFactory<>("details"));

        dateCol.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                if (item instanceof java.sql.Timestamp timestamp) {
                    setText(timestamp.toLocalDateTime().format(formatter));
                } else {
                    setText(item.toString());
                }
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        refresh();
    }

    @FXML
    public void refresh() {
        try {
            List<MedicalAuditEvent> events = auditService.getRecentEvents(300);
            auditTable.setItems(FXCollections.observableArrayList(events));
            if (errorLabel != null) errorLabel.setText("");
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void goToHome() {
        navigate("/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToUsers() {
        navigate("/com/chroniccare/list-users.fxml");
    }

    @FXML
    public void goToBlockedAccounts() {
        navigate("/com/chroniccare/blocked-accounts.fxml");
    }

    @FXML
    public void goToStats() {
        navigate("/com/chroniccare/stats.fxml");
    }

    @FXML
    public void goToMedicalAudit() {
    }

    @FXML
    public void goToProfile() {
        navigate("/com/chroniccare/list-users.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        navigate("/com/chroniccare/login.fxml");
    }

    private void navigate(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            auditTable.getScene().setRoot(root);
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private String getInitials(User user) {
        String p = (user.getPrenom() != null && !user.getPrenom().isEmpty())
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String n = (user.getNom() != null && !user.getNom().isEmpty())
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return p + n;
    }
}

