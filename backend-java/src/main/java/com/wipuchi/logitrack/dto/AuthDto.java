package com.wipuchi.logitrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDto {
    private AuthDto() {}

    public record Login(@NotBlank @Size(max = 128) String username,
                        @NotBlank @Size(max = 256) String password) {
        @Override
        public String toString() {
            return "Login[username=" + username + "]";
        }
    }

    public record Token(String accessToken, String tokenType, long expiresIn) {}
}
