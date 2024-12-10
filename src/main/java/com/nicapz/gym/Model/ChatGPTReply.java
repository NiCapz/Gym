package com.nicapz.gym.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "reply_table")
public class ChatGPTReply {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String content;
    private byte[] audio;

    public ChatGPTReply(String content, byte[] audio) {
        this.content = content;
        this.audio = audio;
    }


    public ChatGPTReply() {

    }
}
