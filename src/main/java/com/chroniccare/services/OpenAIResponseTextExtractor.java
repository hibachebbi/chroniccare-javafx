package com.chroniccare.services;

import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;

public final class OpenAIResponseTextExtractor {

    private OpenAIResponseTextExtractor() {
    }

    public static String extractText(Response response) {
        if (response == null) return "";

        StringBuilder sb = new StringBuilder();

        for (ResponseOutputItem item : response.output()) {
            if (item == null || !item.isMessage()) continue;

            ResponseOutputMessage message = item.asMessage();
            for (ResponseOutputMessage.Content content : message.content()) {
                if (content == null) continue;

                if (content.isOutputText()) {
                    sb.append(content.asOutputText().text());
                } else if (content.isRefusal()) {
                    String refusal = content.asRefusal().refusal();
                    if (refusal != null && !refusal.isBlank()) {
                        sb.append(refusal);
                    }
                }
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }
}

