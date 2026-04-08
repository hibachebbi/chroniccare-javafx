package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.stream.Collectors;

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

    private UserService userService = new UserService();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private ObservableList<User> filteredUsers = FXCollections.observableArrayList();

    private int currentPage = 1;
    private final int PAGE_SIZE = 5;

    @FXML
    public void initialize() {
        // Colonnes
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomCol.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        rolesCol.setCellValueFactory(new PropertyValueFactory<>("roles"));
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("approvalStatus"));

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
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox box = new HBox(5, btnEdit, btnDelete);

            {
                btnEdit.setStyle(
                        "-fx-background-color: #2563eb; -fx-text-fill: white;" +
                                "-fx-cursor: hand; -fx-background-radius: 5;");
                btnDelete.setStyle(
                        "-fx-background-color: #ef4444; -fx-text-fill: white;" +
                                "-fx-cursor: hand; -fx-background-radius: 5;");

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
}