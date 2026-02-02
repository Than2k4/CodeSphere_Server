package com.hcmute.codesphere_server.config;

import com.hcmute.codesphere_server.security.config.WebSocketAuthInterceptor;
import com.hcmute.codesphere_server.security.config.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to carry messages back to the client
        config.enableSimpleBroker("/topic", "/queue", "/user");
        
        // Prefix for messages from client to server
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the /ws endpoint for WebSocket connections
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new com.hcmute.codesphere_server.security.config.PrincipalHandshakeHandler())
                .addInterceptors(webSocketHandshakeInterceptor)
                .withSockJS();
        
        // Also support native WebSocket without SockJS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new com.hcmute.codesphere_server.security.config.PrincipalHandshakeHandler())
                .addInterceptors(webSocketHandshakeInterceptor);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Thêm interceptor để xác thực WebSocket connections
        registration.interceptors(webSocketAuthInterceptor);
    }
}

