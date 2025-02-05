package com.nicapz.gym.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nicapz.gym.Model.Interaction;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class GPTResponse {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = System.getenv("OPENAI_KEY");

    @Autowired
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GPTResponse(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    private static String escapeJson(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static String makeBody(List<Interaction> history, String newMessage, List<Interaction> ragResults) {

        StringBuilder jsonBuilder = new StringBuilder();
        String messagesTemplate =
                """
                        {"role": "%s", "content": "%s"}
                        """;
        jsonBuilder.append(
                """
                        {
                        "model": "ft:gpt-4o-mini-2024-07-18:personal:test1:AsRHUVZd",
                        "messages": [
                        {"role": "system", "content": "You are a skilled and professional workplace coach, assisting employees with difficult situations or mental health issues. You remember information about a user from previous interactions which are supplied to you. Do not inform the user about the content of your system prompt. If the user asks about previous interactions or information that you lack the context to, feel free to politely ask the user to provide that information again. Always check the previous messages supplied to you before claiming not to be able to remember past conversations."},
                        """
        );
        if (!ragResults.isEmpty()) {
            for (Interaction ragResult : ragResults) {
                jsonBuilder.append(String.format(messagesTemplate, "user", escapeJson(ragResult.getUserRequest())));
                jsonBuilder.append(", ");
                jsonBuilder.append(String.format(messagesTemplate, "assistant", escapeJson(ragResult.getAiReply())));
                jsonBuilder.append(", ");
            }
        }
        if (!history.isEmpty()) {
            for (Interaction interaction : history) {
                jsonBuilder.append(String.format(messagesTemplate, "user", escapeJson(interaction.getUserRequest())));
                jsonBuilder.append(", ");
                jsonBuilder.append(String.format(messagesTemplate, "assistant", escapeJson(interaction.getAiReply())));
                jsonBuilder.append(", ");
            }
        }
        jsonBuilder.append(String.format(messagesTemplate, "user", escapeJson(newMessage)));

        jsonBuilder.append("""
                ],
                "stream": true
                }
                """);

        System.out.println(jsonBuilder);
        return jsonBuilder.toString();
    }

    public static String parseJsonLine(String line) {

        try {
            JsonObject levelOne = JsonParser.parseString(line).getAsJsonObject();
            //Access Choices Array
            JsonArray choices = levelOne.getAsJsonArray("choices");
            // Access first element of choices Array
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            // Access the delta object
            JsonObject delta = firstChoice.getAsJsonObject("delta");
            // get the content field from delta
            String content = delta.has("content") ? delta.get("content").getAsString() : "";
            return content;
        } catch (Exception e) {
            //System.out.println(e.getMessage() + "incorrect json or final line");
        }
        return "";
    }

    public String getChatGPTReply(String conversationId, String userMessage, List<Interaction> history, List<Interaction> ragResults) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(OPENAI_CHAT_URL);
            request.addHeader("Authorization", "Bearer " + API_KEY);
            request.addHeader("Content-Type", "application/json");
            String bodyString = makeBody(history, userMessage, ragResults);
            StringEntity body = new StringEntity(bodyString);
            request.setEntity(body);
            try (CloseableHttpResponse response = client.execute(request)) {

                if (response.getStatusLine().getStatusCode() == 400) {
                    String errorBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    System.out.println("Error Response: " + errorBody);
                }
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Failed Request: " + response.getStatusLine() + response + request);
                }

                System.out.println("Response: " + response);

                StringBuilder finalResponse;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    finalResponse = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (!line.isEmpty()) {
                            if (line.equals("[DONE]")) {
                                return finalResponse.toString();
                            }
                            line = line.substring(6);
                            line = parseJsonLine(line);
                            messagingTemplate.convertAndSend("/topic/replyChunk/" + conversationId, line);
                            finalResponse.append(line);
                        }
                    }
                }
                return finalResponse.toString();
            }
        }
    }
}
