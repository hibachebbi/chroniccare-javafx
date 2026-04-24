package com.chroniccare.controllers;

import com.chroniccare.models.Event;
import com.chroniccare.models.EventRegistration;
import com.chroniccare.models.User;
import com.chroniccare.services.EventService;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EventParticipantsController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private Label eventTitleLabel;
    @FXML private Label eventMetaLabel;
    @FXML private Label countLabel;
    @FXML private Label errorLabel;
    @FXML private TextField searchField;
    @FXML private TableView<EventRegistration> participantsTable;
    @FXML private Button exportCsvButton;
    @FXML private TableColumn<EventRegistration, Integer> idCol;
    @FXML private TableColumn<EventRegistration, String> nomCol;
    @FXML private TableColumn<EventRegistration, String> prenomCol;
    @FXML private TableColumn<EventRegistration, String> emailCol;
    @FXML private TableColumn<EventRegistration, String> telephoneCol;
    @FXML private TableColumn<EventRegistration, String> createdAtCol;

    private final EventService eventService = new EventService();
    private final ObservableList<EventRegistration> allRegistrations = FXCollections.observableArrayList();
    private final ObservableList<EventRegistration> displayedRegistrations = FXCollections.observableArrayList();
    private User currentUser;
    private Event currentEvent;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        populateHeader();
        setupTable();
    }

    public void setEvent(Event event) {
        this.currentEvent = event;
        if (event == null) {
            return;
        }

        eventTitleLabel.setText(event.getTitre());
        eventMetaLabel.setText(event.getLieu() + " | " + event.getDateRangeDisplay());
        loadRegistrations();
    }

    @FXML
    public void handleSearch() {
        applyFilter();
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        applyFilter();
    }

    @FXML
    public void handleExportCsv() {
        if (currentEvent == null) {
            errorLabel.setText("Aucun evenement selectionne pour l'export.");
            return;
        }
        if (displayedRegistrations.isEmpty()) {
            errorLabel.setText("Aucun inscrit a exporter.");
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter les inscrits en CSV");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
            fileChooser.setInitialFileName(buildExportFileName());

            File selectedFile = fileChooser.showSaveDialog(
                    participantsTable != null && participantsTable.getScene() != null
                            ? participantsTable.getScene().getWindow()
                            : null
            );
            if (selectedFile == null) {
                return;
            }

            Files.writeString(
                    selectedFile.toPath(),
                    buildCsvContent(),
                    StandardCharsets.UTF_8
            );
            errorLabel.setText("Export CSV genere : " + selectedFile.getName());
        } catch (Exception e) {
            errorLabel.setText("Erreur export CSV : " + e.getMessage());
        }
    }

    @FXML
    public void goToEvents() {
        navigate("/com/chroniccare/coach-events.fxml");
    }

    @FXML
    public void goToExercises() {
        navigate("/com/chroniccare/coach-exercises.fxml");
    }

    @FXML
    public void goToProfile() {
        navigate("/com/chroniccare/profile-coach.fxml");
    }

    @FXML
    public void goToHome() {
        navigate("/com/chroniccare/home.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        navigate("/com/chroniccare/login.fxml");
    }

    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomCol.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        telephoneCol.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        createdAtCol.setCellValueFactory(new PropertyValueFactory<>("createdAtDisplay"));
    }

    private void loadRegistrations() {
        if (currentUser == null || currentEvent == null) {
            return;
        }

        try {
            List<EventRegistration> registrations = eventService.getRegistrationsForEvent(
                    currentEvent.getId(), currentUser.getId());
            allRegistrations.setAll(registrations);
            applyFilter();
            errorLabel.setText("");
        } catch (Exception e) {
            errorLabel.setText("Erreur chargement inscrits : " + e.getMessage());
        }
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<EventRegistration> result = allRegistrations.stream()
                .filter(registration -> keyword.isEmpty()
                        || contains(registration.getNom(), keyword)
                        || contains(registration.getPrenom(), keyword)
                        || contains(registration.getEmail(), keyword)
                        || contains(registration.getTelephone(), keyword))
                .collect(Collectors.toList());

        displayedRegistrations.setAll(result);
        participantsTable.setItems(displayedRegistrations);
        countLabel.setText(result.size() + " participant(s)");
    }

    private String buildExportFileName() {
        String eventTitle = currentEvent != null ? currentEvent.getTitre() : "participants";
        String safeTitle = eventTitle == null ? "participants" : eventTitle
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (safeTitle.isBlank()) {
            safeTitle = "participants";
        }
        return "inscrits-" + safeTitle + ".csv";
    }

    private String buildCsvContent() {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("id;nom;prenom;email;telephone;date_inscription").append('\n');
        for (EventRegistration registration : displayedRegistrations) {
            builder.append(csvValue(String.valueOf(registration.getId()))).append(';')
                    .append(csvValue(registration.getNom())).append(';')
                    .append(csvValue(registration.getPrenom())).append(';')
                    .append(csvValue(registration.getEmail())).append(';')
                    .append(csvValue(registration.getTelephone())).append(';')
                    .append(csvValue(registration.getCreatedAtDisplay()))
                    .append('\n');
        }
        return builder.toString();
    }

    private String csvValue(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private void populateHeader() {
        if (currentUser == null) {
            return;
        }

        String initials = getInitials(currentUser);
        sidebarAvatar.setText(initials);
        sidebarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        topbarAvatar.setText(initials);
        topbarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        topbarDate.setText(LocalDate.now().format(fmt));
    }

    private void navigate(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            participantsTable.getScene().setRoot(root);
        } catch (Exception e) {
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String getInitials(User user) {
        String prenom = user.getPrenom() != null && !user.getPrenom().isEmpty()
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String nom = user.getNom() != null && !user.getNom().isEmpty()
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return prenom + nom;
    }
}
