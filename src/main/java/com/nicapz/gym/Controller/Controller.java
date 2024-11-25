package com.nicapz.gym.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.IOException;
import java.io.InputStream;

@RestController
@CrossOrigin(origins = "*")
public class Controller {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // disable devtools to make website reload less often
    private final Model model = new Model("ModelDE");
    public Controller() throws IOException {
    }

    @PostMapping("/transcribeFull")
    public void complete2(@RequestParam("sessionId") String sessionId, @RequestParam("audio") MultipartFile audioFile, @RequestParam("sampleRate") int sampleRate) throws IOException {
        String transcription = transcribeAudio(audioFile.getInputStream(), model, sampleRate, sessionId);
        System.out.println(transcription);
        transcription = transcription.substring(14, transcription.length()-3);
        messagingTemplate.convertAndSend("/topic/transcriptions/" + sessionId, transcription);
    }

    private String transcribeAudio(InputStream audioStream, Model model, int sampleRate, String sessionId) throws IOException {
        try (Recognizer recognizer = new Recognizer(model, sampleRate)) {
            String partialResult;
            byte[] buffer = new byte[4096];
            int bytesRead = audioStream.read(buffer);
            while((bytesRead = audioStream.read(buffer)) > 0) {
                recognizer.acceptWaveForm(buffer, bytesRead);
                partialResult = recognizer.getPartialResult();
                partialResult = partialResult.substring(17, partialResult.length()-3);
                messagingTemplate.convertAndSend("/topic/partialTranscriptions/" + sessionId, partialResult);
                System.out.println(partialResult);
            }
            return recognizer.getFinalResult();
        }
    }
}