package com.nicapz.gym.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "reply_table")
public class ChatGPTReply {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String content;

    public ChatGPTReply(String content) {
        this.content = content;
    }


    public ChatGPTReply() {

    }
}
