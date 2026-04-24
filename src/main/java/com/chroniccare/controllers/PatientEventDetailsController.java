package com.chroniccare.controllers;

import com.chroniccare.models.Event;
import com.chroniccare.models.Exercise;
import com.chroniccare.models.User;
import com.chroniccare.models.Weather;
import com.chroniccare.services.EventService;
import com.chroniccare.services.ExerciseService;
import com.chroniccare.services.HolidayService;
import com.chroniccare.services.LocationService;
import com.chroniccare.services.WeatherService;
import com.chroniccare.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

public class PatientEventDetailsController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private Label eventTitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label startLabel;
    @FXML private Label endLabel;
    @FXML private Label locationLabel;
    @FXML private Label weatherLabel;
    @FXML private Label holidayLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label mapStatusLabel;
    @FXML private WebView locationMapView;
    @FXML private TableView<Exercise> exercisesTable;
    @FXML private TableColumn<Exercise, String> exNomCol;
    @FXML private TableColumn<Exercise, Integer> exDureeCol;
    @FXML private TableColumn<Exercise, String> exRepCol;
    @FXML private Button registerButton;
    @FXML private Button cancelRegistrationButton;
    @FXML private Label messageLabel;

    private final EventService eventService = new EventService();
    private final ExerciseService exerciseService = new ExerciseService();
    private final ObservableList<Exercise> exercises = FXCollections.observableArrayList();
    private User currentUser;
    private int eventId;
    private volatile int mapRequestSeq = 0;
    private Event currentEvent;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        setupHeader();
        setupTable();
        setupMapView();
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
        loadEventDetails();
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
        exNomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        exDureeCol.setCellValueFactory(new PropertyValueFactory<>("duree"));
        exRepCol.setCellValueFactory(new PropertyValueFactory<>("repetitionsDisplay"));
        exercisesTable.setItems(exercises);
    }

    private void setupMapView() {
        if (locationMapView == null) {
            return;
        }
        locationMapView.setContextMenuEnabled(false);
        locationMapView.setMinHeight(220);
        locationMapView.setPrefHeight(220);
        locationMapView.getEngine().setUserAgent("chroniccare-javafx/1.0");
        locationMapView.getEngine().loadContent(buildMapPlaceholderHtml("Chargement de la carte..."));
    }

    private void loadEventDetails() {
        try {
            Event event = eventService.getById(eventId);
            currentEvent = event;
            if (event == null) {
                messageLabel.setText("Evenement introuvable.");
                registerButton.setDisable(true);
                if (cancelRegistrationButton != null) {
                    cancelRegistrationButton.setDisable(true);
                }
                return;
            }

            eventTitleLabel.setText(event.getTitre());
            statusLabel.setText(event.getStatut() != null ? event.getStatut() : "Non precise");
            startLabel.setText(event.getDateDebutDisplay());
            endLabel.setText(event.getDateFinDisplay());
            locationLabel.setText(event.getLieu() != null ? event.getLieu() : "-");
            descriptionLabel.setText(event.getDescription() != null && !event.getDescription().isBlank()
                    ? event.getDescription()
                    : "Aucune description.");

            loadLocationMap(event.getLieu());
            loadWeather(event.getLieu());
            loadHoliday(event.getDateDebut() != null ? event.getDateDebut().toLocalDate() : null);

            exercises.setAll(exerciseService.getByEventIdPublic(eventId));
            boolean alreadyRegistered = eventService.isPatientRegistered(eventId, currentUser.getEmail());
            updateRegistrationActions(alreadyRegistered, event);
        } catch (Exception e) {
            messageLabel.setText("Erreur chargement details : " + e.getMessage());
        }
    }

    private void loadLocationMap(String location) {
        if (locationMapView == null || mapStatusLabel == null) {
            return;
        }
        if (location == null || location.isBlank()) {
            mapStatusLabel.setText("Carte indisponible: lieu non specifie.");
            locationMapView.getEngine().loadContent(buildMapPlaceholderHtml("Lieu non specifie"));
            return;
        }

        final int requestId = ++mapRequestSeq;
        mapStatusLabel.setText("Chargement de la carte...");
        locationMapView.getEngine().loadContent(buildMapPlaceholderHtml("Recherche de l'emplacement..."));

        Thread mapThread = new Thread(() -> {
            Optional<LocationService.LocationSuggestion> suggestion =
                    LocationService.findBestMatchInTunisia(location);

            Platform.runLater(() -> {
                if (requestId != mapRequestSeq) {
                    return;
                }

                if (suggestion.isPresent() && suggestion.get().hasCoordinates()) {
                    LocationService.LocationSuggestion match = suggestion.get();
                    locationMapView.getEngine().loadContent(
                            buildStaticMapHtml(match.latitude(), match.longitude(), match.displayName()));
                    String label = match.displayName() != null && !match.displayName().isBlank()
                            ? match.displayName()
                            : location;
                    mapStatusLabel.setText("Carte: " + label);
                } else {
                    mapStatusLabel.setText("Carte indisponible pour " + location + ".");
                    locationMapView.getEngine().loadContent(
                            buildMapPlaceholderHtml("Emplacement non trouve dans Nominatim"));
                }
            });
        });
        mapThread.setDaemon(true);
        mapThread.start();
    }

    private String buildStaticMapHtml(double latitude, double longitude, String label) {
        final int zoom = 14;
        final int tileSize = 256;

        double x = lonToTile(longitude, zoom);
        double y = latToTile(latitude, zoom);
        int baseTileX = (int) Math.floor(x);
        int baseTileY = (int) Math.floor(y);
        double pixelOffsetX = (x - baseTileX) * tileSize;
        double pixelOffsetY = (y - baseTileY) * tileSize;
        String safeLabel = escapeHtml(label == null || label.isBlank() ? "Lieu de l'evenement" : label);

        StringBuilder tiles = new StringBuilder();
        for (int row = -1; row <= 1; row++) {
            for (int col = -1; col <= 1; col++) {
                int tileX = baseTileX + col;
                int tileY = baseTileY + row;
                double left = (col + 1) * tileSize;
                double top = (row + 1) * tileSize;
                tiles.append(String.format(
                        Locale.US,
                        "<img src=\"https://tile.openstreetmap.org/%d/%d/%d.png\" " +
                                "style=\"position:absolute;left:%.0fpx;top:%.0fpx;width:256px;height:256px;\" alt=\"tile\"/>",
                        zoom,
                        tileX,
                        tileY,
                        left,
                        top
                ));
            }
        }

        double markerLeft = tileSize + pixelOffsetX;
        double markerTop = tileSize + pixelOffsetY;

        return String.format(
                Locale.US,
                """
                <html>
                  <body style="margin:0;background:#f8fafc;font-family:Arial,sans-serif;overflow:hidden;">
                    <div style="position:relative;width:100vw;height:220px;overflow:hidden;background:#e2e8f0;">
                      <div style="position:absolute;width:768px;height:768px;left:calc(50vw - %.2fpx);top:calc(110px - %.2fpx);">
                        %s
                        <div style="position:absolute;left:%.2fpx;top:%.2fpx;transform:translate(-50%%,-100%%);">
                          <div style="width:18px;height:18px;background:#dc2626;border:3px solid white;border-radius:50%%;
                                      box-shadow:0 4px 12px rgba(15,23,42,0.35);"></div>
                          <div style="width:2px;height:14px;background:#991b1b;margin:-1px auto 0 auto;"></div>
                        </div>
                      </div>
                      <div style="position:absolute;left:12px;top:12px;background:rgba(255,255,255,0.96);padding:8px 10px;
                                  border-radius:10px;border:1px solid #dbe4f0;font-size:12px;color:#0f172a;max-width:calc(100vw - 24px);">
                        %s
                      </div>
                      <div style="position:absolute;right:10px;bottom:8px;background:rgba(255,255,255,0.92);padding:4px 6px;
                                  border-radius:8px;font-size:10px;color:#334155;">
                        © OpenStreetMap contributors
                      </div>
                    </div>
                  </body>
                </html>
                """,
                markerLeft,
                markerTop,
                tiles.toString(),
                markerLeft,
                markerTop,
                safeLabel
        );
    }

    private double lonToTile(double longitude, int zoom) {
        return (longitude + 180.0) / 360.0 * (1 << zoom);
    }

    private double latToTile(double latitude, int zoom) {
        double latRad = Math.toRadians(latitude);
        return (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << zoom);
    }

    private String buildMapPlaceholderHtml(String message) {
        String safeMessage = escapeHtml(message);
        return """
                <html>
                  <body style="margin:0;display:flex;align-items:center;justify-content:center;background:#f8fafc;color:#475569;font-family:Arial,sans-serif;">
                    <div style="padding:24px;text-align:center;border:1px solid #dbe4f0;border-radius:12px;background:#ffffff;">
                      %s
                    </div>
                  </body>
                </html>
                """.formatted(safeMessage);
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void loadWeather(String location) {
        if (location == null || location.isBlank()) {
            weatherLabel.setText("Lieu non specifie");
            return;
        }
        weatherLabel.setText("Chargement meteo...");

        Thread weatherThread = new Thread(() -> {
            Optional<Weather> weather = WeatherService.getWeatherByCity(location);
            Platform.runLater(() -> {
                if (weather.isPresent()) {
                    Weather w = weather.get();
                    weatherLabel.setText(w.getWeatherDisplay());
                } else {
                    weatherLabel.setText("Meteo indisponible pour " + location);
                }
            });
        });
        weatherThread.setDaemon(true);
        weatherThread.start();
    }

    private void loadHoliday(LocalDate eventDate) {
        if (holidayLabel == null) {
            return;
        }
        if (eventDate == null) {
            holidayLabel.setText("Date non specifiee");
            return;
        }

        Optional<String> holidayName = HolidayService.getHolidayName(eventDate);
        if (holidayName.isPresent()) {
            holidayLabel.setText(holidayName.get());
        } else {
            holidayLabel.setText("Pas de jour ferie officiel");
        }
    }

    @FXML
    public void handleRegister() {
        try {
            boolean created = eventService.registerPatientToEvent(eventId, currentUser);
            if (created) {
                if (currentEvent != null) {
                    updateRegistrationActions(true, currentEvent);
                }
                messageLabel.setText("Inscription enregistree avec succes.");
            } else {
                if (currentEvent != null) {
                    updateRegistrationActions(true, currentEvent);
                }
                messageLabel.setText("Vous etes deja inscrit a cet evenement.");
            }
        } catch (Exception e) {
            messageLabel.setText("Erreur inscription : " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelRegistration() {
        try {
            boolean removed = eventService.cancelPatientRegistration(eventId, currentUser.getEmail());
            if (removed) {
                if (currentEvent != null) {
                    updateRegistrationActions(false, currentEvent);
                }
                messageLabel.setText("Inscription annulee avec succes.");
            } else {
                messageLabel.setText("Aucune inscription active a annuler.");
            }
        } catch (Exception e) {
            messageLabel.setText("Erreur annulation : " + e.getMessage());
        }
    }

    @FXML
    public void goToEvents() {
        navigateTo("/com/chroniccare/patient-events.fxml");
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
    public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            exercisesTable.getScene().setRoot(root);
        } catch (Exception e) {
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            exercisesTable.getScene().setRoot(root);
        } catch (Exception e) {
            messageLabel.setText("Erreur navigation : " + e.getMessage());
        }
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

    private void updateRegistrationActions(boolean alreadyRegistered, Event event) {
        registerButton.setDisable(alreadyRegistered);
        if (cancelRegistrationButton != null) {
            boolean canCancel = alreadyRegistered && eventService.canCancelRegistration(event);
            cancelRegistrationButton.setDisable(!canCancel);
        }

        if (!alreadyRegistered) {
            messageLabel.setText("Inscrivez-vous pour reserver votre place.");
            return;
        }
        if (eventService.canCancelRegistration(event)) {
            messageLabel.setText("Vous etes inscrit. Annulation possible jusqu'a 24h avant le debut.");
        } else {
            messageLabel.setText("Vous etes inscrit. Annulation fermee moins de 24h avant le debut.");
        }
    }
}
