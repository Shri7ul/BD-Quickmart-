package com.bdquickmart.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket endpoint registration.
 *
 * <p>The live-tracking handler is introduced in Goal 08; this goal only
 * reserves and exposes the endpoint path so that the transport is wired and
 * reachable from the start.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /** WebSocket endpoint used for order tracking and rider location updates. */
    public static final String TRACKING_PATH = "/ws/tracking";

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new NoopTrackingHandler(), TRACKING_PATH)
                .setAllowedOriginPatterns("*");
    }
}
