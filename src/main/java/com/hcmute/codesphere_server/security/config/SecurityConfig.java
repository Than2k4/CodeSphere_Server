package com.hcmute.codesphere_server.security.config;

import com.hcmute.codesphere_server.security.jwt.JwtFilter;
import com.hcmute.codesphere_server.security.oauth2.CustomOAuth2UserService;
import com.hcmute.codesphere_server.security.oauth2.OAuth2SuccessHandler;
import com.hcmute.codesphere_server.security.authentication.UserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomOAuth2UserService oauthService;
    private final OAuth2SuccessHandler successHandler;
    private final UserDetailService userDetailService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Thời gian cache CORS preflight request (1 giờ)
        configuration.setMaxAge(3600L);
        
        // Cho phép các origin này gọi API
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",      // Frontend React dev
                "http://localhost:5173",      // Frontend Vite dev (default)
                "http://localhost:8081",      // Frontend dev khác
                "https://hcmute-consultant.vercel.app",  // Frontend production
                "https://hcmute-consultant-server-production.up.railway.app"  // Backend production
        ));
        
        // Cho phép gửi credentials (cookies, Authorization header)
        configuration.setAllowCredentials(true);
        
        // Cho phép các headers này
        configuration.setAllowedHeaders(Arrays.asList(
                "Access-Control-Allow-Headers",
                "Access-Control-Allow-Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Origin",
                "Cache-Control",
                "Content-Type",
                "Authorization"  // Quan trọng cho JWT token
        ));
        
        // Cho phép các HTTP methods này
        configuration.setAllowedMethods(Arrays.asList(
                "DELETE", "GET", "POST", "PATCH", "PUT", "OPTIONS"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());
        
        // Cấu hình CORS
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // Cấu hình phân quyền: Public endpoints không cần token, các endpoint khác cần token
        http.authorizeHttpRequests(auth -> auth
                // Public endpoints - không cần token
                .requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/register/init",
                        "/api/v1/auth/register/verify",
                        "/api/v1/auth/login",
                        "/api/v1/auth/google",
                        "/api/v1/auth/forgot-password/init",
                        "/api/v1/auth/forgot-password/verify",
                        "/api/v1/auth/test-token",
                        "/api/v1/languages/**",      // GET languages (public)
                        "/api/v1/categories/**",     // GET categories (public)
                        "/api/v1/tags/**",           // GET tags (public)
                        "/api/v1/problems",         // GET problems list (public)
                        "/api/v1/problems/{id}",    // GET problem detail (public)
                        "/api/v1/ai/**",            // AI endpoints (require auth)
                        // /api/v1/problems/{id}/testcases - cần admin (check trong controller)
                        "/oauth2/**",
                        "/test-auth.html",
                        "/oauth2-redirect.html",
                        "/error",
                        "/static/**",
                        "/*.html",
                        "/ws/**"                     // WebSocket endpoint - authentication handled in interceptor
                ).permitAll()
                // Admin endpoints - cần token và quyền admin (được check trong Controller)
                // /api/v1/admin/languages (POST) - tạo language
                // /api/v1/admin/categories (POST) - tạo category
                // User endpoints - cần token (JWT)
                // Tất cả các endpoint khác - cần token (JWT)
                .anyRequest().authenticated()
        );

        http.oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> userInfo.userService(oauthService))
                .successHandler(successHandler)
        );

        // Xử lý exception: Trả về 401 JSON thay vì redirect OAuth2
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    // Chỉ redirect OAuth2 cho web requests, trả về 401 cho API
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(
                                "{\"success\":false,\"message\":\"Unauthorized - Token không hợp lệ hoặc thiếu\",\"data\":null}"
                        );
                    } else {
                        // Redirect đến OAuth2 login cho web requests
                        response.sendRedirect("/oauth2/authorization/google");
                    }
                })
        );

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
