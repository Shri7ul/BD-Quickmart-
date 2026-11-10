package com.bdquickmart.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Placeholder tracking handler used until the live-tracking module (Goal 08)
 * supplies the real implementation.
 */
public class NoopTrackingHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NoopTrackingHandler.class);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket tracking session opened: {}", session.getId());
        try {
            session.sendMessage(new TextMessage(
                    "{\"type\":\"connected\",\"message\":\"tracking channel ready\"}"));
        } catch (Exception ex) {
            log.warn("Failed to send handshake message to session {}", session.getId(), ex);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket tracking session closed: {} ({})", session.getId(), status.getCode());
    }
}
