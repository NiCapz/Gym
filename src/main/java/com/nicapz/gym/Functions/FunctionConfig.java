package com.nicapz.gym.Functions;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.function.Function;

@Configuration
public class FunctionConfig {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final UserContext userContext;

    public FunctionConfig(SimpMessagingTemplate simpMessagingTemplate, UserContext userContext) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.userContext = userContext;
    }

    @Bean
    @Description("Gauge the users mood on a scale of 1 (extremely bad) to 5 (extremely good) based on their tone and the content of their message. Use only the last message supplied to you for this purpose!")
    public Function<MoodService.Request, MoodService.Response> moodGaugeFunction() {
        return new MoodService(simpMessagingTemplate, userContext);
    }
}
