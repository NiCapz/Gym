package com.nicapz.gym.Functions;

import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class MoodService implements Function<Mood.Request, Mood.Response> {


    @Override
    public Mood.Response apply(Mood.Request request) {
        return null;
    }
}
