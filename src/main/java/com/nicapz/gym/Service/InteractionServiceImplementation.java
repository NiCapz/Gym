package com.nicapz.gym.Service;

import com.nicapz.gym.Model.Interaction;
import com.nicapz.gym.Repositories.InteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InteractionServiceImplementation implements InteractionService {

    @Autowired
    private InteractionRepository interactionRepository;

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
}
