package com.chroniccare.services;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MedicalQuestionnaireService {

    public Map<String, String> getQuestionsForCondition(String condition) {
        String c = normalize(condition);
        Map<String, String> questions = new LinkedHashMap<>();

        // Diabetes (type 1/type 2)
        if (c.contains("diab")) {
            questions.put("diab.glycemie", "Quel est votre niveau moyen de glycémie (à jeun / après repas) ? (valeurs approximatives)");
            questions.put("diab.hba1c", "Quelle est votre dernière HbA1c (si connue) ? (date + valeur)");
            questions.put("diab.traitement", "Quel traitement suivez-vous actuellement ? (nom + dose + fréquence)");
            questions.put("diab.hypo", "Avez-vous eu des épisodes d'hypoglycémie récemment ? (oui/non + fréquence)");
            questions.put("diab.complications", "Avez-vous un suivi ou des complications (yeux, reins, pieds, nerfs) ? (précisez)");
            return questions;
        }

        // Hypertension / HTA
        if (c.contains("hyper") || c.contains("hta") || c.contains("tension")) {
            questions.put("hta.mesures", "Quelles sont vos dernières mesures de tension artérielle ? (ex: 13/8)");
            questions.put("hta.frequence", "À quelle fréquence mesurez-vous votre tension ? (par semaine / mois)");
            questions.put("hta.traitement", "Prenez-vous un traitement antihypertenseur ? (nom + dose + régularité)");
            questions.put("hta.symptomes", "Avez-vous des symptômes (maux de tête, vertiges, palpitations, essoufflement) ? (oui/non + détails)");
            questions.put("hta.habitudes", "Tabac, alcool, sel et activité physique : décrivez brièvement vos habitudes.");
            return questions;
        }

        // Asthma
        if (c.contains("asth")) {
            questions.put("asth.crises", "À quelle fréquence avez-vous des crises ? (semaine / mois)");
            questions.put("asth.secours", "Utilisez-vous un inhalateur de secours ? (oui/non + combien de fois par semaine)");
            questions.put("asth.fond", "Avez-vous un traitement de fond ? (nom + fréquence)");
            questions.put("asth.declencheurs", "Quels sont vos principaux déclencheurs ? (effort, allergènes, froid, fumée, infections…)");
            questions.put("asth.urgences", "Avez-vous eu des urgences / hospitalisations pour asthme ? (oui/non + date approximative)");
            return questions;
        }

        // Fallback: generic but still precise (administrative, non-diagnostic)
        questions.put("gen.symptomes", "Quels sont vos symptômes principaux aujourd'hui ? (liste + intensité)");
        questions.put("gen.debut", "Depuis quand avez-vous cette maladie / ces symptômes ? (date ou durée)");
        questions.put("gen.traitement", "Quel traitement suivez-vous actuellement ? (nom + dose + fréquence)");
        questions.put("gen.suivi", "Avez-vous un suivi médical régulier ? (médecin + fréquence des consultations)");
        questions.put("gen.allergies", "Avez-vous des allergies ou intolérances connues ? (médicaments / aliments)");

        return questions;
    }

    public Map<String, String> getLegacyLabelsForCondition(String condition) {
        String c = normalize(condition);
        Map<String, String> questions = new LinkedHashMap<>();

        if (c.contains("diab")) {
            questions.put("glycemie", "Quel est votre niveau moyen de glycémie ?");
            questions.put("traitement", "Quel traitement prenez-vous actuellement ?");
            questions.put("hypoglycemie", "Avez-vous déjà des épisodes d'hypoglycémie ?");
            questions.put("alimentation", "Suivez-vous un régime alimentaire spécifique ?");
            return questions;
        }

        if (c.contains("hyper") || c.contains("hta") || c.contains("tension")) {
            questions.put("tension", "Quelle est votre tension artérielle moyenne ?");
            questions.put("traitement", "Prenez-vous un traitement antihypertenseur ?");
            questions.put("sel", "Consommez-vous beaucoup de sel ?");
            questions.put("antecedents", "Avez-vous des antécédents familiaux ?");
            return questions;
        }

        if (c.contains("asth")) {
            questions.put("crises", "À quelle fréquence avez-vous des crises ?");
            questions.put("inhalateur", "Utilisez-vous un inhalateur ?");
            questions.put("allergies", "Avez-vous des allergies connues ?");
            questions.put("declencheurs", "Connaissez-vous vos facteurs déclencheurs ?");
            return questions;
        }

        questions.put("symptomes", "Quels sont vos symptômes principaux ?");
        questions.put("traitement", "Quel traitement suivez-vous actuellement ?");
        questions.put("antecedents", "Avez-vous des antécédents médicaux ?");
        questions.put("mode_de_vie", "Décrivez brièvement votre mode de vie.");
        return questions;
    }

    private static String normalize(String input) {
        if (input == null) return "";
        String s = input.trim().toLowerCase(Locale.FRENCH);
        if (s.isEmpty()) return "";
        // Remove accents to make keyword matching more tolerant (ex: "diabète" -> "diabete").
        String noAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccents;
    }
}
