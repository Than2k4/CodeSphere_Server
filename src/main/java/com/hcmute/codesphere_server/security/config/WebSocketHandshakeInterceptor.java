package com.hcmute.codesphere_server.security.config;

import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            // Lấy token từ query parameter
            String token = httpRequest.getParameter("token");
            
            // Nếu không có trong query, thử lấy từ header
            if (token == null || token.isEmpty()) {
                String authHeader = httpRequest.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
            
            if (token != null && !token.isEmpty()) {
                try {
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
                        
                        // Lưu authentication vào attributes để sử dụng sau
                        attributes.put("user", auth);
                        return true;
                    }
                } catch (Exception e) {
                    // Token không hợp lệ
                    return false;
                }
            }
        }
        
        // Cho phép kết nối ngay cả khi không có token (sẽ được xác thực trong STOMP CONNECT)
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Không cần xử lý gì sau handshake
    }
}

