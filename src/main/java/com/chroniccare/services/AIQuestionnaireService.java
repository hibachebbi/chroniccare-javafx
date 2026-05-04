package com.chroniccare.services;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class AIQuestionnaireService {

    public List<String> generateQuestions(String condition) {
        try {
            OpenAIClient client = OpenAIOkHttpClient.fromEnv();
            String model = System.getenv().getOrDefault("OPENAI_QUESTIONNAIRE_MODEL", "gpt-5.4-mini");
            String safeCondition = condition == null ? "" : condition.trim();

            String prompt = """
                    Tu dois générer un questionnaire médical administratif à faire remplir par un patient.
                    Maladie principale : %s

                    Objectif :
                    - Collecter des informations utiles au dossier médical (symptômes, suivi, traitements, mesures pertinentes, facteurs aggravants, complications, urgences).

                    Exemples d'angles selon la maladie (à adapter) :
                    - Hypertension / HTA : mesures de tension, fréquence de mesure, traitement + observance, sel/alcool/tabac, symptômes, suivi.
                    - Diabète : glycémie, HbA1c, traitement, hypoglycémies, complications, suivi.
                    - Asthme : crises, inhalateur de secours, traitement de fond, déclencheurs, urgences, symptômes nocturnes.

                    Contraintes :
                    - Écris en français, ton clair et patient-friendly.
                    - Questions spécifiques à la maladie (évite les questions trop générales).
                    - Pas de diagnostic, pas de conseils, pas d'explication, pas d'introduction.
                    - Donne exactement 7 questions.
                    - Une question par ligne.
                    - Chaque ligne doit être une seule question terminée par ?
                    - Pas de numérotation, pas de puces, pas de Markdown.
                    """.formatted(safeCondition);

            ResponseCreateParams params = ResponseCreateParams.builder()
                    .model(model)
                    .input(prompt)
                    .build();

            Response response = client.responses().create(params);

            String text = OpenAIResponseTextExtractor.extractText(response);

            List<String> questions = parseQuestions(text);
            if (questions.size() >= 3) return questions;

            List<String> fallback = getFallbackQuestions(condition);
            return fallback.isEmpty() ? getGenericFallbackQuestions() : fallback;

        } catch (Exception e) {
            List<String> fallback = getFallbackQuestions(condition);
            return fallback.isEmpty() ? getGenericFallbackQuestions() : fallback;
        }
    }

    private List<String> parseQuestions(String text) {
        if (text == null || text.isBlank()) return List.of();

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String raw : text.split("\\R")) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) continue;

            // Defensive cleanup in case the model adds numbering or bullets.
            line = line.replaceFirst("^[-•*\\d).\\s]+", "").trim();
            if (line.isEmpty()) continue;

            if (!line.endsWith("?")) line = line + "?";
            unique.add(line);
            if (unique.size() >= 7) break;
        }

        return new ArrayList<>(unique);
    }

    private List<String> getFallbackQuestions(String condition) {
        try {
            MedicalQuestionnaireService templates = new MedicalQuestionnaireService();
            List<String> list = new ArrayList<>(templates.getQuestionsForCondition(condition).values());
            if (!list.isEmpty()) return list;
        } catch (Exception ignored) {
        }
        return getGenericFallbackQuestions();
    }

    private List<String> getGenericFallbackQuestions() {
        List<String> fallback = new ArrayList<>();
        fallback.add("Quels sont vos symptômes principaux ?");
        fallback.add("Depuis quand avez-vous cette maladie ou ces symptômes ?");
        fallback.add("Quel traitement suivez-vous actuellement ? (nom + dose + fréquence)");
        fallback.add("Avez-vous un suivi médical régulier ? (médecin + fréquence)");
        fallback.add("Avez-vous des allergies ou intolérances connues ?");
        return fallback;
    }
}
