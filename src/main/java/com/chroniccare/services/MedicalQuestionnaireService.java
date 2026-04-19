package com.chroniccare.services;

import java.util.LinkedHashMap;
import java.util.Map;

public class MedicalQuestionnaireService {

    public Map<String, String> getQuestionsForCondition(String condition) {
        String c = condition == null ? "" : condition.toLowerCase();
        Map<String, String> questions = new LinkedHashMap<>();

        if (c.contains("diab")) {
            questions.put("glycemie", "Quel est votre niveau moyen de glycémie ?");
            questions.put("traitement", "Quel traitement prenez-vous actuellement ?");
            questions.put("hypoglycemie", "Avez-vous déjà des épisodes d'hypoglycémie ?");
            questions.put("alimentation", "Suivez-vous un régime alimentaire spécifique ?");
            return questions;
        }

        if (c.contains("hyper")) {
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
}