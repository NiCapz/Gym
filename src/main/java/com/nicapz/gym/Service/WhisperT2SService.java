package com.nicapz.gym.Service;

import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Service
public class WhisperT2SService {

    private static final String OPENAI_T2S_URL = "https://api.openai.com/v1/audio/speech";
    private static final String API_KEY = System.getenv("OPENAI_KEY");

    public byte[] synthesizeSpeech(String text) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost("https://api.openai.com/v1/audio/speech");
            request.addHeader("Authorization", "Bearer " + API_KEY);
            request.addHeader("Content-Type", "application/json");
            StringEntity body = makeJsonBody(text);
            request.setEntity(body);
            try (CloseableHttpResponse response = client.execute(request)) {
                return response.getEntity().getContent().readAllBytes();
            }
        }
    }

    private static StringEntity makeJsonBody(String text) throws UnsupportedEncodingException {
        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("model", "tts-1");
        jsonBody.put("input", text);
        jsonBody.put("voice", "onyx");
        jsonBody.put("speed", 1.5);
        Gson gson = new Gson();
        String json = gson.toJson(jsonBody);
        return new StringEntity(json);
    }
}