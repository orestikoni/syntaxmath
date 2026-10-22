package com.syntaxmath.backend.auth.dto;

public record MfaEnrollmentResponse(String secret, String otpauthUri) {
}
