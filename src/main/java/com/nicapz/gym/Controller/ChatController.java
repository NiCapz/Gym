package com.nicapz.gym.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nicapz.gym.Service.ChatGPTService;
import com.nicapz.gym.Service.WhisperService;
import com.nicapz.gym.Service.WhisperT2SService;
import org.apache.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final WhisperService whisperService = new WhisperService();
    private final ChatGPTService chatGPTService = new ChatGPTService();
    private final WhisperT2SService whisperT2SService = new WhisperT2SService();


    private String parseReply(String reply) {

        JsonObject levelOne = JsonParser.parseString(reply).getAsJsonObject();
        JsonArray choices = levelOne.getAsJsonArray("choices");
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        String content = message.get("content").getAsString();

        return content;
    }

    @PostMapping("/process-audio")
    public ResponseEntity<?> processAudio(@RequestParam("file") MultipartFile file) throws IOException {
        try {

            String transcription = whisperService.transcribeAudio(file.getBytes(), file.getContentType());
            JsonObject transcriptionJson = JsonParser.parseString(transcription).getAsJsonObject();
            transcription = transcriptionJson.get("text").getAsString();
            System.out.println(transcription);

            String chatReply = chatGPTService.getChatGPTReply(transcription);
            chatReply = parseReply(chatReply);
            System.out.println(chatReply);

            byte[] audioBytes = whisperT2SService.synthesizeSpeech(chatReply);
            String audio = Base64.getEncoder().encodeToString(audioBytes);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(Map.of(
                            "transcription", transcription,
                            "reply", chatReply,
                            "audio", audio
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

}
