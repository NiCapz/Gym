package com.nicapz.gym.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nicapz.gym.Model.Interaction;
import com.nicapz.gym.Repositories.InteractionRepository;
import com.nicapz.gym.Service.*;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final WhisperService whisperService = new WhisperService();
    private final ChatGPTService chatGPTService = new ChatGPTService();
    private final WhisperT2SService whisperT2SService = new WhisperT2SService();
    private final ChatGPTConversationService chatGPTConversationService = new ChatGPTConversationService();

    @Autowired
    InteractionService interactionService;

    private String parseReply(String reply) {

        JsonObject levelOne = JsonParser.parseString(reply).getAsJsonObject();
        JsonArray choices = levelOne.getAsJsonArray("choices");
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        String content = message.get("content").getAsString();

        return content;
    }


    @PostMapping("/process-audio")
    public ResponseEntity<?> processAudio2(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            String transcription = whisperService.transcribeAudio(file.getBytes(), file.getContentType());
            JsonObject transcriptionJson = JsonParser.parseString(transcription).getAsJsonObject();
            transcription = transcriptionJson.get("text").getAsString();
            System.out.println(transcription);

            List<Interaction> history = interactionService.getInteractions();
            System.out.println("... done retrieving history ...");

            String chatReply = chatGPTConversationService.getChatGPTReply("1", transcription, history);
            chatReply = parseReply(chatReply);
            System.out.println(chatReply);

            byte[] audioBytes = whisperT2SService.synthesizeSpeech(chatReply);
            String audio = Base64.getEncoder().encodeToString(audioBytes);

            Interaction interaction = new Interaction("1", transcription, chatReply);
            interactionService.saveInteraction(interaction);
            System.out.println("... interaction saved ...");

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

    @PostMapping("/process-audio2")
    public ResponseEntity<?> processAudio(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            String transcription = whisperService.transcribeAudio(file.getBytes(), file.getContentType());
            JsonObject transcriptionJson = JsonParser.parseString(transcription).getAsJsonObject();
            transcription = transcriptionJson.get("text").getAsString();
            System.out.println(transcription);

            String chatReply = chatGPTService.getChatGPTReply(transcription);
            System.out.println("Raw reply: " + chatReply);
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
