package com.nicapz.gym.Repositories;

import com.nicapz.gym.Model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {

        @Query("SELECT i FROM Interaction i WHERE i.conversationId = :id")
        List<Interaction> findInteractionsByConversationId(String id);

}
