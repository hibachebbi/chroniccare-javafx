package com.chroniccare.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Remplace la racine de scene pour éviter les copier-collers de naviguation.
 */
public final class FxNavigation {

    private FxNavigation() {
    }

    /**
     * {@code path} exemple : {@code "/com/chroniccare/home.fxml"} (ressource sous
     * classpath).
     */
    public static void replaceRoot(Class<?> classpathAnchor, Node sourceUnderScene,
            String path) throws Exception {
        Parent root = FXMLLoader.load(classpathAnchor.getResource(path));
        sourceUnderScene.getScene().setRoot(root);
    }

    /**
     * Navigation admin : charge la page dans le shell admin si disponible,
     * sinon navigue vers admin-shell.fxml avec la page en attente.
     */
    public static void navigateAdmin(Class<?> anchor, Node source, String page) {
        com.chroniccare.controllers.AdminShellController shell = com.chroniccare.controllers.AdminShellController
                .getInstance();
        if (shell != null && shell.isInActiveScene()) {
            switch (page) {
                case "home" -> shell.showHome();
                case "users" -> shell.showUsers();
                case "blocked" -> shell.showBlocked();
                case "audit" -> shell.showAudit();
                case "segment" -> shell.showSegmentation();
                case "stats" -> shell.showStats();
                case "consult" -> shell.showConsultations();
                case "rdv" -> shell.showRdv();
                case "profile" -> shell.showProfile();
            }
        } else {
            SessionManager.getInstance().setPendingAdminPage(page);
            try {
                replaceRoot(anchor, source, "/com/chroniccare/admin-shell.fxml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Navigation nutri : charge la page dans le shell nutri si disponible,
     * sinon navigue vers nutri-shell.fxml avec la page en attente.
     */
    public static void navigateNutri(Class<?> anchor, Node source, String page) {
        com.chroniccare.controllers.NutriShellController shell = com.chroniccare.controllers.NutriShellController
                .getInstance();
        if (shell != null && shell.isInActiveScene()) {
            switch (page) {
                case "home" -> shell.showHome();
                case "profile" -> shell.showProfile();
                case "suivi" -> shell.showSuivi();
                case "etat" -> shell.showEtat();
                case "activite" -> shell.showActivite();
                case "rdv" -> shell.showRdv();
            }
        } else {
            SessionManager.getInstance().setPendingNutriPage(page);
            try {
                replaceRoot(anchor, source, "/com/chroniccare/nutri-shell.fxml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Navigation patient : charge la page dans le shell patient si disponible.
     */
    public static void navigatePatient(Class<?> anchor, Node source, String page) {
        com.chroniccare.controllers.PatientShellController shell = com.chroniccare.controllers.PatientShellController
                .getInstance();
        // Vérifier que le shell est bien dans la scène active
        if (shell != null && shell.isInActiveScene()) {
            switch (page) {
                case "home" -> shell.showHome();
                case "profile" -> shell.showProfile();
                case "etat" -> shell.showEtat();
                case "activite" -> shell.showActivite();
                case "events" -> shell.showEvents();
                case "registrations" -> shell.showRegistrations();
                case "rdv" -> shell.showRdv();
                case "consultations" -> shell.showConsultations();
                // Navigation boutique intégrée au patient-shell
                case "produits" -> shell.showProduits();
                case "produit" -> shell.showProduitDetail();
                case "panier" -> shell.showPanier();
                case "commandes" -> shell.showCommandes();
                case "livraisons" -> shell.showLivraisons();
                case "wishlist" -> shell.showWishlist();
                case "checkout" -> shell.showCheckout();
            }
        } else {
            SessionManager.getInstance().setPendingPatientPage(page);
            try {
                replaceRoot(anchor, source, "/com/chroniccare/patient-shell.fxml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
