package com.nicapz.gym.Functions;

public class Mood {
    public record Request(int mood) {}
    public record Response(String reply) {}
}
