package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import com.chroniccare.utils.SessionManager;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;



public class ListUsersController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idCol;
    @FXML private TableColumn<User, String> nomCol;
    @FXML private TableColumn<User, String> prenomCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> rolesCol;
    @FXML private TableColumn<User, String> genreCol;
    @FXML private TableColumn<User, String> statusCol;
    @FXML private TableColumn<User, Void> actionsCol;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private Label pageLabel;
    @FXML private Label countLabel;
    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarRoleBadge;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;

    private UserService userService = new UserService();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private ObservableList<User> filteredUsers = FXCollections.observableArrayList();

    private int currentPage = 1;
    private final int PAGE_SIZE = 5;

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder à cette page.");
            alert.show();
            Platform.runLater(this::goToHome);
            return;
        }

        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            String initials = getInitials(currentUser);

            sidebarAvatar.setText(initials);
            sidebarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            sidebarRoleBadge.setText(getRoleLabel(currentUser));

            topbarAvatar.setText(initials);
            topbarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            topbarDate.setText(LocalDate.now().format(fmt));
        }
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomCol.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        rolesCol.setCellValueFactory(new PropertyValueFactory<>("roles"));
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("approvalStatus"));


        rolesCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label roleBadge = new Label(formatRole(item));
                roleBadge.setStyle(getRoleBadgeStyle(item));
                setGraphic(roleBadge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label statusBadge = new Label(formatStatus(item));
                statusBadge.setStyle(getStatusBadgeStyle(item));
                setGraphic(statusBadge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        // Tri automatique en cliquant sur les colonnes
        nomCol.setSortable(true);
        prenomCol.setSortable(true);
        emailCol.setSortable(true);
        rolesCol.setSortable(true);
        statusCol.setSortable(true);

        // Filtre par rôle
        roleFilter.setItems(FXCollections.observableArrayList(
                "Tous", "ROLE_ADMIN", "ROLE_PATIENT",
                "ROLE_COACH", "ROLE_NUTRITIONNISTE"
        ));
        roleFilter.setValue("Tous");

        // Colonne Actions
        actionsCol.setCellFactory(col -> new TableCell<>() {

            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("🗑");
            private final Button btnBadge = new Button("🏆");
            private final HBox box = new HBox(8, btnEdit, btnDelete, btnBadge);

            {
                btnBadge.setStyle(
                        "-fx-background-color: #f59e0b; -fx-text-fill: white;" +
                                "-fx-cursor: hand; -fx-background-radius: 8; " +
                                "-fx-font-size: 12; -fx-font-weight: bold;"
                );

                btnEdit.setStyle(
                        "-fx-background-color: #2563eb; -fx-text-fill: white;" +
                                "-fx-cursor: hand; -fx-background-radius: 8; " +
                                "-fx-font-size: 12; -fx-font-weight: bold;"
                );

                btnDelete.setStyle(
                        "-fx-background-color: #ef4444; -fx-text-fill: white;" +
                                "-fx-cursor: hand; -fx-background-radius: 8; " +
                                "-fx-font-size: 12; -fx-font-weight: bold;"
                );

                btnBadge.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleAssignBadge(user);
                });

                btnEdit.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    goToEdit(user);
                });

                btnDelete.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDelete(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        loadUsers();
    }

    private void handleAssignBadge(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Attribution du badge");
        confirm.setContentText("Attribuer le badge à " + user.getNom() + " " + user.getPrenom() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.assignBadgeManually(user.getId());
                    loadUsers();

                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Succès");
                    info.setContentText("Badge attribué avec succès.");
                    info.showAndWait();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void loadUsers() {
        try {
            List<User> users = userService.getAll();
            allUsers.setAll(users);
            filteredUsers.setAll(users);
            currentPage = 1;
            updateTable();
        } catch (Exception e) {
            System.err.println("Erreur chargement : " + e.getMessage());
        }
    }

    private void updateTable() {
        int totalPages = (int) Math.ceil((double) filteredUsers.size() / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filteredUsers.size());

        if (fromIndex > filteredUsers.size()) {
            currentPage = 1;
            fromIndex = 0;
            toIndex = Math.min(PAGE_SIZE, filteredUsers.size());
        }

        ObservableList<User> pageData = FXCollections.observableArrayList(
                filteredUsers.subList(fromIndex, toIndex)
        );

        usersTable.setItems(pageData);
        pageLabel.setText("Page " + currentPage + " / " + totalPages);
        countLabel.setText("Total : " + filteredUsers.size() + " utilisateur(s)");
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        String role = roleFilter.getValue();

        List<User> result = allUsers.stream()
                .filter(u -> {
                    boolean matchKeyword = keyword.isEmpty() ||
                            u.getNom().toLowerCase().contains(keyword) ||
                            u.getPrenom().toLowerCase().contains(keyword) ||
                            u.getEmail().toLowerCase().contains(keyword);

                    boolean matchRole = role == null ||
                            role.equals("Tous") ||
                            u.getRoles().contains(role);

                    return matchKeyword && matchRole;
                })
                .collect(Collectors.toList());

        filteredUsers.setAll(result);
        currentPage = 1;
        updateTable();
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        roleFilter.setValue("Tous");
        filteredUsers.setAll(allUsers);
        currentPage = 1;
        updateTable();
    }

    @FXML
    public void nextPage() {
        int totalPages = (int) Math.ceil((double) filteredUsers.size() / PAGE_SIZE);
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

    private void handleDelete(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Voulez-vous supprimer " +
                user.getNom() + " " + user.getPrenom() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.delete(user.getId());
                    loadUsers();
                } catch (Exception e) {
                    System.err.println("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    private void goToEdit(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/chroniccare/edit-user.fxml"));
            Parent root = loader.load();
            EditUserController controller = loader.getController();
            controller.setUser(user);
            usersTable.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Erreur navigation : " + e.getMessage());
        }
    }

    @FXML
    public void goToAdd() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/chroniccare/add-user.fxml"));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Erreur navigation : " + e.getMessage());
        }
    }
    @FXML
    public void goToHome() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/chroniccare/home.fxml"));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Erreur navigation : " + e.getMessage());
        }
    }
    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/chroniccare/login.fxml"));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Erreur navigation : " + e.getMessage());
        }
    }

    @FXML
    public void goToProfile() {
        try {
            String fxml;
            if (SessionManager.getInstance().isPatient()) {
                fxml = "/com/chroniccare/profile-patient.fxml";
            } else if (SessionManager.getInstance().isCoach()) {
                fxml = "/com/chroniccare/profile-coach.fxml";
            } else if (SessionManager.getInstance().isNutritionniste()) {
                fxml = "/com/chroniccare/profile-nutritionniste.fxml";
            } else if (SessionManager.getInstance().isAdmin()) {
                fxml = "/com/chroniccare/list-users.fxml";
            } else {
                fxml = "/com/chroniccare/profile-patient.fxml";
            }

            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToStats() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/stats.fxml"));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void goToBlockedAccounts() {
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder aux comptes bloqués.");
            alert.showAndWait();
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/blocked-accounts.fxml"));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToMedicalAudit() {
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder à l'audit médical.");
            alert.showAndWait();
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/medical-audit.fxml"));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToPatientSegmentation() {
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder à la segmentation patients.");
            alert.showAndWait();
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/patient-segmentation.fxml"));
            usersTable.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatRole(String role) {
        if (role == null) return "Utilisateur";
        if (role.contains("ROLE_ADMIN")) return "Admin";
        if (role.contains("ROLE_PATIENT")) return "Patient";
        if (role.contains("ROLE_COACH")) return "Coach";
        if (role.contains("ROLE_NUTRITIONNISTE")) return "Nutritionniste";
        return "Utilisateur";
    }

    private String getRoleBadgeStyle(String role) {
        String base = "-fx-padding: 6 12; -fx-background-radius: 14; -fx-font-size: 12; -fx-font-weight: bold;";

        if (role.contains("ROLE_ADMIN")) {
            return base + "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8;";
        }
        if (role.contains("ROLE_PATIENT")) {
            return base + "-fx-background-color: #dcfce7; -fx-text-fill: #15803d;";
        }
        if (role.contains("ROLE_COACH")) {
            return base + "-fx-background-color: #ede9fe; -fx-text-fill: #6d28d9;";
        }
        if (role.contains("ROLE_NUTRITIONNISTE")) {
            return base + "-fx-background-color: #fef3c7; -fx-text-fill: #b45309;";
        }

        return base + "-fx-background-color: #e5e7eb; -fx-text-fill: #374151;";
    }

    private String formatStatus(String status) {
        if (status == null) return "Inconnu";
        if (status.equalsIgnoreCase("approved")) return "Approuvé";
        if (status.equalsIgnoreCase("pending")) return "En attente";
        if (status.equalsIgnoreCase("rejected")) return "Refusé";
        return status;
    }

    private String getStatusBadgeStyle(String status) {
        String base = "-fx-padding: 6 12; -fx-background-radius: 14; -fx-font-size: 12; -fx-font-weight: bold;";

        if (status.equalsIgnoreCase("approved")) {
            return base + "-fx-background-color: #dcfce7; -fx-text-fill: #15803d;";
        }
        if (status.equalsIgnoreCase("pending")) {
            return base + "-fx-background-color: #fef3c7; -fx-text-fill: #b45309;";
        }
        if (status.equalsIgnoreCase("rejected")) {
            return base + "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c;";
        }

        return base + "-fx-background-color: #e5e7eb; -fx-text-fill: #374151;";
    }

    private String getInitials(User user) {
        String p = (user.getPrenom() != null && !user.getPrenom().isEmpty())
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String n = (user.getNom() != null && !user.getNom().isEmpty())
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return p + n;
    }

    private String getRoleLabel(User user) {
        if (user.getRoles() == null) return "Utilisateur";
        if (user.getRoles().contains("ROLE_ADMIN")) return "Administrateur";
        if (user.getRoles().contains("ROLE_NUTRITIONNISTE")) return "Nutritionniste";
        if (user.getRoles().contains("ROLE_COACH")) return "Coach";
        if (user.getRoles().contains("ROLE_PATIENT")) return "Patient";
        return "Utilisateur";
    }
}
