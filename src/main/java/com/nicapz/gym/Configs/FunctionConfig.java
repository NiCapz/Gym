package com.nicapz.gym.Configs;

import com.nicapz.gym.Functions.Mood;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class FunctionConfig {

    @Bean
    @Description("Gauge the users Mood of a scale of 1 (very bad) to 5 (very good)")
    Function<Mood.Request, Mood.Response> gaugeMood() {
        return null;
    }


}
