package com.chroniccare.controllers;

import com.chroniccare.models.Consultation;
import com.chroniccare.models.User;
import com.chroniccare.services.ConsultationService;
import com.chroniccare.utils.SessionManager;
import com.chroniccare.utils.FxNavigation;
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
import javafx.scene.layout.VBox;
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

    @FXML private Label kpiTotalLabel;
    @FXML private Label kpiUnreadLabel;
    @FXML private Label kpiLastLabel;
    @FXML private Label heroPatientName;
    @FXML private Label heroAvatar;
    @FXML private VBox emptySelectionState;
    @FXML private VBox selectedConsultationDetails;
    @FXML private VBox emptyTableState;

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

    @FXML public void goToHome() { FxNavigation.navigatePatient(getClass(), sidebarAvatar, "home"); }
    @FXML public void goToHomeEtat() { FxNavigation.navigatePatient(getClass(), sidebarAvatar, "etat"); }
    @FXML public void goToHomeActivite() { FxNavigation.navigatePatient(getClass(), sidebarAvatar, "activite"); }
    @FXML public void goToProfile() { FxNavigation.navigatePatient(getClass(), sidebarAvatar, "profile"); }
    @FXML public void goToPatientEvents() { FxNavigation.navigatePatient(getClass(), sidebarAvatar, "events"); }
    @FXML public void goToPatientRegistrations() { FxNavigation.navigatePatient(getClass(), sidebarAvatar, "registrations"); }
    @FXML public void goToPatientBookRdv() { FxNavigation.navigatePatient(getClass(), sidebarAvatar, "rdv"); }
    @FXML public void goToPatientFollowup() { FxNavigation.navigatePatient(getClass(), sidebarAvatar, "rdv"); }
    @FXML public void goToPatientConsultations() { FxNavigation.navigatePatient(getClass(), sidebarAvatar, "consultations"); }

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
        updateKpis(consultations);
        boolean isEmpty = consultations.isEmpty();
        if (emptyTableState != null) {
            emptyTableState.setVisible(isEmpty);
            emptyTableState.setManaged(isEmpty);
        }
        if (consultationTable != null) {
            consultationTable.setVisible(!isEmpty);
            consultationTable.setManaged(!isEmpty);
        }
        if (!isEmpty) {
            consultationTable.getSelectionModel().selectFirst();
        } else {
            showConsultation(null);
        }
    }

    private void updateKpis(List<Consultation> consultations) {
        int unread = 0;
        String lastDisplay = "-";
        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (!c.isRead()) unread++;
            if (i == 0) lastDisplay = c.getConsultationDateDisplay();
        }
        if (kpiTotalLabel != null) kpiTotalLabel.setText(String.valueOf(consultations.size()));
        if (kpiUnreadLabel != null) kpiUnreadLabel.setText(String.valueOf(unread));
        if (kpiLastLabel != null) kpiLastLabel.setText(lastDisplay);
    }

    private void showConsultation(Consultation consultation) {
        boolean hasConsultation = consultation != null;
        markAsReadButton.setDisable(!hasConsultation);
        exportButton.setDisable(!hasConsultation);
        if (emptySelectionState != null) {
            emptySelectionState.setVisible(!hasConsultation);
            emptySelectionState.setManaged(!hasConsultation);
        }
        if (selectedConsultationDetails != null) {
            selectedConsultationDetails.setVisible(hasConsultation);
            selectedConsultationDetails.setManaged(hasConsultation);
        }
        if (!hasConsultation) {
            if (detailDateLabel != null) detailDateLabel.setText("-");
            if (detailNutritionistLabel != null) detailNutritionistLabel.setText("-");
            if (detailStatusLabel != null) {
                detailStatusLabel.setText("-");
                applyStatusPill(detailStatusLabel, null);
            }
            if (detailThemeLabel != null) detailThemeLabel.setText("-");
            if (detailFollowUpLabel != null) detailFollowUpLabel.setText("-");
            if (detailReadLabel != null) {
                detailReadLabel.setText("-");
                applyReadPill(detailReadLabel, true);
            }
            if (summaryArea != null) summaryArea.clear();
            if (recommendationsArea != null) recommendationsArea.clear();
            if (mealPlanArea != null) mealPlanArea.clear();
            if (objectivesArea != null) objectivesArea.clear();
            if (medicalNotesArea != null) medicalNotesArea.clear();
            return;
        }
        detailDateLabel.setText(consultation.getConsultationDateDisplay());
        detailNutritionistLabel.setText(valueOrDash(consultation.getNutritionistName()));
        detailStatusLabel.setText(consultation.getAppointmentStatusLabel());
        applyStatusPill(detailStatusLabel, consultation.getAppointmentStatus());
        detailThemeLabel.setText(valueOrDash(consultation.getTheme()));
        detailFollowUpLabel.setText(consultation.getFollowUpDateDisplay());
        detailReadLabel.setText(consultation.isRead() ? "Lue" : "Non lue");
        applyReadPill(detailReadLabel, consultation.isRead());
        summaryArea.setText(valueOrDash(consultation.getSummary()));
        recommendationsArea.setText(valueOrDash(consultation.getRecommendations()));
        mealPlanArea.setText(valueOrDash(consultation.getMealPlan()));
        objectivesArea.setText(valueOrDash(consultation.getObjectives()));
        medicalNotesArea.setText(valueOrDash(consultation.getMedicalNotes()));
    }

    private void applyStatusPill(Label label, String status) {
        if (label == null) return;
        label.getStyleClass().removeAll(
                "status-pill", "status-pending", "status-planned",
                "status-refused", "status-completed", "status-reschedule");
        label.getStyleClass().add("status-pill");
        if (status == null) return;
        switch (status) {
            case "PENDING":
                label.getStyleClass().add("status-pending");
                break;
            case "PLANNED":
                label.getStyleClass().add("status-planned");
                break;
            case "REFUSED":
                label.getStyleClass().add("status-refused");
                break;
            case "COMPLETED":
                label.getStyleClass().add("status-completed");
                break;
            case "RESCHEDULE_PROPOSED":
                label.getStyleClass().add("status-reschedule");
                break;
            default:
                break;
        }
    }

    private void applyReadPill(Label label, boolean read) {
        if (label == null) return;
        label.getStyleClass().removeAll("read-pill", "read-pill-yes", "read-pill-no");
        label.getStyleClass().add("read-pill");
        label.getStyleClass().add(read ? "read-pill-yes" : "read-pill-no");
    }

    private void applyHeader(User currentUser) {
        String initials = getInitials(currentUser);
        if (sidebarAvatar != null) sidebarAvatar.setText(initials);
        if (sidebarUserName != null) sidebarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (topbarAvatar != null) topbarAvatar.setText(initials);
        if (topbarUserName != null) topbarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        if (topbarDate != null) topbarDate.setText(LocalDate.now().format(formatter));
        if (heroAvatar != null) heroAvatar.setText(initials);
        if (heroPatientName != null) heroPatientName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
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
