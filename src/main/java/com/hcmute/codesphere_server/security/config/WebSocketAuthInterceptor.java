package com.hcmute.codesphere_server.security.config;

import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Lấy token từ header Authorization
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    try {
                        String token = authHeader.substring(7);
                        
                        // Validate và parse token
                        if (jwtProvider.validateToken(token)) {
                            Claims claims = jwtProvider.getClaims(token);
                            String userId = claims.getSubject();
                            String email = claims.get("email", String.class);
                            String role = claims.get("role", String.class);
                            
                            UserPrinciple user = new UserPrinciple(userId, email, null,
                                    java.util.List.of(() -> role));
                            
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                            
                            accessor.setUser(auth);
                            
                            // Log để debug
                            System.out.println("WebSocket authenticated user: " + userId + " for STOMP CONNECT");
                        }
                    } catch (Exception e) {
                        // Token không hợp lệ - không set authentication
                        // Connection sẽ bị từ chối nếu cần authenticated
                    }
                }
            }
        }
        
        return message;
    }
}

