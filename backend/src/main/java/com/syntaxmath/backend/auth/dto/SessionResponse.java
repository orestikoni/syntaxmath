package com.syntaxmath.backend.auth.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        boolean current
) {
}
