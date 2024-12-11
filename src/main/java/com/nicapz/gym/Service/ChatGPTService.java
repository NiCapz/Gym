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
    //private static final String API_KEY = System.getenv("API_KEY");

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
                            {"role": "user", "content": "%s"}
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
