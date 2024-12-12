package com.nicapz.gym.Controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nicapz.gym.Model.Interaction;
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
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final WhisperService whisperService = new WhisperService();
    private final WhisperT2SService whisperT2SService = new WhisperT2SService();
    private final StreamGPTResponse streamGPTResponse;
    @Autowired
    public ChatController(StreamGPTResponse streamGPTResponse) {
        this.streamGPTResponse = streamGPTResponse;
    }
    @Autowired
    InteractionService interactionService;
    @Autowired
    public  SimpMessagingTemplate messagingTemplate;

    @PostMapping("/process-audio")
    public void processAudio(@RequestParam("file") MultipartFile file, @RequestParam("sessionId") String sessionId) throws IOException {

        System.out.println("... received chatRequest ...");
        String transcription = whisperService.transcribeAudio(file.getBytes(), file.getContentType());
        JsonObject transcriptionJson = JsonParser.parseString(transcription).getAsJsonObject();
        transcription = transcriptionJson.get("text").getAsString();
        System.out.println(transcription);
        messagingTemplate.convertAndSend("/topic/transcription/" + sessionId, transcription);

        List<Interaction> history = interactionService.getInteractionsByConversationId(sessionId);
        System.out.println("... done retrieving history ...");

        String chatReply = streamGPTResponse.getChatGPTReply(sessionId, transcription, history);
        System.out.println(chatReply);
        messagingTemplate.convertAndSend("/topic/chatReply/" + sessionId, chatReply);


        byte[] audioBytes = whisperT2SService.synthesizeSpeech(chatReply);
        String audio = Base64.getEncoder().encodeToString(audioBytes);
        System.out.println("... sending audio ...");
        messagingTemplate.convertAndSend("/topic/audio/" + sessionId, audio);

        Interaction interaction = new Interaction(sessionId, transcription, chatReply);
        interactionService.saveInteraction(interaction);
        System.out.println("... interaction saved ...");
    }

}
