package com.syntaxmath.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password,
        @Size(max = 64) String mfaCode
) {
}
