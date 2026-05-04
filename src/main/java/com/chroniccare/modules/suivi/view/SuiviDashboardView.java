package com.chroniccare.modules.suivi.view;

import com.chroniccare.models.User;
import com.chroniccare.modules.suivi.controller.ActiviteController;
import com.chroniccare.modules.suivi.controller.EtatController;
import com.chroniccare.modules.suivi.model.Activite;
import com.chroniccare.modules.suivi.model.Etat;
import com.chroniccare.modules.suivi.repository.ActiviteRepository;
import com.chroniccare.modules.suivi.repository.EtatRepository;
import com.chroniccare.modules.suivi.repository.JdbcActiviteRepository;
import com.chroniccare.modules.suivi.repository.JdbcEtatRepository;
import com.chroniccare.modules.suivi.service.ActiviteService;
import com.chroniccare.modules.suivi.service.EtatService;
import com.chroniccare.modules.suivi.service.OpenAiHealthReportService;
import com.chroniccare.modules.suivi.service.SmtpMailService;
import com.chroniccare.utils.MyDatabase;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.OutputStream;
import java.awt.Desktop;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

public class SuiviDashboardView {
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final String ETAT_SORT_RECENT = "Plus recents";
    private static final String ETAT_SORT_OLD = "Plus anciens";
    private static final String ETAT_SORT_PATIENT = "Patient A-Z";
    private static final String ETAT_SORT_TRAITEMENT = "Traitement A-Z";
    private static final String ACTIVITE_SORT_RECENT = "Plus recentes";
    private static final String ACTIVITE_SORT_OLD = "Plus anciennes";
    private static final String ACTIVITE_SORT_PATIENT = "Patient A-Z";
    private static final String ACTIVITE_SORT_TYPE = "Type A-Z";
    private static final Object VOICE_LOCK = new Object();
    private static final int CHAT_ONLINE_MINUTES = 5;
    private static Process activeVoiceProcess;

    private final User currentUser;
    private final EtatController etatController;
    private final ActiviteController activiteController;
    private final SmtpMailService smtpMailService;
    private final OpenAiHealthReportService openAiHealthReportService;
    private final Map<Long, PatientOption> patientById = new HashMap<>();
    private final ObservableList<Etat> etatItems = FXCollections.observableArrayList();
    private final ObservableList<Activite> activiteItems = FXCollections.observableArrayList();

    private TabPane tabPane;

    private TableView<Etat> etatTable;
    private TextField etatSearchField;
    private ComboBox<String> etatSortCombo;
    private ComboBox<PatientOption> etatPatientCombo;
    private TextField etatTraitementField;
    private TextArea etatRemarquesField;
    private ComboBox<String> etatTemperatureCombo;
    private ComboBox<String> etatHydratationCombo;
    private DatePicker etatDateReleveField;
    private Label etatStatsLabel;
    private VBox etatGeneratedActivityBox;
    private Label etatGeneratedActivityTitleLabel;
    private Label etatGeneratedActivityTypeLabel;
    private Label etatGeneratedActivityDureeLabel;
    private Label etatGeneratedActivityCaloriesLabel;
    private Label etatGeneratedActivityDistanceLabel;
    private Label etatGeneratedActivityReposLabel;
    private Label etatGeneratedActivityDateLabel;
    private Label etatGeneratedActivityTraitementLabel;
    private Label etatGeneratedActivityConseilLabel;
    private VBox patientHealthReportBox;

    private TableView<Activite> activiteTable;
    private TextField activiteSearchField;
    private ComboBox<String> activiteSortCombo;
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
    private Label activiteStatsLabel;
    public SuiviDashboardView(User currentUser) {
        this.currentUser = currentUser;
        EtatRepository etatRepository = new JdbcEtatRepository();
        ActiviteRepository activiteRepository = new JdbcActiviteRepository();
        EtatService etatService = new EtatService(etatRepository, activiteRepository);
        ActiviteService activiteService = new ActiviteService(activiteRepository, etatRepository);
        this.etatController = new EtatController(etatService);
        this.activiteController = new ActiviteController(activiteService, etatService);
        this.smtpMailService = new SmtpMailService();
        this.openAiHealthReportService = new OpenAiHealthReportService();
    }

    public Parent build() {
        VBox root = new VBox(16);
        root.getStyleClass().add("suivi-shell");
        root.getChildren().addAll(buildHeaderBanner(), buildTabs());
        installInputConstraints();
        loadPatientsFromDatabase();
        refreshEtatData();
        refreshActiviteData();
        showPatientHealthReportButton();
        return root;
    }

    private HBox buildHeaderBanner() {
        Label icon = new Label("+");
        icon.getStyleClass().add("suivi-header-icon");

        Label title = new Label("Suivi de sante");
        title.getStyleClass().add("suivi-header-title");

        Label subtitle = new Label(buildHeaderSubtitle());
        subtitle.getStyleClass().add("suivi-header-subtitle");
        subtitle.setWrapText(true);

        VBox texts = new VBox(4, title, subtitle);
        texts.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Label roleBadge = new Label(roleBadgeLabel());
        roleBadge.getStyleClass().add("suivi-header-badge");

        HBox banner = new HBox(16, icon, texts, roleBadge);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().add("suivi-header");
        return banner;
    }

    private String buildHeaderSubtitle() {
        if (currentUser == null || currentUser.getRoles() == null) {
            return "Releves cliniques et activites physiques de vos patients.";
        }
        if (currentUser.getRoles().contains("ROLE_PATIENT")) {
            return "Consultez vos releves d'etat et vos activites en un coup d'oeil.";
        }
        if (currentUser.getRoles().contains("ROLE_COACH")) {
            return "Suivez l'etat clinique et les activites physiques recommandees.";
        }
        if (currentUser.getRoles().contains("ROLE_NUTRITIONNISTE")) {
            return "Adaptez les conseils nutritionnels selon les releves recents des patients.";
        }
        return "Releves cliniques et activites physiques.";
    }

    private String roleBadgeLabel() {
        if (currentUser == null || currentUser.getRoles() == null) {
            return "UTILISATEUR";
        }
        if (currentUser.getRoles().contains("ROLE_ADMIN")) return "ADMIN";
        if (currentUser.getRoles().contains("ROLE_COACH")) return "COACH";
        if (currentUser.getRoles().contains("ROLE_NUTRITIONNISTE")) return "NUTRITIONNISTE";
        if (currentUser.getRoles().contains("ROLE_PATIENT")) return "PATIENT";
        return "UTILISATEUR";
    }

    private TabPane buildTabs() {
        tabPane = new TabPane(buildEtatTab(), buildActiviteTab());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("suivi-tabs");
        return tabPane;
    }

