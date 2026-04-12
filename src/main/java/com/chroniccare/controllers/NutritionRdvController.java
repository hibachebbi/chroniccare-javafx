package com.chroniccare.controllers;

import com.chroniccare.models.Appointment;
import com.chroniccare.models.User;
import com.chroniccare.services.AppointmentService;
import com.chroniccare.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class NutritionRdvController {
    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, String> colPatient;
    @FXML private TableColumn<Appointment, String> colMotif;
    @FXML private TableColumn<Appointment, String> colRequested;
    @FXML private TableColumn<Appointment, String> colUrgency;
    @FXML private TableColumn<Appointment, String> colStatus;
    @FXML private Label patientLabel;
    @FXML private Label medicalContextLabel;
    @FXML private Label followUpLabel;
    @FXML private Label statusLabel;
    @FXML private TextArea motifArea;
    @FXML private TextArea responseArea;
    @FXML private DatePicker decisionDatePicker;
    @FXML private ComboBox<String> decisionTimeCombo;
    @FXML private Button acceptButton;
    @FXML private Button refuseButton;
    @FXML private Button proposeButton;
    @FXML private Button openConsultationButton;

    private final AppointmentService appointmentService = new AppointmentService();

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        applyHeader(currentUser);
        configureTable();
        decisionTimeCombo.setItems(FXCollections.observableArrayList(
                "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
                "11:00", "11:30", "12:00", "12:30", "13:00", "13:30",
                "14:00", "14:30", "15:00", "15:30", "16:00", "16:30",
                "17:00", "17:30"
        ));

        try {
            loadAppointments();
        } catch (Exception e) {
            showError("Impossible de charger les demandes : " + e.getMessage());
        }
    }

    @FXML
    public void handleAccept() {
        Appointment appointment = appointmentTable.getSelectionModel().getSelectedItem();
        if (appointment == null) {
            showError("Selectionnez une demande.");
            return;
        }
        LocalDateTime slot = resolveDecisionSlot(appointment);
        if (slot == null) return;

        try {
            appointmentService.acceptAppointment(appointment.getId(), slot, responseArea.getText().trim());
            showSuccess("RDV accepte et planifie.");
            loadAppointments();
        } catch (Exception e) {
            showError("Impossible d'accepter le RDV : " + e.getMessage());
        }
    }

    @FXML
    public void handleRefuse() {
        Appointment appointment = appointmentTable.getSelectionModel().getSelectedItem();
        if (appointment == null) {
            showError("Selectionnez une demande.");
            return;
        }
        try {
            appointmentService.refuseAppointment(appointment.getId(), responseArea.getText().trim());
            showSuccess("Demande refusee.");
            loadAppointments();
        } catch (Exception e) {
            showError("Impossible de refuser le RDV : " + e.getMessage());
        }
    }

    @FXML
    public void handleProposeAnotherSlot() {
        Appointment appointment = appointmentTable.getSelectionModel().getSelectedItem();
        if (appointment == null) {
            showError("Selectionnez une demande.");
            return;
        }
        LocalDateTime slot = resolveDecisionSlot(null);
        if (slot == null) return;

        try {
            appointmentService.proposeReschedule(appointment.getId(), slot, responseArea.getText().trim());
            showSuccess("Un autre creneau a ete propose.");
            loadAppointments();
        } catch (Exception e) {
            showError("Impossible de proposer un creneau : " + e.getMessage());
        }
    }

    @FXML
    public void handleOpenConsultation() {
        Appointment appointment = appointmentTable.getSelectionModel().getSelectedItem();
        if (appointment == null) {
            showError("Selectionnez un rendez-vous.");
            return;
        }
        if (!Appointment.STATUS_PLANNED.equals(appointment.getStatus())
                && !Appointment.STATUS_COMPLETED.equals(appointment.getStatus())) {
            showError("La consultation est disponible uniquement pour un RDV planifie ou termine.");
            return;
        }
        SessionManager.getInstance().setSelectedAppointmentId(appointment.getId());
        navigate("/com/chroniccare/nutrition-consultation.fxml");
    }

    @FXML public void goToHome() { navigate("/com/chroniccare/home.fxml"); }
    @FXML public void goToProfile() { navigate("/com/chroniccare/profile-nutritionniste.fxml"); }
    @FXML public void goToRdv() { navigate("/com/chroniccare/nutrition-rdv.fxml"); }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        navigate("/com/chroniccare/login.fxml");
    }

    private void configureTable() {
        colPatient.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPatientName()));
        colMotif.setCellValueFactory(data -> new SimpleStringProperty(shorten(data.getValue().getMotif(), 40)));
        colRequested.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRequestedDateTimeDisplay()));
        colUrgency.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getUrgencyLevel())));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusLabel()));
        appointmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> showAppointment(newValue));
    }

    private void loadAppointments() throws Exception {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        List<Appointment> appointments = appointmentService.getAppointmentsForNutritionist(currentUser.getId());
        appointmentTable.setItems(FXCollections.observableArrayList(appointments));
        if (!appointments.isEmpty()) {
            appointmentTable.getSelectionModel().selectFirst();
        } else {
            showAppointment(null);
        }
    }

    private void showAppointment(Appointment appointment) {
        boolean hasAppointment = appointment != null;
        acceptButton.setDisable(!hasAppointment);
        refuseButton.setDisable(!hasAppointment);
        proposeButton.setDisable(!hasAppointment);
        openConsultationButton.setDisable(!hasAppointment);
        if (!hasAppointment) {
            patientLabel.setText("-");
            medicalContextLabel.setText("-");
            followUpLabel.setText("-");
            statusLabel.setText("-");
            motifArea.clear();
            responseArea.clear();
            return;
        }
        patientLabel.setText(valueOrDash(appointment.getPatientName()));
        medicalContextLabel.setText(valueOrDash(appointment.getMedicalSnapshot()));
        followUpLabel.setText(valueOrDash(appointment.getFollowUpType()));
        statusLabel.setText(appointment.getStatusLabel());
        motifArea.setText(valueOrDash(appointment.getMotif()));
        responseArea.setText(valueOrDash(appointment.getNutritionistResponse()));
        if (appointment.getScheduledDateTime() != null) {
            decisionDatePicker.setValue(appointment.getScheduledDateTime().toLocalDate());
            decisionTimeCombo.setValue(appointment.getScheduledDateTime().toLocalTime().toString());
        } else if (appointment.getRequestedDateTime() != null) {
            decisionDatePicker.setValue(appointment.getRequestedDateTime().toLocalDate());
            decisionTimeCombo.setValue(appointment.getRequestedDateTime().toLocalTime().toString());
        } else {
            decisionDatePicker.setValue(null);
            decisionTimeCombo.setValue(null);
        }
    }

    private LocalDateTime resolveDecisionSlot(Appointment fallbackAppointment) {
        LocalDate date = decisionDatePicker.getValue();
        String time = decisionTimeCombo.getValue();
        if (date == null || time == null) {
            if (fallbackAppointment != null && fallbackAppointment.getRequestedDateTime() != null) {
                return fallbackAppointment.getRequestedDateTime();
            }
            showError("Selectionnez une date et une heure.");
            return null;
        }
        return LocalDateTime.of(date, LocalTime.parse(time));
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

    private String shorten(String value, int maxLength) {
        if (value == null || value.isBlank()) return "-";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }
}
