package com.nicapz.gym.Configs;

import javax.websocket.ClientEndpointConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WebSocketClientConfig extends ClientEndpointConfig.Configurator {

    private static final String API_KEY = "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";

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        headers.put("Authorization", List.of("Bearer " + API_KEY));
        headers.put("OpenAI-Beta", List.of("realtime-v1"));
    }



}
