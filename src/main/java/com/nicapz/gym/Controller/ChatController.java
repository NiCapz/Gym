package com.nicapz.gym.Controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nicapz.gym.Service.ChatGPTService;
import com.nicapz.gym.Service.WhisperService;
import com.nicapz.gym.Service.WhisperT2SService;
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
@RequestMapping
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final WhisperService whisperService = new WhisperService();
    private final ChatGPTService chatGPTService = new ChatGPTService();
    private final WhisperT2SService whisperT2SService = new WhisperT2SService();

    private WebSocketClient webSocketClient;

    @PostMapping("/initiateSession")
    public void initiateSession() {
        webSocketClient = new WebSocketClient();
    }

    private String parseReply(String reply) {

        JsonObject levelOne = JsonParser.parseString(reply).getAsJsonObject();
        JsonArray choices = levelOne.getAsJsonArray("choices");
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        String content = message.get("content").getAsString();

        return content;
    }


    @PostMapping("/process-audio")
    public void processAudio(@RequestParam("file") MultipartFile file) throws IOException {
        String transcription = whisperService.transcribeAudio(file.getBytes(), file.getContentType());
        JsonObject transcriptionJson = JsonParser.parseString(transcription).getAsJsonObject();
        transcription = transcriptionJson.get("text").getAsString();
        System.out.println(transcription);
        messagingTemplate.convertAndSend("/topic/transcription", transcription);

        String chatReply = chatGPTService.getChatGPTReply(transcription);
        chatReply = parseReply(chatReply);
        System.out.println(chatReply);
        messagingTemplate.convertAndSend("/topic/reply", chatReply);

        byte[] audioBytes = whisperT2SService.synthesizeSpeech(chatReply);
        String audio = Base64.getEncoder().encodeToString(audioBytes);

        messagingTemplate.convertAndSend("/topic/audio", audio);
    }
}


