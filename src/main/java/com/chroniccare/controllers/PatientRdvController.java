package com.chroniccare.controllers;

import com.chroniccare.models.Appointment;
import com.chroniccare.models.HolidayInfo;
import com.chroniccare.models.User;
import com.chroniccare.services.AppointmentService;
import com.chroniccare.services.HolidayService;
import com.chroniccare.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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
import java.util.Optional;

public class PatientRdvController {
    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private Label medicalSummaryLabel;
    @FXML private ComboBox<User> nutritionistCombo;
    @FXML private TextArea motifArea;
    @FXML private DatePicker requestedDatePicker;
    @FXML private ComboBox<String> requestedTimeCombo;
    @FXML private ComboBox<String> urgencyCombo;
    @FXML private ComboBox<String> followUpCombo;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, String> colNutritionist;
    @FXML private TableColumn<Appointment, String> colRequested;
    @FXML private TableColumn<Appointment, String> colStatus;
    @FXML private TableColumn<Appointment, String> colUrgency;
    @FXML private Label selectedStatusLabel;
    @FXML private Label selectedNutritionistLabel;
    @FXML private Label selectedRequestedLabel;
    @FXML private Label selectedScheduledLabel;
    @FXML private Label selectedFollowUpLabel;
    @FXML private TextArea selectedResponseArea;

    private final AppointmentService appointmentService = new AppointmentService();
    private final HolidayService holidayService = new HolidayService();

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        applyHeader(currentUser);
        configureCombos();
        configureTable();
        medicalSummaryLabel.setText(currentUser.getMedicalCondition() == null || currentUser.getMedicalCondition().isBlank()
                ? "Aucune condition medicale renseignee."
                : currentUser.getMedicalCondition());

        try {
            List<User> nutritionists = appointmentService.getNutritionists();
            nutritionistCombo.setItems(FXCollections.observableArrayList(nutritionists));
            if (!nutritionists.isEmpty()) {
                nutritionistCombo.getSelectionModel().selectFirst();
            }
            loadAppointments();
        } catch (Exception e) {
            showError("Impossible de charger les rendez-vous : " + e.getMessage());
        }
    }

    @FXML
    public void handleSubmit() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        clearMessages();

        if (nutritionistCombo.getValue() == null) {
            showError("Selectionnez un nutritionniste.");
            return;
        }
        if (motifArea.getText() == null || motifArea.getText().trim().isEmpty()) {
            showError("Le motif est obligatoire.");
            return;
        }
        if (requestedDatePicker.getValue() == null || requestedTimeCombo.getValue() == null) {
            showError("Renseignez une date et une heure souhaitees.");
            return;
        }

        LocalDateTime requestedDateTime = LocalDateTime.of(
                requestedDatePicker.getValue(),
                LocalTime.parse(requestedTimeCombo.getValue())
        );
        if (requestedDateTime.isBefore(LocalDateTime.now())) {
            showError("Le creneau souhaite doit etre dans le futur.");
            return;
        }
        String availabilityError = validateRequestedSlot(requestedDateTime);
        if (availabilityError != null) {
            showError(availabilityError);
            return;
        }

        try {
            appointmentService.createAppointment(
                    currentUser.getId(),
                    nutritionistCombo.getValue().getId(),
                    motifArea.getText().trim(),
                    requestedDateTime,
                    urgencyCombo.getValue(),
                    followUpCombo.getValue(),
                    currentUser.getMedicalCondition()
            );
            showSuccess("Demande envoyee. Statut initial : En attente.");
            motifArea.clear();
            requestedDatePicker.setValue(null);
            requestedTimeCombo.getSelectionModel().clearSelection();
            urgencyCombo.getSelectionModel().clearSelection();
            followUpCombo.getSelectionModel().clearSelection();
            loadAppointments();
        } catch (Exception e) {
            showError("Echec de creation du rendez-vous : " + e.getMessage());
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

    private void configureCombos() {
        nutritionistCombo.setCellFactory(list -> new UserListCell());
        nutritionistCombo.setButtonCell(new UserListCell());
        requestedTimeCombo.setItems(FXCollections.observableArrayList(
                "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
                "11:00", "11:30", "12:00", "12:30", "13:00", "13:30",
                "14:00", "14:30", "15:00", "15:30", "16:00", "16:30",
                "17:00", "17:30"
        ));
        urgencyCombo.setItems(FXCollections.observableArrayList("Faible", "Normale", "Elevee"));
        followUpCombo.setItems(FXCollections.observableArrayList(
                "Premier bilan", "Suivi regulier", "Suivi post-consultation", "Urgence nutritionnelle"
        ));
    }

    private void configureTable() {
        colNutritionist.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNutritionistName()));
        colRequested.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRequestedDateTimeDisplay()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusLabel()));
        colUrgency.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getUrgencyLevel())));
        appointmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> showSelectedAppointment(newValue));
    }

    private void loadAppointments() throws Exception {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        List<Appointment> appointments = appointmentService.getAppointmentsForPatient(currentUser.getId());
        appointmentTable.setItems(FXCollections.observableArrayList(appointments));
        if (!appointments.isEmpty()) {
            appointmentTable.getSelectionModel().selectFirst();
        } else {
            showSelectedAppointment(null);
        }
    }

    private void showSelectedAppointment(Appointment appointment) {
        if (appointment == null) {
            selectedStatusLabel.setText("-");
            selectedNutritionistLabel.setText("-");
            selectedRequestedLabel.setText("-");
            selectedScheduledLabel.setText("-");
            selectedFollowUpLabel.setText("-");
            selectedResponseArea.setText("");
            return;
        }
        selectedStatusLabel.setText(appointment.getStatusLabel());
        selectedNutritionistLabel.setText(valueOrDash(appointment.getNutritionistName()));
        selectedRequestedLabel.setText(appointment.getRequestedDateTimeDisplay());
        selectedScheduledLabel.setText(appointment.getScheduledDateTimeDisplay());
        selectedFollowUpLabel.setText(valueOrDash(appointment.getFollowUpType()));
        selectedResponseArea.setText(valueOrDash(appointment.getNutritionistResponse()));
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

    private void clearMessages() {
        errorLabel.setText("");
        successLabel.setText("");
    }

    private String validateRequestedSlot(LocalDateTime requestedDateTime) {
        LocalDate date = requestedDateTime.toLocalDate();
        if (holidayService.isWeekend(date)) {
            return "Les rendez-vous ne sont pas disponibles le week-end.";
        }
        try {
            Optional<HolidayInfo> holiday = holidayService.findHoliday(date);
            if (holiday.isPresent()) {
                String holidayName = holiday.get().getLocalName() != null
                        ? holiday.get().getLocalName()
                        : holiday.get().getName();
                return "La date choisie est un jour ferie : " + valueOrDash(holidayName) + ".";
            }
        } catch (Exception e) {
            return "Impossible de verifier les jours feries : " + e.getMessage();
        }
        return null;
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

    private static class UserListCell extends ListCell<User> {
        @Override
        protected void updateItem(User item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? "" : item.getPrenom() + " " + item.getNom());
        }
    }
}
