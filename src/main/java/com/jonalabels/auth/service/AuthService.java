package com.jonalabels.auth.service;

import com.jonalabels.auth.dto.TokenPair;

public interface AuthService {

    TokenPair login(String email, String password);

    TokenPair refresh(String refreshToken);
}
