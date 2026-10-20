package com.syntaxmath.backend.auth.service;

import com.syntaxmath.backend.auth.config.AuthProperties;
import com.syntaxmath.backend.auth.config.ResendProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ResendEmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestClient restClient;
    private final ResendProperties resendProperties;
    private final AuthProperties authProperties;

    public ResendEmailService(ResendProperties resendProperties, AuthProperties authProperties) {
        this.resendProperties = resendProperties;
        this.authProperties = authProperties;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public void sendVerificationEmail(String recipient, String token) {
        String link = authProperties.getAllowedOrigin() + "/verify-email?token=" + token;
        send(recipient, "Verify your SyntaxMath email", """
                Verify your email address by opening this link:
                %s

                This link expires in %s. If you did not create an account, you can ignore this email.
                """.formatted(link, authProperties.getEmailVerificationLifetime()));
    }

    public void sendPasswordResetEmail(String recipient, String token) {
        String link = authProperties.getAllowedOrigin() + "/reset-password?token=" + token;
        send(recipient, "Reset your SyntaxMath password", """
                Reset your password by opening this link:
                %s

                This link expires in %s. If you did not request a reset, you can ignore this email.
                """.formatted(link, authProperties.getPasswordResetLifetime()));
    }

    private void send(String recipient, String subject, String text) {
        if (resendProperties.getApiKey() == null || resendProperties.getApiKey().isBlank()
                || resendProperties.getFrom() == null || resendProperties.getFrom().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Email delivery is not configured"
            );
        }

        try {
            restClient.post()
                    .uri(RESEND_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + resendProperties.getApiKey())
                    .body(Map.of(
                            "from", resendProperties.getFrom(),
                            "to", List.of(recipient),
                            "subject", subject,
                            "text", text
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to deliver authentication email",
                    exception
            );
        }
    }
}
