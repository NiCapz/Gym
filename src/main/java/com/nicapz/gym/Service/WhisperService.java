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
    private static final String API_KEY = "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";

    public String transcribeAudio(byte[] audioBytes, String fileType) throws IOException {
        String fileName = "testFile";
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(WHISPER_URL);
            request.addHeader("Authorization", "Bearer " + API_KEY);

            var builder = MultipartEntityBuilder.create()
                    .addBinaryBody("file", audioBytes, ContentType.create("audio/wav"), "audio/wav")
                    .addTextBody("model", "whisper-1");
            request.setEntity(builder.build());

            try (CloseableHttpResponse response = client.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("Whisper API Response: " + responseBody);
                return EntityUtils.toString(response.getEntity());
            }
        }
    }




}
