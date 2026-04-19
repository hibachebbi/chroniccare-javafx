package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import com.chroniccare.utils.JsonUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import com.chroniccare.services.SecurityService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import com.chroniccare.services.PatientMedicalRecordService;
import com.chroniccare.services.MedicalQuestionnaireService;
import com.chroniccare.services.AIQuestionnaireService;
import com.chroniccare.services.OpenAIMedicalRecordService;
import com.chroniccare.services.MedicalRecordPdfService;
import com.chroniccare.services.MedicalAuditService;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfilePatientController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private Label statusBadge;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<String> genreCombo;
    @FXML private TextField medicalField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private VBox questionnaireBox;
    @FXML private TextArea aiResultArea;
    @FXML private TextField newConditionField;
    @FXML private VBox secondaryConditionsBox;
    @FXML private VBox recordPreviewBox;
    @FXML private TabPane recordTabs;

    private static final Pattern SECTION_PATTERN = Pattern.compile("^(\\s*\\d+\\s*[).\\-]\\s*.+|\\s*Annexe\\b.*)$", Pattern.CASE_INSENSITIVE);

    private UserService userService = new UserService();
    private SecurityService securityService = new SecurityService();
    private User currentUser;
    private PatientMedicalRecordService recordService = new PatientMedicalRecordService();
    private MedicalQuestionnaireService questionnaireService = new MedicalQuestionnaireService();
    private AIQuestionnaireService aiQuestionnaireService = new AIQuestionnaireService();
    private OpenAIMedicalRecordService aiService = new OpenAIMedicalRecordService();
    private MedicalRecordPdfService pdfService = new MedicalRecordPdfService();
    private MedicalAuditService auditService = new MedicalAuditService();
    @FXML
    public void initialize() {
        genreCombo.setItems(FXCollections.observableArrayList("Homme", "Femme"));
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        try {
            auditService.logEvent(currentUser, currentUser.getId(), "PROFILE_VIEW", "Consultation du profil patient.");
            auditService.logEvent(currentUser, currentUser.getId(), "MEDICAL_RECORD_VIEW", "Consultation du dossier médical.");
        } catch (Exception ignored) {
        }

        String initials = getInitials(currentUser);
        if (sidebarAvatar != null) sidebarAvatar.setText(initials);
        if (sidebarUserName != null) sidebarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (topbarAvatar != null) topbarAvatar.setText(initials);
        if (topbarUserName != null) topbarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (topbarDate != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            topbarDate.setText(LocalDate.now().format(fmt));
        }
        if (statusBadge != null) {
            String s = currentUser.getApprovalStatus();
            statusBadge.setText("approved".equals(s) ? "✓ Approuvé" : "pending".equals(s) ? "⏳ En attente" : "✗ Rejeté");
            statusBadge.setStyle("-fx-background-color: " +
                    ("approved".equals(s) ? "#22c55e" : "pending".equals(s) ? "#f97316" : "#ef4444") +
                    "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 11; -fx-font-weight: bold;");
        }
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        telephoneField.setText(currentUser.getTelephone() != null ? currentUser.getTelephone() : "");
        genreCombo.setValue(currentUser.getGenre());

        String mainCondition = currentUser.getMedicalCondition() != null ? currentUser.getMedicalCondition() : "";
        try {
            recordService.ensureRecordExists(currentUser.getId(), mainCondition);

            String fromRecord = recordService.getMainCondition(currentUser.getId());
            if (fromRecord != null && !fromRecord.isBlank()) {
                mainCondition = fromRecord;
            }

            String savedRecord = recordService.getGeneratedRecord(currentUser.getId());
            if (aiResultArea != null && savedRecord != null && !savedRecord.isBlank()) {
                aiResultArea.setText(savedRecord);
            }
        } catch (Exception ignored) {
        }

        medicalField.setText(mainCondition);
        if (questionnaireBox != null) {
            questionnaireBox.setVisible(false);
            questionnaireBox.setManaged(false);
        }

        if (aiResultArea != null) {
            aiResultArea.textProperty().addListener((obs, oldVal, newVal) -> refreshRecordPreview());
        }
        refreshRecordPreview();

        renderSecondaryConditions();
    }

    private boolean valider() {
        StringBuilder errors = new StringBuilder();
        if (nomField.getText().trim().isEmpty()) errors.append("• Nom obligatoire\n");
        if (prenomField.getText().trim().isEmpty()) errors.append("• Prénom obligatoire\n");
        if (!emailField.getText().matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) errors.append("• Email invalide\n");
        if (!telephoneField.getText().isEmpty() && !telephoneField.getText().matches("\\d{8}"))
            errors.append("• Téléphone : exactement 8 chiffres\n");
        if (genreCombo.getValue() == null) errors.append("• Genre obligatoire\n");
        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreurs de saisie");
            alert.setHeaderText("Veuillez corriger les erreurs :");
            alert.setContentText(errors.toString());
            alert.show();
            return false;
        }
        return true;
    }

    @FXML
    public void handleSave() {
        if (!valider()) return;
        try {
            currentUser.setNom(nomField.getText().trim());
            currentUser.setPrenom(prenomField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setTelephone(telephoneField.getText().trim());
            currentUser.setGenre(genreCombo.getValue());
            String mainCondition = medicalField.getText() == null ? "" : medicalField.getText().trim();
            currentUser.setMedicalCondition(mainCondition);
            userService.update(currentUser);
            SessionManager.getInstance().setCurrentUser(currentUser);

            try {
                recordService.ensureRecordExists(currentUser.getId(), mainCondition);
                recordService.updateMainCondition(currentUser.getId(), mainCondition);

                if (aiResultArea != null && aiResultArea.getText() != null) {
                    recordService.updateGeneratedRecord(currentUser.getId(), aiResultArea.getText());
                }

                Map<String, String> answers = new LinkedHashMap<>();
                for (Node node : questionnaireBox.getChildren()) {
                    if (node instanceof TextField field) {
                        String key = field.getUserData() instanceof String s ? s : field.getId();
                        answers.put(key, field.getText());
                    }
                }
                if (!answers.isEmpty()) {
                    recordService.saveQuestionnaireAnswers(currentUser.getId(), JsonUtils.toJson(answers));
                }
            } catch (Exception ignored) {
            }

            try {
                auditService.logEvent(currentUser, currentUser.getId(), "PROFILE_UPDATE", "Modification du profil patient.");
                auditService.logEvent(currentUser, currentUser.getId(), "MEDICAL_RECORD_UPDATE", "Enregistrement des modifications du dossier médical.");
            } catch (Exception ignored) {
            }
            if (successLabel != null) successLabel.setText("✓ Profil mis à jour avec succès !");
            if (errorLabel != null) errorLabel.setText("");
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void goToHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/home.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void loadQuestionnaire() {
        questionnaireBox.getChildren().clear();
        questionnaireBox.setVisible(false);
        questionnaireBox.setManaged(false);

        String condition = medicalField.getText() == null ? "" : medicalField.getText().trim();
        Map<String, String> savedAnswers = Map.of();

        try {
            if (condition.isEmpty()) {
                condition = recordService.getMainCondition(currentUser.getId());
            }

            recordService.ensureRecordExists(currentUser.getId(), condition);
            savedAnswers = JsonUtils.toStringMap(recordService.getQuestionnaireAnswers(currentUser.getId()));
        } catch (Exception ignored) {
        }

        if (condition == null || condition.isBlank()) {
            if (errorLabel != null) errorLabel.setText("Veuillez renseigner votre maladie puis recharger le questionnaire.");
            return;
        }

        try {
            auditService.logEvent(currentUser, currentUser.getId(), "QUESTIONNAIRE_VIEW", "Consultation du questionnaire selon la maladie.");
        } catch (Exception ignored) {
        }

        if (errorLabel != null) errorLabel.setText("");
        questionnaireBox.setVisible(true);
        questionnaireBox.setManaged(true);

        if (savedAnswers != null && !savedAnswers.isEmpty()) {
            boolean keysAreQuestions = savedAnswers.keySet().stream()
                    .anyMatch(k -> k != null && (k.contains(" ") || k.endsWith("?")));
            Map<String, String> legacyLabels = keysAreQuestions
                    ? Map.of()
                    : questionnaireService.getQuestionsForCondition(condition);

            int index = 1;
            for (Map.Entry<String, String> entry : savedAnswers.entrySet()) {
                String questionKey = entry.getKey();
                String question = keysAreQuestions
                        ? questionKey
                        : legacyLabels.getOrDefault(questionKey, questionKey);
                String answer = entry.getValue();

                Label label = new Label("Question " + index + " : " + question);
                label.getStyleClass().add("info-label");
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px; -fx-font-weight: bold;");

                TextField field = new TextField();
                field.setId("question_" + index);
                field.setUserData(question);
                field.setPromptText("Votre réponse");
                field.setMaxWidth(Double.MAX_VALUE);
                field.setStyle("-fx-pref-height: 38; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e2e8f0; -fx-border-width: 1.5;");
                if (answer != null) field.setText(answer);

                questionnaireBox.getChildren().addAll(label, field);
                index++;
            }
            return;
        }

        List<String> questions = aiQuestionnaireService.generateQuestions(condition);
        int index = 1;
        for (String question : questions) {
            Label label = new Label("Question " + index + " : " + question);
            label.getStyleClass().add("info-label");
            label.setWrapText(true);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px; -fx-font-weight: bold;");

            TextField field = new TextField();
            field.setId("question_" + index);
            field.setUserData(question);
            field.setPromptText("Votre réponse");
            field.setMaxWidth(Double.MAX_VALUE);
            field.setStyle("-fx-pref-height: 38; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e2e8f0; -fx-border-width: 1.5;");

            questionnaireBox.getChildren().addAll(label, field);
            index++;
        }
    }
    @FXML
    public void handleGenerateAI() {
        try {
            boolean hasQuestions = questionnaireBox.getChildren().stream().anyMatch(n -> n instanceof TextField);
            if (!hasQuestions) {
                loadQuestionnaire();

                boolean loaded = questionnaireBox.getChildren().stream().anyMatch(n -> n instanceof TextField);
                if (loaded) {
                    if (successLabel != null) successLabel.setText("Questionnaire généré. Remplissez-le puis cliquez à nouveau sur \"Generer dossier IA\".");
                    if (errorLabel != null) errorLabel.setText("");
                }
                return;
            }

            Map<String, String> answers = new LinkedHashMap<>();

            for (Node node : questionnaireBox.getChildren()) {
                if (node instanceof TextField field) {
                    String key = field.getUserData() instanceof String s ? s : field.getId();
                    answers.put(key, field.getText());
                }
            }

            boolean hasAnyAnswer = answers.values().stream().anyMatch(v -> v != null && !v.isBlank());
            if (!hasAnyAnswer) {
                if (errorLabel != null) errorLabel.setText("Veuillez remplir au moins une réponse du questionnaire.");
                return;
            }

            String mainCondition = medicalField.getText() == null ? "" : medicalField.getText().trim();
            if (mainCondition.isEmpty()) {
                try {
                    mainCondition = recordService.getMainCondition(currentUser.getId());
                } catch (Exception ignored) {
                }
            }

            List<String> secondary = List.of();
            String pdfText = null;

            try {
                secondary = recordService.getSecondaryConditions(currentUser.getId());
            } catch (Exception ignored) {
            }

            try {
                pdfText = recordService.getImportedPdfText(currentUser.getId());
            } catch (Exception ignored) {
            }

            String result = aiService.generateMedicalRecord(
                    currentUser.getNom() + " " + currentUser.getPrenom(),
                    mainCondition,
                    secondary,
                    answers,
                    pdfText
            );

            aiResultArea.setText(result);
            if (recordTabs != null) {
                recordTabs.getSelectionModel().select(0);
            }

            try {
                recordService.ensureRecordExists(currentUser.getId(), mainCondition);
                recordService.saveGeneratedRecord(
                        currentUser.getId(),
                        result,
                        JsonUtils.toJson(answers)
                );
            } catch (Exception ignored) {
            }

            try {
                auditService.logEvent(currentUser, currentUser.getId(), "MEDICAL_RECORD_UPDATE", "Dossier médical généré par IA.");
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderSecondaryConditions() {
        if (secondaryConditionsBox == null || currentUser == null) return;

        secondaryConditionsBox.getChildren().clear();

        List<String> conditions = List.of();
        try {
            conditions = recordService.getSecondaryConditions(currentUser.getId());
        } catch (Exception ignored) {
        }

        if (conditions == null || conditions.isEmpty()) {
            Label empty = new Label("Aucune");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            secondaryConditionsBox.getChildren().add(empty);
            return;
        }

        for (String condition : conditions) {
            if (condition == null || condition.isBlank()) continue;

            Label label = new Label(condition);
            label.setStyle(
                    "-fx-background-color: #f1f5f9; -fx-text-fill: #0f172a; -fx-padding: 6 10; " +
                            "-fx-background-radius: 999; -fx-border-color: #e2e8f0; -fx-border-radius: 999;"
            );

            Button removeBtn = new Button("×");
            removeBtn.setFocusTraversable(false);
            removeBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #dc2626; -fx-font-size: 14px; " +
                            "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 6;"
            );
            removeBtn.setOnAction(evt -> {
                try {
                    recordService.removeCondition(currentUser.getId(), condition);
                    renderSecondaryConditions();
                    if (successLabel != null) successLabel.setText("Maladie supprimée.");
                    if (errorLabel != null) errorLabel.setText("");

                    try {
                        auditService.logEvent(currentUser, currentUser.getId(), "MEDICAL_RECORD_UPDATE", "Suppression d'une maladie secondaire : " + condition);
                    } catch (Exception ignored) {
                    }
                } catch (Exception e) {
                    if (errorLabel != null) errorLabel.setText("Erreur : " + e.getMessage());
                }
            });

            HBox row = new HBox(6, label, removeBtn);
            row.setAlignment(Pos.CENTER_LEFT);
            secondaryConditionsBox.getChildren().add(row);
        }
    }

    private void refreshRecordPreview() {
        if (recordPreviewBox == null || aiResultArea == null) return;

        recordPreviewBox.getChildren().clear();

        String content = aiResultArea.getText();
        if (content == null || content.isBlank()) {
            Label placeholder = new Label("Aucun dossier. Cliquez sur \"Generer dossier IA\" pour le créer.");
            placeholder.setWrapText(true);
            placeholder.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            recordPreviewBox.getChildren().add(placeholder);
            return;
        }

        String[] lines = content.split("\\R");
        int index = 0;
        while (index < lines.length && lines[index].trim().isEmpty()) index++;

        if (index < lines.length) {
            String first = lines[index].trim();
            if (!isSectionHeader(first) && !isBullet(first)) {
                Label title = new Label(first);
                title.setWrapText(true);
                title.setMaxWidth(Double.MAX_VALUE);
                title.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: bold;");
                recordPreviewBox.getChildren().add(title);
                index++;
            }
        }

        VBox currentSection = null;

        for (; index < lines.length; index++) {
            String raw = lines[index] == null ? "" : lines[index];
            String line = raw.trim();

            if (line.isEmpty()) {
                Region spacer = new Region();
                spacer.setMinHeight(6);
                if (currentSection != null) currentSection.getChildren().add(spacer);
                else recordPreviewBox.getChildren().add(spacer);
                continue;
            }

            if (isSectionHeader(line)) {
                currentSection = new VBox(8);
                currentSection.setMaxWidth(Double.MAX_VALUE);
                currentSection.setStyle(
                        "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; " +
                                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12;"
                );

                Label sectionTitle = new Label(line);
                sectionTitle.setWrapText(true);
                sectionTitle.setMaxWidth(Double.MAX_VALUE);
                sectionTitle.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 13px; -fx-font-weight: bold;");
                currentSection.getChildren().add(sectionTitle);
                recordPreviewBox.getChildren().add(currentSection);
                continue;
            }

            Node node = isBullet(line) ? buildBulletNode(line) : buildParagraphNode(line);
            if (currentSection != null) {
                currentSection.getChildren().add(node);
            } else {
                recordPreviewBox.getChildren().add(node);
            }
        }
    }

    private boolean isSectionHeader(String line) {
        return line != null && SECTION_PATTERN.matcher(line).matches();
    }

    private boolean isBullet(String line) {
        if (line == null) return false;
        String t = line.trim();
        return t.startsWith("-") || t.startsWith("•");
    }

    private Node buildParagraphNode(String line) {
        Label label = new Label(line);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
        return label;
    }

    private Node buildBulletNode(String line) {
        String text = line.trim().replaceFirst("^[-•]\\s*", "");

        Label dot = new Label("•");
        dot.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-padding: 0 6 0 0;");

        Label body = new Label(text);
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);
        body.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox row = new HBox(4, dot, body);
        row.setAlignment(Pos.TOP_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    @FXML
    public void handleExportPdf() {
        try {
            String content = aiResultArea.getText();

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter le dossier en PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            chooser.setInitialFileName("medical_" + currentUser.getId() + ".pdf");

            File file = chooser.showSaveDialog(nomField.getScene().getWindow());
            if (file == null) return;

            String path = file.getAbsolutePath();

            String patientName = (currentUser.getPrenom() + " " + currentUser.getNom()).trim();
            String mainCondition = medicalField.getText() == null ? "" : medicalField.getText().trim();
            List<String> secondary = List.of();
            try {
                secondary = recordService.getSecondaryConditions(currentUser.getId());
            } catch (Exception ignored) {
            }

            pdfService.exportToPdf(content, path, patientName, mainCondition, secondary);

            if (successLabel != null) successLabel.setText("PDF exporté.");
            if (errorLabel != null) errorLabel.setText("");

            try {
                auditService.logEvent(currentUser, currentUser.getId(), "MEDICAL_RECORD_PDF_EXPORT", "Export du dossier médical en PDF.");
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleImportPdf() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Importer un PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File file = chooser.showOpenDialog(nomField.getScene().getWindow());

            if (file != null) {
                String text = pdfService.importPdfText(file.getAbsolutePath());
                String mainCondition = medicalField.getText() == null ? "" : medicalField.getText().trim();
                recordService.ensureRecordExists(currentUser.getId(), mainCondition);
                recordService.saveImportedPdfText(currentUser.getId(), text);
                if (successLabel != null) successLabel.setText("PDF importé.");
                if (errorLabel != null) errorLabel.setText("");

                try {
                    auditService.logEvent(currentUser, currentUser.getId(), "MEDICAL_RECORD_PDF_IMPORT", "Import d'un PDF dans le dossier médical.");
                } catch (Exception ignored) {
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @FXML
    public void handleAddCondition() {
        try {
            String condition = newConditionField.getText() == null ? "" : newConditionField.getText().trim();
            if (condition.isEmpty()) {
                if (errorLabel != null) errorLabel.setText("Veuillez saisir une maladie.");
                return;
            }

            String mainCondition = medicalField.getText() == null ? "" : medicalField.getText().trim();
            if (!mainCondition.isBlank() && condition.equalsIgnoreCase(mainCondition)) {
                if (errorLabel != null) errorLabel.setText("Cette maladie est déjà votre condition principale.");
                return;
            }

            try {
                List<String> existing = recordService.getSecondaryConditions(currentUser.getId());
                boolean alreadyExists = existing.stream().anyMatch(c -> c != null && c.equalsIgnoreCase(condition));
                if (alreadyExists) {
                    if (errorLabel != null) errorLabel.setText("Cette maladie est déjà ajoutée.");
                    return;
                }
            } catch (Exception ignored) {
            }

            recordService.addCondition(currentUser.getId(), condition);
            newConditionField.clear();
            renderSecondaryConditions();
            if (successLabel != null) successLabel.setText("Maladie ajoutée.");
            if (errorLabel != null) errorLabel.setText("");

            try {
                auditService.logEvent(currentUser, currentUser.getId(), "MEDICAL_RECORD_UPDATE", "Ajout d'une maladie secondaire : " + condition);
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String getInitials(User user) {
        String p = (user.getPrenom() != null && !user.getPrenom().isEmpty())
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String n = (user.getNom() != null && !user.getNom().isEmpty())
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return p + n;
    }
}
