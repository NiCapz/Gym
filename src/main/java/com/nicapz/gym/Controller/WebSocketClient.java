package com.nicapz.gym.Controller;

import lombok.Getter;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.entity.StringEntity;

import javax.websocket.*;


public class WebSocketClient extends Endpoint{

    @Getter
    private static WebSocketClient instance;

    public Session session;

    public WebSocketClient() {
        instance = this;
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        this.session = session;
        System.out.println("connected to server");
        session.addMessageHandler(String.class, this::onMessage);
        System.out.println("Session open: " + session.isOpen());
        sendMessage(openMessage);

        System.out.println("Session ID:" + session.getId());
    }

    private final String openMessage = """
            {
                "type": "response.create",
                "response": {
                    "modalities": ["text"],
                    "instructions": "Please assist the user."
                }
            }
            """;

    public void onMessage(String message) {
        System.out.println("Message received: " + message);
    }

    public void sendMessage(String text) {
        try {
            if (session == null) {
                System.err.println("Session is not open yet, cannot send message.");
                return; // Prevent further execution if session is null
            }
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
