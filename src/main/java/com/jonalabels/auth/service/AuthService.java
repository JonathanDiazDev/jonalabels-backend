package com.jonalabels.auth.service;

import com.jonalabels.auth.dto.RegistroRequestDTO;
import com.jonalabels.auth.dto.TokenPair;

public interface AuthService {

    void registrar(RegistroRequestDTO request);

    TokenPair login(String email, String password);

    TokenPair refresh(String refreshToken);
}
