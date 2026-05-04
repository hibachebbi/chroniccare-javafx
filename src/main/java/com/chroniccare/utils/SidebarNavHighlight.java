package com.chroniccare.utils;

import javafx.scene.Parent;
import javafx.scene.control.Button;

/**
 * Active un bouton de navigation (statut visuel uniquement).
 * Les boutins concernés ont les classes CSS {@code nav-btn} et {@code nav-track}.
 */
public final class SidebarNavHighlight {

    private static final String TRACK_SELECTOR = ".nav-track";
    private static final String ACTIVE = "nav-btn-active";

    private SidebarNavHighlight() {}

    /** @param highlightKey valeur {@code Button#setUserData} (ex. {@code adm:users}). */
    public static void activate(Parent root, String highlightKey) {
        if (root == null || highlightKey == null) {
            return;
        }
        root.lookupAll(TRACK_SELECTOR).stream()
                .filter(n -> n instanceof Button)
                .map(n -> (Button) n)
                .forEach(b -> {
                    b.getStyleClass().remove(ACTIVE);
                    Object ud = b.getUserData();
                    if (highlightKey.equals(ud instanceof String ? (String) ud : ud != null ? String.valueOf(ud) : null)) {
                        if (!b.getStyleClass().contains(ACTIVE)) {
                            b.getStyleClass().add(ACTIVE);
                        }
                    }
                });
    }
}
