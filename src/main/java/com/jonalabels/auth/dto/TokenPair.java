package com.jonalabels.auth.dto;

public record TokenPair(String accessToken, String refreshToken, String email, String rol) {
}
