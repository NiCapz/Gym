package com.nicapz.gym.Service;

import com.nicapz.gym.Model.Interaction;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ChatGPTConversationService {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "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";

    public static String makeBody(List<Interaction> interactions, String newMessage) {

        StringBuilder jsonBuilder = new StringBuilder();
        String messagesTemplate =
                """
                        {"role": "%s", "content": "%s"}
                        """;
        jsonBuilder.append(
                """
                        {
                        "model": "gpt-4",
                        "messages": [
                        {"role": "system", "content": "You are a helpful assistant. Please use a maximum of 900 characters for your answers."},
                        """
        );
        if (!interactions.isEmpty()) {
            for (Interaction interaction : interactions) {
                jsonBuilder.append(String.format(messagesTemplate, "user", interaction.getUserRequest()));
                jsonBuilder.append(", ");
                jsonBuilder.append(String.format(messagesTemplate, "assistant", interaction.getAiReply()));
                jsonBuilder.append(", ");
            }
        }
        jsonBuilder.append(String.format(messagesTemplate, "user", newMessage));

        jsonBuilder.append("""
                ]
                }
                """);

        return jsonBuilder.toString();
    }

    public String getChatGPTReply(String conversationId, String userMessage, List<Interaction> history) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(OPENAI_CHAT_URL);
            request.addHeader("Authorization", "Bearer " + API_KEY);
            request.addHeader("Content-Type", "application/json");
            String bodyString = makeBody(history, userMessage);
            StringEntity body = new StringEntity(bodyString);
            request.setEntity(body);
            System.out.println("... sending request ...");
            try (CloseableHttpResponse response = client.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }


}
