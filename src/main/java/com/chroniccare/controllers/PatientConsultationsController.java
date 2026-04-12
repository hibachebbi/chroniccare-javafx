package com.chroniccare.controllers;

import com.chroniccare.models.Consultation;
import com.chroniccare.models.User;
import com.chroniccare.services.ConsultationService;
import com.chroniccare.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PatientConsultationsController {
    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private TableView<Consultation> consultationTable;
    @FXML private TableColumn<Consultation, String> colDate;
    @FXML private TableColumn<Consultation, String> colNutritionist;
    @FXML private TableColumn<Consultation, String> colStatus;
    @FXML private TableColumn<Consultation, String> colTheme;
    @FXML private Label detailDateLabel;
    @FXML private Label detailNutritionistLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailThemeLabel;
    @FXML private Label detailFollowUpLabel;
    @FXML private Label detailReadLabel;
    @FXML private TextArea summaryArea;
    @FXML private TextArea recommendationsArea;
    @FXML private TextArea mealPlanArea;
    @FXML private TextArea objectivesArea;
    @FXML private TextArea medicalNotesArea;
    @FXML private Button markAsReadButton;
    @FXML private Button exportButton;

    private final ConsultationService consultationService = new ConsultationService();

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        applyHeader(currentUser);
        configureTable();
        try {
            loadConsultations();
        } catch (Exception e) {
            showError("Impossible de charger les consultations : " + e.getMessage());
        }
    }

    @FXML
    public void handleMarkAsRead() {
        Consultation consultation = consultationTable.getSelectionModel().getSelectedItem();
        if (consultation == null) return;
        try {
            consultationService.markAsRead(consultation.getId());
            showSuccess("Consultation marquee comme lue.");
            loadConsultations();
        } catch (Exception e) {
            showError("Impossible de mettre a jour la consultation : " + e.getMessage());
        }
    }

    @FXML
    public void handleExport() {
        Consultation consultation = consultationTable.getSelectionModel().getSelectedItem();
        if (consultation == null) {
            showError("Selectionnez une consultation a exporter.");
            return;
        }
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter la consultation");
            chooser.setInitialFileName("consultation-" + consultation.getId() + ".txt");
            File file = chooser.showSaveDialog(exportButton.getScene().getWindow());
            if (file == null) return;

            String content = "Consultation du " + consultation.getConsultationDateDisplay() + System.lineSeparator() +
                    "Nutritionniste : " + consultation.getNutritionistName() + System.lineSeparator() +
                    "Theme : " + consultation.getTheme() + System.lineSeparator() +
                    "Statut : " + consultation.getAppointmentStatusLabel() + System.lineSeparator() +
                    "Suivi : " + consultation.getFollowUpDateDisplay() + System.lineSeparator() + System.lineSeparator() +
                    "Resume" + System.lineSeparator() + consultation.getSummary() + System.lineSeparator() + System.lineSeparator() +
                    "Recommandations" + System.lineSeparator() + consultation.getRecommendations() + System.lineSeparator() + System.lineSeparator() +
                    "Plan alimentaire" + System.lineSeparator() + consultation.getMealPlan() + System.lineSeparator() + System.lineSeparator() +
                    "Objectifs" + System.lineSeparator() + consultation.getObjectives() + System.lineSeparator() + System.lineSeparator() +
                    "Notes medicales" + System.lineSeparator() + valueOrDash(consultation.getMedicalNotes());
            Files.writeString(file.toPath(), content);
            showSuccess("Consultation exportee.");
        } catch (Exception e) {
            showError("Export impossible : " + e.getMessage());
        }
    }

    @FXML public void goToHome() { navigate("/com/chroniccare/home.fxml"); }
    @FXML public void goToProfile() { navigate("/com/chroniccare/profile-patient.fxml"); }
    @FXML public void goToPatientEvents() { navigate("/com/chroniccare/patient-events.fxml"); }
    @FXML public void goToPatientRegistrations() { navigate("/com/chroniccare/patient-registrations.fxml"); }
    @FXML public void goToPatientBookRdv() { navigate("/com/chroniccare/patient-rdv.fxml"); }
    @FXML public void goToPatientFollowup() { navigate("/com/chroniccare/patient-rdv.fxml"); }
    @FXML public void goToPatientConsultations() { navigate("/com/chroniccare/patient-consultations.fxml"); }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        navigate("/com/chroniccare/login.fxml");
    }

    private void configureTable() {
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getConsultationDateDisplay()));
        colNutritionist.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNutritionistName()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAppointmentStatusLabel()));
        colTheme.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTheme()));
        consultationTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> showConsultation(newValue));
    }

    private void loadConsultations() throws Exception {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        List<Consultation> consultations = consultationService.getConsultationsForPatient(currentUser.getId());
        consultationTable.setItems(FXCollections.observableArrayList(consultations));
        if (!consultations.isEmpty()) {
            consultationTable.getSelectionModel().selectFirst();
        } else {
            showConsultation(null);
        }
    }

    private void showConsultation(Consultation consultation) {
        boolean hasConsultation = consultation != null;
        markAsReadButton.setDisable(!hasConsultation);
        exportButton.setDisable(!hasConsultation);
        if (!hasConsultation) {
            detailDateLabel.setText("-");
            detailNutritionistLabel.setText("-");
            detailStatusLabel.setText("-");
            detailThemeLabel.setText("-");
            detailFollowUpLabel.setText("-");
            detailReadLabel.setText("-");
            summaryArea.clear();
            recommendationsArea.clear();
            mealPlanArea.clear();
            objectivesArea.clear();
            medicalNotesArea.clear();
            return;
        }
        detailDateLabel.setText(consultation.getConsultationDateDisplay());
        detailNutritionistLabel.setText(valueOrDash(consultation.getNutritionistName()));
        detailStatusLabel.setText(consultation.getAppointmentStatusLabel());
        detailThemeLabel.setText(valueOrDash(consultation.getTheme()));
        detailFollowUpLabel.setText(consultation.getFollowUpDateDisplay());
        detailReadLabel.setText(consultation.isRead() ? "Lu" : "Non lu");
        summaryArea.setText(valueOrDash(consultation.getSummary()));
        recommendationsArea.setText(valueOrDash(consultation.getRecommendations()));
        mealPlanArea.setText(valueOrDash(consultation.getMealPlan()));
        objectivesArea.setText(valueOrDash(consultation.getObjectives()));
        medicalNotesArea.setText(valueOrDash(consultation.getMedicalNotes()));
    }

    private void applyHeader(User currentUser) {
        String initials = getInitials(currentUser);
        sidebarAvatar.setText(initials);
        sidebarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        topbarAvatar.setText(initials);
        topbarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        topbarDate.setText(LocalDate.now().format(formatter));
    }

    private void navigate(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Region region = (Region) sidebarAvatar.getScene().getRoot();
            region.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation impossible : " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        successLabel.setText("");
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        errorLabel.setText("");
    }

    private String getInitials(User user) {
        String prenom = user.getPrenom() != null && !user.getPrenom().isEmpty()
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String nom = user.getNom() != null && !user.getNom().isEmpty()
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return prenom + nom;
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
