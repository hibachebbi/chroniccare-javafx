package com.chroniccare.controllers;

import com.chroniccare.models.Event;
import com.chroniccare.models.Exercise;
import com.chroniccare.models.User;
import com.chroniccare.services.EventService;
import com.chroniccare.services.ExerciseService;
import com.chroniccare.utils.SessionManager;
import com.chroniccare.utils.FxNavigation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PatientRegistrationsController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private TextField searchField;
    @FXML private Label kpiRegistrationsLabel;
    @FXML private Label kpiUpcomingLabel;
    @FXML private Label kpiExercisesLabel;
    @FXML private Button filterAllBtn;
    @FXML private Button filterUpcomingBtn;
    @FXML private Button filterTodayBtn;
    @FXML private TableView<Event> registrationsTable;
    @FXML private TableColumn<Event, String> titreCol;
    @FXML private TableColumn<Event, String> lieuCol;
    @FXML private TableColumn<Event, String> dateDebutCol;
    @FXML private TableColumn<Event, String> dateFinCol;
    @FXML private TableColumn<Event, String> statutCol;
    @FXML private Button cancelRegistrationButton;
    @FXML private Label detailsLabel;
    @FXML private TableView<Exercise> exercisesTable;
    @FXML private TableColumn<Exercise, String> exNomCol;
    @FXML private TableColumn<Exercise, Integer> exDureeCol;
    @FXML private TableColumn<Exercise, String> exRepCol;
    @FXML private Label countLabel;
    @FXML private VBox emptyStateBox;
    @FXML private Label messageLabel;

    private final EventService eventService = new EventService();
    private final ExerciseService exerciseService = new ExerciseService();
    private final ObservableList<Event> allRegistrations = FXCollections.observableArrayList();
    private final ObservableList<Event> displayedRegistrations = FXCollections.observableArrayList();
    private final ObservableList<Exercise> exercises = FXCollections.observableArrayList();
    private User currentUser;
    private String activeQuickFilter = "all";
    private Event selectedRegistration;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        setupHeader();
        setupTables();
        addQRCodeColumn();
        loadRegistrations();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        registrationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> onRegistrationSelected(newValue));
        setActiveQuickFilter("all");
    }

    private void setupHeader() {
        if (currentUser == null) {
            return;
        }
        String initials = getInitials(currentUser);
        if (sidebarAvatar != null) sidebarAvatar.setText(initials);
        if (sidebarUserName != null) sidebarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (topbarAvatar != null) topbarAvatar.setText(initials);
        if (topbarUserName != null) topbarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        if (topbarDate != null) topbarDate.setText(LocalDate.now().format(fmt));
    }

    private void setupTables() {
        titreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        lieuCol.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        dateDebutCol.setCellValueFactory(new PropertyValueFactory<>("dateDebutDisplay"));
        dateFinCol.setCellValueFactory(new PropertyValueFactory<>("dateFinDisplay"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        statutCol.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(toStatusLabel(status));
                badge.getStyleClass().setAll("status-chip", getStatusClass(status));
                setGraphic(badge);
            }
        });
        registrationsTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(Event event, boolean empty) {
                super.updateItem(event, empty);
                getStyleClass().removeAll("row-status-pending", "row-status-valid", "row-status-cancelled");
                if (empty || event == null) {
                    return;
                }
                getStyleClass().add(getRowClass(event.getStatut()));
            }
        });
        registrationsTable.setItems(displayedRegistrations);
        if (cancelRegistrationButton != null) {
            cancelRegistrationButton.setDisable(true);
        }

        exNomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        exDureeCol.setCellValueFactory(new PropertyValueFactory<>("duree"));
        exRepCol.setCellValueFactory(new PropertyValueFactory<>("repetitionsDisplay"));
        exercisesTable.setItems(exercises);
    }

    private void addQRCodeColumn() {
        TableColumn<Event, Void> qrCol = new TableColumn<>("QR Code");
        qrCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Voir QR");
            {
                btn.setStyle("-fx-padding: 5px 10px; -fx-font-size: 11px;");
                btn.setOnAction(event -> {
                    Event registration = getTableView().getItems().get(getIndex());
                    if (registration != null) {
                        showQRCodeModal(registration);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        registrationsTable.getColumns().add(qrCol);
    }

    private void showQRCodeModal(Event event) {
        try {
            var registration = eventService.getPatientRegistrationForEvent(event.getId(), currentUser.getEmail());
            String qrPath = registration != null ? registration.getQrCodePath() : null;
            String qrToken = registration != null ? registration.getQrCodeToken() : null;

            if (qrPath == null || qrToken == null) {
                messageLabel.setText("QR Code non trouvé pour cette inscription.");
                return;
            }

            Stage modal = new Stage();
            modal.setTitle("Code QR - " + event.getTitre());
            
            File qrFile = new File(qrPath);
            if (!qrFile.exists()) {
                messageLabel.setText("Fichier QR Code non trouvé: " + qrPath);
                return;
            }

            Image qrImage = new Image(qrFile.toURI().toString());
            ImageView imageView = new ImageView(qrImage);
            imageView.setFitWidth(400);
            imageView.setFitHeight(400);
            imageView.setPreserveRatio(true);

            Label titleLabel = new Label(event.getTitre());
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10px;");

            VBox vbox = new VBox(10);
            vbox.setStyle("-fx-padding: 20px; -fx-alignment: center;");
            vbox.getChildren().addAll(titleLabel, imageView);

            Scene scene = new Scene(vbox, 500, 550);
            modal.setScene(scene);
            modal.show();
        } catch (Exception e) {
            messageLabel.setText("Erreur affichage QR Code: " + e.getMessage());
        }
    }

    private void loadRegistrations() {
        if (currentUser == null) {
            return;
        }
        try {
            allRegistrations.setAll(eventService.getRegisteredEventsByPatientEmail(currentUser.getEmail()));
            refreshKpis();
            applyFilters();
            messageLabel.setText(allRegistrations.isEmpty()
                    ? "Aucune inscription pour le moment."
                    : "");
        } catch (Exception e) {
            messageLabel.setText("Erreur chargement inscriptions : " + e.getMessage());
        }
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        LocalDate today = LocalDate.now();
        List<Event> filtered = allRegistrations.stream()
                .filter(event -> {
                    LocalDate eventDate = event.getDateDebut() != null ? event.getDateDebut().toLocalDate() : null;
                    return switch (activeQuickFilter) {
                        case "today" -> eventDate != null && eventDate.equals(today);
                        case "upcoming" -> eventDate != null && !eventDate.isBefore(today);
                        default -> true;
                    };
                })
                .filter(event ->
                        contains(event.getTitre(), query) ||
                        contains(event.getLieu(), query) ||
                        contains(event.getStatut(), query))
                .collect(Collectors.toList());

        displayedRegistrations.setAll(filtered);
        countLabel.setText(filtered.size() + " inscription(s) affichee(s)");
        boolean isEmpty = filtered.isEmpty();
        emptyStateBox.setVisible(isEmpty);
        emptyStateBox.setManaged(isEmpty);
    }

    private void onRegistrationSelected(Event event) {
        selectedRegistration = event;
        exercises.clear();
        if (event == null) {
            detailsLabel.setText("Selectionnez une inscription pour voir les exercices associes.");
            if (cancelRegistrationButton != null) {
                cancelRegistrationButton.setDisable(true);
            }
            return;
        }
        detailsLabel.setText("Exercices de : " + event.getTitre());
        if (cancelRegistrationButton != null) {
            cancelRegistrationButton.setDisable(!eventService.canCancelRegistration(event));
        }
        try {
            exercises.setAll(exerciseService.getByEventIdPublic(event.getId()));
        } catch (Exception e) {
            messageLabel.setText("Erreur chargement exercices : " + e.getMessage());
        }
    }

    @FXML public void goToEvents() { FxNavigation.navigatePatient(getClass(), registrationsTable, "events"); }
    @FXML public void goToPatientEvents() { FxNavigation.navigatePatient(getClass(), registrationsTable, "events"); }
    @FXML public void goToPatientRegistrations() { FxNavigation.navigatePatient(getClass(), registrationsTable, "registrations"); }
    @FXML public void goToPatientBookRdv() { FxNavigation.navigatePatient(getClass(), registrationsTable, "rdv"); }
    @FXML public void goToPatientConsultations() { FxNavigation.navigatePatient(getClass(), registrationsTable, "consultations"); }
    @FXML public void goToHomeEtat() { FxNavigation.navigatePatient(getClass(), registrationsTable, "etat"); }
    @FXML public void goToHomeActivite() { FxNavigation.navigatePatient(getClass(), registrationsTable, "activite"); }
    @FXML public void goToHome() { FxNavigation.navigatePatient(getClass(), registrationsTable, "home"); }
    @FXML public void goToProfile() { FxNavigation.navigatePatient(getClass(), registrationsTable, "profile"); }

    @FXML
    public void handleFilterAll() {
        setActiveQuickFilter("all");
        applyFilters();
    }

    @FXML
    public void handleFilterUpcoming() {
        setActiveQuickFilter("upcoming");
        applyFilters();
    }

    @FXML
    public void handleFilterToday() {
        setActiveQuickFilter("today");
        applyFilters();
    }

    @FXML
    public void handleCancelRegistration() {
        if (selectedRegistration == null) {
            messageLabel.setText("Selectionnez d'abord une inscription.");
            return;
        }

        try {
            boolean removed = eventService.cancelPatientRegistration(selectedRegistration.getId(), currentUser.getEmail());
            if (!removed) {
                messageLabel.setText("Aucune inscription active a annuler.");
                return;
            }

            messageLabel.setText("Inscription annulee avec succes.");
            exercises.clear();
            selectedRegistration = null;
            detailsLabel.setText("Selectionnez une inscription pour voir les exercices associes.");
            if (cancelRegistrationButton != null) {
                cancelRegistrationButton.setDisable(true);
            }
            loadRegistrations();
        } catch (Exception e) {
            messageLabel.setText("Erreur annulation : " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            registrationsTable.getScene().setRoot(root);
        } catch (Exception e) {
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void refreshKpis() throws Exception {
        LocalDate today = LocalDate.now();
        long upcoming = allRegistrations.stream()
                .filter(event -> event.getDateDebut() != null)
                .filter(event -> !event.getDateDebut().toLocalDate().isBefore(today))
                .count();
        int exercisesCount = 0;
        for (Event event : allRegistrations) {
            exercisesCount += exerciseService.getByEventIdPublic(event.getId()).size();
        }
        kpiRegistrationsLabel.setText(String.valueOf(allRegistrations.size()));
        kpiUpcomingLabel.setText(String.valueOf(upcoming));
        kpiExercisesLabel.setText(String.valueOf(exercisesCount));
    }

    private void setActiveQuickFilter(String quickFilter) {
        activeQuickFilter = quickFilter;
        filterAllBtn.getStyleClass().remove("quick-filter-chip-active");
        filterUpcomingBtn.getStyleClass().remove("quick-filter-chip-active");
        filterTodayBtn.getStyleClass().remove("quick-filter-chip-active");
        switch (quickFilter) {
            case "today" -> filterTodayBtn.getStyleClass().add("quick-filter-chip-active");
            case "upcoming" -> filterUpcomingBtn.getStyleClass().add("quick-filter-chip-active");
            default -> filterAllBtn.getStyleClass().add("quick-filter-chip-active");
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String toStatusLabel(String status) {
        if (status == null) return "Inconnu";
        return switch (status.toLowerCase()) {
            case "en_attente" -> "En attente";
            case "valide" -> "Valide";
            case "annule" -> "Annule";
            default -> status;
        };
    }

    private String getStatusClass(String status) {
        if (status == null) return "status-chip-pending";
        return switch (status.toLowerCase()) {
            case "valide" -> "status-chip-valid";
            case "annule" -> "status-chip-cancelled";
            default -> "status-chip-pending";
        };
    }

    private String getRowClass(String status) {
        if (status == null) return "row-status-pending";
        return switch (status.toLowerCase()) {
            case "valide" -> "row-status-valid";
            case "annule" -> "row-status-cancelled";
            default -> "row-status-pending";
        };
    }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            registrationsTable.getScene().setRoot(root);
        } catch (Exception e) {
            messageLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }

    private String getInitials(User user) {
        String p = user.getPrenom() != null && !user.getPrenom().isEmpty()
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase()
                : "";
        String n = user.getNom() != null && !user.getNom().isEmpty()
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase()
                : "";
        return p + n;
    }
}
