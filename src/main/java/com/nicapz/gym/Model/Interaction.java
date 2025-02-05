package com.nicapz.gym.Model;
import java.util.stream.Collectors;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Arrays;
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
    private String userId;

    @Getter
    @Column(length = 1000)
    private String userRequest;

    @Getter
    @Column(length = 10000)
    private String aiReply;

    public String toString() {
        return "User: " + userRequest + " Assistant: " + aiReply;
    }

    public Interaction() {}

    public Interaction(String userId, String ConversationId, String userRequest, String aiReply) {
        this.userId = userId;
        this.conversationId = ConversationId;
        this.userRequest = userRequest;
        this.aiReply = aiReply;
    }

}