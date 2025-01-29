package com.nicapz.gym.Service;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;


import java.io.IOException;

@Service
public class WhisperService {

    private static final String WHISPER_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String API_KEY = System.getenv("OPENAI_KEY");

    public String transcribeAudio(byte[] audioBytes, String fileType) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(WHISPER_URL);
            request.addHeader("Authorization", "Bearer " + API_KEY);
            var builder = MultipartEntityBuilder.create()
                    .addBinaryBody("file", audioBytes, ContentType.create("audio/wav"), "audio/wav")
                    .addTextBody("model", "whisper-1");
            request.setEntity(builder.build());
            try (CloseableHttpResponse response = client.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }
}
