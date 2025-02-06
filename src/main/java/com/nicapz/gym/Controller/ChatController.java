package com.nicapz.gym.Controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nicapz.gym.Functions.UserContext;
import com.nicapz.gym.Model.Interaction;
import com.nicapz.gym.Service.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final WhisperService whisperService = new WhisperService();
    private final WhisperT2SService whisperT2SService = new WhisperT2SService();
    private final StreamGPTResponse streamGPTResponse;
    private final UserContext userContext;

    @Autowired
    private SpringAIChatClient springAIChatClient;
    @Autowired
    InteractionService interactionService;
    @Autowired
    public SimpMessagingTemplate messagingTemplate;
    @Autowired
    public RAG rag;

    @Autowired
    public ChatController(StreamGPTResponse streamGPTResponse, UserContext userContext) {
        this.streamGPTResponse = streamGPTResponse;
        this.userContext = userContext;
    }
/*
    @PostMapping("/process-audio")
    public void processAudio(@RequestParam("file") MultipartFile file, @RequestParam("sessionId") String sessionId) throws IOException {
        System.out.println("... received chatRequest ...");
        String text = whisperService.transcribeAudio(file.getBytes(), file.getContentType());
        JsonObject transcriptionJson = JsonParser.parseString(text).getAsJsonObject();
        text = transcriptionJson.get("text").getAsString();
        System.out.println(text);
        messagingTemplate.convertAndSend("/topic/transcription/" + sessionId, text);
        List<Interaction> history = interactionService.getInteractionsByConversationId(sessionId);
        float[] embedding = rag.embedPrompt(text);
        List<Interaction> ragResults = rag.semanticSearch(embedding, 5, .4f);

        System.out.println("Retrieved interactions: " + ragResults.size());
        System.out.println("Interactions: ");

        String chatReply = streamGPTResponse.getChatGPTReply(sessionId, text, history, ragResults);
        messagingTemplate.convertAndSend("/topic/chatReply/" + sessionId, chatReply);
        System.out.println(chatReply);

        byte[] audioBytes = whisperT2SService.synthesizeSpeech(chatReply);
        String audio = Base64.getEncoder().encodeToString(audioBytes);
        messagingTemplate.convertAndSend("/topic/audio/" + sessionId, audio);

        interactionService.saveInteractionWithVector(text, chatReply, sessionId, embedding);

        System.out.println("... interaction saved ...");
    }*/

    @PostMapping("/process-text")
    public void processText(@RequestParam("text") String text, @RequestParam("sessionId") String sessionId, @RequestParam("userId") String userId) throws IOException {
        userContext.setUserId(userId);
        System.out.println("user ID: " + userId);
        messagingTemplate.convertAndSend("/topic/transcription/" + sessionId, text);
        String springAiResponse = springAIChatClient.generateResponse(text, sessionId, userId);
        float[] embedding = rag.embedPrompt(text);
        System.out.println("AI response: " + springAiResponse);
        messagingTemplate.convertAndSend("/topic/chatReply/" + sessionId, springAiResponse);

        byte[] audioBytes = whisperT2SService.synthesizeSpeech(springAiResponse);
        String audio = Base64.getEncoder().encodeToString(audioBytes);
        messagingTemplate.convertAndSend("/topic/audio/" + sessionId, audio);
        interactionService.saveInteractionWithVector(userId, text, springAiResponse, sessionId, embedding);
        System.out.println("... interaction saved ...");
    }
}