    private Tab buildEtatTab() {
        etatTable = new TableView<>();
        etatTable.getStyleClass().add("suivi-table");
        etatTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        etatTable.setPlaceholder(new Label("Aucun etat ne correspond a votre recherche."));

        TableColumn<Etat, String> patientCol = new TableColumn<>("Patient");
        patientCol.setCellValueFactory(d -> new SimpleStringProperty(resolveUserLabel(d.getValue().getUtilisateurId())));
        TableColumn<Etat, String> traitementCol = new TableColumn<>("Traitement");
        traitementCol.setCellValueFactory(d -> new SimpleStringProperty(valueOrDash(d.getValue().getTraitementEnCours())));
        TableColumn<Etat, String> remarquesCol = new TableColumn<>("Remarques");
        remarquesCol.setCellValueFactory(d -> new SimpleStringProperty(valueOrDash(d.getValue().getRemarquesCliniques())));
        TableColumn<Etat, String> tempCol = new TableColumn<>("Temperature");
        tempCol.setCellValueFactory(d -> new SimpleStringProperty(formatTemperature(d.getValue().getTemperatureCorporelle())));
        TableColumn<Etat, String> hydCol = new TableColumn<>("Hydratation (L)");
        hydCol.setCellValueFactory(d -> new SimpleStringProperty(formatHydratation(d.getValue().getNiveauHydratation())));
        TableColumn<Etat, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(formatDate(d.getValue().getDateReleve())));
        etatTable.getColumns().addAll(patientCol, traitementCol, remarquesCol, tempCol, hydCol, dateCol);
        if (canReviewGeneratedActivity()) {
            TableColumn<Etat, Etat> actionCol = new TableColumn<>("Action");
            actionCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
            actionCol.setCellFactory(col -> new TableCell<>() {
                private final Button consulterBtn = button("Consulter", "btn-neutral");
                {
                    consulterBtn.setOnAction(e -> {
                        Etat etat = getItem();
                        if (etat != null) {
                            runAction(() -> openGeneratedActivityDialog(etat));
                        }
                    });
                }

                @Override
                protected void updateItem(Etat item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty || item == null ? null : consulterBtn);
                }
            });
            etatTable.getColumns().add(actionCol);
        }
        applyWrappedTextCells(patientCol, traitementCol, remarquesCol, tempCol, hydCol, dateCol);
        configureEtatTableFiltering();

        etatSearchField = new TextField();
        etatSearchField.setPromptText("Rechercher par patient, traitement, remarques...");
        etatSearchField.getStyleClass().add("suivi-search-field");
        etatSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyEtatFilters());
        etatSortCombo = new ComboBox<>();
        etatSortCombo.getItems().setAll(ETAT_SORT_RECENT, ETAT_SORT_OLD, ETAT_SORT_PATIENT, ETAT_SORT_TRAITEMENT);
        etatSortCombo.setValue(ETAT_SORT_RECENT);
        etatSortCombo.getStyleClass().add("suivi-filter-combo");
        etatSortCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyEtatFilters());
        etatPatientCombo = new ComboBox<>();
        etatPatientCombo.setMaxWidth(Double.MAX_VALUE);
        etatTraitementField = new TextField();
        etatRemarquesField = new TextArea();
        etatRemarquesField.setPrefRowCount(3);
        etatTemperatureCombo = new ComboBox<>();
        etatTemperatureCombo.getItems().setAll(Etat.TEMPERATURE_CORPORELLE_CHOICES);
        etatTemperatureCombo.setMaxWidth(Double.MAX_VALUE);
        etatHydratationCombo = new ComboBox<>();
        etatHydratationCombo.getItems().setAll(Etat.NIVEAU_HYDRATATION_CHOICES);
        etatHydratationCombo.setMaxWidth(Double.MAX_VALUE);
        configureHydratationComboDisplay(etatHydratationCombo);
        etatDateReleveField = new DatePicker(LocalDate.now());
        etatStatsLabel = new Label();
        etatStatsLabel.getStyleClass().add("section-subtitle");
        etatStatsLabel.setWrapText(true);
        etatGeneratedActivityBox = buildEtatGeneratedActivityBox();
        patientHealthReportBox = buildPatientHealthReportBox();

        Button addBtn = button("Ajouter", "btn-primary");
        Button updateBtn = button("Modifier", "btn-primary");
        Button deleteBtn = button("Supprimer", "btn-danger");
        Button statsBtn = button("Statistique", "btn-neutral");
        Button exportBtn = button("Exporter CSV", "btn-neutral");
        Button reportBtn = canGenerateAiReport() ? button("Rapport IA 6 etats", "btn-neutral") : null;
        Button chatBtn = canUseMessenger() ? button("Discussion", "btn-neutral") : null;

        addBtn.setOnAction(e -> runAction(this::handleAddEtat));
        updateBtn.setOnAction(e -> runAction(this::handleUpdateEtat));
        deleteBtn.setOnAction(e -> runAction(this::handleDeleteEtat));
        statsBtn.setOnAction(e -> runAction(this::showEtatStatsDialog));
        exportBtn.setOnAction(e -> runAction(this::exportEtatsCsv));
        if (reportBtn != null) {
            reportBtn.setOnAction(e -> runAction(this::openSelectedPatientHealthReportDialog));
        }
        if (chatBtn != null) {
            chatBtn.setOnAction(e -> runAction(this::openMessengerPopup));
        }

        etatTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                selectPatient(etatPatientCombo, selected.getUtilisateurId());
                etatTraitementField.setText(selected.getTraitementEnCours());
                etatRemarquesField.setText(selected.getRemarquesCliniques());
                etatTemperatureCombo.setValue(selected.getTemperatureCorporelle());
                etatHydratationCombo.setValue(selected.getNiveauHydratation());
                etatDateReleveField.setValue(selected.getDateReleve() == null ? LocalDate.now() : selected.getDateReleve().toLocalDate());
                updateEtatGeneratedActivityBox(selected, findGeneratedActivite(selected.getId()).orElse(null));
            }
        });

        VBox formCard = buildFormCard(
                "Etat",
                "Saisissez les releves et observations du patient.",
                new VBox(10, buildEtatFormGrid(), etatGeneratedActivityBox, patientHealthReportBox),
                new HBox(12, addBtn, updateBtn, deleteBtn),
                500
        );
        VBox tableCard = buildTableCard(
                "Historique des etats",
                "Recherchez et triez rapidement les releves visibles selon votre role.",
                etatStatsLabel,
                buildTableToolbar(etatSearchField, etatSortCombo, this::resetEtatFilters, statsBtn, exportBtn, reportBtn, chatBtn),
                etatTable
        );
        VBox layout = canManageEtat()
                ? new VBox(16, formCard, tableCard)
                : new VBox(16, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        return new Tab("Etat", layout);
    }

    private Tab buildActiviteTab() {
        activiteTable = new TableView<>();
        activiteTable.getStyleClass().add("suivi-table");
        activiteTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        activiteTable.setPlaceholder(new Label("Aucune activite ne correspond a votre recherche."));

        TableColumn<Activite, String> patientCol = new TableColumn<>("Patient");
        patientCol.setCellValueFactory(d -> new SimpleStringProperty(resolveUserLabel(d.getValue().getUtilisateurId())));
        TableColumn<Activite, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(valueOrDash(d.getValue().getType())));
        TableColumn<Activite, Number> dureeCol = new TableColumn<>("Duree (min)");
        dureeCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getDuree()));
        dureeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                Activite activite = getTableRow() == null ? null : (Activite) getTableRow().getItem();
                setText(formatDuree(activite));
            }
        });
        TableColumn<Activite, Number> caloriesCol = new TableColumn<>("Calories");
        caloriesCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getCalories()));
        TableColumn<Activite, Number> distanceCol = new TableColumn<>("Distance");
        distanceCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getDistanceKm()));
        distanceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDistance(item.doubleValue()));
            }
        });
        TableColumn<Activite, Number> reposCol = new TableColumn<>("Repos (h)");
        reposCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getHeuresRepos()));
        reposCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatRepos(item.intValue()));
            }
        });
        TableColumn<Activite, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(formatDate(d.getValue().getDateActivite())));
        TableColumn<Activite, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(d -> new SimpleStringProperty(valueOrDash(d.getValue().getNotes())));
        TableColumn<Activite, String> etatCol = new TableColumn<>("Etat lie");
        etatCol.setCellValueFactory(d -> new SimpleStringProperty(findEtatLabel(d.getValue().getEtatId())));
        activiteTable.getColumns().addAll(patientCol, typeCol, dureeCol, caloriesCol, distanceCol, reposCol, dateCol, notesCol, etatCol);
        applyWrappedTextCells(patientCol, typeCol, dateCol, notesCol, etatCol);
        configureActiviteTableFiltering();

        activiteSearchField = new TextField();
        activiteSearchField.setPromptText("Rechercher par patient, type, notes, etat...");
        activiteSearchField.getStyleClass().add("suivi-search-field");
        activiteSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyActiviteFilters());
        activiteSortCombo = new ComboBox<>();
        activiteSortCombo.getItems().setAll(ACTIVITE_SORT_RECENT, ACTIVITE_SORT_OLD, ACTIVITE_SORT_PATIENT, ACTIVITE_SORT_TYPE);
        activiteSortCombo.setValue(ACTIVITE_SORT_RECENT);
        activiteSortCombo.getStyleClass().add("suivi-filter-combo");
        activiteSortCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyActiviteFilters());
        activitePatientCombo = new ComboBox<>();
        activitePatientCombo.setMaxWidth(Double.MAX_VALUE);
        activiteEtatLabel = new Label("-");
        activiteEtatLabel.getStyleClass().add("suivi-help-text");
        activiteEtatLabel.setWrapText(true);
        activiteTypeCombo = new ComboBox<>();
        activiteTypeCombo.getItems().setAll(Activite.TYPE_CHOICES);
        activiteTypeCombo.setMaxWidth(Double.MAX_VALUE);
        activiteDureeField = new TextField();
        activiteCaloriesField = new TextField();
        activiteDistanceField = new TextField();
        activiteReposField = new TextField();
        activiteDateField = new DatePicker(LocalDate.now());
        activiteNotesField = new TextArea();
        activiteNotesField.setPrefRowCount(3);
        activiteStatsLabel = new Label();
        activiteStatsLabel.getStyleClass().add("section-subtitle");
        activiteStatsLabel.setWrapText(true);
        activitePatientCombo.valueProperty().addListener((obs, oldValue, selected) -> updateEtatLabelForPatient(selected));

        Button addBtn = button("Ajouter", "btn-primary");
        Button updateBtn = button("Modifier", "btn-primary");
        Button deleteBtn = button("Supprimer", "btn-danger");
        Button statsBtn = button("Statistique", "btn-neutral");
        Button exportBtn = button("Exporter CSV", "btn-neutral");
        Button reportBtn = canGenerateAiReport() ? button("Rapport IA 6 etats", "btn-neutral") : null;
        Button chatBtn = canUseMessenger() ? button("Discussion", "btn-neutral") : null;

        addBtn.setOnAction(e -> runAction(this::handleAddActivite));
        updateBtn.setOnAction(e -> runAction(this::handleUpdateActivite));
        deleteBtn.setOnAction(e -> runAction(this::handleDeleteActivite));
        statsBtn.setOnAction(e -> runAction(this::showActiviteStatsDialog));
        exportBtn.setOnAction(e -> runAction(this::exportActivitesCsv));
        if (reportBtn != null) {
            reportBtn.setOnAction(e -> runAction(this::openSelectedPatientHealthReportDialog));
        }
        if (chatBtn != null) {
            chatBtn.setOnAction(e -> runAction(this::openMessengerPopup));
        }

        activiteTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                selectPatient(activitePatientCombo, selected.getUtilisateurId());
                activiteTypeCombo.setValue(selected.getType());
                activiteDureeField.setText(String.valueOf(selected.getDuree()));
                activiteCaloriesField.setText(String.valueOf(selected.getCalories()));
                activiteDistanceField.setText(selected.getDistanceKm() == null ? "" : String.valueOf(selected.getDistanceKm()));
                activiteReposField.setText(selected.getHeuresRepos() == null ? "" : String.valueOf(selected.getHeuresRepos()));
                activiteDateField.setValue(selected.getDateActivite() == null ? LocalDate.now() : selected.getDateActivite().toLocalDate());
                activiteNotesField.setText(selected.getNotes());
            }
        });

        VBox formCard = buildFormCard("Activite", "Le coach et le nutritionniste peuvent suivre les patients selon l'etat du patient.", buildActiviteFormGrid(), new HBox(12, addBtn, updateBtn, deleteBtn), 500);
        VBox tableCard = buildTableCard(
                "Historique des activites",
                "Affinez la liste en temps reel et changez l'ordre d'affichage selon le besoin.",
                activiteStatsLabel,
                buildTableToolbar(activiteSearchField, activiteSortCombo, this::resetActiviteFilters, statsBtn, exportBtn, reportBtn, chatBtn),
                activiteTable
        );
        VBox layout = canManageActivite()
                ? new VBox(16, formCard, tableCard)
                : new VBox(16, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        return new Tab("Activite", layout);
    }

    private VBox buildFormCard(String titleText, String subtitleText, Parent form, HBox actions, double prefWidth) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("card", "suivi-form-card");
        card.setPrefWidth(prefWidth);
        card.getChildren().addAll(buildCardHeader(titleText, subtitleText), new Separator(), form, actions);
        actions.getStyleClass().add("suivi-actions");
        return card;
    }

    private VBox buildTableCard(String titleText, String subtitleText, Label statsLabel, Parent toolbar, TableView<?> table) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("card", "suivi-table-card");
        VBox.setVgrow(table, Priority.ALWAYS);
        card.getChildren().addAll(buildCardHeader(titleText, subtitleText), statsLabel, toolbar, new Separator(), table);
        return card;
    }

    private VBox buildCardHeader(String titleText, String subtitleText) {
        Label icon = new Label("|");
        icon.getStyleClass().add("card-icon");
        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");
        HBox titleRow = new HBox(10, icon, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("section-subtitle");
        subtitle.setWrapText(true);

        VBox header = new VBox(4, titleRow, subtitle);
        return header;
    }

    private HBox buildTableToolbar(TextField searchField, ComboBox<String> sortCombo, Runnable resetAction, Button statsBtn, Button exportBtn) {
        return buildTableToolbar(searchField, sortCombo, resetAction, statsBtn, exportBtn, new Button[0]);
    }

    private HBox buildTableToolbar(TextField searchField, ComboBox<String> sortCombo, Runnable resetAction,
                                   Button statsBtn, Button exportBtn, Button... extraButtons) {
        Label sortLabel = new Label("Trier par");
        sortLabel.getStyleClass().add("suivi-toolbar-label");
        Button resetBtn = button("Reinitialiser", "btn-neutral");
        resetBtn.getStyleClass().add("suivi-reset-btn");
        resetBtn.setOnAction(e -> resetAction.run());
        HBox toolbar = new HBox(12, searchField, sortLabel, sortCombo, resetBtn, statsBtn, exportBtn);
        if (extraButtons != null) {
            for (Button extraBtn : extraButtons) {
                if (extraBtn != null) {
                    toolbar.getChildren().add(extraBtn);
                }
            }
        }
        toolbar.getStyleClass().add("suivi-toolbar");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        return toolbar;
    }

    @SafeVarargs
    private final <T> void applyWrappedTextCells(TableColumn<T, String>... columns) {
        for (TableColumn<T, String> column : columns) {
            column.setCellFactory(col -> new TableCell<>() {
                private final Label label = new Label();
                {
                    label.getStyleClass().add("suivi-cell-text");
                    label.setWrapText(true);
                    label.setMaxWidth(Double.MAX_VALUE);
                    setPrefHeight(USE_COMPUTED_SIZE);
                    setGraphic(label);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        label.setText(null);
                        setGraphic(null);
                    } else {
                        label.setText(item);
                        label.setPrefWidth(Math.max(col.getWidth() - 18, 80));
                        setGraphic(label);
                    }
                }
            });
        }
    }

    private GridPane buildEtatFormGrid() {
        GridPane form = formGrid();
        form.addRow(0, formLabel("Patient"), etatPatientCombo);
        form.addRow(1, formLabel("Traitement"), etatTraitementField);
        form.addRow(2, formLabel("Remarques"), etatRemarquesField);
        form.addRow(3, formLabel("Temperature (deg C)"), etatTemperatureCombo);
        form.addRow(4, formLabel("Hydratation (L)"), etatHydratationCombo);
        form.addRow(5, formLabel("Date"), etatDateReleveField);
        GridPane.setHgrow(etatPatientCombo, Priority.ALWAYS);
        GridPane.setHgrow(etatTraitementField, Priority.ALWAYS);
        GridPane.setHgrow(etatRemarquesField, Priority.ALWAYS);
        GridPane.setHgrow(etatTemperatureCombo, Priority.ALWAYS);
        GridPane.setHgrow(etatHydratationCombo, Priority.ALWAYS);
        return form;
    }

    private GridPane buildActiviteFormGrid() {
        GridPane form = formGrid();
        form.addRow(0, formLabel("Patient"), activitePatientCombo);
        form.addRow(1, formLabel("Etat courant"), activiteEtatLabel);
        form.addRow(2, formLabel("Type"), activiteTypeCombo);
        form.addRow(3, formLabel("Duree"), activiteDureeField);
        form.addRow(4, formLabel("Calories"), activiteCaloriesField);
        form.addRow(5, formLabel("Distance (km)"), activiteDistanceField);
        form.addRow(6, formLabel("Repos (h)"), activiteReposField);
        form.addRow(7, formLabel("Date"), activiteDateField);
        form.addRow(8, formLabel("Notes"), activiteNotesField);
        GridPane.setHgrow(activitePatientCombo, Priority.ALWAYS);
        GridPane.setHgrow(activiteTypeCombo, Priority.ALWAYS);
        GridPane.setHgrow(activiteDureeField, Priority.ALWAYS);
        GridPane.setHgrow(activiteCaloriesField, Priority.ALWAYS);
        GridPane.setHgrow(activiteDistanceField, Priority.ALWAYS);
        GridPane.setHgrow(activiteReposField, Priority.ALWAYS);
        GridPane.setHgrow(activiteNotesField, Priority.ALWAYS);
        return form;
    }

    private GridPane formGrid() {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        return form;
    }

    private Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("info-label");
        label.setMinWidth(110);
        label.setPrefWidth(110);
        return label;
    }

    private Button button(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setMinHeight(40);
        button.setPrefHeight(40);
        return button;
    }

    private void openMessengerPopup() {
        if (!canUseMessenger()) {
            throw new IllegalStateException("La discussion est reservee au patient, au coach et au nutritionniste.");
        }

        ensureMessengerTables();
        refreshMessengerPresence();

        ObservableList<ChatContact> contacts = FXCollections.observableArrayList(loadMessengerContacts());
        if (contacts.isEmpty()) {
            throw new IllegalStateException("Aucun contact disponible pour la discussion.");
        }

        Long preferredContactId = resolvePreferredMessengerContactId();
        ListView<ChatContact> contactList = new ListView<>(contacts);
        contactList.setPrefWidth(270);
        contactList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ChatContact item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Circle statusDot = new Circle(5, isChatContactOnline(item.lastSeen()) ? Color.web("#22c55e") : Color.web("#94a3b8"));
                Label name = new Label(item.displayName());
                name.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
                Label meta = new Label(item.roleLabel() + " | " + item.email());
                meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                VBox textBox = new VBox(2, name, meta);
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label unreadBadge = new Label(item.unreadCount() > 0 ? String.valueOf(item.unreadCount()) : "");
                unreadBadge.setVisible(item.unreadCount() > 0);
                unreadBadge.setManaged(item.unreadCount() > 0);
                unreadBadge.setMinSize(22, 22);
                unreadBadge.setAlignment(Pos.CENTER);
                unreadBadge.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 11; -fx-text-fill: white; "
                        + "-fx-font-size: 11px; -fx-font-weight: 900; -fx-padding: 2 6 2 6;");
                HBox row = new HBox(10, statusDot, textBox, spacer, unreadBadge);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 10, 8, 10));
                setGraphic(row);
            }
        });

        Label headerName = new Label("Discussion");
        headerName.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        Label headerMeta = new Label("Selectionnez un contact");
        headerMeta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: 700;");
        Circle headerStatusDot = new Circle(6, Color.web("#94a3b8"));
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: 700;");
        Button cameraBtn = new Button("Camera");
        cameraBtn.setDisable(true);
        cameraBtn.setStyle("-fx-background-color: #0f766e; -fx-text-fill: white; -fx-font-size: 12px; "
                + "-fx-font-weight: 900; -fx-background-radius: 18; -fx-padding: 8 14 8 14;");
        cameraBtn.setOnAction(e -> runAction(() -> {
            ChatContact selected = contactList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                throw new IllegalStateException("Choisissez un contact avant de lancer la visio.");
            }
            openMeetForContact(selected);
            statusLabel.setText("Lien de visio envoye et reunion ouverte.");
        }));
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, headerStatusDot, new VBox(2, headerName, headerMeta), headerSpacer, cameraBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14));
        header.setStyle("-fx-background-color: linear-gradient(to right, #eff6ff, #f8fafc);"
                + "-fx-border-color: #dbeafe; -fx-border-width: 0 0 1 0;");

        VBox messageList = new VBox(10);
        messageList.setPadding(new Insets(16));
        messageList.setFillWidth(true);

        ScrollPane messageScroll = new ScrollPane(messageList);
        messageScroll.setFitToWidth(true);
        messageScroll.setStyle("-fx-background: white; -fx-background-color: white;");
        VBox.setVgrow(messageScroll, Priority.ALWAYS);

        TextArea composer = new TextArea();
        composer.setPromptText("Ecrire un message...");
        composer.setPrefRowCount(3);
        composer.setWrapText(true);
        composer.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 16; "
                + "-fx-background-radius: 16; -fx-padding: 10;");

        Button sendBtn = reportButton("Envoyer", "#2563eb");
        Button refreshBtn = reportButton("Actualiser", "#0f766e");
        Button closeBtn = reportButton("Fermer", "#64748b");

        Runnable refreshView = () -> {
            refreshMessengerPresence();
            List<ChatContact> refreshedContacts = loadMessengerContacts();
            ChatContact selected = contactList.getSelectionModel().getSelectedItem();
            Long selectedId = selected == null ? null : selected.id();
            contacts.setAll(refreshedContacts);
            selectPreferredChatContact(contactList, selectedId != null ? selectedId : preferredContactId);
            renderMessengerConversation(contactList.getSelectionModel().getSelectedItem(), messageList, messageScroll,
                    headerName, headerMeta, headerStatusDot, statusLabel, cameraBtn, false);
        };

        contactList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selectedValue) ->
                renderMessengerConversation(selectedValue, messageList, messageScroll, headerName, headerMeta, headerStatusDot, statusLabel, cameraBtn, true));

        sendBtn.setOnAction(e -> runAction(() -> {
            ChatContact selected = contactList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                throw new IllegalStateException("Choisissez un contact avant d'envoyer un message.");
            }
            sendMessengerMessage(selected.id(), composer.getText());
            composer.clear();
            refreshView.run();
            statusLabel.setText("Message envoye.");
        }));

        refreshBtn.setOnAction(e -> runAction(refreshView::run));
        closeBtn.setOnAction(e -> {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        });

        HBox composerActions = new HBox(10, refreshBtn, sendBtn, closeBtn);
        composerActions.setAlignment(Pos.CENTER_RIGHT);
        VBox composerBox = new VBox(10, composer, composerActions, statusLabel);
        composerBox.setPadding(new Insets(14));
        composerBox.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

        BorderPane conversationPane = new BorderPane();
        conversationPane.setTop(header);
        conversationPane.setCenter(messageScroll);
        conversationPane.setBottom(composerBox);

        BorderPane root = new BorderPane();
        root.setLeft(contactList);
        root.setCenter(conversationPane);
        BorderPane.setMargin(contactList, new Insets(0, 1, 0, 0));
        root.setStyle("-fx-background-color: white;");

        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        Window owner = tabPane == null || tabPane.getScene() == null ? null : tabPane.getScene().getWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Discussion instantanee");
        stage.setScene(new Scene(root, 980, 670));

        Timeline refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            try {
                refreshView.run();
            } catch (Exception ex) {
                statusLabel.setText("Actualisation impossible: " + ex.getMessage());
            }
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        stage.setOnShown(e -> {
            selectPreferredChatContact(contactList, preferredContactId);
            refreshView.run();
            refreshTimeline.play();
        });
        stage.setOnCloseRequest(e -> refreshTimeline.stop());
        stage.show();
    }

    private void ensureMessengerTables() {
        String createMessages = "CREATE TABLE IF NOT EXISTS suivi_chat_messages ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                + "sender_id BIGINT NOT NULL, "
                + "receiver_id BIGINT NOT NULL, "
                + "message_text TEXT NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";
        String createPresence = "CREATE TABLE IF NOT EXISTS suivi_chat_presence ("
                + "user_id BIGINT PRIMARY KEY, "
                + "display_name VARCHAR(255), "
                + "role_label VARCHAR(80), "
                + "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";
        String createReadState = "CREATE TABLE IF NOT EXISTS suivi_chat_read_state ("
                + "user_id BIGINT NOT NULL, "
                + "contact_id BIGINT NOT NULL, "
                + "last_read_at TIMESTAMP NULL, "
                + "PRIMARY KEY (user_id, contact_id)"
                + ")";
        try (PreparedStatement createMessagesStmt = MyDatabase.getInstance().getConnection().prepareStatement(createMessages);
             PreparedStatement createPresenceStmt = MyDatabase.getInstance().getConnection().prepareStatement(createPresence);
             PreparedStatement createReadStateStmt = MyDatabase.getInstance().getConnection().prepareStatement(createReadState)) {
            createMessagesStmt.execute();
            createPresenceStmt.execute();
            createReadStateStmt.execute();
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible d'initialiser la messagerie: " + ex.getMessage(), ex);
        }
    }

    private void refreshMessengerPresence() {
        if (currentUser == null) {
            return;
        }
        String sql = "INSERT INTO suivi_chat_presence (user_id, display_name, role_label, last_seen) "
                + "VALUES (?, ?, ?, CURRENT_TIMESTAMP) "
                + "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), role_label = VALUES(role_label), last_seen = CURRENT_TIMESTAMP";
        try (PreparedStatement statement = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            statement.setLong(1, currentUser.getId());
            statement.setString(2, resolveCurrentUserDisplayName());
            statement.setString(3, resolveRoleLabel(currentUser.getRoles()));
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de mettre a jour le statut en ligne: " + ex.getMessage(), ex);
        }
    }

    private ChatNotification findLatestUnreadNotificationBeforePresenceRefresh() {
        String sql = "SELECT m.id, m.sender_id, m.message_text, m.created_at, u.nom, u.prenom, u.email, "
                + "(SELECT COUNT(*) FROM suivi_chat_messages x "
                + "LEFT JOIN suivi_chat_read_state rs ON rs.user_id = ? AND rs.contact_id = x.sender_id "
                + "WHERE x.receiver_id = ? AND x.sender_id = m.sender_id "
                + "AND (rs.last_read_at IS NULL OR x.created_at > rs.last_read_at)) AS unread_count "
                + "FROM suivi_chat_messages m "
                + "JOIN users u ON u.id = m.sender_id "
                + "WHERE m.receiver_id = ? AND m.sender_id <> ? "
                + "AND NOT EXISTS (SELECT 1 FROM suivi_chat_read_state r "
                + "WHERE r.user_id = ? AND r.contact_id = m.sender_id AND r.last_read_at > m.created_at) "
                + "ORDER BY m.created_at DESC, m.id DESC "
                + "LIMIT 1";

        try (PreparedStatement statement = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            statement.setLong(1, currentUser.getId());
            statement.setLong(2, currentUser.getId());
            statement.setLong(3, currentUser.getId());
            statement.setLong(4, currentUser.getId());
            statement.setLong(5, currentUser.getId());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String senderName = buildDisplayName(
                        rs.getString("prenom"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        0
                );
                String preview = optionalText(rs.getString("message_text"));
                if (preview.length() > 220) {
                    preview = preview.substring(0, 220) + "...";
                }
                Timestamp createdAt = rs.getTimestamp("created_at");
                return new ChatNotification(
                        senderName,
                        preview,
                        rs.getInt("unread_count"),
                        createdAt == null ? null : createdAt.toLocalDateTime()
                );
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de verifier les nouveaux messages: " + ex.getMessage(), ex);
        }
    }

    private List<ChatContact> loadMessengerContacts() {
        String sql;
        if (isPatient()) {
            sql = "SELECT u.id, u.nom, u.prenom, u.email, u.roles, p.last_seen, "
                    + "COUNT(m.id) AS unread_count, MAX(m.created_at) AS last_message_at "
                    + "FROM users u LEFT JOIN suivi_chat_presence p ON p.user_id = u.id "
                    + "LEFT JOIN suivi_chat_messages m ON m.sender_id = u.id AND m.receiver_id = ? "
                    + "AND NOT EXISTS (SELECT 1 FROM suivi_chat_read_state rs "
                    + "WHERE rs.user_id = ? AND rs.contact_id = u.id AND rs.last_read_at >= m.created_at) "
                    + "WHERE u.id <> ? AND (u.roles LIKE '%ROLE_COACH%' OR u.roles LIKE '%ROLE_NUTRITIONNISTE%') "
                    + "GROUP BY u.id, u.nom, u.prenom, u.email, u.roles, p.last_seen "
                    + "ORDER BY unread_count DESC, last_message_at DESC, u.prenom, u.nom, u.email";
        } else {
            sql = "SELECT u.id, u.nom, u.prenom, u.email, u.roles, p.last_seen, "
                    + "COUNT(m.id) AS unread_count, MAX(m.created_at) AS last_message_at "
                    + "FROM users u LEFT JOIN suivi_chat_presence p ON p.user_id = u.id "
                    + "LEFT JOIN suivi_chat_messages m ON m.sender_id = u.id AND m.receiver_id = ? "
                    + "AND NOT EXISTS (SELECT 1 FROM suivi_chat_read_state rs "
                    + "WHERE rs.user_id = ? AND rs.contact_id = u.id AND rs.last_read_at >= m.created_at) "
                    + "WHERE u.id <> ? AND u.roles LIKE '%ROLE_PATIENT%' "
                    + "GROUP BY u.id, u.nom, u.prenom, u.email, u.roles, p.last_seen "
                    + "ORDER BY unread_count DESC, last_message_at DESC, u.prenom, u.nom, u.email";
        }

        try (PreparedStatement statement = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            statement.setLong(1, currentUser.getId());
            statement.setLong(2, currentUser.getId());
            statement.setLong(3, currentUser.getId());
            try (ResultSet rs = statement.executeQuery()) {
                ObservableList<ChatContact> contacts = FXCollections.observableArrayList();
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String displayName = buildDisplayName(rs.getString("prenom"), rs.getString("nom"), rs.getString("email"), id);
                    contacts.add(new ChatContact(
                            id,
                            displayName,
                            valueOrDash(rs.getString("email")),
                            resolveRoleLabel(rs.getString("roles")),
                            rs.getTimestamp("last_seen"),
                            rs.getInt("unread_count")
                    ));
                }
                return contacts;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de charger les contacts de discussion: " + ex.getMessage(), ex);
        }
    }

    private void renderMessengerConversation(ChatContact selectedContact, VBox messageList, ScrollPane messageScroll,
                                             Label headerName, Label headerMeta, Circle headerStatusDot, Label statusLabel,
                                             Button cameraBtn, boolean scrollToBottom) {
        double previousScrollValue = messageScroll.getVvalue();
        boolean wasNearBottom = previousScrollValue >= 0.95;
        messageList.getChildren().clear();
        if (selectedContact == null) {
            headerName.setText("Discussion");
            headerMeta.setText("Selectionnez un contact");
            headerStatusDot.setFill(Color.web("#94a3b8"));
            if (cameraBtn != null) {
                cameraBtn.setDisable(true);
            }
            return;
        }

        headerName.setText(selectedContact.displayName());
        headerMeta.setText(selectedContact.roleLabel() + " | " + (isChatContactOnline(selectedContact.lastSeen()) ? "En ligne" : "Hors ligne"));
        headerStatusDot.setFill(isChatContactOnline(selectedContact.lastSeen()) ? Color.web("#22c55e") : Color.web("#94a3b8"));
        if (cameraBtn != null) {
            cameraBtn.setDisable(false);
        }
        markConversationAsRead(selectedContact.id());

        List<ChatMessage> messages = loadMessengerMessages(selectedContact.id());
        if (messages.isEmpty()) {
            Label emptyLabel = new Label("Aucun message pour le moment. Commencez la discussion.");
            emptyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: 700;");
            emptyLabel.setPadding(new Insets(20));
            messageList.getChildren().add(emptyLabel);
            statusLabel.setText("Conversation prete.");
            return;
        }

        for (ChatMessage message : messages) {
            boolean mine = message.senderId().equals((long) currentUser.getId());
            Region bubbleContent;
            String messageText = message.messageText();
            if (messageText != null && messageText.contains("https://meet.jit.si/")) {
                VBox meetBox = new VBox(8);
                Label bubble = new Label(messageText);
                bubble.setWrapText(true);
                bubble.setMaxWidth(360);
                bubble.setStyle(mine
                        ? "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700;"
                        : "-fx-text-fill: #0f172a; -fx-font-size: 13px; -fx-font-weight: 700;");

                Button joinBtn = new Button("Rejoindre la visio");
                joinBtn.setStyle("-fx-background-color: white; -fx-text-fill: #0f766e; -fx-font-size: 12px; "
                        + "-fx-font-weight: 900; -fx-background-radius: 14; -fx-padding: 6 12 6 12;");
                joinBtn.setOnAction(e -> runAction(() -> openMeetLinkFromMessage(messageText)));

                meetBox.getChildren().addAll(bubble, joinBtn);
                meetBox.setMaxWidth(360);
                meetBox.setPadding(new Insets(10, 14, 10, 14));
                meetBox.setStyle(mine
                        ? "-fx-background-color: linear-gradient(to right, #2563eb, #1d4ed8); -fx-background-radius: 18;"
                        : "-fx-background-color: #e2e8f0; -fx-background-radius: 18;");
                bubbleContent = meetBox;
            } else {
                Label bubble = new Label(messageText);
                bubble.setWrapText(true);
                bubble.setMaxWidth(360);
                bubble.setPadding(new Insets(10, 14, 10, 14));
                bubble.setStyle(mine
                        ? "-fx-background-color: linear-gradient(to right, #2563eb, #1d4ed8); -fx-background-radius: 18; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700;"
                        : "-fx-background-color: #e2e8f0; -fx-background-radius: 18; -fx-text-fill: #0f172a; -fx-font-size: 13px; -fx-font-weight: 700;");
                bubbleContent = bubble;
            }

            Label meta = new Label((mine ? "Vous" : selectedContact.displayName()) + " • " + formatChatDateTime(message.createdAt()));
            meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

            VBox bubbleBox = new VBox(4, bubbleContent, meta);
            bubbleBox.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            HBox row = new HBox(bubbleBox);
            row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            messageList.getChildren().add(row);
        }

        statusLabel.setText(messages.size() + " message(s) charges.");
        restoreMessageScrollPosition(messageScroll, scrollToBottom || wasNearBottom ? 1.0 : previousScrollValue);
    }

    private void restoreMessageScrollPosition(ScrollPane messageScroll, double targetScrollValue) {
        Platform.runLater(() -> {
            messageScroll.applyCss();
            messageScroll.layout();
            messageScroll.setVvalue(targetScrollValue);
        });
    }

    private List<ChatMessage> loadMessengerMessages(Long otherUserId) {
        String sql = "SELECT id, sender_id, receiver_id, message_text, created_at "
                + "FROM suivi_chat_messages "
                + "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) "
                + "ORDER BY created_at ASC, id ASC";
        try (PreparedStatement statement = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            statement.setLong(1, currentUser.getId());
            statement.setLong(2, otherUserId);
            statement.setLong(3, otherUserId);
            statement.setLong(4, currentUser.getId());
            try (ResultSet rs = statement.executeQuery()) {
                ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
                while (rs.next()) {
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    messages.add(new ChatMessage(
                            rs.getLong("id"),
                            rs.getLong("sender_id"),
                            rs.getLong("receiver_id"),
                            valueOrDash(rs.getString("message_text")),
                            createdAt == null ? LocalDateTime.now() : createdAt.toLocalDateTime()
                    ));
                }
                return messages;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de charger les messages: " + ex.getMessage(), ex);
        }
    }

    private void sendMessengerMessage(Long receiverId, String messageText) {
        if (receiverId == null) {
            throw new IllegalArgumentException("Destinataire introuvable.");
        }
        if (currentUser != null && receiverId.equals((long) currentUser.getId())) {
            throw new IllegalArgumentException("Vous ne pouvez pas envoyer un message a votre propre compte.");
        }
        String text = requiredText(messageText, "Message");
        String sql = "INSERT INTO suivi_chat_messages (sender_id, receiver_id, message_text, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement statement = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            statement.setLong(1, currentUser.getId());
            statement.setLong(2, receiverId);
            statement.setString(3, text);
            statement.executeUpdate();
            refreshMessengerPresence();
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible d'envoyer le message: " + ex.getMessage(), ex);
        }
    }

    private void markConversationAsRead(Long contactId) {
        String sql = "INSERT INTO suivi_chat_read_state (user_id, contact_id, last_read_at) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP) "
                + "ON DUPLICATE KEY UPDATE last_read_at = CURRENT_TIMESTAMP";
        try (PreparedStatement statement = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            statement.setLong(1, currentUser.getId());
            statement.setLong(2, contactId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de marquer les messages comme lus: " + ex.getMessage(), ex);
        }
    }

    private void openMeetForContact(ChatContact contact) {
        if (contact == null) {
            throw new IllegalStateException("Aucun contact selectionne pour la visio.");
        }

        String meetUrl = buildMeetUrl(contact.id());
        sendMessengerMessage(contact.id(), "Lien de visio: " + meetUrl);

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(meetUrl));
            } else {
                throw new IllegalStateException("Aucun navigateur disponible pour ouvrir la visio.");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible d'ouvrir la visio: " + ex.getMessage(), ex);
        }
    }

    private void openMeetLinkFromMessage(String messageText) {
        String marker = "https://meet.jit.si/";
        int start = messageText == null ? -1 : messageText.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Aucun lien de visio detecte dans ce message.");
        }

        String meetUrl = messageText.substring(start).trim();
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(meetUrl));
            } else {
                throw new IllegalStateException("Aucun navigateur disponible pour ouvrir la visio.");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible d'ouvrir la visio: " + ex.getMessage(), ex);
        }
    }

    private String buildMeetUrl(Long contactId) {
        long currentId = currentUser == null ? 0 : currentUser.getId();
        long minId = Math.min(currentId, contactId == null ? 0 : contactId);
        long maxId = Math.max(currentId, contactId == null ? 0 : contactId);
        return "https://meet.jit.si/chroniccare-" + minId + "-" + maxId;
    }

    private void selectPreferredChatContact(ListView<ChatContact> contactList, Long preferredContactId) {
        if (contactList.getItems().isEmpty()) {
            return;
        }
        if (preferredContactId != null) {
            for (ChatContact contact : contactList.getItems()) {
                if (contact.id().equals(preferredContactId)) {
                    contactList.getSelectionModel().select(contact);
                    return;
                }
            }
        }
        if (contactList.getSelectionModel().getSelectedItem() == null) {
            contactList.getSelectionModel().selectFirst();
        }
    }

    private Long resolvePreferredMessengerContactId() {
        if (isPatient()) {
            return null;
        }
        if (etatTable != null && etatTable.getSelectionModel().getSelectedItem() != null) {
            return etatTable.getSelectionModel().getSelectedItem().getUtilisateurId();
        }
        if (activiteTable != null && activiteTable.getSelectionModel().getSelectedItem() != null) {
            return activiteTable.getSelectionModel().getSelectedItem().getUtilisateurId();
        }
        if (etatPatientCombo != null && etatPatientCombo.getValue() != null) {
            return etatPatientCombo.getValue().id();
        }
        if (activitePatientCombo != null && activitePatientCombo.getValue() != null) {
            return activitePatientCombo.getValue().id();
        }
        return null;
    }

    private boolean isChatContactOnline(Timestamp lastSeen) {
        if (lastSeen == null) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CHAT_ONLINE_MINUTES);
        return !lastSeen.toLocalDateTime().isBefore(threshold);
    }

    private String resolveCurrentUserDisplayName() {
        if (currentUser == null) {
            return "Utilisateur";
        }
        return buildDisplayName(currentUser.getPrenom(), currentUser.getNom(), currentUser.getEmail(), currentUser.getId());
    }

    private String buildDisplayName(String prenom, String nom, String email, long fallbackId) {
        String fullName = optionalText(prenom) + " " + optionalText(nom);
        String normalized = fullName.trim();
        if (!normalized.isBlank()) {
            return normalized;
        }
        String safeEmail = optionalText(email);
        return safeEmail.isBlank() ? "Utilisateur #" + fallbackId : safeEmail;
    }

    private String resolveRoleLabel(String roles) {
        String normalized = optionalText(roles);
        if (normalized.contains("ROLE_NUTRITIONNISTE")) {
            return "Nutritionniste";
        }
        if (normalized.contains("ROLE_COACH")) {
            return "Coach";
        }
        if (normalized.contains("ROLE_PATIENT")) {
            return "Patient";
        }
        if (normalized.contains("ROLE_ADMIN")) {
            return "Admin";
        }
        return "Utilisateur";
    }

    private String formatChatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }

    public void showEtatTab() {
        if (tabPane != null) {
            tabPane.getSelectionModel().select(0);
        }
    }

    public void showActiviteTab() {
        if (tabPane != null) {
            tabPane.getSelectionModel().select(1);
        }
    }

    public void showLoginMessageNotification() {
        if (!canUseMessenger() || currentUser == null) {
            return;
        }
        ensureMessengerTables();
        ChatNotification notification = findLatestUnreadNotificationBeforePresenceRefresh();
        refreshMessengerPresence();
        if (notification == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nouveau message");
        alert.setHeaderText("Vous avez un nouveau message");
        alert.setContentText("De: " + notification.senderName()
                + "\nMessages non lus: " + notification.unreadCount()
                + "\n\n" + notification.preview());
        alert.showAndWait();
    }

    private void handleAddEtat() {
        EtatFormData data = validateEtatForm();
        Etat savedEtat = etatController.createEtat(data.utilisateurId(), data.traitementEnCours(), data.remarquesCliniques(),
                data.temperatureCorporelle(), data.niveauHydratation(), data.dateReleve());
        clearEtatForm();
        refreshEtatData();
        refreshActiviteData();
        Activite generatedActivite = findGeneratedActivite(savedEtat.getId()).orElse(null);
        updateEtatGeneratedActivityBox(savedEtat, generatedActivite);
        showPatientHealthReportButton();
        etatTable.refresh();
    }

    private void handleUpdateEtat() {
        Etat selected = selectedEtat();
        EtatFormData data = validateEtatForm();
        etatController.updateEtat(selected.getId(), data.utilisateurId(), data.traitementEnCours(),
                data.remarquesCliniques(), data.temperatureCorporelle(), data.niveauHydratation(), data.dateReleve());
        clearEtatForm();
        refreshEtatData();
        refreshActiviteData();
        showPatientHealthReportButton();
    }

    private void handleDeleteEtat() {
        if (confirmDelete("Supprimer cet etat ?")) {
            etatController.deleteEtat(selectedEtat().getId());
            clearEtatForm();
            refreshEtatData();
            refreshActiviteData();
            showPatientHealthReportButton();
        }
    }

    private void handleAddActivite() {
        ActiviteFormData data = validateActiviteForm();
        activiteController.createActivite(data.utilisateurId(), activiteSelectedEtatId, data.type(), data.duree(),
                data.calories(), data.distanceKm(), data.heuresRepos(), data.dateActivite(), data.notes());
        clearActiviteForm();
        refreshActiviteData();
        showPatientHealthReportButton();
    }

    private void handleUpdateActivite() {
        Activite selected = selectedActivite();
        ActiviteFormData data = validateActiviteForm();
        activiteController.updateActivite(selected.getId(), data.utilisateurId(), activiteSelectedEtatId, data.type(),
                data.duree(), data.calories(), data.distanceKm(), data.heuresRepos(), data.dateActivite(), data.notes());
        clearActiviteForm();
        refreshActiviteData();
        showPatientHealthReportButton();
    }

    private void handleDeleteActivite() {
        if (confirmDelete("Supprimer cette activite ?")) {
            activiteController.deleteActivite(selectedActivite().getId());
            clearActiviteForm();
            refreshActiviteData();
            showPatientHealthReportButton();
        }
    }

    private void loadPatientsFromDatabase() {
        String sql = "SELECT id, nom, prenom, email, roles FROM users ORDER BY prenom, nom, email";
        try (PreparedStatement statement = MyDatabase.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            patientById.clear();
            etatPatientCombo.getItems().clear();
            activitePatientCombo.getItems().clear();
            while (rs.next()) {
                String roles = rs.getString("roles");
                if (roles == null || !roles.contains("ROLE_PATIENT")) continue;
                long id = rs.getLong("id");
                PatientOption patient = new PatientOption(id,
                        valueOrDash(rs.getString("prenom")) + " " + valueOrDash(rs.getString("nom")),
                        valueOrDash(rs.getString("email")));
                patientById.put(id, patient);
                etatPatientCombo.getItems().add(patient);
                activitePatientCombo.getItems().add(patient);
            }
            if (isPatient()) {
                selectPatient(etatPatientCombo, (long) currentUser.getId());
                selectPatient(activitePatientCombo, (long) currentUser.getId());
                etatPatientCombo.setDisable(true);
                activitePatientCombo.setDisable(true);
            }
            updateEtatLabelForPatient(activitePatientCombo.getValue());
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de charger les patients: " + ex.getMessage(), ex);
        }
    }

    private void refreshEtatData() {
        etatItems.setAll(filterEtats(etatController.listEtats()));
        applyEtatFilters();
        updateEtatStats();
        updateEtatLabelForPatient(activitePatientCombo == null ? null : activitePatientCombo.getValue());
    }

    private void refreshActiviteData() {
        activiteItems.setAll(filterActivites(activiteController.listActivites()));
        applyActiviteFilters();
        updateActiviteStats();
    }

    private void updateEtatStats() {
        int totalEtats = etatItems.size();
        int totalPatients = etatItems.stream()
                .map(Etat::getUtilisateurId)
                .filter(id -> id != null)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .size();
        long withTemperature = etatItems.stream()
                .map(Etat::getTemperatureCorporelle)
                .filter(value -> value != null && !value.isBlank())
                .count();
        long withHydratation = etatItems.stream()
                .map(Etat::getNiveauHydratation)
                .filter(value -> value != null && !value.isBlank())
                .count();
        etatStatsLabel.setText("Statistiques visibles: " + totalEtats + " etat(s), " + totalPatients + " patient(s), "
                + withTemperature + " releve(s) de temperature, "
                + withHydratation + " releve(s) d'hydratation.");
    }

    private void updateActiviteStats() {
        int totalActivites = activiteItems.size();
        int totalPatients = activiteItems.stream()
                .map(Activite::getUtilisateurId)
                .filter(id -> id != null)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .size();
        int totalDuree = activiteItems.stream()
                .map(Activite::getDuree)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        int totalCalories = activiteItems.stream()
                .map(Activite::getCalories)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        int totalReposHeures = activiteItems.stream()
                .map(Activite::getHeuresRepos)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        double totalDistanceKm = activiteItems.stream()
                .map(Activite::getDistanceKm)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        activiteStatsLabel.setText("Statistiques visibles: " + totalActivites + " activite(s), " + totalPatients + " patient(s), "
                + totalDuree + " min, " + totalCalories + " calories, " + formatDistance(totalDistanceKm) + " parcourus, "
                + formatRepos(totalReposHeures) + " de repos.");
    }

    private void showEtatStatsDialog() {
        PieChart temperatureChart = new PieChart();
        temperatureChart.setTitle("Temperature (deg C)");
        Map<String, Integer> temperatureCounts = new HashMap<>();
        for (Etat etat : etatItems) {
            String key = formatTemperature(etat.getTemperatureCorporelle());
            temperatureCounts.merge(key, 1, Integer::sum);
        }
        temperatureCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> temperatureChart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue())));

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Patient");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Hydratation moyenne (L)");
        BarChart<String, Number> hydratationChart = new BarChart<>(xAxis, yAxis);
        hydratationChart.setTitle("Hydratation moyenne par patient");
        XYChart.Series<String, Number> hydratationSeries = new XYChart.Series<>();
        Map<String, Double> hydratationTotals = new HashMap<>();
        Map<String, Integer> hydratationReleves = new HashMap<>();
        for (Etat etat : etatItems) {
            if (etat.getNiveauHydratation() == null || etat.getNiveauHydratation().isBlank()) {
                continue;
            }
            String key = resolvePatientChartLabel(etat.getUtilisateurId());
            hydratationTotals.merge(key, parseDoubleSafe(etat.getNiveauHydratation()), Double::sum);
            hydratationReleves.merge(key, 1, Integer::sum);
        }
        hydratationTotals.entrySet().stream()
                .filter(entry -> hydratationReleves.getOrDefault(entry.getKey(), 0) > 0)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    int releves = hydratationReleves.getOrDefault(entry.getKey(), 0);
                    double moyenne = releves == 0 ? 0 : entry.getValue() / releves;
                    hydratationSeries.getData().add(new XYChart.Data<>(entry.getKey(), moyenne));
                });
        hydratationChart.getData().add(hydratationSeries);
        hydratationChart.setLegendVisible(false);
        hydratationChart.setAnimated(false);
        hydratationChart.setCategoryGap(18);
        hydratationChart.setBarGap(6);
        hydratationChart.setPrefHeight(280);

        VBox content = new VBox(14,
                new Label("Statistiques graphiques des etats visibles"),
                temperatureChart,
                hydratationChart
        );
        showStatsDialog("Statistiques Etat", content);
    }

    private void showActiviteStatsDialog() {
        PieChart typeChart = new PieChart();
        typeChart.setTitle("Activites par type");
        Map<String, Integer> typeCounts = new HashMap<>();
        for (Activite activite : activiteItems) {
            String key = valueOrDash(activite.getType());
            typeCounts.merge(key, 1, Integer::sum);
        }
        typeCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> typeChart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue())));

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Patient");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Distance (km)");
        BarChart<String, Number> distanceChart = new BarChart<>(xAxis, yAxis);
        distanceChart.setTitle("Distance totale par patient");
        XYChart.Series<String, Number> distanceSeries = new XYChart.Series<>();
        Map<String, Double> distanceByPatient = new HashMap<>();
        for (Activite activite : activiteItems) {
            String key = resolvePatientChartLabel(activite.getUtilisateurId());
            distanceByPatient.merge(key, activite.getDistanceKm() == null ? 0.0 : activite.getDistanceKm(), Double::sum);
        }
        distanceByPatient.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> distanceSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));
        if (distanceSeries.getData().isEmpty()) {
            yAxis.setLabel("Duree (min)");
            distanceChart.setTitle("Duree totale par patient");
            Map<String, Integer> dureeByPatient = new HashMap<>();
            for (Activite activite : activiteItems) {
                String key = resolvePatientChartLabel(activite.getUtilisateurId());
                dureeByPatient.merge(key, activite.getDuree() == null ? 0 : activite.getDuree(), Integer::sum);
            }
            dureeByPatient.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> distanceSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));
        }
        distanceChart.getData().add(distanceSeries);
        distanceChart.setLegendVisible(false);
        distanceChart.setAnimated(false);
        distanceChart.setCategoryGap(20);
        distanceChart.setBarGap(6);
        distanceChart.setPrefHeight(320);

        VBox content = new VBox(14,
                new Label("Statistiques graphiques des activites visibles"),
                new Label("Base de calcul: activites visibles dans la liste actuelle. Le graphe montre la distance totale par patient en km. Si aucune distance n'est renseignee, il affiche la duree totale par patient en minutes."),
                typeChart,
                distanceChart
        );
        showStatsDialog("Statistiques Activite", content);
    }

    private void showStatsDialog(String title, VBox content) {
        content.setStyle("-fx-padding: 16;");
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        Window owner = tabPane == null || tabPane.getScene() == null ? null : tabPane.getScene().getWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle(title);
        stage.setScene(new Scene(content, 760, 620));
        stage.setResizable(false);
        stage.showAndWait();
    }

    private void exportEtatsCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("id;utilisateur_id;patient;traitement;remarques;temperature;hydratation;date_releve\n");
        for (Etat etat : etatItems) {
            csv.append(csvValue(etat.getId())).append(';')
                    .append(csvValue(etat.getUtilisateurId())).append(';')
                    .append(csvValue(resolveUserLabel(etat.getUtilisateurId()))).append(';')
                    .append(csvValue(etat.getTraitementEnCours())).append(';')
                    .append(csvValue(etat.getRemarquesCliniques())).append(';')
                    .append(csvValue(etat.getTemperatureCorporelle())).append(';')
                    .append(csvValue(etat.getNiveauHydratation())).append(';')
                    .append(csvValue(formatExportDate(etat.getDateReleve())))
                    .append('\n');
        }
        writeCsvFile("export-etats.csv", csv.toString());
    }

    private void exportActivitesCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("id;utilisateur_id;patient;etat_id;etat;type;duree;calories;distance;repos;date_activite;notes;created_at\n");
        for (Activite activite : activiteItems) {
            csv.append(csvValue(activite.getId())).append(';')
                    .append(csvValue(activite.getUtilisateurId())).append(';')
                    .append(csvValue(resolveUserLabel(activite.getUtilisateurId()))).append(';')
                    .append(csvValue(activite.getEtatId())).append(';')
                    .append(csvValue(findEtatLabel(activite.getEtatId()))).append(';')
                    .append(csvValue(activite.getType())).append(';')
                    .append(csvValue(activite.getDuree())).append(';')
                    .append(csvValue(activite.getCalories())).append(';')
                    .append(csvValue(activite.getDistanceKm())).append(';')
                    .append(csvValue(activite.getHeuresRepos())).append(';')
                    .append(csvValue(formatExportDate(activite.getDateActivite()))).append(';')
                    .append(csvValue(activite.getNotes())).append(';')
                    .append(csvValue(formatExportDate(activite.getCreatedAt())))
                    .append('\n');
        }
        writeCsvFile("export-activites.csv", csv.toString());
    }

    private void writeCsvFile(String defaultFileName, String content) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer l'export CSV");
        chooser.setInitialFileName(defaultFileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        Window owner = tabPane == null || tabPane.getScene() == null ? null : tabPane.getScene().getWindow();
        java.io.File selectedFile = chooser.showSaveDialog(owner);
        if (selectedFile == null) return;

        try {
            Path path = selectedFile.toPath();
            Files.writeString(path, content, StandardCharsets.UTF_8);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Export termine");
            alert.setContentText("Fichier cree: " + path.toAbsolutePath());
            alert.showAndWait();
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible d'ecrire le fichier CSV: " + ex.getMessage(), ex);
        }
    }

    private String formatExportDate(LocalDateTime value) {
        return value == null ? "" : EXPORT_DATE_FORMAT.format(value);
    }

    private String csvValue(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private List<Etat> filterEtats(List<Etat> etats) {
        if (!isPatient()) return etats;
        long userId = currentUser.getId();
        return etats.stream().filter(e -> e.getUtilisateurId() != null && e.getUtilisateurId() == userId).toList();
    }

    private List<Activite> filterActivites(List<Activite> activites) {
        if (!isPatient()) return activites;
        long userId = currentUser.getId();
        return activites.stream().filter(a -> a.getUtilisateurId() != null && a.getUtilisateurId() == userId).toList();
    }

    private void updateEtatLabelForPatient(PatientOption patient) {
        if (patient == null) {
            activiteSelectedEtatId = null;
            activiteEtatLabel.setText("-");
            return;
        }
        Optional<Etat> latestEtat = etatController.listEtats().stream()
                .filter(e -> patient.id().equals(e.getUtilisateurId()))
                .max(Comparator.comparing(Etat::getDateReleve, Comparator.nullsLast(Comparator.naturalOrder())));
        if (latestEtat.isPresent()) {
            Etat etat = latestEtat.get();
            activiteSelectedEtatId = etat.getId();
            activiteEtatLabel.setText("Traitement: " + valueOrDash(etat.getTraitementEnCours()) + " | Temp: "
                    + formatTemperature(etat.getTemperatureCorporelle()) + " | Hyd: " + formatHydratation(etat.getNiveauHydratation()));
        } else {
            activiteSelectedEtatId = null;
            activiteEtatLabel.setText("Aucun etat pour ce patient.");
        }
    }

    private EtatFormData validateEtatForm() {
        PatientOption patient = requireSelection(etatPatientCombo, "Choisissez un patient.");
        return new EtatFormData(patient.id(), requiredText(etatTraitementField.getText(), "Traitement"),
                optionalText(etatRemarquesField.getText()), optionalText(etatTemperatureCombo.getValue()),
                optionalText(etatHydratationCombo.getValue()),
                requireNonPastDate(etatDateReleveField.getValue(), "Date releve").atStartOfDay());
    }

    private ActiviteFormData validateActiviteForm() {
        PatientOption patient = requireSelection(activitePatientCombo, "Choisissez un patient.");
        return new ActiviteFormData(patient.id(), requiredText(activiteTypeCombo.getValue(), "Type"),
                parseIntRequired(activiteDureeField.getText(), "Duree"),
                parseIntRequired(activiteCaloriesField.getText(), "Calories"),
                parseDoubleOptional(activiteDistanceField.getText(), "Distance"),
                parseIntOptional(activiteReposField.getText(), "Repos"),
                requireNonPastDate(activiteDateField.getValue(), "Date activite").atStartOfDay(),
                optionalText(activiteNotesField.getText()));
    }

    private <T> T requireSelection(ComboBox<T> comboBox, String message) {
        T value = comboBox.getValue();
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }

    private LocalDate requireNonPastDate(LocalDate date, String label) {
        if (date == null) throw new IllegalArgumentException(label + " obligatoire.");
        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException(label + " ne peut pas etre dans le passe.");
        return date;
    }

    private void installInputConstraints() {
        installIntegerFilter(activiteDureeField);
        installIntegerFilter(activiteCaloriesField);
        installIntegerFilter(activiteReposField);
        installDecimalFilter(activiteDistanceField);
        enforceMaxLength(etatTraitementField, 255);
        enforceMaxLength(etatRemarquesField, 2000);
        enforceMaxLength(activiteNotesField, 1000);
    }

    private void clearEtatForm() {
        etatTable.getSelectionModel().clearSelection();
        if (!etatPatientCombo.isDisabled()) etatPatientCombo.getSelectionModel().clearSelection();
        etatTraitementField.clear();
        etatRemarquesField.clear();
        etatTemperatureCombo.getSelectionModel().clearSelection();
        etatHydratationCombo.getSelectionModel().clearSelection();
        etatDateReleveField.setValue(LocalDate.now());
    }

    private VBox buildEtatGeneratedActivityBox() {
        etatGeneratedActivityTitleLabel = new Label("Activite generee");
        etatGeneratedActivityTitleLabel.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 700;");

        etatGeneratedActivityTypeLabel = buildEtatGeneratedActivityLine();
        etatGeneratedActivityDureeLabel = buildEtatGeneratedActivityLine();
        etatGeneratedActivityCaloriesLabel = buildEtatGeneratedActivityLine();
        etatGeneratedActivityDistanceLabel = buildEtatGeneratedActivityLine();
        etatGeneratedActivityReposLabel = buildEtatGeneratedActivityLine();
        etatGeneratedActivityDateLabel = buildEtatGeneratedActivityLine();
        etatGeneratedActivityTraitementLabel = buildEtatGeneratedActivityLine();
        etatGeneratedActivityConseilLabel = buildEtatGeneratedActivityLine();
        etatGeneratedActivityConseilLabel.setWrapText(true);

        VBox box = new VBox(
                6,
                etatGeneratedActivityTitleLabel,
                etatGeneratedActivityTypeLabel,
                etatGeneratedActivityDureeLabel,
                etatGeneratedActivityCaloriesLabel,
                etatGeneratedActivityDistanceLabel,
                etatGeneratedActivityReposLabel,
                etatGeneratedActivityDateLabel,
                etatGeneratedActivityTraitementLabel,
                etatGeneratedActivityConseilLabel,
                buildEtatGeneratedActivityCloseRow()
        );
        box.setVisible(false);
        box.setManaged(false);
        box.setStyle("-fx-background-color: #f8fbff; -fx-border-color: #d4e4ff; -fx-border-radius: 10; "
                + "-fx-background-radius: 10; -fx-padding: 12 14;");
        return box;
    }

    private HBox buildEtatGeneratedActivityCloseRow() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("Fermer");
        closeBtn.setFocusTraversable(false);
        closeBtn.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-font-weight: 700; "
                + "-fx-background-radius: 8; -fx-padding: 6 12; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> hideEtatGeneratedActivityBox());

        return new HBox(10, spacer, closeBtn);
    }

    private VBox buildPatientHealthReportBox() {
        VBox box = new VBox(8);
        box.setVisible(false);
        box.setManaged(false);
        box.setStyle("-fx-background-color: #f8fbff; -fx-border-color: #bfdbfe; -fx-border-width: 1; "
                + "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 14;");

        Label title = new Label("Rapport de sante IA - 6 etats");
        title.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 16px; -fx-font-weight: 800;");

        Label subtitle = new Label("Lecture seule - ouvrez votre rapport base sur vos 6 derniers etats et activites recentes.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-font-weight: 600;");

        Button openReportBtn = reportButton("Lire mon rapport", "#2563eb");
        openReportBtn.setOnAction(e -> runAction(this::openPatientReadonlyHealthReportDialog));

        box.getChildren().addAll(title, subtitle, openReportBtn);
        return box;
    }

    private void showPatientHealthReportButton() {
        if (!isPatient() || patientHealthReportBox == null || currentUser == null) {
            return;
        }

        patientHealthReportBox.setVisible(true);
        patientHealthReportBox.setManaged(true);
    }

    private void openPatientReadonlyHealthReportDialog() {
        if (!isPatient() || currentUser == null) {
            throw new IllegalStateException("Rapport patient indisponible.");
        }

        Long userId = (long) currentUser.getId();
        PatientOption patient = patientById.get(userId);
        List<Etat> recentEtats = etatController.listEtats().stream()
                .filter(etat -> userId.equals(etat.getUtilisateurId()))
                .sorted(Comparator.comparing(Etat::getDateReleve, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .sorted(Comparator.comparing(Etat::getDateReleve, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<Activite> recentActivites = activiteController.listActivites().stream()
                .filter(activite -> userId.equals(activite.getUtilisateurId()))
                .sorted(Comparator.comparing(Activite::getDateActivite, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .sorted(Comparator.comparing(Activite::getDateActivite, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        requireMinimumEtatsForReport(recentEtats);

        LocalDate startDate = recentEtats.get(0).getDateReleve().toLocalDate();
        LocalDate today = recentEtats.get(recentEtats.size() - 1).getDateReleve().toLocalDate();

        String report = buildHealthReport(
                patient,
                recentEtats,
                recentActivites,
                generateAiHealthRemark(patient, recentEtats, recentActivites),
                startDate,
                today
        );
        openReadonlyReportDialog("Mon rapport de sante IA", resolveUserLabel(userId), report, startDate, today);
    }

    private Label buildEtatGeneratedActivityLine() {
        Label label = new Label();
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #16324f; -fx-font-size: 13px; -fx-font-weight: 600;");
        return label;
    }

    private void updateEtatGeneratedActivityBox(Etat etat, Activite activite) {
        if (etatGeneratedActivityBox == null || etat == null) {
            return;
        }

        String type = activite == null ? suggestActivityType(etat) : valueOrDash(activite.getType());
        String duree = activite == null ? "-" : formatDuree(activite);
        String calories = activite == null ? "-" : valueOrDash(String.valueOf(activite.getCalories())) + " kcal";
        String distance = activite == null ? "-" : formatDistanceOrDash(activite);
        String repos = activite == null ? "-" : formatRepos(activite.getHeuresRepos());
        String date = activite == null ? formatDate(etat.getDateReleve()) : formatDate(activite.getDateActivite());
        String traitement = valueOrDash(etat.getTraitementEnCours());
        String conseil = activite == null ? buildLogicalActivityMessage(etat) : formatConseil(activite, etat);

        etatGeneratedActivityTypeLabel.setText("Type: " + type);
        etatGeneratedActivityDureeLabel.setText("Duree: " + duree);
        etatGeneratedActivityCaloriesLabel.setText("Calories: " + calories);
        etatGeneratedActivityDistanceLabel.setText("Distance: " + distance);
        etatGeneratedActivityReposLabel.setText("Repos: " + repos);
        etatGeneratedActivityDateLabel.setText("Date: " + date);
        etatGeneratedActivityTraitementLabel.setText("Traitement: " + traitement);
        etatGeneratedActivityConseilLabel.setText("Conseil: " + conseil);

        etatGeneratedActivityBox.setVisible(true);
        etatGeneratedActivityBox.setManaged(true);
    }

    private void hideEtatGeneratedActivityBox() {
        if (etatGeneratedActivityBox == null) {
            return;
        }
        etatGeneratedActivityBox.setVisible(false);
        etatGeneratedActivityBox.setManaged(false);
    }

    private void openGeneratedActivityDialog(Etat etat) {
        Activite activite = findGeneratedActivite(etat.getId())
                .orElseThrow(() -> new IllegalStateException("Aucune activite generee pour cet etat."));
        PatientOption patient = patientById.get(etat.getUtilisateurId());
        String patientEmail = patient == null ? "" : optionalText(patient.email());

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().setAll(Activite.TYPE_CHOICES);
        typeCombo.setValue(activite.getType());
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        TextField dureeField = new TextField(activite.getDuree() == null ? "" : String.valueOf(activite.getDuree()));
        TextField caloriesField = new TextField(activite.getCalories() == null ? "" : String.valueOf(activite.getCalories()));
        TextField distanceField = new TextField(activite.getDistanceKm() == null ? "" : String.valueOf(activite.getDistanceKm()));
        TextField reposField = new TextField(activite.getHeuresRepos() == null ? "" : String.valueOf(activite.getHeuresRepos()));
        DatePicker dateField = new DatePicker(activite.getDateActivite() == null ? LocalDate.now() : activite.getDateActivite().toLocalDate());
        TextArea notesField = new TextArea(optionalText(activite.getNotes()));
        notesField.setPrefRowCount(5);
        TextArea mailField = new TextArea(buildMailDraft(etat, activite, patientEmail));
        mailField.setPrefRowCount(7);
        styleMailDialogControl(typeCombo);
        styleMailDialogControl(dureeField);
        styleMailDialogControl(caloriesField);
        styleMailDialogControl(distanceField);
        styleMailDialogControl(reposField);
        styleMailDialogControl(dateField);
        styleMailDialogControl(notesField);
        styleMailDialogControl(mailField);

        installIntegerFilter(dureeField);
        installIntegerFilter(caloriesField);
        installIntegerFilter(reposField);
        installDecimalFilter(distanceField);

        Label helpLabel = new Label("Patient: " + resolveUserLabel(etat.getUtilisateurId())
                + "\nEtat: " + valueOrDash(etat.getTraitementEnCours())
                + " | Temp: " + formatTemperature(etat.getTemperatureCorporelle())
                + " | Hyd: " + formatHydratation(etat.getNiveauHydratation()));
        helpLabel.setWrapText(true);
        helpLabel.setStyle("-fx-text-fill: #16324f; -fx-font-size: 13px; -fx-font-weight: 700; "
                + "-fx-background-color: #eaf3ff; -fx-padding: 12; -fx-background-radius: 12; "
                + "-fx-border-color: #bfdbfe; -fx-border-radius: 12;");

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #1d4ed8; -fx-font-size: 12px; -fx-font-weight: 700;");

        GridPane form = formGrid();
        form.addRow(0, formLabel("Type"), typeCombo);
        form.addRow(1, formLabel("Duree"), dureeField);
        form.addRow(2, formLabel("Calories"), caloriesField);
        form.addRow(3, formLabel("Distance"), distanceField);
        form.addRow(4, formLabel("Repos"), reposField);
        form.addRow(5, formLabel("Date"), dateField);
        form.addRow(6, formLabel("Notes"), notesField);
        form.addRow(7, formLabel("Mail patient"), mailField);
        GridPane.setHgrow(typeCombo, Priority.ALWAYS);
        GridPane.setHgrow(dureeField, Priority.ALWAYS);
        GridPane.setHgrow(caloriesField, Priority.ALWAYS);
        GridPane.setHgrow(distanceField, Priority.ALWAYS);
        GridPane.setHgrow(reposField, Priority.ALWAYS);
        GridPane.setHgrow(notesField, Priority.ALWAYS);
        GridPane.setHgrow(mailField, Priority.ALWAYS);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Consulter l'activite generee");

        Label title = new Label("Consulter et envoyer l'activite");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Button enregistrerBtn = reportButton("Enregistrer", "#2563eb");
        Button envoyerBtn = reportButton("Envoyer par mail", "#0f766e");
        Button rapportBtn = canGenerateAiReport() ? button("Rapport IA 6 etats", "btn-neutral") : null;
        if (rapportBtn != null) {
            rapportBtn.setPrefWidth(170);
            rapportBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-weight: 800; "
                    + "-fx-background-radius: 10; -fx-padding: 10 14; -fx-cursor: hand;");
        }
        Button fermerBtn = reportButton("Fermer", "#64748b");

        enregistrerBtn.setOnAction(e -> runAction(() -> {
            Activite updated = saveReviewedActivity(activite, etat, typeCombo, dureeField, caloriesField, distanceField, reposField, dateField, notesField);
            refreshEtatData();
            refreshActiviteData();
            updateEtatGeneratedActivityBox(etat, updated);
            etatTable.refresh();
            statusLabel.setText("Mise a jour avec succes.");
            mailField.setText(buildMailDraft(etat, updated, patientEmail));
        }));

        envoyerBtn.setOnAction(e -> runAction(() -> {
            Activite updated = saveReviewedActivity(activite, etat, typeCombo, dureeField, caloriesField, distanceField, reposField, dateField, notesField);
            refreshEtatData();
            refreshActiviteData();
            updateEtatGeneratedActivityBox(etat, updated);
            etatTable.refresh();
            sendPatientMail(patientEmail, "Mise a jour de votre activite", optionalText(mailField.getText()));
            statusLabel.setText("Email envoye avec succes.");
        }));

        if (rapportBtn != null) {
            rapportBtn.setOnAction(e -> runAction(() -> openHealthReportDialog(etat.getUtilisateurId())));
        }
        fermerBtn.setOnAction(e -> stage.close());

        HBox actions = new HBox(10, enregistrerBtn, envoyerBtn, fermerBtn);
        if (rapportBtn != null) {
            actions.getChildren().add(2, rapportBtn);
        }
        VBox root = new VBox(14, title, helpLabel, form, statusLabel, actions);
        root.setStyle("-fx-padding: 22; -fx-background-color: linear-gradient(to bottom right, #f8fafc, #eef6ff);");
        stage.setScene(new Scene(root, 760, 620));
        stage.showAndWait();
    }

    private void styleMailDialogControl(javafx.scene.control.Control control) {
        control.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10; "
                + "-fx-background-radius: 10; -fx-padding: 8; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
    }

    private Activite saveReviewedActivity(
            Activite activite,
            Etat etat,
            ComboBox<String> typeCombo,
            TextField dureeField,
            TextField caloriesField,
            TextField distanceField,
            TextField reposField,
            DatePicker dateField,
            TextArea notesField
    ) {
        String type = requiredText(typeCombo.getValue(), "Type");
        Integer calories = parseIntRequired(caloriesField.getText(), "Calories");
        Integer repos = parseIntOptional(reposField.getText(), "Repos");
        Double distance = parseDoubleOptional(distanceField.getText(), "Distance");
        LocalDateTime dateActivite = requireNonPastDate(dateField.getValue(), "Date activite").atStartOfDay();
        String notes = optionalText(notesField.getText());

        Integer duree;
        if ("repos".equalsIgnoreCase(type)) {
            duree = 0;
            distance = null;
            if (repos == null || repos <= 0) {
                throw new IllegalArgumentException("Repos obligatoire pour une activite de type repos.");
            }
        } else {
            duree = parseIntRequired(dureeField.getText(), "Duree");
        }

        return activiteController.updateActivite(
                activite.getId(),
                etat.getUtilisateurId(),
                etat.getId(),
                type,
                duree,
                calories,
                distance,
                repos,
                dateActivite,
                notes
        );
    }

    private String buildMailDraft(Etat etat, Activite activite, String patientEmail) {
        return "Bonjour,\n\n"
                + "Votre activite a ete examinee par votre coach / nutritionniste.\n\n"
                + "Patient: " + valueOrDash(patientEmail) + "\n"
                + "Traitement: " + valueOrDash(etat.getTraitementEnCours()) + "\n"
                + "Temperature: " + formatTemperature(etat.getTemperatureCorporelle()) + "\n"
                + "Hydratation: " + formatHydratation(etat.getNiveauHydratation()) + "\n\n"
                + "Activite retenue:\n"
                + "- Type: " + valueOrDash(activite.getType()) + "\n"
                + "- Duree: " + formatDuree(activite) + "\n"
                + "- Calories: " + valueOrDash(String.valueOf(activite.getCalories())) + " kcal\n"
                + "- Distance: " + formatDistanceOrDash(activite) + "\n"
                + "- Repos: " + formatRepos(activite.getHeuresRepos()) + "\n"
                + "- Date: " + formatDate(activite.getDateActivite()) + "\n\n"
                + "Commentaire:\n"
                + formatConseil(activite, etat)
                + "\n\nCordialement.";
    }

    private void sendPatientMail(String patientEmail, String subject, String body) {
        if (patientEmail == null || patientEmail.isBlank() || "-".equals(patientEmail)) {
            throw new IllegalStateException("Email patient introuvable.");
        }
        smtpMailService.sendMail(patientEmail, subject, body);
    }

    private void openSelectedPatientHealthReportDialog() {
        if (!canGenerateAiReport()) {
            throw new IllegalStateException("Seul le coach ou le nutritionniste peut generer et envoyer un rapport IA.");
        }

        Long userId = null;
        Etat selectedEtat = etatTable == null ? null : etatTable.getSelectionModel().getSelectedItem();
        Activite selectedActivite = activiteTable == null ? null : activiteTable.getSelectionModel().getSelectedItem();

        if (selectedEtat != null) {
            userId = selectedEtat.getUtilisateurId();
        } else if (selectedActivite != null) {
            userId = selectedActivite.getUtilisateurId();
        } else if (etatPatientCombo != null && etatPatientCombo.getValue() != null) {
            userId = etatPatientCombo.getValue().id();
        } else if (activitePatientCombo != null && activitePatientCombo.getValue() != null) {
            userId = activitePatientCombo.getValue().id();
        } else if (isPatient() && currentUser != null) {
            userId = (long) currentUser.getId();
        }

        if (userId == null) {
            throw new IllegalStateException("Selectionnez un patient ou une ligne avant de generer le rapport.");
        }
        openHealthReportDialog(userId);
    }

    private void openHealthReportDialog(Long userId) {
        if (!canGenerateAiReport()) {
            throw new IllegalStateException("Seul le coach ou le nutritionniste peut generer et envoyer un rapport IA.");
        }

        PatientOption patient = patientById.get(userId);
        String patientEmail = patient == null ? "" : optionalText(patient.email());
        List<Etat> recentEtats = etatController.listEtats().stream()
                .filter(etat -> userId.equals(etat.getUtilisateurId()))
                .sorted(Comparator.comparing(Etat::getDateReleve, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .sorted(Comparator.comparing(Etat::getDateReleve, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<Activite> recentActivites = activiteController.listActivites().stream()
                .filter(activite -> userId.equals(activite.getUtilisateurId()))
                .sorted(Comparator.comparing(Activite::getDateActivite, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .sorted(Comparator.comparing(Activite::getDateActivite, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        requireMinimumEtatsForReport(recentEtats);

        LocalDate startDate = recentEtats.get(0).getDateReleve().toLocalDate();
        LocalDate today = recentEtats.get(recentEtats.size() - 1).getDateReleve().toLocalDate();

        String aiRemark = generateAiHealthRemark(patient, recentEtats, recentActivites);
        TextArea reportField = new TextArea(buildHealthReport(patient, recentEtats, recentActivites, aiRemark, startDate, today));
        reportField.setWrapText(true);
        reportField.setPrefRowCount(18);
        reportField.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #0f172a; "
                + "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label title = new Label("Rapport de sante IA - 6 etats");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Label subtitle = new Label("Patient: " + resolveUserLabel(userId) + " | Periode: " + startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " - " + today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-font-weight: 600;");

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #166534; -fx-font-size: 13px; -fx-font-weight: 800;");

        Button remarkBtn = reportButton("Generer remarque IA", "#0f766e");
        Button sendBtn = reportButton("Envoyer rapport par mail", "#2563eb");
        ComboBox<String> languageCombo = reportLanguageCombo();
        Button translateBtn = reportButton("Traduire", "#7c3aed");
        Button voiceBtn = reportButton("Voix IA", "#ea580c");
        Button pauseVoiceBtn = reportButton("Pause voix", "#b45309");
        Button closeBtn = reportButton("Fermer", "#64748b");

        remarkBtn.setOnAction(e -> runAction(() -> {
            String refreshedRemark = generateAiHealthRemark(patient, recentEtats, recentActivites);
            reportField.setText(buildHealthReport(patient, recentEtats, recentActivites, refreshedRemark, startDate, today));
            statusLabel.setText("Remarque IA generee avec succes.");
        }));

        sendBtn.setOnAction(e -> runAction(() -> {
            sendPatientMail(patientEmail, "Rapport de sante IA - 6 etats", optionalText(reportField.getText()));
            statusLabel.setText("Rapport envoye par mail avec succes.");
        }));

        translateBtn.setOnAction(e -> runAction(() -> {
            reportField.setText(translateReport(optionalText(reportField.getText()), languageCombo.getValue()));
            statusLabel.setText("Rapport traduit en " + languageCombo.getValue() + ".");
        }));

        voiceBtn.setOnAction(e -> runAction(() -> {
            speakReportWithAiVoice(optionalText(reportField.getText()));
            statusLabel.setText("Lecture vocale lancee.");
        }));
        pauseVoiceBtn.setOnAction(e -> {
            stopVoicePlayback();
            statusLabel.setText("Lecture vocale en pause.");
        });

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Rapport de sante IA");
        stage.setOnCloseRequest(e -> stopVoicePlayback());
        closeBtn.setOnAction(e -> {
            stopVoicePlayback();
            stage.close();
        });

        HBox actions = new HBox(10, languageCombo, translateBtn, voiceBtn, pauseVoiceBtn, remarkBtn, sendBtn, closeBtn);
        actions.setStyle("-fx-alignment: center-right;");
        VBox root = new VBox(14, title, subtitle, buildReportSummaryCard(recentEtats, recentActivites), reportField, statusLabel, actions);
        root.setStyle("-fx-padding: 22; -fx-background-color: linear-gradient(to bottom right, #f8fafc, #eef6ff);");
        stage.setScene(new Scene(root, 860, 700));
        stage.showAndWait();
    }

    private HBox buildReportSummaryCard(List<Etat> etats, List<Activite> activites) {
        Label etatCount = reportPill("Etats: " + etats.size());
        Label activityCount = reportPill("Activites: " + activites.size());
        Label risk = reportPill("Niveau: " + buildHealthRiskLabel(etats, activites));
        HBox box = new HBox(10, etatCount, activityCount, risk);
        box.setStyle("-fx-padding: 12; -fx-background-color: white; -fx-border-color: #dbeafe; "
                + "-fx-border-radius: 14; -fx-background-radius: 14;");
        return box;
    }

    private Label reportPill(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-padding: 7 12; -fx-background-color: #e0f2fe; -fx-text-fill: #075985; "
                + "-fx-font-weight: 800; -fx-background-radius: 999;");
        return label;
    }

    private Button reportButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(170);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: 800; "
                + "-fx-background-radius: 10; -fx-padding: 10 14; -fx-cursor: hand;");
        return btn;
    }

    private ComboBox<String> reportLanguageCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().setAll("Francais", "Anglais", "Allemand", "Italien");
        combo.setValue("Anglais");
        combo.setPrefWidth(130);
        combo.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10; "
                + "-fx-background-radius: 10; -fx-padding: 4; -fx-font-weight: 700;");
        return combo;
    }

    private String translateReport(String report, String language) {
        if (report == null || report.isBlank() || language == null || "Francais".equalsIgnoreCase(language)) {
            return report;
        }

        Map<String, String> replacements = new LinkedHashMap<>();
        if ("Anglais".equalsIgnoreCase(language)) {
            replacements.put("Bonjour,", "Hello,");
            replacements.put("Voici votre rapport de sante genere automatiquement a partir de vos 6 derniers etats.", "Here is your automatically generated health report based on your last 6 health states.");
            replacements.put("Patient:", "Patient:");
            replacements.put("Periode:", "Period:");
            replacements.put(" au ", " to ");
            replacements.put("Synthese IA:", "AI summary:");
            replacements.put("Etats releves:", "Recorded health states:");
            replacements.put("Aucun etat enregistre sur cette periode.", "No health state recorded during this period.");
            replacements.put("Traitement:", "Treatment:");
            replacements.put("Temp:", "Temperature:");
            replacements.put("Hydratation:", "Hydration:");
            replacements.put("Remarques:", "Clinical notes:");
            replacements.put("Activites recommandees / suivies:", "Recommended / tracked activities:");
            replacements.put("Aucune activite enregistree sur cette periode.", "No activity recorded during this period.");
            replacements.put("Type:", "Type:");
            replacements.put("Duree:", "Duration:");
            replacements.put("Calories:", "Calories:");
            replacements.put("Distance:", "Distance:");
            replacements.put("Repos:", "Rest:");
            replacements.put("Note:", "Note:");
            replacements.put("Conseil professionnel:", "Professional advice:");
            replacements.put("Ce rapport aide au suivi, mais ne remplace pas un avis medical.", "This report supports follow-up, but does not replace medical advice.");
            replacements.put("En cas de fievre elevee, malaise, douleur importante ou aggravation, contactez un professionnel de sante.", "In case of high fever, discomfort, significant pain, or worsening symptoms, contact a healthcare professional.");
            replacements.put("Cordialement,", "Best regards,");
            replacements.put("Temperature tres elevee detectee sur la periode.", "Very high temperature detected during the period.");
            replacements.put("La priorite est le repos, l'hydratation reguliere et un avis medical si les symptomes persistent ou s'aggravent.", "Priority should be rest, regular hydration, and medical advice if symptoms persist or worsen.");
            replacements.put("Etat fragile detecte:", "Fragile condition detected:");
            replacements.put("privilegier une activite tres douce, surveiller la temperature et renforcer l'hydratation avant tout effort.", "prefer very gentle activity, monitor temperature, and increase hydration before any effort.");
            replacements.put("Suivi globalement stable.", "Overall follow-up is stable.");
            replacements.put("Maintenir une activite moderee, une bonne hydratation et une surveillance reguliere des signes cliniques.", "Maintain moderate activity, good hydration, and regular monitoring of clinical signs.");
            replacements.put("h de repos", "h of rest");
            replacements.put("deg C", "C");
        } else if ("Allemand".equalsIgnoreCase(language)) {
            replacements.put("Bonjour,", "Guten Tag,");
            replacements.put("Voici votre rapport de sante genere automatiquement a partir de vos 6 derniers etats.", "Hier ist Ihr automatisch erstellter Gesundheitsbericht basierend auf Ihren letzten 6 Gesundheitszustanden.");
            replacements.put("Patient:", "Patient:");
            replacements.put("Periode:", "Zeitraum:");
            replacements.put(" au ", " bis ");
            replacements.put("Synthese IA:", "KI-Zusammenfassung:");
            replacements.put("Etats releves:", "Erfasste Gesundheitszustande:");
            replacements.put("Aucun etat enregistre sur cette periode.", "In diesem Zeitraum wurde kein Gesundheitszustand erfasst.");
            replacements.put("Traitement:", "Behandlung:");
            replacements.put("Temp:", "Temperatur:");
            replacements.put("Hydratation:", "Flussigkeitszufuhr:");
            replacements.put("Remarques:", "Klinische Hinweise:");
            replacements.put("Activites recommandees / suivies:", "Empfohlene / verfolgte Aktivitaten:");
            replacements.put("Aucune activite enregistree sur cette periode.", "In diesem Zeitraum wurde keine Aktivitat erfasst.");
            replacements.put("Type:", "Typ:");
            replacements.put("Duree:", "Dauer:");
            replacements.put("Calories:", "Kalorien:");
            replacements.put("Distance:", "Distanz:");
            replacements.put("Repos:", "Ruhe:");
            replacements.put("Note:", "Notiz:");
            replacements.put("Conseil professionnel:", "Professioneller Rat:");
            replacements.put("Ce rapport aide au suivi, mais ne remplace pas un avis medical.", "Dieser Bericht unterstutzt die Betreuung, ersetzt aber keinen arztlichen Rat.");
            replacements.put("En cas de fievre elevee, malaise, douleur importante ou aggravation, contactez un professionnel de sante.", "Bei hohem Fieber, Unwohlsein, starken Schmerzen oder Verschlechterung wenden Sie sich an medizinisches Fachpersonal.");
            replacements.put("Cordialement,", "Mit freundlichen Grussen,");
            replacements.put("Temperature tres elevee detectee sur la periode.", "Sehr hohe Temperatur im Zeitraum festgestellt.");
            replacements.put("La priorite est le repos, l'hydratation reguliere et un avis medical si les symptomes persistent ou s'aggravent.", "Prioritat haben Ruhe, regelmassige Flussigkeitszufuhr und arztlicher Rat, wenn Symptome anhalten oder sich verschlimmern.");
            replacements.put("Etat fragile detecte:", "Empfindlicher Zustand festgestellt:");
            replacements.put("privilegier une activite tres douce, surveiller la temperature et renforcer l'hydratation avant tout effort.", "sehr leichte Aktivitat bevorzugen, Temperatur uberwachen und vor jeder Anstrengung mehr trinken.");
            replacements.put("Suivi globalement stable.", "Die Betreuung ist insgesamt stabil.");
            replacements.put("Maintenir une activite moderee, une bonne hydratation et une surveillance reguliere des signes cliniques.", "Moderate Aktivitat, gute Flussigkeitszufuhr und regelmassige Uberwachung klinischer Zeichen beibehalten.");
            replacements.put("h de repos", "Std. Ruhe");
            replacements.put("deg C", "C");
        } else if ("Italien".equalsIgnoreCase(language)) {
            replacements.put("Bonjour,", "Buongiorno,");
            replacements.put("Voici votre rapport de sante genere automatiquement a partir de vos 6 derniers etats.", "Ecco il suo rapporto sanitario generato automaticamente in base ai suoi ultimi 6 stati di salute.");
            replacements.put("Patient:", "Paziente:");
            replacements.put("Periode:", "Periodo:");
            replacements.put(" au ", " al ");
            replacements.put("Synthese IA:", "Sintesi IA:");
            replacements.put("Etats releves:", "Stati di salute registrati:");
            replacements.put("Aucun etat enregistre sur cette periode.", "Nessuno stato di salute registrato in questo periodo.");
            replacements.put("Traitement:", "Trattamento:");
            replacements.put("Temp:", "Temperatura:");
            replacements.put("Hydratation:", "Idratazione:");
            replacements.put("Remarques:", "Note cliniche:");
            replacements.put("Activites recommandees / suivies:", "Attivita raccomandate / seguite:");
            replacements.put("Aucune activite enregistree sur cette periode.", "Nessuna attivita registrata in questo periodo.");
            replacements.put("Type:", "Tipo:");
            replacements.put("Duree:", "Durata:");
            replacements.put("Calories:", "Calorie:");
            replacements.put("Distance:", "Distanza:");
            replacements.put("Repos:", "Riposo:");
            replacements.put("Note:", "Nota:");
            replacements.put("Conseil professionnel:", "Consiglio professionale:");
            replacements.put("Ce rapport aide au suivi, mais ne remplace pas un avis medical.", "Questo rapporto aiuta il monitoraggio, ma non sostituisce un parere medico.");
            replacements.put("En cas de fievre elevee, malaise, douleur importante ou aggravation, contactez un professionnel de sante.", "In caso di febbre alta, malessere, dolore importante o peggioramento, contatti un professionista sanitario.");
            replacements.put("Cordialement,", "Cordiali saluti,");
            replacements.put("Temperature tres elevee detectee sur la periode.", "Temperatura molto alta rilevata nel periodo.");
            replacements.put("La priorite est le repos, l'hydratation reguliere et un avis medical si les symptomes persistent ou s'aggravent.", "La priorita e il riposo, l'idratazione regolare e un parere medico se i sintomi persistono o peggiorano.");
            replacements.put("Etat fragile detecte:", "Condizione fragile rilevata:");
            replacements.put("privilegier une activite tres douce, surveiller la temperature et renforcer l'hydratation avant tout effort.", "preferire un'attivita molto leggera, controllare la temperatura e aumentare l'idratazione prima di ogni sforzo.");
            replacements.put("Suivi globalement stable.", "Monitoraggio globalmente stabile.");
            replacements.put("Maintenir une activite moderee, une bonne hydratation et une surveillance reguliere des signes cliniques.", "Mantenere attivita moderata, buona idratazione e controllo regolare dei segni clinici.");
            replacements.put("h de repos", "h di riposo");
            replacements.put("deg C", "C");
        }

        String translated = report;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            translated = translated.replace(entry.getKey(), entry.getValue());
        }
        return translated;
    }

    private void speakReportWithAiVoice(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Aucun texte a lire.");
        }
        try {
            stopVoicePlayback();
            Process process = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-Command",
                    "$voice = New-Object -ComObject SAPI.SpVoice; "
                            + "$text = [Console]::In.ReadToEnd(); "
                            + "$voice.Speak($text) | Out-Null"
            ).start();
            registerVoiceProcess(process);
            try (OutputStream outputStream = process.getOutputStream()) {
                outputStream.write(text.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Lecture vocale impossible sur ce PC.", ex);
        }
    }

    public static void stopVoicePlayback() {
        Process processToStop;
        synchronized (VOICE_LOCK) {
            processToStop = activeVoiceProcess;
            activeVoiceProcess = null;
        }
        if (processToStop == null || !processToStop.isAlive()) {
            return;
        }
        processToStop.destroy();
        try {
            if (!processToStop.waitFor(400, TimeUnit.MILLISECONDS) && processToStop.isAlive()) {
                processToStop.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (processToStop.isAlive()) {
                processToStop.destroyForcibly();
            }
        }
    }

    private void registerVoiceProcess(Process process) {
        synchronized (VOICE_LOCK) {
            activeVoiceProcess = process;
        }
        Thread cleanupThread = new Thread(() -> {
            try {
                process.waitFor();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                synchronized (VOICE_LOCK) {
                    if (activeVoiceProcess == process) {
                        activeVoiceProcess = null;
                    }
                }
            }
        }, "suivi-voice-cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    private void openReadonlyReportDialog(String titleText, String patientLabel, String report,
                                          LocalDate startDate, LocalDate endDate) {
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label subtitle = new Label("Patient: " + patientLabel + " | Periode: "
                + startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - "
                + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-font-weight: 600;");

        TextArea reportField = new TextArea(report);
        reportField.setEditable(false);
        reportField.setWrapText(true);
        reportField.setPrefRowCount(20);
        reportField.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #0f172a; "
                + "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10; -fx-background-radius: 10;");

        ComboBox<String> languageCombo = reportLanguageCombo();
        Button translateBtn = reportButton("Traduire", "#7c3aed");
        Button voiceBtn = reportButton("Voix IA", "#ea580c");
        Button pauseVoiceBtn = reportButton("Pause voix", "#b45309");
        Button closeBtn = reportButton("Fermer", "#64748b");

        translateBtn.setOnAction(e -> runAction(() -> reportField.setText(translateReport(report, languageCombo.getValue()))));
        voiceBtn.setOnAction(e -> runAction(() -> speakReportWithAiVoice(optionalText(reportField.getText()))));
        pauseVoiceBtn.setOnAction(e -> stopVoicePlayback());

        HBox actions = new HBox(10, languageCombo, translateBtn, voiceBtn, pauseVoiceBtn, closeBtn);
        actions.setStyle("-fx-alignment: center-right;");

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(titleText);
        stage.setOnCloseRequest(e -> stopVoicePlayback());
        closeBtn.setOnAction(e -> {
            stopVoicePlayback();
            stage.close();
        });

        VBox root = new VBox(14, title, subtitle, reportField, actions);
        root.setStyle("-fx-padding: 22; -fx-background-color: linear-gradient(to bottom right, #f8fafc, #eef6ff);");
        stage.setScene(new Scene(root, 820, 650));
        stage.showAndWait();
    }

    private String buildHealthReport(PatientOption patient, List<Etat> etats, List<Activite> activites,
                                     String aiRemark, LocalDate startDate, LocalDate endDate) {
        String patientLabel = patient == null ? "Patient" : patient.displayName() + " - " + patient.email();
        StringBuilder report = new StringBuilder();
        report.append("Bonjour,\n\n");
        report.append("Voici votre rapport de sante genere automatiquement a partir de vos 6 derniers etats.\n\n");
        report.append("Patient: ").append(patientLabel).append("\n");
        report.append("Periode: ").append(startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append(" au ").append(endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");
        report.append("Synthese IA:\n");
        report.append(aiRemark).append("\n\n");

        report.append("Etats releves:\n");
        if (etats.isEmpty()) {
            report.append("- Aucun etat enregistre sur cette periode.\n");
        } else {
            for (Etat etat : etats) {
                report.append("- ").append(formatDate(etat.getDateReleve()))
                        .append(" | Traitement: ").append(valueOrDash(etat.getTraitementEnCours()))
                        .append(" | Temp: ").append(formatTemperature(etat.getTemperatureCorporelle()))
                        .append(" | Hydratation: ").append(formatHydratation(etat.getNiveauHydratation()))
                        .append(" | Remarques: ").append(valueOrDash(etat.getRemarquesCliniques()))
                        .append("\n");
            }
        }

        report.append("\nActivites recommandees / suivies:\n");
        if (activites.isEmpty()) {
            report.append("- Aucune activite enregistree sur cette periode.\n");
        } else {
            for (Activite activite : activites) {
                report.append("- ").append(formatDate(activite.getDateActivite()))
                        .append(" | Type: ").append(valueOrDash(activite.getType()))
                        .append(" | Duree: ").append(formatDuree(activite))
                        .append(" | Calories: ").append(valueOrDash(String.valueOf(activite.getCalories()))).append(" kcal")
                        .append(" | Distance: ").append(formatDistanceOrDash(activite))
                        .append(" | Repos: ").append(formatRepos(activite.getHeuresRepos()))
                        .append(" | Note: ").append(valueOrDash(activite.getNotes()))
                        .append("\n");
            }
        }

        report.append("\nConseil professionnel:\n");
        report.append("Ce rapport aide au suivi, mais ne remplace pas un avis medical. En cas de fievre elevee, malaise, douleur importante ou aggravation, contactez un professionnel de sante.\n\n");
        report.append("Cordialement,\nchronic care");
        return report.toString();
    }

    private String generateAiHealthRemark(PatientOption patient, List<Etat> etats, List<Activite> activites) {
        String patientLabel = patient == null ? "Patient" : patient.displayName() + " - " + patient.email();
        return openAiHealthReportService.generateHealthRemark(patientLabel, etats, activites);
    }

    private String buildHealthRiskLabel(List<Etat> etats, List<Activite> activites) {
        double maxTemperature = etats.stream()
                .mapToDouble(etat -> parseDoubleSafe(etat.getTemperatureCorporelle()))
                .max()
                .orElse(0);
        double minHydratation = etats.stream()
                .mapToDouble(etat -> parseDoubleSafe(etat.getNiveauHydratation()))
                .filter(value -> value > 0)
                .min()
                .orElse(0);
        boolean hasRestOnly = !activites.isEmpty() && activites.stream()
                .allMatch(activite -> "repos".equalsIgnoreCase(optionalText(activite.getType())));

        if (maxTemperature >= 39.0) {
            return "surveillance forte";
        }
        if (maxTemperature >= 38.0 || minHydratation > 0 && minHydratation < 1.5 || hasRestOnly) {
            return "prudence";
        }
        return "stable";
    }

    private void requireMinimumEtatsForReport(List<Etat> etats) {
        if (etats.size() < 6) {
            throw new IllegalStateException(
                    "Rapport IA indisponible pour le moment.\n\n"
                            + "Le patient doit avoir au moins 6 etats enregistres pour generer le rapport IA.\n"
                            + "Etats disponibles: " + etats.size() + "/6.\n"
                            + "Veuillez ajouter les etats manquants, puis regenerer le rapport."
            );
        }
    }

    private String suggestActivityType(Etat etat) {
        double temperature = parseDoubleSafe(etat.getTemperatureCorporelle());
        double hydratation = parseDoubleSafe(etat.getNiveauHydratation());
        String contexte = (optionalText(etat.getTraitementEnCours()) + " " + optionalText(etat.getRemarquesCliniques()))
                .toLowerCase(Locale.ROOT);

        if (contexte.contains("repos") || contexte.contains("arret") || contexte.contains("bless")
                || contexte.contains("fract") || contexte.contains("crise")) {
            return "repos";
        }
        if (temperature >= 39.0 || contexte.contains("fievre") || contexte.contains("temperature trop elevee")) {
            return "repos complet";
        }
        if (temperature >= 38.0 || hydratation > 0 && hydratation < 1.5
                || contexte.contains("fatigue") || contexte.contains("douleur")
                || contexte.contains("vertige") || contexte.contains("faible") || contexte.contains("fiev")) {
            return "marche douce";
        }
        if (temperature >= 37.5 || hydratation > 0 && hydratation < 2.0
                || contexte.contains("stress") || contexte.contains("suivi")
                || contexte.contains("controle") || contexte.contains("tension") || contexte.contains("respir")) {
            return "marche moderee";
        }
        if (contexte.contains("cardio") || contexte.contains("diab")
                || contexte.contains("surpoids") || contexte.contains("reeducation")) {
            return "marche moderee";
        }
        return "marche d'entretien";
    }

    private String buildLogicalActivityMessage(Etat etat) {
        String contexte = (optionalText(etat.getTraitementEnCours()) + " " + optionalText(etat.getRemarquesCliniques()))
                .toLowerCase(Locale.ROOT);
        double temperature = parseDoubleSafe(etat.getTemperatureCorporelle());
        double hydratation = parseDoubleSafe(etat.getNiveauHydratation());

        if (temperature >= 39.0 || contexte.contains("fievre") || contexte.contains("temperature trop elevee")) {
            return "Temperature tres elevee: repos complet, hydratation et avis medical conseilles.";
        }
        if (contexte.contains("repos") || contexte.contains("arret") || contexte.contains("bless")
                || contexte.contains("fract") || contexte.contains("crise")) {
            return "Repos conseille avec surveillance rapprochee.";
        }
        if (temperature >= 38.0 || hydratation > 0 && hydratation < 1.5
                || contexte.contains("fatigue") || contexte.contains("douleur")
                || contexte.contains("vertige") || contexte.contains("faible")) {
            return "Priorite a une marche douce, a l'hydratation et a la recuperation.";
        }
        if (contexte.contains("diab") || contexte.contains("cardio") || contexte.contains("surpoids")) {
            return "Activite reguliere et moderee recommandee pour un suivi progressif et stable.";
        }
        return "Activite d'entretien reguliere recommandee selon l'etat actuel du patient.";
    }

    private Optional<Activite> findGeneratedActivite(Long etatId) {
        if (etatId == null) {
            return Optional.empty();
        }
        return activiteController.listActivites().stream()
                .filter(activite -> etatId.equals(activite.getEtatId()))
                .max(Comparator.comparing(Activite::getId, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private String formatDuree(Activite activite) {
        if (activite == null) {
            return "-";
        }
        if ("repos".equalsIgnoreCase(optionalText(activite.getType()))) {
            return activite.getHeuresRepos() == null ? "Repos conseille" : activite.getHeuresRepos() + " h de repos";
        }
        return activite.getDuree() == null ? "-" : activite.getDuree() + " min";
    }

    private String formatDistanceOrDash(Activite activite) {
        if (activite == null) {
            return "-";
        }
        if ("repos".equalsIgnoreCase(optionalText(activite.getType()))) {
            return "-";
        }
        return formatDistance(activite.getDistanceKm());
    }

    private String formatConseil(Activite activite, Etat etat) {
        String type = activite == null ? "" : optionalText(activite.getType());
        double temperature = etat == null ? 0 : parseDoubleSafe(etat.getTemperatureCorporelle());

        if ("repos".equalsIgnoreCase(type) && temperature >= 39.0) {
            return "Temperature tres elevee. Repos complet, hydratation et avis medical conseilles.";
        }
        if ("repos".equalsIgnoreCase(type)) {
            return "Repos conseille pour favoriser la recuperation et limiter l'effort physique.";
        }

        String notes = activite == null ? "" : optionalText(activite.getNotes());
        if (notes.isBlank()) {
            return buildLogicalActivityMessage(etat);
        }
        return notes;
    }

    private void clearActiviteForm() {
        activiteTable.getSelectionModel().clearSelection();
        if (!activitePatientCombo.isDisabled()) activitePatientCombo.getSelectionModel().clearSelection();
        activiteSelectedEtatId = null;
        activiteEtatLabel.setText("-");
        activiteTypeCombo.getSelectionModel().clearSelection();
        activiteDureeField.clear();
        activiteCaloriesField.clear();
        activiteDistanceField.clear();
        activiteReposField.clear();
        activiteDateField.setValue(LocalDate.now());
        activiteNotesField.clear();
        if (activitePatientCombo.isDisabled()) updateEtatLabelForPatient(activitePatientCombo.getValue());
    }

    private Etat selectedEtat() {
        Etat selected = etatTable.getSelectionModel().getSelectedItem();
        if (selected == null) throw new IllegalStateException("Choisissez un etat.");
        return selected;
    }

    private Activite selectedActivite() {
        Activite selected = activiteTable.getSelectionModel().getSelectedItem();
        if (selected == null) throw new IllegalStateException("Choisissez une activite.");
        return selected;
    }

    private void selectPatient(ComboBox<PatientOption> comboBox, Long userId) {
        if (userId == null) return;
        for (PatientOption patient : comboBox.getItems()) {
            if (patient.id().equals(userId)) {
                comboBox.getSelectionModel().select(patient);
                return;
            }
        }
    }

    private String resolveUserLabel(Long userId) {
        PatientOption patient = patientById.get(userId);
        return patient == null ? "Patient #" + userId : patient.displayName() + " - " + patient.email();
    }

    private String resolvePatientChartLabel(Long userId) {
        PatientOption patient = patientById.get(userId);
        if (patient == null) {
            return "Patient #" + userId;
        }
        String label = patient.displayName();
        if (label == null || label.isBlank() || "-".equals(label)) {
            label = patient.email();
        }
        return label.length() > 18 ? label.substring(0, 18) + "..." : label;
    }

    private String findEtatLabel(Long etatId) {
        if (etatId == null) return "-";
        return etatController.listEtats().stream().filter(e -> etatId.equals(e.getId()))
                .map(e -> valueOrDash(e.getTraitementEnCours())).findFirst().orElse("Inconnu");
    }

    private boolean isPatient() {
        return currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().contains("ROLE_PATIENT");
    }

    private boolean isAdmin() {
        return currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().contains("ROLE_ADMIN");
    }

    private boolean isCoach() {
        return currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().contains("ROLE_COACH");
    }

    private boolean isNutritionniste() {
        return currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().contains("ROLE_NUTRITIONNISTE");
    }

    private boolean canManageEtat() {
        return isPatient();
    }

    private boolean canManageActivite() {
        return isCoach() || isNutritionniste();
    }

    private boolean canReviewGeneratedActivity() {
        return isCoach() || isNutritionniste();
    }

    private boolean canGenerateAiReport() {
        return isCoach() || isNutritionniste();
    }

    private boolean canUseMessenger() {
        return isPatient() || isCoach() || isNutritionniste();
    }

    private void configureEtatTableFiltering() {
        FilteredList<Etat> filteredEtats = new FilteredList<>(etatItems, etat -> true);
        SortedList<Etat> sortedEtats = new SortedList<>(filteredEtats);
        etatTable.setItems(sortedEtats);
        etatTable.getProperties().put("filteredEtats", filteredEtats);
        etatTable.getProperties().put("sortedEtats", sortedEtats);
    }

    private void configureActiviteTableFiltering() {
        FilteredList<Activite> filteredActivites = new FilteredList<>(activiteItems, activite -> true);
        SortedList<Activite> sortedActivites = new SortedList<>(filteredActivites);
        activiteTable.setItems(sortedActivites);
        activiteTable.getProperties().put("filteredActivites", filteredActivites);
        activiteTable.getProperties().put("sortedActivites", sortedActivites);
    }

    @SuppressWarnings("unchecked")
    private void applyEtatFilters() {
        if (etatTable == null) return;
        FilteredList<Etat> filteredEtats = (FilteredList<Etat>) etatTable.getProperties().get("filteredEtats");
        SortedList<Etat> sortedEtats = (SortedList<Etat>) etatTable.getProperties().get("sortedEtats");
        if (filteredEtats == null || sortedEtats == null) return;

        String query = normalizeSearch(etatSearchField == null ? null : etatSearchField.getText());
        filteredEtats.setPredicate(etat -> matchesEtatSearch(etat, query));
        sortedEtats.setComparator(buildEtatComparator(etatSortCombo == null ? null : etatSortCombo.getValue()));
    }

    @SuppressWarnings("unchecked")
    private void applyActiviteFilters() {
        if (activiteTable == null) return;
        FilteredList<Activite> filteredActivites = (FilteredList<Activite>) activiteTable.getProperties().get("filteredActivites");
        SortedList<Activite> sortedActivites = (SortedList<Activite>) activiteTable.getProperties().get("sortedActivites");
        if (filteredActivites == null || sortedActivites == null) return;

        String query = normalizeSearch(activiteSearchField == null ? null : activiteSearchField.getText());
        filteredActivites.setPredicate(activite -> matchesActiviteSearch(activite, query));
        sortedActivites.setComparator(buildActiviteComparator(activiteSortCombo == null ? null : activiteSortCombo.getValue()));
    }

    private void resetEtatFilters() {
        if (etatSearchField != null) etatSearchField.clear();
        if (etatSortCombo != null) etatSortCombo.setValue(ETAT_SORT_RECENT);
        applyEtatFilters();
    }

    private void resetActiviteFilters() {
        if (activiteSearchField != null) activiteSearchField.clear();
        if (activiteSortCombo != null) activiteSortCombo.setValue(ACTIVITE_SORT_RECENT);
        applyActiviteFilters();
    }

    private boolean matchesEtatSearch(Etat etat, String query) {
        if (query.isEmpty()) return true;
        return containsSearch(resolveUserLabel(etat.getUtilisateurId()), query)
                || containsSearch(etat.getTraitementEnCours(), query)
                || containsSearch(etat.getRemarquesCliniques(), query)
                || containsSearch(etat.getTemperatureCorporelle(), query)
                || containsSearch(etat.getNiveauHydratation(), query)
                || containsSearch(formatDate(etat.getDateReleve()), query);
    }

    private boolean matchesActiviteSearch(Activite activite, String query) {
        if (query.isEmpty()) return true;
        return containsSearch(resolveUserLabel(activite.getUtilisateurId()), query)
                || containsSearch(activite.getType(), query)
                || containsSearch(activite.getNotes(), query)
                || containsSearch(findEtatLabel(activite.getEtatId()), query)
                || containsSearch(formatDate(activite.getDateActivite()), query)
                || containsSearch(String.valueOf(activite.getDuree()), query)
                || containsSearch(String.valueOf(activite.getCalories()), query)
                || containsSearch(String.valueOf(activite.getDistanceKm()), query)
                || containsSearch(String.valueOf(activite.getHeuresRepos()), query);
    }

    private Comparator<Etat> buildEtatComparator(String selectedSort) {
        if (ETAT_SORT_OLD.equals(selectedSort)) {
            return Comparator.comparing(Etat::getDateReleve, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Etat::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if (ETAT_SORT_PATIENT.equals(selectedSort)) {
            return Comparator.comparing((Etat etat) -> normalizeSort(resolveUserLabel(etat.getUtilisateurId())))
                    .thenComparing(Etat::getDateReleve, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if (ETAT_SORT_TRAITEMENT.equals(selectedSort)) {
            return Comparator.comparing((Etat etat) -> normalizeSort(etat.getTraitementEnCours()))
                    .thenComparing(Etat::getDateReleve, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparing(Etat::getDateReleve, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Etat::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<Activite> buildActiviteComparator(String selectedSort) {
        if (ACTIVITE_SORT_OLD.equals(selectedSort)) {
            return Comparator.comparing(Activite::getDateActivite, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Activite::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if (ACTIVITE_SORT_PATIENT.equals(selectedSort)) {
            return Comparator.comparing((Activite activite) -> normalizeSort(resolveUserLabel(activite.getUtilisateurId())))
                    .thenComparing(Activite::getDateActivite, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if (ACTIVITE_SORT_TYPE.equals(selectedSort)) {
            return Comparator.comparing((Activite activite) -> normalizeSort(activite.getType()))
                    .thenComparing(Activite::getDateActivite, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparing(Activite::getDateActivite, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Activite::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private String normalizeSort(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private boolean containsSearch(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void installIntegerFilter(TextField field) {
        UnaryOperator<TextFormatter.Change> filter = change -> change.getControlNewText().matches("\\d*") ? change : null;
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private void installDecimalFilter(TextField field) {
        UnaryOperator<TextFormatter.Change> filter = change -> change.getControlNewText().matches("\\d*(\\.\\d{0,2})?") ? change : null;
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private void enforceMaxLength(TextField field, int max) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > max) field.setText(newValue.substring(0, max));
        });
    }

    private void enforceMaxLength(TextArea field, int max) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > max) field.setText(newValue.substring(0, max));
        });
    }

    private Integer parseIntRequired(String input, String label) {
        try {
            if (input == null || input.isBlank()) throw new IllegalArgumentException(label + " obligatoire.");
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " invalide.");
        }
    }

    private Integer parseIntOptional(String input, String label) {
        try {
            if (input == null || input.isBlank()) return null;
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " invalide.");
        }
    }

    private Double parseDoubleOptional(String input, String label) {
        try {
            if (input == null || input.isBlank()) return null;
            return Double.parseDouble(input.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " invalide.");
        }
    }

    private double parseDoubleSafe(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String requiredText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " obligatoire.");
        return value.trim();
    }

    private String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean confirmDelete(String message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Confirmation");
        confirm.setContentText(message);
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
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

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
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

    private String formatRepos(Integer heuresRepos) {
        if (heuresRepos == null) {
            return "-";
        }
        return heuresRepos + " h";
    }

    private String formatDistance(Double valueKm) {
        if (valueKm == null) {
            return "-";
        }
        if (valueKm < 1) {
            long meters = Math.round(valueKm * 1000);
            return meters + " m";
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

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    @FunctionalInterface
    private interface Action { void run(); }

    private record ChatNotification(String senderName, String preview, int unreadCount, LocalDateTime createdAt) {}

    private record ChatContact(Long id, String displayName, String email, String roleLabel, Timestamp lastSeen, int unreadCount) {
        @Override public String toString() { return displayName; }
    }

    private record ChatMessage(Long id, Long senderId, Long receiverId, String messageText, LocalDateTime createdAt) {}

    private record PatientOption(Long id, String displayName, String email) {
        @Override public String toString() { return displayName + " - " + email; }
    }

    private record EtatFormData(Long utilisateurId, String traitementEnCours, String remarquesCliniques,
                                String temperatureCorporelle, String niveauHydratation, LocalDateTime dateReleve) {}

    private record ActiviteFormData(Long utilisateurId, String type, Integer duree, Integer calories,
                                    Double distanceKm, Integer heuresRepos, LocalDateTime dateActivite, String notes) {}
}
