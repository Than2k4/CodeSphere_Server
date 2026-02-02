package com.hcmute.codesphere_server.security.oauth2;

import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.security.jwt.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Value("${app.oauth2.authorizedRedirectUri:http://localhost:8080/oauth2-redirect.html}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        UserPrinciple user = (UserPrinciple) authentication.getPrincipal();

        String token = jwtProvider.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getAuthorities().iterator().next().getAuthority()
        );

        String redirectUrl = frontendRedirectUri + "?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
