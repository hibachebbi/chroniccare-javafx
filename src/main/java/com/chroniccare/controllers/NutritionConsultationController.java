package com.chroniccare.controllers;

import com.chroniccare.models.Appointment;
import com.chroniccare.models.Consultation;
import com.chroniccare.models.User;
import com.chroniccare.services.AppointmentService;
import com.chroniccare.services.ConsultationService;
import com.chroniccare.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class NutritionConsultationController {
    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private Label patientNameLabel;
    @FXML private Label requestedSlotLabel;
    @FXML private Label followUpTypeLabel;
    @FXML private Label medicalContextLabel;
    @FXML private Label historyLabel;
    @FXML private TextField themeField;
    @FXML private TextArea summaryArea;
    @FXML private TextArea recommendationsArea;
    @FXML private TextArea mealPlanArea;
    @FXML private TextArea objectivesArea;
    @FXML private TextArea medicalNotesArea;
    @FXML private DatePicker followUpDatePicker;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private final AppointmentService appointmentService = new AppointmentService();
    private final ConsultationService consultationService = new ConsultationService();
    private Appointment currentAppointment;

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        applyHeader(currentUser);
        try {
            Integer appointmentId = SessionManager.getInstance().getSelectedAppointmentId();
            if (appointmentId == null) {
                showError("Aucun rendez-vous selectionne.");
                return;
            }
            currentAppointment = appointmentService.getAppointmentById(appointmentId);
            if (currentAppointment == null) {
                showError("Rendez-vous introuvable.");
                return;
            }
            populateContext();
            populateExistingConsultation();
        } catch (Exception e) {
            showError("Impossible de charger la consultation : " + e.getMessage());
        }
    }

    @FXML
    public void handleSaveConsultation() {
        if (currentAppointment == null) {
            showError("Rendez-vous indisponible.");
            return;
        }
        if (themeField.getText() == null || themeField.getText().trim().isEmpty()
                || summaryArea.getText().trim().isEmpty()
                || recommendationsArea.getText().trim().isEmpty()
                || mealPlanArea.getText().trim().isEmpty()
                || objectivesArea.getText().trim().isEmpty()) {
            showError("Theme, resume, recommandations, plan alimentaire et objectifs sont obligatoires.");
            return;
        }
        try {
            consultationService.saveConsultation(
                    currentAppointment,
                    themeField.getText().trim(),
                    summaryArea.getText().trim(),
                    recommendationsArea.getText().trim(),
                    mealPlanArea.getText().trim(),
                    objectivesArea.getText().trim(),
                    medicalNotesArea.getText().trim(),
                    followUpDatePicker.getValue()
            );
            successLabel.setText("Consultation enregistree. Le rendez-vous passe en Termine.");
            errorLabel.setText("");
            populateExistingConsultation();
            populateContext();
        } catch (Exception e) {
            showError("Impossible d'enregistrer la consultation : " + e.getMessage());
        }
    }

    @FXML public void goToHome() { navigate("/com/chroniccare/home.fxml"); }
    @FXML public void goToProfile() { navigate("/com/chroniccare/profile-nutritionniste.fxml"); }
    @FXML public void goToRdv() { navigate("/com/chroniccare/nutrition-rdv.fxml"); }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        navigate("/com/chroniccare/login.fxml");
    }

    private void populateContext() throws Exception {
        patientNameLabel.setText(valueOrDash(currentAppointment.getPatientName()));
        requestedSlotLabel.setText(currentAppointment.getScheduledDateTime() != null
                ? currentAppointment.getScheduledDateTimeDisplay()
                : currentAppointment.getRequestedDateTimeDisplay());
        followUpTypeLabel.setText(valueOrDash(currentAppointment.getFollowUpType()));
        medicalContextLabel.setText(valueOrDash(currentAppointment.getMedicalSnapshot()));

        List<Consultation> history = consultationService.getConsultationsForNutritionistPatient(
                currentAppointment.getNutritionistId(),
                currentAppointment.getPatientId()
        );
        if (history.isEmpty()) {
            historyLabel.setText("Aucune consultation precedente.");
        } else {
            Consultation latest = history.get(0);
            historyLabel.setText("Derniere consultation : " + latest.getConsultationDateDisplay()
                    + " | Theme : " + valueOrDash(latest.getTheme())
                    + " | Objectifs : " + valueOrDash(latest.getObjectives()));
        }
    }

    private void populateExistingConsultation() throws Exception {
        Consultation consultation = consultationService.getConsultationByAppointmentId(currentAppointment.getId());
        if (consultation == null) return;

        themeField.setText(valueOrBlank(consultation.getTheme()));
        summaryArea.setText(valueOrBlank(consultation.getSummary()));
        recommendationsArea.setText(valueOrBlank(consultation.getRecommendations()));
        mealPlanArea.setText(valueOrBlank(consultation.getMealPlan()));
        objectivesArea.setText(valueOrBlank(consultation.getObjectives()));
        medicalNotesArea.setText(valueOrBlank(consultation.getMedicalNotes()));
        followUpDatePicker.setValue(consultation.getFollowUpDate());
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

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
