package com.nicapz.gym.Service;

import com.nicapz.gym.Model.Interaction;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface InteractionService {

    public void saveInteraction(Interaction interaction);
    public List<Interaction> getInteractions();
    public List<Interaction> getInteractionsByConversationId(String id);
    public void deleteInteraction(long id);


}
