package com.nicapz.gym.Service;

import com.nicapz.gym.Model.Interaction;
import com.nicapz.gym.Repositories.InteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InteractionServiceImplementation implements InteractionService {

    @Autowired
    private InteractionRepository interactionRepository;

    private final JdbcClient jdbcClient;

    public InteractionServiceImplementation(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void saveInteraction(Interaction interaction) {
        interactionRepository.save(interaction);
    }

    @Override
    public List<Interaction> getInteractions() {
        return interactionRepository.findAll();
    }

    @Override
    public List<Interaction> getInteractionsByConversationId(String id) {
        return interactionRepository.findInteractionsByConversationId(id);
    }

    @Override
    public void deleteInteraction(long id) {
        interactionRepository.deleteById(id);
    }

    @Override
    public void saveInteractionWithVector(String userRequest, String aiReply, String conversationId, float[] embedding) {
        jdbcClient.sql("INSERT INTO interactions  (user_request, ai_reply, conversation_id, vector) VALUES (:userRequest, :aiReply, :conversationId, :vector::vector)")
                .param("userRequest", userRequest)
                .param("aiReply", aiReply)
                .param("conversationId", conversationId)
                .param("vector", embedding)
                .update();
    }


}
