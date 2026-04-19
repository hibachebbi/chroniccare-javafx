package com.chroniccare.services;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import java.util.ArrayList;
import java.util.List;

public class AIQuestionnaireService {

    public List<String> generateQuestions(String condition) {
        try {
            OpenAIClient client = OpenAIOkHttpClient.fromEnv();
            String model = System.getenv().getOrDefault("OPENAI_QUESTIONNAIRE_MODEL", "gpt-4.1-mini");

            String prompt = """
                    Génère un questionnaire médical administratif en français pour un patient atteint de : %s.
                    Donne uniquement 5 questions courtes, claires et utiles.
                    Pas de diagnostic.
                    Pas d'explication.
                    Une question par ligne.
                    """.formatted(condition);

            ResponseCreateParams params = ResponseCreateParams.builder()
                    .model(model)
                    .input(prompt)
                    .build();

            Response response = client.responses().create(params);

            String text = OpenAIResponseTextExtractor.extractText(response);

            List<String> questions = new ArrayList<>();
            for (String line : text.split("\n")) {
                line = line.trim();
                if (!line.isEmpty()) {
                    questions.add(line.replaceFirst("^[-0-9.).\\s]+", ""));
                }
            }

            if (questions.isEmpty()) {
                return getFallbackQuestions();
            }

            return questions;

        } catch (Exception e) {
            return getFallbackQuestions();
        }
    }

    private List<String> getFallbackQuestions() {
        List<String> fallback = new ArrayList<>();
        fallback.add("Quels sont vos symptômes principaux ?");
        fallback.add("Quel traitement suivez-vous actuellement ?");
        fallback.add("Depuis quand avez-vous cette maladie ?");
        fallback.add("Avez-vous des antécédents médicaux ?");
        fallback.add("Avez-vous un suivi médical régulier ?");
        return fallback;
    }
}
