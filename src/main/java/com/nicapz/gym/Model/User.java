package com.nicapz.gym.Model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
public class User {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    private String username;

    @Setter
    @Getter
    private String firstName;

    @Setter
    @Getter
    private String lastName;

    @Setter
    @Getter
    private int experiencePoints;

    @Setter
    @Getter
    private int level;

    @Enumerated(EnumType.STRING)
    private rank rank;

    void addExperiencePoints(int points) {
        this.experiencePoints += points;
    }

    void levelUp() {
        this.level++;
    }
}
