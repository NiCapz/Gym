package com.nicapz.gym.Model;

import jakarta.persistence.*;
import lombok.Generated;

@Entity
@Table
public class UserRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String content;
    private String transcription;


    UserRequest(String content, String transcription) {
        this.content = content;
        this.transcription = transcription;
    }


    public UserRequest() {

    }
}
