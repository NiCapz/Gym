package com.nicapz.gym.Controller;

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

    private final WhisperService whisperService;
    private final ChatGPTService chatGPTService;
    private final WhisperT2SService whisperT2SService;

    public ChatController(WhisperService whisperService, ChatGPTService chatGPTService, WhisperT2SService whisperT2SService) {
        this.whisperService = whisperService;
        this.chatGPTService = chatGPTService;
        this.whisperT2SService = whisperT2SService;
    }

    @PostMapping("/process-audio")
    public ResponseEntity<?> processAudio(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            String transcription = whisperService.transcribeAudio(file.getBytes(), file.getContentType());

            String chatReply = chatGPTService.getChatGPTReply(transcription);

            byte[] audioBytes = whisperT2SService.synthesizeSpeech(chatReply);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(Map.of(
                            "transcription", transcription,
                            "reply", chatReply,
                            "audio", Base64.getEncoder().encodeToString(audioBytes)
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

}
