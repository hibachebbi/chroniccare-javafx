package com.chroniccare.controllers;

import com.chroniccare.models.Event;
import com.chroniccare.models.User;
import com.chroniccare.services.EventService;
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
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CoachEventsController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, Integer> idCol;
    @FXML private TableColumn<Event, String> titreCol;
    @FXML private TableColumn<Event, String> statutCol;
    @FXML private TableColumn<Event, String> lieuCol;
    @FXML private TableColumn<Event, String> dateDebutCol;
    @FXML private TableColumn<Event, String> dateFinCol;
    @FXML private TableColumn<Event, Void> actionsCol;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label countLabel;
    @FXML private Label errorLabel;
    @FXML private Label kpiTotalEventsLabel;
    @FXML private Label kpiWeekEventsLabel;
    @FXML private Label kpiWithParticipantsLabel;
    @FXML private Label kpiTotalParticipantsLabel;
    @FXML private Button filterAllBtn;
    @FXML private Button filterTodayBtn;
    @FXML private Button filterWeekBtn;
    @FXML private Button filterWithParticipantsBtn;
    @FXML private Button filterWithoutParticipantsBtn;
    @FXML private VBox emptyStateBox;

    private final EventService eventService = new EventService();
    private final ObservableList<Event> allEvents = FXCollections.observableArrayList();
    private final ObservableList<Event> displayedEvents = FXCollections.observableArrayList();
    private final Map<Integer, Integer> registrationCountByEventId = new HashMap<>();
    private User currentUser;
    private String activeQuickFilter = "all";

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        setupSidebar();
        setupTable();
        setupSorting();
        setupLiveSearch();
        setActiveQuickFilter("all");
        loadEvents();
    }

    private void setupSidebar() {
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
        titreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        lieuCol.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        dateDebutCol.setCellValueFactory(new PropertyValueFactory<>("dateDebutDisplay"));
        dateFinCol.setCellValueFactory(new PropertyValueFactory<>("dateFinDisplay"));
        statutCol.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }

                badge.setText(toStatusLabel(status));
                badge.getStyleClass().setAll("status-chip", getStatusClass(status));
                setGraphic(badge);
            }
        });
        eventsTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(Event event, boolean empty) {
                super.updateItem(event, empty);
                getStyleClass().removeAll("row-status-pending", "row-status-valid", "row-status-cancelled");
                if (empty || event == null) {
                    return;
                }
                getStyleClass().add(getRowClass(event.getStatut()));
            }
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final Button btnParticipants = new Button("Inscrits");
            private final HBox box = new HBox(6, btnEdit, btnDelete, btnParticipants);

            {
                btnEdit.getStyleClass().add("table-action-primary");
                btnDelete.getStyleClass().add("table-action-danger");
                btnParticipants.getStyleClass().add("table-action-secondary");

                btnEdit.setOnAction(e -> goToEdit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
                btnParticipants.setOnAction(e -> goToParticipants(getTableView().getItems().get(getIndex())));
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
                "Titre A-Z",
                "Titre Z-A",
                "Date debut croissante",
                "Date debut decroissante",
                "Statut A-Z"
        ));
        sortCombo.setValue("Plus recents");
        sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void loadEvents() {
        if (currentUser == null) {
            return;
        }

        try {
            List<Event> events = eventService.getByCoachId(currentUser.getId());
            allEvents.setAll(events);
            registrationCountByEventId.clear();
            registrationCountByEventId.putAll(eventService.getRegistrationCountByEventForCoach(currentUser.getId()));
            refreshKpis();
            applyFilters();
            errorLabel.setText("");
        } catch (Exception e) {
            errorLabel.setText("Erreur chargement evenements : " + e.getMessage());
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
    public void handleFilterToday() {
        setActiveQuickFilter("today");
        applyFilters();
    }

    @FXML
    public void handleFilterWeek() {
        setActiveQuickFilter("week");
        applyFilters();
    }

    @FXML
    public void handleFilterWithParticipants() {
        setActiveQuickFilter("withParticipants");
        applyFilters();
    }

    @FXML
    public void handleFilterWithoutParticipants() {
        setActiveQuickFilter("withoutParticipants");
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
        navigate("/com/chroniccare/add-event.fxml");
    }

    @FXML
    public void goToExercises() {
        navigate("/com/chroniccare/coach-exercises.fxml");
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

        List<Event> result = allEvents.stream()
                .filter(event -> keyword.isEmpty()
                        || contains(event.getTitre(), keyword)
                        || contains(event.getLieu(), keyword)
                        || contains(event.getStatut(), keyword)
                         || contains(event.getDescription(), keyword))
                .filter(this::matchesQuickFilter)
                .sorted(getComparator(sortCombo.getValue()))
                .collect(Collectors.toList());

        displayedEvents.setAll(result);
        eventsTable.setItems(displayedEvents);
        toggleEmptyState(result.isEmpty());
        countLabel.setText(result.size() + " evenement(s)");
    }

    private void refreshKpis() {
        int totalEvents = allEvents.size();
        long eventsThisWeek = allEvents.stream().filter(this::isInCurrentWeek).count();
        long withParticipants = allEvents.stream().filter(event -> getParticipantsCount(event) > 0).count();
        int totalParticipants = allEvents.stream().mapToInt(this::getParticipantsCount).sum();

        kpiTotalEventsLabel.setText(String.valueOf(totalEvents));
        kpiWeekEventsLabel.setText(String.valueOf(eventsThisWeek));
        kpiWithParticipantsLabel.setText(String.valueOf(withParticipants));
        kpiTotalParticipantsLabel.setText(String.valueOf(totalParticipants));
    }

    private Comparator<Event> getComparator(String sortValue) {
        if ("Titre A-Z".equals(sortValue)) {
            return Comparator.comparing(event -> safeLower(event.getTitre()));
        }
        if ("Titre Z-A".equals(sortValue)) {
            return Comparator.comparing((Event event) -> safeLower(event.getTitre())).reversed();
        }
        if ("Date debut croissante".equals(sortValue)) {
            return Comparator.comparing(Event::getDateDebut, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("Date debut decroissante".equals(sortValue)) {
            return Comparator.comparing(Event::getDateDebut, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("Statut A-Z".equals(sortValue)) {
            return Comparator.comparing(event -> safeLower(event.getStatut()));
        }
        return Comparator.comparing(Event::getDateDebut, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private boolean matchesQuickFilter(Event event) {
        if ("today".equals(activeQuickFilter)) {
            return isToday(event);
        }
        if ("week".equals(activeQuickFilter)) {
            return isInCurrentWeek(event);
        }
        if ("withParticipants".equals(activeQuickFilter)) {
            return getParticipantsCount(event) > 0;
        }
        if ("withoutParticipants".equals(activeQuickFilter)) {
            return getParticipantsCount(event) == 0;
        }
        return true;
    }

    private boolean isToday(Event event) {
        return event.getDateDebut() != null && event.getDateDebut().toLocalDate().isEqual(LocalDate.now());
    }

    private boolean isInCurrentWeek(Event event) {
        if (event.getDateDebut() == null) {
            return false;
        }
        LocalDate now = LocalDate.now();
        LocalDate start = now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate end = now.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        LocalDate eventDate = event.getDateDebut().toLocalDate();
        return !eventDate.isBefore(start) && !eventDate.isAfter(end);
    }

    private int getParticipantsCount(Event event) {
        return registrationCountByEventId.getOrDefault(event.getId(), 0);
    }

    private void toggleEmptyState(boolean isEmpty) {
        emptyStateBox.setVisible(isEmpty);
        emptyStateBox.setManaged(isEmpty);
        eventsTable.setVisible(!isEmpty);
        eventsTable.setManaged(!isEmpty);
    }

    private String toStatusLabel(String status) {
        String normalized = safeLower(status);
        if ("valide".equals(normalized)) {
            return "Valide";
        }
        if ("annule".equals(normalized)) {
            return "Annule";
        }
        return "En attente";
    }

    private String getStatusClass(String status) {
        String normalized = safeLower(status);
        if ("valide".equals(normalized)) {
            return "status-chip-valid";
        }
        if ("annule".equals(normalized)) {
            return "status-chip-cancelled";
        }
        return "status-chip-pending";
    }

    private String getRowClass(String status) {
        String normalized = safeLower(status);
        if ("valide".equals(normalized)) {
            return "row-status-valid";
        }
        if ("annule".equals(normalized)) {
            return "row-status-cancelled";
        }
        return "row-status-pending";
    }

    private void setActiveQuickFilter(String filter) {
        activeQuickFilter = filter;
        filterAllBtn.getStyleClass().remove("quick-filter-chip-active");
        filterTodayBtn.getStyleClass().remove("quick-filter-chip-active");
        filterWeekBtn.getStyleClass().remove("quick-filter-chip-active");
        filterWithParticipantsBtn.getStyleClass().remove("quick-filter-chip-active");
        filterWithoutParticipantsBtn.getStyleClass().remove("quick-filter-chip-active");

        if ("today".equals(filter)) {
            filterTodayBtn.getStyleClass().add("quick-filter-chip-active");
        } else if ("week".equals(filter)) {
            filterWeekBtn.getStyleClass().add("quick-filter-chip-active");
        } else if ("withParticipants".equals(filter)) {
            filterWithParticipantsBtn.getStyleClass().add("quick-filter-chip-active");
        } else if ("withoutParticipants".equals(filter)) {
            filterWithoutParticipantsBtn.getStyleClass().add("quick-filter-chip-active");
        } else {
            filterAllBtn.getStyleClass().add("quick-filter-chip-active");
        }
    }

    private void handleDelete(Event event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer l'evenement \"" + event.getTitre() + "\" et ses exercices associes ?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    eventService.delete(event.getId(), currentUser.getId());
                    loadEvents();
                } catch (Exception e) {
                    errorLabel.setText("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    private void goToEdit(Event event) {
        try {
            releaseViewMemory();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chroniccare/edit-event.fxml"));
            Parent root = loader.load();
            EditEventController controller = loader.getController();
            controller.setEvent(event);
            eventsTable.getScene().setRoot(root);
        } catch (Exception e) {
            errorLabel.setText("Erreur navigation : " + formatExceptionMessage(e));
        }
    }

    private void goToParticipants(Event event) {
        try {
            releaseViewMemory();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chroniccare/event-participants.fxml"));
            Parent root = loader.load();
            EventParticipantsController controller = loader.getController();
            controller.setEvent(event);
            eventsTable.getScene().setRoot(root);
        } catch (Exception e) {
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }

    private void navigate(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            eventsTable.getScene().setRoot(root);
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
        eventsTable.setItems(FXCollections.emptyObservableList());
        displayedEvents.clear();
        allEvents.clear();
        registrationCountByEventId.clear();
    }

    private String formatExceptionMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return root.toString();
        }
        return message;
    }
}
