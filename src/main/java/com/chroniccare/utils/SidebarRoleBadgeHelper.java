package com.chroniccare.utils;

import com.chroniccare.models.User;
import javafx.scene.control.Label;

/**
 * Applique la meme palette de couleurs pour la pilule de role dans la barre laterale,
 * quel que soit l'ecran (home, coach-events, etc.).
 */
public final class SidebarRoleBadgeHelper {

    private static final String COACH = "sidebar-role-coach";
    private static final String PATIENT = "sidebar-role-patient";
    private static final String ADMIN = "sidebar-role-admin";
    private static final String NUTRITION = "sidebar-role-nutritionniste";

    private SidebarRoleBadgeHelper() {
    }

    public static void applyRoleStyle(Label badge, User user) {
        if (badge == null) {
            return;
        }
        badge.getStyleClass().removeAll(COACH, PATIENT, ADMIN, NUTRITION);
        if (user == null || user.getRoles() == null) {
            return;
        }
        if (user.getRoles().contains("ROLE_COACH")) {
            badge.getStyleClass().add(COACH);
        } else if (user.getRoles().contains("ROLE_PATIENT")) {
            badge.getStyleClass().add(PATIENT);
        } else if (user.getRoles().contains("ROLE_ADMIN")) {
            badge.getStyleClass().add(ADMIN);
        } else if (user.getRoles().contains("ROLE_NUTRITIONNISTE")) {
            badge.getStyleClass().add(NUTRITION);
        }
    }

    public static void applyAdminStyle(Label badge) {
        if (badge == null) {
            return;
        }
        badge.getStyleClass().removeAll(COACH, PATIENT, ADMIN, NUTRITION);
        badge.getStyleClass().add(ADMIN);
    }
}
