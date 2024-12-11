package com.nicapz.gym.Model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "interactions")
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Getter
    private String conversationId;
    @Getter
    private String userRequest;
    @Getter
    @Column(length = 1000)
    private String aiReply;


    public Interaction() {}

    public Interaction(String ConversationId, String userRequest, String aiReply) {
        this.conversationId = ConversationId;
        this.userRequest = userRequest;
        this.aiReply = aiReply;
    }

}
