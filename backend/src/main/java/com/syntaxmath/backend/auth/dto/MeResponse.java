package com.syntaxmath.backend.auth.dto;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String firstName,
        String lastName
) {
}
