package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.SecurityService;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import com.chroniccare.utils.SidebarNavHighlight;
import com.chroniccare.utils.SidebarRoleBadgeHelper;
import com.chroniccare.utils.FxNavigation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class BlockedAccountsController {

    @FXML private VBox sidebarRoot;
    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarRoleBadge;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;

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

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            String initials = "";
            if (currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty()) {
                initials += String.valueOf(currentUser.getPrenom().charAt(0)).toUpperCase(Locale.ROOT);
            }
            if (currentUser.getNom() != null && !currentUser.getNom().isEmpty()) {
                initials += String.valueOf(currentUser.getNom().charAt(0)).toUpperCase(Locale.ROOT);
            }
            if (sidebarAvatar != null) sidebarAvatar.setText(initials);
            if (sidebarUserName != null) {
                sidebarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            }
            if (sidebarRoleBadge != null) {
                sidebarRoleBadge.setText("Administrateur");
                SidebarRoleBadgeHelper.applyRoleStyle(sidebarRoleBadge, currentUser);
            }
            if (topbarAvatar != null) topbarAvatar.setText(initials);
            if (topbarUserName != null) {
                topbarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            }
            if (topbarDate != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
                topbarDate.setText(LocalDate.now().format(fmt));
            }
        }

        if (sidebarRoot != null) {
            SidebarNavHighlight.activate(sidebarRoot, "adm:blocked");
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

                Label badge;
                if (user.isSecurityManualLocked()) {
                    badge = new Label("MANUEL");
                    badge.getStyleClass().add("badge-manuel");
                } else if (user.getSecurityBlockedUntil() != null
                        && user.getSecurityBlockedUntil().toLocalDateTime().isAfter(LocalDateTime.now())) {
                    badge = new Label("AUTO");
                    badge.getStyleClass().add("badge-auto");
                } else if (user.getSecurityBlockedUntil() != null) {
                    badge = new Label("EXPIRE");
                    badge.getStyleClass().add("badge-auto");
                } else if (!user.isActive()) {
                    badge = new Label("DESACTIVE");
                    badge.getStyleClass().add("badge-manuel");
                } else if ("rejected".equalsIgnoreCase(user.getApprovalStatus())) {
                    badge = new Label("REFUSE");
                    badge.getStyleClass().add("badge-auto");
                } else {
                    badge = new Label("BLOQUE");
                    badge.getStyleClass().add("badge-auto");
                }

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
                    if (!empty && getIndex() < getTableView().getItems().size()) {
                        User user = getTableView().getItems().get(getIndex());
                        if (user != null && user.isSecurityManualLocked()) {
                            setText("Jusqu'a deblocage admin");
                            setAlignment(Pos.CENTER);
                            return;
                        }
                        if (user != null && !user.isActive()) {
                            setText("Compte desactive");
                            setAlignment(Pos.CENTER);
                            return;
                        }
                        if (user != null && "rejected".equalsIgnoreCase(user.getApprovalStatus())) {
                            setText("Acces refuse");
                            setAlignment(Pos.CENTER);
                            return;
                        }
                    }
                    setText(null);
                    return;
                }

                if (item instanceof java.sql.Timestamp timestamp) {
                    String formatted = timestamp.toLocalDateTime().format(formatter);
                    if (timestamp.toLocalDateTime().isBefore(LocalDateTime.now())) {
                        setText(formatted + " (expire)");
                    } else {
                        setText(formatted);
                    }
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
                    if (!empty && getIndex() < getTableView().getItems().size()) {
                        User user = getTableView().getItems().get(getIndex());
                        String fallback = buildFallbackReason(user);
                        if (fallback != null) {
                            setText(fallback);
                            setTooltip(new Tooltip(fallback));
                            return;
                        }
                    }
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

    private String buildFallbackReason(User user) {
        if (user == null) return null;
        if (user.getSecurityBlockReason() != null && !user.getSecurityBlockReason().isBlank()) {
            return user.getSecurityBlockReason();
        }
        if (!user.isActive()) {
            return "Compte desactive.";
        }
        if ("rejected".equalsIgnoreCase(user.getApprovalStatus())) {
            return "Compte refuse par l'administration.";
        }
        if (user.isSecurityManualLocked()) {
            return "Compte bloque manuellement par un administrateur.";
        }
        if (user.getSecurityBlockedUntil() != null) {
            if (user.getSecurityBlockedUntil().toLocalDateTime().isBefore(LocalDateTime.now())) {
                return "Blocage automatique expire, compte a verifier ou debloquer.";
            }
            return "Compte temporairement bloque pour raison de securite.";
        }
        return null;
    }

    @FXML
    public void goToHome() {
        FxNavigation.navigateAdmin(getClass(), blockedTable, "home");
    }

    @FXML
    public void goToUsers() {
        FxNavigation.navigateAdmin(getClass(), blockedTable, "users");
    }

    @FXML
    public void goToStats() {
        FxNavigation.navigateAdmin(getClass(), blockedTable, "stats");
    }

    @FXML
    public void goToMedicalAudit() {
        FxNavigation.navigateAdmin(getClass(), blockedTable, "audit");
    }

    @FXML
    public void goToPatientSegmentation() {
        FxNavigation.navigateAdmin(getClass(), blockedTable, "segment");
    }

    @FXML
    public void goToBlockedAccounts() {
        FxNavigation.navigateAdmin(getClass(), blockedTable, "blocked");
    }

    @FXML
    public void goToProfile() {
        FxNavigation.navigateAdmin(getClass(), blockedTable, "profile");
    }

    @FXML
    public void goToAdminConsultLedger() {
        FxNavigation.navigateAdmin(getClass(), blockedTable, "consult");
    }

    @FXML
    public void goToAdminNutritionRdv() {
        FxNavigation.navigateAdmin(getClass(), blockedTable, "rdv");
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
