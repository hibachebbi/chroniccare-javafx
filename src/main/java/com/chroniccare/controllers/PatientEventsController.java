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
import javafx.scene.control.Button;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class PatientEventsController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private TextField searchField;
    @FXML private Label kpiTotalEventsLabel;
    @FXML private Label kpiUpcomingEventsLabel;
    @FXML private Label kpiRegisteredEventsLabel;
    @FXML private Button filterAllBtn;
    @FXML private Button filterTodayBtn;
    @FXML private Button filterWeekBtn;
    @FXML private Button filterRegisteredBtn;
    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> titreCol;
    @FXML private TableColumn<Event, String> lieuCol;
    @FXML private TableColumn<Event, String> dateDebutCol;
    @FXML private TableColumn<Event, String> dateFinCol;
    @FXML private TableColumn<Event, String> statutCol;
    @FXML private TableColumn<Event, Void> actionsCol;
    @FXML private Label countLabel;
    @FXML private VBox emptyStateBox;
    @FXML private Label messageLabel;

    private final EventService eventService = new EventService();
    private final ObservableList<Event> allEvents = FXCollections.observableArrayList();
    private final ObservableList<Event> displayedEvents = FXCollections.observableArrayList();
    private final Set<Integer> registeredEventIds = new HashSet<>();
    private User currentUser;
    private String activeQuickFilter = "all";

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        setupHeader();
        setupTable();
        loadEvents();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        setActiveQuickFilter("all");
    }

    private void setupHeader() {
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
        titreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        lieuCol.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        dateDebutCol.setCellValueFactory(new PropertyValueFactory<>("dateDebutDisplay"));
        dateFinCol.setCellValueFactory(new PropertyValueFactory<>("dateFinDisplay"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
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
        eventsTable.setRowFactory(table -> {
            TableRow<Event> row = new TableRow<>() {
                @Override
                protected void updateItem(Event event, boolean empty) {
                    super.updateItem(event, empty);
                    getStyleClass().removeAll("row-status-pending", "row-status-valid", "row-status-cancelled");
                    if (empty || event == null) {
                        return;
                    }
                    getStyleClass().add(getRowClass(event.getStatut()));
                }
            };
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    openEventDetails(row.getItem());
                }
            });
            return row;
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetails = new Button("Voir details");
            private final HBox box = new HBox(btnDetails);
            {
                btnDetails.getStyleClass().add("table-action-primary");
                btnDetails.setOnAction(e -> openEventDetails(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        eventsTable.setItems(displayedEvents);
    }

    private void loadEvents() {
        if (currentUser == null) {
            return;
        }
        try {
            List<Event> events = eventService.getAvailableForPatients();
            allEvents.setAll(events);
            registeredEventIds.clear();
            for (Event event : eventService.getRegisteredEventsByPatientEmail(currentUser.getEmail())) {
                registeredEventIds.add(event.getId());
            }
            refreshKpis();
            applyFilters();
            messageLabel.setText("");
        } catch (Exception e) {
            messageLabel.setText("Erreur chargement evenements : " + e.getMessage());
        }
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

        List<Event> filtered = allEvents.stream()
                .filter(event -> {
                    LocalDate eventDate = event.getDateDebut() != null ? event.getDateDebut().toLocalDate() : null;
                    return switch (activeQuickFilter) {
                        case "today" -> eventDate != null && eventDate.equals(today);
                        case "week" -> eventDate != null && !eventDate.isBefore(weekStart) && !eventDate.isAfter(weekEnd);
                        case "registered" -> registeredEventIds.contains(event.getId());
                        default -> true;
                    };
                })
                .filter(event ->
                        contains(event.getTitre(), query) ||
                        contains(event.getLieu(), query) ||
                        contains(event.getStatut(), query))
                .collect(Collectors.toList());

        displayedEvents.setAll(filtered);
        countLabel.setText(filtered.size() + " evenement(s) affiche(s)");
        boolean isEmpty = filtered.isEmpty();
        emptyStateBox.setVisible(isEmpty);
        emptyStateBox.setManaged(isEmpty);
    }

    private void openEventDetails(Event event) {
        if (event == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chroniccare/patient-event-details.fxml"));
            Parent root = loader.load();
            PatientEventDetailsController controller = loader.getController();
            controller.setEventId(event.getId());
            eventsTable.getScene().setRoot(root);
        } catch (Exception e) {
            messageLabel.setText("Erreur ouverture details : " + e.getMessage());
        }
    }

    @FXML
    public void goToMyRegistrations() {
        navigateTo("/com/chroniccare/patient-registrations.fxml");
    }

    @FXML
    public void goToHome() {
        navigateTo("/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToProfile() {
        navigateTo("/com/chroniccare/profile-patient.fxml");
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
    public void handleFilterRegistered() {
        setActiveQuickFilter("registered");
        applyFilters();
    }

    @FXML
    public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            eventsTable.getScene().setRoot(root);
        } catch (Exception e) {
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            eventsTable.getScene().setRoot(root);
        } catch (Exception e) {
            messageLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }

    private void refreshKpis() {
        LocalDate today = LocalDate.now();
        long upcoming = allEvents.stream()
                .filter(event -> event.getDateDebut() != null)
                .filter(event -> !event.getDateDebut().toLocalDate().isBefore(today))
                .count();
        kpiTotalEventsLabel.setText(String.valueOf(allEvents.size()));
        kpiUpcomingEventsLabel.setText(String.valueOf(upcoming));
        kpiRegisteredEventsLabel.setText(String.valueOf(registeredEventIds.size()));
    }

    private void setActiveQuickFilter(String quickFilter) {
        activeQuickFilter = quickFilter;
        filterAllBtn.getStyleClass().remove("quick-filter-chip-active");
        filterTodayBtn.getStyleClass().remove("quick-filter-chip-active");
        filterWeekBtn.getStyleClass().remove("quick-filter-chip-active");
        filterRegisteredBtn.getStyleClass().remove("quick-filter-chip-active");
        switch (quickFilter) {
            case "today" -> filterTodayBtn.getStyleClass().add("quick-filter-chip-active");
            case "week" -> filterWeekBtn.getStyleClass().add("quick-filter-chip-active");
            case "registered" -> filterRegisteredBtn.getStyleClass().add("quick-filter-chip-active");
            default -> filterAllBtn.getStyleClass().add("quick-filter-chip-active");
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String toStatusLabel(String status) {
        if (status == null) return "Inconnu";
        return switch (status.toLowerCase()) {
            case "en_attente" -> "En attente";
            case "valide" -> "Valide";
            case "annule" -> "Annule";
            default -> status;
        };
    }

    private String getStatusClass(String status) {
        if (status == null) return "status-chip-pending";
        return switch (status.toLowerCase()) {
            case "valide" -> "status-chip-valid";
            case "annule" -> "status-chip-cancelled";
            default -> "status-chip-pending";
        };
    }

    private String getRowClass(String status) {
        if (status == null) return "row-status-pending";
        return switch (status.toLowerCase()) {
            case "valide" -> "row-status-valid";
            case "annule" -> "row-status-cancelled";
            default -> "row-status-pending";
        };
    }

    private String getInitials(User user) {
        String p = user.getPrenom() != null && !user.getPrenom().isEmpty()
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase()
                : "";
        String n = user.getNom() != null && !user.getNom().isEmpty()
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase()
                : "";
        return p + n;
    }
}
