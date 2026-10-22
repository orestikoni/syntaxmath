package com.syntaxmath.backend.auth.dto;

import java.util.List;

public record MfaRecoveryCodesResponse(List<String> recoveryCodes) {
}
