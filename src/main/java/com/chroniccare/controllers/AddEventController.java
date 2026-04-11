package com.chroniccare.controllers;

import com.chroniccare.models.Event;
import com.chroniccare.models.Exercise;
import com.chroniccare.models.ExerciseSelectionRow;
import com.chroniccare.models.User;
import com.chroniccare.services.EventService;
import com.chroniccare.services.ExerciseService;
import com.chroniccare.utils.FormValidationUtils;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AddEventController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private TextField titreField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField lieuField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private ComboBox<String> heureDebutCombo;
    @FXML private DatePicker dateFinPicker;
    @FXML private ComboBox<String> heureFinCombo;
    @FXML private TextArea descriptionArea;
    @FXML private TableView<ExerciseSelectionRow> exercisesTable;
    @FXML private TableColumn<ExerciseSelectionRow, Boolean> selectedCol;
    @FXML private TableColumn<ExerciseSelectionRow, String> exerciseNameCol;
    @FXML private TableColumn<ExerciseSelectionRow, Integer> exerciseDurationCol;
    @FXML private TableColumn<ExerciseSelectionRow, String> exerciseRepetitionsCol;
    @FXML private TableColumn<ExerciseSelectionRow, String> exerciseEventCol;
    @FXML private Label selectedExercisesInfoLabel;
    @FXML private Label sessionWindowInfoLabel;
    @FXML private Label formHintLabel;
    @FXML private Label errorLabel;

    private final EventService eventService = new EventService();
    private final ExerciseService exerciseService = new ExerciseService();
    private final ObservableList<ExerciseSelectionRow> selectableExercises = FXCollections.observableArrayList();
    private static final DateTimeFormatter INPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        populateHeader();
        configureStatusCombo();
        setupExercisesTable();
        setupFormExperience();
        clearError();
        loadSelectableExercises();
    }

    @FXML
    public void handleSave() {
        try {
            clearError();
            if (!validateForm()) {
                return;
            }
            Event event = buildEvent();
            int eventId = eventService.insertAndReturnId(event);
            assignSelectedExercises(eventId);
            goToList();
        } catch (Exception e) {
            showError("Erreur enregistrement : " + e.getMessage());
        }
    }

    @FXML
    public void goToList() {
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

    @FXML
    public void applyMorningPreset() {
        if (dateDebutPicker.getValue() == null) {
            dateDebutPicker.setValue(LocalDate.now());
        }
        if (dateFinPicker.getValue() == null) {
            dateFinPicker.setValue(dateDebutPicker.getValue());
        }
        heureDebutCombo.setValue("09:00");
        heureFinCombo.setValue("10:00");
        refreshSessionWindowSummary();
        formHintLabel.setText("Preset applique : session matinale configuree.");
    }

    @FXML
    public void applyEveningPreset() {
        if (dateDebutPicker.getValue() == null) {
            dateDebutPicker.setValue(LocalDate.now());
        }
        if (dateFinPicker.getValue() == null) {
            dateFinPicker.setValue(dateDebutPicker.getValue());
        }
        heureDebutCombo.setValue("18:00");
        heureFinCombo.setValue("19:00");
        refreshSessionWindowSummary();
        formHintLabel.setText("Preset applique : session du soir configuree.");
    }

    private void setupExercisesTable() {
        selectedCol.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        selectedCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectedCol));

        exerciseNameCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        exerciseDurationCol.setCellValueFactory(new PropertyValueFactory<>("duree"));
        exerciseRepetitionsCol.setCellValueFactory(new PropertyValueFactory<>("repetitionsDisplay"));
        exerciseEventCol.setCellValueFactory(new PropertyValueFactory<>("evenementTitreDisplay"));

        exercisesTable.setItems(selectableExercises);
        exercisesTable.setEditable(true);
        exercisesTable.setOnMouseClicked(event -> refreshSelectionSummary());
        exercisesTable.setOnKeyReleased(event -> refreshSelectionSummary());
    }

    private void configureStatusCombo() {
        statutCombo.setItems(FXCollections.observableArrayList("en_attente", "valide", "annule"));
        statutCombo.setValue("en_attente");
        statutCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                if (value == null) {
                    return "";
                }
                if ("valide".equals(value)) {
                    return "Valide";
                }
                if ("annule".equals(value)) {
                    return "Annule";
                }
                return "En attente";
            }

            @Override
            public String fromString(String value) {
                return value;
            }
        });
    }

    private void setupFormExperience() {
        LocalDate today = LocalDate.now();
        dateDebutPicker.setValue(today);
        dateFinPicker.setValue(today);
        List<String> timeSlots = buildTimeSlots();
        heureDebutCombo.setItems(FXCollections.observableArrayList(timeSlots));
        heureFinCombo.setItems(FXCollections.observableArrayList(timeSlots));
        heureDebutCombo.setValue("09:00");
        heureFinCombo.setValue("10:00");

        selectedExercisesInfoLabel.setText("0");
        formHintLabel.setText("Selectionne les exercices utiles puis ajuste le statut et les horaires.");
        refreshSessionWindowSummary();

        dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) -> refreshSessionWindowSummary());
        dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) -> refreshSessionWindowSummary());
        heureDebutCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshSessionWindowSummary());
        heureFinCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshSessionWindowSummary());
        titreField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateHint();
            refreshValidationFeedback();
        });
        lieuField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateHint();
            refreshValidationFeedback();
        });
        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> refreshValidationFeedback());
        statutCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshValidationFeedback());
        dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) -> refreshValidationFeedback());
        dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) -> refreshValidationFeedback());
        heureDebutCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshValidationFeedback());
        heureFinCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshValidationFeedback());
    }

    private void loadSelectableExercises() {
        if (currentUser == null) {
            return;
        }

        try {
            List<Exercise> exercises = exerciseService.getByCoachId(currentUser.getId());
            selectableExercises.setAll(exercises.stream().map(exercise -> {
                return new ExerciseSelectionRow(exercise);
            }).toList());
            refreshSelectionSummary();
        } catch (Exception e) {
            showError("Erreur chargement exercices : " + e.getMessage());
        }
    }

    private void assignSelectedExercises(int eventId) throws Exception {
        for (ExerciseSelectionRow row : selectableExercises) {
            if (row.isSelected()) {
                exerciseService.assignExerciseToEvent(row.getExercise().getId(), eventId, currentUser.getId());
            }
        }
    }

    private Event buildEvent() {
        if (currentUser == null) {
            throw new IllegalStateException("Session coach introuvable.");
        }

        String titre = titreField.getText().trim();
        String lieu = lieuField.getText().trim();
        String description = descriptionArea.getText().trim();
        String statut = statutCombo.getValue();

        LocalDateTime dateDebut = parseDateTime(dateDebutPicker.getValue(), heureDebutCombo.getValue());
        LocalDateTime dateFin = parseDateTime(dateFinPicker.getValue(), heureFinCombo.getValue());

        Event event = new Event();
        event.setTitre(titre);
        event.setLieu(lieu);
        event.setDescription(description);
        event.setStatut(statut);
        event.setDateDebut(dateDebut);
        event.setDateFin(dateFin);
        event.setCoachId(currentUser.getId());
        return event;
    }

    private boolean validateForm() {
        clearInvalidStyles();
        Set<String> errors = new LinkedHashSet<>();

        String titre = titreField.getText() == null ? "" : titreField.getText().trim();
        String lieu = lieuField.getText() == null ? "" : lieuField.getText().trim();
        String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        String heureDebut = heureDebutCombo.getValue() == null ? "" : heureDebutCombo.getValue().trim();
        String heureFin = heureFinCombo.getValue() == null ? "" : heureFinCombo.getValue().trim();

        if (titre.isEmpty()) {
            markInvalid(titreField, "Titre obligatoire.", errors);
        } else if (!FormValidationUtils.isValidEventTitle(titre)) {
            markInvalid(titreField, "Titre invalide (5 a 100 caracteres, texte lisible).", errors);
        }

        if (statutCombo.getValue() == null) {
            markInvalid(statutCombo, "Statut obligatoire.", errors);
        }

        if (lieu.isEmpty()) {
            markInvalid(lieuField, "Lieu obligatoire.", errors);
        } else if (!FormValidationUtils.isValidLocation(lieu)) {
            markInvalid(lieuField, "Lieu invalide (pas de chiffres, lettres uniquement).", errors);
        }

        if (description.isEmpty()) {
            markInvalid(descriptionArea, "Description obligatoire.", errors);
        } else if (!FormValidationUtils.isValidDescription(description, 15)) {
            markInvalid(descriptionArea, "Description invalide (min 15 caracteres).", errors);
        }

        LocalDateTime dateDebut = parseOptionalDateTime(dateDebutPicker.getValue(), heureDebutCombo.getValue());
        LocalDateTime dateFin = parseOptionalDateTime(dateFinPicker.getValue(), heureFinCombo.getValue());
        if (dateDebut == null) {
            markInvalid(dateDebutPicker, "Date/heure de debut invalide.", errors);
            markInvalid(heureDebutCombo, "Date/heure de debut invalide.", errors);
        }
        if (dateFin == null) {
            markInvalid(dateFinPicker, "Date/heure de fin invalide.", errors);
            markInvalid(heureFinCombo, "Date/heure de fin invalide.", errors);
        }
        if (dateDebut != null && dateFin != null && !dateFin.isAfter(dateDebut)) {
            markInvalid(dateFinPicker, "La fin doit etre apres le debut.", errors);
            markInvalid(heureFinCombo, "La fin doit etre apres le debut.", errors);
        }
        if (dateDebut != null && dateFin != null && dateFin.isAfter(dateDebut)) {
            long duration = Duration.between(dateDebut, dateFin).toMinutes();
            if (duration < 15 || duration > 480) {
                markInvalid(heureFinCombo, "Duree invalide (entre 15 min et 8h).", errors);
            }
        }
        if (!heureDebut.isEmpty() && !heureDebut.matches("^\\d{2}:\\d{2}$")) {
            markInvalid(heureDebutCombo, "Heure de debut invalide (HH:mm).", errors);
        }
        if (!heureFin.isEmpty() && !heureFin.matches("^\\d{2}:\\d{2}$")) {
            markInvalid(heureFinCombo, "Heure de fin invalide (HH:mm).", errors);
        }

        if (!errors.isEmpty()) {
            showError("• " + String.join("\n• ", errors));
            return false;
        }

        clearError();
        return true;
    }

    private LocalDateTime parseDateTime(LocalDate date, String timeText) {
        if (date == null || timeText == null || timeText.trim().isEmpty()) {
            throw new IllegalArgumentException("Les dates et heures sont obligatoires.");
        }

        try {
            LocalTime time = LocalTime.parse(timeText.trim(), INPUT_TIME_FORMATTER);
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format heure invalide. Utilise HH:mm.");
        }
    }

    private void refreshSelectionSummary() {
        long selected = selectableExercises.stream().filter(ExerciseSelectionRow::isSelected).count();
        selectedExercisesInfoLabel.setText(String.valueOf(selected));
        updateHint();
    }

    private void refreshSessionWindowSummary() {
        LocalDateTime start = parseOptionalDateTime(dateDebutPicker.getValue(), heureDebutCombo.getValue());
        LocalDateTime end = parseOptionalDateTime(dateFinPicker.getValue(), heureFinCombo.getValue());
        if (start == null || end == null) {
            sessionWindowInfoLabel.setText("--");
            return;
        }
        if (!end.isAfter(start)) {
            sessionWindowInfoLabel.setText("Invalide");
            return;
        }
        long minutes = Duration.between(start, end).toMinutes();
        sessionWindowInfoLabel.setText(minutes + " min");
    }

    private LocalDateTime parseOptionalDateTime(LocalDate date, String timeText) {
        if (date == null || timeText == null || timeText.trim().isEmpty()) {
            return null;
        }
        try {
            LocalTime time = LocalTime.parse(timeText.trim(), INPUT_TIME_FORMATTER);
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private List<String> buildTimeSlots() {
        List<String> slots = new ArrayList<>();
        LocalTime current = LocalTime.MIDNIGHT;
        LocalTime end = LocalTime.of(23, 45);
        while (!current.isAfter(end)) {
            slots.add(current.format(DISPLAY_TIME_FORMATTER));
            current = current.plusMinutes(15);
        }
        return slots;
    }

    private void updateHint() {
        String titre = titreField.getText() == null ? "" : titreField.getText().trim();
        String lieu = lieuField.getText() == null ? "" : lieuField.getText().trim();
        if (titre.isEmpty() || lieu.isEmpty()) {
            formHintLabel.setText("Ajoute un titre et un lieu pour finaliser le contexte de la session.");
            return;
        }
        formHintLabel.setText("Session prete : \"" + titre + "\" a " + lieu + ".");
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
            titreField.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Erreur navigation : " + e.getMessage());
        }
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

    private void refreshValidationFeedback() {
        if (errorLabel.isVisible()) {
            validateForm();
        }
    }

    private void markInvalid(Control control, String message, Set<String> errors) {
        if (!control.getStyleClass().contains("field-invalid")) {
            control.getStyleClass().add("field-invalid");
        }
        errors.add(message);
    }

    private void clearInvalidStyles() {
        titreField.getStyleClass().remove("field-invalid");
        statutCombo.getStyleClass().remove("field-invalid");
        lieuField.getStyleClass().remove("field-invalid");
        dateDebutPicker.getStyleClass().remove("field-invalid");
        heureDebutCombo.getStyleClass().remove("field-invalid");
        dateFinPicker.getStyleClass().remove("field-invalid");
        heureFinCombo.getStyleClass().remove("field-invalid");
        descriptionArea.getStyleClass().remove("field-invalid");
    }

    private String getInitials(User user) {
        String prenom = user.getPrenom() != null && !user.getPrenom().isEmpty()
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String nom = user.getNom() != null && !user.getNom().isEmpty()
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return prenom + nom;
    }
}
