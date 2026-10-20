package com.syntaxmath.backend.auth.config;

import com.syntaxmath.backend.auth.filter.OriginCheckFilter;
import com.syntaxmath.backend.auth.filter.SessionAuthenticationFilter;
import com.syntaxmath.backend.oauth.service.GoogleOAuth2UserService;
import com.syntaxmath.backend.oauth.service.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OriginCheckFilter originCheckFilter;
    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(OriginCheckFilter originCheckFilter,
                          SessionAuthenticationFilter sessionAuthenticationFilter,
                          GoogleOAuth2UserService googleOAuth2UserService,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.originCheckFilter = originCheckFilter;
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                ))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(info -> info.oidcUserService(googleOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .addFilterBefore(originCheckFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
