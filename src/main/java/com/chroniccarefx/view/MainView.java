package com.chroniccarefx.view;

import com.chroniccarefx.config.Db;
import com.chroniccarefx.controller.ActiviteController;
import com.chroniccarefx.controller.EtatController;
import com.chroniccarefx.model.Activite;
import com.chroniccarefx.model.Etat;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class MainView {
    private final EtatController etatController;
    private final ActiviteController activiteController;

    private final Map<Long, String> userEmailById = new HashMap<>();

    private TableView<Etat> etatTable;
    private TextField etatUtilisateurIdField;
    private TextField etatTraitementField;
    private TextArea etatRemarquesField;
    private ComboBox<String> etatTemperatureCombo;
    private ComboBox<String> etatHydratationCombo;
    private DatePicker etatDateReleveField;

    private TableView<Activite> activiteTable;
    private ComboBox<PatientOption> activitePatientCombo;
    private Label activiteEtatLabel;
    private Long activiteSelectedEtatId;
    private ComboBox<String> activiteTypeCombo;
    private TextField activiteDureeField;
    private TextField activiteCaloriesField;
    private TextField activiteDistanceField;
    private TextField activiteReposField;
    private DatePicker activiteDateField;
    private TextArea activiteNotesField;

    public MainView(EtatController etatController, ActiviteController activiteController) {
        this.etatController = etatController;
        this.activiteController = activiteController;
    }

    public Parent build() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        TabPane tabPane = new TabPane(buildEtatTab(), buildActiviteTab());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        installInputConstraints();

        root.setTop(new Label("Gestion Suivi - Structure alignee sur entities Symfony"));
        root.setCenter(tabPane);

        loadPatientsFromDatabase();
        refreshEtatData();
        refreshActiviteData();
        return root;
    }

    private Tab buildEtatTab() {
        etatTable = new TableView<>();

        TableColumn<Etat, String> userCol = new TableColumn<>("Patient");
        userCol.setCellValueFactory(data -> new SimpleStringProperty(resolveUserEmail(data.getValue().getUtilisateurId())));

        TableColumn<Etat, String> trCol = new TableColumn<>("Traitement");
        trCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTraitementEnCours()));

        TableColumn<Etat, String> remCol = new TableColumn<>("Remarques");
        remCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRemarquesCliniques()));

        TableColumn<Etat, String> tempCol = new TableColumn<>("Temperature");
        tempCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTemperatureCorporelle()));

        TableColumn<Etat, String> hydCol = new TableColumn<>("Hydratation");
        hydCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNiveauHydratation()));

        TableColumn<Etat, LocalDateTime> dateCol = new TableColumn<>("Date releve");
        dateCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDateReleve()));

        etatTable.getColumns().addAll(userCol, trCol, remCol, tempCol, hydCol, dateCol);
        etatTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        etatUtilisateurIdField = new TextField();
        etatTraitementField = new TextField();
        etatRemarquesField = new TextArea();
        etatRemarquesField.setPrefRowCount(3);
        etatTemperatureCombo = new ComboBox<>();
        etatTemperatureCombo.getItems().setAll(Etat.TEMPERATURE_CORPORELLE_CHOICES);
        etatHydratationCombo = new ComboBox<>();
        etatHydratationCombo.getItems().setAll(Etat.NIVEAU_HYDRATATION_CHOICES);
        etatDateReleveField = new DatePicker(LocalDate.now());

        Button addBtn = new Button("Ajouter");
        Button updateBtn = new Button("Modifier");
        Button deleteBtn = new Button("Supprimer");

        addBtn.setOnAction(e -> runAction(() -> {
            EtatFormData data = validateEtatForm();
            etatController.createEtat(
                    data.utilisateurId(),
                    data.traitementEnCours(),
                    data.remarquesCliniques(),
                    data.temperatureCorporelle(),
                    data.niveauHydratation(),
                    data.dateReleve()
            );
            clearEtatForm();
            refreshEtatData();
            refreshActiviteData();
        }));

        updateBtn.setOnAction(e -> runAction(() -> {
            Etat selected = selectedEtat();
            EtatFormData data = validateEtatForm();
            etatController.updateEtat(
                    selected.getId(),
                    data.utilisateurId(),
                    data.traitementEnCours(),
                    data.remarquesCliniques(),
                    data.temperatureCorporelle(),
                    data.niveauHydratation(),
                    data.dateReleve()
            );
            clearEtatForm();
            refreshEtatData();
            refreshActiviteData();
        }));

        deleteBtn.setOnAction(e -> runAction(() -> {
            Etat selected = selectedEtat();
            if (!confirmDelete("Confirmer suppression", "Supprimer cet etat ?")) {
                return;
            }
            etatController.deleteEtat(selected.getId());
            clearEtatForm();
            refreshEtatData();
            refreshActiviteData();
        }));

        etatTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                etatUtilisateurIdField.setText(String.valueOf(selected.getUtilisateurId()));
                etatTraitementField.setText(selected.getTraitementEnCours());
                etatRemarquesField.setText(selected.getRemarquesCliniques());
                etatTemperatureCombo.setValue(selected.getTemperatureCorporelle());
                etatHydratationCombo.setValue(selected.getNiveauHydratation());
                etatDateReleveField.setValue(selected.getDateReleve().toLocalDate());
            }
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("User ID"), etatUtilisateurIdField);
        form.addRow(1, new Label("Traitement"), etatTraitementField);
        form.addRow(2, new Label("Remarques"), etatRemarquesField);
        form.addRow(3, new Label("Temperature"), etatTemperatureCombo);
        form.addRow(4, new Label("Hydratation"), etatHydratationCombo);
        form.addRow(5, new Label("Date releve"), etatDateReleveField);

        GridPane.setHgrow(etatUtilisateurIdField, Priority.ALWAYS);
        GridPane.setHgrow(etatTraitementField, Priority.ALWAYS);
        GridPane.setHgrow(etatRemarquesField, Priority.ALWAYS);
        GridPane.setHgrow(etatTemperatureCombo, Priority.ALWAYS);
        GridPane.setHgrow(etatHydratationCombo, Priority.ALWAYS);

        HBox buttons = new HBox(10, addBtn, updateBtn, deleteBtn);
        VBox content = new VBox(12, form, buttons, etatTable);
        VBox.setVgrow(etatTable, Priority.ALWAYS);

        return new Tab("Etat", content);
    }

    private Tab buildActiviteTab() {
        activiteTable = new TableView<>();

        TableColumn<Activite, String> userCol = new TableColumn<>("Patient");
        userCol.setCellValueFactory(data -> new SimpleStringProperty(resolveUserEmail(data.getValue().getUtilisateurId())));

        TableColumn<Activite, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));

        TableColumn<Activite, Number> dureeCol = new TableColumn<>("Duree");
        dureeCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDuree()));

        TableColumn<Activite, Number> calCol = new TableColumn<>("Calories");
        calCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCalories()));

        TableColumn<Activite, Number> distCol = new TableColumn<>("Distance KM");
        distCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDistanceKm()));

        TableColumn<Activite, Number> reposCol = new TableColumn<>("Heures repos");
        reposCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getHeuresRepos()));

        TableColumn<Activite, LocalDateTime> dateCol = new TableColumn<>("Date activite");
        dateCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDateActivite()));

        TableColumn<Activite, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotes()));

        TableColumn<Activite, LocalDateTime> createdCol = new TableColumn<>("Created At");
        createdCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCreatedAt()));

        TableColumn<Activite, String> etatCol = new TableColumn<>("Etat");
        etatCol.setCellValueFactory(data -> new SimpleStringProperty(findEtatLabel(data.getValue().getEtatId())));

        activiteTable.getColumns().addAll(
                userCol, typeCol, dureeCol, calCol, distCol, reposCol, dateCol, notesCol, createdCol, etatCol
        );
        activiteTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        activitePatientCombo = new ComboBox<>();
        activiteEtatLabel = new Label("-");
        activiteTypeCombo = new ComboBox<>();
        activiteTypeCombo.getItems().setAll(Activite.TYPE_CHOICES);
        activiteDureeField = new TextField();
        activiteCaloriesField = new TextField();
        activiteDistanceField = new TextField();
        activiteReposField = new TextField();
        activiteDateField = new DatePicker(LocalDate.now());
        activiteNotesField = new TextArea();
        activiteNotesField.setPrefRowCount(3);

        activitePatientCombo.valueProperty().addListener((obs, oldValue, selected) -> updateEtatLabelForPatient(selected));

        Button addBtn = new Button("Ajouter");
        Button updateBtn = new Button("Modifier");
        Button deleteBtn = new Button("Supprimer");

        addBtn.setOnAction(e -> runAction(() -> {
            ActiviteFormData data = validateActiviteForm();
            activiteController.createActivite(
                    data.utilisateurId(),
                    activiteSelectedEtatId,
                    data.type(),
                    data.duree(),
                    data.calories(),
                    data.distanceKm(),
                    data.heuresRepos(),
                    data.dateActivite(),
                    data.notes()
            );
            clearActiviteForm();
            refreshActiviteData();
        }));

        updateBtn.setOnAction(e -> runAction(() -> {
            Activite selected = selectedActivite();
            ActiviteFormData data = validateActiviteForm();
            activiteController.updateActivite(
                    selected.getId(),
                    data.utilisateurId(),
                    activiteSelectedEtatId,
                    data.type(),
                    data.duree(),
                    data.calories(),
                    data.distanceKm(),
                    data.heuresRepos(),
                    data.dateActivite(),
                    data.notes()
            );
            clearActiviteForm();
            refreshActiviteData();
        }));

        deleteBtn.setOnAction(e -> runAction(() -> {
            Activite selected = selectedActivite();
            if (!confirmDelete("Confirmer suppression", "Supprimer cette activite ?")) {
                return;
            }
            activiteController.deleteActivite(selected.getId());
            clearActiviteForm();
            refreshActiviteData();
        }));

        activiteTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                selectPatientInCombo(selected.getUtilisateurId());
                activiteTypeCombo.setValue(selected.getType());
                activiteDureeField.setText(String.valueOf(selected.getDuree()));
                activiteCaloriesField.setText(String.valueOf(selected.getCalories()));
                activiteDistanceField.setText(selected.getDistanceKm() == null ? "" : String.valueOf(selected.getDistanceKm()));
                activiteReposField.setText(selected.getHeuresRepos() == null ? "" : String.valueOf(selected.getHeuresRepos()));
                activiteDateField.setValue(selected.getDateActivite().toLocalDate());
                activiteNotesField.setText(selected.getNotes());
            }
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Patient"), activitePatientCombo);
        form.addRow(1, new Label("Etat patient"), activiteEtatLabel);
        form.addRow(2, new Label("Type"), activiteTypeCombo);
        form.addRow(3, new Label("Duree"), activiteDureeField);
        form.addRow(4, new Label("Calories"), activiteCaloriesField);
        form.addRow(5, new Label("Distance KM"), activiteDistanceField);
        form.addRow(6, new Label("Heures repos"), activiteReposField);
        form.addRow(7, new Label("Date activite"), activiteDateField);
        form.addRow(8, new Label("Notes"), activiteNotesField);

        GridPane.setHgrow(activitePatientCombo, Priority.ALWAYS);
        GridPane.setHgrow(activiteTypeCombo, Priority.ALWAYS);
        GridPane.setHgrow(activiteNotesField, Priority.ALWAYS);

        HBox buttons = new HBox(10, addBtn, updateBtn, deleteBtn);
        VBox content = new VBox(12, form, buttons, activiteTable);
        VBox.setVgrow(activiteTable, Priority.ALWAYS);

        return new Tab("Activite", content);
    }

    private void refreshEtatData() {
        etatTable.getItems().setAll(etatController.listEtats());
    }

    private void refreshActiviteData() {
        loadPatientsFromDatabase();
        activiteTable.getItems().setAll(activiteController.listActivites());
    }

    private void loadPatientsFromDatabase() {
        String sql = "SELECT id, email, roles FROM users ORDER BY email";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                userEmailById.clear();
                activitePatientCombo.getItems().clear();
                while (rs.next()) {
                    String roles = rs.getString("roles");
                    if (roles == null || !roles.contains("ROLE_PATIENT")) {
                        continue;
                    }
                    long id = rs.getLong("id");
                    String email = rs.getString("email");
                    userEmailById.put(id, email);
                    activitePatientCombo.getItems().add(new PatientOption(id, email));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de charger la liste des patients: " + ex.getMessage(), ex);
        }
    }

    private void updateEtatLabelForPatient(PatientOption patient) {
        if (patient == null) {
            activiteSelectedEtatId = null;
            activiteEtatLabel.setText("-");
            return;
        }

        Optional<Etat> latestEtat = etatController.listEtats().stream()
                .filter(e -> patient.id().equals(e.getUtilisateurId()))
                .max(Comparator.comparing(Etat::getDateReleve).thenComparing(Etat::getId));

        if (latestEtat.isPresent()) {
            Etat etat = latestEtat.get();
            activiteSelectedEtatId = etat.getId();
            activiteEtatLabel.setText(
                    "Etat: " + valueOrDash(etat.getTraitementEnCours())
                            + " | Temp: " + valueOrDash(etat.getTemperatureCorporelle())
                            + " | Hyd: " + valueOrDash(etat.getNiveauHydratation())
            );
        } else {
            activiteSelectedEtatId = null;
            activiteEtatLabel.setText("Aucun etat pour ce patient");
        }
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Etat selectedEtat() {
        Etat selected = etatTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            throw new IllegalStateException("Choisir un etat dans la table.");
        }
        return selected;
    }

    private Activite selectedActivite() {
        Activite selected = activiteTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            throw new IllegalStateException("Choisir une activite dans la table.");
        }
        return selected;
    }

    private PatientOption selectedPatient() {
        PatientOption selected = activitePatientCombo.getValue();
        if (selected == null) {
            throw new IllegalStateException("Choisir un patient.");
        }
        return selected;
    }

    private void clearEtatForm() {
        etatTable.getSelectionModel().clearSelection();
        etatUtilisateurIdField.clear();
        etatTraitementField.clear();
        etatRemarquesField.clear();
        etatTemperatureCombo.getSelectionModel().clearSelection();
        etatHydratationCombo.getSelectionModel().clearSelection();
        etatDateReleveField.setValue(LocalDate.now());
    }

    private void clearActiviteForm() {
        activiteTable.getSelectionModel().clearSelection();
        activitePatientCombo.getSelectionModel().clearSelection();
        activiteSelectedEtatId = null;
        activiteEtatLabel.setText("-");
        activiteTypeCombo.getSelectionModel().clearSelection();
        activiteDureeField.clear();
        activiteCaloriesField.clear();
        activiteDistanceField.clear();
        activiteReposField.clear();
        activiteDateField.setValue(LocalDate.now());
        activiteNotesField.clear();
    }

    private void selectPatientInCombo(Long userId) {
        if (userId == null) {
            activitePatientCombo.getSelectionModel().clearSelection();
            return;
        }

        for (PatientOption patient : activitePatientCombo.getItems()) {
            if (patient.id().equals(userId)) {
                activitePatientCombo.getSelectionModel().select(patient);
                return;
            }
        }
        activitePatientCombo.getSelectionModel().clearSelection();
    }

    private String findEtatLabel(Long etatId) {
        if (etatId == null) {
            return "-";
        }
        return etatController.listEtats().stream()
                .filter(etat -> etat.getId().equals(etatId))
                .map(etat -> valueOrDash(etat.getTraitementEnCours()))
                .findFirst()
                .orElse("Inconnu");
    }

    private String resolveUserEmail(Long userId) {
        if (userId == null) {
            return "-";
        }
        return userEmailById.getOrDefault(userId, "user#" + userId);
    }

    private void installInputConstraints() {
        installIntegerFilter(etatUtilisateurIdField);
        installIntegerFilter(activiteDureeField);
        installIntegerFilter(activiteCaloriesField);
        installIntegerFilter(activiteReposField);
        installDecimalFilter(activiteDistanceField);

        enforceMaxLength(etatTraitementField, 255);
        enforceMaxLength(etatRemarquesField, 2000);
        enforceMaxLength(activiteNotesField, 1000);
    }

    private EtatFormData validateEtatForm() {
        Long utilisateurId = parseLongRequired(etatUtilisateurIdField.getText(), "User ID");
        String traitement = requiredText(etatTraitementField.getText(), "Traitement").trim();
        String remarques = optionalText(etatRemarquesField.getText());
        String temperature = optionalText(etatTemperatureCombo.getValue());
        String hydratation = optionalText(etatHydratationCombo.getValue());
        LocalDate dateReleveDate = requireNonPastDate(etatDateReleveField.getValue(), "Date releve");
        LocalDateTime dateReleve = dateReleveDate.atStartOfDay();

        if (traitement.length() > 255) {
            throw new IllegalArgumentException("Traitement max 255 caracteres.");
        }
        if (remarques.length() > 2000) {
            throw new IllegalArgumentException("Remarques max 2000 caracteres.");
        }

        return new EtatFormData(utilisateurId, traitement, remarques, temperature, hydratation, dateReleve);
    }

    private ActiviteFormData validateActiviteForm() {
        PatientOption patient = selectedPatient();
        String type = requiredText(activiteTypeCombo.getValue(), "Type");
        Integer duree = parseIntRequired(activiteDureeField.getText(), "Duree");
        Integer calories = parseIntRequired(activiteCaloriesField.getText(), "Calories");
        Double distance = parseDoubleOptional(activiteDistanceField.getText(), "Distance");
        Integer repos = parseIntOptional(activiteReposField.getText(), "Heures repos");
        LocalDate dateActiviteDate = requireNonPastDate(activiteDateField.getValue(), "Date activite");
        LocalDateTime dateActivite = dateActiviteDate.atStartOfDay();
        String notes = optionalText(activiteNotesField.getText());

        if (duree <= 0) {
            throw new IllegalArgumentException("Duree doit etre > 0.");
        }
        if (calories < 0) {
            throw new IllegalArgumentException("Calories doit etre >= 0.");
        }
        if (distance != null && distance < 0) {
            throw new IllegalArgumentException("Distance doit etre >= 0.");
        }
        if (repos != null && repos < 0) {
            throw new IllegalArgumentException("Heures repos doit etre >= 0.");
        }
        if (notes.length() > 1000) {
            throw new IllegalArgumentException("Notes max 1000 caracteres.");
        }

        return new ActiviteFormData(patient.id(), type, duree, calories, distance, repos, dateActivite, notes);
    }

    private LocalDate requireNonPastDate(LocalDate date, String label) {
        if (date == null) {
            throw new IllegalArgumentException(label + " obligatoire.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(label + " ne peut pas etre dans le passe.");
        }
        return date;
    }

    private void installIntegerFilter(TextField field) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            return next.matches("\\d*") ? change : null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private void installDecimalFilter(TextField field) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            return next.matches("\\d*(\\.\\d{0,2})?") ? change : null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private void enforceMaxLength(TextField field, int max) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > max) {
                field.setText(newValue.substring(0, max));
            }
        });
    }

    private void enforceMaxLength(TextArea field, int max) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > max) {
                field.setText(newValue.substring(0, max));
            }
        });
    }

    private Long parseLongRequired(String input, String label) {
        try {
            if (input == null || input.isBlank()) {
                throw new IllegalArgumentException(label + " obligatoire.");
            }
            return Long.parseLong(input.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " invalide.");
        }
    }

    private Integer parseIntRequired(String input, String label) {
        try {
            if (input == null || input.isBlank()) {
                throw new IllegalArgumentException(label + " obligatoire.");
            }
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " invalide.");
        }
    }

    private Integer parseIntOptional(String input, String label) {
        try {
            if (input == null || input.isBlank()) {
                return null;
            }
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " invalide.");
        }
    }

    private Double parseDoubleOptional(String input, String label) {
        try {
            if (input == null || input.isBlank()) {
                return null;
            }
            return Double.parseDouble(input.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " invalide.");
        }
    }

    private LocalDateTime parseDateRequired(LocalDate date, String label) {
        if (date == null) {
            throw new IllegalArgumentException(label + " obligatoire.");
        }
        return date.atStartOfDay();
    }

    private String requiredText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " obligatoire.");
        }
        return value;
    }

    private boolean confirmDelete(String title, String message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(title);
        confirm.setContentText(message);
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    private void runAction(Action action) {
        try {
            action.run();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Erreur");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    @FunctionalInterface
    private interface Action {
        void run();
    }

    private record PatientOption(Long id, String email) {
        @Override
        public String toString() {
            return email;
        }
    }

    private record EtatFormData(
            Long utilisateurId,
            String traitementEnCours,
            String remarquesCliniques,
            String temperatureCorporelle,
            String niveauHydratation,
            LocalDateTime dateReleve
    ) {
    }

    private record ActiviteFormData(
            Long utilisateurId,
            String type,
            Integer duree,
            Integer calories,
            Double distanceKm,
            Integer heuresRepos,
            LocalDateTime dateActivite,
            String notes
    ) {
    }
}
