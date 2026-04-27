package com.chroniccare.controllers;

import com.chroniccare.models.Appointment;
import com.chroniccare.models.Consultation;
import com.chroniccare.models.ConsultationDraft;
import com.chroniccare.models.OpenFoodFactsProduct;
import com.chroniccare.models.User;
import com.chroniccare.services.AppointmentService;
import com.chroniccare.services.ConsultationService;
import com.chroniccare.services.GeminiService;
import com.chroniccare.services.OpenFoodFactsService;
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
import java.util.Optional;

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
    @FXML private TextField foodSearchField;
    @FXML private Label foodSearchStatusLabel;
    @FXML private Label productNameLabel;
    @FXML private Label productBrandLabel;
    @FXML private Label productNutriScoreLabel;
    @FXML private Label productCaloriesLabel;
    @FXML private Label productSugarsLabel;
    @FXML private Label productFatLabel;
    @FXML private Label productProteinsLabel;
    @FXML private Label productIngredientsLabel;
    @FXML private Label aiStatusLabel;
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
    private final OpenFoodFactsService openFoodFactsService = new OpenFoodFactsService();
    private final GeminiService geminiService = new GeminiService();
    private Appointment currentAppointment;
    private OpenFoodFactsProduct selectedProduct;

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        applyHeader(currentUser);
        clearProductResult();
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

    @FXML
    public void handleSearchFood() {
        String searchTerm = foodSearchField.getText();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            foodSearchStatusLabel.setText("Saisissez un produit a rechercher.");
            clearProductResult();
            return;
        }

        try {
            Optional<OpenFoodFactsProduct> result = openFoodFactsService.searchFirstProduct(searchTerm);
            if (result.isEmpty()) {
                foodSearchStatusLabel.setText("Aucun produit trouve sur Open Food Facts.");
                clearProductResult();
                return;
            }
            selectedProduct = result.get();
            fillProductResult(selectedProduct);
            foodSearchStatusLabel.setText("Produit nutritionnel charge depuis Open Food Facts.");
        } catch (Exception e) {
            foodSearchStatusLabel.setText("Recherche Open Food Facts impossible : " + e.getMessage());
            clearProductResult();
        }
    }

    @FXML
    public void handleInsertFoodIntoRecommendations() {
        if (selectedProduct == null) {
            showError("Recherchez d'abord un produit Open Food Facts.");
            return;
        }
        String snippet = selectedProduct.toRecommendationSnippet();
        if (recommendationsArea.getText() == null || recommendationsArea.getText().isBlank()) {
            recommendationsArea.setText(snippet);
        } else {
            recommendationsArea.appendText(System.lineSeparator() + snippet);
        }
        successLabel.setText("Informations produit ajoutees aux recommandations.");
        errorLabel.setText("");
    }

    @FXML
    public void handleGenerateWithAi() {
        if (currentAppointment == null) {
            showError("Rendez-vous indisponible.");
            return;
        }

        aiStatusLabel.setText("Generation Gemini en cours...");
        try {
            ConsultationDraft draft = geminiService.generateConsultationDraft(
                    currentAppointment.getPatientName(),
                    currentAppointment.getMotif(),
                    currentAppointment.getFollowUpType(),
                    currentAppointment.getMedicalSnapshot(),
                    themeField.getText(),
                    summaryArea.getText(),
                    recommendationsArea.getText(),
                    mealPlanArea.getText(),
                    objectivesArea.getText(),
                    medicalNotesArea.getText(),
                    selectedProduct
            );
            applyDraft(draft);
            aiStatusLabel.setText("Brouillon Gemini genere. Verifiez puis ajustez avant enregistrement.");
            successLabel.setText("Les champs ont ete enrichis par Gemini.");
            errorLabel.setText("");
        } catch (Exception e) {
            aiStatusLabel.setText("Generation Gemini indisponible.");
            showError("Impossible de generer avec Gemini : " + e.getMessage());
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

    private void fillProductResult(OpenFoodFactsProduct product) {
        productNameLabel.setText(valueOrDash(product.getProductName()));
        productBrandLabel.setText(valueOrDash(product.getBrands()));
        productNutriScoreLabel.setText(valueOrDash(toUpper(product.getNutriScoreGrade())));
        productCaloriesLabel.setText(formatNumber(product.getEnergyKcal100g(), "kcal / 100g"));
        productSugarsLabel.setText(formatNumber(product.getSugars100g(), "g / 100g"));
        productFatLabel.setText(formatNumber(product.getFat100g(), "g / 100g"));
        productProteinsLabel.setText(formatNumber(product.getProteins100g(), "g / 100g"));
        productIngredientsLabel.setText(valueOrDash(product.getIngredientsText()));
    }

    private void applyDraft(ConsultationDraft draft) {
        if (draft == null) {
            return;
        }
        themeField.setText(valueOrBlank(draft.getTheme()));
        summaryArea.setText(valueOrBlank(draft.getSummary()));
        recommendationsArea.setText(joinWithPatientMessage(draft.getRecommendations(), draft.getFollowUpMessage()));
        mealPlanArea.setText(valueOrBlank(draft.getMealPlan()));
        objectivesArea.setText(valueOrBlank(draft.getObjectives()));
    }

    private void clearProductResult() {
        selectedProduct = null;
        productNameLabel.setText("-");
        productBrandLabel.setText("-");
        productNutriScoreLabel.setText("-");
        productCaloriesLabel.setText("-");
        productSugarsLabel.setText("-");
        productFatLabel.setText("-");
        productProteinsLabel.setText("-");
        productIngredientsLabel.setText("-");
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

    private String joinWithPatientMessage(String recommendations, String followUpMessage) {
        String firstPart = valueOrBlank(recommendations).trim();
        String secondPart = valueOrBlank(followUpMessage).trim();
        if (firstPart.isEmpty()) {
            return secondPart;
        }
        if (secondPart.isEmpty()) {
            return firstPart;
        }
        return firstPart + System.lineSeparator() + System.lineSeparator() + "Message patient :" +
                System.lineSeparator() + secondPart;
    }

    private String toUpper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private String formatNumber(Double value, String suffix) {
        if (value == null) {
            return "-";
        }
        if (Math.rint(value) == value) {
            return value.intValue() + " " + suffix;
        }
        return String.format(Locale.US, "%.1f %s", value, suffix);
    }
}
