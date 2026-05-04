package com.chroniccare.utils;

import com.chroniccare.models.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    /** Après navigation vers home.fxml : "etat" ou "activite" pour ouvrir l'onglet suivi correspondant. */
    private volatile String pendingHomeSuiviTab;
    /** RDV sélectionné côté nutritionniste (consultation liée). */
    private volatile Integer selectedAppointmentId;
    /** Page à afficher dans le shell admin après navigation. */
    private volatile String pendingAdminPage;
    /** Page à afficher dans le shell nutri après navigation. */
    private volatile String pendingNutriPage;
    /** Page à afficher dans le shell patient après navigation. */
    private volatile String pendingPatientPage;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }
    public void logout() {
        this.currentUser = null;
        this.pendingHomeSuiviTab = null;
        this.selectedAppointmentId = null;
        this.pendingAdminPage = null;
        this.pendingNutriPage = null;
        this.pendingPatientPage = null;
    }

    public void setPendingAdminPage(String page) { this.pendingAdminPage = page; }
    public String consumePendingAdminPage() { String p = pendingAdminPage; pendingAdminPage = null; return p; }

    public void setPendingNutriPage(String page) { this.pendingNutriPage = page; }
    public String consumePendingNutriPage() { String p = pendingNutriPage; pendingNutriPage = null; return p; }

    public void setPendingPatientPage(String page) { this.pendingPatientPage = page; }
    public String consumePendingPatientPage() { String p = pendingPatientPage; pendingPatientPage = null; return p; }

    public void setPendingHomeSuiviTab(String tab) {
        this.pendingHomeSuiviTab = tab;
    }

    /** Retourne la valeur une fois puis l'efface. */
    public String consumePendingHomeSuiviTab() {
        String t = pendingHomeSuiviTab;
        pendingHomeSuiviTab = null;
        return t;
    }

    public boolean isAdmin() {
        return currentUser != null &&
                currentUser.getRoles().contains("ROLE_ADMIN");
    }

    public boolean isPatient() {
        return currentUser != null &&
                currentUser.getRoles().contains("ROLE_PATIENT");
    }

    public boolean isCoach() {
        return currentUser != null &&
                currentUser.getRoles().contains("ROLE_COACH");
    }

    public boolean isNutritionniste() {
        return currentUser != null &&
                currentUser.getRoles().contains("ROLE_NUTRITIONNISTE");
    }

    public Integer getSelectedAppointmentId() {
        return selectedAppointmentId;
    }

    public void setSelectedAppointmentId(Integer selectedAppointmentId) {
        this.selectedAppointmentId = selectedAppointmentId;
    }
}