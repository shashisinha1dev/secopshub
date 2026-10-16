package com.secopshub.auth.dto;

import java.util.List;

public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String username,
        List<String> roles) {
}
