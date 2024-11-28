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


    UserRequest(String content) {
        this.content = content;
    }


    public UserRequest() {

    }
}
