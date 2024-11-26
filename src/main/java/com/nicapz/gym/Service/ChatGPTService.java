package com.nicapz.gym.Service;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ChatGPTService {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "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";

    public String getChatGPTReply (String userMessage) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(OPENAI_CHAT_URL);
            request.addHeader("Authorization", "Bearer " + API_KEY);
            request.addHeader("Content-Type", "application/json");

            StringEntity body = new StringEntity(
                    """
                            {
                            "model": "gpt-4",
                            "messages": [
                            {"role": user, "content": "%s"}
                            ]
                            }
                            """.formatted(userMessage)
            );
            request.setEntity(body);

            try (CloseableHttpResponse response = client.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

}
