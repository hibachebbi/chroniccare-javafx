package com.chroniccare.utils;

import com.chroniccare.entities.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }
    public void logout() { this.currentUser = null; }

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
}