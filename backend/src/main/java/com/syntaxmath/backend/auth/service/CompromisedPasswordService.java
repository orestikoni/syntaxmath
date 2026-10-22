package com.syntaxmath.backend.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class CompromisedPasswordService {

    private static final String HIBP_RANGE_URL = "https://api.pwnedpasswords.com/range/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public boolean isCompromised(String password) {
        String passwordHash = sha1(password);
        String prefix = passwordHash.substring(0, 5);
        String suffix = passwordHash.substring(5);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HIBP_RANGE_URL + prefix))
                .timeout(Duration.ofSeconds(3))
                .header("Add-Padding", "true")
                .header("User-Agent", "SyntaxMath-backend")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.US_ASCII)
            );
            if (response.statusCode() != 200) {
                throw serviceUnavailable(null);
            }
            boolean compromised = false;
            for (String responseLine : response.body().lines().toList()) {
                String line = responseLine.trim();
                if (!line.isEmpty() && matchesSuffix(line, suffix)) {
                    compromised = true;
                }
            }
            return compromised;
        } catch (IOException exception) {
            throw serviceUnavailable(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw serviceUnavailable(exception);
        }
    }

    private boolean matchesSuffix(String line, String suffix) {
        int separator = line.indexOf(':');
        if (separator != 35 || !line.substring(0, separator).matches("[A-Fa-f0-9]{35}")) {
            throw serviceUnavailable(null);
        }
        try {
            int count = Integer.parseInt(line.substring(separator + 1));
            return line.substring(0, separator).equalsIgnoreCase(suffix) && count > 0;
        } catch (NumberFormatException exception) {
            throw serviceUnavailable(exception);
        }
    }

    private String sha1(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is unavailable", exception);
        }
    }

    private ResponseStatusException serviceUnavailable(Exception cause) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Password safety check is temporarily unavailable",
                cause
        );
    }
}
