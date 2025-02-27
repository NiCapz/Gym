package com.nicapz.gym.Controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nicapz.gym.Functions.UserContext;
import com.nicapz.gym.Service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

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

    @PostMapping("/initiate-session")
    public void initiateSession(@RequestParam ("sessionId") String sessionId, @RequestParam("userId") String userId) throws IOException {
        System.out.println("initiating Session.");
        userContext.setUserId(userId);

        String userGreeting = "Hi there.";
        messagingTemplate.convertAndSend("/topic/greeting/" + sessionId, userGreeting);
        System.out.println(userGreeting);
        String welcomeResponse = springAIChatClient.helloUser(userGreeting);
        byte[] audioBytes = whisperT2SService.synthesizeSpeech(welcomeResponse);
        System.out.println(welcomeResponse);
        float[] embedding = rag.embedPrompt(welcomeResponse);
        messagingTemplate.convertAndSend("/topic/chatReply/" + sessionId, welcomeResponse);
        String audio = Base64.getEncoder().encodeToString(audioBytes);
        messagingTemplate.convertAndSend("/topic/audio/" + sessionId, audio);
        interactionService.saveInteractionWithVector(userId, userGreeting, welcomeResponse, sessionId, embedding);

    }

    @PostMapping("/process-audio")
    public void processAudio(@RequestParam("file") MultipartFile file, @RequestParam("sessionId") String sessionId, @RequestParam("userId") String userId) throws IOException {
        System.out.println("... received chatRequest ...");
        String text = whisperService.transcribeAudio(file.getBytes(), file.getContentType());
        JsonObject transcriptionJson = JsonParser.parseString(text).getAsJsonObject();
        text = transcriptionJson.get("text").getAsString();
        System.out.println(text);
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

    @PostMapping("/process-text")
    public void processText(@RequestParam("text") String text, @RequestParam("sessionId") String sessionId, @RequestParam("userId") String userId) throws IOException {
        userContext.setUserId(userId);
        System.out.println("Controller 86: user ID: " + userId);
        messagingTemplate.convertAndSend("/topic/transcription/" + sessionId, text);
        String springAiResponse = springAIChatClient.generateResponse(text, sessionId, userId);
        byte[] audioBytes = whisperT2SService.synthesizeSpeech(springAiResponse);
        String audio = Base64.getEncoder().encodeToString(audioBytes);
        System.out.println("AI response: " + springAiResponse);
        messagingTemplate.convertAndSend("/topic/audio/" + sessionId, audio);
        messagingTemplate.convertAndSend("/topic/chatReply/" + sessionId, springAiResponse);

        float[] embedding = rag.embedPrompt(text);
        interactionService.saveInteractionWithVector(userId, text, springAiResponse, sessionId, embedding);
        System.out.println("... interaction saved ...");
    }
    @PostMapping("/process-button")
    public void processButton(@RequestParam("prompt") String prompt, @RequestParam("userVisible") String userVisible, @RequestParam("sessionId") String sessionId, @RequestParam("userId") String userId) throws IOException {
        userContext.setUserId(userId);
        System.out.println("Controller 86: user ID: " + userId);
        messagingTemplate.convertAndSend("/topic/transcription/" + sessionId, userVisible
        );
        String springAiResponse = springAIChatClient.generateResponse(prompt, sessionId, userId);
        byte[] audioBytes = whisperT2SService.synthesizeSpeech(springAiResponse);
        String audio = Base64.getEncoder().encodeToString(audioBytes);
        System.out.println("AI response: " + springAiResponse);
        messagingTemplate.convertAndSend("/topic/audio/" + sessionId, audio);
        messagingTemplate.convertAndSend("/topic/chatReply/" + sessionId, springAiResponse);

        float[] embedding = rag.embedPrompt(prompt);
        interactionService.saveInteractionWithVector(userId, prompt, springAiResponse, sessionId, embedding);
        System.out.println("... interaction saved ...");
    }
}
