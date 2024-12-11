package com.nicapz.gym.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nicapz.gym.Controller.ChatController;
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
import java.util.List;

@Service
public class StreamGPTResponse {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "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";
    //private static final String API_KEY = System.getenv("API_KEY");

    @Autowired
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public StreamGPTResponse(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

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
                ],
                "stream": true
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
            System.out.println(bodyString);
            StringEntity body = new StringEntity(bodyString);
            request.setEntity(body);
            System.out.println("... sending request ...");
            try (CloseableHttpResponse response = client.execute(request)) {

                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Failed Request: " + response.getStatusLine());
                }

                StringBuilder finalResponse;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    finalResponse = new StringBuilder();
                    String line;
                    String lineContent = "";

                    while ((line = reader.readLine()) != null) {
                        if (!line.isEmpty()) {
                            System.out.println("Received Chunk: " + line);
                        }
                        if (!line.isEmpty()) {
                            line = line.substring(6).trim();
                        }
                        JsonObject jsonChunk = JsonParser.parseString(line).getAsJsonObject();
                        if (jsonChunk.has("choices")) {
                            JsonArray choices = jsonChunk.getAsJsonArray("choices");
                            for (JsonElement choice : choices) {
                                JsonObject choiceObj = choice.getAsJsonObject();
                                if (choiceObj.has("delta")) {
                                    JsonObject delta = choiceObj.getAsJsonObject("delta");
                                    if (delta.has("content")) {
                                        lineContent = delta.get("content").getAsString();
                                    }
                                    messagingTemplate.convertAndSend("/topic/replyChunk/"+ conversationId, lineContent);

                                    // Append the content to the final response
                                    finalResponse.append(lineContent);
                                }
                            }
                        }

                    }
                }
                return finalResponse.toString();
            }
        }
    }


}
