package com.chroniccare.controllers;

import com.chroniccare.models.PatientSegmentationRow;
import com.chroniccare.models.ProfileGroupRow;
import com.chroniccare.models.User;
import com.chroniccare.services.PatientSegmentationService;
import com.chroniccare.utils.SessionManager;
import com.chroniccare.utils.SidebarNavHighlight;
import com.chroniccare.utils.SidebarRoleBadgeHelper;
import com.chroniccare.utils.FxNavigation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PatientSegmentationController {

    @FXML private VBox sidebarRoot;
    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarRoleBadge;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;

    @FXML private TableView<ProfileGroupRow> profilesTable;
    @FXML private TableColumn<ProfileGroupRow, String> groupCol;
    @FXML private TableColumn<ProfileGroupRow, Integer> countCol;
    @FXML private TableColumn<ProfileGroupRow, String> sampleCol;

    @FXML private TableView<PatientSegmentationRow> riskTable;
    @FXML private TableColumn<PatientSegmentationRow, String> patientRiskCol;
    @FXML private TableColumn<PatientSegmentationRow, String> conditionRiskCol;
    @FXML private TableColumn<PatientSegmentationRow, Integer> secondaryRiskCol;
    @FXML private TableColumn<PatientSegmentationRow, String> riskLevelCol;
    @FXML private TableColumn<PatientSegmentationRow, Integer> riskScoreCol;
    @FXML private TableColumn<PatientSegmentationRow, String> riskDetailsCol;
    @FXML private Label riskLowBadge;
    @FXML private Label riskMediumBadge;
    @FXML private Label riskHighBadge;

    @FXML private TableView<PatientSegmentationRow> behaviorTable;
    @FXML private TableColumn<PatientSegmentationRow, String> patientBehaviorCol;
    @FXML private TableColumn<PatientSegmentationRow, String> segmentCol;
    @FXML private TableColumn<PatientSegmentationRow, Integer> viewsCol;
    @FXML private TableColumn<PatientSegmentationRow, Integer> updatesCol;
    @FXML private TableColumn<PatientSegmentationRow, Integer> activityScoreCol;
    @FXML private TableColumn<PatientSegmentationRow, Object> lastActivityCol;
    @FXML private TableColumn<PatientSegmentationRow, String> behaviorDetailsCol;
    @FXML private Label behaviorVeryActiveBadge;
    @FXML private Label behaviorActiveBadge;
    @FXML private Label behaviorOccasionalBadge;
    @FXML private Label behaviorInactiveBadge;

    @FXML private Label errorLabel;

    private final PatientSegmentationService segmentationService = new PatientSegmentationService();

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder à la segmentation patients.");
            alert.show();
            Platform.runLater(this::goToHome);
            return;
        }

        String initials = getInitials(user);
        if (sidebarAvatar != null) sidebarAvatar.setText(initials);
        if (sidebarUserName != null) sidebarUserName.setText(user.getPrenom() + " " + user.getNom());
        if (sidebarRoleBadge != null) {
            sidebarRoleBadge.setText("Administrateur");
            SidebarRoleBadgeHelper.applyRoleStyle(sidebarRoleBadge, user);
        }
        if (topbarAvatar != null) topbarAvatar.setText(initials);
        if (topbarUserName != null) topbarUserName.setText(user.getPrenom() + " " + user.getNom());
        if (topbarDate != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            topbarDate.setText(LocalDate.now().format(fmt));
        }

        if (sidebarRoot != null) {
            SidebarNavHighlight.activate(sidebarRoot, "adm:segment");
        }

        setupTables();
        refresh();
    }

    private void setupTables() {
        if (profilesTable != null) profilesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (riskTable != null) riskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (behaviorTable != null) behaviorTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        groupCol.setCellValueFactory(new PropertyValueFactory<>("groupName"));
        countCol.setCellValueFactory(new PropertyValueFactory<>("patientCount"));
        sampleCol.setCellValueFactory(new PropertyValueFactory<>("samplePatients"));

        patientRiskCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        conditionRiskCol.setCellValueFactory(new PropertyValueFactory<>("mainCondition"));
        secondaryRiskCol.setCellValueFactory(new PropertyValueFactory<>("secondaryCount"));
        riskLevelCol.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));
        riskScoreCol.setCellValueFactory(new PropertyValueFactory<>("riskScore"));
        riskDetailsCol.setCellValueFactory(new PropertyValueFactory<>("riskDetails"));

        riskLevelCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setAlignment(Pos.CENTER);
                String c = item.toLowerCase(Locale.ROOT);
                if (c.contains("élev")) setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#b91c1c; -fx-font-weight:bold;");
                else if (c.contains("moy")) setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#b45309; -fx-font-weight:bold;");
                else if (c.contains("faib")) setStyle("-fx-background-color:#dcfce7; -fx-text-fill:#15803d; -fx-font-weight:bold;");
                else setStyle("-fx-text-fill:#334155;");
            }
        });

        patientBehaviorCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        segmentCol.setCellValueFactory(new PropertyValueFactory<>("behaviorSegment"));
        viewsCol.setCellValueFactory(new PropertyValueFactory<>("views30"));
        updatesCol.setCellValueFactory(new PropertyValueFactory<>("updates30"));
        activityScoreCol.setCellValueFactory(new PropertyValueFactory<>("activityScore"));
        lastActivityCol.setCellValueFactory(new PropertyValueFactory<>("lastActivityAt"));
        behaviorDetailsCol.setCellValueFactory(new PropertyValueFactory<>("behaviorDetails"));

        segmentCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setAlignment(Pos.CENTER);
                String c = item.toLowerCase(Locale.ROOT);
                if (c.contains("très")) setStyle("-fx-background-color:#dbeafe; -fx-text-fill:#1d4ed8; -fx-font-weight:bold;");
                else if (c.contains("actif")) setStyle("-fx-background-color:#dcfce7; -fx-text-fill:#15803d; -fx-font-weight:bold;");
                else if (c.contains("occasion")) setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#b45309; -fx-font-weight:bold;");
                else setStyle("-fx-background-color:#e5e7eb; -fx-text-fill:#374151; -fx-font-weight:bold;");
            }
        });

        lastActivityCol.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                if (item instanceof Timestamp ts) {
                    setText(ts.toLocalDateTime().format(formatter));
                } else {
                    setText(item.toString());
                }
            }
        });
    }

    @FXML
    public void refresh() {
        try {
            List<PatientSegmentationRow> rows = segmentationService.getPatientsSegmentation();

            profilesTable.setItems(FXCollections.observableArrayList(buildProfileGroups(rows)));

            List<PatientSegmentationRow> riskRows = new ArrayList<>(rows);
            riskRows.sort(Comparator.comparingInt(PatientSegmentationRow::getRiskScore).reversed());
            riskTable.setItems(FXCollections.observableArrayList(riskRows));

            List<PatientSegmentationRow> behaviorRows = new ArrayList<>(rows);
            behaviorRows.sort(Comparator.comparing(PatientSegmentationRow::getLastActivityAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            behaviorTable.setItems(FXCollections.observableArrayList(behaviorRows));

            updateBadges(rows);
            if (errorLabel != null) errorLabel.setText("");
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void updateBadges(List<PatientSegmentationRow> rows) {
        if (rows == null) rows = List.of();

        long low = rows.stream().filter(r -> containsIgnoreCase(r.getRiskLevel(), "faib")).count();
        long medium = rows.stream().filter(r -> containsIgnoreCase(r.getRiskLevel(), "moy")).count();
        long high = rows.stream().filter(r -> containsIgnoreCase(r.getRiskLevel(), "élev") || containsIgnoreCase(r.getRiskLevel(), "elev")).count();

        if (riskLowBadge != null) riskLowBadge.setText("Faible : " + low);
        if (riskMediumBadge != null) riskMediumBadge.setText("Moyen : " + medium);
        if (riskHighBadge != null) riskHighBadge.setText("Élevé : " + high);

        long veryActive = rows.stream().filter(r -> containsIgnoreCase(r.getBehaviorSegment(), "très")).count();
        long active = rows.stream().filter(r -> equalsIgnoreCase(r.getBehaviorSegment(), "Actif")).count();
        long occasional = rows.stream().filter(r -> containsIgnoreCase(r.getBehaviorSegment(), "occasion")).count();
        long inactive = rows.stream().filter(r -> containsIgnoreCase(r.getBehaviorSegment(), "inact")).count();

        if (behaviorVeryActiveBadge != null) behaviorVeryActiveBadge.setText("Très actif : " + veryActive);
        if (behaviorActiveBadge != null) behaviorActiveBadge.setText("Actif : " + active);
        if (behaviorOccasionalBadge != null) behaviorOccasionalBadge.setText("Occasionnel : " + occasional);
        if (behaviorInactiveBadge != null) behaviorInactiveBadge.setText("Inactif : " + inactive);
    }

    private boolean containsIgnoreCase(String text, String needle) {
        if (text == null || needle == null) return false;
        return text.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private List<ProfileGroupRow> buildProfileGroups(List<PatientSegmentationRow> rows) {
        Map<String, List<String>> groups = new LinkedHashMap<>();

        for (PatientSegmentationRow row : rows) {
            String condition = row.getMainCondition() == null ? "" : row.getMainCondition().trim();
            condition = extractPrimaryCondition(condition);
            String genre = row.getGenre() == null || row.getGenre().isBlank() ? "Non renseigné" : row.getGenre().trim();
            String comorb = row.getSecondaryCount() >= 2
                    ? "2+ comorbidités"
                    : row.getSecondaryCount() == 1 ? "1 comorbidité" : "0 comorbidité";
            String key = (condition.isBlank() ? "Non renseignée" : condition) + " · " + genre + " · " + comorb;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row.getPatientName());
        }

        List<ProfileGroupRow> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            List<String> names = entry.getValue();
            names.sort(String::compareToIgnoreCase);
            int count = names.size();

            int sampleSize = Math.min(5, count);
            String sample = String.join(", ", names.subList(0, sampleSize));
            if (count > sampleSize) sample += "  +" + (count - sampleSize);

            result.add(new ProfileGroupRow(entry.getKey(), count, sample));
        }

        result.sort(Comparator.comparingInt(ProfileGroupRow::getPatientCount).reversed());
        return result;
    }

    private String extractPrimaryCondition(String condition) {
        if (condition == null) return "";
        String c = condition.trim();
        if (c.isEmpty()) return "";
        String[] split = c.split("[,;]");
        return split.length > 0 ? split[0].trim() : c;
    }

    @FXML public void goToHome() { FxNavigation.navigateAdmin(getClass(), profilesTable, "home"); }
    @FXML public void goToUsers() { FxNavigation.navigateAdmin(getClass(), profilesTable, "users"); }
    @FXML public void goToBlockedAccounts() { FxNavigation.navigateAdmin(getClass(), profilesTable, "blocked"); }
    @FXML public void goToMedicalAudit() { FxNavigation.navigateAdmin(getClass(), profilesTable, "audit"); }
    @FXML public void goToPatientSegmentation() { FxNavigation.navigateAdmin(getClass(), profilesTable, "segment"); }
    @FXML public void goToStats() { FxNavigation.navigateAdmin(getClass(), profilesTable, "stats"); }
    @FXML public void goToAdminConsultLedger() { FxNavigation.navigateAdmin(getClass(), profilesTable, "consult"); }
    @FXML public void goToAdminNutritionRdv() { FxNavigation.navigateAdmin(getClass(), profilesTable, "rdv"); }
    @FXML public void goToProfile() { FxNavigation.navigateAdmin(getClass(), profilesTable, "profile"); }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        navigate("/com/chroniccare/login.fxml");
    }

    private void navigate(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            profilesTable.getScene().setRoot(root);
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur : " + e.getMessage());
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
