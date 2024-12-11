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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    private final StreamGPTResponse streamGPTResponse;

    @Autowired
    public ChatController(StreamGPTResponse streamGPTResponse) {
        this.streamGPTResponse = streamGPTResponse;
    }

    @Autowired
    InteractionService interactionService;

    @Autowired
    public  SimpMessagingTemplate messagingTemplate;

    private String parseReply(String reply) {

        JsonObject levelOne = JsonParser.parseString(reply).getAsJsonObject();
        JsonArray choices = levelOne.getAsJsonArray("choices");
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        String content = message.get("content").getAsString();

        return content;
    }

    @PostMapping("/process-audio")
    public void processAudio(@RequestParam("file") MultipartFile file, @RequestParam("sessionId") String sessionId) throws IOException {

        String transcription = whisperService.transcribeAudio(file.getBytes(), file.getContentType());
        JsonObject transcriptionJson = JsonParser.parseString(transcription).getAsJsonObject();
        transcription = transcriptionJson.get("text").getAsString();
        System.out.println(transcription);
        messagingTemplate.convertAndSend("/topic/transcription/" + sessionId, transcription);

        List<Interaction> history = interactionService.getInteractionsByConversationId(sessionId);
        System.out.println("... done retrieving history ...");

        String chatReply = streamGPTResponse.getChatGPTReply(sessionId, transcription, history);
        System.out.println(chatReply);
        chatReply = parseReply(chatReply);
        System.out.println(chatReply);
        messagingTemplate.convertAndSend("/topic/chatReply/"+ sessionId, chatReply);


        byte[] audioBytes = whisperT2SService.synthesizeSpeech(chatReply);
        String audio = Base64.getEncoder().encodeToString(audioBytes);
        messagingTemplate.convertAndSend("/topic/audio/" + sessionId, audio);

        Interaction interaction = new Interaction(sessionId, transcription, chatReply);
        interactionService.saveInteraction(interaction);
        System.out.println("... interaction saved ...");
    }

    @PostMapping("/process-audio2")
    public void processAudio2(@RequestParam("file") MultipartFile file, @RequestParam("sessionId") String sessionId) throws IOException {

            String transcription = whisperService.transcribeAudio(file.getBytes(), file.getContentType());
            JsonObject transcriptionJson = JsonParser.parseString(transcription).getAsJsonObject();
            transcription = transcriptionJson.get("text").getAsString();
            System.out.println(transcription);
            messagingTemplate.convertAndSend("/topic/transcription/" + sessionId, transcription);

            List<Interaction> history = interactionService.getInteractionsByConversationId(sessionId);
            System.out.println("... done retrieving history ...");

            String chatReply = chatGPTConversationService.getChatGPTReply(sessionId, transcription, history);
            System.out.println(chatReply);
            chatReply = parseReply(chatReply);
            System.out.println(chatReply);
            messagingTemplate.convertAndSend("/topic/chatReply/"+ sessionId, chatReply);


            byte[] audioBytes = whisperT2SService.synthesizeSpeech(chatReply);
            String audio = Base64.getEncoder().encodeToString(audioBytes);
            messagingTemplate.convertAndSend("/topic/audio/" + sessionId, audio);

            Interaction interaction = new Interaction(sessionId, transcription, chatReply);
            interactionService.saveInteraction(interaction);
            System.out.println("... interaction saved ...");
    }
}
