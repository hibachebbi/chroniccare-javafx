package com.chroniccare.controllers;

import com.chroniccare.models.Event;
import com.chroniccare.models.Exercise;
import com.chroniccare.models.User;
import com.chroniccare.services.EventService;
import com.chroniccare.services.ExerciseService;
import com.chroniccare.services.OpenAiSuggestionService;
import com.chroniccare.utils.FormValidationUtils;
import com.chroniccare.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AddExerciseController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private TextField nomField;
    @FXML private TextField dureeField;
    @FXML private TextField repetitionsField;
    @FXML private ComboBox<Event> eventCombo;
    @FXML private TextArea descriptionArea;
    @FXML private Button suggestDescriptionButton;
    @FXML private Label aiSuggestionLabel;
    @FXML private TextField videoUrlField;
    @FXML private Button testVideoButton;
    @FXML private Label errorLabel;

    private final ExerciseService exerciseService = new ExerciseService();
    private final EventService eventService = new EventService();
    private final OpenAiSuggestionService openAiSuggestionService = new OpenAiSuggestionService();
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        populateHeader();
        clearError();
        setupLiveValidation();
        loadEvents();
    }

    @FXML
    public void handleSave() {
        try {
            clearError();
            if (!validateForm()) {
                return;
            }
            Exercise exercise = buildExerciseFromValidatedFields();
            exerciseService.insert(exercise);
            goToList();
        } catch (Exception e) {
            showError("Erreur enregistrement : " + e.getMessage());
        }
    }

    @FXML
    public void handleTestVideoUrl() {
        String url = videoUrlField.getText() == null ? "" : videoUrlField.getText().trim();
        if (url.isEmpty()) {
            setAiSuggestionState(false, "Colle un lien YouTube d'abord.");
            return;
        }
        if (!isValidYoutubeUrl(url)) {
            setAiSuggestionState(false, "Format invalide. Utilise: youtube.com/watch?v=... ou youtu.be/...");
            return;
        }
        setAiSuggestionState(false, "✅ Lien YouTube valide!");
    }

    @FXML
    public void handleSuggestDescription() {
        String nom = nomField.getText() == null ? "" : nomField.getText().trim();
        if (nom.isEmpty()) {
            setAiSuggestionState(false, "Renseigne au moins le nom de l'exercice avant la suggestion.");
            return;
        }

        setAiSuggestionState(true, "Generation IA en cours...");
        String duration = dureeField.getText() == null ? "" : dureeField.getText().trim();
        String reps = repetitionsField.getText() == null ? "" : repetitionsField.getText().trim();
        Event selectedEvent = eventCombo.getValue();
        String eventTitle = selectedEvent != null ? selectedEvent.getTitre() : "";

        Thread aiThread = new Thread(() -> {
            try {
                String suggestion = openAiSuggestionService.suggestExerciseDescription(
                        nom,
                        duration,
                        reps,
                        eventTitle
                );
                Platform.runLater(() -> {
                    descriptionArea.setText(suggestion);
                    setAiSuggestionState(false, "Description suggeree. Tu peux la modifier.");
                    refreshValidationFeedback();
                });
            } catch (Exception e) {
                Platform.runLater(() -> setAiSuggestionState(false, "IA indisponible : " + e.getMessage()));
            }
        });
        aiThread.setDaemon(true);
        aiThread.start();
    }

    @FXML
    public void goToList() {
        navigate("/com/chroniccare/coach-exercises.fxml");
    }

    @FXML
    public void goToEvents() {
        navigate("/com/chroniccare/coach-events.fxml");
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

    private void loadEvents() {
        if (currentUser == null) {
            return;
        }

        try {
            List<Event> events = eventService.getByCoachIdForSelection(currentUser.getId());
            eventCombo.setItems(FXCollections.observableArrayList(events));
        } catch (Exception e) {
            showError("Erreur chargement evenements : " + e.getMessage());
        }
    }

    private Exercise buildExerciseFromValidatedFields() {
        Exercise exercise = new Exercise();
        exercise.setNom(nomField.getText().trim());
        exercise.setDescription(descriptionArea.getText().trim());
        exercise.setDuree(Integer.parseInt(dureeField.getText().trim()));
        exercise.setEvenementId(eventCombo.getValue().getId());

        String repetitionsText = repetitionsField.getText().trim();
        if (!repetitionsText.isEmpty()) {
            exercise.setRepetitions(Integer.parseInt(repetitionsText));
        }

        String videoUrl = videoUrlField.getText() == null ? "" : videoUrlField.getText().trim();
        if (!videoUrl.isEmpty() && isValidYoutubeUrl(videoUrl)) {
            exercise.setVideoUrl(videoUrl);
        }

        return exercise;
    }

    private boolean validateForm() {
        clearInvalidStyles();
        Set<String> errors = new LinkedHashSet<>();

        String nom = nomField.getText() == null ? "" : nomField.getText().trim();
        String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        String duree = dureeField.getText() == null ? "" : dureeField.getText().trim();
        String repetitions = repetitionsField.getText() == null ? "" : repetitionsField.getText().trim();

        if (nom.isEmpty()) {
            markInvalid(nomField, "Nom obligatoire.", errors);
        } else if (!FormValidationUtils.isValidExerciseName(nom)) {
            markInvalid(nomField, "Nom invalide (3 a 100 caracteres, texte lisible).", errors);
        }

        if (eventCombo.getValue() == null) {
            markInvalid(eventCombo, "Evenement obligatoire.", errors);
        }

        if (duree.isEmpty()) {
            markInvalid(dureeField, "Duree obligatoire.", errors);
        } else if (!FormValidationUtils.isPositiveIntInRange(duree, 1, 240)) {
            markInvalid(dureeField, "Duree invalide (entre 1 et 240 minutes).", errors);
        }

        if (!repetitions.isEmpty() && !FormValidationUtils.isPositiveIntInRange(repetitions, 1, 500)) {
            markInvalid(repetitionsField, "Repetitions invalides (entre 1 et 500).", errors);
        }

        if (description.isEmpty()) {
            markInvalid(descriptionArea, "Description obligatoire.", errors);
        } else if (!FormValidationUtils.isValidDescription(description, 12)) {
            markInvalid(descriptionArea, "Description invalide (min 12 caracteres).", errors);
        }

        if (!errors.isEmpty()) {
            showError("• " + String.join("\n• ", errors));
            return false;
        }

        clearError();
        return true;
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
            nomField.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Erreur navigation : " + e.getMessage());
        }
    }

    private void setupLiveValidation() {
        nomField.textProperty().addListener((obs, oldVal, newVal) -> refreshValidationFeedback());
        dureeField.textProperty().addListener((obs, oldVal, newVal) -> refreshValidationFeedback());
        repetitionsField.textProperty().addListener((obs, oldVal, newVal) -> refreshValidationFeedback());
        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> refreshValidationFeedback());
        eventCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshValidationFeedback());
    }

    private void refreshValidationFeedback() {
        if (errorLabel.isVisible()) {
            validateForm();
        }
    }

    private void setAiSuggestionState(boolean loading, String message) {
        if (suggestDescriptionButton != null) {
            suggestDescriptionButton.setDisable(loading);
        }
        if (aiSuggestionLabel != null) {
            aiSuggestionLabel.setText(message);
        }
    }

    private void markInvalid(Control control, String message, Set<String> errors) {
        if (!control.getStyleClass().contains("field-invalid")) {
            control.getStyleClass().add("field-invalid");
        }
        errors.add(message);
    }

    private void clearInvalidStyles() {
        nomField.getStyleClass().remove("field-invalid");
        eventCombo.getStyleClass().remove("field-invalid");
        dureeField.getStyleClass().remove("field-invalid");
        repetitionsField.getStyleClass().remove("field-invalid");
        descriptionArea.getStyleClass().remove("field-invalid");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private String getInitials(User user) {
        String prenom = user.getPrenom() != null && !user.getPrenom().isEmpty()
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String nom = user.getNom() != null && !user.getNom().isEmpty()
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return prenom + nom;
    }

    private boolean isValidYoutubeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return url.contains("youtube.com") || url.contains("youtu.be");
    }
}
