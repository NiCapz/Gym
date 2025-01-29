package com.nicapz.gym.Controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.sql.Connection;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final WhisperService whisperService = new WhisperService();
    private final WhisperT2SService whisperT2SService = new WhisperT2SService();
    private final StreamGPTResponse streamGPTResponse;
    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;


    @Autowired
    public ChatController(StreamGPTResponse streamGPTResponse, ChatClient.Builder chatClientBuilder, EmbeddingModel embeddingModel) {
        this.streamGPTResponse = streamGPTResponse;
        this.chatClient = chatClientBuilder.build();
        this.embeddingModel = embeddingModel;

    }

    @Autowired
    private JdbcClient jdbcClient;
    @Autowired
    InteractionService interactionService;
    @Autowired
    public SimpMessagingTemplate messagingTemplate;

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

    @PostMapping("/process-text")
    public void processText(@RequestParam("text") String text, @RequestParam("sessionId") String sessionId) throws IOException {
        System.out.println("... received chatRequest ...");
        System.out.println(text);
        messagingTemplate.convertAndSend("/topic/transcription/" + sessionId, text);

        float[] embedding = this.embeddingModel.embed(text);
        //List<Interaction> history = interactionService.getInteractionsByConversationId(sessionId);


        List<Interaction> history = jdbcClient.sql("SELECT id, conversation_id, user_request, ai_reply " +
                        "FROM interactions ORDER BY vector <-> :queryEmbedding::vector LIMIT 3")
                .param("queryEmbedding", embedding)
                .query(Interaction.class)
                .list();
        System.out.println("Retrieved interactions: " + history.size());
        System.out.println("Interactions: ");
        for (Interaction interaction : history) {
            System.out.println(interaction.getUserRequest());
        }



        System.out.println("... done retrieving history ...");
        String chatReply = streamGPTResponse.getChatGPTReply(sessionId, text, history);
        /*String chatReply = this.chatClient.prompt()
                .user(text)
                .call()
                .content();*/

        System.out.println(chatReply);
        messagingTemplate.convertAndSend("/topic/chatReply/" + sessionId, chatReply);

        byte[] audioBytes = whisperT2SService.synthesizeSpeech(chatReply);
        String audio = Base64.getEncoder().encodeToString(audioBytes);
        messagingTemplate.convertAndSend("/topic/audio/" + sessionId, audio);

        Interaction interaction = new Interaction(sessionId, text, chatReply);

        jdbcClient.sql("INSERT INTO interactions  (user_request, ai_reply, conversation_id, vector) VALUES (:userRequest, :aiReply, :conversationId, :vector::vector)")
                .param("userRequest", text)
                .param("aiReply", chatReply)
                .param("conversationId", sessionId)
                .param("vector", embedding)
                .update();


        //interactionService.saveInteraction(interaction);
        System.out.println("... interaction saved ...");
    }

}
