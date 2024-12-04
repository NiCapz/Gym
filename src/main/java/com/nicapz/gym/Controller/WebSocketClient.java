package com.nicapz.gym.Controller;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.entity.StringEntity;

import javax.websocket.*;

@ClientEndpoint
public class WebSocketClient {

    private Session session;

    private final String openMessage = """
            {
                "type": "response.create",
                "response": {
                    "modalities": ["text"],
                    "instructions": "Please assist the user.",
                }
            }
            """;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("connected to server");
        sendMessage(openMessage);
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println(message);
    }

    public void sendMessage(String text) {
        try {
            String sanitizedText = StringEscapeUtils.escapeJson(text);
            String message = """
                    {
                        "type": "conversation.item.create",
                        "item": {
                            "type": "message",
                            "role": "user",
                            "content": [
                                    {
                                        "type": "input_text",
                                        "text": "%s"                                    }
                                ]
                            }
                    }""".formatted(sanitizedText);
            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("Session " + session + "disconnected from server, reason: " + closeReason.getReasonPhrase());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("Error: " + error.getMessage());
        error.printStackTrace();
    }
}
