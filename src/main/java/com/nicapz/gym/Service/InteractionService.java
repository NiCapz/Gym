package com.nicapz.gym.Service;

import com.nicapz.gym.Model.Interaction;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface InteractionService {

    void saveInteraction(Interaction interaction);
    List<Interaction> getInteractions();
    List<Interaction> getInteractionsByConversationId(String id);
    void deleteInteraction(long id);
    void saveInteractionWithVector(String userId, String userRequest, String aiReply, String conversationId, float[] embedding);


}
