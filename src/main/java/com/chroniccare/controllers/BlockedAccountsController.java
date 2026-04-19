package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.SecurityService;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;

public class BlockedAccountsController {

    @FXML private TableView<User> blockedTable;
    @FXML private TableColumn<User, Integer> idCol;
    @FXML private TableColumn<User, String> nomCol;
    @FXML private TableColumn<User, String> prenomCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> reasonCol;
    @FXML private TableColumn<User, Object> blockedUntilCol;
    @FXML private TableColumn<User, String> modeCol;
    @FXML private TableColumn<User, Void> actionsCol;

    private final UserService userService = new UserService();
    private final SecurityService securityService = new SecurityService();

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder aux comptes bloqués.");
            alert.show();
            Platform.runLater(this::goToHome);
            return;
        }

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomCol.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("securityBlockReason"));
        blockedUntilCol.setCellValueFactory(new PropertyValueFactory<>("securityBlockedUntil"));

        blockedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        modeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                User user = getTableView().getItems().get(getIndex());

                Label badge = new Label(user.isSecurityManualLocked() ? "MANUEL" : "AUTO");
                badge.getStyleClass().add(user.isSecurityManualLocked() ? "badge-manuel" : "badge-auto");

                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        blockedUntilCol.setCellFactory(col -> new TableCell<>() {
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

                setAlignment(Pos.CENTER);
            }
        });

        reasonCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }

                String text = item.length() > 45 ? item.substring(0, 45) + "..." : item;
                setText(text);
                setTooltip(new Tooltip(item));
            }
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnUnlock = new Button("Débloquer");
            private final Button btnKeep = new Button("Laisser bloqué");
            private final HBox box = new HBox(10);

            {
                btnUnlock.getStyleClass().add("btn-unlock");
                btnKeep.getStyleClass().add("btn-keep-blocked");
                box.setAlignment(Pos.CENTER);

                btnUnlock.setOnAction(e -> {
                    User selected = getTableView().getItems().get(getIndex());
                    try {
                        User admin = SessionManager.getInstance().getCurrentUser();
                        securityService.adminUnlockUser(selected.getId(), admin.getId());
                        loadBlockedUsers();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                btnKeep.setOnAction(e -> {
                    User selected = getTableView().getItems().get(getIndex());

                    TextInputDialog dialog = new TextInputDialog("Compte maintenu bloqué après vérification admin.");
                    dialog.setTitle("Motif admin");
                    dialog.setHeaderText("Donner la cause du maintien du blocage");
                    dialog.setContentText("Motif :");

                    dialog.showAndWait().ifPresent(reason -> {
                        try {
                            User admin = SessionManager.getInstance().getCurrentUser();
                            securityService.adminKeepBlocked(selected.getId(), admin.getId(), reason);
                            loadBlockedUsers();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                User user = getTableView().getItems().get(getIndex());
                box.getChildren().clear();

                if (user.isSecurityManualLocked()) {
                    box.getChildren().add(btnUnlock);
                } else {
                    box.getChildren().addAll(btnUnlock, btnKeep);
                }

                setGraphic(box);
                setAlignment(Pos.CENTER);
            }
        });

        loadBlockedUsers();
    }

    private void loadBlockedUsers() {
        try {
            blockedTable.setItems(FXCollections.observableArrayList(userService.getBlockedUsers()));
        } catch (Exception e) {
            e.printStackTrace();
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
    public void goToStats() {
        navigate("/com/chroniccare/stats.fxml");
    }

    @FXML
    public void goToMedicalAudit() {
        navigate("/com/chroniccare/medical-audit.fxml");
    }

    @FXML
    public void goToBlockedAccounts() {
    }

    @FXML
    public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            blockedTable.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            blockedTable.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
