package com.chroniccare.controllers;

import com.chroniccare.models.Exercise;
import com.chroniccare.models.User;
import com.chroniccare.services.ExerciseService;
import com.chroniccare.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CoachExercisesController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private TableView<Exercise> exercisesTable;
    @FXML private TableColumn<Exercise, Integer> idCol;
    @FXML private TableColumn<Exercise, String> nomCol;
    @FXML private TableColumn<Exercise, String> evenementCol;
    @FXML private TableColumn<Exercise, Integer> dureeCol;
    @FXML private TableColumn<Exercise, String> repetitionsCol;
    @FXML private TableColumn<Exercise, Void> actionsCol;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label countLabel;
    @FXML private Label errorLabel;
    @FXML private Label kpiTotalExercisesLabel;
    @FXML private Label kpiAverageDurationLabel;
    @FXML private Label kpiLinkedEventsLabel;
    @FXML private Label kpiTotalRepetitionsLabel;
    @FXML private Button filterAllBtn;
    @FXML private Button filterShortBtn;
    @FXML private Button filterMediumBtn;
    @FXML private Button filterLongBtn;
    @FXML private Button filterNoRepetitionsBtn;
    @FXML private VBox emptyStateBox;

    private final ExerciseService exerciseService = new ExerciseService();
    private final ObservableList<Exercise> allExercises = FXCollections.observableArrayList();
    private final ObservableList<Exercise> displayedExercises = FXCollections.observableArrayList();
    private User currentUser;
    private String activeQuickFilter = "all";

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        populateHeader();
        setupTable();
        setupSorting();
        setupLiveSearch();
        setActiveQuickFilter("all");
        loadExercises();
    }

    private void populateHeader() {
        if (currentUser == null) {
            return;
        }

        String initials = getInitials(currentUser);
        sidebarAvatar.setText(initials);
        sidebarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        topbarAvatar.setText(initials);
        topbarUserName.setText(currentUser.getPrenom() + " " + currentUser.getNom());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        topbarDate.setText(LocalDate.now().format(fmt));
    }

    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        evenementCol.setCellValueFactory(new PropertyValueFactory<>("evenementTitre"));
        dureeCol.setCellValueFactory(new PropertyValueFactory<>("duree"));
        repetitionsCol.setCellValueFactory(new PropertyValueFactory<>("repetitionsDisplay"));

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox box = new HBox(6, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("table-action-primary");
                btnDelete.getStyleClass().add("table-action-danger");

                btnEdit.setOnAction(e -> goToEdit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupLiveSearch() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void setupSorting() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Plus recents",
                "Nom A-Z",
                "Nom Z-A",
                "Duree croissante",
                "Duree decroissante",
                "Evenement A-Z"
        ));
        sortCombo.setValue("Plus recents");
        sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void loadExercises() {
        if (currentUser == null) {
            return;
        }

        try {
            List<Exercise> exercises = exerciseService.getByCoachId(currentUser.getId());
            allExercises.setAll(exercises);
            refreshKpis();
            applyFilters();
            errorLabel.setText("");
        } catch (Exception e) {
            errorLabel.setText("Erreur chargement exercices : " + e.getMessage());
        }
    }

    @FXML
    public void handleSearch() {
        applyFilters();
    }

    @FXML
    public void handleFilterAll() {
        setActiveQuickFilter("all");
        applyFilters();
    }

    @FXML
    public void handleFilterShort() {
        setActiveQuickFilter("short");
        applyFilters();
    }

    @FXML
    public void handleFilterMedium() {
        setActiveQuickFilter("medium");
        applyFilters();
    }

    @FXML
    public void handleFilterLong() {
        setActiveQuickFilter("long");
        applyFilters();
    }

    @FXML
    public void handleFilterNoRepetitions() {
        setActiveQuickFilter("noRepetitions");
        applyFilters();
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        sortCombo.setValue("Plus recents");
        setActiveQuickFilter("all");
        applyFilters();
    }

    @FXML
    public void goToAdd() {
        navigate("/com/chroniccare/add-exercise.fxml");
    }

    @FXML
    public void goToEvents() {
        navigate("/com/chroniccare/coach-events.fxml");
    }

    @FXML
    public void goToProfile() {
        navigate("/com/chroniccare/profile-coach.fxml");
    }

    @FXML
    public void goToHome() {
        navigate("/com/chroniccare/home.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        navigate("/com/chroniccare/login.fxml");
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        List<Exercise> result = allExercises.stream()
                .filter(exercise -> keyword.isEmpty()
                        || contains(exercise.getNom(), keyword)
                        || contains(exercise.getDescription(), keyword)
                        || contains(exercise.getEvenementTitre(), keyword))
                .filter(this::matchesQuickFilter)
                .sorted(getComparator(sortCombo.getValue()))
                .collect(Collectors.toList());

        displayedExercises.setAll(result);
        exercisesTable.setItems(displayedExercises);
        toggleEmptyState(result.isEmpty());
        countLabel.setText(result.size() + " exercice(s)");
    }

    private void refreshKpis() {
        int totalExercises = allExercises.size();
        double avgDuration = allExercises.stream().mapToInt(Exercise::getDuree).average().orElse(0.0);
        int linkedEvents = (int) allExercises.stream()
                .map(Exercise::getEvenementId)
                .filter(id -> id != null)
                .collect(Collectors.toSet())
                .size();
        int totalRepetitions = allExercises.stream()
                .map(Exercise::getRepetitions)
                .filter(rep -> rep != null)
                .mapToInt(Integer::intValue)
                .sum();

        kpiTotalExercisesLabel.setText(String.valueOf(totalExercises));
        kpiAverageDurationLabel.setText(String.format(Locale.FRENCH, "%.1f", avgDuration));
        kpiLinkedEventsLabel.setText(String.valueOf(linkedEvents));
        kpiTotalRepetitionsLabel.setText(String.valueOf(totalRepetitions));
    }

    private Comparator<Exercise> getComparator(String sortValue) {
        if ("Nom A-Z".equals(sortValue)) {
            return Comparator.comparing(exercise -> safeLower(exercise.getNom()));
        }
        if ("Nom Z-A".equals(sortValue)) {
            return Comparator.comparing((Exercise exercise) -> safeLower(exercise.getNom())).reversed();
        }
        if ("Duree croissante".equals(sortValue)) {
            return Comparator.comparingInt(Exercise::getDuree);
        }
        if ("Duree decroissante".equals(sortValue)) {
            return Comparator.comparingInt(Exercise::getDuree).reversed();
        }
        if ("Evenement A-Z".equals(sortValue)) {
            return Comparator.comparing(exercise -> safeLower(exercise.getEvenementTitre()));
        }
        return Comparator.comparingInt(Exercise::getId).reversed();
    }

    private boolean matchesQuickFilter(Exercise exercise) {
        if ("short".equals(activeQuickFilter)) {
            return exercise.getDuree() <= 30;
        }
        if ("medium".equals(activeQuickFilter)) {
            return exercise.getDuree() > 30 && exercise.getDuree() <= 60;
        }
        if ("long".equals(activeQuickFilter)) {
            return exercise.getDuree() > 60;
        }
        if ("noRepetitions".equals(activeQuickFilter)) {
            return exercise.getRepetitions() == null || exercise.getRepetitions() <= 0;
        }
        return true;
    }

    private void toggleEmptyState(boolean isEmpty) {
        emptyStateBox.setVisible(isEmpty);
        emptyStateBox.setManaged(isEmpty);
        exercisesTable.setVisible(!isEmpty);
        exercisesTable.setManaged(!isEmpty);
    }

    private void setActiveQuickFilter(String filter) {
        activeQuickFilter = filter;
        filterAllBtn.getStyleClass().remove("quick-filter-chip-active");
        filterShortBtn.getStyleClass().remove("quick-filter-chip-active");
        filterMediumBtn.getStyleClass().remove("quick-filter-chip-active");
        filterLongBtn.getStyleClass().remove("quick-filter-chip-active");
        filterNoRepetitionsBtn.getStyleClass().remove("quick-filter-chip-active");

        if ("short".equals(filter)) {
            filterShortBtn.getStyleClass().add("quick-filter-chip-active");
        } else if ("medium".equals(filter)) {
            filterMediumBtn.getStyleClass().add("quick-filter-chip-active");
        } else if ("long".equals(filter)) {
            filterLongBtn.getStyleClass().add("quick-filter-chip-active");
        } else if ("noRepetitions".equals(filter)) {
            filterNoRepetitionsBtn.getStyleClass().add("quick-filter-chip-active");
        } else {
            filterAllBtn.getStyleClass().add("quick-filter-chip-active");
        }
    }

    private void handleDelete(Exercise exercise) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer l'exercice \"" + exercise.getNom() + "\" ?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    exerciseService.delete(exercise.getId(), currentUser.getId());
                    loadExercises();
                } catch (Exception e) {
                    errorLabel.setText("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    private void goToEdit(Exercise exercise) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chroniccare/edit-exercise.fxml"));
            Parent root = loader.load();
            EditExerciseController controller = loader.getController();
            controller.setExercise(exercise);
            exercisesTable.getScene().setRoot(root);
        } catch (Exception e) {
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }

    private void navigate(String fxmlPath) {
        try {
            releaseViewMemory();
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            exercisesTable.getScene().setRoot(root);
        } catch (Exception e) {
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String getInitials(User user) {
        String prenom = user.getPrenom() != null && !user.getPrenom().isEmpty()
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String nom = user.getNom() != null && !user.getNom().isEmpty()
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return prenom + nom;
    }

    private void releaseViewMemory() {
        exercisesTable.setItems(FXCollections.emptyObservableList());
        displayedExercises.clear();
        allExercises.clear();
    }
}
