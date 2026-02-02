package com.hcmute.codesphere_server.security.config;

import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class PrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Lấy authentication từ attributes (đã được set bởi WebSocketHandshakeInterceptor)
        Object userObj = attributes.get("user");
        
        if (userObj instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth = 
                    (org.springframework.security.authentication.UsernamePasswordAuthenticationToken) userObj;
            
            if (auth.getPrincipal() instanceof UserPrinciple) {
                UserPrinciple userPrinciple = (UserPrinciple) auth.getPrincipal();
                // Trả về một Principal với getName() trả về userId (để Spring map đúng user destination)
                return new Principal() {
                    @Override
                    public String getName() {
                        return userPrinciple.getUserId();
                    }
                };
            }
        }
        
        // Fallback: tạo anonymous principal
        return new Principal() {
            @Override
            public String getName() {
                return "anonymous";
            }
        };
    }
}

