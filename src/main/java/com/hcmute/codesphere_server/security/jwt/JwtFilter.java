package com.hcmute.codesphere_server.security.jwt;

import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, java.io.IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);

                // Validate token trước khi parse
                if (jwtProvider.validateToken(token)) {
                    Claims claims = jwtProvider.getClaims(token);
                    String userId = claims.getSubject();
                    String email = claims.get("email", String.class);
                    String role = claims.get("role", String.class);

                    UserPrinciple user = new UserPrinciple(userId, email, null,
                            java.util.List.of(() -> role));

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // Token không hợp lệ - không set authentication
                // Spring Security sẽ trả về 401 Unauthorized nếu endpoint cần authenticated
            }
        }
        filterChain.doFilter(request, response);
    }
}
