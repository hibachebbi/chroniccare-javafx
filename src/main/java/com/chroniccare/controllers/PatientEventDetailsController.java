package com.chroniccare.controllers;

import com.chroniccare.models.Event;
import com.chroniccare.models.Exercise;
import com.chroniccare.models.User;
import com.chroniccare.services.EventService;
import com.chroniccare.services.ExerciseService;
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
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PatientEventDetailsController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private Label eventTitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label startLabel;
    @FXML private Label endLabel;
    @FXML private Label locationLabel;
    @FXML private Label descriptionLabel;
    @FXML private TableView<Exercise> exercisesTable;
    @FXML private TableColumn<Exercise, String> exNomCol;
    @FXML private TableColumn<Exercise, Integer> exDureeCol;
    @FXML private TableColumn<Exercise, String> exRepCol;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;

    private final EventService eventService = new EventService();
    private final ExerciseService exerciseService = new ExerciseService();
    private final ObservableList<Exercise> exercises = FXCollections.observableArrayList();
    private User currentUser;
    private int eventId;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        setupHeader();
        setupTable();
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
        loadEventDetails();
    }

    private void setupHeader() {
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

    private void setupTable() {
        exNomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        exDureeCol.setCellValueFactory(new PropertyValueFactory<>("duree"));
        exRepCol.setCellValueFactory(new PropertyValueFactory<>("repetitionsDisplay"));
        exercisesTable.setItems(exercises);
    }

    private void loadEventDetails() {
        try {
            Event event = eventService.getById(eventId);
            if (event == null) {
                messageLabel.setText("Evenement introuvable.");
                registerButton.setDisable(true);
                return;
            }
            eventTitleLabel.setText(event.getTitre());
            statusLabel.setText(event.getStatut() != null ? event.getStatut() : "Non precise");
            startLabel.setText(event.getDateDebutDisplay());
            endLabel.setText(event.getDateFinDisplay());
            locationLabel.setText(event.getLieu() != null ? event.getLieu() : "-");
            descriptionLabel.setText(event.getDescription() != null && !event.getDescription().isBlank()
                    ? event.getDescription()
                    : "Aucune description.");
            exercises.setAll(exerciseService.getByEventIdPublic(eventId));
            boolean alreadyRegistered = eventService.isPatientRegistered(eventId, currentUser.getEmail());
            registerButton.setDisable(alreadyRegistered);
            messageLabel.setText(alreadyRegistered
                    ? "Vous etes deja inscrit a cet evenement."
                    : "Inscrivez-vous pour reserver votre place.");
        } catch (Exception e) {
            messageLabel.setText("Erreur chargement details : " + e.getMessage());
        }
    }

    @FXML
    public void handleRegister() {
        try {
            boolean created = eventService.registerPatientToEvent(eventId, currentUser);
            if (created) {
                registerButton.setDisable(true);
                messageLabel.setText("Inscription enregistree avec succes.");
            } else {
                registerButton.setDisable(true);
                messageLabel.setText("Vous etes deja inscrit a cet evenement.");
            }
        } catch (Exception e) {
            messageLabel.setText("Erreur inscription : " + e.getMessage());
        }
    }

    @FXML
    public void goToEvents() {
        navigateTo("/com/chroniccare/patient-events.fxml");
    }

    @FXML
    public void goToMyRegistrations() {
        navigateTo("/com/chroniccare/patient-registrations.fxml");
    }

    @FXML
    public void goToHome() {
        navigateTo("/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToProfile() {
        navigateTo("/com/chroniccare/profile-patient.fxml");
    }

    @FXML
    public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            exercisesTable.getScene().setRoot(root);
        } catch (Exception e) {
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            exercisesTable.getScene().setRoot(root);
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
