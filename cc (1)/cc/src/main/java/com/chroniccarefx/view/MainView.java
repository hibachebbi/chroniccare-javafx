package com.chroniccarefx.view;

import com.chroniccarefx.config.Db;
import com.chroniccarefx.controller.ActiviteController;
import com.chroniccarefx.controller.EtatController;
import com.chroniccarefx.model.Activite;
import com.chroniccarefx.model.Etat;
import java.awt.Desktop;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class MainView {
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
    private Label etatStatsLabel;

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
    private TextArea activiteRemarqueField;
    private Label activiteStatsLabel;

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
        tempCol.setCellValueFactory(data -> new SimpleStringProperty(formatTemperature(data.getValue().getTemperatureCorporelle())));

        TableColumn<Etat, String> hydCol = new TableColumn<>("Hydratation (L)");
        hydCol.setCellValueFactory(data -> new SimpleStringProperty(formatHydratation(data.getValue().getNiveauHydratation())));

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
        configureHydratationComboDisplay(etatHydratationCombo);
        etatDateReleveField = new DatePicker(LocalDate.now());
        etatStatsLabel = new Label();
        etatStatsLabel.setWrapText(true);

        Button addBtn = new Button("Ajouter");
        Button updateBtn = new Button("Modifier");
        Button deleteBtn = new Button("Supprimer");
        Button exportBtn = new Button("Exporter CSV");
        Button reportBtn = new Button("Rapport 7 jrs");

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
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Etat enregistre");
            alert.setContentText("Une activite adaptee a ete generee automatiquement pour ce patient.");
            alert.showAndWait();
        }));

        exportBtn.setOnAction(e -> runAction(this::exportEtatsCsv));
        reportBtn.setOnAction(e -> runAction(this::openSelectedPatientHealthReportDialog));

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
        form.addRow(3, new Label("Temperature (deg C)"), etatTemperatureCombo);
        form.addRow(4, new Label("Hydratation (L)"), etatHydratationCombo);
        form.addRow(5, new Label("Date releve"), etatDateReleveField);

        GridPane.setHgrow(etatUtilisateurIdField, Priority.ALWAYS);
        GridPane.setHgrow(etatTraitementField, Priority.ALWAYS);
        GridPane.setHgrow(etatRemarquesField, Priority.ALWAYS);
        GridPane.setHgrow(etatTemperatureCombo, Priority.ALWAYS);
        GridPane.setHgrow(etatHydratationCombo, Priority.ALWAYS);

        HBox buttons = new HBox(10, addBtn, updateBtn, deleteBtn, exportBtn, reportBtn);
        VBox content = new VBox(12, etatStatsLabel, form, buttons, etatTable);
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

        TableColumn<Activite, Number> distCol = new TableColumn<>("Distance");
        distCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDistanceKm()));
        distCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDistance(item.doubleValue()));
            }
        });

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
        activiteRemarqueField = new TextArea();
        activiteRemarqueField.setPrefRowCount(4);
        activiteStatsLabel = new Label();
        activiteStatsLabel.setWrapText(true);

        activitePatientCombo.valueProperty().addListener((obs, oldValue, selected) -> updateEtatLabelForPatient(selected));

        Button addBtn = new Button("Ajouter");
        Button updateBtn = new Button("Modifier");
        Button deleteBtn = new Button("Supprimer");
        Button exportBtn = new Button("Exporter CSV");
        Button remarqueBtn = new Button("Remarque");
        Button envoyerBtn = new Button("Envoyer");
        Button reportBtn = new Button("Rapport 7 jrs");

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

        exportBtn.setOnAction(e -> runAction(this::exportActivitesCsv));
        remarqueBtn.setOnAction(e -> runAction(this::generateRemarkForSelectedActivite));
        envoyerBtn.setOnAction(e -> runAction(this::sendRemarkEmailForSelectedActivite));
        reportBtn.setOnAction(e -> runAction(this::openSelectedPatientHealthReportDialog));

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
                activiteRemarqueField.clear();
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
        form.addRow(5, new Label("Distance (km)"), activiteDistanceField);
        form.addRow(6, new Label("Heures repos"), activiteReposField);
        form.addRow(7, new Label("Date activite"), activiteDateField);
        form.addRow(8, new Label("Notes"), activiteNotesField);
        form.addRow(9, new Label("Remarque coach/nutritionniste"), activiteRemarqueField);

        GridPane.setHgrow(activitePatientCombo, Priority.ALWAYS);
        GridPane.setHgrow(activiteTypeCombo, Priority.ALWAYS);
        GridPane.setHgrow(activiteNotesField, Priority.ALWAYS);
        GridPane.setHgrow(activiteRemarqueField, Priority.ALWAYS);

        HBox buttons = new HBox(10, addBtn, updateBtn, deleteBtn, exportBtn, reportBtn, remarqueBtn, envoyerBtn);
        VBox content = new VBox(12, activiteStatsLabel, form, buttons, activiteTable);
        VBox.setVgrow(activiteTable, Priority.ALWAYS);

        return new Tab("Activite", content);
    }

    private void refreshEtatData() {
        List<Etat> etats = etatController.listEtats();
        etatTable.getItems().setAll(etats);
        updateEtatStats(etats);
    }

    private void refreshActiviteData() {
        loadPatientsFromDatabase();
        List<Activite> activites = activiteController.listActivites();
        activiteTable.getItems().setAll(activites);
        updateActiviteStats(activites);
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

    private void updateEtatStats(List<Etat> etats) {
        int totalEtats = etats.size();
        int totalPatients = etats.stream()
                .map(Etat::getUtilisateurId)
                .filter(id -> id != null)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .size();
        long avecTemperature = etats.stream()
                .map(Etat::getTemperatureCorporelle)
                .filter(value -> value != null && !value.isBlank())
                .count();
        double hydratationTotale = etats.stream()
                .map(Etat::getNiveauHydratation)
                .filter(value -> value != null && !value.isBlank())
                .mapToDouble(this::parseDoubleSafe)
                .sum();

        etatStatsLabel.setText(
                "Statistiques des etats: "
                        + totalEtats + " etat(s), "
                        + totalPatients + " patient(s) suivis, "
                        + avecTemperature + " releve(s) de temperature, "
                        + formatDecimal(hydratationTotale) + " L d'hydratation cumulee."
        );
    }

    private void updateActiviteStats(List<Activite> activites) {
        int totalActivites = activites.size();
        int totalPatients = activites.stream()
                .map(Activite::getUtilisateurId)
                .filter(id -> id != null)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .size();
        int dureeTotale = activites.stream()
                .map(Activite::getDuree)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        int caloriesTotales = activites.stream()
                .map(Activite::getCalories)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        double distanceTotaleKm = activites.stream()
                .map(Activite::getDistanceKm)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        activiteStatsLabel.setText(
                "Statistiques des activites: "
                        + totalActivites + " activite(s), "
                        + totalPatients + " patient(s) actifs, "
                        + dureeTotale + " min, "
                        + caloriesTotales + " calories, "
                        + formatDistance(distanceTotaleKm) + " parcourus."
        );
    }

    private void exportEtatsCsv() {
        List<Etat> etats = etatController.listEtats();
        StringBuilder csv = new StringBuilder();
        csv.append("id;utilisateur_id;patient;traitement;remarques;temperature;hydratation;date_releve\n");
        for (Etat etat : etats) {
            csv.append(csvValue(etat.getId())).append(';')
                    .append(csvValue(etat.getUtilisateurId())).append(';')
                    .append(csvValue(resolveUserEmail(etat.getUtilisateurId()))).append(';')
                    .append(csvValue(etat.getTraitementEnCours())).append(';')
                    .append(csvValue(etat.getRemarquesCliniques())).append(';')
                    .append(csvValue(etat.getTemperatureCorporelle())).append(';')
                    .append(csvValue(etat.getNiveauHydratation())).append(';')
                    .append(csvValue(formatDateTime(etat.getDateReleve())))
                    .append('\n');
        }
        writeCsvFile("export-etats-tous-utilisateurs.csv", csv.toString());
    }

    private void exportActivitesCsv() {
        List<Activite> activites = activiteController.listActivites();
        StringBuilder csv = new StringBuilder();
        csv.append("id;utilisateur_id;patient;etat_id;etat;type;duree;calories;distance_km;heures_repos;date_activite;notes;created_at\n");
        for (Activite activite : activites) {
            csv.append(csvValue(activite.getId())).append(';')
                    .append(csvValue(activite.getUtilisateurId())).append(';')
                    .append(csvValue(resolveUserEmail(activite.getUtilisateurId()))).append(';')
                    .append(csvValue(activite.getEtatId())).append(';')
                    .append(csvValue(findEtatLabel(activite.getEtatId()))).append(';')
                    .append(csvValue(activite.getType())).append(';')
                    .append(csvValue(activite.getDuree())).append(';')
                    .append(csvValue(activite.getCalories())).append(';')
                    .append(csvValue(activite.getDistanceKm())).append(';')
                    .append(csvValue(activite.getHeuresRepos())).append(';')
                    .append(csvValue(formatDateTime(activite.getDateActivite()))).append(';')
                    .append(csvValue(activite.getNotes())).append(';')
                    .append(csvValue(formatDateTime(activite.getCreatedAt())))
                    .append('\n');
        }
        writeCsvFile("export-activites-tous-utilisateurs.csv", csv.toString());
    }

    private void writeCsvFile(String defaultFileName, String content) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer l'export CSV");
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));

        Window owner = etatTable != null && etatTable.getScene() != null ? etatTable.getScene().getWindow() : null;
        java.io.File selectedFile = fileChooser.showSaveDialog(owner);
        if (selectedFile == null) {
            return;
        }

        Path path = selectedFile.toPath();
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible d'ecrire le fichier CSV: " + ex.getMessage(), ex);
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Export termine");
        alert.setContentText("Fichier cree: " + path.toAbsolutePath());
        alert.showAndWait();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : EXPORT_DATE_FORMAT.format(value);
    }

    private void generateRemarkForSelectedActivite() {
        Activite activite = selectedActivite();
        Etat etat = findEtatById(activite.getEtatId()).orElse(null);
        String patientEmail = resolveUserEmail(activite.getUtilisateurId());

        StringBuilder remark = new StringBuilder();
        remark.append("Bonjour,\n\n");
        remark.append("Suite a l'activite ").append(valueOrDash(activite.getType()))
                .append(" planifiee pour le ").append(formatDateTime(activite.getDateActivite())).append(", ");
        remark.append("voici la remarque du coach / nutritionniste.\n\n");
        remark.append("Resume de l'activite:\n");
        remark.append("- Duree: ").append(valueOrDash(stringValue(activite.getDuree()))).append(" min\n");
        remark.append("- Calories: ").append(valueOrDash(stringValue(activite.getCalories()))).append('\n');
        remark.append("- Distance: ").append(formatDistance(activite.getDistanceKm())).append('\n');
        remark.append("- Repos conseille: ").append(valueOrDash(stringValue(activite.getHeuresRepos()))).append(" h\n");

        if (etat != null) {
            remark.append("- Etat associe: ").append(valueOrDash(etat.getTraitementEnCours())).append('\n');
            remark.append("- Temperature: ").append(formatTemperature(etat.getTemperatureCorporelle())).append('\n');
            remark.append("- Hydratation: ").append(formatHydratation(etat.getNiveauHydratation())).append('\n');
        }

        if (!optionalText(activite.getNotes()).isEmpty()) {
            remark.append("- Notes IA: ").append(activite.getNotes()).append('\n');
        }

        remark.append("\nRemarque personnalisee:\n");
        if (etat != null && shouldTakeItEasy(etat)) {
            remark.append("Merci de garder un rythme doux, de bien vous hydrater et de reduire l'intensite si une gene apparait.\n");
        } else {
            remark.append("Vous pouvez poursuivre l'activite recommandee en restant attentif(ve) a vos sensations et a votre recuperation.\n");
        }
        remark.append("\nPatient cible: ").append(patientEmail).append('\n');
        remark.append("\nCordialement.");

        activiteRemarqueField.setText(remark.toString());
    }

    private void sendRemarkEmailForSelectedActivite() {
        Activite activite = selectedActivite();
        String patientEmail = resolveUserEmail(activite.getUtilisateurId());
        String remarque = requiredText(activiteRemarqueField.getText(), "Remarque").trim();

        if (patientEmail.isBlank() || patientEmail.startsWith("user#") || "-".equals(patientEmail)) {
            throw new IllegalStateException("Adresse email du patient introuvable.");
        }

        String subject = "Remarque sur votre activite " + valueOrDash(activite.getType());
        String mailto = "mailto:" + encodeMailComponent(patientEmail)
                + "?subject=" + encodeMailComponent(subject)
                + "&body=" + encodeMailComponent(remarque);

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
                Desktop.getDesktop().mail(URI.create(mailto));
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(mailto));
            } else {
                throw new IllegalStateException("Aucun client mail n'est disponible sur cette machine.");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de preparer l'email: " + ex.getMessage(), ex);
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Email pret a etre envoye");
        alert.setContentText("Le message pour " + patientEmail + " a ete prepare dans votre client mail.");
        alert.showAndWait();
    }

    private void openSelectedPatientHealthReportDialog() {
        Long userId = resolvePatientIdForReport();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);

        List<Etat> recentEtats = etatController.listEtats().stream()
                .filter(etat -> userId.equals(etat.getUtilisateurId()))
                .filter(etat -> isWithinReportPeriod(etat.getDateReleve(), startDate, endDate))
                .sorted(Comparator.comparing(Etat::getDateReleve, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<Activite> recentActivites = activiteController.listActivites().stream()
                .filter(activite -> userId.equals(activite.getUtilisateurId()))
                .filter(activite -> isWithinReportPeriod(activite.getDateActivite(), startDate, endDate))
                .sorted(Comparator.comparing(Activite::getDateActivite, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        String patientLabel = resolveUserEmail(userId);
        String report = buildHealthReport(userId, patientLabel, recentEtats, recentActivites, startDate, endDate);
        openReadonlyReportDialog("Rapport 7 jours", patientLabel, report, startDate, endDate);
    }

    private Long resolvePatientIdForReport() {
        Etat selectedEtat = etatTable == null ? null : etatTable.getSelectionModel().getSelectedItem();
        if (selectedEtat != null && selectedEtat.getUtilisateurId() != null) {
            return selectedEtat.getUtilisateurId();
        }

        Activite selectedActivite = activiteTable == null ? null : activiteTable.getSelectionModel().getSelectedItem();
        if (selectedActivite != null && selectedActivite.getUtilisateurId() != null) {
            return selectedActivite.getUtilisateurId();
        }

        PatientOption selectedPatient = activitePatientCombo == null ? null : activitePatientCombo.getValue();
        if (selectedPatient != null && selectedPatient.id() != null) {
            return selectedPatient.id();
        }

        String etatUserId = etatUtilisateurIdField == null ? "" : optionalText(etatUtilisateurIdField.getText());
        if (!etatUserId.isBlank()) {
            return parseLongRequired(etatUserId, "User ID");
        }

        throw new IllegalStateException("Choisir un patient ou une ligne avant de generer le rapport.");
    }

    private boolean isWithinReportPeriod(LocalDateTime value, LocalDate startDate, LocalDate endDate) {
        if (value == null) {
            return false;
        }
        LocalDate date = value.toLocalDate();
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private String buildHealthReport(Long userId, String patientLabel, List<Etat> etats, List<Activite> activites,
                                     LocalDate startDate, LocalDate endDate) {
        StringBuilder report = new StringBuilder();
        report.append("Bonjour,\n\n");
        report.append("Voici votre rapport de sante genere automatiquement sur les 7 derniers jours.\n\n");
        report.append("Patient: ").append(valueOrDash(patientLabel)).append('\n');
        report.append("User ID: ").append(userId == null ? "-" : userId).append('\n');
        report.append("Periode: ").append(startDate.format(REPORT_DATE_FORMAT))
                .append(" au ").append(endDate.format(REPORT_DATE_FORMAT)).append("\n\n");
        report.append("Synthese automatique:\n");
        report.append(buildHealthRemark(etats, activites)).append("\n\n");

        report.append("Etats releves:\n");
        if (etats.isEmpty()) {
            report.append("- Aucun etat enregistre sur cette periode.\n");
        } else {
            for (Etat etat : etats) {
                report.append("- ").append(formatReportDate(etat.getDateReleve()))
                        .append(" | Traitement: ").append(valueOrDash(etat.getTraitementEnCours()))
                        .append(" | Temp: ").append(formatTemperature(etat.getTemperatureCorporelle()))
                        .append(" | Hydratation: ").append(formatHydratation(etat.getNiveauHydratation()))
                        .append(" | Remarques: ").append(valueOrDash(etat.getRemarquesCliniques()))
                        .append('\n');
            }
        }

        report.append("\nActivites recommandees / suivies:\n");
        if (activites.isEmpty()) {
            report.append("- Aucune activite enregistree sur cette periode.\n");
        } else {
            for (Activite activite : activites) {
                report.append("- ").append(formatReportDate(activite.getDateActivite()))
                        .append(" | Type: ").append(valueOrDash(activite.getType()))
                        .append(" | Duree: ").append(valueOrDash(stringValue(activite.getDuree()))).append(" min")
                        .append(" | Calories: ").append(valueOrDash(stringValue(activite.getCalories()))).append(" kcal")
                        .append(" | Distance: ").append(formatDistance(activite.getDistanceKm()))
                        .append(" | Repos: ").append(valueOrDash(stringValue(activite.getHeuresRepos()))).append(" h")
                        .append(" | Notes: ").append(valueOrDash(activite.getNotes()))
                        .append('\n');
            }
        }

        report.append("\nConseil professionnel:\n");
        report.append("Ce rapport aide au suivi, mais ne remplace pas un avis medical. ");
        report.append("En cas de fievre elevee, malaise, douleur importante ou aggravation, contactez un professionnel de sante.\n\n");
        report.append("Cordialement,\nchronic care");
        return report.toString();
    }

    private String buildHealthRemark(List<Etat> etats, List<Activite> activites) {
        if (etats.isEmpty() && activites.isEmpty()) {
            return "Aucune donnee recente disponible. Il est conseille de saisir au moins un etat et une activite pour un suivi utile.";
        }

        double maxTemperature = etats.stream()
                .map(Etat::getTemperatureCorporelle)
                .mapToDouble(this::parseDoubleSafe)
                .max()
                .orElse(0);

        double minHydratation = etats.stream()
                .map(Etat::getNiveauHydratation)
                .mapToDouble(this::parseDoubleSafe)
                .filter(value -> value > 0)
                .min()
                .orElse(0);

        int totalActiveMinutes = activites.stream()
                .map(Activite::getDuree)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();

        int totalRestHours = activites.stream()
                .map(Activite::getHeuresRepos)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();

        if (maxTemperature >= 39.0) {
            return "Temperature tres elevee detectee sur la periode. La priorite est le repos, l'hydratation reguliere et un avis medical si les symptomes persistent.";
        }
        if (maxTemperature >= 38.0 || (minHydratation > 0 && minHydratation < 1.5)) {
            return "Etat fragile detecte. Il vaut mieux privilegier une activite douce, surveiller la temperature et renforcer l'hydratation.";
        }
        if (totalActiveMinutes == 0 && totalRestHours > 0) {
            return "La periode montre surtout du repos. Une reprise progressive peut etre envisagee si l'etat reste stable.";
        }
        if (totalActiveMinutes >= 150 && maxTemperature < 38.0) {
            return "Le suivi des 7 derniers jours est globalement positif avec une activite reguliere et sans signe majeur d'alerte.";
        }
        return "Le suivi reste globalement stable. Continuez un rythme progressif, une bonne hydratation et une surveillance reguliere des signes cliniques.";
    }

    private void openReadonlyReportDialog(String titleText, String patientLabel, String report,
                                          LocalDate startDate, LocalDate endDate) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titleText);
        alert.setHeaderText(valueOrDash(patientLabel) + " | Periode: "
                + startDate.format(REPORT_DATE_FORMAT) + " - " + endDate.format(REPORT_DATE_FORMAT));

        TextArea reportField = new TextArea(report);
        reportField.setEditable(false);
        reportField.setWrapText(true);
        reportField.setPrefRowCount(20);
        reportField.setPrefColumnCount(90);
        reportField.setMaxWidth(Double.MAX_VALUE);
        reportField.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setContent(reportField);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setPrefWidth(900);
        alert.showAndWait();
    }

    private String formatReportDate(LocalDateTime value) {
        return value == null ? "-" : value.format(EXPORT_DATE_FORMAT);
    }

    private String csvValue(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        String escaped = text.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
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
                            + " | Temp: " + formatTemperature(etat.getTemperatureCorporelle())
                            + " | Hyd: " + formatHydratation(etat.getNiveauHydratation())
            );
        } else {
            activiteSelectedEtatId = null;
            activiteEtatLabel.setText("Aucun etat pour ce patient");
        }
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void configureHydratationComboDisplay(ComboBox<String> comboBox) {
        StringConverter<String> converter = new StringConverter<>() {
            @Override
            public String toString(String value) {
                return formatHydratation(value);
            }

            @Override
            public String fromString(String value) {
                return value;
            }
        };
        comboBox.setConverter(converter);
        comboBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : converter.toString(item));
            }
        });
        comboBox.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : converter.toString(item));
            }
        });
    }

    private String formatTemperature(String value) {
        String normalized = valueOrDash(value);
        return "-".equals(normalized) ? normalized : normalized + " deg C";
    }

    private String formatHydratation(String value) {
        String normalized = valueOrDash(value);
        return "-".equals(normalized) ? normalized : normalized + " L";
    }

    private String formatDistance(Double valueKm) {
        if (valueKm == null) {
            return "-";
        }
        if (valueKm < 1) {
            long metres = Math.round(valueKm * 1000);
            return metres + " m";
        }
        return formatDecimal(valueKm) + " km";
    }

    private String formatDecimal(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private boolean shouldTakeItEasy(Etat etat) {
        if (etat == null) {
            return false;
        }

        double temperature = parseDoubleSafe(optionalText(etat.getTemperatureCorporelle()));
        double hydratation = parseDoubleSafe(optionalText(etat.getNiveauHydratation()));
        String contexte = (optionalText(etat.getTraitementEnCours()) + " " + optionalText(etat.getRemarquesCliniques())).toLowerCase(Locale.ROOT);

        return temperature >= 38.0
                || hydratation > 0 && hydratation < 1.5
                || contexte.contains("fatigue")
                || contexte.contains("douleur")
                || contexte.contains("vertige");
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
        activiteRemarqueField.clear();
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

    private Optional<Etat> findEtatById(Long etatId) {
        if (etatId == null) {
            return Optional.empty();
        }
        return etatController.listEtats().stream()
                .filter(etat -> etatId.equals(etat.getId()))
                .findFirst();
    }

    private String encodeMailComponent(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
                .replace("+", "%20");
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
        enforceMaxLength(activiteRemarqueField, 4000);
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
