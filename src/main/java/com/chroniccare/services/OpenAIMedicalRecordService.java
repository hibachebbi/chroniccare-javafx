package com.chroniccare.services;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class OpenAIMedicalRecordService {

    public String generateMedicalRecord(
            String patientName,
            String mainCondition,
            List<String> secondaryConditions,
            Map<String, String> questionnaireAnswers,
            String importedPdfText
    ) {
        String prompt = buildPrompt(patientName, mainCondition, secondaryConditions, questionnaireAnswers, importedPdfText);

        try {
            OpenAIClient client = OpenAIOkHttpClient.fromEnv();
            String model = System.getenv().getOrDefault("OPENAI_MEDICAL_RECORD_MODEL", "gpt-5.4-mini");

            ResponseCreateParams params = ResponseCreateParams.builder()
                    .model(model)
                    .input(prompt)
                    .build();

            Response response = client.responses().create(params);
            String text = OpenAIResponseTextExtractor.extractText(response);

            if (text == null || text.isBlank()) {
                return getFallbackRecord(patientName, mainCondition, secondaryConditions, questionnaireAnswers, importedPdfText);
            }

            return text;
        } catch (Exception e) {
            return getFallbackRecord(patientName, mainCondition, secondaryConditions, questionnaireAnswers, importedPdfText);
        }
    }

    private String getFallbackRecord(
            String patientName,
            String mainCondition,
            List<String> secondaryConditions,
            Map<String, String> questionnaireAnswers,
            String importedPdfText
    ) {
        StringJoiner answers = new StringJoiner("\n");
        if (questionnaireAnswers != null) {
            for (Map.Entry<String, String> entry : questionnaireAnswers.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isBlank()) continue;
                answers.add("- " + entry.getKey() + " : " + entry.getValue());
            }
        }

        String extraConditions = (secondaryConditions == null || secondaryConditions.isEmpty())
                ? "Aucune"
                : String.join(", ", secondaryConditions);

        String pdfPart = (importedPdfText == null || importedPdfText.isBlank())
                ? "Aucun PDF importé."
                : importedPdfText;

        String patientNameDisplay = patientName == null || patientName.isBlank() ? "Non renseigné" : patientName;
        String mainConditionDisplay = mainCondition == null || mainCondition.isBlank() ? "Non renseignée" : mainCondition;
        String answersDisplay = answers.toString().isBlank() ? "- Aucune réponse." : answers.toString();

        return """
                Dossier médical patient (mode dégradé — IA indisponible)

                1. Identité patient
                - Patient : %s

                2. Condition principale
                - %s

                3. Maladies associées
                - %s

                4. Symptômes / informations déclarées
                %s

                5. Traitements / habitudes déclarées
                - Non renseigné (à compléter).

                6. Points de vigilance
                - Non renseigné.

                7. Résumé administratif
                - Condition principale : %s
                - Maladies associées : %s

                8. Avertissement
                Ce document est généré automatiquement et ne remplace pas un avis médical.

                Annexe — Texte importé depuis PDF
                %s
                """.formatted(
                patientNameDisplay,
                mainConditionDisplay,
                extraConditions,
                answersDisplay,
                mainConditionDisplay,
                extraConditions,
                pdfPart
        );
    }

    private String buildPrompt(
            String patientName,
            String mainCondition,
            List<String> secondaryConditions,
            Map<String, String> questionnaireAnswers,
            String importedPdfText
    ) {
        StringJoiner answers = new StringJoiner("\n");
        if (questionnaireAnswers != null) {
            for (Map.Entry<String, String> entry : questionnaireAnswers.entrySet()) {
                answers.add("- " + entry.getKey() + " : " + entry.getValue());
            }
        }

        String extraConditions = (secondaryConditions == null || secondaryConditions.isEmpty())
                ? "Aucune"
                : String.join(", ", secondaryConditions);

        String pdfPart = (importedPdfText == null || importedPdfText.isBlank())
                ? "Aucun PDF importé."
                : importedPdfText;

        return """
                Tu es un assistant de rédaction médicale administrative.
                Tu ne poses pas de diagnostic et tu ne remplaces jamais un médecin.
                Génère un dossier médical patient clair, structuré et professionnel en français.

                Contraintes :
                - Ne pas inventer d'informations absentes.
                - Mentionner explicitement quand une information est inconnue.
                - Produire un document structuré avec les sections suivantes :
                  1. Identité patient
                  2. Condition principale
                  3. Maladies associées
                  4. Symptômes / informations déclarées
                  5. Traitements / habitudes déclarées
                  6. Points de vigilance
                  7. Résumé administratif
                  8. Avertissement : ce document est généré automatiquement et ne remplace pas un avis médical.

                Patient : %s
                Condition principale : %s
                Maladies associées : %s

                Réponses questionnaire :
                %s

                Texte importé depuis PDF :
                %s
                """.formatted(
                patientName == null ? "Non renseigné" : patientName,
                mainCondition == null ? "Non renseignée" : mainCondition,
                extraConditions,
                answers.toString().isBlank() ? "Aucune réponse." : answers.toString(),
                pdfPart
        );
    }
}
